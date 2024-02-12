package carbon.compose.dropdown

import androidx.annotation.IntRange
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import carbon.compose.foundation.input.onEnterKeyEvent
import carbon.compose.foundation.interaction.FocusIndication
import carbon.compose.foundation.motion.Motion
import carbon.compose.foundation.text.CarbonTypography
import carbon.compose.foundation.text.Text

private val dropdownOptionHeight = 40.dp

private val dropdownTransitionSpecFloat = tween<Float>(
    durationMillis = Motion.Duration.moderate01,
    easing = Motion.Standard.productiveEasing
)

private val dropdownTransitionSpecDp = tween<Dp>(
    durationMillis = Motion.Duration.moderate01,
    easing = Motion.Standard.productiveEasing
)

private const val CHEVRON_ROTATION_ANGLE = 180f

/**
 * # Dropdown
 *
 * Dropdowns present a list of options from which a user can select one option.
 * A selected option can represent a value in a form, or can be used as an action to filter or sort
 * existing content.
 *
 * Only one option can be selected at a time.
 * - By default, the dropdown displays placeholder text in the field when closed.
 * - Clicking on a closed field opens a menu of options.
 * - Selecting an option from the menu closes it and the selected option text replaces the
 * placeholder text in the field and also remains as an option in place if the menu is open.
 *
 * (From [Dropdown documentation](https://carbondesignsystem.com/components/dropdown/usage/))
 *
 * @param K Type to identify the options.
 * @param expanded Whether the dropdown is expanded or not.
 * @param fieldPlaceholderText The text to be displayed in the field when no option is selected.
 * @param selectedOption The currently selected option. When not null, the option associated with
 * this key will be displayed in the field.
 * @param options The options to be displayed in the dropdown menu. A map signature ensures that the
 * keys are unique and can be used to identify the selected option. The strings associated with each
 * key are the texts to be displayed in the dropdown menu.
 * @param onOptionSelected Callback invoked when an option is selected. The selected option is
 * passed as a parameter, and the callback should be used to update a remembered state with the new
 * value.
 * @param onExpandedChange Callback invoked when the expanded state of the dropdown changes. It
 * should be used to update a remembered state with the new value.
 * @param onDismissRequest Callback invoked when the dropdown menu should be dismissed.
 * @param modifier The modifier to be applied to the dropdown.
 * @param minVisibleItems The minimum number of items to be visible in the dropdown menu before the
 * user needs to scroll. This value is used to calculate the height of the menu. Defaults to 4.
 */
@Composable
public fun <K : Any> Dropdown(
    expanded: Boolean,
    fieldPlaceholderText: String,
    selectedOption: K?,
    options: Map<K, String>,
    onOptionSelected: (K) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @IntRange(from = 1) minVisibleItems: Int = 4
) {
    val interactionSource = remember { MutableInteractionSource() }

    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    val transition = updateTransition(expandedStates, "Dropdown")

    val colors = DropdownColors.colors()

    val chevronRotation by transition.animateFloat(
        transitionSpec = { dropdownTransitionSpecFloat },
        label = "Chevron rotation"
    ) {
        if (it) CHEVRON_ROTATION_ANGLE else 0f
    }

    BoxWithConstraints(
        modifier = modifier
            .height(40.dp)
            .indication(
                interactionSource = interactionSource,
                indication = FocusIndication()
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .focusable(interactionSource = interactionSource)
                .fillMaxHeight()
                .background(colors.fieldBackgroundColor)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Custom pointer input to handle input events on the field.
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val expandStateOnDown = expandedStates.currentState
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)?.let {
                            // Avoid expanding back if the dropdown was expanded on down.
                            if (!expandStateOnDown) {
                                onExpandedChange(!expandedStates.currentState)
                            }
                        }
                    }
                }
                .onEnterKeyEvent {
                    onExpandedChange(!expandedStates.currentState)
                }
                .semantics {
                    onClick {
                        onExpandedChange(!expandedStates.currentState)
                        true
                    }
                }
        ) {
            Text(
                text = options[selectedOption] ?: fieldPlaceholderText,
                style = CarbonTypography.bodyCompact01,
                color = colors.fieldTextColor,
                modifier = Modifier.weight(1f)
            )
            Image(
                imageVector = chevronDownIcon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.chevronIconColor),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .rotate(chevronRotation)
            )
        }
        Spacer(
            modifier = Modifier
                .background(color = colors.fieldBorderColor)
                .height(1.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        // TODO Place popup on top of the field if the menu doesn't have enough space below it.
        if (expandedStates.currentState || expandedStates.targetState) {
            Popup(
                popupPositionProvider = DropdownMenuPositionProvider,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true)
            ) {
                DropdownContent(
                    selectedOption = selectedOption,
                    options = options,
                    visibleItemsBeforeScroll = minVisibleItems,
                    transition = transition,
                    colors = colors,
                    onOptionSelected = { option ->
                        onOptionSelected(option)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .width(maxWidth)
                        .onEscape(onDismissRequest)
                )
            }
        }
    }
}

