package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.SortModel
import org.medtroniclabs.uhis.databinding.FragmentSortingDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.FilterSortInterface
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientListViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SortingDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentSortingDialogBinding
    var filterSortInterface: FilterSortInterface? = null
    private val activityVm: PatientListViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "SortingDialogFragment"

        fun newInstance(): SortingDialogFragment = SortingDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSortingDialogBinding.inflate(inflater, container, false)
        isCancelable = false
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        prefillValues()
        setListeners()
    }

    private fun initializeViews() {
        val sort = activityVm.sort ?: return
        if (sort.origin == UIConstants.FOLLOW_UP) {
            binding.apply {
                rbRedRisk.visibility = View.GONE
                rbLatestAssessment.visibility = View.GONE
                rbBP.visibility = View.GONE
                rbBG.visibility = View.GONE
                rbMedicalReview.visibility = View.GONE
                rbLatestMedicalReview.visibility =
                    if (!sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE
                rbOldestMedicalReview.visibility =
                    if (!sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE

                rbLatestNextReview.visibility =
                    if (!sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE
                rbOldestNextReview.visibility =
                    if (!sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE

                rbAssessmentDueDate.visibility =
                    if (sort.followUpType == DefinedParams.ASSESSMENT_FOLLOW_UP) View.VISIBLE else View.GONE

                rbLatestScreening.visibility =
                    if (sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE
                rbOldestScreening.visibility =
                    if (sort.followUpType.equals(DefinedParams.SCREENED, true)) View.VISIBLE else View.GONE
            }
        }
    }

    private fun prefillValues() {
        val sort = activityVm.sort ?: return
        sort.let { sort ->
            binding.apply {
                rbRedRisk.isChecked = sort.isRedRisk != null
                rbLatestAssessment.isChecked = sort.isLatestAssessment != null
                rbBP.isChecked = sort.isHighLowBp != null
                rbBG.isChecked = sort.isHighLowBg != null
                rbLatestMedicalReview.isChecked = sort.isLastReviewDate != null && sort.isLastReviewDate == true
                rbOldestMedicalReview.isChecked = sort.isLastReviewDate != null && sort.isLastReviewDate == false

                rbLatestNextReview.isChecked = sort.isNextReviewDate != null && sort.isNextReviewDate == true
                rbOldestNextReview.isChecked = sort.isNextReviewDate != null && sort.isNextReviewDate == false

                rbAssessmentDueDate.isChecked = sort.isAssessmentDueDate != null
                rbLatestScreening.isChecked = sort.isScreeningDueDate != null && sort.isScreeningDueDate == true
                rbOldestScreening.isChecked = sort.isScreeningDueDate != null && sort.isScreeningDueDate == false
            }
            binding.let {
                enableResetBtn(
                    it.rbRedRisk.isChecked ||
                        it.rbLatestAssessment.isChecked ||
                        it.rbMedicalReview.isChecked ||
                        it.rbBP.isChecked ||
                        it.rbBG.isChecked ||
                        it.rbAssessmentDueDate
                            .isChecked ||
                        it.rbLatestScreening.isChecked ||
                        it.rbOldestScreening.isChecked ||
                        it.rbOldestMedicalReview.isChecked ||
                        it.rbLatestMedicalReview.isChecked ||
                        it.rbLatestNextReview.isChecked ||
                        it.rbOldestNextReview.isChecked,
                )
            }
        }
    }

    private fun setListeners() {
        val sort = activityVm.sort ?: return
        binding.rgSortCondition.setOnCheckedChangeListener { _, checkedId ->
            if (connectivityManager.isNetworkAvailable()) {
                enableResetBtn(true)
                val radioButton =
                    if (checkedId > 0) {
                        binding.rgSortCondition.findViewById<RadioButton>(
                            checkedId,
                        )
                    } else {
                        null
                    }
                if (radioButton != null && radioButton.isChecked) {
                    applySort(
                        SortModel(
                            isRedRisk = value(checkedId, binding.rbRedRisk.id, true),
                            isLatestAssessment = value(
                                checkedId,
                                binding.rbLatestAssessment.id,
                                true,
                            ),
                            isLastReviewDate = handleMedicalReview(checkedId),
                            isHighLowBp = value(checkedId, binding.rbBP.id, true),
                            isHighLowBg = value(checkedId, binding.rbBG.id, true),
                            isAssessmentDueDate = value(
                                checkedId,
                                binding.rbAssessmentDueDate.id,
                                sort.origin == UIConstants.FOLLOW_UP,
                            ),
                            isScreeningDueDate = handleScreening(checkedId),
                            isNextReviewDate = handleNextReview(checkedId),
                        ),
                    )
                }
            } else {
                dismiss()
                (activity as BaseActivity).showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error),
                    false,
                ) {}
            }
        }
        binding.btnReset.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)
    }

    private fun handleScreening(checkedId: Int): Boolean? =
        when (checkedId) {
            binding.rbLatestScreening.id -> true
            binding.rbOldestScreening.id -> false
            else -> null
        }

    private fun handleMedicalReview(checkedId: Int): Boolean? =
        when (checkedId) {
            binding.rbLatestMedicalReview.id -> true
            binding.rbOldestMedicalReview.id -> false
            else -> null
        }

    private fun handleNextReview(checkedId: Int): Boolean? =
        when (checkedId) {
            binding.rbLatestNextReview.id -> true
            binding.rbOldestNextReview.id -> false
            else -> null
        }

    private fun value(
        checkedId: Int,
        id: Int,
        isDec: Boolean,
    ): Boolean? = if (checkedId == id) isDec else null

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> applySort(null)
            binding.labelHeader.ivClose.id -> dismiss()
            binding.btnReset.id -> doReset()
        }
    }

    private fun applySort(sortModel: SortModel?) {
        filterSortInterface?.sort(sortModel)
        dismiss()
    }

    private fun doReset() {
        binding.rgSortCondition.clearCheck()
        binding.btnDone.isEnabled = true
        enableResetBtn(false)
    }

    private fun enableResetBtn(isReset: Boolean) {
        binding.btnReset.isEnabled = isReset
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        if (parent is FilterSortInterface) {
            filterSortInterface = parent
        } else if (context is FilterSortInterface) {
            filterSortInterface = context
        } else {
            throw RuntimeException()
        }
    }
}
