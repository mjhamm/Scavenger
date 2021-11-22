package com.app.scavenger.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogBuilder: MaterialAlertDialogBuilder {

    private var dialog: AlertDialog? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, theme: Int) : super(context, theme)

    fun getDialog(): Dialog? {
        return dialog
    }

    companion object {
        fun showInformationDialog(context: Context, title: String, message: String) {
            val builder = DialogBuilder(context)
            val dialog = builder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}