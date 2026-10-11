package com.grafizum.repio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.ExerciseSearch
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.util.getString

/**
 * The exercise the user picked in [ExercisePickerBody].
 *
 * [muscleGroup] is the exercise's own PRECISE muscle, resolved here so both hosts
 * store the same thing: the filter is a broad body part, but the row a host saves
 * keeps the precise label its badge and the Analytics split read.
 */
data class ExercisePick(val name: String, val muscleGroup: String)

/**
 * The exercises the picker's filters allow, in popularity order.
 *
 * EXTRACTED FROM THE UI SO IT CAN BE TESTED WITHOUT A SCREEN. The pool rules are
 * the ones that have bitten this project repeatedly — "the filter doesn't work"
 * was always a pool question, never a drawing question — and a rule that lives
 * inside a composable can only be checked by looking at it.
 *
 * [equipmentCategories] is the user's kit (coarse categories, from setup);
 * [grantedTags] are the fine tags when the host has them — see
 * `pickerGrantedTags`, which prefers the fine tags because the coarse list has no
 * category for a chair, a bench or a pull-up bar and re-expanding it would drop
 * grants the user actually ticked. Null means unrestricted.
 *
 * [selectedEquipment] empty = every category; [selectedMuscleGroup] null or blank
 * = any muscle, and may be either a broad body part or a precise muscle because
 * `getExercisesForEquipment` answers both.
 */
internal fun exercisePickerPool(
    equipmentCategories: List<String>,
    grantedTags: Set<String>?,
    selectedEquipment: List<String>,
    selectedMuscleGroup: String?,
    excludedName: String? = null,
): List<String> {
    val equipmentFilter = selectedEquipment.ifEmpty { WorkoutTemplates.allEquipmentCategories }
    var pool = if (selectedMuscleGroup.isNullOrBlank()) {
        equipmentFilter.flatMap { WorkoutTemplates.getAllExercisesForEquipment(it) }
    } else {
        equipmentFilter.flatMap { WorkoutTemplates.getExercisesForEquipment(it, selectedMuscleGroup) }
    }
    // The fine-tag gate, the same one the week generator applies: the coarse
    // "Bodyweight" chip covers every bar-less movement, so without it a
    // dumbbells-and-chair kit is offered pull-ups and muscle-ups.
    val granted = WorkoutTemplates.pickerGrantedTags(grantedTags, equipmentCategories)
    if (granted != null) pool = pool.filter { ExerciseEquipmentTags.isAvailable(it, granted) }
    return ExercisePopularity.sortedByPopularity(
        pool.distinct().filter { excludedName == null || !it.equals(excludedName, ignoreCase = true) }
    )
}

/**
 * The equipment chips the picker opens with: the kit the user declared, plus "Other".
 *
 * [equipmentCategories] is what setup stored (see `userEquipmentCategories`), which is the
 * owner's *"the equipment icons which the user selected when creating the exercise week"*.
 *
 * "Other" RIDES ALONG AND IS NOT OPTIONAL. The catalogue files a plank, a hollow hold, every
 * stretch and pose - and cardio like a walk - under `Other`, because none of them needs kit at
 * all. It is therefore not equipment the user can be said to lack, and treating it as a chip
 * they did not tick made the picker unable to offer a Plank to anyone who had ever answered the
 * equipment question: 115 of the catalogue's movements, hidden by a filter the user never
 * chose. A user who declared nothing still gets every category, which the pool then narrows
 * by what they own.
 *
 * Order follows the catalogue's, so the chip row reads the same as the setup grid.
 */
internal fun defaultEquipmentFilter(equipmentCategories: List<String>): List<String> {
    val kit = equipmentCategories.filter { it in WorkoutTemplates.allEquipmentCategories }
    if (kit.isEmpty()) return WorkoutTemplates.allEquipmentCategories
    val wanted = kit.toSet() + WorkoutTemplates.EQUIPMENT_OTHER
    return WorkoutTemplates.allEquipmentCategories.filter { it in wanted }
}

