# ResourceDrawer - Документация

## Описание
`ResourceDrawer` - это реализация интерфейса `Drawer` для отрисовки ресурсов (drawable) на canvas с поддержкой различных типов масштабирования, размеров, отступов и выравнивания.

## Основные возможности

### 1. Типы масштабирования (ScaleType)
- **FIT_CENTER** - масштабирует изображение так, чтобы оно полностью помещалось в View
- **CENTER_CROP** - масштабирует изображение так, чтобы оно заполнило всю View, обрезая лишнее
- **CENTER_INSIDE** - масштабирует изображение так, чтобы оно заполнило всю View, возможно оставляя пустые области
- **CENTER** - масштабирует изображение по центру без изменения размера
- **CENTER_CROP_TOP** - масштабирует изображение по центру, обрезая лишнее сверху
- **CENTER_CROP_BOTTOM** - масштабирует изображение по центру, обрезая лишнее снизу

### 2. Выравнивание
- **Вертикальное выравнивание (VerticalAlignment)**: TOP, CENTER, BOTTOM
- **Горизонтальное выравнивание (HorizontalAlignment)**: LEFT, CENTER, RIGHT
- **Поддержка размеров родительского контейнера**: `parentWidth` и `parentHeight` для точного позиционирования

### 3. Автоматическое переключение ресурсов
- **focusedResourceId** - ресурс для отображения при фокусе
- **unfocusedResourceId** - ресурс для отображения без фокуса
- Автоматическое переключение при изменении состояния `isFocused`

### 4. Настройка параметров
- **Размеры** - можно указать желаемую ширину и высоту
- **Размеры родительского контейнера** - для точного позиционирования относительно родителя
- **Отступы** - настраиваемые отступы через класс `Paddings`
- **Ресурсы** - отдельные ID для состояний фокуса и без фокуса

## Использование

### Базовое использование
```kotlin
val resourceDrawer = ResourceDrawer(context, view)

// Установка ресурсов для разных состояний
resourceDrawer.setFocusedResource(R.drawable.my_image_focused)
resourceDrawer.setUnfocusedResource(R.drawable.my_image_unfocused)

// Или установка обоих ресурсов одновременно
resourceDrawer.setResources(R.drawable.my_image_focused, R.drawable.my_image_unfocused)

// Установка размеров
resourceDrawer.setSize(200, 150)

// Установка размеров родительского контейнера для точного позиционирования
resourceDrawer.setParentSize(400, 300)

// Установка отступов
resourceDrawer.setPaddings(Paddings(16, 16, 16, 16))

// Установка типа масштабирования
resourceDrawer.setScaleType(ResourceDrawer.ScaleType.FIT_CENTER)

// Установка выравнивания
resourceDrawer.setHorizontalAlignment(ResourceDrawer.HorizontalAlignment.RIGHT)
resourceDrawer.setVerticalAlignment(ResourceDrawer.VerticalAlignment.CENTER)

// Или установка обоих выравниваний одновременно
resourceDrawer.setAlignment(
    ResourceDrawer.HorizontalAlignment.RIGHT,
    ResourceDrawer.VerticalAlignment.CENTER
)
```

### Использование через updateState
```kotlin
resourceDrawer.updateState { oldState ->
    oldState.copy(
        focusedResourceId = R.drawable.my_image_focused,
        unfocusedResourceId = R.drawable.my_image_unfocused,
        width = 200,
        height = 150,
        parentWidth = 400,
        parentHeight = 300,
        paddings = Paddings(16, 16, 16, 16),
        scaleType = ResourceDrawer.ScaleType.CENTER_CROP,
        horizontalAlignment = ResourceDrawer.HorizontalAlignment.RIGHT,
        verticalAlignment = ResourceDrawer.VerticalAlignment.TOP
    )
}
```

