package com.app.scavenger.v2.activities

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.scavenger.databinding.ActivitySendFeedbackBinding

class SendFeedback: AppCompatActivity() {

    private lateinit var binding: ActivitySendFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.sendFeedbackToolbar.setTitle("Send Feedback")

        binding.submitFeedbackTextButton.isEnabled = false
        binding.submitFeedbackTextButton.setTextColor(Color.GRAY)
    }
}