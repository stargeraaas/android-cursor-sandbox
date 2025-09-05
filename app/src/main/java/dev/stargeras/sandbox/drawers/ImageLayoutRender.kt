package dev.stargeras.sandbox.drawers

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import dev.stargeras.sandbox.views.utils.HorizontalAlignment
import dev.stargeras.sandbox.views.utils.Paddings
import dev.stargeras.sandbox.views.utils.PositionCalculator
import dev.stargeras.sandbox.views.utils.VerticalAlignment

/**
 * Drawer для отрисовки ресурсов (drawable) на canvas с поддержкой ScaleType, размеров и отступов.
 * Аналогичен ImageView по функциональности масштабирования.
 */
class ImageLayoutRender(
    private val context: Context,
    targetView: View,
) : BaseLayoutRender<ImageLayoutRender.State>(targetView) {

    /**
     * Состояние ImageDrawer
     */
    data class State(
        val focusedResourceId: Int? = null,
        val unfocusedResourceId: Int? = null,
        val paddings: Paddings = Paddings(0, 0, 0, 0),
        val isFocused: Boolean = false,
        val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
        val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT,
    )

    var state: State = State()
        private set

    private var drawable: Drawable? = null

    /**
     * Обновляет состояние ResourceDrawer
     */
    override fun updateState(newState: (State) -> State) {
        val oldState = state
        val newState = newState(state)

        if (oldState == newState) {
            return
        }

        state = newState

        // Если изменился ресурс или состояние фокуса, загружаем новый drawable
        if (wasResourcesOrFocusChanged(oldState, newState)) {
            loadDrawable()
        }

        redraw()
    }

    /** Проверка на изменение ресурсов или фокуса для отрисовки нового изображения */
    private fun wasResourcesOrFocusChanged(oldState: State, newState: State): Boolean {
        return oldState.focusedResourceId != newState.focusedResourceId ||
            oldState.unfocusedResourceId != newState.unfocusedResourceId ||
            oldState.isFocused != newState.isFocused
    }

    /** Загрузка drawable по ID ресурса в зависимости от состояния фокуса */
    private fun loadDrawable() {
        getResourceIdByFocusState()?.let { id ->
            try {
                drawable = ContextCompat.getDrawable(context, id)
            } catch (e: Exception) {
                drawable = null
            }
        } ?: run {
            drawable = null
        }
    }

    private fun getResourceIdByFocusState(): Int? = if (state.isFocused) {
        state.focusedResourceId
    } else {
        state.unfocusedResourceId
    }

    private fun calculateHorizontalPosition(): Int {
        return PositionCalculator.calculateHorizontalPosition(
            parentWidth = internalState.parentWidth,
            viewWidth = internalState.desiredWidth,
            alignment = state.horizontalAlignment,
            paddings = state.paddings
        ).apply {
            updateCoordinates { oldState ->
                oldState.copy(left = this, right = (this + internalState.desiredWidth))
            }
        }
    }

    private fun calculateVerticalPosition(): Int {
        return PositionCalculator.calculateVerticalPosition(
            parentHeight = internalState.parentHeight,
            viewHeight = internalState.desiredHeight,
            alignment = state.verticalAlignment,
            paddings = state.paddings
        ).apply {
            updateCoordinates { oldState ->
                oldState.copy(top = this, bottom = (this + internalState.desiredHeight))
            }
        }
    }

    override fun draw(canvas: Canvas) {
        val drawable = drawable ?: return

        canvas.save()
        drawable.setBounds(coordinates.left, coordinates.top, coordinates.right, coordinates.bottom)
        drawable.draw(canvas)

        canvas.restore()
    }

    override fun measure(
        desiredWidth: Int,
        desiredHeight: Int,
        parentWidth: Int,
        parentHeight: Int,
    ): LayoutRender.MeasuredResult {

        updateInternalState { oldState ->
            oldState.copy(
                desiredWidth = desiredWidth,
                desiredHeight = desiredHeight,
                parentWidth = parentWidth,
                parentHeight = parentHeight
            )
        }

        // Рассчитываем координаты для отрисовки
        calculateHorizontalPosition()
        calculateVerticalPosition()


        return LayoutRender.MeasuredResult(internalState.desiredWidth, internalState.desiredHeight)
    }
}
