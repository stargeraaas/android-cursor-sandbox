package dev.stargeras.sandbox

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.stargeras.sandbox.views.NavigationView
import dev.stargeras.sandbox.views.ToggleView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.titleSubtitleCard).requestFocus()
        
        // Демонстрация работы ToggleView
        setupToggleViews()
    }
    
    private fun setupToggleViews() {
        // Настраиваем первый ToggleView
        findViewById<ToggleView>(R.id.navigationView1).apply {
            updateState { oldState ->
                oldState.copy(
                    title = "Toggle 1",
                    subtitle = "Click to toggle state",
                    isChecked = false
                )
            }
        }

        // Настраиваем второй ToggleView
        findViewById<NavigationView>(R.id.navigationView).apply {
            updateState { oldState ->
                oldState.copy(
                    title = "Настроить",
                    isChecked = true
                )
            }
        }
    }
}