@Composable
private fun <K : Any> DropdownContent(
    options: Map<K, String>,
    selectedOption: K?,
    visibleItemsBeforeScroll: Int,
    transition: Transition<Boolean>,
    colors: DropdownColors,
    onOptionSelected: (K) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItemFocusRequester = remember { FocusRequester() }
    val maxHeight =
        options.size.coerceAtMost(visibleItemsBeforeScroll.coerceAtLeast(1)) *
            dropdownOptionHeight +
            dropdownOptionHeight * .5f

    val actualSelectedOption = selectedOption ?: options.keys.first()

    val height by transition.animateDp(
        transitionSpec = { dropdownTransitionSpecDp },
        label = "Popup content height"
    ) {
        if (it) maxHeight else 0.dp
    }

    val elevation by transition.animateDp(
        transitionSpec = { dropdownTransitionSpecDp },
        label = "Popup content shadow"
    ) {
        if (it) 3.dp else 0.dp
    }

    LazyColumn(
        state = rememberLazyListState(
            initialFirstVisibleItemIndex = options.keys.indexOf(actualSelectedOption)
        ),
        modifier = modifier
            .height(height)
            // This should be a box shadow (-> 0 2px 6px 0 rgba(0,0,0,.2)). But compose
            // doesn't provide the same API as CSS for shadows. A 3dp elevation is the
            // best approximation that could be found for now.
            .shadow(elevation = elevation)
            .background(color = colors.menuOptionBackgroundColor)
    ) {
        itemsIndexed(options.entries.toList()) { index, option ->
            SideEffect {
                if (option.key == actualSelectedOption) {
                    currentItemFocusRequester.requestFocus()
                }
            }
            DropdownMenuOption(
                option = option,
                onOptionSelected = onOptionSelected,
                showDivider = index != 0,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (option.key == actualSelectedOption) {
                            Modifier.focusRequester(currentItemFocusRequester)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

private fun Modifier.onEscape(block: () -> Unit) = onPreviewKeyEvent {
    if (it.key == Key.Escape) {
        block()
        true
    } else {
        false
    }
}

@Composable
private fun <K : Any> DropdownMenuOption(
    option: Map.Entry<K, String>,
    colors: DropdownColors,
    showDivider: Boolean,
    onOptionSelected: (K) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Box(
        modifier = modifier
            .height(dropdownOptionHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = FocusIndication(),
                onClick = { onOptionSelected(option.key) }
            )
            .padding(horizontal = 16.dp)
    ) {
        if (showDivider) {
            DropdownMenuOptionDivider(
                colors.menuOptionBorderColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = option.value,
                style = CarbonTypography.bodyCompact01,
                color = colors.menuOptionTextColor,
            )
        }
    }
}

@Composable
private fun DropdownMenuOptionDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .background(color = color)
            .height(1.dp)
            .fillMaxWidth()
    )
}

private object DropdownMenuPositionProvider : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = anchorBounds.left,
        y = anchorBounds.top + anchorBounds.height
    )
}
