package dev.stargeras.sandbox.drawers

import android.util.Log
import android.view.View
import dev.stargeras.sandbox.views.attributes.ViewCoordinates

/** Базовый класс для всех объектов рендеринга View */
abstract class BaseLayoutRender<State>(private val targetView: View) : LayoutRender<State> {

    protected var internalState: InternalState = InternalState()
    private set

    protected var coordinates: ViewCoordinates = ViewCoordinates()
        private set

    protected fun updateCoordinates(newState: (ViewCoordinates) -> ViewCoordinates) {
        val oldState = coordinates
        val newState = newState.invoke(coordinates)

        if (newState == oldState) return

        coordinates = newState
    }

    protected fun updateInternalState(newState: (InternalState) -> InternalState) {
        val oldState = internalState
        val newState = newState.invoke(internalState)

        if (newState == oldState) return

        internalState = newState

        Log.w("InternalState", "InternalState: $newState class = ${this::class.simpleName}")
    }

    /** Запуск переотрисовки всей view. */
    protected fun redraw() {
        targetView.apply {
            invalidate()
            requestLayout()
        }
    }

    protected data class InternalState(
        open val desiredWidth: Int = 0,
        open val desiredHeight: Int = 0,
        open val parentWidth: Int = 0,
        open val parentHeight: Int = 0,
    )
}