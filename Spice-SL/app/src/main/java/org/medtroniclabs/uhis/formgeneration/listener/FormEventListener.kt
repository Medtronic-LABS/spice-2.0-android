package org.medtroniclabs.uhis.formgeneration.listener

import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.formgeneration.model.FormLayout

interface FormEventListener {
    fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long? = null,
    )

    fun onPopulate(targetId: String)

    fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    )

    fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>? = null,
        description: String? = null,
        dosageListModel: ArrayList<RecommendedDosageListModel>? = null,
    )

    fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>? = null,
    )

    fun onRenderingComplete()

    fun onUpdateInstruction(
        id: String,
        selectedId: Any? = null,
    )

    fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>? = null,
    )

    fun onAgeCheckForPregnancy()

    fun handleMandatoryCondition(formLayout: FormLayout? = null)

    fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>? = null,
        resultHashMap: HashMap<String, Any>,
    )

    fun onQRScanRequested()
}
