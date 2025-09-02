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
import androidx.core.graphics.withTranslation
import dev.stargeras.sandbox.R
import dev.stargeras.sandbox.TextStyle
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

    var state: State = State()
        private set

    var textStyles: TextStyles = TextStyles(
        titleStyle = TextStyle(
            R.style.TextAppearance_PC_Title_Focused,
            R.style.TextAppearance_PC_Title_Unfocused
        ),
        subtitleStyle = TextStyle(
            R.style.TextAppearance_PC_Subtitle_Focused,
            R.style.TextAppearance_PC_Subtitle_Unfocused
        )
    )
        private set(value) {
            field = value
            updateTitlePaint()
        }

    private var paints = Paints()

    private val titlePaint: TextPaint
        get() = if (state.isFocused) paints.titleFocused else paints.titleUnfocused

    private val subtitlePaint: TextPaint
        get() = if (state.isFocused) paints.subtitleFocused else paints.subtitleUnfocused

    private val styledTextView = TextView(context)

    init {
        updateTitlePaint()
    }

    /** Обновляет состояние в соответствии с новыми настройками. */
    fun updateState(newState: (State) -> State) {
        state = newState(state)

        targetView.apply {
            invalidate()
            requestLayout()
        }
    }

    /** Обновляет стили текста в соответствии с новыми настройками. */
    fun updateTextStyles(newTextStyles: (TextStyles) -> TextStyles) {
        textStyles = newTextStyles(textStyles)

        updateTitlePaint()

        targetView.apply {
            invalidate()
            requestLayout()
        }
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
            y += state.spacing
        }

        subtitleLayout?.let { layout ->
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    override fun measure(
        desiredWidth: Int,
        heightMeasureSpec: Int,
        measured: (Int, Int) -> Unit
    ) {

        val measuredWidth = desiredWidth + state.paddings.horizontal()

        buildLayouts(measuredWidth)

        val titleHeight = titleLayout?.height ?: 0
        val subtitleHeight = subtitleLayout?.height ?: 0

        val spacing = if (titleHeight > 0 && subtitleHeight > 0) state.spacing else 0

        val desiredHeight = titleHeight + spacing + subtitleHeight + state.paddings.vertical()

        Log.i("MEASURE", "TitleSubtitle::width=$measuredWidth, height=$desiredHeight")

        measured.invoke(measuredWidth, desiredHeight)
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentWidth = availableWidth
        if (contentWidth <= 0) {
            titleLayout = null
            subtitleLayout = null
            return
        }

        titleLayout = makeLayout(state.title, titlePaint, state.maxTitleLines, contentWidth)
        subtitleLayout =
            makeLayout(state.subtitle, subtitlePaint, state.maxSubtitleLines, contentWidth)
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
        return max(maxWidth.toInt(), state.maxWidth)
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
        return if (state.maxWidth <= 0) {
            calculateOptimalWidth()
        } else {
            min(state.maxWidth, calculateOptimalWidth())
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
        val subtitle: String = "",
        val spacing: Int = 0,
        val maxWidth: Int = 0,
        val maxTitleLines: Int = 1,
        val maxSubtitleLines: Int = 1,
        val isFocused: Boolean = false,
        val paddings: Paddings = Paddings(0, 0, 0, 0)
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

    data class TextStyles(
        val titleStyle: TextStyle,
        val subtitleStyle: TextStyle
    )

}