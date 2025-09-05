package dev.stargeras.sandbox.drawers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import dev.stargeras.sandbox.R
import dev.stargeras.sandbox.TextStyle
import dev.stargeras.sandbox.views.utils.HorizontalAlignment
import dev.stargeras.sandbox.views.utils.Paddings
import dev.stargeras.sandbox.views.utils.VerticalAlignment
import kotlin.math.max
import kotlin.math.min

/**
 * Класс для отрисовки заголовка и подзаголовка с настраиваемыми стилями.
 * Поддерживает разные стили для состояний фокуса/без фокуса, многострочность и троеточие.
 *
 * @see BaseLayoutRender
 */
class TitleSubtitleLayoutRender(
    context: Context,
    targetView: View,
) : BaseLayoutRender<TitleSubtitleLayoutRender.State>(targetView) {

    private var titleLayout: StaticLayout? = null
    private var subtitleLayout: StaticLayout? = null

    var state: State = State()
        private set

    private var textStyles: TextStyles = TextStyles(
        titleStyle = TextStyle(
            R.style.TextAppearance_PC_Title_Focused,
            R.style.TextAppearance_PC_Title_Unfocused
        ),
        subtitleStyle = TextStyle(
            R.style.TextAppearance_PC_Subtitle_Focused,
            R.style.TextAppearance_PC_Subtitle_Unfocused
        )
    )
        set(value) {
            field = value
            updateTitlePaint()
        }

    /** Объект для хранения кистей текста. */
    private var paints = Paints()

    /** Кисть для заголовка. */
    private val titlePaint: TextPaint
        get() = if (state.isFocused) paints.titleFocused else paints.titleUnfocused

    /** Кисть для подзаголовка. */
    private val subtitlePaint: TextPaint
        get() = if (state.isFocused) paints.subtitleFocused else paints.subtitleUnfocused

    /** TextView для применения стиля из ресурсов и маппинга его в TextPaint. */
    private val styledTextView = TextView(context)

    /** Обновляет состояние в соответствии с новыми настройками. */
    override fun updateState(newState: (State) -> State) {
        state = newState(state)
        updateTitlePaint()
        redraw()
    }

    override fun draw(canvas: Canvas) {
        var y = getLeftY()
        val x = getTopX()

        titleLayout?.let { layout ->
            drawLayout(canvas, layout, x, y)
            y += layout.height
        }

        subtitleLayout?.let { layout ->
            // Отступ между заголовком и подзаголовком
            y += state.spacing
            drawLayout(canvas, layout, x, y)
        }
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun getLeftY() = if (state.verticalAlignment == VerticalAlignment.CENTER) {
        calculateVerticalOffset(internalState.parentHeight, getMeasuredHeightWithoutPaddings())
    } else {
        state.paddings.top.toFloat()
    }

    private fun getTopX() = if (state.horizontalAlignment == HorizontalAlignment.CENTER) {
        0f
    } else {
        state.paddings.left.toFloat()
    }

    private fun getMeasuredHeightWithoutPaddings(): Int {
        var height = 0

        height += titleLayout?.height ?: 0
        height += subtitleLayout?.height ?: 0
        height += if (subtitleLayout?.height == 0) 0 else state.spacing

        return height
    }

    override fun measure(
        desiredWidth: Int,
        desiredHeight: Int,
        parentWidth: Int,
        parentHeight: Int,
    ): LayoutRender.MeasuredResult {
        val measuredWidth = desiredWidth + state.paddings.horizontal()

        buildLayouts(measuredWidth)

        val titleHeight = titleLayout?.height ?: 0
        val subtitleHeight = subtitleLayout?.height ?: 0

        val spacing = if (titleHeight > 0 && subtitleHeight > 0) state.spacing else 0

        val measuredHeight = titleHeight + spacing + subtitleHeight + state.paddings.vertical()

        updateInternalState { oldState ->
            oldState.copy(
                desiredWidth = measuredWidth,
                desiredHeight = measuredHeight,
                parentWidth = parentWidth,
                parentHeight = parentHeight
            )
        }

        return LayoutRender.MeasuredResult(internalState.desiredWidth, internalState.desiredHeight)
    }

    /**
     * Вычисляет горизонтальную позицию в зависимости от выравнивания и размера родителя
     */
    private fun calculateHorizontalPosition(availableWidth: Int, contentWidth: Float): Float {
        return when (state.horizontalAlignment) {
            HorizontalAlignment.LEFT -> {
                if (internalState.parentWidth > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    state.paddings.left.toFloat()
                } else {
                    // Иначе используем доступную ширину View
                    state.paddings.left.toFloat()
                }
            }

            HorizontalAlignment.CENTER -> {
                if (internalState.parentWidth > 0) {
                    // Центрируем относительно родительского контейнера
                    (internalState.parentWidth - contentWidth) / 2
                } else {
                    // Центрируем относительно доступной области View
                    state.paddings.left + (availableWidth - contentWidth) / 2
                }
            }

            HorizontalAlignment.RIGHT -> {
                if (internalState.parentWidth > 0) {
                    // Позиционируем по правому краю родительского контейнера
                    internalState.parentWidth - contentWidth - state.paddings.right
                } else {
                    // Позиционируем по правому краю доступной области View
                    state.paddings.left + (availableWidth - contentWidth)
                }
            }
        }
    }

    /**
     * Вычисляет вертикальную позицию в зависимости от выравнивания и размера родителя
     */
    private fun calculateVerticalPosition(availableHeight: Int, contentHeight: Float): Float {
        return when (state.verticalAlignment) {
            VerticalAlignment.TOP -> {
                if (internalState.parentHeight > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    state.paddings.top.toFloat()
                } else {
                    // Иначе используем доступную высоту View
                    state.paddings.top.toFloat()
                }
            }

            VerticalAlignment.CENTER -> {
                if (internalState.parentHeight > 0) {
                    // Центрируем относительно родительского контейнера
                    (internalState.parentHeight - contentHeight) / 2
                } else {
                    // Центрируем относительно доступной области View
                    state.paddings.top + (availableHeight - contentHeight) / 2
                }
            }

            VerticalAlignment.BOTTOM -> {
                if (internalState.parentHeight > 0) {
                    // Позиционируем по нижнему краю родительского контейнера
                    internalState.parentHeight - contentHeight - state.paddings.bottom
                } else {
                    // Позиционируем по нижнему краю доступной области View
                    state.paddings.top + (availableHeight - contentHeight)
                }
            }
        }
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentWidth = availableWidth
        if (contentWidth <= 0) {
            titleLayout = null
            subtitleLayout = null
            return
        }

        titleLayout = makeLayout(state.title, titlePaint, state.maxTitleLines, contentWidth)

        if (state.hasSubtitle()) {
            subtitleLayout =
                makeLayout(state.subtitle, subtitlePaint, state.maxSubtitleLines, contentWidth)
        }
    }

    private fun makeLayout(
        text: CharSequence?,
        paint: TextPaint,
        maxLines: Int,
        width: Int,
    ): StaticLayout? {
        val src = text ?: return null
        return StaticLayout.Builder.obtain(src, 0, src.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    /**
     * Вычисляет оптимальную ширину контента на основе текста и стилей.
     *
     * @return оптимальная ширина в пикселях
     */
    fun calculateOptimalWidth(): Int {
        var maxWidth = 0

        // Ширина заголовка
        if (state.hasTitle()) {
            val titleWidth =
                calculateTextWidth(state.title, titlePaint) + state.paddings.horizontal()
            maxWidth = max(maxWidth, titleWidth.toInt())
        }

        // Ширина подзаголовка
        if (state.hasSubtitle()) {
            val subtitleWidth =
                calculateTextWidth(state.subtitle, subtitlePaint) + state.paddings.horizontal()
            maxWidth = max(maxWidth, subtitleWidth.toInt())
        }

        // Вычисляем ширину контента между заголовком, подзаголовком и максимальной шириной
        return max(maxWidth.toInt(), internalState.desiredWidth)
    }

    /**
     * Вычисляет ширину текста без переносов.
     *
     * @param text текст для измерения
     * @param paint кисть для измерения
     * @return ширина текста в пикселях
     */
    private fun calculateTextWidth(text: String, paint: TextPaint): Float {
        return paint.measureText(text)
    }

    /** Вычисляет высоту контента по содержимому. Заголовок, подзаголовок и отступ. */
    fun getContentHeight(): Int {
        val width = getContentWidth()
        val height = getContentHeight(width.toFloat())
        return height
    }

    /** Вычисляет ширину контента по содержимому. */
    fun getContentWidth(): Int {
        return if (internalState.desiredWidth <= 0) {
            calculateOptimalWidth()
        } else {
            min(internalState.desiredWidth, calculateOptimalWidth())
        }
    }

    /**
     * Вычисляет высоту контента для заданной ширины.
     *
     * @param width ширина для вычисления высоты
     * @return высота контента в пикселях
     */
    private fun getContentHeight(width: Float): Int {
        return calculateContentHeight(width)
    }

    /**
     * Вычисляет высоту контента с учетом всех параметров.
     *
     * @param width ширина для вычисления высоты
     * @return высота контента в пикселях
     */
    private fun calculateContentHeight(width: Float): Int {
        var totalHeight = 0f

        // Если нет заголовка, выбрасываем исключение
        if (!state.hasTitle()) {
            return throw IllegalArgumentException("No title provided")
        }

        // Высота заголовка
        totalHeight += calculateTextHeight(state.title, titlePaint, width, state.maxTitleLines)

        totalHeight += calculateSubtitleHeight(width)

        return totalHeight.toInt()
    }

    /** Расчет высоты подзаголовка с учетом максимальной ширины. */
    private fun calculateSubtitleHeight(width: Float): Float {
        // Отступ между текстами только если есть подзаголовок
        return if (state.hasSubtitle()) {
            state.spacing + calculateTextHeight(state.subtitle, subtitlePaint, width, state.maxSubtitleLines)
        } else {
            0f
        }
    }

    /**
     * Вычисляет высоту текста с учетом стилей и ограничений.
     *
     * @param text текст для измерения
     * @param paint кисть для измерения
     * @param width максимальная ширина
     * @param maxLines максимальное количество строк
     * @return высота текста в пикселях
     */
    private fun calculateTextHeight(
        text: String,
        paint: TextPaint,
        width: Float,
        maxLines: Int,
    ): Float {
        val words = text.split(" ")
        var currentLine = ""
        var lineCount = 0

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= width) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lineCount++
                    currentLine = word
                } else {
                    lineCount++
                    currentLine = ""
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lineCount++
        }

        val actualLines = min(lineCount, maxLines)
        return actualLines * paint.fontSpacing
    }

    /** Обновляет кисть для заголовка в соответствии с текущим стилем и состоянием фокуса. */
    private fun updateTitlePaint() {
        styledTextView.apply {
            updatePaints { oldState ->
                oldState.copy(
                    titleFocused = applyStyleToTextPaint(
                        textStyles.titleStyle.styleFocused,
                        paints.titleFocused
                    ),
                    titleUnfocused = applyStyleToTextPaint(
                        textStyles.titleStyle.styleUnfocused,
                        paints.titleUnfocused
                    ),
                    subtitleFocused = applyStyleToTextPaint(
                        textStyles.subtitleStyle.styleFocused,
                        paints.subtitleFocused
                    ),
                    subtitleUnfocused = applyStyleToTextPaint(
                        textStyles.subtitleStyle.styleUnfocused,
                        paints.subtitleUnfocused
                    )
                )
            }
        }
    }

    private fun calculateHorizontalOffset(layoutWidth: Int, contentWidth: Int): Float {
        return when (state.horizontalAlignment) {
            HorizontalAlignment.LEFT -> 0f
            HorizontalAlignment.CENTER -> (contentWidth - layoutWidth) / 2f
            HorizontalAlignment.RIGHT -> (contentWidth - layoutWidth).toFloat()
        }
    }

    private fun calculateVerticalOffset(parentHeight: Int, viewHeight: Int): Float {
        return when (state.verticalAlignment) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (parentHeight / 2 - viewHeight / 2).toFloat()
            VerticalAlignment.BOTTOM -> parentHeight.toFloat()
        }.apply {
            Log.i(
                "getMeasuredHeightWithoutPaddings",
                "verticalOffset = $this parentHeight = $parentHeight viewHeight = $viewHeight"
            )
        }
    }

    private fun TextView.applyStyleToTextPaint(styleId: Int, paint: TextPaint): TextPaint {
        setTextAppearance(styleId)

        return paint.let { paint ->
            paint.color = currentTextColor
            paint.textSize = textSize
            paint.typeface = typeface
            paint
        }
    }

    private fun updatePaints(newState: (Paints) -> Paints) {
        paints = newState.invoke(paints)
    }

    /**
     * Состояние, описывающее данные и параметры макета с заголовком и подзаголовком.
     *
     * Используется для вычисления размеров и отрисовки элемента, содержащего текстовые компоненты:
     * заголовок (title) и подзаголовок (subtitle), с настройками позиционирования и внешнего вида.
     */
    data class State(
        val title: String = "",
        val subtitle: String = "",
        /** Расстояние между заголовком и подзаголовком. */
        val spacing: Int = 0,
        val maxTitleLines: Int = 1,
        val maxSubtitleLines: Int = 1,
        val isFocused: Boolean = false,
        val paddings: Paddings = Paddings(0, 0, 0, 0),
        val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
        val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT,
    ) {

        fun hasTitle() = title.isNotEmpty()

        fun hasSubtitle() = subtitle.isNotEmpty()
    }

    private data class Paints(
        val titleFocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val titleUnfocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val subtitleFocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val subtitleUnfocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
    )

    data class TextStyles(
        val titleStyle: TextStyle,
        val subtitleStyle: TextStyle,
    )
}