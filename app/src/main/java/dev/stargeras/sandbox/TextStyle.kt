package dev.stargeras.sandbox

import androidx.annotation.StyleRes

/**
 * Класс для хранения стилей текста в разных состояниях (фокус/без фокуса).
 * Содержит ресурсы для цвета, размера шрифта и стиля текста.
 */
data class TextStyle(
    /** Ресурс цвета текста при фокусе */
    @StyleRes val styleFocused: Int,

    /** Ресурс цвета текста без фокуса */
    @StyleRes val styleUnfocused: Int,
)
