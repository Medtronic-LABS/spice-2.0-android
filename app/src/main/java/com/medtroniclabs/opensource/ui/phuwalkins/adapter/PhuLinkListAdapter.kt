package com.medtroniclabs.opensource.ui.phuwalkins.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.phuwalkins.listener.PhuLinkCallback

class PhuLinkListAdapter(
    private val phoneNumberCode: String?,
    private val patients: List<UnAssignedHouseholdMemberDetail>,
    private val listener: PhuLinkCallback
) :
    RecyclerView.Adapter<PhuLinkListAdapter.PatientViewHolder>() {

    // ViewHolder class to hold references to the views
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameAgeGender: TextView = itemView.findViewById(R.id.patientNameAgeGender)
        val village: TextView = itemView.findViewById(R.id.patientVillage)
        val mobile: TextView = itemView.findViewById(R.id.patientMobile)
        val linkPatientBtn: TextView = itemView.findViewById(R.id.linkPatientBtn)
        val callPatientBtn: AppCompatImageView = itemView.findViewById(R.id.callPatientBtn)
    }

    // Inflates the item layout when RecyclerView needs a new item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.linkpatient_list_phu, parent, false)
        return PatientViewHolder(view)
    }

    // Binds data to the views
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        // Set text for Name, Age, and Gender
        holder.nameAgeGender.text =
            getPatientName(
                holder.itemView.context,
                patient.name,
                patient.dateOfBirth,
                patient.gender
            )
        patient.name
        // Set text for Village
        holder.village.text = patient.villageName
        // Set text for Mobile Number
        holder.mobile.text = "+${phoneNumberCode} ${patient.phoneNumber}"
        // Handle Link Patient button click
        holder.linkPatientBtn.safeClickListener {
            listener.onLinkClicked(patient)
        }
        // Handle Call button click
        holder.callPatientBtn.safeClickListener {
            // Add logic to make a phone call
            listener.onCallClicked(patient)
        }
    }

    // Returns the total number of items in the list
    override fun getItemCount(): Int = patients.size
    private fun getPatientName(
        context: Context,
        name: String?,
        dob: String?,
        gender: String?
    ): String {
        return context.getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                context
            ),
            CommonUtils.getGenderText(gender, context)
        )
    }
}
