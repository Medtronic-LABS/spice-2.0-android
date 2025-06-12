package com.medtroniclabs.opensource.voice

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.ViewMicrophoneStatusBinding

class MicrophoneStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewMicrophoneStatusBinding
    
    init {
        binding = ViewMicrophoneStatusBinding.inflate(LayoutInflater.from(context), this, true)
        setListeningStatus(false)
    }
    
    fun setListeningStatus(isListening: Boolean) {
        if (isListening) {
            binding.ivMicrophone.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.ivMicrophone.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            binding.tvStatus.text = "🟢 Listening..."
        } else {
            binding.ivMicrophone.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.ivMicrophone.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            binding.tvStatus.text = "🔴 Tap to speak"
        }
    }
    
    fun setOnMicrophoneClickListener(listener: OnClickListener) {
        binding.ivMicrophone.setOnClickListener(listener)
    }
}
