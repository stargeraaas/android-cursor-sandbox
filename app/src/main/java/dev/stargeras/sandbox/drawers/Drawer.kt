package dev.stargeras.sandbox.drawers

import android.graphics.Canvas

/** Класс для измерения размера и отрисовки объекта */
interface Drawer<State> {

    fun draw(canvas: Canvas)

    fun measure(desiredWidth: Int, desiredHeight: Int): MeasuredResult

    fun updateState(newState: (State) -> State)

    data class MeasuredResult(val width: Int, val height: Int)

}