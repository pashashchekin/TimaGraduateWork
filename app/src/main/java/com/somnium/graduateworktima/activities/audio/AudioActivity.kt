package com.somnium.graduateworktima.activities.audio

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.somnium.graduateworktima.R
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.somnium.graduateworktima.BaseActivity
import com.somnium.graduateworktima.dialogs.DocumentsActionsDialog
import com.somnium.graduateworktima.models.Document
import java.text.SimpleDateFormat
import java.util.*

class AudioActivity : BaseActivity(2) {

    private lateinit var audioUri: Uri
    private lateinit var addAudio: ImageView
    private lateinit var audioRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var mStorageRef: StorageReference? = null
    private var mDatabaseRef: DatabaseReference? = null
    private var mStorage: FirebaseStorage? = null
    private var mUploadTask: StorageTask<*>? = null
    private var mAudioRVAdapter: FirebaseRecyclerAdapter<Document, AudioActivity.AudioVH>? = null
    private var fileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)
        setupBottomNavigation()
        initUI()
        mStorageRef = FirebaseStorage.getInstance().getReference("audio")
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("audio")
        mStorage = FirebaseStorage.getInstance()
        audioRecyclerView.setHasFixedSize(true)
        audioRecyclerView.layoutManager = LinearLayoutManager(this)
        addAudio.setOnClickListener { onChoose() }
        fetch()

//        val products = arrayListOf<Document>()
//        val ref = FirebaseDatabase.getInstance().getReference("audio")
//        ref.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                for (productSnapshot in dataSnapshot.children) {
//                    val product = productSnapshot.getValue(Document::class.java)
//                    products.add(product!!)
//
//                }
//                for (prod in products) {
//                    Log.d("qwe",prod.key  + prod.name + prod.date + prod.url)
//                }
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {
//                throw databaseError.toException()
//            }
//        })
    }

    private fun initUI() {
        addAudio = findViewById(R.id.add_audio)
        audioRecyclerView = findViewById(R.id.audio_recyclerview)
        progressBar = findViewById(R.id.all_audios)
    }

    private fun onChoose() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            audioUri = data.data
            fileName = getFileName(audioUri)
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
        if (audioUri != null) {
            val fileReference = mStorageRef!!.child(fileName.toString())
            mUploadTask = fileReference.putFile(audioUri)
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
                    Toast.makeText(this, "Аудио добавлено", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetch() {
        val personsRef = FirebaseDatabase.getInstance().reference.child("audio")
        val personsQuery = personsRef.orderByKey()
        val personsOptions = FirebaseRecyclerOptions.Builder<Document>().setQuery(personsQuery, Document::class.java).build()

        mAudioRVAdapter = object : FirebaseRecyclerAdapter<Document, AudioVH>(personsOptions) {

            override fun onDataChanged() {
                super.onDataChanged()
                progressBar.visibility = View.GONE
                audioRecyclerView.visibility = View.VISIBLE
            }

            override fun onBindViewHolder(holder: AudioVH, position: Int, document: Document) {
                holder.setName(document.name)
                holder.setDate(document.date)
                holder.itemView.setOnClickListener {
                    holder.audioMoreActions(document)
                    holder.activateAudio(document)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioVH {

                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.audio_item, parent, false)
                return AudioVH(view)
            }
        }
        audioRecyclerView.adapter = mAudioRVAdapter

    }

    inner class AudioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var audioName: TextView = itemView.findViewById(R.id.audio_name)
        private var audioDate: TextView = itemView.findViewById(R.id.audio_date)
        private var downloadAudio: ImageView = itemView.findViewById(R.id.download_audio)
        private var audio: ConstraintLayout = itemView.findViewById(R.id.block_audio)

        fun setName(name: String) {
            audioName.text = name
        }

        fun setDate(date: String) {
            audioDate.text = date
        }

        fun audioMoreActions(document: Document) {
            downloadAudio.setOnClickListener {

                val dialog = DocumentsActionsDialog(this@AudioActivity)
                val downloadTxt = dialog.getDownloadTxt()
                val deleteTxt = dialog.getDeleteTxt()

                downloadTxt.setOnClickListener {
                    downloadFile(this@AudioActivity, document.name, "", Environment.DIRECTORY_DOWNLOADS, document.url)
                    dialog.dismiss()
                }

                deleteTxt.setOnClickListener {
                    val imageRef = mStorage!!.getReferenceFromUrl(document.url)
                    imageRef.delete().addOnSuccessListener {
                        mDatabaseRef!!.child(document.key.toString()).removeValue()
                        Toast.makeText(this@AudioActivity, "Аудио удалено", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }

        fun activateAudio(document: Document) {
            audio.setOnClickListener {
                val intent = Intent(this@AudioActivity, AudioActivateActivity::class.java)
                intent.putExtra("audio", document.url)
                startActivity(intent)
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
        mAudioRVAdapter!!.startListening()
    }

    public override fun onStop() {
        super.onStop()
        mAudioRVAdapter!!.stopListening()
    }
}
