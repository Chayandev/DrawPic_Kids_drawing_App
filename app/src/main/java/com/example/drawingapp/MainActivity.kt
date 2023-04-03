package com.example.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val rotateOpenAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_open_anim
        )
    }
    private val rotateCloseAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_close_anim
        )
    }
    private val fromBottomAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.from_buttom_anim
        )
    }
    private val toBottomAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom_anim
        )
    }
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                binding.galleryImgAdd.setImageURI(result.data?.data)
            }
        }

    //request
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Permission grated now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()
                    val pickIntent =
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    if (permissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    var customPgDialog: Dialog? = null
    private var clicked: Boolean = false
    private lateinit var binding: ActivityMainBinding
    private var mPaintBtnCurrPostion: ImageButton? = null
    private var brushDialog: Dialog? = null
    private var choiceCustomDialog: Dialog? = null
    private var customDialog: Dialog? = null
    private var customProgressDialog: Dialog? = null
    private var imageToShare: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        brushDialog = Dialog(this)
        brushDialog!!.setContentView(R.layout.brushselect_dialog)
        brushDialog!!.setTitle("Brush Size: ")
        binding.drawingView.setSizeForBrush(10.toFloat())
        binding.brushView.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        mPaintBtnCurrPostion = binding.llPaintClr[0] as ImageButton
        mPaintBtnCurrPostion!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palat_pressed)
        )
        binding.wipeBtn.setOnClickListener {
            customDialog = Dialog(this)
            customDialog!!.setContentView(R.layout.custome_alert_dialog)
            customDialog!!.setTitle("Confirm")
            val noBtn = customDialog?.findViewById<Button>(R.id.no_btn)
            val yesBtn = customDialog?.findViewById<Button>(R.id.yes_btn)
            noBtn?.setOnClickListener {
                customDialog?.dismiss()
            }
            yesBtn?.setOnClickListener {
                binding.drawingView.resetScreen()
                Snackbar.make(
                    binding.activityLayout,
                    "Canvas is Cleared Successfully!",
                    Snackbar.LENGTH_SHORT
                ).show()
                customDialog?.dismiss()
            }
            customDialog!!.setCancelable(false)
            customDialog?.show()
        }
        binding.undoBtn.setOnClickListener {
            binding.drawingView.onUndoClick()
        }
        binding.redoBtn.setOnClickListener {
            binding.drawingView.onRedoCLick()
        }
        binding.fabAdd.setOnClickListener {
            onAddButtonClicked()
        }
        //save btn
        binding.saveBtn.setOnClickListener {
            choiceCustomDialog = Dialog(this)
            choiceCustomDialog!!.setContentView(R.layout.choice_custom_dialog)
            choiceCustomDialog?.setTitle("Please Confirm one choice")
            var drawBtn = choiceCustomDialog!!.findViewById<Button>(R.id.onlyDrawBtn)
            var frameBtn = choiceCustomDialog!!.findViewById<Button>(R.id.frameBtn)
            var cancelBtn = choiceCustomDialog?.findViewById<ImageView>(R.id.cancel)
            drawBtn.setOnClickListener {
                showCustomProgressBar()
                if (isReadStorageAllowed()) {
                    lifecycleScope.launch {
                        saveBitmapFile(getBitmapFromView(binding.drawingView))
                    }
                }
                choiceCustomDialog?.dismiss()
            }
            frameBtn.setOnClickListener {
                showCustomProgressBar()
                if (isReadStorageAllowed()) {
                    lifecycleScope.launch {
                        saveBitmapFile(getBitmapFromView(binding.frame))
                    }
                }
                choiceCustomDialog!!.dismiss()
            }
            cancelBtn?.setOnClickListener {
                choiceCustomDialog?.dismiss()
            }
            choiceCustomDialog?.setCancelable(false)
            choiceCustomDialog?.show()

        }
        //image add
        binding.addImg.setOnClickListener {
            requestStoragePermission()
        }
        //image remove
        binding.deletImage.setOnClickListener {
            if(binding.galleryImgAdd.getDrawable()!=null) {
                val dltDialog: Dialog = Dialog(this)
                dltDialog.setContentView(R.layout.delet_permission_custom_dialog)
                dltDialog.setTitle("Background image removal permission")
                val yBtn=dltDialog.findViewById<Button>(R.id.yes_btn)
                val nBtn=dltDialog.findViewById<Button>(R.id.no_btn)
                yBtn.setOnClickListener {
                    binding.galleryImgAdd.setImageURI(null)
                    dltDialog.dismiss()
                    Snackbar.make(binding.activityLayout,"Background image is removed sucessfully",Snackbar.LENGTH_SHORT).show()
                }
                nBtn.setOnClickListener {
                    dltDialog.dismiss()
                }
                dltDialog.setCancelable(false)
                dltDialog.show()
            }
            else{
                Snackbar.make(binding.activityLayout,"No background is present to remove.",Snackbar.LENGTH_SHORT).show()
            }

        }
        binding.shareBtn.setOnClickListener {
            if (imageToShare != null) {
                shareImage(imageToShare!!)
                imageToShare = null
            } else {
                Snackbar.make(
                    binding.activityLayout,
                    "At first save drawing to share.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun onAddButtonClicked() {
        setVisibility(clicked)
        setAnimation(clicked)
        setClickable(clicked)
        clicked = !clicked
    }

    private fun setAnimation(click: Boolean) {
        if (!click) {
            binding.saveBtn.visibility = View.VISIBLE
            binding.shareBtn.visibility = View.VISIBLE
        } else {
            binding.saveBtn.visibility = View.INVISIBLE
            binding.shareBtn.visibility = View.INVISIBLE
        }
    }

    private fun setVisibility(click: Boolean) {
        if (!click) {
            binding.saveBtn.startAnimation(fromBottomAnimation)
            binding.shareBtn.startAnimation(fromBottomAnimation)
            binding.fabAdd.startAnimation(rotateOpenAnimation)
        } else {
            binding.saveBtn.startAnimation(toBottomAnimation)
            binding.shareBtn.startAnimation(toBottomAnimation)
            binding.fabAdd.startAnimation(rotateCloseAnimation)
        }
    }

    private fun setClickable(click: Boolean) {
        if (!click) {
            binding.saveBtn.isClickable = true
            binding.shareBtn.isClickable = true

        } else {
            binding.saveBtn.isClickable = false
            binding.shareBtn.isClickable = false
        }

    }

    private fun showBrushSizeChooserDialog() {
        val smallBrush = brushDialog?.findViewById<ImageButton>(R.id.ib_brush_small)
        val midSmallBrush = brushDialog?.findViewById<ImageButton>(R.id.ib_brush_mid_small)
        val mediumBrush = brushDialog?.findViewById<ImageButton>(R.id.ib_brush_medium)
        val largeBrush = brushDialog?.findViewById<ImageButton>(R.id.ib_brush_large)
        val brushPointerList = ArrayList<ImageButton>()
        brushPointerList.add(smallBrush!!)
        brushPointerList.add(midSmallBrush!!)
        brushPointerList.add(mediumBrush!!)
        brushPointerList.add(largeBrush!!)
        smallBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(5.toFloat())
            brushPointerPressed(0, brushPointerList)
            brushDialog?.dismiss()
        }
        midSmallBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushPointerPressed(1, brushPointerList)
            brushDialog?.dismiss()
        }
        mediumBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(15.toFloat())
            brushPointerPressed(2, brushPointerList)
            brushDialog?.dismiss()
        }
        largeBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(25.toFloat())
            brushPointerPressed(3, brushPointerList)
            brushDialog?.dismiss()
        }
        brushDialog?.setCancelable(false)
        brushDialog?.show()
    }

    private fun brushPointerPressed(indx: Int, brushPointers: ArrayList<ImageButton>) {
        defaultBrushPointerView(brushPointers)
        when (indx) {
            0 -> brushPointers[0].setImageResource(R.drawable.small_brush_pressed)

            1 -> brushPointers[1].setImageResource(R.drawable.mid_small_brush_pressed)

            2 -> brushPointers[2].setImageResource(R.drawable.medium_brush_pressed)

            3 -> brushPointers[3].setImageResource(R.drawable.large_brush_pressed)
        }
    }

    private fun defaultBrushPointerView(brushPointers: ArrayList<ImageButton>) {
        for (i in brushPointers.indices) {
            when (i) {
                0 -> brushPointers[0].setImageResource(R.drawable.small_brush)

                1 -> brushPointers[1].setImageResource(R.drawable.mid_small_brush)

                2 -> brushPointers[2].setImageResource(R.drawable.medium_brush)

                3 -> brushPointers[3].setImageResource(R.drawable.large_brush)
            }
        }
    }

    fun paintClickListner(view: View) {
        if (view !== mPaintBtnCurrPostion) {
            val imageBtn = view as ImageButton
            val color = imageBtn.tag.toString()
            binding.drawingView.setColor(color)
            imageBtn!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palat_pressed)
            )
            mPaintBtnCurrPostion?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palat_normal)
            )
            mPaintBtnCurrPostion = view
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationalDialog(
                "DRAWPiC",
                "Drawpic The Kids Drawing App" + "needs to Access Your External Storage"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    // android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "DRAWPiC_" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelCustomProgresBar()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                            imageToShare = FileProvider.getUriForFile(
                                this@MainActivity,
                                "com.example.drawingapp.fileprovider",
                                f
                            )
                            // shareImage(FileProvider.getUriForFile(this@MainActivity,"com.example.drawingapp.fileprovider",f))
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Somthing went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showCustomProgressBar() {
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.custom_progress_bar)
        customProgressDialog?.show()
    }

    private fun cancelCustomProgresBar() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.type = "image/png"
        startActivity(Intent.createChooser(intent, "Share image via "))
    }

    private fun showRationalDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }


}