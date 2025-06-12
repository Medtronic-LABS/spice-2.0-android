package com.medtroniclabs.opensource.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class VoiceFeedbackManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    init {
        textToSpeech = TextToSpeech(context, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            isInitialized = true
        }
    }
    
    fun speak(text: String) {
        if (isInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    fun confirmAction(action: String) {
        speak("$action confirmed")
    }
    
    fun announceError(error: String) {
        speak("Error: $error")
    }
    
    fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
