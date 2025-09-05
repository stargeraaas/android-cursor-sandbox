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

class ToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseCanvasView(context, attrs, defStyleAttr) {

    // Слушатель состояния переключателя
    var toggleStateListener: ToggleStateListener? = null

    private val cardDrawer by lazy { RectangleLayoutRender(context, this) }

    private val textDrawer by lazy { TitleSubtitleLayoutRender(context, this) }

    private val toggleImageDrawer by lazy { ImageLayoutRender(context, this) }

    var state: State = State()
        private set

    private var internalState: InternalState = InternalState()

    private val titleViewWidth = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_title_width)

    override var viewState: ViewState = ViewState(
        size = Size(
            width = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_view_width),
            height = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_view_height)
        ),
        isFocused = false
    )

    init {
        setOnClickListener {
            updateState { oldState -> oldState.copy(isChecked = !oldState.isChecked) }
            toggleStateListener?.onToggleStateChange(state.isChecked)
        }

        // Инициализация состояния прямоугольника
        cardDrawer.updateState { oldState -> oldState.copy(focusScalePercent = 0.03f) }

        // Инициализация состояния заголовка и подзаголовка
        textDrawer.updateState { oldState ->
            oldState.copy(
                paddings = Paddings(
                    top = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    right = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_title_padding_right),
                    bottom = context.resources.getDimensionPixelSize(R.dimen.pc_vertical_padding),
                    left = context.resources.getDimensionPixelSize(R.dimen.pc_horizontal_padding),
                )
            )
        }

        // Инициализация состояния переключателя
        toggleImageDrawer.updateState { oldState ->
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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Отрисовка карточки
        cardDrawer.draw(canvas)

        // Отрисовка заголовка и подзаголовка
        textDrawer.draw(canvas)

        // Отрисовка переключателя
        toggleImageDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Вычисляем ширину и высоту для заголовка и подзаголовка
        val measuredTitle = textDrawer.measure(
            desiredWidth = titleViewWidth,
            desiredHeight = textDrawer.getContentHeight(),
            parentWidth = viewState.size.width,
            parentHeight = viewState.size.height
        )

        // Вычисляем ширину и высоту для прямоугольника по ширине и высоте заголовка и подзаголовка
        val measuredRectangle = cardDrawer.measure(
            desiredWidth = viewState.size.width,
            desiredHeight = measuredTitle.height,
            parentWidth = viewState.size.width,
            parentHeight = viewState.size.height
        )

        // Отрисовка переключателя
        toggleImageDrawer.apply {
            measure(
                desiredWidth = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_view_width),
                desiredHeight = context.resources.getDimensionPixelSize(R.dimen.pc_toggle_view_height),
                parentWidth = measuredRectangle.width,
                parentHeight = measuredRectangle.height
            )
        }

        resolveAndSetMeasuring(
            measuredWidth = measuredRectangle.width,
            measuredWidthSpec = widthMeasureSpec,
            measuredHeight = measuredRectangle.height,
            measuredHeightSpec = heightMeasureSpec
        )
    }

    override fun updateFocusState(focused: Boolean) {
        updateState { oldState -> oldState.copy(isFocused = focused) }
    }

    fun updateState(newState: (State) -> State) {
        val oldState = state
        val newState = newState.invoke(state)

        if (oldState == newState) {
            return
        }

        state = newState
        cardDrawer.updateState { oldState ->
            oldState.copy(
                isFocused = state.isFocused
            )
        }

        textDrawer.updateState { oldState ->
            oldState.copy(
                title = state.title,
                subtitle = state.subtitle,
                isFocused = state.isFocused
            )
        }

        updateToggleDrawerState()
    }

    private fun updateToggleDrawerState() {
        toggleImageDrawer.updateState { oldState ->
            // Обновляем состояние переключателя
            val toggleImageState = if (state.isChecked && state.isFocused) {
                oldState.copy(
                    focusedResourceId = internalState.focusedCheckedResourceId,
                    unfocusedResourceId = internalState.unfocusedCheckedResourceId
                )
            } else if (state.isChecked && !state.isFocused) {
                oldState.copy(
                    focusedResourceId = internalState.focusedCheckedResourceId,
                    unfocusedResourceId = internalState.unfocusedCheckedResourceId
                )
            } else {
                oldState.copy(
                    focusedResourceId = internalState.focusedUncheckedResourceId,
                    unfocusedResourceId = internalState.focusedUncheckedResourceId
                )
            }

            toggleImageState.copy(
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
        val isChecked: Boolean = false,
    )

    private data class InternalState(
        val focusedCheckedResourceId: Int = R.drawable.img_toggle_on,
        val unfocusedCheckedResourceId: Int = R.drawable.img_toggle_on,
        val focusedUncheckedResourceId: Int = R.drawable.img_toggle_off,
        val unfocusedUncheckedResourceId: Int = R.drawable.img_toggle_off,
    )

    /** Слушатель состояния переключателя */
    interface ToggleStateListener {
        fun onToggleStateChange(isChecked: Boolean)
    }
}