### Интеграция в кастомную View
```kotlin
class MyCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val resourceDrawer = ResourceDrawer(context, this)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        resourceDrawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec)

        val measuredResource = resourceDrawer.measure(desiredWidth, desiredHeight)
        setMeasuredDimension(measuredResource.width, measuredResource.height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Обновляем размеры в ResourceDrawer
        resourceDrawer.setSize(w, h)
        
        // Обновляем размеры родительского контейнера для точного позиционирования
        resourceDrawer.setParentSize(w, h)
    }
}
```

## Примеры ScaleType

### FIT_CENTER
Изображение масштабируется так, чтобы полностью поместиться в View, сохраняя пропорции.

### CENTER_CROP
Изображение масштабируется так, чтобы заполнить всю View, обрезая лишнее по краям.

### CENTER_INSIDE
Изображение масштабируется так, чтобы поместиться в View, но не увеличивается больше своего исходного размера.

### CENTER
Изображение отображается в центре View без изменения размера.

### CENTER_CROP_TOP / CENTER_CROP_BOTTOM
Изображение масштабируется как CENTER_CROP, но позиционируется по верхнему или нижнему краю соответственно.

## Примеры выравнивания

### Горизонтальное выравнивание
- **LEFT** - изображение прижимается к левому краю
- **CENTER** - изображение центрируется
- **RIGHT** - изображение прижимается к правому краю

### Вертикальное выравнивание
- **TOP** - изображение прижимается к верхнему краю
- **CENTER** - изображение центрируется
- **BOTTOM** - изображение прижимается к нижнему краю

## Выравнивание относительно родительского контейнера

ResourceDrawer поддерживает два режима позиционирования:

### 1. Позиционирование относительно View (по умолчанию)
Если `parentWidth` и `parentHeight` не указаны (равны 0), выравнивание происходит относительно доступной области View с учетом отступов.

### 2. Позиционирование относительно родительского контейнера
Если указаны `parentWidth` и `parentHeight`, выравнивание происходит относительно этих размеров, что позволяет точно позиционировать изображение в родительском контейнере.

```kotlin
// Позиционирование относительно View
resourceDrawer.setAlignment(
    ResourceDrawer.HorizontalAlignment.CENTER,
    ResourceDrawer.VerticalAlignment.CENTER
)

// Позиционирование относительно родительского контейнера
resourceDrawer.setParentSize(400, 300)
resourceDrawer.setAlignment(
    ResourceDrawer.HorizontalAlignment.RIGHT,
    ResourceDrawer.VerticalAlignment.BOTTOM
)
```

## Автоматическое переключение ресурсов

ResourceDrawer автоматически переключает ресурсы в зависимости от состояния фокуса:

```kotlin
// При isFocused = true отображается focusedResourceId
// При isFocused = false отображается unfocusedResourceId

resourceDrawer.setFocused(true)  // Покажет focusedResourceId
resourceDrawer.setFocused(false) // Покажет unfocusedResourceId
```

Если один из ресурсов не указан, используется другой как fallback.

## Особенности реализации

1. **Матрица трансформации** - используется `Matrix` для эффективного масштабирования и позиционирования
2. **Автоматическая перерисовка** - при изменении параметров автоматически вызывается `invalidate()` и `requestLayout()`
3. **Обработка ошибок** - корректная обработка случаев, когда drawable не может быть загружен
4. **Callback поддержка** - drawable получает callback для автоматической перерисовки при изменении
5. **Автоматическое переключение ресурсов** - при изменении состояния фокуса
6. **Гибкое выравнивание** - независимое управление горизонтальным и вертикальным позиционированием
7. **Поддержка родительского контейнера** - точное позиционирование относительно заданных размеров
8. **Адаптивное позиционирование** - автоматический выбор режима позиционирования в зависимости от настроек

## Зависимости
- `androidx.core:core-ktx` - для `ContextCompat.getDrawable()`
- `android.graphics.*` - для работы с Canvas, Matrix, RectF
- `android.graphics.drawable.Drawable` - для работы с drawable ресурсами
