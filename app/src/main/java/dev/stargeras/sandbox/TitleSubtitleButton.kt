package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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

    private val titlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.pc_label_text_unfocused, context.theme)
        val tv = TextView(context)
        textSize = tv.textSize
        typeface = tv.typeface
    }

    private val subtitlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.pc_label_text_unfocused, context.theme)
        val tv = TextView(context)
        textSize = tv.textSize
        typeface = tv.typeface
    }

    private var titleTextAppearanceResId: Int = 0
    private var subtitleTextAppearanceResId: Int = 0
    private var titleLayout: StaticLayout? = null
    private var subtitleLayout: StaticLayout? = null

    // Отступ между заголовком и подзаголовком
    private val titleSubtitleSpacingPx: Int = resources.getDimensionPixelSize(R.dimen.pc_title_subtitle_spacing)
    private var verticalPaddingPx: Int = resources.getDimensionPixelSize(R.dimen.pc_vertical_padding)
    private var horizontalPaddingPx: Int = resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding)

    // Цвета текста
    private val textColorUnfocused: Int = resources.getColor(R.color.pc_label_text_unfocused, context.theme)
    private val textColorFocused: Int = resources.getColor(R.color.pc_label_text_focused, context.theme)

    init {
        // Читаем атрибуты
        context.withStyledAttributes(attrs, R.styleable.LabelTextView) {
            titleText = getString(R.styleable.LabelTextView_pc_titleText)
            subtitleText = getString(R.styleable.LabelTextView_pc_subtitleText)
            titleTextAppearanceResId = getResourceId(R.styleable.LabelTextView_pc_titleTextAppearance, 0)
            subtitleTextAppearanceResId = getResourceId(R.styleable.LabelTextView_pc_subtitleTextAppearance, 0)
            verticalPaddingPx = getDimensionPixelSize(R.styleable.LabelTextView_pc_verticalPadding, verticalPaddingPx)
            horizontalPaddingPx = getDimensionPixelSize(R.styleable.LabelTextView_pc_horizontalPadding, horizontalPaddingPx)
        }

        // Применяем textAppearance, если заданы
        if (titleTextAppearanceResId != 0) {
            TextView(context).apply {
                setTextAppearance(titleTextAppearanceResId)
                titlePaint.color = currentTextColor
                titlePaint.textSize = textSize
                titlePaint.typeface = typeface
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
        val color = if (hasFocus()) textColorFocused else textColorUnfocused
        titlePaint.color = color
        subtitlePaint.color = color
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val color = if (gainFocus) textColorFocused else textColorUnfocused
        titlePaint.color = color
        subtitlePaint.color = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 1) Считаем требуемые размеры контента на базе текущих текстов
        val titleTextWidth = titleText?.let { titlePaint.measureText(it.toString()) } ?: 0f
        val subtitleTextWidth = subtitleText?.let { subtitlePaint.measureText(it.toString()) } ?: 0f
        val contentWidthPx = (kotlin.math.max(titleTextWidth, subtitleTextWidth).toInt() + horizontalPaddingPx + horizontalPaddingPx)

        val titleTextHeight = if (!titleText.isNullOrEmpty()) titlePaint.fontMetricsInt.let { it.bottom - it.top } else 0
        val subtitleTextHeight = if (!subtitleText.isNullOrEmpty()) subtitlePaint.fontMetricsInt.let { it.bottom - it.top } else 0
        val spacing = if (!titleText.isNullOrEmpty() && !subtitleText.isNullOrEmpty()) titleSubtitleSpacingPx else 0
        val contentHeightPx = verticalPaddingPx + titleTextHeight + spacing + subtitleTextHeight + verticalPaddingPx

        // 2) Обновляем базовые размеры прямоугольника под контент
        currentContentWidthPx = contentWidthPx
        currentContentHeightPx = contentHeightPx

        // 3) Строим лэйауты на ширину контента, чтобы избежать переноса
        // Для текстового layout доступная ширина = общая минус горизонтальные паддинги
        val layoutWidth = (currentContentWidthPx ?: rectBaseWidthPx) - horizontalPaddingPx - horizontalPaddingPx
        buildLayouts(layoutWidth)

        // 4) Доверим измерение базовому классу с обновлёнными параметрами
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun buildLayouts(availableWidth: Int) {
        val contentW = availableWidth.coerceAtLeast(0)
        val t = titleText
        titleLayout = if (!t.isNullOrEmpty() && contentW > 0) {
            StaticLayout.Builder
                .obtain(t, 0, t.length, titlePaint, contentW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
        } else null

        val s = subtitleText
        subtitleLayout = if (!s.isNullOrEmpty() && contentW > 0) {
            StaticLayout.Builder
                .obtain(s, 0, s.length, subtitlePaint, contentW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
        } else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rectW = contentWidth().toFloat()
        val rectH = contentHeight().toFloat()

        val rectLeft = (width - rectW) / 2f
        val rectTop = (height - rectH) / 2f

        val hasTitle = titleLayout != null
        val hasSubtitle = subtitleLayout != null

        if (!hasTitle && !hasSubtitle) return

        val titleH = titleLayout?.height ?: 0
        val subtitleH = subtitleLayout?.height ?: 0
        val spacing = if (hasTitle && hasSubtitle) titleSubtitleSpacingPx else 0
        val blockH = verticalPaddingPx + titleH + spacing + subtitleH + verticalPaddingPx

        var y = rectTop + (rectH - blockH) / 2f + verticalPaddingPx
        val xBase = rectLeft + horizontalPaddingPx

        titleLayout?.let { layout ->
            // Выравнивание по левому краю базовой области
            val x = xBase
            canvas.withTranslation(x, y) { layout.draw(this) }
            y += layout.height + spacing
        }

        subtitleLayout?.let { layout ->
            val x = xBase
            canvas.withTranslation(x, y) { layout.draw(this) }
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

    fun setTitleTextAppearance(resId: Int) {
        if (resId == 0) return
        titleTextAppearanceResId = resId
        TextView(context).apply {
            setTextAppearance(resId)
            titlePaint.color = currentTextColor
            titlePaint.textSize = textSize
            titlePaint.typeface = typeface
        }
        requestLayout()
        invalidate()
    }

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


