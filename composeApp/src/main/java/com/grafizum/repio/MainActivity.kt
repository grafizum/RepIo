package com.grafizum.repio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.grafizum.repio.ui.screens.*
import com.grafizum.repio.ui.theme.LocalLightTheme
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.google.android.play.core.appupdate.AppUpdateInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.util.AppUpdateChecker
import com.grafizum.repio.util.applyAppIconChange
import com.grafizum.repio.util.NotificationHelper
import com.grafizum.repio.ui.components.AdaptiveWidthContainer
import com.grafizum.repio.ui.components.AppDialog
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.LocalWeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.grafizum.repio.R


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * The sliding pill behind the selected tab.
 *
 * It was `primary.copy(alpha = 0.1f)` everywhere, and on a dark scheme that is NOT a dimmer
 * accent - it is a different colour: 10% of gold over the bar's raised grey lands on #3F3B31, a
 * muddy tan, measured on the emulator at 1.5% of the frame as a 246x166px patch that follows the
 * selection from tab to tab. Owner, 2026-10-14: *"instead of a creamy gold i want an accent
 * gold"*. The accent already marks the selected tab where it can actually be an accent - the
 * icon and the label are full `primary` (see `selectedIconColor` below) - so the pill takes a
 * neutral rung of the same grey ladder on a dark scheme and keeps its soft bronze tint on a light
 * one, where a tint of the accent over cream is a designed edge rather than a muddied one.
 *
 * A dark scheme's pill is deliberately a WHISPER: `surfaceContainerHighest` is one rung above the
 * bar's own raised grey, so it reads as a faint lift and does no signalling of its own. Making
 * the selected tab legible is the accent's job here, and the gold icon at 10:1 on the canvas does
 * it without painting the accent as a surface.
 *
 * Pure, so `DarkThemeContrastTest` can hold every dark theme to it without rendering the bar.
 */
internal fun navIndicatorFill(scheme: ColorScheme, isLightTheme: Boolean): Color =
    if (isLightTheme) scheme.primary.copy(alpha = 0.1f) else scheme.surfaceContainerHighest

/**
 * The bottom bar's own grey.
 *
 * Owner, 2026-10-14: *"the grey of the bottom bar ... is the one you were supposed to make darker
 * ... i want it to have the same visuals as the light beige theme"*. The bar was `surface` (plus
 * Material's 3dp tonal lift), and on a dark scheme that composited to #2C2C2C - 1.42:1 from the
 * canvas, where the beige theme's bar is #F0EBE5 against a #FDFCFB canvas, 1.157:1, and is the
 * WIDEST neutral step that theme ever takes. So the bar states its rung instead of inheriting one,
 * with the tonal lift left on top of it for the shadow (it can only add a whisper now - see the
 * dark scheme's `surfaceTint`).
 *
 * ON A LIGHT SCHEME it is `surfaceContainerHigh`, which `withThemedContainers` collapses onto the
 * scheme's own `surface` - the bar is a step off the canvas and that is all it needs to be. That
 * step MEASURES 1.044:1 through the collapse; the raw tokens read 1.157:1, which is the number
 * to quote only if the collapse is forgotten - it is what a threshold gets written against that
 * the rendering then cannot meet.
 *
 * ON A DARK SCHEME it is THE CANVAS ITSELF, not a rung of its own.
 *
 * It used to be a token of its own - DarkBarSurface, #1B1B1B, 1.121:1 over the canvas - a
 * deliberate step, chosen
 * on 2026-10-14 so the bar would not share the hero cards' grey. That step is exactly what the
 * owner reported on 2026-10-17: *"there is a black line between the content of the day and the
 * bottom tab switches, remove it thats not professional."*
 *
 * MEASURED ON THE PHONE (SM-S906B, My Workout tab, x=540): the last card ends at #191919 (lum
 * 25), then 42px of CANVAS at #0E0E0E (lum 14), then the bar at #1B1B1B (lum 27). Nothing is
 * drawn in that strip - it is the page background showing between the content and the bar - but
 * it is the darkest thing on the screen at that boundary, so the eye reads it as a line rather
 * than as space.
 *
 * THE ASYMMETRY IS THE WHOLE STORY, and it is why the light theme never had this report. In
 * beige the bar is DARKER than the canvas (1.157:1), so the same strip is the BRIGHTEST thing
 * there and reads as breathing room. On dark the canvas is the deepest tone the palette owns, so
 * a bar one notch ABOVE it leaves the strip as the darkest band. There is no value the bar can
 * take that is both a step below the canvas and not below the palette's floor - so the step is
 * the defect, not its size.
 *
 * The bar therefore matches the canvas exactly and the tabs float on the page, with the sliding
 * pill ([navIndicatorFill]) and the accent icons carrying the affordance. That is also the
 * direction the owner's own 2026-10-14 correction was already heading - the bar was rejected at
 * 1.42:1 for being too light, and this is the same move taken all the way.
 */
