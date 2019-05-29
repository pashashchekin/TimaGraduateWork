package com.somnium.graduateworktima.activities.photo

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.somnium.graduateworktima.R
import com.google.firebase.database.FirebaseDatabase
import com.firebase.ui.database.FirebaseRecyclerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.UploadTask
import com.somnium.graduateworktima.BaseActivity
import com.somnium.graduateworktima.dialogs.DocumentsActionsDialog
import com.somnium.graduateworktima.models.Document
import java.text.SimpleDateFormat
import java.util.*


class PhotoActivity : BaseActivity(0) {
    private lateinit var imageUri: Uri
    private lateinit var addPhoto: ImageView
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var mStorageRef: StorageReference? = null
    private var mDatabaseRef: DatabaseReference? = null
    private var mStorage: FirebaseStorage? = null

    private var mUploadTask: StorageTask<*>? = null
    private var mPhotoRVAdapter: FirebaseRecyclerAdapter<Document, PhotoVH>? = null
    private var fileName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        setupBottomNavigation()
        initUI()
        mStorageRef = FirebaseStorage.getInstance().getReference("photo")
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("photo")
        mStorage = FirebaseStorage.getInstance()
        photoRecyclerView.setHasFixedSize(true)
        photoRecyclerView.layoutManager = LinearLayoutManager(this)
        addPhoto.setOnClickListener { onChoose() }
        fetch()
    }

    private fun initUI() {
        addPhoto = findViewById(R.id.add_photo)
        photoRecyclerView = findViewById(R.id.photo_recyclerview)
        progressBar = findViewById(R.id.all_photos)
    }

    private fun onChoose() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            imageUri = data.data
            fileName = getFileName(imageUri)
            onUpload()
        }
    }

    private fun onUpload() {
        if (mUploadTask != null && mUploadTask!!.isInProgress) {
        } else {
            uploadFile()
        }
    }

    private fun uploadFile() {
        if (imageUri != null) {
            val fileReference = mStorageRef!!.child(fileName.toString())
            mUploadTask = fileReference.putFile(imageUri)
            val urlTask = (mUploadTask as UploadTask).continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation fileReference.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var date = Date()
                    val formatter = SimpleDateFormat("dd MMM HH:mm")
                    val answer: String = formatter.format(date)
                    val downloadUri = task.result
                    val uploadId = mDatabaseRef!!.push().key
                    val upload = Document(uploadId.toString(), fileName.toString(), downloadUri.toString(), answer)
                    mDatabaseRef!!.child(uploadId!!).setValue(upload)
                    Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetch() {
        val personsRef = FirebaseDatabase.getInstance().reference.child("photo")
        val personsQuery = personsRef.orderByKey()
        val personsOptions = FirebaseRecyclerOptions.Builder<Document>().setQuery(personsQuery, Document::class.java).build()

        mPhotoRVAdapter = object : FirebaseRecyclerAdapter<Document, PhotoVH>(personsOptions) {

            override fun onDataChanged() {
                super.onDataChanged()
                progressBar.visibility = View.GONE
                photoRecyclerView.visibility = View.VISIBLE
            }

            override fun onBindViewHolder(holder: PhotoVH, position: Int, document: Document) {
                holder.setImage(document.url)
                holder.setName(document.name)
                holder.setDate(document.date)

                holder.itemView.setOnClickListener {
                    holder.activatePhoto(document)
                    holder.documentMoreActions(document)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {

                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.photo_item, parent, false)
                return PhotoVH(view)
            }
        }
        photoRecyclerView.adapter = mPhotoRVAdapter

    }

    inner class PhotoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var photoName: TextView = itemView.findViewById(R.id.photo_name)
        private var photoDate: TextView = itemView.findViewById(R.id.photo_date)
        private var downloadPhoto: ImageView = itemView.findViewById(R.id.download_photo)
        private var imagePhoto: ImageView = itemView.findViewById(R.id.photo_image)
        private var photo: ConstraintLayout = itemView.findViewById(R.id.block_photo)

        fun setName(name: String) {
            photoName.text = name
        }

        fun setDate(date: String) {
            photoDate.text = date
        }

        fun setImage(url: String) {
            Glide.with(this@PhotoActivity)
                    .asBitmap()
                    .load(url)
                    .into(imagePhoto)
        }

        fun activatePhoto(document: Document) {
            photo.setOnClickListener {
                val intent = Intent(this@PhotoActivity, FullscreenActivity::class.java)
                intent.putExtra("image", document.url)
                startActivity(intent)
            }
        }

        fun documentMoreActions(document: Document) {
            downloadPhoto.setOnClickListener {

                val dialog = DocumentsActionsDialog(this@PhotoActivity)
                val downloadTxt = dialog.getDownloadTxt()
                val deleteTxt = dialog.getDeleteTxt()

                downloadTxt.setOnClickListener {
                    downloadFile(this@PhotoActivity, document.name, "", Environment.DIRECTORY_DOWNLOADS, document.url)
                    dialog.dismiss()
                }

                deleteTxt.setOnClickListener {
                    val imageRef = mStorage!!.getReferenceFromUrl(document.url)
                    imageRef.delete().addOnSuccessListener {
                        mDatabaseRef!!.child(document.key).removeValue()
                        Toast.makeText(this@PhotoActivity, "Фото удалено", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun downloadFile(context: Context, fileName: String, fileExtension: String, destinationDirectory: String, url: String) {

        val downloadmanager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension)

        downloadmanager.enqueue(request)
    }

    public override fun onStart() {
        super.onStart()
        mPhotoRVAdapter!!.startListening()
    }

    public override fun onStop() {
        super.onStop()
        mPhotoRVAdapter!!.stopListening()
    }
}










