package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.checkIsTablet
import org.medtroniclabs.uhis.databinding.DialogControlledPatientStatusBinding
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.util.CommonDialogInterface
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientTypeViewModel
import kotlin.getValue

@AndroidEntryPoint
class PatientTypeDialog(
    private val hMap: HashMap<String, Any>,
    private val sites: ArrayList<SiteEntity>,
    private val commonDialogInterface: CommonDialogInterface,
) :
    DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogControlledPatientStatusBinding
    private val viewModel: PatientTypeViewModel by activityViewModels()

    private var adapter: CustomSpinnerAdapter? = null

    companion object {
        const val TAG = "PatientTypeDialog"

        fun newInstance(
            hMap: HashMap<String, Any>,
            sites: ArrayList<SiteEntity>,
            commonDialogInterface: CommonDialogInterface,
        ) = PatientTypeDialog(hMap, sites, commonDialogInterface)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogControlledPatientStatusBinding.inflate(inflater, container, false)
        isCancelable = false
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initViews()
        setObservers()
    }

    override fun onStart() {
        super.onStart()
        if (CommonUtils.checkIsLargeTablet(requireContext()) || checkIsTablet(requireContext())) {
            setDialogPercent(getWidthPercentage(), getHeightPercentage())
        } else {
            setDialogPercent(45, 80)
        }
    }

    private fun getHeightPercentage(): Int {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            60
        } else {
            45
        }
    }

    private fun getWidthPercentage(): Int {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            45
        } else {
            75
        }
    }

    private fun setObservers() {
        viewModel.patientTypeMap.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    closeDialog()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }
            }
        }
    }

    private fun closeDialog() {
        dismiss()
        commonDialogInterface.onSuccess()
    }

    private fun loadSites(data: ArrayList<SiteEntity>) {
        adapter =
            CustomSpinnerAdapter(requireContext())
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        data.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                ),
            )
            adapter?.setData(dropDownList)
            binding.etSitesSpinner.adapter = adapter
            binding.etSitesSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        pos: Int,
                        itemId: Long,
                    ) {
                        binding.btnSubmit.isEnabled = pos > 0
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /**
                         * usage of this method is not required
                         */
                    }
                }
        }
    }

    private fun initViews() {
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnSubmit.safeClickListener(this)
        loadSites(sites)
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnCancel.id, binding.labelHeader.ivClose.id -> closeDialog()
            binding.btnSubmit.id -> {
                val selectedItem =
                    adapter?.getData(position = binding.etSitesSpinner.selectedItemPosition)
                selectedItem?.let {
                    hMap[DefinedParams.PRESCRIBED_SITE_ID] =
                        it[DefinedParams.ID].toString().toLong()
                    viewModel.updatePatientType(requireContext(), hMap)
                }
            }
        }
    }

    fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }
}
