package com.somnium.graduateworktima.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import com.somnium.graduateworktima.R


class DocumentsActionsDialog(context: Context) {
    private val dialog: AlertDialog
    private var downloadTxt: TextView
    private var deleteTxt: TextView


    init {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.dialog_more_selected, null)
        val builder = AlertDialog.Builder(context)
        builder.setView(view)
        builder.setCancelable(true)
        dialog = builder.create()
        downloadTxt = view.findViewById(R.id.download_dialog)
        deleteTxt = view.findViewById(R.id.delete_dialog)
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun getDownloadTxt(): TextView {
        return downloadTxt
    }

    fun getDeleteTxt(): TextView {
        return deleteTxt
    }
}