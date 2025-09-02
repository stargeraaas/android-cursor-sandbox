package dev.stargeras.sandbox

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.withTranslation

/**
 * Data class для хранения координат прямоугольника.
 */
data class RectangleCoordinates(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * Класс для отрисовки прямоугольника с настраиваемыми параметрами и анимацией.
 * Инкапсулирует логику рисования прямоугольника с поддержкой масштабирования, скругления углов и анимации фокуса.
 */
class RectangleDrawer(
    private val context: Context,
    private val targetView: View
) : Drawer {

    /** Цвет без фокуса. */
    @ColorInt
    var colorUnfocused: Int = context.resources.getColor(R.color.pc_color_unfocused, context.theme)

    /** Цвет при фокусе. */
    @ColorInt
    var colorFocused: Int = context.resources.getColor(R.color.pc_color_focused, context.theme)

    /** Радиус скругления углов (px). По умолчанию 8dp. */
    var cornerRadiusPx: Float = context.resources.getDimension(R.dimen.pc_corner_radius)

    /** Текущий масштаб контента для режима рисования. 1f — базовый размер. */
    var currentScale: Float = 1f

    /** Флаг, указывающий, находится ли элемент в фокусе. */
    var isFocused: Boolean = false
        set(value) {
            if (field == value) return

            field = value

            startFocusAnimation(field)
        }

    /** Процент масштабирования при фокусе [0f..1f]. По умолчанию 3% */
    var focusScalePercent: Float =
        context.resources.getFraction(R.fraction.pc_focus_scale_percent, 1, 1)
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Длительность анимации (мс). */
    var animationDurationMs: Long =
        context.resources.getInteger(R.integer.pc_animation_duration_ms).toLong()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorUnfocused
    }

    private var scaleAnimator: ValueAnimator? = null


    /** Кэшированная ширина прямоугольника */
    private var cachedTargetWidth: Int = 0

    /** Кэшированная высота прямоугольника */
    private var cachedTargetHeight: Int = 0

    /** Кэшированная левая координата прямоугольника */
    var leftX: Int = 0
        private set

    /** Кэшированная верхняя координата прямоугольника */
    var topY: Int = 0
        private set

    /** Кэшированная правая координата прямоугольника */
    var rightX: Int = 0
        private set

    /** Кэшированная нижняя координата прямоугольника */
    var bottomY: Int = 0
        private set

    /**
     * Вычисляет и кэширует координаты прямоугольника для заданных параметров.
     *
     * @param availableWidth доступная ширина области рисования
     * @param availableHeight доступная высота области рисования
     * @param targetWidth целевая ширина прямоугольника
     * @param targetHeight целевая высота прямоугольника
     */
    fun calculateCoordinates(
        targetWidth: Int,
        targetHeight: Int
    ): Pair<Int, Int> {
        // Кэшируем размеры
        cachedTargetWidth = targetWidth
        cachedTargetHeight = targetHeight

        // Вычисляем координаты для центрирования
        // Прямоугольник должен быть центрирован в доступной области
        leftX = 0
        topY = 0
        rightX = leftX + targetWidth
        bottomY = topY + targetHeight

        return Pair(rightX - leftX, bottomY - topY)
    }


    private fun getPaintColor() = if (isFocused) colorFocused else colorUnfocused


    /**
     * Обновляет состояние фокуса и цвет кисти.
     *
     * @param focused новое состояние фокуса
     */
    private fun updateFocusState(focused: Boolean) {
        isFocused = focused
        paint.color = getPaintColor()
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
            1f + focusScalePercent.coerceIn(0f, 1f)
        } else {
            1f
        }

        Log.w(
            "RectangleDrawer",
            "startFocusAnimation: gainFocus=$gainFocus, start=$start, end=$end"
        )

        scaleAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = animationDurationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float

                currentScale = value
                Log.d("RectangleDrawer", "Animation update: currentScale=$value")
                targetView.invalidate()
                targetView.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    Log.d("RectangleDrawer", "Animation started: gainFocus=$gainFocus")
                    updateFocusState(gainFocus)
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    Log.d("RectangleDrawer", "Animation ended: finalScale=$currentScale")
                    // Дополнительная логика при завершении анимации может быть добавлена здесь
                }
            })
            start()
        }
    }

    /**
     * Отменяет текущую анимацию, если она выполняется.
     */
    fun cancelAnimation() {
        scaleAnimator?.cancel()
    }

    /**
     * Устанавливает масштаб без анимации.
     *
     * @param scale новый масштаб
     */
    fun setScale(scale: Float) {
        currentScale = scale
    }

    override fun draw(canvas: Canvas) {
        // Обновляем цвет в зависимости от состояния фокуса
        paint.color = getPaintColor()

        Log.i("TitleSubtitleCard", "x=$leftX y=$topY bottomY=$bottomY rightX=$rightX")

        canvas.withTranslation(0f,0f) {
            // Рисуем прямоугольник используя кэшированные координаты
            canvas.drawRoundRect(
                leftX.toFloat(),
                topY.toFloat(),
                rightX.toFloat(),
                bottomY.toFloat(),
                cornerRadiusPx,
                cornerRadiusPx,
                paint
            )
        }
    }

    override fun measure(
        desiredWidth: Int,
        desiredHeight: Int,
        measured: (Int, Int) -> Unit
    ) {

        Log.w("MEASURE", "Rectangle::desiredWidth=$desiredWidth, desiredHeight=$desiredHeight")

        val scaledValueWidth = (desiredWidth - desiredWidth / currentScale).toInt()

        val scaledValueHeight =
            (desiredHeight - desiredHeight / currentScale).toInt()

        Log.v("MEASURE", "Rectangle::scaledValueWidth=$scaledValueWidth, scaledValueHeight=$scaledValueHeight")

        val totalWidth = desiredWidth + scaledValueWidth
        val totalHeight = desiredHeight + scaledValueHeight

        val (width, height) = calculateCoordinates(totalWidth, totalHeight)

        Log.i("MEASURE", "Rectangle::width=$width, height=$height")

        measured.invoke(width, height)
    }
}
