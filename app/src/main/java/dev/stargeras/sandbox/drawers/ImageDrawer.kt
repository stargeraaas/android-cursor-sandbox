package dev.stargeras.sandbox.drawers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import dev.stargeras.sandbox.views.utils.HorizontalAlignment
import dev.stargeras.sandbox.views.utils.VerticalAlignment
import kotlin.math.max
import kotlin.math.min

/**
 * Drawer для отрисовки ресурсов (drawable) на canvas с поддержкой ScaleType, размеров и отступов.
 * Аналогичен ImageView по функциональности масштабирования.
 */
class ImageDrawer(
    private val context: Context,
    private val targetView: View
) : Drawer {

    /**
     * Типы масштабирования, аналогичные ImageView.ScaleType
     */
    enum class ScaleType {
        /** Масштабирует изображение так, чтобы оно полностью помещалось в View */
        FIT_CENTER,
        /** Масштабирует изображение так, чтобы оно заполнило всю View, обрезая лишнее */
        CENTER_CROP,
        /** Масштабирует изображение так, чтобы оно заполнило всю View, возможно оставляя пустые области */
        CENTER_INSIDE,
        /** Масштабирует изображение по центру без изменения размера */
        CENTER,
        /** Масштабирует изображение по центру, обрезая лишнее */
        CENTER_CROP_TOP,
        /** Масштабирует изображение по центру, обрезая лишнее снизу */
        CENTER_CROP_BOTTOM
    }


    /**
     * Состояние ResourceDrawer
     */
    data class State(
        val focusedResourceId: Int? = null,
        val unfocusedResourceId: Int? = null,
        val width: Int = 0,
        val height: Int = 0,
        val parentWidth: Int = 0,
        val parentHeight: Int = 0,
        val paddings: Paddings = Paddings(0, 0, 0, 0),
        val scaleType: ScaleType = ScaleType.FIT_CENTER,
        val isFocused: Boolean = false,
        val verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
        val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT
    )

    var state: State = State()
        private set

    private var drawable: Drawable? = null
    private val matrix = Matrix()
    private val drawableRect = RectF()
    private val viewRect = RectF()

    /**
     * Обновляет состояние ResourceDrawer
     */
    fun updateState(newState: (State) -> State) {
        val oldState = state
        state = newState(state)

        Log.w(TAG, "updateState: $state")

        // Если изменился ресурс или состояние фокуса, загружаем новый drawable
        if (oldState.focusedResourceId != state.focusedResourceId || 
            oldState.unfocusedResourceId != state.unfocusedResourceId ||
            oldState.isFocused != state.isFocused) {
            loadDrawable()
        }

        // Пересчитываем матрицу трансформации
        updateMatrix()

        targetView.apply {
            invalidate()
            requestLayout()
        }
    }

    /**
     * Загружает drawable по ID ресурса в зависимости от состояния фокуса
     */
    private fun loadDrawable() {
        val resourceId = if (state.isFocused) {
            state.focusedResourceId ?: state.unfocusedResourceId
        } else {
            state.unfocusedResourceId ?: state.focusedResourceId
        }

        resourceId?.let { id ->
            try {
                drawable = ContextCompat.getDrawable(context, id)
                drawable?.let { drawable ->
                    // Устанавливаем callback для перерисовки при изменении drawable
                    drawable.callback = targetView
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки drawable с ID: $id", e)
                drawable = null
            }
        } ?: run {
            drawable = null
        }
    }

    /**
     * Обновляет матрицу трансформации в соответствии с ScaleType и выравниванием
     */
    private fun updateMatrix() {
        val drawable = drawable ?: return
        
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        
        if (drawableWidth <= 0 || drawableHeight <= 0) return

        val viewWidth = state.width
        val viewHeight = state.height
        
        if (viewWidth <= 0 || viewHeight <= 0) return

        // Сбрасываем матрицу
        matrix.reset()

        // Вычисляем доступную область для отрисовки (с учетом отступов)
        val availableWidth = viewWidth - state.paddings.horizontal()
        val availableHeight = viewHeight - state.paddings.vertical()

        when (state.scaleType) {
            ScaleType.FIT_CENTER -> {
                val scale = min(
                    availableWidth.toFloat() / drawableWidth,
                    availableHeight.toFloat() / drawableHeight
                )
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                
                val left = calculateHorizontalPosition(availableWidth, scaledWidth)
                val top = calculateVerticalPosition(availableHeight, scaledHeight)
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)
            }
            
            ScaleType.CENTER_CROP -> {
                val scale = max(
                    availableWidth.toFloat() / drawableWidth,
                    availableHeight.toFloat() / drawableHeight
                )
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                
                val left = calculateHorizontalPosition(availableWidth, scaledWidth)
                val top = calculateVerticalPosition(availableHeight, scaledHeight)
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)
            }
            
            ScaleType.CENTER_INSIDE -> {
                val scale = min(
                    availableWidth.toFloat() / drawableWidth,
                    availableHeight.toFloat() / drawableHeight
                ).coerceAtMost(1f) // Не увеличиваем изображение
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                
                val left = calculateHorizontalPosition(availableWidth, scaledWidth)
                val top = calculateVerticalPosition(availableHeight, scaledHeight)
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)
            }
            
            ScaleType.CENTER -> {
                val left = calculateHorizontalPosition(availableWidth, drawableWidth.toFloat())
                val top = calculateVerticalPosition(availableHeight, drawableHeight.toFloat())
                
                matrix.setTranslate(left, top)
            }
            
            ScaleType.CENTER_CROP_TOP -> {
                val scale = max(
                    availableWidth.toFloat() / drawableWidth,
                    availableHeight.toFloat() / drawableHeight
                )
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                
                val left = calculateHorizontalPosition(availableWidth, scaledWidth)
                val top = calculateVerticalPosition(availableHeight, scaledHeight)
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)
            }
            
            ScaleType.CENTER_CROP_BOTTOM -> {
                val scale = max(
                    availableWidth.toFloat() / drawableWidth,
                    availableHeight.toFloat() / drawableHeight
                )
                
                val scaledWidth = drawableWidth * scale
                val scaledHeight = drawableHeight * scale
                
                val left = calculateHorizontalPosition(availableWidth, scaledWidth)
                val top = calculateVerticalPosition(availableHeight, scaledHeight)
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)
            }
        }
    }

    /**
     * Вычисляет горизонтальную позицию в зависимости от выравнивания и размера родителя
     */
    private fun calculateHorizontalPosition(availableWidth: Int, contentWidth: Float): Float {
        Log.d(TAG, "calculateHorizontalPosition: $availableWidth $contentWidth alignment: ${state.horizontalAlignment}")
        return when (state.horizontalAlignment) {
            HorizontalAlignment.LEFT -> {
                if (state.parentWidth > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    state.paddings.left.toFloat()
                } else {
                    // Иначе используем доступную ширину View
                    state.paddings.left.toFloat()
                }
            }
            HorizontalAlignment.CENTER -> {
                if (state.parentWidth > 0) {
                    // Центрируем относительно родительского контейнера
                    (state.parentWidth - contentWidth) / 2
                } else {
                    // Центрируем относительно доступной области View
                    state.paddings.left + (availableWidth - contentWidth) / 2
                }
            }
            HorizontalAlignment.RIGHT -> {
                if (state.parentWidth > 0) {
                    // Позиционируем по правому краю родительского контейнера
                    state.parentWidth - contentWidth - state.paddings.right
                } else {
                    // Позиционируем по правому краю доступной области View
                    state.paddings.left + (availableWidth - contentWidth)
                }
            }
        }
    }

    /**
     * Вычисляет вертикальную позицию в зависимости от выравнивания и размера родителя
     */
    private fun calculateVerticalPosition(availableHeight: Int, contentHeight: Float): Float {
        Log.d(TAG, "calculateVerticalPosition: $availableHeight $contentHeight alignment: ${state.verticalAlignment}")

        return when (state.verticalAlignment) {
            VerticalAlignment.TOP -> {
                if (state.parentHeight > 0) {
                    // Если указан размер родителя, позиционируем относительно него
                    state.paddings.top.toFloat()
                } else {
                    // Иначе используем доступную высоту View
                    state.paddings.top.toFloat()
                }
            }
            VerticalAlignment.CENTER -> {
                if (state.parentHeight > 0) {
                    // Центрируем относительно родительского контейнера
                    (state.parentHeight - contentHeight) / 2
                } else {
                    // Центрируем относительно доступной области View
                    state.paddings.top + (availableHeight - contentHeight) / 2
                }
            }
            VerticalAlignment.BOTTOM -> {
                if (state.parentHeight > 0) {
                    // Позиционируем по нижнему краю родительского контейнера
                    state.parentHeight - contentHeight - state.paddings.bottom
                } else {
                    // Позиционируем по нижнему краю доступной области View
                    state.paddings.top + (availableHeight - contentHeight)
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        val drawable = drawable ?: return
        
        canvas.save()
        canvas.concat(matrix)
        
        // Рисуем drawable в его естественных границах
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        
        canvas.restore()
    }

    override fun measure(desiredWidth: Int, desiredHeight: Int): Drawer.MeasureResult {
        val drawable = drawable

        if (drawable == null) {
            // Если drawable не загружен, используем минимальные размеры
            val totalWidth = state.paddings.horizontal()
            val totalHeight = state.paddings.vertical()
            return Drawer.MeasureResult(totalWidth, totalHeight)
        }

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            // Если drawable не имеет размеров, используем минимальные размеры
            val totalWidth = state.paddings.horizontal()
            val totalHeight = state.paddings.vertical()
            return Drawer.MeasureResult(totalWidth, totalHeight)
        }

        var finalWidth = desiredWidth
        var finalHeight = desiredHeight

        when (state.scaleType) {
            ScaleType.FIT_CENTER -> {
                // Вычисляем размеры с сохранением пропорций
                val scale = min(
                    desiredWidth.toFloat() / drawableWidth,
                    desiredHeight.toFloat() / drawableHeight
                )
                finalWidth = (drawableWidth * scale).toInt()
                finalHeight = (drawableHeight * scale).toInt()
            }
            
            ScaleType.CENTER_CROP -> {
                // Размеры равны желаемым размерам
                finalWidth = desiredWidth
                finalHeight = desiredHeight
            }
            
            ScaleType.CENTER_INSIDE -> {
                // Размеры не больше желаемых, с сохранением пропорций
                val scale = min(
                    desiredWidth.toFloat() / drawableWidth,
                    desiredHeight.toFloat() / drawableHeight
                ).coerceAtMost(1f)
                finalWidth = (drawableWidth * scale).toInt()
                finalHeight = (drawableHeight * scale).toInt()
            }
            
            ScaleType.CENTER -> {
                // Размеры равны размерам drawable
                finalWidth = drawableWidth
                finalHeight = drawableHeight
            }
            
            ScaleType.CENTER_CROP_TOP, ScaleType.CENTER_CROP_BOTTOM -> {
                // Размеры равны желаемым размерам
                finalWidth = desiredWidth
                finalHeight = desiredHeight
            }
        }

        // Добавляем отступы к финальным размерам
        val totalWidth = finalWidth + state.paddings.horizontal()
        val totalHeight = finalHeight + state.paddings.vertical()

        return Drawer.MeasureResult(totalWidth, totalHeight)
    }

    companion object {
        private const val TAG = "ResourceDrawer"
    }
}
