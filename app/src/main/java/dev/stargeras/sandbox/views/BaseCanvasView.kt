package dev.stargeras.sandbox.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

abstract class BaseCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    protected abstract var viewState: ViewState

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        defaultFocusHighlightEnabled = false
    }

    protected fun updateViewSizeState(newState: (ViewState) -> ViewState) {
        viewState = newState(viewState)
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

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        updateFocusState(gainFocus)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateFocusState(isFocused)
    }

    protected fun resolveAndSetMeasuring(
        measuredWidth: Int,
        measuredWidthSpec: Int,
        measuredHeight: Int,
        measuredHeightSpec: Int,
    ) {
        val measuredWidth = resolveSize(measuredWidth, measuredWidthSpec)
        val measuredHeight = resolveSize(measuredHeight, measuredHeightSpec)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /** Обновление состояния фокуса */
    protected abstract fun updateFocusState(focused: Boolean)

    data class ViewState(
        val size: Size,
        val isFocused: Boolean,
    )

    data class Size(
        val width: Int,
        val height: Int,
    )
}