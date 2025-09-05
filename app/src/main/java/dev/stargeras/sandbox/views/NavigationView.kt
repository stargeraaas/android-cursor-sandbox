package dev.stargeras.sandbox.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import dev.stargeras.sandbox.R
import dev.stargeras.sandbox.drawers.ImageLayoutRender
import dev.stargeras.sandbox.views.utils.Paddings
import dev.stargeras.sandbox.drawers.RectangleLayoutRender
import dev.stargeras.sandbox.drawers.TitleSubtitleLayoutRender
import dev.stargeras.sandbox.views.utils.HorizontalAlignment
import dev.stargeras.sandbox.views.utils.VerticalAlignment

class NavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseCanvasView(context, attrs, defStyleAttr) {

    private val backgroundCardDrawer by lazy { RectangleLayoutRender(context, this) }

    private val textDrawer by lazy { TitleSubtitleLayoutRender(context, this) }

    private val arrowImageDrawer by lazy { ImageLayoutRender(context, this) }

    var state: State = State()
        private set

    override var viewState: ViewState = ViewState(
        size = Size(
            width = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_width),
            height = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_height)
        ),
        isFocused = false
    )

    private var internalState: InternalState = InternalState()

    init {
        backgroundCardDrawer.updateState { oldState -> oldState.copy(focusScalePercent = 0.03f) }

        arrowImageDrawer.updateState { oldState ->
            oldState.copy(
                paddings = Paddings(
                    top = 0,
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                    bottom = 0,
                    left = 0
                ),
                verticalAlignment = VerticalAlignment.CENTER,
                horizontalAlignment = HorizontalAlignment.RIGHT,
            )
        }

        textDrawer.updateState { oldState ->
            oldState.copy(
                paddings = Paddings(
                    top = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                    bottom = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    left = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Отрисовка карточки
        backgroundCardDrawer.draw(canvas)
        // Отрисовка текста
        textDrawer.draw(canvas)
        // Отрисовка стрелки
        arrowImageDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Вычисляем ширину и высоту для заголовка и подзаголовка
        val measuredTitle = textDrawer.measure(
            desiredWidth = viewState.size.width,
            desiredHeight = textDrawer.getContentHeight(),
            parentWidth = viewState.size.width,
            parentHeight = viewState.size.height
        )

        // Вычисляем ширину и высоту для прямоугольника по ширине и высоте заголовка и подзаголовка
        val measuredRectangle = backgroundCardDrawer.measure(
            desiredWidth = viewState.size.width,
            desiredHeight = measuredTitle.height,
            parentWidth = viewState.size.width,
            parentHeight = viewState.size.height
        )

        // Отрисовка переключателя
        arrowImageDrawer.apply {
            measure(
                desiredWidth = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_width),
                desiredHeight = context.resources.getDimensionPixelSize(R.dimen.pc_navigation_icon_height),
                parentWidth = viewState.size.width,
                parentHeight = measuredRectangle.height,
            )
        }

        resolveAndSetMeasuring(
            measuredRectangle.width, widthMeasureSpec,
            measuredRectangle.height, heightMeasureSpec
        )
    }

    override fun updateFocusState(focused: Boolean) {
        updateState { oldState -> oldState.copy(isFocused = focused) }
    }

    fun updateState(newState: (State) -> State) {
        state = newState.invoke(state)

        // Обновляем внутреннее состояние прямоугольника
        backgroundCardDrawer.updateState { oldState ->
            oldState.copy(
                isFocused = state.isFocused
            )
        }

        // Обновляем внутреннее состояние заголовка и подзаголовка
        textDrawer.updateState { oldState ->
            oldState.copy(
                title = state.title,
                subtitle = state.subtitle,
                isFocused = state.isFocused,
                verticalAlignment = VerticalAlignment.CENTER
            )
        }

        // Обновляем внутреннее стрелки навигации
        arrowImageDrawer.updateState { oldState ->
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
        val isFocused: Boolean = false,
    )

    private data class InternalState(
        val focusedNavigationIconRes: Int = R.drawable.img_right_arrow_focused,
        val unfocusedNavigationIconRes: Int = R.drawable.img_right_arrow_unfocused,
    )
}