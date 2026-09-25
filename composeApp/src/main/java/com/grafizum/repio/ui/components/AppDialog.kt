package com.grafizum.repio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The one popup surface every dialog in the app renders through.
 *
 * The owner asked for the popup cards to stop mixing background colours and
 * sizes ("Make the popup cards ALLA the same design"). The app had drifted into
 * two dialects: Material's `AlertDialog` (its own container colour, a 28dp
 * radius, its own button row) for the add/edit/filter dialogs, and a hand-rolled
 * full-height `Surface` for the replace dialog. Two dialects means a form popup
 * and a filter popup never match on the same phone, and every future dialog
 * picks one of them by accident.
 *
 * So there is one shell now:
 *
 *  - the same container colour, corner radius, width cap and padding for all of
 *    them, read from [WindowSize] rather than measured per screen;
 *  - a title that stays put, a body that scrolls when the keyboard or a long
 *    form needs the room, and a button row pinned to the bottom - the structure
 *    `AlertDialog` gave for free and the hand-rolled dialog had to reinvent;
 *  - an optional [heightFraction] for the dialogs that genuinely want the whole
 *    window (the replace dialog's exercise list).
 *
 * Deliberately NOT an `AlertDialog`: its text slot sizes itself to its content,
 * which is what left the replace dialog with a one-card list on a short phone.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    /** Share of the window height this popup may take; null = wrap its content. */
    heightFraction: Float? = null,
    /**
     * Whether the shell scrolls the body. False for a body that brings its own
     * scroller (a lazy list, which cannot live inside a scrolling column - it
     * would be measured with unbounded height); the body then simply takes the
     * room the title and buttons leave.
     */
    scrollableBody: Boolean = true,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    /** A form's save button is disabled until its required fields are filled. */
    confirmEnabled: Boolean = true,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    /**
     * Tapping the title clears the keyboard. The form dialogs have always done
     * this (a keyboard covering half the form with no other way to dismiss it),
     * so the shell offers it rather than each caller wrapping the title again.
     */
    onTitleTap: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val window = LocalWindowSize.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(window.dialogCorner),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = window.dialogMaxWidth)
                    .padding(horizontal = window.screenPadding, vertical = window.sectionSpacing)
                    .then(
                        if (heightFraction != null) Modifier.fillMaxHeight(heightFraction)
                        else Modifier.heightIn(max = window.windowHeightDp.dp * 0.94f)
                    )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(window.dialogGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(window.cardPadding)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (onTitleTap == null) Modifier
                            else Modifier.pointerInput(onTitleTap) {
                                detectTapGestures(onTap = { onTitleTap() })
                            }
                        )
                    }

                    // The body takes the room the title and the buttons leave, and
                    // scrolls inside it: a long form on a short window must not push
                    // its own confirm button off the screen.
                    Column(
                        verticalArrangement = Arrangement.spacedBy(window.dialogGap),
                        modifier = Modifier
                            .weight(1f, fill = !scrollableBody)
                            .then(
                                if (scrollableBody) Modifier.verticalScroll(rememberScrollState())
                                else Modifier
                            )
                    ) {
                        content()
                    }

                    if (confirmLabel != null || dismissLabel != null) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (dismissLabel != null && onDismiss != null) {
                                TextButton(onClick = onDismiss) { Text(dismissLabel) }
                            }
                            if (confirmLabel != null && onConfirm != null) {
                                Button(onClick = onConfirm, enabled = confirmEnabled) {
                                    Text(confirmLabel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
