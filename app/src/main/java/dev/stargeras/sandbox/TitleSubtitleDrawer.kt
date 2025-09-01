package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.resolveSize
import android.widget.TextView
import androidx.core.graphics.withTranslation
import kotlin.math.max
import kotlin.math.min

/**
 * Класс для отрисовки заголовка и подзаголовка с настраиваемыми стилями.
 * Поддерживает разные стили для состояний фокуса/без фокуса, многострочность и троеточие.
 */
class TitleSubtitleDrawer(
    private val context: Context,
    private val targetView: View
) : Drawer {

    private var titleLayout: StaticLayout? = null
    private var subtitleLayout: StaticLayout? = null

    private var state: State = State()

    fun updateState(newState: (State) -> State) {
        state = newState(state)

        targetView.apply {
            invalidate()
            requestLayout()
        }
    }

    /** Текст заголовка */
    var titleText: String = ""
        set(value) {
            field = value
            targetView.invalidate()
            targetView.requestLayout()
        }

    /** Текст подзаголовка */
    var subtitleText: String? = null
        set(value) {
            field = value
            targetView.invalidate()
            targetView.requestLayout()
        }

    /** Стиль текста заголовка */
    private var titleStyle: TextStyle = TextStyle(
        R.style.TextAppearance_PC_Title_Focused,
        R.style.TextAppearance_PC_Title_Unfocused
    )

    /** Стиль текста подзаголовка */
    private var subtitleStyle: TextStyle =
        TextStyle(R.style.TextAppearance_PC_Subtitle_Focused, R.style.TextAppearance_PC_Subtitle_Unfocused)

    /** Отступ между заголовком и подзаголовком (px) */
    var spacingBetweenTexts: Int = 8

    /** Минимальная ширина отрисовки (px) */
    var minWidth: Int = 0

    /** Желаемая ширина отрисовки (px) */
    private var desiredWidth: Int = 0

    private var paints = Paints()

    private val titlePaint: TextPaint
        get() = if (state.isFocused) paints.titleFocused else paints.titleUnfocused

    private val subtitlePaint: TextPaint
        get() = if (state.isFocused) paints.subtitleFocused else paints.subtitleUnfocused

    private val styledTextView = TextView(context)

    /**
     * Устанавливает стиль для заголовка.
     *
     * @param textStyle объект TextStyle с параметрами стиля
     */
    fun setTitleTextStyle(textStyle: TextStyle) {
        titleStyle = textStyle
        updateTitlePaint()
    }

    /**
     * Устанавливает стиль для подзаголовка.
     *
     * @param textStyle объект TextStyle с параметрами стиля
     */
    fun setSubtitleTextStyle(textStyle: TextStyle) {
        subtitleStyle = textStyle
        updateTitlePaint()
    }

    override fun draw(canvas: Canvas) {
        var y = state.paddings.top.toFloat()
        val x = state.paddings.left.toFloat()

        titleLayout?.let { layout ->
            canvas.withTranslation(x, y) {
                layout.draw(this)
            }
            y += layout.height
        }

        if (titleLayout != null && subtitleLayout != null) {
            y += spacingBetweenTexts
        }

        subtitleLayout?.let { layout ->
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    override fun measure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        measured: (Int, Int) -> Unit
    ) {
        val desiredWidth = widthMeasureSpec
        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec) + state.paddings.horizontal()

        buildLayouts(measuredWidth)

        val titleHeight = titleLayout?.height ?: 0
        val subtitleHeight = subtitleLayout?.height ?: 0

        val spacing = if (titleHeight > 0 && subtitleHeight > 0) spacingBetweenTexts else 0

        val desiredHeight = titleHeight + spacing + subtitleHeight + state.paddings.vertical()

        val measuredHeight = resolveSize(desiredHeight, desiredHeight)

        measured.invoke(measuredWidth, measuredHeight)
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentWidth = availableWidth
        if (contentWidth <= 0) {
            titleLayout = null
            subtitleLayout = null
            return
        }

        titleLayout = makeLayout(titleText, titlePaint, state.maxTitleLines, contentWidth)
        subtitleLayout = makeLayout(subtitleText, subtitlePaint, state.maxSubtitleLines, contentWidth)
    }

    private fun makeLayout(
        text: CharSequence?,
        paint: TextPaint,
        maxLines: Int,
        width: Int
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

    // ---------- Методы вычисления размеров ----------

    /**
     * Вычисляет оптимальную ширину контента на основе текста и стилей.
     *
     * @return оптимальная ширина в пикселях
     */
    fun calculateOptimalWidth(): Int {
        var maxWidth = 0f

        // Ширина заголовка
        if (titleText.isNotEmpty()) {
            val titleWidth = calculateTextWidth(titleText, titlePaint) + state.paddings.horizontal()
            maxWidth = max(maxWidth, titleWidth)
        }

        // Ширина подзаголовка
        if (subtitleText?.isNotEmpty() == true) {
            val subtitleWidth = calculateTextWidth(subtitleText!!, subtitlePaint) + state.paddings.horizontal()
            maxWidth = max(maxWidth, subtitleWidth)
        }

        // Учитываем минимальную и желаемую ширину
        val optimalWidth = maxWidth.toInt()
        return max(max(optimalWidth, minWidth), desiredWidth)
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

    /**
     * Вычисляет размеры контента для отрисовки.
     *
     * @return Pair<ширина, высота> в пикселях
     */
    fun getContentSize(): Pair<Int, Int> {
        val width = calculateOptimalWidth()
        val height = getContentHeight(width.toFloat())
        Log.d("TitleSubtitleCard", "getContentSize: $width, $height")
        return Pair(width, height)
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

        // Если нет заголовка, возвращаем высоту контента равную нулю
        if (!state.hasTitle()) {
            return 0
        }

        // Высота заголовка
        val titleHeight = calculateTextHeight(state.title, titlePaint, width, state.maxTitleLines)
        totalHeight += titleHeight

        // Отступ между текстами только если есть подзаголовок
        if (state.hasSubtitle()) {
            totalHeight += state.spacing

            val subtitleHeight =
                calculateTextHeight(state.subtitle, subtitlePaint, width, state.maxSubtitleLines)
            totalHeight += subtitleHeight
        }

        return totalHeight.toInt()
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
        maxLines: Int
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


    /**
     * Обновляет кисть для заголовка в соответствии с текущим стилем и состоянием фокуса.
     */
    private fun updateTitlePaint() {
        styledTextView.apply {
            updatePaints { oldState ->
                oldState.copy(
                    titleFocused = applyStyleToTextPaint(
                        titleStyle.styleFocused,
                        paints.titleFocused
                    ),
                    titleUnfocused = applyStyleToTextPaint(
                        titleStyle.styleUnfocused,
                        paints.titleUnfocused
                    ),
                    subtitleFocused = applyStyleToTextPaint(
                        subtitleStyle.styleFocused,
                        paints.subtitleFocused
                    ),
                    subtitleUnfocused = applyStyleToTextPaint(
                        subtitleStyle.styleUnfocused,
                        paints.subtitleUnfocused
                    )
                )
            }
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

    data class State(
        val title: String = "",
        val subtitle: String ="",
        val spacing: Int = 0,
        val maxTitleLines: Int = 1,
        val maxSubtitleLines: Int = 1,
        val isFocused: Boolean = false,
        val paddings: Paddings = Paddings()
    ) {
        fun hasTitle() = title.isNotEmpty()

        fun hasSubtitle() = subtitle.isNotEmpty()
    }

    private data class Paints(
        val titleFocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val titleUnfocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val subtitleFocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG),
        val subtitleUnfocused: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    )

    data class Paddings(
        val top: Int = 32,
        val right: Int =32,
        val bottom: Int = 32,
        val left: Int = 32
    ) {
        fun horizontal() = left + right

        fun vertical() = top + bottom
    }

}
