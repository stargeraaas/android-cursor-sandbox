package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.content.res.use
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation

/**
 * Кастомная вью, рисующая заголовок и подзаголовок вручную на Canvas.
 *
 * Особенности:
 * - фиксированная по содержимому высота (wrap_content), ширина задаётся через атрибут/метод
 * - по умолчанию ширина 394px (см. `pc_default_width`)
 * - каждая строка ограничена максимум 3 линиями и обрезается многоточием
 * - стили текста зафиксированы при инициализации и не изменяются через XML
 * - фокусировка отключена
 */
class TitleSubtitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var titleText: CharSequence? = null
    private var subtitleText: CharSequence? = null

    private var maxTitleLines: Int = DEFAULT_MAX_LINES
    private var maxSubtitleLines: Int = DEFAULT_MAX_LINES

    private val titlePaint = TextView(context).let { textView ->
        textView.setTextAppearance(R.style.TextAppearance_PC_Title_Unfocused)

        TextPaint().apply {
            isAntiAlias = true
            color = textView.currentTextColor
            textSize = textView.textSize
            typeface = textView.typeface
        }
    }

    private val subtitlePaint = TextView(context).let { textView ->
        textView.setTextAppearance(R.style.TextAppearance_PC_Subtitle)

        TextPaint().apply {
            isAntiAlias = true
            color = textView.currentTextColor
            textSize = textView.textSize
            typeface = textView.typeface
        }
    }

    private var titleLayout: StaticLayout? = null
    private var subtitleLayout: StaticLayout? = null

    // Вертикальный интервал между заголовком и подзаголовком
    private val lineSpacingPx: Int = resources.getDimensionPixelSize(R.dimen.pc_line_spacing)
    private var fixedWidthPx: Int = resources.getDimensionPixelSize(R.dimen.pc_default_width)


    init {
        isFocusable = false
        isFocusableInTouchMode = false

        context.withStyledAttributes(attrs, R.styleable.TitleSubtitleView) {
            titleText = getString(R.styleable.TitleSubtitleView_pc_titleText)
            subtitleText = getString(R.styleable.TitleSubtitleView_pc_subtitleText)

            maxTitleLines = getInt(R.styleable.TitleSubtitleView_pc_titleMaxLines, DEFAULT_MAX_LINES)
                .coerceAtMost(DEFAULT_MAX_LINES)
            maxSubtitleLines = getInt(R.styleable.TitleSubtitleView_pc_subtitleMaxLines, DEFAULT_MAX_LINES)
                .coerceAtMost(DEFAULT_MAX_LINES)

            val customWidth = getDimensionPixelSize(
                R.styleable.TitleSubtitleView_pc_widthPx,
                -1
            )
            if (customWidth > 0) fixedWidthPx = customWidth
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = fixedWidthPx ?: resources.getDimensionPixelSize(R.dimen.pc_default_width)
        val measuredW = resolveSize(desiredWidth, widthMeasureSpec)

        buildLayouts(measuredW)

        val titleH = titleLayout?.height ?: 0
        val subtitleH = subtitleLayout?.height ?: 0
        val spacing = if (titleH > 0 && subtitleH > 0) lineSpacingPx else 0
        val desiredHeight = titleH + spacing + subtitleH

        val measuredH = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredW, measuredH)
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentWidth = availableWidth - paddingLeft - paddingRight
        if (contentWidth <= 0) {
            titleLayout = null
            subtitleLayout = null
            return
        }

        titleLayout = makeLayout(titleText, titlePaint, maxTitleLines, contentWidth)
        subtitleLayout = makeLayout(subtitleText, subtitlePaint, maxSubtitleLines, contentWidth)
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var y = paddingTop.toFloat()
        val x = paddingLeft.toFloat()

        titleLayout?.let { layout ->
            canvas.withTranslation(x, y) {
                layout.draw(this)
            }
            y += layout.height
        }

        if (titleLayout != null && subtitleLayout != null) {
            y += lineSpacingPx
        }

        subtitleLayout?.let { layout ->
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    // Публичные API
    fun setTitleText(text: CharSequence?) {
        titleText = text
        requestLayout()
        invalidate()
    }

    fun setSubtitleText(text: CharSequence?) {
        subtitleText = text
        requestLayout()
        invalidate()
    }

    fun setTitleMaxLines(lines: Int) {
        maxTitleLines = lines.coerceAtMost(DEFAULT_MAX_LINES)
        requestLayout()
        invalidate()
    }

    fun setSubtitleMaxLines(lines: Int) {
        maxSubtitleLines = lines.coerceAtMost(DEFAULT_MAX_LINES)
        requestLayout()
        invalidate()
    }

    fun setFixedWidth(@Px widthPx: Int) {
        fixedWidthPx = widthPx
        requestLayout()
    }

    override fun setFocusable(focusable: Int) {
        super.setFocusable(View.NOT_FOCUSABLE)
    }

    companion object {
        private const val DEFAULT_MAX_LINES = 3
    }
}

