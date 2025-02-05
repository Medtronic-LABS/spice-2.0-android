package com.medtroniclabs.opensource.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentChooseSiteBinding
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.boarding.ResourceLoadingScreen
import com.medtroniclabs.opensource.ui.landing.viewmodel.LandingViewModel

class ChooseSiteDialogueFragment : DialogFragment(),
    View.OnClickListener {

    private val viewModel: LandingViewModel by activityViewModels()

    lateinit var binding: FragmentChooseSiteBinding

    companion object {
        const val TAG = "ChooseSiteDialog"
        fun newInstance(): ChooseSiteDialogueFragment {
            val fragment = ChooseSiteDialogueFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChooseSiteBinding.inflate(inflater, container, false)
        binding.btnConfirm.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getUserHealthFacility()
        attachObserver()
    }

    private fun loadAdapter(healthFacilityEntities: ArrayList<HealthFacilityEntity>) {
       val defaultSiteId = SecuredPreference.getTenantId()
        binding.rvSiteList.layoutManager = LinearLayoutManager(binding.root.context)
       /* binding.rvSiteList.addItemDecoration(
            DividerNCDItemDecoration(activity, R.drawable.divider)
        )*/
        binding.rvSiteList.adapter = SiteAdapter(healthFacilityEntities,defaultSiteId) {
            enableConfirm(it,healthFacilityEntities.size!=1)
        }
    }

    private fun enableConfirm(siteEntity: HealthFacilityEntity, listCount: Boolean) {
        viewModel.selectedSiteEntity = siteEntity
        binding.btnConfirm.isEnabled = listCount
    }

    private fun attachObserver() {
        viewModel.userHealthFacilityLiveData.observe(viewLifecycleOwner) { resource ->
            resource.data?.let {
                loadAdapter(it)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnCancel -> {
                dismiss()
            }

            R.id.btnConfirm -> {
                (requireActivity() as? BaseActivity)?.withNetworkAvailability(
                    online = {
                        viewModel.selectedSiteEntity?.let {
                            SecuredPreference.putBoolean(
                                SecuredPreference.EnvironmentKey.ISMETALOADED.name,
                                false
                            )
                            SecuredPreference.putBoolean(
                                SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
                                false
                            )
                            SecuredPreference.putLong(
                                SecuredPreference.EnvironmentKey.TENANT_ID.name,
                                it.tenantId
                            )
                            SecuredPreference.putLong(
                                SecuredPreference.EnvironmentKey.ORGANIZATION_ID.name,
                                it.id
                            )
                            SecuredPreference.putString(
                                SecuredPreference.EnvironmentKey.ORGANIZATION_FHIR_ID.name,
                                it.fhirId
                            )
                            SecuredPreference.putLong(
                                SecuredPreference.EnvironmentKey.DISTRICT_ID.name,
                                it.districtId
                            )

                            SecuredPreference.putLong(
                                SecuredPreference.EnvironmentKey.CHIEFDOM_ID.name,
                                it.chiefdomId
                            )
                            triggerResourceLoading()
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventName = AnalyticsDefinedParams.NCDChangeFacility,
                                isCompleted = true
                            )
                            dismiss()
                        }
                    }, offline = {
                        dismiss()
                    }
                )
            }
        }
    }

    private fun triggerResourceLoading() {
       if (requireActivity() is BaseActivity) {
           val intent =  Intent(
               requireActivity(), ResourceLoadingScreen::class.java
           )
           intent.putExtra(DefinedParams.changeFacility,true)
           (requireActivity() as BaseActivity).startAsNewActivity(intent)
       }
    }

}