/**
 * The share of the window the picker's list box may take before it scrolls inside itself.
 *
 * A NUMBER, BECAUSE THE FAILURE WAS GEOMETRIC. The list used to ask its parent for "whatever you
 * have left" (`Modifier.weight`), and both hosts draw the picker inside `AppDialog`'s DEFAULT,
 * scrolling body - where there IS no leftover height to divide, so the lazy list was handed an
 * unbounded height and laid every row out at 0px. Measured: the rows came back with a height of
 * `0.0px` inside a scrolling body while the same component measured fine in a bounded one. The
 * owner saw the result on the phone - *"not just a label but actually exercises i can select
 * from"* - and a count line with nothing under it is exactly that measurement.
 *
 * So the box now states its own height: `heightIn` caps it at this share of the window (and floors
 * it at [PICKER_LIST_MIN_HEIGHT]), which is bounded information the list can lay out against 
 * whether the parent scrolls or not. Short pools still wrap their content and make a short box.
 */
private const val PICKER_LIST_MAX_WINDOW_SHARE = 0.40f

/** The shortest the list box may get, so it always reads as a list rather than a sliver. */
private val PICKER_LIST_MIN_HEIGHT = 200.dp

/**
 * One result of the picker, drawn as GRAPHICS rather than prose.
 *
 * The owner's ruling, 2026-10-11: *"i want the whole card to have graphical
 * informations rather than bloat text"*. So the row is the exercise's equipment
 * glyph, its name, and its muscle badge — and deliberately NOT the two-line method
 * text the replace dialog's cards carry, which is most of what made those cards
 * read as a wall of words. The method text still exists; it belongs on the level
 * card and the exercise editor, where the user has already chosen the movement.
 */
@Composable
fun ExerciseResultCard(
    name: String,
    modifier: Modifier = Modifier,
    /** True for the exercise the host is currently about to add: tinted, not just tapped. */
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val category = WorkoutTemplates.getEquipmentForExercise(name)
    // The precise muscle, not the broad part the filter works in: the badge is the
    // row's only written information, and "Front Deltoids" is worth more there than
    // "Shoulders".
    val precise = resolvePreciseMuscleGroup(name, WorkoutTemplates.getMuscleGroupForExercise(name))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category.isNotBlank()) {
                // The label moves to the content description, where it costs no
                // width — the same trade the collapsed equipment row makes.
                EquipmentIconImage(
                    icon = equipmentCategoryIcon(category),
                    contentDescription = getString(WorkoutTemplates.equipmentLabelKey(category)),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = translateExerciseName(name),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MuscleGroupBadge(muscleGroup = precise, exerciseName = name)
        }
    }
}

/**
 * The exercise picker both ADD-exercise cards draw when the level system is on:
 * a search field, the two filters, and a list of [ExerciseResultCard]s.
 *
 * WHY IT IS SHARED. There are two places a new exercise is created — the routine
 * editor and the welcome/setup form — and they are separate dialogs, which is
 * exactly how one of them ended up without the level UI at all (owner report,
 * 2026-10-09). The replace-exercise picker already had this shape and the add
 * cards did not; rather than build it twice, the body lives here once and both
 * hosts draw it. Custom mode keeps the classic input boxes and never sees this.
 *
 * THE TWO HALVES OF THE OWNER'S RULE (2026-10-11): *"i want to have exercise name
 * that the user can input to search an exercise and under it filter options for
 * the searched exercise, but if the user only uses the filters and doesnt search
 * by name show al the possible exercises based on those filters"*. Both halves are
 * one list: the search narrows whatever the filters allow, and a blank query
 * leaves the popularity-ordered pool untouched. *"show the equipment filter
 * minimized with the equipment icons which the user selected when creating the
 * exercise week at first"* is the collapsed row's initial value — the kit itself,
 * editable one tap away rather than a cage.
 */
