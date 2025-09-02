package dev.stargeras.sandbox.drawers

import android.graphics.Canvas

/** Класс для измерения размера и отрисовки объекта */
interface Drawer {
    fun draw(canvas: Canvas)

    fun measure(desiredWidth: Int, desiredHeight: Int, measured: (Int, Int) -> Unit)
}