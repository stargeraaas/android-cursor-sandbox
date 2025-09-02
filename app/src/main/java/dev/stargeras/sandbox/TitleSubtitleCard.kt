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

        titleSubtitleDrawer.updateState { oldState ->
            oldState.copy(
                title = "Title",
                subtitle = "Subtitle",
                isFocused = false
            )
        }



        titleSubtitleDrawer.setTitleTextStyle(
            TextStyle(
                R.style.TextAppearance_PC_Title_Focused,
                R.style.TextAppearance_PC_Title_Unfocused
            )
        )
        titleSubtitleDrawer.setSubtitleTextStyle(
            TextStyle(
                R.style.TextAppearance_PC_Subtitle_Focused,
                R.style.TextAppearance_PC_Subtitle_Unfocused
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rectangleDrawer.draw(canvas)

        titleSubtitleDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ширина View = ширина прямоугольника (задана кодом)
        val desiredWidth = rectangleWidthPx

        // Высота View = высота текста (заголовок + подзаголовок + отступ)
        val textHeight = titleSubtitleDrawer.getContentSize().second
        val desiredHeight = textHeight

        var totalWidth = 0
        var totalHeight = 0

        titleSubtitleDrawer.measure(desiredWidth, desiredHeight) { w, h ->
            Log.e("MEASURE", "Measured TitleSubtitleDrawer: width=$w height=$h")

            rectangleDrawer.measure(rectangleWidthPx, h) { w, h ->

                Log.e(
                    "MEASURE",
                    "Measured RectangleDrawer: width=$w height=$h"
                )

                totalWidth = w
                totalHeight = h

            }

        }

        val measuredWidth = resolveSize(totalWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(totalHeight, heightMeasureSpec)

        setMeasuredDimension(564, 200)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        rectangleDrawer.isFocused = gainFocus

        titleSubtitleDrawer.updateState { oldSTate -> oldSTate.copy(isFocused = gainFocus) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Приводим цвет к актуальному состоянию фокуса
        rectangleDrawer.isFocused = isFocused
    }

    private var state: State = State()

    fun updateState(newState: (State) -> State) {
        state = newState.invoke(state)
    }

    data class State(
        val title: String = "",
        val subtitle: String = "",
        val isFocused: Boolean = false
    )

}