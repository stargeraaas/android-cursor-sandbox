package dev.stargeras.sandbox.drawers

import android.graphics.Canvas

/** Класс для измерения размера и отрисовки объекта */
interface LayoutRender<State> {

    /**
     * Выполняет вычисление размеров элемента на основе желаемых размеров.
     *
     * @param desiredWidth  Желаемая ширина элемента.
     * @param desiredHeight Желаемая высота элемента.
     * @param parentWidth   Ширина родительского элемента.
     * @param parentHeight  Высота родительского элемента.
     * @return Результат измерения с фактическими шириной и высотой.
     */
    fun measure(desiredWidth: Int, desiredHeight: Int, parentWidth: Int, parentHeight: Int): MeasuredResult

    /**
     * Отрисовывает элемент на заданном холсте.
     *
     * @param canvas Холст для отрисовки.
     */
    fun draw(canvas: Canvas)

    /**
     * Обновляет внутреннее состояние элемента.
     *
     * @param newState Функция, возвращающая новое состояние на основе старого.
     */
    fun updateState(newState: (State) -> State)

    /**
     * Результат измерения размеров.
     *
     * @property width  Ширина после измерения.
     * @property height Высота после измерения.
     */
    data class MeasuredResult(val width: Int, val height: Int)
}