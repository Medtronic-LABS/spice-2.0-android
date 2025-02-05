package com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.FragmentHba1cNudgesDialogBinding
import com.medtroniclabs.opensource.ncd.data.HBA1CModel
import com.medtroniclabs.opensource.ncd.data.LabTestResult
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.common.composeui.DialogComposeUtils
import com.medtroniclabs.opensource.ui.common.composeui.TextStyles
import com.medtroniclabs.opensource.ui.common.composeui.TextStyles.labelTextStyle
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationViewModel

class HBA1CNudgesDialog(val callback: (isClosed: Boolean) -> Unit) : DialogFragment() {

    companion object {
        const val TAG = "HBA1CNudgesDialog"
        fun newInstance(callback: (isClosed: Boolean) -> Unit): HBA1CNudgesDialog {
            return HBA1CNudgesDialog(callback)
        }
    }

    private lateinit var binding: FragmentHba1cNudgesDialogBinding
    private val viewModel: InvestigationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHba1cNudgesDialogBinding.inflate(inflater, container, false)
        binding.composeView.setContent {
            Surface(
                shape = MaterialTheme.shapes.medium.copy(CornerSize(8.dp)),
                color = colorResource(id = R.color.white)
            ) {
                DisplayCardContent()
            }
        }
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    @Composable
    private fun DisplayCardContent() {
        Column(Modifier.wrapContentHeight()) {
            DialogComposeUtils.TitleDialogueView(
                title = stringResource(id = R.string.recommended_investigations)
            )
            Box {
                Column(
                    Modifier
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 80.dp)
                        .verticalScroll(rememberScrollState())
                        .wrapContentHeight()
                ) {
                    val hba1cList by viewModel.labTestPredictionLiveData.observeAsState()
                    when (hba1cList?.state) {
                        ResourceState.SUCCESS -> {
                            DisplayHBA1CDetails(hba1cList?.data)
                        }

                        else -> {
                            //else block
                        }
                    }
                }
                DrawBottomBar(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(colorResource(id = R.color.white))
                )
            }
        }
    }

    private fun closeDialog() {
        callback.invoke(true)
        this@HBA1CNudgesDialog.dismiss()
    }

    @Composable
    private fun DisplayHBA1CDetails(hba1cList: HashMap<String, Any>?) {
        RowCell(
            stringResource(id = R.string.hba1c_recommended_question),
            textStyle = labelTextStyle,
            modifier = Modifier.padding(10.dp)
        )
        val rawList = hba1cList?.get(NCDMRUtil.HbA1c) as? ArrayList<LinkedTreeMap<String, Any>>
        val dataList = rawList?.map {
            Gson().fromJson(Gson().toJson(it), HBA1CModel::class.java)
        }
        if (dataList.isNullOrEmpty()) {
            Text(
                getString(R.string.no_data_found),
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth(1f),
                textAlign = TextAlign.Center
            )
        } else GenerateHbA1cTable(ArrayList(dataList))
        GenerateDescription(
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 15.dp)
        )
    }

    @Composable
    private fun DrawBottomBar(modifier: Modifier) {
        DialogComposeUtils.CardBottomView(modifier = modifier) {
            closeDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            super.onViewCreated(view, savedInstanceState)
            isCancelable = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun GenerateDescription(modifier: Modifier) {
        val txStyle = labelTextStyle.copy(
            color = colorResource(id = R.color.primary_medium_blue),
            fontSize = TextStyles.FontSize_16
        )
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowCell(
                stringResource(id = R.string.recommended_investigations),
                textStyle = labelTextStyle,
                modifier = Modifier
                    .weight(0.5f)
                    .padding(end = 2.dp)
            )
            RowCell(
                stringResource(id = R.string.separator_colon),
                textStyle = labelTextStyle
            )
            RowCell(
                stringResource(id = R.string.glucose_hba1c),
                textStyle = txStyle,
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 10.dp)
            )
        }
    }

    @Composable
    fun GenerateHbA1cTable(hba1cList: ArrayList<HBA1CModel>) {
        val column1Weight = .5f
        val column2Weight = .5f
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .border(
                    1.dp, colorResource(id = R.color.button_disabled),
                    shape = RoundedCornerShape(
                        CornerSize(10.dp)
                    )
                )
                .wrapContentHeight()
        ) {
            AddTableHeader(column1Weight, column2Weight)
            DialogComposeUtils.DividerWidget()
            repeat(hba1cList.size) { itemIndex ->
                val item = hba1cList.get(itemIndex)
                if (item.labTestResults.isNotEmpty()) {
                    repeat(item.labTestResults.size) { index ->
                        item.labTestResults[index].let {
                            AddResultRows(
                                item,
                                it,
                                column1Weight,
                                column2Weight
                            )
                            AddDivider(itemIndex, hba1cList.size)
                        }
                    }
                } else {
                    AddResultRows(
                        item,
                        null,
                        column1Weight,
                        column2Weight
                    )
                    AddDivider(itemIndex, hba1cList.size)
                }
            }
        }
    }

    @Composable
    private fun AddResultRows(
        item: HBA1CModel,
        result: LabTestResult?,
        column1Weight: Float,
        column2Weight: Float
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
        ) {
            RowCell(
                if (result != null) "${result.value} ${result.unit}" else getString(R.string.hyphen_symbol),
                Modifier
                    .weight(column1Weight)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                TextStyles.cellsTextStyle
            )
            RowCell(
                item.recommendedOn?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    )
                } ?: getString(R.string.hyphen_symbol),
                Modifier
                    .weight(column2Weight)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                TextStyles.cellsTextStyle
            )
        }
    }

    @Composable
    private fun AddTableHeader(column1Weight: Float, column2Weight: Float) {
        Row(
            modifier = Modifier
                .padding(10.dp)
        ) {
            RowCell(
                getString(R.string.glucose_hba1c),
                Modifier
                    .weight(column1Weight)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                TextStyles.rowHeaderStyle
            )
            RowCell(
                getString(R.string.review_date),
                Modifier
                    .weight(column2Weight)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                TextStyles.rowHeaderStyle
            )
        }
    }

    @Composable
    fun AddDivider(itemIndex: Int, size: Int) {
        if (itemIndex < size - 1)
            DialogComposeUtils.DividerWidget()
    }

    @Composable
    fun RowCell(text: String, modifier: Modifier = Modifier, textStyle: TextStyle? = null) {
        Text(
            text,
            modifier,
            style = textStyle ?: TextStyle(),
            fontFamily = textStyle?.fontFamily,
            fontWeight = textStyle?.fontWeight,
            fontSize = textStyle?.fontSize ?: TextUnit.Unspecified
        )
    }
}