package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation


/**
 * Вью, наследующаяся от BaseFocusableCanvasView, рисует однострочный/многострочный текст
 * с отступом 16dp слева от базового прямоугольника и вертикальным центрированием.
 * Цвет текста инвертируется по фокусу: без фокуса — белый, с фокусом — чёрный.
 * Стили текста можно назначить через textAppearance.
 */
class TitleSubtitleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseFocusableCanvasView(context, attrs, defStyleAttr) {

    // Параметры
    private var titleText: CharSequence? = null
    private var subtitleText: CharSequence? = null

    private var maxTitleLines: Int = 1
    private var maxSubtitleLines: Int = 1


    private val unfocusedTitlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

    private val focusedTitlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

    private val subtitlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

    private fun isPaddingUnsetPerSide(): Boolean {
        // Считаем, что если хотя бы один из per-side отличен от дефолта из ресурсов, то он задан явно
        // Здесь используем простую эвристику: если все равны дефолтам, и при этом заданы групповые — применим групповые
        val topDefaultPadding = resources.getDimensionPixelSize(R.dimen.pc_padding_top)
        val bottomDefaultPadding = resources.getDimensionPixelSize(R.dimen.pc_padding_bottom)
        val startDefaultPadding = resources.getDimensionPixelSize(R.dimen.pc_padding_start)
        val endDefaultPadding = resources.getDimensionPixelSize(R.dimen.pc_padding_end)

        val allAreDefault =
            paddingTopPx == topDefaultPadding && paddingBottomPx == bottomDefaultPadding && paddingStartPx == startDefaultPadding && paddingEndPx == endDefaultPadding
        return allAreDefault
    }

    var focusedTitleTextAppearanceResId: Int = 0
        set(value) {
            if (field == value || value == 0) return

            field = value
            TextView(context).apply {
                setTextAppearance(field)
                focusedTitlePaint.textSize = textSize
                focusedTitlePaint.typeface = typeface
                focusedTitlePaint.color = currentTextColor
            }

            requestLayout()
            invalidate()
        }

    private var unfocusedTitleTextAppearanceResId: Int = 0
        set(value) {
            if (field == value || value == 0) return

            field = value
            TextView(context).apply {
                setTextAppearance(field)
                unfocusedTitlePaint.textSize = textSize
                unfocusedTitlePaint.typeface = typeface
                unfocusedTitlePaint.color = currentTextColor
            }

            requestLayout()
            invalidate()
        }

    private var subtitleTextAppearanceResId: Int = 0

    private var titleLayout: StaticLayout? = null

    private var subtitleLayout: StaticLayout? = null

    // Отступ между заголовком и подзаголовком
    private val titleSubtitleSpacingPx: Int =
        resources.getDimensionPixelSize(R.dimen.pc_title_subtitle_spacing)

