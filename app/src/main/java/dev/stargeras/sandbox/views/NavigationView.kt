package dev.stargeras.sandbox.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import dev.stargeras.sandbox.R
import dev.stargeras.sandbox.drawers.Paddings
import dev.stargeras.sandbox.drawers.RectangleDrawer
import dev.stargeras.sandbox.drawers.ImageDrawer
import dev.stargeras.sandbox.drawers.TitleSubtitleDrawer
import dev.stargeras.sandbox.views.utils.HorizontalAlignment
import dev.stargeras.sandbox.views.utils.VerticalAlignment

class NavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rectangleDrawer by lazy { RectangleDrawer(context, this) }

    private val titleSubtitleDrawer by lazy { TitleSubtitleDrawer(context, this) }

    private val arrowDrawer by lazy { ImageDrawer(context, this) }

    private val rectangleWidthPx: Int =
        context.resources.getDimensionPixelSize(R.dimen.pc_min_content_width)

    var state: State = State()
        private set

    private var internalState: InternalState = InternalState(
        focusedNavigationIconRes = R.drawable.img_right_arrow_focused,
        unfocusedNavigationIconRes = R.drawable.img_right_arrow_unfocused,
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false

        rectangleDrawer.updateState { oldState -> oldState.copy(focusScalePercent = 0.03f) }

        arrowDrawer.updateState { oldState ->
            oldState.copy(
                paddings = Paddings(
                    top = 0,
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                    bottom = 0,
                    left = 0
                ),
                verticalAlignment = VerticalAlignment.CENTER,
                horizontalAlignment = HorizontalAlignment.RIGHT,
                scaleType = ImageDrawer.ScaleType.CENTER_CROP
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

        arrowDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Вычисляем ширину и высоту для заголовка и подзаголовка
        val measuredTitle = titleSubtitleDrawer.measure(
            rectangleWidthPx,
            titleSubtitleDrawer.getContentHeight()
        )

        // Вычисляем ширину и высоту для прямоугольника по ширине и высоте заголовка и подзаголовка
        val measuredRectangle = rectangleDrawer.measure(
            rectangleWidthPx,
            measuredTitle.height
        )

        titleSubtitleDrawer.updateState { oldState ->
            oldState.copy(
                parentWidth = measuredRectangle.width,
                parentHeight = measuredRectangle.height
            )
        }

        // Отрисовка переключателя
        arrowDrawer.apply {
            updateState { oldState ->
                oldState.copy(
                    width = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_width),
                    height = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_height),
                    parentWidth = rectangleWidthPx,
                    parentHeight = measuredRectangle.height,
                )
            }

            measure(
                context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_width),
                context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_height)
            )
        }

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
                isFocused = state.isFocused,
                verticalAlignment = VerticalAlignment.CENTER
            )
        }

        arrowDrawer.updateState { oldState ->
            oldState.copy(
                focusedResourceId = internalState.focusedNavigationIconRes,
                unfocusedResourceId = internalState.unfocusedNavigationIconRes,
                verticalAlignment = VerticalAlignment.CENTER,
                horizontalAlignment = HorizontalAlignment.RIGHT,
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

    private data class InternalState(
        val focusedNavigationIconRes: Int = 0,
        val unfocusedNavigationIconRes: Int = 0,
    )

}