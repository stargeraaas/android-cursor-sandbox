package dev.stargeras.sandbox.drawers

import android.view.View
import dev.stargeras.sandbox.views.attributes.ViewCoordinates

abstract class BaseDrawer<State>(private val targetView: View) : Drawer<State> {

    protected var coordinates: ViewCoordinates = ViewCoordinates()
        private set

    protected fun updateCoordinates(newState: (ViewCoordinates) -> ViewCoordinates) {
        val oldState = coordinates
        val newState = newState(oldState)

        if (newState == oldState) return

        coordinates = newState(coordinates)
    }

    /** Запуск переотрисовки */
    protected fun redraw() {
        targetView.apply {
            invalidate()
            requestLayout()
        }
    }
}