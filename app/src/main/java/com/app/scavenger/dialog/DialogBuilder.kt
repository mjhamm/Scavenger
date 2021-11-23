package com.app.scavenger.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
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
            DialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        fun showErrorDialog(context: Context, theme: Int, title: String, message: String, function: (dialog: DialogInterface, which: Int) -> Unit) {
            DialogBuilder(context, theme)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", function)
                .create()
                .show()
        }
    }
}