internal fun navBarFill(scheme: ColorScheme, isLightTheme: Boolean): Color =
    if (isLightTheme) scheme.surfaceContainerHigh else scheme.background

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home") { override val icon = Icons.Default.Home }
    object Workout : Screen("workout", "Workout") { override val icon = Icons.Default.FitnessCenter }
    object Analytics : Screen("analytics", "Analytics") { override val icon = Icons.Default.Assessment }
    object Settings : Screen("settings", "Settings") { override val icon = Icons.Default.Settings }
    object EasterEgg : Screen("easter_egg", "Creator") { override val drawableRes = R.drawable.ic_easter_egg }
    object Onboarding : Screen("onboarding", "Welcome") { override val icon = Icons.Default.Home }

    open val icon: ImageVector? = null
    open val drawableRes: Int? = null
}

open class MainActivity : AppCompatActivity() {

    private val viewModel: WorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = WorkoutRepository(db.workoutDao())
                return WorkoutViewModel(repository) as T
            }
        }
    }

    private val _navTarget = MutableStateFlow<String?>(null)
    val navTarget: StateFlow<String?> = _navTarget.asStateFlow()

    // Set when the app is (re)opened via the "update available" notification,
    // so the Play update flow can start as soon as the UI is ready.
    private val _startUpdateFlow = MutableStateFlow(false)
    val startUpdateFlowOnResume: StateFlow<Boolean> = _startUpdateFlow.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lock phones to portrait. Tablets (>=600dp smallest screen width) keep full
        // sensor rotation so they can also be used in landscape.
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Create notification channel for rest timer
        NotificationHelper.createNotificationChannel(this)

        // Handle initial intent
        handleIntent(intent)

        setContent {
            val themeOption by viewModel.themeOption.collectAsState()
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            LaunchedEffect(appLanguage) {
                val targetLocales = if (appLanguage == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(appLanguage)
                }
                
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            LaunchedEffect(Unit) {
                delay(200) // Small warm up to avoid black flicker
            }

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // Update foreground state
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> com.grafizum.repio.util.AppEventBus.setAppForeground(true)
                        Lifecycle.Event.ON_STOP -> {
                            com.grafizum.repio.util.AppEventBus.setAppForeground(false)
                            // Timestamp the last use so the background check can tell
                            // how many days the app has been sitting idle.
                            com.grafizum.repio.util.MotivationNudger.recordAppUsage(context)
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    com.grafizum.repio.util.AppEventBus.setAppForeground(false)
                }
            }

            DisposableEffect(keepScreenOn) {
                val activity = context as? Activity
                if (keepScreenOn) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // One unit for the whole app. A load turns into a string in a dozen
            // places — the picker, the live card, both editors, the logged-set
            // summary — so it is provided here rather than passed to each, which
            // is how one of them ends up printing kilograms to a US user.
            val weightUnit by viewModel.weightUnit.collectAsState()
            CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
                MainAppStructure(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Once per process: see [onResume]'s repair. */
    private var launcherIconRepaired = false

    override fun onResume() {
        super.onResume()
        // THE LAUNCHER ENTRY IS PUT BACK IN STEP WITH THE STORED THEME, once per process.
        //
        // Two jobs, one moment. (1) MIGRATION: the entries stopped being the app on
        // 2026-10-14 (they hand off to this activity now - see [ThemeLauncherEntry]), so a
        // device that updates INTO that split still has whatever component the old build
        // enabled, and since that component no longer exists Android falls back to the entry
        // the manifest enables, which is Beige - every existing user would find their icon
        // turned cream. (2) REPAIR: any install whose icon and theme have drifted apart gets
        // them back together, which is the state the owner's report was about.
        //
        // ON RESUME, NOT IN Application.onCreate: the entry that launched the process calls
        // finish() as soon as it has handed off, and by onResume that transient entry task is
        // gone, so disabling entries here cannot take a window with it - which is exactly the
        // property the hand-off exists to guarantee. Doing it one step earlier, while the
        // entry was still the thing on screen, would close the launch it is trying to serve.
        if (!launcherIconRepaired) {
            launcherIconRepaired = true
            applyAppIconChange(viewModel.themeOption.value)
        }
    }

    private fun handleIntent(intent: Intent?) {
        // THE THEME THE LAUNCHER ENTRY RAN UNDER, handed over as an extra - the same "sync the
        // theme preference with what was launched" the class-name map used to do, which can no
        // longer work: every theme entry hands off to this activity now (see
        // [ThemeLauncherEntry]), so this class's own name identifies no theme at all.
        // In [handleIntent] rather than in onCreate so a tap on the icon while the app is
        // already running lands here too, through onNewIntent.
        intent?.getStringExtra(ThemeLauncherEntry.EXTRA_THEME_KEY)?.let { theme ->
            viewModel.updateThemeOptionSilent(theme)
            intent.removeExtra(ThemeLauncherEntry.EXTRA_THEME_KEY)
        }
        intent?.getStringExtra("TARGET_SCREEN")?.let {
            _navTarget.value = it
            intent.removeExtra("TARGET_SCREEN")
        }
        if (intent?.getBooleanExtra("START_UPDATE_FLOW", false) == true) {
            _startUpdateFlow.value = true
            intent.removeExtra("START_UPDATE_FLOW")
        }
    }

    fun clearNavTarget() {
        _navTarget.value = null
    }

    fun clearStartUpdateFlow() {
        _startUpdateFlow.value = false
    }
}

@Composable
fun MainAppStructure(viewModel: WorkoutViewModel) {
    val isFirstTimeUser by viewModel.isFirstTimeUser.collectAsState()
    // Collected here as well as in the composition root, which owns the
    // setApplicationLocales effect: this one only feeds the walkthrough's language
    // button, so the button's label follows the choice immediately.
    val appLanguage by viewModel.appLanguage.collectAsState()
    val themeOption by viewModel.themeOption.collectAsState()
    val isDeveloper = themeOption.uppercase() == "DEVELOPER"
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // NOTE: the window size is read INSIDE WorkoutTrackerTheme, below. Read here it
    // was the composition local's DEFAULT value - widthDp 420, isNarrow false,
    // isLargeText false, navBarHeight 76dp - because the provider that computes it
    // lives inside the theme. So the whole app SHELL (the bottom navigation bar) was
    // laid out for a window that does not exist: measured on the 1.8x emulator, its
    // four labels still tried to share 1080px at the default narrow=false size and
    // truncated to "My W... | Analyt... | Settin..." no matter what the window said.
    val motivationalNudges by viewModel.motivationalNudges.collectAsState()

    // The skip nudge is delivered by a daily background alarm (only while the app
    // is closed and has been unused for days). Opening the app records the visit,
    // clears any waiting nudge and re-arms/removes the alarm with the setting.
    LaunchedEffect(motivationalNudges) {
        com.grafizum.repio.util.MotivationNudger.onAppOpened(context)
    }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showInAppRestNotification by remember { mutableStateOf(false) }
    // The app-level top banner (locked-field refusals). Null = nothing showing.
    var topBannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        com.grafizum.repio.util.AppEventBus.topBanner.collect { message ->
            topBannerMessage = message
        }
    }
    // Auto-dismiss. Keyed on the message so two refusals in a row restart the
    // clock rather than letting the second inherit the first one's remaining
    // time and vanishing while the user is still looking at it.
    LaunchedEffect(topBannerMessage) {
        if (topBannerMessage != null) {
            delay(2000)
            topBannerMessage = null
        }
    }
    // Which timer produced the banner above. False = a rest, true = a timed
    // exercise whose interval ran out.
    var inAppTimerWasExercise by remember { mutableStateOf(false) }

    var easterEggUnlocked by remember { mutableStateOf(false) }
    var navSequence by remember { mutableStateOf(emptyList<Int>()) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val baseNavigationItems = listOf(
        Screen.Home,
        Screen.Workout,
        Screen.Analytics,
        Screen.Settings
    )

    val navigationItems = if (easterEggUnlocked) {
        baseNavigationItems + Screen.EasterEgg
    } else {
        baseNavigationItems
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { navigationItems.size }
    )

    // Auto-scroll to Easter Egg tab when unlocked
    LaunchedEffect(easterEggUnlocked) {
        if (easterEggUnlocked) {
            // Use scrollToPage for "instant" snap as requested
            pagerState.scrollToPage(navigationItems.size - 1)
        }
    }

    // Auto-close logic for Easter Egg tab
    LaunchedEffect(pagerState.currentPage, easterEggUnlocked) {
        if (easterEggUnlocked) {
            val isEasterEggTab = navigationItems.getOrNull(pagerState.currentPage) == Screen.EasterEgg
            if (!isEasterEggTab) {
                // User navigated away from the easter egg tab
                delay(3000)
                // If they are still not on the tab after 3 seconds, lock it
                val stillNotOnTab = navigationItems.getOrNull(pagerState.currentPage) != Screen.EasterEgg
                if (stillNotOnTab) {
                    easterEggUnlocked = false
                    navSequence = emptyList() // Clear sequence when locking
                    // If the current page index is now out of bounds (because the list shrunk),
                    // move to the last available index (Settings)
                    if (pagerState.currentPage >= baseNavigationItems.size) {
                        pagerState.scrollToPage(baseNavigationItems.size - 1)
                    }
                }
            }
        }
    }

    // Auto-select today's workout when navigating to the Workout tab (via click or swipe)
    LaunchedEffect(pagerState.currentPage) {
        val currentTime = System.currentTimeMillis()

        // Timeout check (5 seconds)
        if (currentTime - lastInteractionTime > 5000) {
            navSequence = emptyList()
        }

        if (navigationItems[pagerState.currentPage] == Screen.Workout) {
            viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
        }

        // Track sequence for Settings (index 3) and Analytics (index 2)
        val currentIndex = baseNavigationItems.indexOf(navigationItems[pagerState.currentPage])
        if (currentIndex == 2 || currentIndex == 3) {
            if (navSequence.lastOrNull() != currentIndex) {
                navSequence = (navSequence + currentIndex).takeLast(5)
                lastInteractionTime = currentTime
            }
        } else {
            // Reset if they go to Home or MyWorkout
            navSequence = emptyList()
        }
    }

    // System Back Button Handling
    BackHandler(enabled = !isFirstTimeUser) {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0, animationSpec = tween(500))
            }
        } else {
            showExitConfirmation = true
        }
    }

    // Update current route for AppEventBus
    LaunchedEffect(pagerState.currentPage, navigationItems) {
        val currentRoute = navigationItems.getOrNull(pagerState.currentPage)?.route
        com.grafizum.repio.util.AppEventBus.setCurrentRoute(currentRoute)
    }

    // Listen for rest timer events
    LaunchedEffect(Unit) {
        com.grafizum.repio.util.AppEventBus.restTimerEnded.collect { wasExerciseTimer ->
            // Show custom notification ONLY if not in Workout tab
            val currentRoute = com.grafizum.repio.util.AppEventBus.currentRoute.value
            if (currentRoute != Screen.Workout.route) {
                // The banner says rest or "time is up" depending on which timer
                // ended - the same split the system notification uses.
                showInAppRestNotification = true
                inAppTimerWasExercise = wasExerciseTimer
            }
        }
    }

    // ---------------- Update Nudge (Play In-App Updates, flexible flow) ----------------
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? MainActivity

    // Latest update info Play reported during this session (null = nothing pending).
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    // Set when the user asks to update (snackbar action or notification tap).
    var requestedUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    // True once the user has dealt with the nudge during this session.
    var updateNudgeHandled by remember { mutableStateOf(false) }
    // The in-app "update available" card. Shown instead of a snackbar so the ask
    // is a themed popup the user can clearly accept or decline.
    var showUpdateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Fresh session: clear any stale "update available" notification from the tray.
    LaunchedEffect(Unit) {
        NotificationHelper.cancelUpdateNotification(context)
    }

    val updateAvailableTitle = stringResource(R.string.update_available_title)
    val updateAvailableMessage = stringResource(R.string.update_available_message)
    val updateNowLabel = stringResource(R.string.update_now)
    val updateLaterLabel = stringResource(R.string.update_later)

    // Ask the user, don't act for them: the nudge is a themed in-app card, and the
    // Play flexible flow starts ONLY from that card's own button. Nothing is
    // upgraded automatically. Declining records the same cooldown the old
    // snackbar used, so "not now" is respected for a few days.
    fun dismissUpdateNudge() {
        showUpdateDialog = false
        updateNudgeHandled = true
        AppUpdateChecker.markNudgeDismissed(context)
    }

    // Also leaves a tray notification, for the case where the user backs out of
    // the app before answering the card.
    fun nudgeUpdate() {
        if (updateNudgeHandled) return
        NotificationHelper.showUpdateAvailableNotification(context)
        showUpdateDialog = true
    }

    // Begin the flexible update via the Play dialog when the user taps the snackbar
    // action or opens the app from the "update available" notification.
    LaunchedEffect(requestedUpdateInfo) {
        requestedUpdateInfo?.let { info ->
            NotificationHelper.cancelUpdateNotification(context)
            activity?.let { AppUpdateChecker.startFlexibleUpdate(it, info) }
            requestedUpdateInfo = null
        }
    }

    // Check Play whenever the app is foregrounded (also covers returning after the
    // user backed out of the Play dialog, which Play marks as CANCELED). Skips
    // silently when offline, within the cooldown, or when no update exists.
    //
    // repeatOnLifecycle rather than an ON_RESUME observer that launches into the
    // composition's scope: that scope is cancelled whenever the composition restarts
    // (the splash screen hands over to the app), which silently threw the check away on
    // the very launch that mattered. This re-runs on every resume instead, and is only
    // cancelled when the Activity goes away.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val info = AppUpdateChecker.checkForUpdate(context) ?: return@repeatOnLifecycle
            pendingUpdateInfo = info
            nudgeUpdate()
        }
    }

    // Opened via the "update available" notification -> go straight to the Play dialog.
    val startUpdateRequested by (activity?.startUpdateFlowOnResume?.collectAsState()
        ?: remember { mutableStateOf(false) })
    LaunchedEffect(startUpdateRequested) {
        if (startUpdateRequested) {
            activity?.clearStartUpdateFlow()
            AppUpdateChecker.checkForUpdate(context, ignoreCooldown = true)?.let { info ->
                updateNudgeHandled = true
                pendingUpdateInfo = info
                requestedUpdateInfo = info
            }
        }
    }

    // Re-nudge later in the same session as soon as connectivity returns,
    // if Play already told us an update was waiting.
    DisposableEffect(lifecycleOwner) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Only surface the nudge once per session; Play is re-queried on next open.
                if (AppUpdateChecker.isWithinCooldown(context)) return
                if (pendingUpdateInfo != null) return
                if (snackbarHostState.currentSnackbarData != null) return

                coroutineScope.launch {
                    val info = AppUpdateChecker.checkForUpdate(context) ?: return@launch
                    pendingUpdateInfo = info
                    nudgeUpdate()
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Connectivity nudging is best-effort only.
        }

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
                // Callback may already be unregistered.
            }
        }
    }

    // ------------------------------------------------------------------------------

    if (showExitConfirmation) {
        val activityContext = context.findActivity()
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(stringResource(R.string.exit_app_title))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(stringResource(R.string.exit_app_message))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        activityContext?.finish()
                    }
                ) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        showExitConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.stay))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Observe lifecycle events to check rest timer
    val navTarget by activity?.navTarget?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(navTarget) {
        navTarget?.let { route ->
            val pageIndex = navigationItems.indexOfFirst { it.route == route }
            if (pageIndex != -1) {
                pagerState.animateScrollToPage(pageIndex, animationSpec = tween(500))
            } else {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            activity?.clearNavTarget()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    viewModel.tickTimer()
                    // Keep the background timer armed: it is what counts the rest down and
                    // alerts when the app is closed.
                    viewModel.ensureRestTimerRunning()
                    viewModel.refreshTodaySelection()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.checkTimerOnAppClose()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Force Onboarding screen if it's the first time
    WorkoutTrackerTheme(themeOption = themeOption, pageIndex = if (isFirstTimeUser) -1 else pagerState.currentPage) {
        // Inside the theme, where LocalWindowSize is actually provided.
        val window = LocalWindowSize.current
        var showAppSplash by remember { mutableStateOf(true) }
        // Observed as state rather than read once through a plain accessor: the
        // dismissal has to recompose this branch away, which only happens when
        // the flag is collected. Reading it as a value left the walkthrough on
        // screen forever, because nothing else changed after the tap.
        val showLevelGuide by viewModel.shouldShowLevelGuide.collectAsState()
        // Collected, not read once: the splash has to recompose the moment the plan is
        // actually on screen. Owner, 2026-10-27: load in as soon as it is ready, with a
        // 0.2 s floor and no fixed wait - see AppSplashScreen.
        val startupReady by viewModel.startupReady.collectAsState()

        if (showAppSplash) {
            AppSplashScreen(ready = startupReady, onComplete = { showAppSplash = false })
        } else if (showLevelGuide) {
            // One-time level-guide gate, shown once per install and before the
            // welcome setup: it announces the level system, then dismissing it
            // hands the user straight to the workout chooser. Its description
            // of the Easy/Medium/Hard choice is written in the future tense,
            // since that choice is still ahead of them on a fresh install.
            // Neither this gate nor the setup below is inside the Scaffold, so neither
            // inherits Scaffold's system-bar insets; each reserves them itself from the
            // theme's [LocalSafeArea]. See that token for why a 16dp page margin was not
            // enough on the Pixel 7a and looked fine on the S22+.
            LevelGuideOnboardingScreen(
                onDismiss = { viewModel.markLevelGuideSeen() },
                // Wired to the same preference as Settings ▸ Language. The
                // setApplicationLocales effect above reacts to it, so a pick here
                // re-renders this very screen in the chosen language.
                appLanguage = appLanguage,
                onLanguageSelected = { viewModel.setAppLanguage(it) }
            )
        } else if (isFirstTimeUser) {
            // Adaptive shell: caps content width and centers it on tablets/foldables.
            AdaptiveWidthContainer {
                FirstTimeSetupScreen(
                    viewModel = viewModel,
                    onComplete = {
                        // ViewModel updates isFirstTimeUser, which triggers recomposition.
                        // The level guide then appears on the next recomposition with
                        // the chosen intensity already baked into every start level.
                    },
                    onCancel = {
                        viewModel.cancelFirstTimeSetup()
                        viewModel.setRequestedWorkoutSubTab(1) // Set sub-tab to "Workout"
                        coroutineScope.launch {
                            pagerState.scrollToPage(1) // Set main pager to "My Workout"
                        }
                    }
                )
            }
        } else {
            val focusManager = LocalFocusManager.current

            // Update-available card. Rendered here (not as a snackbar) so it uses
            // the same themed popup shell as every other dialog in the app, and
            // "Update" is the ONLY path that starts the Play flow.
            if (showUpdateDialog && pendingUpdateInfo != null) {
                AppDialog(
                    onDismissRequest = { dismissUpdateNudge() },
                    title = updateAvailableTitle,
                    confirmLabel = updateNowLabel,
                    onConfirm = {
                        dismissUpdateNudge()
                        requestedUpdateInfo = pendingUpdateInfo
                    },
                    dismissLabel = updateLaterLabel,
                    onDismiss = { dismissUpdateNudge() }
                ) {
                    Text(
                        text = updateAvailableMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Scaffold(
                bottomBar = {
                    Surface(
                        // ZERO, and it has to be: the bar now IS the canvas on a dark scheme, and
                        // a tonal lift on top of it would re-derive a lighter grey - putting the
                        // very seam this pass removes right back. It also makes the rendered bar
                        // the value [navBarFill] returns, so what DarkThemeContrastTest measures
                        // is what the phone draws.
                        tonalElevation = 0.dp,
                        color = navBarFill(MaterialTheme.colorScheme, LocalLightTheme.current),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tabWidth = this.maxWidth / navigationItems.size
                            val density = LocalDensity.current

                            // THE TOKEN IS THE BAR'S OWN CONTENT HEIGHT, so the system's
                            // gesture inset is added ON TOP of it rather than absorbed by
                            // it.
                            //
                            // `NavigationBar` applies `NavigationBarDefaults.windowInsets`
                            // (= the navigation bars, bottom side) as padding INSIDE
                            // whatever height it is handed. Forcing it to
                            // `window.navBarHeight`, which this bar did, therefore did not
                            // produce a bar of that height: it produced one whose content
                            // area was the token MINUS the gesture inset - about 24dp on
                            // both test phones, i.e. a quarter of the token silently
                            // spent on system chrome.
                            //
                            // That is invisible until the bar gets slim enough that
                            // Material's own 48dp item no longer fits in what is left,
                            // and then it shows up as the bar's icons riding up over the
                            // content above them. Measured on the emulator by sweeping the
                            // window width, the item row finished 4.2dp short of its bar
                            // at 455dp, 4.2dp at 411dp, 1.7dp at 376dp, and then 1.3dp
                            // and 3.9dp PAST the top of the bar at 346dp and 320dp - on
                            // the last of which the final exercise row was drawn behind
                            // the bar. Adding the inset back makes the token mean what it
                            // says - the room the icons and labels actually get - on every
                            // phone regardless of its gesture bar, and on a 3-button
                            // navigation device (inset 0) the bar is exactly the token.
                            val navBarContentHeight = window.navBarHeight
                            val navBarHeight = navBarContentHeight + with(density) {
                                WindowInsets.navigationBars.getBottom(this).toDp()
                            }

                            // Sliding Indicator Background. Sized to the CONTENT height and
                            // drawn from the top, which is exactly the row the items occupy
                            // inside the bar below it.
                            Surface(
                                modifier = Modifier
                                    .width(tabWidth)
                                    // The bar's own height, from the same token the bar
                                    // itself uses: a hard-coded 80dp held a 2-line 1.8x
                                    // label badly (the icon was drawn over the text).
                                    .height(navBarContentHeight)
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                        translationX = with(density) { (pageOffset * tabWidth.toPx()) }
                                    },
                                color = navIndicatorFill(
                                    MaterialTheme.colorScheme,
                                    LocalLightTheme.current
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {}

                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp, // Elevation is handled by the parent Surface
                                // Sized by the token plus the gesture inset, never by
                                // Material's default: at a large font scale the bar carries
                                // a label, so it has to be able to hold it
                                // (window.navBarHeight rides the font only once isLargeText
                                // is true).
                                modifier = Modifier.height(navBarHeight)
                            ) {
                                navigationItems.forEachIndexed { index, screen ->
                                    val isSelected = pagerState.targetPage == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            val currentTime = System.currentTimeMillis()

                                            if (screen == Screen.Settings) {
                                                // Easter egg trigger: Settings (3) -> Analytics (2) -> Settings (3) -> Analytics (2) -> Settings (3)
                                                // and then press Settings button
                                                val isCorrectSequence = navSequence == listOf(3, 2, 3, 2, 3) || 
                                                                       (navSequence.size == 4 && navSequence == listOf(3, 2, 3, 2) && pagerState.currentPage == 3)
                                                
                                                if (currentTime - lastInteractionTime <= 5000 && isCorrectSequence) {
                                                    easterEggUnlocked = true
                                                }
                                            }

                                            if (screen == Screen.Workout) {
                                                viewModel.setRequestedWorkoutSubTab(0) // Default to "Workout Now" on tab click
                                                viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
                                            }
                                            if (pagerState.currentPage != index) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index, animationSpec = tween(500))
                                                }
                                            }
                                        },
                                        icon = {
                                            if (screen.icon != null) {
                                                Icon(
                                                    imageVector = screen.icon!!,
                                                    contentDescription = screen.title
                                                )
                                            } else if (screen.drawableRes != null) {
                                                Icon(
                                                    painter = painterResource(screen.drawableRes!!),
                                                    contentDescription = screen.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        },
                                        // AT A LARGE TEXT SCALE THE BAR IS ICONS ONLY.
                                        //
                                        // Measured on the 1.8x emulator, twice. First
                                        // four labels at labelSmall want ~250px each
                                        // while the bar gives ~218px, so the strip read
                                        // "Home | My W... | Analyt... | Settin..." - the
                                        // owner's "the interface is still thin". Then,
                                        // with only the selected tab labelled, the dump
                                        // showed the label box at y 2186-2323 against the
                                        // icon band at 2122-2235: NavigationBarItem
                                        // places the label in a fixed slot sized for a
                                         // font-1.0 line, so a 1.8x label is painted
                                        // straight over its own icon (the owner's "My
                                        // Workout" badge collision). A slot inside
                                        // Material's item cannot be re-laid-out from
                                        // here, so at this size the bar carries the
                                        // ICONS and their content descriptions only -
                                        // no truncated label, no overlap, and the screen
                                        // each tab opens still names itself in its header.
                                        label = if (window.isLargeText) {
                                            null
                                        } else {
                                        {
                                            val labelKey = when(screen.route) {
                                                "home" -> "smart_workout_assistant"
                                                "workout" -> "program_name"
                                                "analytics" -> "analytics_reports"
                                                "settings" -> "settings"
                                                else -> null
                                            }
                                            Text(
                                                text = if (labelKey != null) getString(labelKey) else screen.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                // Narrow phones only have ~90dp per tab, so the label steps
                                                // down one size to show more of "Smart Workout Assistant"
                                                // instead of collapsing to "Smart Wo…".
                                                style = if (window.isNarrow) {
                                                    MaterialTheme.typography.labelSmall
                                                } else {
                                                    MaterialTheme.typography.labelMedium
                                                }
                                            )
                                        }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .padding(vertical = 0.dp)
                                            .semantics { testTag = "nav_tab_${screen.route}" }
                                    )
                                }
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    AdaptiveWidthContainer {
                        when (navigationItems[pageIndex]) {
                        is Screen.Home -> {
                            val navigateToTab: (String) -> Unit = { route ->
                                val targetIndex = navigationItems.indexOfFirst { it.route == route }
                                if (targetIndex != -1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetIndex, animationSpec = tween(500))
                                    }
                                }
                            }

                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToWorkoutNow = { 
                                    viewModel.setRequestedWorkoutSubTab(0)
                                    navigateToTab(Screen.Workout.route) 
                                }
                            )
                        }

                        is Screen.Workout -> {
                            WorkoutTabScreen(
                                viewModel = viewModel,
                                isActive = pagerState.settledPage == pageIndex,
                                onBack = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0, animationSpec = tween(500))
                                    }
                                }
                            )
                        }

                        is Screen.Analytics -> {
                            AnalyticsScreen(
                                viewModel = viewModel
                            )
                        }

                        is Screen.Settings -> {
                            SettingsScreen(viewModel = viewModel)
                        }

                        is Screen.EasterEgg -> {
                            EasterEggScreen(viewModel = viewModel)
                        }

                        else -> {}
                    }
                    } // AdaptiveWidthContainer
                }

                // Custom In-App Rest Timer Notification
                AnimatedVisibility(
                    visible = showInAppRestNotification,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeOut(animationSpec = tween(durationMillis = 500)),
                    modifier = Modifier
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .align(androidx.compose.ui.Alignment.TopCenter)
                ) {
                    Surface(
                        onClick = {
                            showInAppRestNotification = false
                            coroutineScope.launch {
                                val workoutIndex = navigationItems.indexOfFirst { it.route == Screen.Workout.route }
                                if (workoutIndex != -1) {
                                    pagerState.animateScrollToPage(workoutIndex, animationSpec = tween(500))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                            
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(
                                    if (inAppTimerWasExercise) R.string.notification_exercise_title
                                    else R.string.notification_title
                                ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(
                                        if (inAppTimerWasExercise) R.string.notification_exercise_text
                                        else R.string.in_app_rest_ended_message
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Auto-hide after 2.5 seconds
                    LaunchedEffect(showInAppRestNotification) {
                        if (showInAppRestNotification) {
                            delay(2500)
                            showInAppRestNotification = false
                        }
                    }
                }

                // APP-LEVEL TOP BANNER — the last thing drawn, so it sits ABOVE
                // a dialog and above the tab content both.
                //
                // This is the "show it at the top with a swift snappy drop in
                // and drop out" the owner asked for the "switch to custom"
                // refusal. It lives here rather than inside the exercise editor
                // because a dialog cannot draw outside its own bounds, so a
                // notice rendered in there scrolls with the form and can end up
                // off-screen at the moment the user taps a locked field.
                AnimatedVisibility(
                    visible = topBannerMessage != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 220)
                    ) + fadeIn(animationSpec = tween(durationMillis = 160)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 180)
                    ) + fadeOut(animationSpec = tween(durationMillis = 180)),
                    modifier = Modifier
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .align(androidx.compose.ui.Alignment.TopCenter)
                ) {
                    Surface(
                        onClick = { topBannerMessage = null },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        // A refusal, not a note: it keeps the red rim so it does
                        // not read as an informational banner.
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth(0.86f)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
}

