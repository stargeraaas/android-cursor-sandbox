package dev.stargeras.sandbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
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
                maxWidth = context.resources.getDimensionPixelSize(R.dimen.pc_max_content_width),
                paddings = Paddings(
                    top = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                    bottom = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    left = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                )
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                requestFocus()
                return false
            }
            MotionEvent.ACTION_UP -> {
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
        // Вычисляем ширину и высоту для заголовка и подзаголовка
        val measuredTitle = titleSubtitleDrawer.measure(rectangleWidthPx, titleSubtitleDrawer.getContentHeight())

        val measuredRectangle = rectangleDrawer.measure(rectangleWidthPx, measuredTitle.height)

        val measuredWidth = resolveSize(measuredRectangle.width, widthMeasureSpec)
        val measuredHeight = resolveSize(measuredRectangle.height, heightMeasureSpec)

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

    /** Обновление состояния фокуса */
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

    /** Класс состояния текущей View */
    data class State(
        val title: String = "",
        val subtitle: String = "",
        val isFocused: Boolean = false
    )
}