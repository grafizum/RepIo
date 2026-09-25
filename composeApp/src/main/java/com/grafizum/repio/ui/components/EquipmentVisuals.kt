package com.grafizum.repio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DryCleaning
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.grafizum.repio.R
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates

/**
 * The visual vocabulary of the equipment filter.
 *
 * The fine-grained tag list used to be sixteen checkbox sentences the user had
 * to read before they could act. It is now a tile grid: one glanceable icon per
 * tag with a SHORT label underneath, so the setup reads like choosing from a
 * wall of equipment, not filling in a form. The icon carries the shape
 * ("chair", "stairs", "rings"); the one-or-two-word label carries the precision
 * the icon cannot ("chair OR bench").
 *
 * Generic material symbols cannot draw gym hardware — a bench is not a sofa
 * (`Weekend`), a rack is not moving boxes (`Storage`), a machine is not a robot
 * arm (`PrecisionManufacturing`). The nine shapes that matter most are custom
 * vector drawables (ic_eq_*) drawn for exactly the equipment they name; only
 * the shapes material genuinely covers (dumbbell, bands, stairs, wall, bag,
 * towel) still use stock icons.
 *
 * The coarse picker chips (replace/add dialogs) share the same mapping, so
 * "Barbell" means the same drawing everywhere equipment appears.
 */
sealed class EquipmentIcon {
    data class Vector(val imageVector: ImageVector) : EquipmentIcon()
    data class Res(@DrawableRes val id: Int) : EquipmentIcon()
}

/**
 * Renders an [EquipmentIcon] wherever an [Icon] would sit. Drawable-backed
 * icons tint exactly like vector ones (the painter's opaque pixels take the
 * tint), so selection colors work identically on both kinds.
 */
@Composable
fun EquipmentIconImage(
    icon: EquipmentIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    when (icon) {
        is EquipmentIcon.Vector -> Icon(icon.imageVector, contentDescription, modifier = modifier, tint = tint)
        is EquipmentIcon.Res -> Icon(painterResource(icon.id), contentDescription, modifier = modifier, tint = tint)
    }
}

fun equipmentTagIcon(tag: String): EquipmentIcon = when (tag) {
    ExerciseEquipmentTags.CHAIR -> EquipmentIcon.Res(R.drawable.ic_eq_chair)
    ExerciseEquipmentTags.BENCH -> EquipmentIcon.Res(R.drawable.ic_eq_bench)
    ExerciseEquipmentTags.PULL_UP_BAR -> EquipmentIcon.Res(R.drawable.ic_eq_pullup)
    ExerciseEquipmentTags.DIP_BARS -> EquipmentIcon.Res(R.drawable.ic_eq_dipbars)
    ExerciseEquipmentTags.GYMNASTIC_RINGS -> EquipmentIcon.Res(R.drawable.ic_eq_rings)
    ExerciseEquipmentTags.WALL -> EquipmentIcon.Vector(Icons.Outlined.GridOn)
    ExerciseEquipmentTags.STAIRS -> EquipmentIcon.Vector(Icons.Outlined.Stairs)
    ExerciseEquipmentTags.BACKPACK_OR_WEIGHTED_BAG -> EquipmentIcon.Vector(Icons.Outlined.Backpack)
    ExerciseEquipmentTags.TOWEL -> EquipmentIcon.Vector(Icons.Outlined.DryCleaning)
    ExerciseEquipmentTags.RESISTANCE_BANDS -> EquipmentIcon.Vector(Icons.Outlined.Gesture)
    ExerciseEquipmentTags.DUMBBELLS_ONLY -> EquipmentIcon.Vector(Icons.Outlined.FitnessCenter)
    ExerciseEquipmentTags.DUMBBELLS_AND_BENCH -> EquipmentIcon.Res(R.drawable.ic_eq_bench)
    // Legacy pre-split tag: a persisted "chair or bench" still has to draw
    // something, and the bench is the tile that replaced it.
    ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR -> EquipmentIcon.Vector(Icons.Outlined.FitnessCenter)
    ExerciseEquipmentTags.BARBELL_AND_RACK -> EquipmentIcon.Res(R.drawable.ic_eq_rack)
    ExerciseEquipmentTags.BARBELL_AND_FLOOR -> EquipmentIcon.Res(R.drawable.ic_eq_barbell)
    ExerciseEquipmentTags.KETTLEBELL_ONLY -> EquipmentIcon.Res(R.drawable.ic_eq_kettlebell)
    ExerciseEquipmentTags.CABLE_MACHINE -> EquipmentIcon.Res(R.drawable.ic_eq_cable)
    ExerciseEquipmentTags.MACHINE_OTHER -> EquipmentIcon.Res(R.drawable.ic_eq_machine)
    else -> EquipmentIcon.Vector(Icons.Outlined.Category)
}

