package com.somnium.graduateworktima.activities.video

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.android.gms.tasks.Continuation
import com.google.firebase.storage.UploadTask
import com.somnium.graduateworktima.BaseActivity
import com.somnium.graduateworktima.R
import com.somnium.graduateworktima.dialogs.DocumentsActionsDialog
import com.somnium.graduateworktima.models.Document
import java.text.SimpleDateFormat
import java.util.*

class VideoActivity : BaseActivity(1) {
    private lateinit var videoUri: Uri
    private lateinit var addVideo: ImageView
    private lateinit var videoRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var mStorageRef: StorageReference? = null
    private var mDatabaseRef: DatabaseReference? = null
    private var mStorage: FirebaseStorage? = null
    private var mUploadTask: StorageTask<*>? = null
    private var mVideoRVAdapter: FirebaseRecyclerAdapter<Document, VideoActivity.VideoVH>? = null
    private var fileName: String? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        setupBottomNavigation()
        initUI()
        mStorageRef = FirebaseStorage.getInstance().getReference("video")
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("video")
        mStorage = FirebaseStorage.getInstance()
        videoRecyclerView.setHasFixedSize(true)
        videoRecyclerView.layoutManager = LinearLayoutManager(this)
        addVideo.setOnClickListener { onChoose() }
        fetch()


    }

    private fun initUI() {
        addVideo = findViewById(R.id.add_video)
        videoRecyclerView = findViewById(R.id.video_recyclerview)
        progressBar = findViewById(R.id.all_videos)
    }

    private fun onChoose() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            videoUri = data.data
            fileName = getFileName(videoUri)
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
        if (videoUri != null) {
            val fileReference = mStorageRef!!.child(fileName.toString())
            mUploadTask = fileReference.putFile(videoUri)
            val urlTask = (mUploadTask as UploadTask).continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception.let {
                        throw it!!
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
                    Toast.makeText(this, "Видео добавлено", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetch() {
        val personsRef = FirebaseDatabase.getInstance().reference.child("video")
        val personsQuery = personsRef.orderByKey()
        val personsOptions = FirebaseRecyclerOptions.Builder<Document>().setQuery(personsQuery, Document::class.java).build()

        mVideoRVAdapter = object : FirebaseRecyclerAdapter<Document, VideoVH>(personsOptions) {

            override fun onDataChanged() {
                super.onDataChanged()
                progressBar.visibility = View.GONE
                videoRecyclerView.visibility = View.VISIBLE
            }

            override fun onBindViewHolder(holder: VideoVH, position: Int, document: Document) {
                holder.setName(document.name)
                holder.setDate(document.date)
                holder.setImage(document.url)
                holder.itemView.setOnClickListener {
                    holder.documentMoreActions(document)
                    holder.activateVideo(document)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoVH {

                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.video_item, parent, false)
                return VideoVH(view)
            }
        }
        videoRecyclerView.adapter = mVideoRVAdapter

    }

    inner class VideoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {



        private var videoName: TextView = itemView.findViewById(R.id.video_name)
        private var videoDate: TextView = itemView.findViewById(R.id.video_date)
        private var downloadVideo: ImageView = itemView.findViewById(R.id.download_video)
        private var imageVideo : ImageView = itemView.findViewById(R.id.video_image)
        private var video : ConstraintLayout = itemView.findViewById(R.id.block_video)

        fun setName(name: String) {
            videoName.text = name
        }

        fun setDate(date: String) {
            videoDate.text = date
        }
        fun setImage(url : String) {
            Glide.with(this@VideoActivity)
                    .asBitmap()
                    .load(url)
                    .into(imageVideo)
        }

        fun activateVideo(document: Document) {
            video.setOnClickListener {
                val intent = Intent(this@VideoActivity, VideoActivateActivity::class.java)
                intent.putExtra("video",document.url)
                startActivity(intent)
            }
        }

        fun documentMoreActions(document: Document) {
            downloadVideo.setOnClickListener {


                val dialog = DocumentsActionsDialog(this@VideoActivity)
                val downloadTxt = dialog.getDownloadTxt()
                val deleteTxt = dialog.getDeleteTxt()

                downloadTxt.setOnClickListener {
                    downloadFile(this@VideoActivity, document.name, "", Environment.DIRECTORY_DOWNLOADS, document.url)
                    dialog.dismiss()
                }

                deleteTxt.setOnClickListener {
                    val imageRef = mStorage!!.getReferenceFromUrl(document.url)
                    imageRef.delete().addOnSuccessListener {
                        mDatabaseRef!!.child(document.key.toString()).removeValue()
                        Toast.makeText(this@VideoActivity, "Видео удалено", Toast.LENGTH_SHORT).show()
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
        mVideoRVAdapter!!.startListening()
    }

    public override fun onStop() {
        super.onStop()
        mVideoRVAdapter!!.stopListening()
    }
}
