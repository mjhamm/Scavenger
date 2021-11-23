package com.app.scavenger.v2.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.app.scavenger.BaseApplication
import com.app.scavenger.R
import com.app.scavenger.databinding.ActivityForgotPasswordBinding
import com.app.scavenger.dialog.DialogBuilder
import com.app.scavenger.utils.ConnectionDetector
import com.app.scavenger.utils.Constants

class ForgotPassword: AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var connection: ConnectionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connection = ConnectionDetector(this)
        binding.forgotToolbar.setTitle("Forgot Password")
        binding.forgotPassButton.isEnabled = false

        binding.forgotEditText.addTextChangedListener {
            object : TextWatcher {

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (p0?.toString()?.trim()?.length != 0) {
                        binding.forgotPassButton.isEnabled = p0?.toString()?.trim()?.contains("@") == true
                    } else {
                        binding.forgotPassButton.isEnabled = false
                    }
                }

                override fun afterTextChanged(p0: Editable?) {}
            }
        }

        binding.forgotPassButton.setOnClickListener {
            if (!connection.connectedToInternet()) {
                DialogBuilder.showInformationDialog(
                    this,
                    null,
                    Constants.noInternetTitle,
                    Constants.noInternetMessage
                )
            } else {
                BaseApplication.instance.getFirebaseAuth()
                    .sendPasswordResetEmail(binding.forgotEditText.text.toString().trim())
                    .addOnFailureListener {
                        DialogBuilder.showInformationDialog(
                            this,
                            R.style.ReportAlertTheme,
                            Constants.accountNotFound,
                            Constants.accountNotFoundMessage
                        )
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            DialogBuilder.showErrorDialog(
                                this,
                                R.style.ReportAlertTheme,
                                Constants.resetPassTitle,
                                Constants.resetPassMessage
                            ) { dialog, _ ->
                                dialog.dismiss()
                                finish()
                            }
                        }
                    }
            }
        }
    }
}