package com.medtroniclabs.opensource.voice

import com.medtroniclabs.opensource.data.model.ChipViewItemModel

sealed class VoiceCommand {
    data class PatientSelection(val isFirstTime: Boolean, val patientName: String? = null) : VoiceCommand()
    data class VitalSigns(val type: String, val value: String) : VoiceCommand()
    data class Symptoms(val symptom: String) : VoiceCommand()
    data class Diagnosis(val diagnosis: String) : VoiceCommand()
    data class Prescription(val medication: String) : VoiceCommand()
    data class LabTest(val testName: String) : VoiceCommand()
    data class Confirmation(val action: String) : VoiceCommand()
    data class Unknown(val text: String) : VoiceCommand()
}

class VoiceCommandProcessor {
    fun processCommand(spokenText: String): VoiceCommand {
        val text = spokenText.lowercase().trim()
        
        return when {
            text.contains("first-time review") || text.contains("first time review") -> 
                VoiceCommand.PatientSelection(isFirstTime = true)
            
            text.contains("follow-up for") || text.contains("followup for") -> {
                val patientName = extractPatientName(text)
                VoiceCommand.PatientSelection(isFirstTime = false, patientName = patientName)
            }
            
            text.contains("bp") || text.contains("blood pressure") -> {
                val bpValue = extractBPValue(text)
                VoiceCommand.VitalSigns("blood_pressure", bpValue)
            }
            
            text.contains("complains of") || text.contains("symptoms") -> {
                val symptom = extractSymptom(text)
                VoiceCommand.Symptoms(symptom)
            }
            
            text.contains("diagnosis is") || text.contains("diagnosed with") -> {
                val diagnosis = extractDiagnosis(text)
                VoiceCommand.Diagnosis(diagnosis)
            }
            
            text.contains("add prescription for") || text.contains("prescribe") -> {
                val medication = extractMedication(text)
                VoiceCommand.Prescription(medication)
            }
            
            text.contains("order lab test") || text.contains("lab test") -> {
                val testName = extractLabTest(text)
                VoiceCommand.LabTest(testName)
            }
            
            text.contains("confirm") || text.contains("submit") || text.contains("complete") -> {
                VoiceCommand.Confirmation(text)
            }
            
            else -> VoiceCommand.Unknown(text)
        }
    }
    
    private fun extractPatientName(text: String): String? {
        val pattern = Regex("follow-?up for (.+)")
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }
    
    private fun extractBPValue(text: String): String {
        val pattern = Regex("bp (\\d+)/(\\d+)|blood pressure (\\d+)/(\\d+)")
        val match = pattern.find(text)
        return match?.groupValues?.let { groups ->
            "${groups[1] ?: groups[3]}/${groups[2] ?: groups[4]}"
        } ?: ""
    }
    
    private fun extractSymptom(text: String): String {
        return text.substringAfter("complains of").trim()
            .ifEmpty { text.substringAfter("symptoms").trim() }
    }
    
    private fun extractDiagnosis(text: String): String {
        return text.substringAfter("diagnosis is").trim()
            .ifEmpty { text.substringAfter("diagnosed with").trim() }
    }
    
    private fun extractMedication(text: String): String {
        return text.substringAfter("add prescription for").trim()
            .ifEmpty { text.substringAfter("prescribe").trim() }
    }
    
    private fun extractLabTest(text: String): String {
        return text.substringAfter("order lab test").trim()
            .ifEmpty { text.substringAfter("lab test").trim() }
    }
}
