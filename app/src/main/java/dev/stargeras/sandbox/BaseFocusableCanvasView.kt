package dev.stargeras.sandbox

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min

/**
 * Базовая вью для рисования на Canvas с поддержкой фокуса и анимации изменения размера.
 * - Рисует прямоугольник заданного базового размера (в пикселях), минимальные размеры: 412dp x 60dp
 * - При получении фокуса меняет цвет и масштабируется на процент от размера (анимация)
 * - Имеет настраиваемые padding'и (по умолчанию 18dp каждый)
 * - Высота вью равна высоте контента + вертикальные отступы; ширина — с учётом мин. ширины и отступов
 * - Все параметры вынесены в переменные, константы по умолчанию — в companion object
 */
open class BaseFocusableCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ---------- Параметры, доступные для переопределения в наследниках ----------

    /** Базовая ширина рисуемого прямоугольника без учёта анимации (px). */
    protected open var rectBaseWidthPx: Int = resources.getDimensionPixelSize(R.dimen.pc_min_content_width)

    /** Базовая высота рисуемого прямоугольника без учёта анимации (px). */
    protected open var rectBaseHeightPx: Int = resources.getDimensionPixelSize(R.dimen.pc_min_content_height)

    /** Минимальная ширина контента (px). */
    protected open var minContentWidthPx: Int = resources.getDimensionPixelSize(R.dimen.pc_min_content_width)

    /** Минимальная высота контента (px). */
    protected open var minContentHeightPx: Int = resources.getDimensionPixelSize(R.dimen.pc_min_content_height)

    /** Текущая желаемая ширина контента (px), задаётся наследниками. */
    protected open var currentContentWidthPx: Int? = null

    /** Текущая желаемая высота контента (px), задаётся наследниками. */
    protected open var currentContentHeightPx: Int? = null

    /** Цвет без фокуса. */
    @ColorInt
    protected open var colorUnfocused: Int = resources.getColor(R.color.pc_color_unfocused, context.theme)

    /** Цвет при фокусе. */
    @ColorInt
    protected open var colorFocused: Int = resources.getColor(R.color.pc_color_focused, context.theme)

    /** Процент масштабирования при фокусе [0f..1f]. По умолчанию 3% */
    protected open var focusScalePercent: Float = resources.getFraction(R.fraction.pc_focus_scale_percent, 1, 1)
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Длительность анимации (мс). */
    protected open var animationDurationMs: Long = resources.getInteger(R.integer.pc_animation_duration_ms).toLong()

    /** Радиус скругления углов (px). По умолчанию 8dp. */
    protected open var cornerRadiusPx: Float = resources.getDimension(R.dimen.pc_corner_radius)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorUnfocused
    }

    /** Текущий масштаб контента для режима рисования. 1f — базовый размер. */
    private var currentScale: Float = 1f
        set(value) {
            field = value
            Log.e("CANVAS_VIEW", "currentScale= $currentScale")
        }

    /** Использовать ли масштабирование свойств View (scaleX/scaleY) вместо перерисовки размеров. */
    protected open var useViewPropertyScale: Boolean = false

    private var scaleAnimator: ValueAnimator? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false
    }

    /** Ширина области рисования (без внутренних отступов) на базе текущего масштаба. */
    fun contentWidth(): Int {
        val requested = currentContentWidthPx ?: rectBaseWidthPx
        val stable = max(requested, minContentWidthPx)
        return (stable * currentScale).toInt()
    }

    /** Высота области рисования (без внутренних отступов) на базе текущего масштаба. */
    fun contentHeight(): Int {
        val requested = currentContentHeightPx ?: rectBaseHeightPx
        val stable = max(requested, minContentHeightPx)
        return (stable * currentScale).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Стабильные размеры без учёта анимационного масштаба (чтобы не было дёрганий лэйаута)
        val requestedWidth = currentContentWidthPx ?: rectBaseWidthPx
        val requestedHeight = currentContentHeightPx ?: rectBaseHeightPx
        Log.v("CANVAS_VIEW", "requestedWidth= $requestedWidth requestedHeight= $requestedHeight")
        val desiredWidth = max(requestedWidth, minContentWidthPx)
        val desiredHeight = max(requestedHeight, minContentHeightPx)
        Log.v("CANVAS_VIEW", "desiredWidth= $desiredWidth desiredHeight= $desiredHeight")
        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        Log.v("CANVAS_VIEW", "measuredWidth= $measuredWidth measuredHeight= $measuredHeight")

        val scaledValueWidth = (measuredWidth - measuredWidth / currentScale).toInt()
        val scaledWidth = measuredWidth + scaledValueWidth

        val scaledValueHeight = (measuredWidth - measuredWidth / currentScale).toInt()
        val scaledHeight = measuredHeight + scaledValueHeight

        Log.e("CANVAS_VIEW", "scaledWidth= $scaledWidth scaledValueWidth= $scaledValueWidth")

        setMeasuredDimension(scaledWidth, scaledHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Доступная область рисования — вся вью
        val availableWidth = max(0f, width.toFloat())
        val availableHeight = max(0f, height.toFloat())

        // Целевой размер содержимого с учётом масштаба (если используем рисование масштабом)
        val targetWidth = min(contentWidth().toFloat(), availableWidth)
        val targetHeight = min(contentHeight().toFloat(), availableHeight)

        // Центрируем прямоугольник внутри доступной области
        val left = (availableWidth - targetWidth) / 2f - (availableWidth - availableWidth / currentScale) / 2
        val top = (availableHeight - targetHeight) / 2f - (availableHeight - availableHeight / currentScale) / 2
        val right = left + targetWidth
        val bottom = top + targetHeight

        canvas.drawRoundRect(left, top, right, bottom, cornerRadiusPx, cornerRadiusPx, paint)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        startFocusAnimation(gainFocus)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Приводим цвет к актуальному состоянию фокуса
        paint.color = if (hasFocus()) colorFocused else colorUnfocused
    }

    private fun startFocusAnimation(gain: Boolean) {
        scaleAnimator?.cancel()

        val start = if (useViewPropertyScale) scaleX else currentScale
        val end = if (gain) 1f + focusScalePercent.coerceIn(0f, 1f) else 1f

        scaleAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = animationDurationMs
            addUpdateListener {
                val value = it.animatedValue as Float

                currentScale = value
                invalidate()
                requestLayout()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    paint.color = if (gain) colorFocused else colorUnfocused
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
//                    if (!gain && useViewPropertyScale) {
//                        // Возвращаем стабильный масштаб свойств
//                        scaleX = 1f
//                        scaleY = 1f
//                    }
                }
            })
            start()
        }
    }
}


