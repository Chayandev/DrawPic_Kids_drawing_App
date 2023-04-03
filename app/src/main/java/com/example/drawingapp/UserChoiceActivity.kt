package com.example.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class UserChoiceActivity : AppCompatActivity() {
    private lateinit var tutVideoBtn: Button
    private lateinit var skipBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_choice)
        tutVideoBtn = findViewById(R.id.tutorial)
        skipBtn = findViewById(R.id.skip)
        tutVideoBtn.setOnClickListener {
            val intent = Intent(this, TutorialViewActivity::class.java)
            startActivity(intent)
        }
        skipBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}