    private var verticalPaddingPx: Int =
        resources.getDimensionPixelSize(R.dimen.pc_vertical_padding)
    private var horizontalPaddingPx: Int =
        resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding)

    private var paddingTopPx: Int = resources.getDimensionPixelSize(R.dimen.pc_padding_top)
    private var paddingBottomPx: Int = resources.getDimensionPixelSize(R.dimen.pc_padding_bottom)
    private var paddingStartPx: Int = resources.getDimensionPixelSize(R.dimen.pc_padding_start)
    private var paddingEndPx: Int = resources.getDimensionPixelSize(R.dimen.pc_padding_end)

    private val titleColorUnfocused: Int =
        resources.getColor(R.color.pc_title_text_unfocused, context.theme)

    private val titleColorFocused: Int =
        resources.getColor(R.color.pc_title_text_focused, context.theme)

    private val subtitleColorUnfocused: Int =
        resources.getColor(R.color.pc_subtitle_text_unfocused, context.theme)

    private val subtitleColorFocused: Int =
        resources.getColor(R.color.pc_subtitle_text_focused, context.theme)

    init {
        // Читаем атрибуты
        context.withStyledAttributes(attrs, R.styleable.TitleSubtitleButton) {
            titleText = getString(R.styleable.TitleSubtitleButton_pc_titleText)

            subtitleText = getString(R.styleable.TitleSubtitleButton_pc_subtitleText)

            focusedTitleTextAppearanceResId =
                getResourceId(R.styleable.TitleSubtitleButton_pc_focusedTitleTextAppearance, 0)

            unfocusedTitleTextAppearanceResId =
                getResourceId(R.styleable.TitleSubtitleButton_pc_unfocusedTitleTextAppearance, 0)

            subtitleTextAppearanceResId =
                getResourceId(R.styleable.TitleSubtitleButton_pc_subtitleTextAppearance, 0)

            verticalPaddingPx = getDimensionPixelSize(
                R.styleable.TitleSubtitleButton_pc_verticalPadding,
                verticalPaddingPx
            )

            horizontalPaddingPx = getDimensionPixelSize(
                R.styleable.TitleSubtitleButton_pc_horizontalPadding,
                horizontalPaddingPx
            )

            paddingTopPx =
                getDimensionPixelSize(R.styleable.TitleSubtitleButton_pc_paddingTop, paddingTopPx)

            paddingBottomPx = getDimensionPixelSize(
                R.styleable.TitleSubtitleButton_pc_paddingBottom,
                paddingBottomPx
            )

            paddingStartPx = getDimensionPixelSize(
                R.styleable.TitleSubtitleButton_pc_paddingStart,
                paddingStartPx
            )

            paddingEndPx =
                getDimensionPixelSize(R.styleable.TitleSubtitleButton_pc_paddingEnd, paddingEndPx)

            maxTitleLines = getInt(R.styleable.TitleSubtitleButton_pc_titleMaxLines, 1)

            maxSubtitleLines = getInt(R.styleable.TitleSubtitleButton_pc_subtitleMaxLines, 1)
        }

        // Fallback: если заданы групповые паддинги, но не заданы поканальные — применяем групповые
        if (isPaddingUnsetPerSide()) {
            paddingTopPx = verticalPaddingPx
            paddingBottomPx = verticalPaddingPx
            paddingStartPx = horizontalPaddingPx
            paddingEndPx = horizontalPaddingPx
        }

        // Применяем textAppearance, если заданы
        if (unfocusedTitleTextAppearanceResId != 0) {
            TextView(context).apply {
                setTextAppearance(unfocusedTitleTextAppearanceResId)
                unfocusedTitlePaint.color = currentTextColor
                unfocusedTitlePaint.textSize = textSize
                unfocusedTitlePaint.typeface = typeface
            }
        }

        if (subtitleTextAppearanceResId != 0) {
            TextView(context).apply {
                setTextAppearance(subtitleTextAppearanceResId)
                subtitlePaint.color = currentTextColor
                subtitlePaint.textSize = textSize
                subtitlePaint.typeface = typeface
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Приводим цвет текста к актуальному состоянию фокуса
        val focused = hasFocus()
        unfocusedTitlePaint.color = if (focused) titleColorFocused else titleColorUnfocused
        subtitlePaint.color = if (focused) subtitleColorFocused else subtitleColorUnfocused
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        unfocusedTitlePaint.color = if (gainFocus) titleColorFocused else titleColorUnfocused
        subtitlePaint.color = if (gainFocus) subtitleColorFocused else subtitleColorUnfocused
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth =
            currentContentWidthPx ?: resources.getDimensionPixelSize(R.dimen.pc_default_width)
        val measuredW = resolveSize(desiredWidth, widthMeasureSpec)

        buildLayouts(measuredW)

        val titleH = titleLayout?.height ?: 0
        val subtitleH = subtitleLayout?.height ?: 0
        val spacing = if (titleH > 0 && subtitleH > 0) titleSubtitleSpacingPx else 0
        val desiredHeight = titleH + spacing + subtitleH

        val measuredH = resolveSize(desiredHeight, heightMeasureSpec)

        super.onMeasure(measuredW, measuredH)
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentWidth = availableWidth - paddingLeft - paddingRight
        if (contentWidth <= 0) {
            titleLayout = null
            subtitleLayout = null
            return
        }

        titleLayout = makeLayout(titleText, getTitlePaint(), maxTitleLines, contentWidth)
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

        val rectWidth = contentWidth().toFloat()
        val rectHeight = contentHeight().toFloat()

        val rectLeft = (width - rectWidth) / 2f
        val rectTop = (height - rectHeight) / 2f

        val hasTitle = titleLayout != null
        val hasSubtitle = subtitleLayout != null

        if (!hasTitle && !hasSubtitle)
            return

        val titleHeight = titleLayout?.height ?: 0
        val subtitleHeight = subtitleLayout?.height ?: 0
        val spacing = if (hasTitle && hasSubtitle) titleSubtitleSpacingPx else 0

        // Важно: не компенсируем масштаб Canvas. Точки (rectLeft, rectTop) и отступы
        // будут масштабироваться вместе с View вокруг pivot, сохраняя выравнивание.
        val blockHeight = paddingTopPx + titleHeight + spacing + subtitleHeight + paddingBottomPx

        var y = rectTop + (rectHeight - blockHeight) / 2f + paddingTopPx

        val xBase = rectLeft + paddingStartPx

        titleLayout?.let { layout ->
            val x = xBase
            canvas.withTranslation(x, y) { layout.draw(this) }
            y += layout.height + spacing
        }

        subtitleLayout?.let { layout ->
            val x = xBase
            canvas.withTranslation(x, y) { layout.draw(this) }
        }
    }

    private fun getTitlePaint(): TextPaint =
        if (isFocused) focusedTitlePaint else unfocusedTitlePaint

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

//    fun setTitleTextAppearance(resId: Int) {
//        if (resId == 0) return
//        unfocusedTitleTextAppearanceResId = resId
//        TextView(context).apply {
//            setTextAppearance(resId)
//            unfocusedTitlePaint.color = currentTextColor
//            unfocusedTitlePaint.textSize = textSize
//            unfocusedTitlePaint.typeface = typeface
//        }
//        requestLayout()
//        invalidate()
//    }

    fun setSubtitleTextAppearance(resId: Int) {
        if (resId == 0) return
        subtitleTextAppearanceResId = resId
        TextView(context).apply {
            setTextAppearance(resId)
            subtitlePaint.color = currentTextColor
            subtitlePaint.textSize = textSize
            subtitlePaint.typeface = typeface
        }
        requestLayout()
        invalidate()
    }
}


