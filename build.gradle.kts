import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.OffsetDateTime
import java.util.Properties

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// ---------------------------------------------------------------------------
// Version bumping
//
// version.properties is the single source of truth for VERSION_NAME and
// VERSION_CODE. VERSION_NAME is bumped deliberately, based on what changed:
//
//   feature  ->  minor + 1, patch unchanged   (1.2.2 -> 1.3.2)
//   fix      ->  patch + 1                    (1.3.2 -> 1.3.3)
//   breaking ->  major + 1, patch unchanged   (1.3.2 -> 2.0.2)
//
// VERSION_CODE is incremented automatically by every Release build so each
// upload to Play gets a unique, increasing code. This task deliberately leaves
// it alone, otherwise the two increments would double count.
//
//   ./gradlew bumpVersion -Ptype=feature -Pnote="Responsive layout on all screen sizes"
//
// The new version is also reported to the Grafizum dashboard when
// GRAFIZUM_DASHBOARD_URL and GRAFIZUM_DASHBOARD_TOKEN are set (system env or
// the git-ignored .env). When they are not set the report is skipped, and a
// failed report never fails the bump.
// ---------------------------------------------------------------------------

val appName = "RepIo"
val packageName = "com.grafizum.repio"
val versionPropsFile = rootProject.file("version.properties")

fun loadVersionProps(): Properties {
    val props = Properties()
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { props.load(it) }
    }
    return props
}

/** Splits a MAJOR.MINOR.PATCH string, tolerating a leading "v". */
fun splitVersionName(name: String): Triple<Int, Int, Int> {
    val parts = name.trim().removePrefix("v").split(".")
    require(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
        "VERSION_NAME must look like MAJOR.MINOR.PATCH, but version.properties has '$name'."
    }
    return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

/** Reads a setting from the system environment first, then from the git-ignored .env. */
fun environmentSetting(key: String): String? {
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return null
    val props = Properties()
    FileInputStream(envFile).use { props.load(it) }
    return props.getProperty(key)?.takeIf { it.isNotBlank() }
}

fun jsonEscape(value: String): String = buildString {
    for (character in value) {
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character < ' ') append("\\u%04x".format(character.code)) else append(character)
        }
    }
}

/**
 * Pushes the new version to the Grafizum dashboard.
 *
 * Returns a short status line for the build log. Never throws: a dashboard that
 * is unreachable or unconfigured must not block a local version bump.
 */
fun reportToDashboard(
    versionName: String,
    versionCode: Int,
    changeType: String,
    note: String
): String {
    val url = environmentSetting("GRAFIZUM_DASHBOARD_URL")
    val token = environmentSetting("GRAFIZUM_DASHBOARD_TOKEN")

    if (url.isNullOrBlank() || token.isNullOrBlank()) {
        return "Grafizum dashboard not configured " +
            "(set GRAFIZUM_DASHBOARD_URL and GRAFIZUM_DASHBOARD_TOKEN) - report skipped."
    }

    val payload = """
        {
          "app": "${jsonEscape(appName)}",
          "packageName": "${jsonEscape(packageName)}",
          "versionName": "${jsonEscape(versionName)}",
          "versionCode": $versionCode,
          "changeType": "${jsonEscape(changeType)}",
          "note": "${jsonEscape(note)}",
          "reportedAt": "${OffsetDateTime.now()}"
        }
    """.trimIndent()

    return try {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }?.take(300).orEmpty()
        connection.disconnect()

        if (status in 200..299) {
            "Grafizum dashboard updated (HTTP $status)."
        } else {
            "Grafizum dashboard rejected the report (HTTP $status) $body - bump kept locally."
        }
    } catch (exception: Exception) {
        "Could not reach the Grafizum dashboard (${exception.javaClass.simpleName}: " +
            "${exception.message}) - bump kept locally."
    }
}

tasks.register("bumpVersion") {
    group = "versioning"
    description =
        "Bumps VERSION_NAME per change type (-Ptype=feature|fix|breaking, optional -Pnote=\"...\")."

    doLast {
        val requestedType = (project.findProperty("type") as String?)?.trim()?.lowercase()
            ?: throw GradleException(
                "Missing -Ptype. Use one of: feature, fix, breaking. " +
                    "Example: ./gradlew bumpVersion -Ptype=feature -Pnote=\"What changed\""
            )

        val changeType = when (requestedType) {
            "feature", "feat", "minor" -> "feature"
            "fix", "bugfix", "patch" -> "fix"
            "breaking", "major" -> "breaking"
            else -> throw GradleException(
                "Unknown -Ptype '$requestedType'. Use one of: feature, fix, breaking."
            )
        }
        val note = (project.findProperty("note") as String?)?.trim().orEmpty()

        val props = loadVersionProps()
        val currentName = props.getProperty("VERSION_NAME")
            ?: throw GradleException("version.properties has no VERSION_NAME.")
        val currentCode = (props.getProperty("VERSION_CODE") ?: "0").toInt()
        val (major, minor, patch) = splitVersionName(currentName)

        val (nextMajor, nextMinor, nextPatch) = when (changeType) {
            "feature" -> Triple(major, minor + 1, patch)
            "fix" -> Triple(major, minor, patch + 1)
            // A new major starts a fresh minor cycle, but the patch digit carries
            // over because it tracks the release line, not the feature set.
            else -> Triple(major + 1, 0, patch)
        }
        val nextName = "$nextMajor.$nextMinor.$nextPatch"

        props.setProperty("VERSION_NAME", nextName)
        versionPropsFile.outputStream().use { props.store(it, null) }

        logger.lifecycle("")
        logger.lifecycle("Version bumped ($changeType): $currentName -> $nextName")
        logger.lifecycle("  versionCode: $currentCode (increments automatically on each Release build)")
        if (note.isNotEmpty()) logger.lifecycle("  note: $note")
        logger.lifecycle("  ${reportToDashboard(nextName, currentCode, changeType, note)}")
        logger.lifecycle("")
    }
}
