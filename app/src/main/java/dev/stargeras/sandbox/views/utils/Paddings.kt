package dev.stargeras.sandbox.views.utils

/**  Класс, представляющий отступы (поля) вокруг элемента */
data class Paddings(
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int
) {
    fun horizontal() = left + right

    fun vertical() = top + bottom
}