package com.vachanammusic.artists


import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vachanammusic.artists.databinding.MainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainBinding
    private val intent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
        design()
        binding.button1.background = getGradientDrawable(100, 2, -0x555556, Color.TRANSPARENT)
        binding.button2.background = getGradientDrawable(100, 2, -0x555556, Color.TRANSPARENT)

    }

    private fun initialize() {
        binding.button1.setOnClickListener {
            intent.setClass(applicationContext, ArtistCreationActivity::class.java)
            startActivity(intent)
        }

        binding.button2.setOnClickListener {
            intent.setClass(applicationContext, SongUploadingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getGradientDrawable(
        cornerRadius: Int,
        strokeWidth: Int,
        strokeColor: Int,
        backgroundColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius.toFloat()
            this.setStroke(strokeWidth, strokeColor)
            this.setColor(backgroundColor)
        }
    }

    private fun design() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#000000")
    }

}
