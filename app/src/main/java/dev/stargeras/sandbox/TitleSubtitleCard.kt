package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import dev.stargeras.sandbox.drawers.Paddings
import dev.stargeras.sandbox.drawers.RectangleDrawer
import dev.stargeras.sandbox.drawers.TitleSubtitleDrawer

class TitleSubtitleCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val rectangleDrawer by lazy { RectangleDrawer(context, this) }

    private val titleSubtitleDrawer by lazy { TitleSubtitleDrawer(context, this) }

    private val rectangleWidthPx: Int =
        context.resources.getDimensionPixelSize(R.dimen.pc_min_content_width)

    private var state: State = State()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false

        rectangleDrawer.updateState { oldState -> oldState.copy(focusScalePercent = 0.03f) }

        updateState {
            State(
                title = "Title",
                subtitle = "Subtitle",
                isFocused = false
            )
        }

        titleSubtitleDrawer.updateState { oldState ->
            oldState.copy(
                paddings = Paddings(
                    top = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                    bottom = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    left = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                )
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.w("RectangleDrawer", "onTouch")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Устанавливаем фокус при нажатии
                requestFocus()
                return false
            }
            MotionEvent.ACTION_UP -> {
                // Вызываем performClick при отпускании
                performClick()
                return false
            }
        }
        return super.onTouchEvent(event)
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
            rectangleDrawer.measure(rectangleWidthPx, h) { w, h ->
                totalWidth = w
                totalHeight = h
            }
        }

        val measuredWidth = resolveSize(totalWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(totalHeight, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        updateFocusState(gainFocus)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateFocusState(isFocused)
    }

    private fun updateFocusState(gainFocus: Boolean) {
        updateState { oldState -> oldState.copy(isFocused = gainFocus) }
    }

    fun updateState(newState: (State) -> State) {
        state = newState.invoke(state)

        rectangleDrawer.updateState { oldState ->
            oldState.copy(
                isFocused = state.isFocused
            )
        }

        titleSubtitleDrawer.updateState { oldState ->
            oldState.copy(
                title = state.title,
                subtitle = state.subtitle,
                isFocused = state.isFocused
            )
        }
    }

    data class State(
        val title: String = "",
        val subtitle: String = "",
        val isFocused: Boolean = false
    )

}