package com.medtroniclabs.opensource.ncd.medicalreview.prescription.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentPrescriptionResultDialogBinding
import com.medtroniclabs.opensource.ncd.data.PrescriptionResults
import com.medtroniclabs.opensource.ncd.data.RecentBGLogs
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.viewmodel.NCDPrescriptionViewModel


class PrescriptionResultDialogFragment : DialogFragment() {

    lateinit var binding: FragmentPrescriptionResultDialogBinding
    private val prescriptionViewModel: NCDPrescriptionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPrescriptionResultDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachObserver()
    }

    companion object {
        const val TAG = "PrescriptionResultDialogFragment"
        fun newInstance(): PrescriptionResultDialogFragment {
            return PrescriptionResultDialogFragment()
        }
    }

    private fun attachObserver() {
        prescriptionViewModel.prescriptionPredictionResponseLiveDate.value?.data?.let { resourceState ->
            resourceState.recentBGLogs.let { bloodGlucoseList ->
                binding.medicationNudegeComposeView.setContent {
                    MedicationNudgeDialogueView(
                        Modifier,
                        bloodGlucoseList,
                        resourceState.prescriptionResults
                    )
                }
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

    @Composable
    fun MedicationNudgeDialogueView(
        modifier: Modifier,
        bloodGlucoseList: ArrayList<RecentBGLogs>,
        prescriptionResults: ArrayList<PrescriptionResults>
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium.copy(androidx.compose.foundation.shape.CornerSize(8.dp)),
            color = colorResource(id = R.color.white)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TitleDialogueView(stringResource(id = R.string.treatment_intensification))
                CardContentView(Modifier, bloodGlucoseList)
                PrescriptionDetailView(Modifier, getMedicationName(prescriptionResults))
                Spacer(modifier = Modifier.height(30.dp))
                Divider(thickness = (0.5).dp, color = colorResource(id = R.color.gray_bg_site))
                CardBottomView(
                    Modifier
                        .align(Alignment.End)
                )
            }
        }

    }

    @Composable
    fun CardBottomView(modifier: Modifier) {
        Row(
            modifier = modifier
                .padding(16.dp),
        ) {
            Button(shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.cobalt_blue)),
                onClick = {
                    onDoneClicked()
                }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    }

    private fun onDoneClicked() {
        this.dismiss()
    }

    private fun getMedicationName(prescriptionResult: ArrayList<PrescriptionResults>): String? {
        val medicationNameList = ArrayList<String>()
        prescriptionResult.forEach { result ->
            result.apply {
                var name = ""
                if (medicationName != null)
                    name += medicationName
                if (dosageUnitValue != null && dosageUnitName != null)
                    name = "$name ( $dosageUnitValue $dosageUnitName )"
                if (dosageFrequencyName != null)
                    name = "$name $dosageFrequencyName"
                medicationNameList.add(name)
            }
        }
        return if (medicationNameList.size > 0) {
            medicationNameList.joinToString(separator = ", ")
        } else {
            null
        }
    }

    @Composable
    private fun PrescriptionDetailView(modifier: Modifier, medicationNameDetails: String?) {
        Row(modifier = modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
            Text(
                text = stringResource(id = R.string.presciption_since_last_3),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = TextUnit(16f, TextUnitType.Sp),
                color = colorResource(id = R.color.table_title)
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp)
            )
            Text(
                text = medicationNameDetails ?: stringResource(id = R.string.hyphen_symbol),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = TextUnit(16f, TextUnitType.Sp),
                color = colorResource(id = R.color.secondary_black)
            )
        }
    }

    @Composable
    fun CardContentView(modifier: Modifier, bgList: ArrayList<RecentBGLogs>) {
        Column(modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            HeaderCardContent(
                stringResource(id = R.string.medication_nudege_card_header_text_prefix),
                stringResource(id = R.string.medication_nudege_card_header_text_sufix)
            )
            MedicationNudgeTable(Modifier, bgList)
        }
    }

    @Composable
    fun MedicationNudgeTable(modifier: Modifier, bgList: ArrayList<RecentBGLogs>) {
        Column(
            modifier = modifier.border(
                width = (0.5).dp,
                color = colorResource(id = R.color.border_color_medication),
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            MedicationNudgeTableRow(
                stringResource(id = R.string.glucose_fbs),
                stringResource(id = R.string.glucose_rbs),
                stringResource(id = R.string.glucose_hba1c),
                stringResource(id = R.string.review_date),
                colorResource(id = R.color.table_title),
                TextUnit(16f, TextUnitType.Sp)
            )
            Divider(
                modifier = Modifier,
                color = colorResource(id = R.color.border_color_medication),
                thickness = (0.5).dp
            )
            LazyColumn {
                itemsIndexed(bgList) { index, recentBg ->
                    when (recentBg.glucoseType) {
                        DefinedParams.fbs -> {
                            MedicationNudgeTableRow(
                                fbs = getGlucoseValue(
                                    recentBg.glucoseValue,
                                    recentBg.glucoseUnit
                                ),
                                rbs = stringResource(id = R.string.hyphen_symbol),
                                hba1c = getGlucoseValue(
                                    recentBg.hba1c,
                                    recentBg.hba1cUnit
                                ),
                                reviewDate = getReviewDate(recentBg.glucoseDateTime)
                                    ?: stringResource(id = R.string.hyphen_symbol),
                                colorResource = colorResource(id = R.color.secondary_black),
                                textUnit = TextUnit(16f, TextUnitType.Sp)
                            )
                        }

                        DefinedParams.rbs -> {
                            MedicationNudgeTableRow(
                                fbs = stringResource(id = R.string.hyphen_symbol),
                                rbs = getGlucoseValue(
                                    recentBg.glucoseValue,
                                    recentBg.glucoseUnit
                                ),
                                hba1c = getGlucoseValue(recentBg.hba1c,
                                    recentBg.hba1cUnit),
                                reviewDate = getReviewDate(recentBg.glucoseDateTime)
                                    ?: stringResource(id = R.string.hyphen_symbol),
                                colorResource = colorResource(id = R.color.secondary_black),
                                textUnit = TextUnit(16f, TextUnitType.Sp)
                            )
                        }

                        else -> {
                            MedicationNudgeTableRow(
                                fbs = stringResource(id = R.string.hyphen_symbol),
                                rbs = stringResource(id = R.string.hyphen_symbol),
                                hba1c = getGlucoseValue(recentBg.hba1c,
                                    recentBg.hba1cUnit),
                                reviewDate = getReviewDate(recentBg.glucoseDateTime),
                                colorResource = colorResource(id = R.color.secondary_black),
                                textUnit = TextUnit(16f, TextUnitType.Sp)
                            )
                        }
                    }
                    if (index != bgList.size - 1) {
                        Divider(
                            modifier = Modifier,
                            color = colorResource(id = R.color.border_color_medication),
                            thickness = (0.5).dp
                        )
                    }
                }
            }
        }
    }

    private fun getGlucoseValue(glucoseValue: Double?, glucoseUnit: String?): String {
        return glucoseValue?.let { value ->
            val formattedValue = CommonUtils.getDecimalFormatted(value)
            if (glucoseUnit != null) {
                "$formattedValue $glucoseUnit"
            } else {
                formattedValue
            }
        } ?: getString(R.string.hyphen_symbol)
    }

    private fun getReviewDate(bgTakenOn: String?): String {

        if (bgTakenOn != null) {

            return DateUtils.convertDateTimeToDate(
                bgTakenOn,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_ddMMMyyyy
            )
        }

        return ""
    }

    @Composable
    fun MedicationNudgeTableRow(
        fbs: String,
        rbs: String,
        hba1c: String,
        reviewDate: String,
        colorResource: Color,
        textUnit: TextUnit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            RowValueText(modifier = Modifier.weight(1f), text = fbs, colorResource, textUnit)
            RowValueText(modifier = Modifier.weight(1f), text = rbs, colorResource, textUnit)
            RowValueText(modifier = Modifier.weight(1f), text = hba1c, colorResource, textUnit)
            RowValueText(modifier = Modifier.weight(1f), text = reviewDate, colorResource, textUnit)
        }
    }


    @Composable
    fun RowValueText(modifier: Modifier, text: String, colorResource: Color, textUnit: TextUnit) {
        Text(
            modifier = modifier,
            text = text,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            color = colorResource,
            fontSize = textUnit,
        )
    }


    @Composable
    fun HeaderCardContent(titlePrefix: String, titleSuffix: String) {
        val annotatedString = buildAnnotatedString {
            append(titlePrefix)
            withStyle(style = SpanStyle(colorResource(id = R.color.a_red_error))) {
                append(" $titleSuffix")
            }
        }
        Column {
            Text(
                text = annotatedString,
                fontFamily = FontFamily(Font(R.font.inter_bold, FontWeight(700))),
                fontSize = TextUnit(16f, TextUnitType.Sp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    fun TitleDialogueView(title: String) {
        Surface(
            modifier = Modifier.fillMaxWidth(), color = colorResource(id = R.color.gray_bg_site)
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.Unspecified
            ) {
                Text(
                    text = title,
                    color = colorResource(id = R.color.secondary_black),
                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            }
        }
    }

}