/** Icon for a coarse catalog category ("Barbell", "Dumbbell", …) on picker chips. */
fun equipmentCategoryIcon(category: String): EquipmentIcon = when (category) {
    WorkoutTemplates.EQUIPMENT_BODYWEIGHT -> EquipmentIcon.Vector(Icons.Outlined.SelfImprovement)
    WorkoutTemplates.EQUIPMENT_BARBELL -> EquipmentIcon.Res(R.drawable.ic_eq_barbell)
    WorkoutTemplates.EQUIPMENT_DUMBBELL -> EquipmentIcon.Vector(Icons.Outlined.FitnessCenter)
    WorkoutTemplates.EQUIPMENT_KETTLEBELL -> EquipmentIcon.Res(R.drawable.ic_eq_kettlebell)
    WorkoutTemplates.EQUIPMENT_CABLE -> EquipmentIcon.Res(R.drawable.ic_eq_cable)
    WorkoutTemplates.EQUIPMENT_MACHINE -> EquipmentIcon.Res(R.drawable.ic_eq_machine)
    WorkoutTemplates.EQUIPMENT_BANDS -> EquipmentIcon.Vector(Icons.Outlined.Gesture)
    else -> EquipmentIcon.Vector(Icons.Outlined.Category)
}

/**
 * SHORT tile label for a fine tag — deliberately not the checkbox sentences:
 * the icon says what it is, the label only disambiguates ("Chair / Bench").
 */
fun equipmentTagShortLabelKey(tag: String): String = when (tag) {
    ExerciseEquipmentTags.CHAIR -> "equipment_tag_chair"
    ExerciseEquipmentTags.CHAIR_OR_BENCH -> "equipment_tag_bench"
    ExerciseEquipmentTags.PULL_UP_BAR -> "equipment_tag_pullup"
    ExerciseEquipmentTags.DIP_BARS -> "equipment_tag_dips"
    ExerciseEquipmentTags.GYMNASTIC_RINGS -> "equipment_tag_rings"
    ExerciseEquipmentTags.WALL -> "equipment_tag_wall"
    ExerciseEquipmentTags.STAIRS -> "equipment_tag_stairs"
    ExerciseEquipmentTags.BACKPACK_OR_WEIGHTED_BAG -> "equipment_tag_bag"
    ExerciseEquipmentTags.TOWEL -> "equipment_tag_towel"
    ExerciseEquipmentTags.RESISTANCE_BANDS -> "equipment_tag_bands"
    ExerciseEquipmentTags.DUMBBELLS_ONLY -> "equipment_tag_dumbbells"
    ExerciseEquipmentTags.DUMBBELLS_AND_BENCH -> "equipment_tag_db_bench"
    ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR -> "equipment_tag_dumbbells"
    // Label and drawing have to name the SAME thing: the rack tile drew a
    // squat rack under the word "Barbell", and the barbell tile drew a barbell
    // under "Rack / floor", so neither matched its own icon.
    ExerciseEquipmentTags.BARBELL_AND_RACK -> "equipment_tag_rack"
    ExerciseEquipmentTags.BARBELL_AND_FLOOR -> "equipment_tag_barbell"
    ExerciseEquipmentTags.KETTLEBELL_ONLY -> "equipment_tag_kettlebell"
    ExerciseEquipmentTags.CABLE_MACHINE -> "equipment_tag_cable"
    ExerciseEquipmentTags.MACHINE_OTHER -> "equipment_tag_machines"
    else -> "equipment"
}
