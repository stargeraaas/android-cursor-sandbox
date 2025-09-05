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
import dev.stargeras.sandbox.drawers.LayoutRender.MeasuredResult
import dev.stargeras.sandbox.drawers.RectangleLayoutRender.State

/**
 * Класс для отрисовки прямоугольника с настраиваемыми параметрами и анимацией.
 * Инкапсулирует логику рисования прямоугольника с поддержкой масштабирования, скругления углов и анимации фокуса.
 *
 * @see BaseLayoutRender
 */
class RectangleLayoutRender(
    context: Context,
    targetView: View,
) : BaseLayoutRender<State>(targetView) {

    var state: State = State(
        cornerRadiusPx = context.resources.getDimension(R.dimen.pc_corner_radius),
        focusScalePercent = 0.03f,
        colors = RectangleColors(
            colorFocused = context.resources.getColor(R.color.pc_color_focused, context.theme),
            colorUnfocused = context.resources.getColor(R.color.pc_color_unfocused, context.theme)
        ),
        isFocused = false
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
        val viewWidth = internalState.desiredWidth
        val viewHeight = internalState.desiredHeight

        val leftX = -internalState.scaledWidthPaddingValue()
        val topY = -internalState.scaledHeightPaddingValue()

        var rightX = leftX + viewWidth
        var bottomY = topY + viewHeight

        if (state.isFocused) {
            rightX = leftX + internalState.desiredWidth + internalState.scaledWidthPaddingValue()
            bottomY = topY + internalState.desiredWidth + internalState.scaledHeightPaddingValue()
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

        val start = currentScale

        val end = if (gainFocus) {
            1f + state.focusScalePercent.coerceIn(0f, 1f)
        } else {
            1f
        }

        scaleAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = animationDurationMs
            addUpdateListener { animator ->
                // Обновляем текущий масштаб увеличения для вычисления новых координат
                currentScale = animator.animatedValue as Float
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

    private var currentScale = 1f

    private fun InternalState.scaledWidthPaddingValue() = (desiredWidth * currentScale).toInt()
    private fun InternalState.scaledHeightPaddingValue() = (desiredWidth * currentScale).toInt()

    override fun measure(desiredWidth: Int, desiredHeight: Int, parentWidth: Int, parentHeight: Int): MeasuredResult {
        // Вычисляем размеры с учетом масштаба
        val totalWidth = desiredWidth + internalState.scaledWidthPaddingValue()
        val totalHeight = desiredHeight + internalState.scaledHeightPaddingValue()

        updateInternalState { oldState ->
            oldState.copy(
                desiredWidth = totalWidth,
                desiredHeight = totalHeight,
                parentWidth = parentWidth,
                parentHeight = parentHeight
            )
        }

        // Вычисляем координаты для центрирования
        calculateCoordinates()

        return MeasuredResult(internalState.desiredWidth, internalState.desiredHeight)
    }

    /**
     * Состояние прямоугольника, необходимое для отрисовки и анимации.
     *
     * Хранит параметры, влияющие на размеры, масштабирование и фокус элемента.
     *
     * @property cornerRadiusPx радиус скругления углов (px).
     * @property isFocused флаг фокуса элемента.
     * @property currentScale текущий масштаб увеличения.
     * @property focusScalePercent процент увеличения размеров при получении фокуса.
     */
    data class State(
        val cornerRadiusPx: Float,
        val focusScalePercent: Float,
        val colors: RectangleColors,
        val isFocused: Boolean = false,
    )

    data class RectangleColors(
        @ColorInt val colorFocused: Int,
        @ColorInt val colorUnfocused: Int,
    )
}