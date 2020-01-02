package com.innomalist.taxi.common.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.innomalist.taxi.common.R

object LoadingDialog {
    var dialog: AlertDialog? = null
    fun display(context: Context) {
        if(dialog != null) {
            return
        }
        val materialDialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_loading)
        materialDialogBuilder.setCancelable(false)
        dialog = materialDialogBuilder.show()
    }
    fun hide() {
        if (dialog != null) {
            dialog!!.dismiss()
            dialog = null
        }
    }
}