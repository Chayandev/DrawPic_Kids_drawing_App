package com.example.kidsdrawingappDrawPic

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.net.Uri.parse
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

class TutorialViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_view)
        val video = findViewById<VideoView>(R.id.tutVedio)
        val text=findViewById<TextView>(R.id.instruction)
        val skipBtn = findViewById<Button>(R.id.skipBtn)
        val pgBar = findViewById<ProgressBar>(R.id.progressBar)
       // val sRotate=findViewById<ImageView>(R.id.screen_size)
        var flag:Int=0
        skipBtn.setBackgroundColor(Color.rgb(60, 114, 101))
        skipBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
//        sRotate.setOnClickListener {
//            if(flag ==0) {
//                skipBtn.visibility=View.INVISIBLE
//                text.visibility=View.INVISIBLE
//                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//                flag=1
//            }
//            else{
//                requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                skipBtn.visibility=View.VISIBLE
//                text.visibility=View.VISIBLE
//                flag=0
//            }
//        }
        val mediaController = MediaController(this)
        mediaController.setAnchorView(video)
        //specify the location of media file
        val uri: Uri = parse(
            "android.resource://" + packageName + "/"
                    + R.raw.vedio
        );
        video.setMediaController(mediaController)
        video.setVideoURI(uri)
        video.requestFocus()
        pgBar.alpha = 1F
        pgBar.animate().setDuration(3000).alpha(1F).withEndAction() {
            pgBar.visibility = View.INVISIBLE
            video.start()
        }
        video.setOnErrorListener { mp, what, extra ->
            Toast.makeText(this,"Somthing Error occured with the vedio",Toast.LENGTH_SHORT).show()
            false
        }
    }

}