package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
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
        set(value) {
            field = value
            rectangleDrawer.colorUnfocused = value
        }

    /** Цвет при фокусе. */
    @ColorInt
    protected open var colorFocused: Int = resources.getColor(R.color.pc_color_focused, context.theme)
        set(value) {
            field = value
            rectangleDrawer.colorFocused = value
        }

    /** Процент масштабирования при фокусе [0f..1f]. По умолчанию 3% */
    protected open var focusScalePercent: Float = resources.getFraction(R.fraction.pc_focus_scale_percent, 1, 1)
        set(value) {
            field = value.coerceIn(0f, 1f)
            rectangleDrawer.focusScalePercent = field
        }

    /** Длительность анимации (мс). */
    protected open var animationDurationMs: Long = resources.getInteger(R.integer.pc_animation_duration_ms).toLong()
        set(value) {
            field = value
            rectangleDrawer.animationDurationMs = value
        }

    /** Радиус скругления углов (px). По умолчанию 8dp. */
    protected open var cornerRadiusPx: Float = resources.getDimension(R.dimen.pc_corner_radius)
        set(value) {
            field = value
            rectangleDrawer.cornerRadiusPx = value
        }

    /** Объект для отрисовки прямоугольника и управления анимацией. */
    private val rectangleDrawer = RectangleDrawer(context, this)


    /** Использовать ли масштабирование свойств View (scaleX/scaleY) вместо перерисовки размеров. */
    protected open var useViewPropertyScale: Boolean = false
        set(value) {
            field = value
        }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false
        
        // Инициализируем RectangleDrawer с текущими параметрами
        rectangleDrawer.colorUnfocused = colorUnfocused
        rectangleDrawer.colorFocused = colorFocused
        rectangleDrawer.cornerRadiusPx = cornerRadiusPx
        rectangleDrawer.focusScalePercent = focusScalePercent
        rectangleDrawer.animationDurationMs = animationDurationMs
    }

    /** Ширина области рисования (без внутренних отступов) на базе текущего масштаба. */
    fun contentWidth(): Int {
        val requested = currentContentWidthPx ?: rectBaseWidthPx
        val stable = max(requested, minContentWidthPx)
        return (stable * rectangleDrawer.currentScale).toInt()
    }

    /** Высота области рисования (без внутренних отступов) на базе текущего масштаба. */
    fun contentHeight(): Int {
        val requested = currentContentHeightPx ?: rectBaseHeightPx
        val stable = max(requested, minContentHeightPx)
        return (stable * rectangleDrawer.currentScale).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Стабильные размеры без учёта анимационного масштаба (чтобы не было дёрганий лэйаута)
        val requestedWidth = currentContentWidthPx ?: rectBaseWidthPx
        val requestedHeight = currentContentHeightPx ?: rectBaseHeightPx
        val desiredWidth = max(requestedWidth, minContentWidthPx)
        val desiredHeight = max(requestedHeight, minContentHeightPx)
        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)

        val scaledValueWidth = (measuredWidth - measuredWidth / rectangleDrawer.currentScale).toInt()
        val scaledWidth = measuredWidth + scaledValueWidth

        val scaledValueHeight = (measuredWidth - measuredWidth / rectangleDrawer.currentScale).toInt()
        val scaledHeight = measuredHeight + scaledValueHeight

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

        // Используем RectangleDrawer для отрисовки
        rectangleDrawer.drawRectangle(canvas, availableWidth, availableHeight, targetWidth.toInt(), targetHeight.toInt())
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        rectangleDrawer.isFocused = gainFocus
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Приводим цвет к актуальному состоянию фокуса
        rectangleDrawer.isFocused = hasFocus()
    }
}
