package com.medtroniclabs.opensource.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.ActivityMedicalReviewToolsBinding
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientMenuFragment
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewActivity
import com.medtroniclabs.opensource.voice.*
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MedicalReviewToolsActivity : BaseActivity(), VoiceCommandListener {

    private lateinit var binding: ActivityMedicalReviewToolsBinding
    private val toolsViewModel: ToolsViewModel by viewModels()
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var voiceCommandProcessor: VoiceCommandProcessor
    private lateinit var voiceFeedbackManager: VoiceFeedbackManager
    private lateinit var microphoneStatusView: MicrophoneStatusView
    
    companion object {
        private const val MICROPHONE_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalReviewToolsBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.search_patient),
            homeAndBackVisibility = Pair(true, true),
            callbackHome = {
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
        )
        checkMicrophonePermission()
        initializeView()
    }

    private fun initializeView() {
        val fragmentTag =
            PatientMenuFragment.TAG
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            fragment = PatientMenuFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                intent.getStringExtra(DefinedParams.ID),
                intent.getStringExtra(DefinedParams.Gender),
                intent.getStringExtra(DefinedParams.DOB),
                childPatientId =intent.getStringExtra(DefinedParams.ChildPatientId),
                dateOfDelivery = intent.getStringExtra(DefinedParams.DateOfDelivery),
                intent.getStringExtra(DefinedParams.NeonateOutcome)
            )
            setTitle(
                intent.getStringExtra(DefinedParams.MenuTitle)
                    ?: getString(R.string.search_patient)
            )
            supportFragmentManager.beginTransaction()
                .add(R.id.menuItemsFragment, fragment, fragmentTag)
                .commit()
        }
    }
    
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MICROPHONE_PERMISSION_REQUEST_CODE
            )
        } else {
            initializeVoiceInput()
        }
    }
    
    private fun initializeVoiceInput() {
        voiceInputManager = VoiceInputManager(this)
        voiceCommandProcessor = VoiceCommandProcessor()
        voiceFeedbackManager = VoiceFeedbackManager(this)
        
        microphoneStatusView = MicrophoneStatusView(this)
        addViewToToolbar(microphoneStatusView)
        
        voiceInputManager.voiceResult.observe(this) { result ->
            processVoiceCommand(result)
        }
        
        voiceInputManager.isListening.observe(this) { isListening ->
            microphoneStatusView.setListeningStatus(isListening)
        }
        
        voiceInputManager.error.observe(this) { error ->
            voiceFeedbackManager.announceError(error)
        }
        
        microphoneStatusView.setOnMicrophoneClickListener {
            if (voiceInputManager.isListening.value == true) {
                voiceInputManager.stopListening()
            } else {
                voiceInputManager.startListening()
            }
        }
    }
    
    private fun processVoiceCommand(spokenText: String) {
        val command = voiceCommandProcessor.processCommand(spokenText)
        onVoiceCommand(command)
    }
    
    override fun onVoiceCommand(command: VoiceCommand) {
        when (command) {
            is VoiceCommand.PatientSelection -> {
                if (command.isFirstTime) {
                    voiceFeedbackManager.confirmAction("Opening first-time review")
                    startNCDMedicalReview()
                } else {
                    voiceFeedbackManager.confirmAction("Searching for patient: ${command.patientName}")
                }
            }
            is VoiceCommand.Unknown -> {
                voiceFeedbackManager.announceError("Command not recognized: ${command.text}")
            }
            else -> {
                voiceFeedbackManager.announceError("Command not supported in this screen")
            }
        }
    }
    
    private fun startNCDMedicalReview() {
        val intent = Intent(this, NCDMedicalReviewActivity::class.java)
        val patientId = intent.extras?.getString("patientId")
        if (!patientId.isNullOrBlank()) {
            intent.putExtra("patientId", patientId)
        }
        startActivity(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeVoiceInput()
            } else {
                showErrorDialog(getString(R.string.error), "Microphone permission required for voice input")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::voiceInputManager.isInitialized) {
            voiceInputManager.destroy()
        }
        if (::voiceFeedbackManager.isInitialized) {
            voiceFeedbackManager.destroy()
        }
    }
}
