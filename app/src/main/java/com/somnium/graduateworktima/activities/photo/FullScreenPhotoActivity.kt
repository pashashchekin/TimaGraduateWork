package com.somnium.graduateworktima.activities.photo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.somnium.graduateworktima.R
import com.squareup.picasso.Picasso
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.widget.Toast
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

const val IMAGE_URL = "image"

class FullscreenActivity : AppCompatActivity() {
    private var imgUrl: String? = null

    private var mStorage: FirebaseStorage? = null
    private var mDatabaseRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
        mStorage = FirebaseStorage.getInstance()
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("photo")
        imgUrl = intent.extras.getString(IMAGE_URL)
        val photoView = findViewById<View>(R.id.photo_view) as PhotoView
        val imageUri = Uri.parse(imgUrl)
        Glide.with(this@FullscreenActivity)
                .load(imageUri)
                .into(photoView)
    }
}

