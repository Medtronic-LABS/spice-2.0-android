package org.medtroniclabs.uhis.ui.medicalreview.familyplan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DefinedParams.POST_PARTUM
import org.medtroniclabs.uhis.common.DefinedParams.Post_Partum
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentContraceptivesBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.viewmodel.ContraceptivesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.ClientType
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CombineOralContraceptive
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CombinedOralContraceptiveComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Condoms
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.EmergencyContraceptive
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Implants
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Injectables
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.Microlut
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.MicrolutQuantity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherFPMethod
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherImplantComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherImplantSpecify
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherInjectableComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherInjectablesSpecify
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherOralSpecify
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherPermanentMethodComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherProgestinOnlyOralsComments
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.OtherSpecify
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.PermanentMethod
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.PostPartum
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.ProgestinOnlyOrals
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

@AndroidEntryPoint
class ContraceptivesFragment : BaseFragment() {
    private lateinit var binding: FragmentContraceptivesBinding
    private lateinit var iucdTagView: TagListCustomView
    private val viewModel: ContraceptivesViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentContraceptivesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initializeListener()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.contraceptiveMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resource.data?.let {
                        initializeMetaItems(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        patientViewModel.patientCurrentStatus.observe(viewLifecycleOwner) { status ->
            initClientType()
        }
    }

    private fun initClientType() {
        viewModel.contraceptiveMetaList.value?.data?.let { metaItems ->
            if (!binding.ClientTypeRoot.isVisible()) {
                binding.ClientTypeRoot.visible()
                addCustomView(
                    getSingleSelectionViewData(
                        metaItems
                            .filter {
                                it.category.equals(
                                    MedicalReviewTypeEnums.client_type.name,
                                    true,
                                )
                            }.sortedBy { it.displayOrder },
                    ),
                    ClientType,
                    getClientTypeResult(),
                    clientTypeSelectionCallBack,
                    binding.ClientTypeRoot,
                )
            }
        }
    }

    private fun initializeMetaItems(metaList: List<MedicalReviewMetaItems>) {
        addCustomView(
            getSingleSelectionViewData(
                metaList.filter { it.category.equals(MedicalReviewTypeEnums.combined_oral_contraceptive.name, true) }.sortedBy { it.displayOrder },
            ),
            CombineOralContraceptive,
            viewModel.resultHashMap,
            combinedOralSelectionCallBack,
            binding.CombinedOralContraceptiveRoot,
        )
        addCustomView(
            getSingleSelectionViewData(metaList.filter { it.category.equals(MedicalReviewTypeEnums.progestin.name, true) }.sortedBy { it.displayOrder }),
            ProgestinOnlyOrals,
            viewModel.resultHashMap,
            progestinSelectionCallBack,
            binding.ProgestinOnlyOralsRoot,
        )

        addCustomView(
            getSingleSelectionViewData(metaList.filter { it.category.equals(MedicalReviewTypeEnums.injectables.name, true) }.sortedBy { it.displayOrder }),
            Injectables,
            viewModel.resultHashMap,
            injectableSelectionCallBack,
            binding.InjectableRoot,
        )

        addCustomView(
            getSingleSelectionViewData(metaList.filter { it.category.equals(MedicalReviewTypeEnums.implants.name, true) }.sortedBy { it.displayOrder }),
            Implants,
            viewModel.resultHashMap,
            implantsSelectionCallBack,
            binding.ImplantsRoot,
        )

        addCustomView(
            getSingleSelectionViewData(metaList.filter { it.category.equals(MedicalReviewTypeEnums.condoms.name, true) }.sortedBy { it.displayOrder }),
            Condoms,
            viewModel.resultHashMap,
            condomsSelectionCallBack,
            binding.CondomsRoot,
        )

        addCustomView(
            getSingleSelectionViewData(
                metaList.filter { it.category.equals(MedicalReviewTypeEnums.emergency_contraceptive.name, true) }.sortedBy { it.displayOrder },
            ),
            EmergencyContraceptive,
            viewModel.resultHashMap,
            emergencyContraSelectionCallBack,
            binding.EmergencyContraceptiveRoot,
        )

        addCustomView(
            getSingleSelectionViewData(metaList.filter { it.category.equals(MedicalReviewTypeEnums.permanent_method.name, true) }.sortedBy { it.displayOrder }),
            PermanentMethod,
            viewModel.resultHashMap,
            permanentMethodSelectionCallBack,
            binding.PermanentMethodRoot,
        )

        val iucdListItem =
            metaList.filter { it.category.equals(MedicalReviewTypeEnums.iucd.name, true) }
        val chipItemList = mutableListOf<ChipViewItemModel>()
        iucdListItem.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    value = it.value,
                ),
            )
            iucdTagView.addChipItemList(chipItemList, viewModel.selectedIUCD)
        }
    }

    private fun getClientTypeResult(): HashMap<String, Any> {
        return patientViewModel.patientCurrentStatus.value?.let {
            return if (it.contains(Post_Partum, true) && !viewModel.resultHashMap.containsKey(ClientType)) {
                viewModel.resultHashMap[ClientType] = POST_PARTUM
                enablePostPartumChipView()
                resultMapChanged()
                viewModel.resultHashMap
            } else {
                viewModel.resultHashMap
            }
        } ?: kotlin.run {
            viewModel.resultHashMap
        }
    }

    private fun initializeListener() {
        binding.etOtherInjectableComments.addTextChangedListener { notes ->
            viewModel.otherInjectableComments = if (notes.isNullOrBlank()) null else notes.toString()
        }
        binding.etOtherImplantsComments.addTextChangedListener { notes ->
            viewModel.otherImplantComments = if (notes.isNullOrBlank()) null else notes.toString()
        }
        binding.etOtherPermanentMethodComments.addTextChangedListener { notes ->
            viewModel.otherPermanentMethodComments = if (notes.isNullOrBlank()) null else notes.toString()
        }
        binding.etProgestinOnlyOralsComments.addTextChangedListener { notes ->
            viewModel.otherProgestinOnlyOralsComments = if (notes.isNullOrBlank()) null else notes.toString()
        }
        binding.etCombinedOralContraceptiveComments.addTextChangedListener { notes ->
            viewModel.combinedOralContraceptiveComments = if (notes.isNullOrBlank()) null else notes.toString()
        }
    }

    private fun initViews() {
        binding.ClientTypeRoot.gone()
        binding.tvPostPartum.markMandatory()
        binding.tvQuantityLabel.markMandatory()

        viewModel.getMetaList(MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name)

        iucdTagView = TagListCustomView(
            binding.root.context,
            binding.tagViewIUCD,
        ) { _, _, _ ->
            viewModel.selectedIUCD = ArrayList(iucdTagView.getSelectedTags())
        }

        binding.etQuantity.addTextChangedListener { editable ->
            val quantityString = editable.toString()
            viewModel.quantity = if (quantityString.isBlank()) {
                null
            } else {
                try {
                    quantityString.toLong()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun getSingleSelectionViewData(metaList: List<MedicalReviewMetaItems>): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        metaList.forEach {
            flowList.add(
                getOptionMap(
                    it.value ?: it.name,
                    it.name,
                ),
            )
        }
        return flowList
    }

    private fun addCustomView(
        data: ArrayList<Map<String, Any>>,
        tag: String,
        hashMap: HashMap<String, Any>,
        callback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)?,
        container: ViewGroup?,
    ) {
        SingleSelectionCustomView(binding.root.context).apply {
            this.tag = tag
            addViewElements(
                data,
                false,
                hashMap,
                Pair(tag, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callback,
            )
            container?.addView(this)
        }
    }

    private var clientTypeSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[ClientType] = selectedID as String
            if (selectedID.equals(PostPartum, true)) {
                enablePostPartumChipView()
            } else {
                viewModel.resultHashMap.remove(PostPartum)
                binding.tvPostPartum.gone()
                binding.PostPartumRoot.removeAllViews()
                binding.PostPartumRoot.gone()
            }
            resultMapChanged()
        }

    private fun enablePostPartumChipView() {
        if (!(binding.tvPostPartum.isVisible())) {
            binding.tvPostPartum.visible()
            viewModel.contraceptiveMetaList.value?.data?.let { metaList ->
                addCustomView(
                    getSingleSelectionViewData(
                        metaList
                            .filter {
                                it.category.equals(
                                    MedicalReviewTypeEnums.post_partum.name,
                                    true,
                                )
                            }.sortedBy { it.displayOrder },
                    ),
                    PostPartum,
                    viewModel.resultHashMap,
                    postPartumSelectionCallBack,
                    binding.PostPartumRoot,
                )
            }
            binding.PostPartumRoot.visible()
        }
    }

    private var postPartumSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[PostPartum] = selectedID as String
            resultMapChanged()
        }

    private var combinedOralSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[CombineOralContraceptive] = selectedID as String
            if (selectedID.equals(OtherOralSpecify, true)) {
                binding.etCombinedOralContraceptiveComments.visible()
            } else {
                binding.etCombinedOralContraceptiveComments.gone()
                binding.etCombinedOralContraceptiveComments.text?.clear()
                viewModel.resultHashMap.remove(CombinedOralContraceptiveComments)
            }
            resultMapChanged()
        }

    private var progestinSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[ProgestinOnlyOrals] = selectedID as String
            when (selectedID.lowercase()) {
                Microlut.lowercase() -> {
                    binding.tvQuantityLabel.visible()
                    binding.etQuantity.visible()
                    binding.etProgestinOnlyOralsComments.text?.clear()
                    binding.etProgestinOnlyOralsComments.gone()
                    viewModel.resultHashMap.remove(OtherProgestinOnlyOralsComments)
                }
                OtherSpecify.lowercase() -> {
                    binding.etProgestinOnlyOralsComments.visible()
                    binding.etQuantity.text?.clear()
                    viewModel.resultHashMap.remove(MicrolutQuantity)
                    binding.tvQuantityLabel.gone()
                    binding.etQuantity.gone()
                }
            }
            resultMapChanged()
        }

    private var injectableSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[Injectables] = selectedID as String
            if (selectedID.equals(OtherInjectablesSpecify, true)) {
                binding.etOtherInjectableComments.visible()
            } else {
                binding.etOtherInjectableComments.gone()
                binding.etOtherInjectableComments.text?.clear()
                viewModel.resultHashMap.remove(OtherInjectableComments)
            }
            resultMapChanged()
        }

    private var implantsSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[Implants] = selectedID as String
            if (selectedID.equals(OtherImplantSpecify, true)) {
                binding.etOtherImplantsComments.visible()
            } else {
                binding.etOtherImplantsComments.gone()
                binding.etOtherImplantsComments.text?.clear()
                viewModel.resultHashMap.remove(OtherImplantComments)
            }
            resultMapChanged()
        }

    private var condomsSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[Condoms] = selectedID as String
            resultMapChanged()
        }

    private var emergencyContraSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[EmergencyContraceptive] = selectedID as String
            resultMapChanged()
        }

    private var permanentMethodSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultHashMap[PermanentMethod] = selectedID as String
            if (selectedID.equals(OtherFPMethod, true)) {
                binding.etOtherPermanentMethodComments.visible()
            } else {
                binding.etOtherPermanentMethodComments.gone()
                binding.etOtherPermanentMethodComments.text?.clear()
                viewModel.resultHashMap.remove(OtherPermanentMethodComments)
            }
            resultMapChanged()
        }

    private fun resultMapChanged() {
        setFragmentResult(
            MedicalReviewDefinedParams.CONTRACEPTIVES_ITEMS,
            bundleOf(
                MedicalReviewDefinedParams.CONTRACEPTIVES_VALUES to true,
            ),
        )
    }

    fun validInputs(): Boolean {
        var isValid: Boolean

        if (viewModel.resultHashMap.containsKey(ClientType) &&
            (viewModel.resultHashMap[ClientType] as? String)?.equals(
                PostPartum,
                true,
            ) == true
        ) {
            isValid = checkAndToggleError(PostPartum, binding.tvPostPartumErrorMessage)
            hideOtherErrorViews(binding.tvPostPartumErrorMessage.id)
        } else {
            binding.tvPostPartumErrorMessage.gone()
            isValid = true
        }

        if (isValid) {
            isValid = checkAndValidateOtherError(
                CombineOralContraceptive,
                binding.etCombinedOralContraceptiveComments,
                binding.tvContraceptivesErrorMessage,
                viewModel.combinedOralContraceptiveComments,
            )
            hideOtherErrorViews(binding.tvContraceptivesErrorMessage.id)
        }

        if (isValid && viewModel.resultHashMap.containsKey(ProgestinOnlyOrals)) {
            isValid = checkMicrolut()
            hideOtherErrorViews(binding.tvQuantityErrorMessage.id)
        }

        if (isValid) {
            isValid = checkAndValidateOtherError(
                ProgestinOnlyOrals,
                binding.etProgestinOnlyOralsComments,
                binding.tvQuantityErrorMessage,
                viewModel.otherProgestinOnlyOralsComments,
            )
            hideOtherErrorViews(binding.tvQuantityErrorMessage.id)
        }

        if (isValid) {
            isValid = checkAndValidateOtherError(
                Injectables,
                binding.etOtherInjectableComments,
                binding.tvInjectablesErrorMessage,
                viewModel.otherInjectableComments,
            )
            hideOtherErrorViews(binding.tvInjectablesErrorMessage.id)
        }

        if (isValid) {
            isValid = checkAndValidateOtherError(
                Implants,
                binding.etOtherImplantsComments,
                binding.tvImplantErrorMessage,
                viewModel.otherImplantComments,
            )
            hideOtherErrorViews(binding.tvImplantErrorMessage.id)
        }

        if (isValid) {
            isValid = checkAndValidateOtherError(
                PermanentMethod,
                binding.etOtherPermanentMethodComments,
                binding.tvPermanentMethodErrorMessage,
                viewModel.otherPermanentMethodComments,
            )
            hideOtherErrorViews(binding.tvPermanentMethodErrorMessage.id)
        }

        return isValid
    }

    private fun hideOtherErrorViews(errorView: Int) {
        val allErrorViews = listOf(
            binding.tvPostPartumErrorMessage.id,
            binding.tvQuantityErrorMessage.id,
            binding.tvContraceptivesErrorMessage.id,
            binding.tvProgestinErrorMessage.id,
            binding.tvInjectablesErrorMessage.id,
            binding.tvImplantErrorMessage.id,
            binding.tvPermanentMethodErrorMessage.id,
        )

        for (id in allErrorViews) {
            if (id != errorView) {
                val view = binding.root.findViewById<View>(id)
                view?.visibility = View.GONE
            }
        }
    }

    private fun checkAndToggleError(
        key: String,
        errorView: View,
    ): Boolean {
        if (viewModel.resultHashMap.containsKey(key)) {
            errorView.gone()
            return true
        } else {
            errorView.visible()
            return false
        }
    }

    private fun checkAndValidateOtherError(
        key: String,
        editView: View,
        errorView: View,
        comments: String?,
    ): Boolean {
        if ((
                viewModel.resultHashMap.containsKey(key) &&
                    ((viewModel.resultHashMap[key]) as? String)?.contains(
                        "other",
                    ) == true
            ) &&
            editView.isVisible() &&
            comments.isNullOrEmpty()
        ) {
            errorView.visible()
            return false
        } else {
            errorView.gone()
            return true
        }
    }

    private fun checkMicrolut(): Boolean {
        if ((viewModel.resultHashMap[ProgestinOnlyOrals] as? String)?.equals(Microlut, true) == true) {
            if (viewModel.quantity == null) {
                binding.tvQuantityErrorMessage.visible()
                return false
            } else {
                binding.tvQuantityErrorMessage.gone()
                return true
            }
        } else {
            binding.tvQuantityErrorMessage.gone()
            return true
        }
    }

    companion object {
        const val TAG = "ContraceptivesFragment"
    }
}
