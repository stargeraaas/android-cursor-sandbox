package dev.stargeras.sandbox.views.utils

import android.util.Log
import dev.stargeras.sandbox.drawers.Paddings

object PositionCalculator {
    /**
     * Вычисляет горизонтальную позицию в зависимости от выравнивания и размера родителя
     */
    fun calculateHorizontalPosition(
        parentWidth: Int,
        viewWidth: Int,
        alignment: HorizontalAlignment,
        paddings: Paddings,
    ): Int {
        Log.d("ALIGNMENT", "calculateHorizontalPosition: parentWidth: $parentWidth viewWidth: $viewWidth alignment: $alignment")

        return when (alignment) {
            HorizontalAlignment.LEFT -> {
                if (parentWidth > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    paddings.left
                } else {
                    // Иначе используем доступную ширину View
                    paddings.left
                }
            }

            HorizontalAlignment.CENTER -> {
                if (parentWidth > 0) {
                    // Центрируем относительно родительского контейнера
                    (parentWidth - viewWidth) / 2
                } else {
                    // Центрируем относительно доступной области View
                    paddings.left + (parentWidth - viewWidth) / 2
                }
            }

            HorizontalAlignment.RIGHT -> {
                if (parentWidth > 0) {
                    // Позиционируем по правому краю родительского контейнера
                    parentWidth - viewWidth - paddings.right
                } else {
                    // Позиционируем по правому краю доступной области View
                    paddings.left + (parentWidth - viewWidth)
                }
            }
        }
    }

    /**
     * Вычисляет вертикальную позицию в зависимости от выравнивания и размера родителя
     */
    fun calculateVerticalPosition(
        parentHeight: Int,
        viewHeight: Int,
        alignment: VerticalAlignment,
        paddings: Paddings,
    ): Int {
        Log.d("ALIGNMENT", "calculateVerticalPosition: parentHeight: $parentHeight viewHeight: $viewHeight alignment: $alignment")

        return when (alignment) {
            VerticalAlignment.TOP -> {
                if (parentHeight > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    paddings.top
                } else {
                    // Иначе используем доступную высоту View
                    paddings.top
                }
            }

            VerticalAlignment.CENTER -> {
                if (parentHeight > 0) {
                    // Центрируем относительно родительского контейнера
                    (parentHeight - viewHeight) / 2
                } else {
                    // Центрируем относительно доступной области View
                    paddings.top + (parentHeight - viewHeight) / 2
                }
            }

            VerticalAlignment.BOTTOM -> {
                if (parentHeight > 0) {
                    // Позиционируем по нижнему краю родительского контейнера
                    parentHeight - viewHeight - paddings.bottom
                } else {
                    // Позиционируем по нижнему краю доступной области View
                    paddings.top + (parentHeight - viewHeight)
                }
            }
        }
    }
}