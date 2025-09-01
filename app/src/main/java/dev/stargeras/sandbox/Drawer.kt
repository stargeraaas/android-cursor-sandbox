package dev.stargeras.sandbox

import android.graphics.Canvas

interface Drawer {
    fun draw(canvas: Canvas)

    fun measure(desiredWidth: Int, desiredHeight: Int, measured: (Int, Int) -> Unit)
}