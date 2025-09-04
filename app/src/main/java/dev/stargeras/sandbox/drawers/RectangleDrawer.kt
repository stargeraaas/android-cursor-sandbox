package dev.stargeras.sandbox.drawers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import dev.stargeras.sandbox.R
import dev.stargeras.sandbox.drawers.Drawer.MeasuredResult
import dev.stargeras.sandbox.drawers.RectangleDrawer.State

/**
 * Класс для отрисовки прямоугольника с настраиваемыми параметрами и анимацией.
 * Инкапсулирует логику рисования прямоугольника с поддержкой масштабирования, скругления углов и анимации фокуса.
 */
class RectangleDrawer(
    context: Context,
    targetView: View,
) : BaseDrawer<State>(targetView) {

    var state: State = State(
        desiredWidth = 0,
        desiredHeight = 0,
        cornerRadiusPx = context.resources.getDimension(R.dimen.pc_corner_radius),
        focusScalePercent = 0.03f,
        colors = RectangleColors(
            colorFocused = context.resources.getColor(R.color.pc_color_focused, context.theme),
            colorUnfocused = context.resources.getColor(R.color.pc_color_unfocused, context.theme)
        ),
        isFocused = false,
        currentScale = 1f
    )
        private set

    /** Длительность анимации (мс). */
    var animationDurationMs: Long =
        context.resources.getInteger(R.integer.pc_animation_duration_ms).toLong()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = state.colors.colorUnfocused
    }

    private var scaleAnimator: ValueAnimator? = null

    override fun updateState(newState: (State) -> State) {
        val oldState = state
        val newState = newState.invoke(oldState)

        if (newState == oldState) {
            return
        }

        state = newState

        if (oldState.isFocused == newState.isFocused) {
            // Если состояние фокуса не изменилось просто перерисовываем
            // это происходит когда у нас идет анимация
            redraw()
        } else {
            // Если состояние фокуса изменилось, запускаем анимацию
            startFocusAnimation(newState.isFocused)
        }
    }

    /** Вычисляет и сохраняет координаты карточки прямоугольника */
    private fun calculateCoordinates() {
        // Вычисляем координаты для центрирования
        val viewWidth = state.width
        val viewHeight = state.height

        val leftX = -state.scaledWidthPaddingValue()
        val topY = -state.scaledHeightPaddingValue()

        var rightX = leftX + viewWidth
        var bottomY = topY + viewHeight

        if (state.isFocused) {
            rightX = leftX + state.width + state.scaledWidthPaddingValue()
            bottomY = topY + state.height + state.scaledHeightPaddingValue()
        }

        updateCoordinates { oldState ->
            oldState.copy(
                left = leftX,
                top = topY,
                right = rightX,
                bottom = bottomY
            )
        }
    }

    private fun getPaintColor() =
        if (state.isFocused) state.colors.colorFocused else state.colors.colorUnfocused

    /**
     * Обновляет состояние фокуса и цвет кисти.
     *
     * @param focused новое состояние фокуса
     */
    private fun updateFocusState(focused: Boolean) {
        updateState { oldState -> oldState.copy(isFocused = focused) }
    }

    /**
     * Запускает анимацию изменения масштаба при получении/потере фокуса.
     *
     * @param gainFocus true если элемент получает фокус, false если теряет
     */
    private fun startFocusAnimation(gainFocus: Boolean) {
        scaleAnimator?.cancel()

        val start = state.currentScale

        val end = if (gainFocus) {
            1f + state.focusScalePercent.coerceIn(0f, 1f)
        } else {
            1f
        }

        scaleAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = animationDurationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                // Обновляем текущий масштаб увеличения для вычисления новых координат
                updateState { oldState -> oldState.copy(currentScale = value) }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    updateFocusState(gainFocus)
                }
            })

            start()
        }
    }

    override fun draw(canvas: Canvas) {
        // Обновляем цвет в зависимости от состояния фокуса
        paint.color = getPaintColor()

        Log.v("ImageDrawer", "RECTANGLE: draw: coordinates= $coordinates")

        coordinates.apply {
            // Рисуем прямоугольник используя кэшированные координаты
            canvas.drawRoundRect(
                /* left = */ left.toFloat(),
                /* top = */ top.toFloat(),
                /* right = */ right.toFloat(),
                /* bottom = */ bottom.toFloat(),
                /* rx = */ state.cornerRadiusPx,
                /* ry = */ state.cornerRadiusPx,
                /* paint = */ paint
            )
        }
    }

    override fun measure(desiredWidth: Int, desiredHeight: Int): MeasuredResult {
        // Вычисляем размеры с учетом масштаба
        val scaledValueWidth = state.scaledWidthPaddingValue()
        val scaledValueHeight = state.scaledHeightPaddingValue()

        val totalWidth = desiredWidth + scaledValueWidth
        val totalHeight = desiredHeight + scaledValueHeight

        updateState { oldState ->
            oldState.copy(
                desiredWidth = totalWidth,
                desiredHeight = totalHeight
            )
        }

        // Вычисляем координаты для центрирования
        calculateCoordinates()

        return MeasuredResult(totalWidth, totalHeight)
    }

    /**
     * Состояние прямоугольника, необходимое для отрисовки и анимации.
     *
     * Хранит параметры, влияющие на размеры, масштабирование и фокус элемента.
     *
     * @property desiredWidth желаемая ширина содержимого (без учета масштабирования).
     * @property desiredHeight желаемая высота содержимого (без учета масштабирования).
     * @property cornerRadiusPx радиус скругления углов (px).
     * @property isFocused флаг фокуса элемента.
     * @property currentScale текущий масштаб увеличения.
     * @property focusScalePercent процент увеличения размеров при получении фокуса.
     */
    data class State(
        val desiredWidth: Int,
        val desiredHeight: Int,
        val cornerRadiusPx: Float,
        val focusScalePercent: Float,
        val colors: RectangleColors,
        val isFocused: Boolean = false,
        val currentScale: Float = 1f,
    ) {

        /** Общая ширина с учетом масштабирования */
        val width: Int = desiredWidth + scaledWidthPaddingValue()

        /** Общая высота с учетом масштабирования */
        val height: Int = desiredHeight + scaledHeightPaddingValue()

        /** Отступ по ширине после масштабирования */
        fun scaledWidthPaddingValue() = ((desiredWidth - desiredWidth / currentScale).toInt()) / 2

        /** Отступ по высоте после масштабирования */
        fun scaledHeightPaddingValue() =
            ((desiredHeight - desiredHeight / currentScale).toInt()) / 2
    }

    data class RectangleColors(
        @ColorInt val colorFocused: Int,
        @ColorInt val colorUnfocused: Int,
    )
}