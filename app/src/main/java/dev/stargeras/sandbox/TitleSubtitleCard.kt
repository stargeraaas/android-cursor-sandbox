package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.withTranslation
import kotlin.math.max

class TitleSubtitleCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val rectangleDrawer by lazy { RectangleDrawer(context, this) }

    private val titleSubtitleDrawer by lazy { TitleSubtitleDrawer(context, this) }

    private val rectangleWidthPx: Int = 500
    private val rectangleHeightPx: Int = 200

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false

        rectangleDrawer.focusScalePercent = 0.03f

        titleSubtitleDrawer.titleText = "Title"
        titleSubtitleDrawer.subtitleText = "Subtitle"
        titleSubtitleDrawer.setTitleTextStyle(TextStyle(R.style.TextAppearance_PC_Title_Focused, R.style.TextAppearance_PC_Title_Unfocused))
        titleSubtitleDrawer.setSubtitleTextStyle(TextStyle(R.style.TextAppearance_PC_Subtitle_Focused, R.style.TextAppearance_PC_Subtitle_Unfocused))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.w("TitleSubtitleCard", "onDraw\nwidth= $width height= $height \nrectangleWidthPx= $rectangleWidthPx\n" +
                "textWidth= ${titleSubtitleDrawer.getContentSize().first} textHeight= ${titleSubtitleDrawer.getContentSize().second}")

        // Теперь View имеет размеры прямоугольника
        // Прямоугольник заполняет всю View
        rectangleDrawer.calculateCoordinates(width.toFloat(), height.toFloat(), width, height)
        
        // Рисуем карточку прямоугольника
        rectangleDrawer.drawRectangle(canvas)

        titleSubtitleDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ширина View = ширина прямоугольника (задана кодом)
        val desiredWidth = rectangleWidthPx
        
        // Высота View = высота текста (заголовок + подзаголовок + отступ)
        val textHeight = titleSubtitleDrawer.getContentSize().second
        val desiredHeight = textHeight

        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)

        // Учитываем масштабирование для анимации фокуса
        val scaledValueWidth = (measuredWidth - measuredWidth / rectangleDrawer.currentScale).toInt()
        val scaledWidth = measuredWidth + scaledValueWidth

        val scaledValueHeight = (measuredHeight - measuredHeight / rectangleDrawer.currentScale).toInt()
        val scaledHeight = measuredHeight + scaledValueHeight


        titleSubtitleDrawer.measure(desiredWidth, -1) { w, h ->
            Log.e("TitleSubtitleCard", "Measured TitleSubtitleDrawer: width=$w height=$h")

            setMeasuredDimension(scaledWidth, h)
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        rectangleDrawer.startFocusAnimation(gainFocus)

        titleSubtitleDrawer.updateState { oldSTate -> oldSTate.copy(isFocused = gainFocus) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Приводим цвет к актуальному состоянию фокуса
        rectangleDrawer.updateFocusState(hasFocus())
    }

}