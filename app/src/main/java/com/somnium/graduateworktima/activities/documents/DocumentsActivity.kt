package com.somnium.graduateworktima.activities.documents

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.somnium.graduateworktima.BaseActivity
import com.somnium.graduateworktima.dialogs.DocumentsActionsDialog
import com.somnium.graduateworktima.R
import com.somnium.graduateworktima.models.Document
import java.text.SimpleDateFormat
import java.util.*

class DocumentsActivity : BaseActivity(3) {

    private lateinit var imageUri: Uri
    private lateinit var addDocument: ImageView
    private lateinit var documentRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var webView: WebView? = null
    private var mStorageRef: StorageReference? = null
    private var mDatabaseRef: DatabaseReference? = null
    private var mStorage: FirebaseStorage? = null
    private var mUploadTask: StorageTask<*>? = null
    private var mDocumentRVAdapter: FirebaseRecyclerAdapter<Document, AudioVH>? = null
    private var fileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents)
        setupBottomNavigation()
        initUI()
        mStorageRef = FirebaseStorage.getInstance().getReference("document")
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("document")
        mStorage = FirebaseStorage.getInstance()
        documentRecyclerView.setHasFixedSize(true)
        documentRecyclerView.layoutManager = LinearLayoutManager(this)
        addDocument.setOnClickListener { onChoose() }
        fetch()
    }

    private fun initUI() {
        addDocument = findViewById(R.id.add_document)
        documentRecyclerView = findViewById(R.id.documents_recyclerview)
        progressBar = findViewById(R.id.all_documents)
        webView = findViewById(R.id.webView)
    }

    private fun onChoose() {
        val intent = Intent()
        intent.type = "application/msword"
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
                    Toast.makeText(this, "Документ добавлен", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetch() {
        val personsRef = FirebaseDatabase.getInstance().reference.child("document")
        val personsQuery = personsRef.orderByKey()
        val personsOptions = FirebaseRecyclerOptions.Builder<Document>().setQuery(personsQuery, Document::class.java).build()

        mDocumentRVAdapter = object : FirebaseRecyclerAdapter<Document, AudioVH>(personsOptions) {

            override fun onDataChanged() {
                super.onDataChanged()
                progressBar.visibility = View.GONE
                documentRecyclerView.visibility = View.VISIBLE
            }

            override fun onBindViewHolder(holder: AudioVH, position: Int, document: Document) {
                holder.setName(document.name)
                holder.setDate(document.date)
                holder.itemView.setOnClickListener {
                    //holder.hz()
                    holder.documentMoreActions(document)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioVH {

                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.document_item, parent, false)
                return AudioVH(view)
            }
        }
        documentRecyclerView.adapter = mDocumentRVAdapter

    }

    inner class AudioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var docName: TextView = itemView.findViewById(R.id.document_name)
        private var docDate: TextView = itemView.findViewById(R.id.document_date)
        private var downloadDoc: ImageView = itemView.findViewById(R.id.download_document)
//        private var constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)

        fun setName(name: String) {
            docName.text = name
        }

        fun setDate(date: String) {
            docDate.text = date
        }

        fun documentMoreActions(document: Document) {
            downloadDoc.setOnClickListener {


                val dialog = DocumentsActionsDialog(this@DocumentsActivity)
                val downloadTxt = dialog.getDownloadTxt()
                val deleteTxt = dialog.getDeleteTxt()

                downloadTxt.setOnClickListener {
                    downloadFile(this@DocumentsActivity, document.name, "", Environment.DIRECTORY_DOWNLOADS, document.url)
                    dialog.dismiss()
                }

                deleteTxt.setOnClickListener {
                    val imageRef = mStorage!!.getReferenceFromUrl(document.url)
                    imageRef.delete().addOnSuccessListener {
                        mDatabaseRef!!.child(document.key.toString()).removeValue()
                        Toast.makeText(this@DocumentsActivity, "Документ удален", Toast.LENGTH_SHORT).show()
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
        mDocumentRVAdapter!!.startListening()
    }

    public override fun onStop() {
        super.onStop()
        mDocumentRVAdapter!!.stopListening()
    }
}