@Composable
fun ExercisePickerBody(
    equipmentCategories: List<String>,
    grantedTags: Set<String>?,
    onPick: (ExercisePick) -> Unit,
    modifier: Modifier = Modifier,
    /** An exercise to keep out of the list (the row being replaced), if any. */
    excludedName: String? = null,
    /** The exercise already picked, drawn tinted so a change of mind is visible. */
    selectedName: String? = null,
    /** The catalogue the search may range over; defaults to the pool's own names. */
    allAvailableExercises: List<String> = emptyList()
) {
    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current

    // The kit as the starting filter — the owner's "the equipment the user selected
    // when creating the exercise week", and see [defaultEquipmentFilter] for why "Other"
    // comes with it. Every transition below compares against this same value, so
    // "has the user changed the filter?" and "what does Reset go back to?" cannot drift.
    val startingFilter = remember(equipmentCategories) { defaultEquipmentFilter(equipmentCategories) }
    var selectedEquipment by remember(startingFilter) { mutableStateOf(startingFilter) }
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val pool = remember(equipmentCategories, grantedTags, selectedEquipment, selectedMuscleGroup, excludedName) {
        exercisePickerPool(
            equipmentCategories = equipmentCategories,
            grantedTags = grantedTags,
            selectedEquipment = selectedEquipment,
            selectedMuscleGroup = selectedMuscleGroup,
            excludedName = excludedName
        )
    }

    // Search over the filtered pool, with the same typo-tolerant matcher the rest of
    // the app uses — it matches canonical AND translated names, so a German user
    // typing "kniebeuge" finds Barbell Back Squat. The per-name normalisation is
    // query-independent, hence the prepared index rather than work per keystroke.
    val searchCandidates = remember(pool) {
        val names = allAvailableExercises.ifEmpty { pool }
        names.map { name -> ExerciseSearch.Candidate(name, ExerciseLocalization.allNamesFor(name)) }
    }
    val searchIndex = remember(searchCandidates) { ExerciseSearch.PreparedIndex.build(searchCandidates) }
    val visible = remember(searchQuery, searchIndex, pool) {
        if (searchQuery.isBlank()) pool
        else {
            val poolKeys = pool.map { it.lowercase() }.toHashSet()
            ExerciseSearch.searchPrepared(searchIndex, searchQuery)
                .map { it.canonical }
                .filter { it.lowercase() in poolKeys }
                .distinct()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = getString("exercise_name_placeholder"),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = getString("close")
                        )
                    }
                }
            },
            // Tagged because the field IS the first half of the owner's rule, and a test
            // that typed into "whichever node says e.g. Bench Press" would be testing the
            // placeholder rather than the search.
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "exercise_picker_search" }
        )

        EquipmentFilterCollapsible(
            selectedEquipment = selectedEquipment,
            onToggle = { key ->
                selectedEquipment =
                    if (selectedEquipment.contains(key)) selectedEquipment - key
                    else selectedEquipment + key
            }
        )

        MuscleGroupFilterCollapsible(
            selectedMuscleGroup = selectedMuscleGroup,
            onSelect = { selectedMuscleGroup = it }
        )

        // THE COUNT LINE IS GONE. Owner, 2026-10-11: *"remove ... the 'exercise (34)' label i want
        // it to actually show all of those exercises in a scroll box"*. It was the third thing in a
        // row that already said what was filtering (the two collapse rows above), and the number it
        // printed was the one piece of information the list below it states far better by simply
        // being there. Reset survives - a way back out of a filter is a control, not a caption - and
        // the row itself is only drawn when there is something to reset, so an untouched card is
        // search, filters, and exercises.
        if (searchQuery.isNotBlank() || selectedMuscleGroup != null ||
            selectedEquipment != startingFilter
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedMuscleGroup = null
                        selectedEquipment = startingFilter
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(getString("reset"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Resolved OUTSIDE the `semantics {}` block below: that lambda is not a composable scope, so
        // a string lookup cannot happen inside it.
        val listDescription = getString("exercises_count", visible.size)

        if (visible.isEmpty()) {
            Text(
                text = getString("no_exercises_for_filters"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                // A BOX WITH A HEIGHT OF ITS OWN - see [PICKER_LIST_MAX_WINDOW_SHARE]. Asking the
                // parent for "the rest" is what made the rows 0px tall in the one place this card
                // is ever drawn.
                //
                // The count did not disappear, it moved HERE: how many movements the list holds is
                // real information, and a screen reader is the one reader that cannot see how far
                // the list goes. It is spoken, never drawn, which is what the owner asked for - see
                // the note where the caption used to be.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = PICKER_LIST_MIN_HEIGHT,
                        max = (window.windowHeightDp * PICKER_LIST_MAX_WINDOW_SHARE).dp
                    )
                    .semantics {
                        testTag = "exercise_picker_list"
                        contentDescription = listDescription
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible) { name ->
                    ExerciseResultCard(
                        name = name,
                        isSelected = selectedName != null && name.equals(selectedName, ignoreCase = true)
                    ) {
                        focusManager.clearFocus()
                        onPick(
                            ExercisePick(
                                name = name,
                                muscleGroup = resolvePreciseMuscleGroup(
                                    name,
                                    WorkoutTemplates.getMuscleGroupForExercise(name)
                                ).ifBlank { WorkoutTemplates.getMuscleGroupForExercise(name) }
                            )
                        )
                    }
                }
            }
        }
    }
}
