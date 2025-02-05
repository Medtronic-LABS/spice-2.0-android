package com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentHba1cNudgesDialogBinding
import com.medtroniclabs.opensource.ncd.data.HBA1CModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.common.composeui.DialogComposeUtils
import com.medtroniclabs.opensource.ui.common.composeui.TextStyles
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationViewModel
import kotlin.math.roundToInt

class LipidsNudgesDialog : DialogFragment() {
    private val viewModel: InvestigationViewModel by activityViewModels()
    lateinit var binding: FragmentHba1cNudgesDialogBinding

    companion object {
        const val TAG = "LipidsNudgesDialog"
        fun newInstance(): LipidsNudgesDialog {
            return LipidsNudgesDialog()
        }
    }

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
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 70.dp)
                        .verticalScroll(rememberScrollState())
                        .wrapContentHeight()
                ) {
                    val lipidsList by viewModel.labTestPredictionLiveData.observeAsState()
                    when (lipidsList?.state) {
                        ResourceState.SUCCESS -> {
                            DisplayLipidsDetails(lipidsList?.data)
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

    @Composable
    private fun DisplayLipidsDetails(hba1cList: HashMap<String, Any>?) {
        var testText = ""
        val dataListLP = hba1cList?.get(NCDMRUtil.LipidProfile) as? ArrayList<*>?
        var list = dataListLP?.let {
            if (it.isNotEmpty()) {
                testText = getString(R.string.lipid_profile)
                ArrayList(it)
            } else null
        }
        val dataListRFT = hba1cList?.get(NCDMRUtil.RenalFunctionTest) as? ArrayList<*>?
        dataListRFT?.let {
            it.ifEmpty { null }?.let { renalList ->
                if (list.isNullOrEmpty()) {
                    testText = getString(R.string.renal_function_test)
                    list = ArrayList(renalList)
                } else {
                    testText =
                        "$testText ${getString(R.string.and)} ${getString(R.string.renal_function_test)}"
                    list?.addAll(renalList)
                }
            }
        }
        RowCell(
            stringResource(id = R.string.lipids_recommended_investigations, testText, testText),
            textStyle = TextStyles.labelTextStyle.copy(fontSize = TextStyles.FontSize_16),
            modifier = Modifier.padding(10.dp)
        )
        if ((list?.size ?: 0) > 0) {
            val data = list?.map {
                Gson().fromJson(Gson().toJson(it), HBA1CModel::class.java)
            } ?: emptyList()
            GenerateLipidsTable(ArrayList(data))
        } else {
            Text(
                getString(R.string.no_data_found),
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth(1f),
                textAlign = TextAlign.Center
            )
        }
        GenerateDescription(
            testText,
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
        )
    }

    @Composable
    private fun GenerateLipidsTable(lipids: ArrayList<*>?) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .border(
                    1.dp, colorResource(id = R.color.button_disabled),
                    shape = RoundedCornerShape(
                        CornerSize(10.dp)
                    )
                )
                .wrapContentHeight()
        ) {
            val listSize = (lipids?.size ?: 0) - 1
            AddTableHeader()
            DialogComposeUtils.DividerWidget()
            repeat(lipids?.size ?: 0) { itemIndex ->
                val item = lipids?.get(itemIndex)
                if (item != null && item is HBA1CModel) {
                    var isExpanded by rememberSaveable { mutableStateOf(false) }
                    val resultSize = item.labTestResults.size
                    item.testName?.let {
                        item.recommendedOn?.let { it1 ->
                            AddRowHeader(
                                it,
                                it1,
                                iconModifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                            ) {
                                isExpanded = !isExpanded
                            }
                        }
                    }
                    if (itemIndex < listSize)
                        DialogComposeUtils.DividerWidget()
                    if (isExpanded) {
                        AddResultRowView(resultSize, item, itemIndex, listSize)
                    }
                }
            }
        }
    }

    @Composable
    fun AddResultRowView(size: Int, item: HBA1CModel, itemIndex: Int, listSize: Int) {
        if (size > 0) {
            AddResultRows(
                item,
                size
            )
        } else {
            RowCell(
                "${getString(R.string.hyphen_symbol)} ${getString(R.string.hyphen_symbol)}",
                modifier = Modifier
                    .background(colorResource(id = R.color.primary_medium_blue_bg))
                    .padding(vertical = 10.dp, horizontal = 40.dp)
                    .fillMaxWidth(),
                textStyle = TextStyles.labelTextStyle
            )
        }
        if (itemIndex < listSize)
            DialogComposeUtils.DividerWidget()
    }

    @Composable
    private fun AddTableHeader() {
        val headerTextStyle =
            TextStyles.rowHeaderStyle.copy(
                color = colorResource(id = R.color.charcoal_grey),
                fontSize = 16.sp
            )
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowCell(
                text = getString(R.string.test_name),
                Modifier
                    .weight(0.8f)
                    .padding(
                        horizontal =
                        10.dp, vertical = 2.dp
                    ),
                headerTextStyle
            )
            RowCell(
                getString(R.string.review_date),
                Modifier
                    .weight(0.2f)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                headerTextStyle
            )
        }
    }

    @Composable
    private fun AddResultRows(
        item: HBA1CModel,
        listSize: Int
    ) {
        NonLazyGrid(
            columns = 2,
            itemCount = listSize,
            modifier = Modifier
                .background(colorResource(id = R.color.primary_medium_blue_bg))
                .padding(vertical = 10.dp, horizontal = 5.dp)
                .wrapContentWidth()
        ) { index ->
            item.labTestResults.get(index).let { resultItem ->
                val style = TextStyles.cellsTextStyle.copy(
                    fontSize = TextStyles.FontSize_14, color = colorResource(
                        id = R.color.text_label_color
                    )
                )
                Row(
                    modifier = Modifier
                        .padding(5.dp)
                ) {
                    RowCell(
                        resultItem.name ?: getString(R.string.hyphen_symbol),
                        Modifier
                            .weight(0.5f)
                            .padding(
                                start = 10.dp,
                                end = 5.dp,
                                top = 2.dp,
                                bottom = 2.dp
                            ),
                        style
                    )
                    RowCell(
                        text = getString(R.string.separator_colon),
                        Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                        style
                    )
                    RowCell(
                        if (resultItem.value?.isEmpty() == false) "${resultItem.value} ${resultItem.unit ?: ""}" else getString(
                            R.string.hyphen_symbol
                        ),
                        Modifier
                            .weight(0.5f)
                            .padding(start = 5.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                        style.copy(color = colorResource(id = R.color.secondary_black))
                    )
                }
            }
        }
    }

    @Composable
    private fun AddRowHeader(
        title: String,
        date: String,
        iconModifier: Modifier,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .fillMaxWidth()
                .clickable {
                    onClick.invoke()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(0.8f)
            ) {
                IconButton(
                    onClick = {
                        onClick.invoke()
                    }, modifier = Modifier
                        .padding(vertical = 5.dp)
                        .size(24.dp, 24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        tint = colorResource(id = R.color.table_title),
                        contentDescription = null,
                        modifier = iconModifier
                    )
                }
                RowCell(
                    title,
                    Modifier
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    TextStyles.rowHeaderStyle
                )
            }
            RowCell(
                DateUtils.convertDateFormat(
                    date,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMMyyyy
                ),
                Modifier
                    .weight(0.2f)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                TextStyles.rowHeaderStyle
            )
        }
    }

    @Composable
    fun RowCell(
        text: String,
        modifier: Modifier = Modifier,
        textStyle: TextStyle? = null,
        textAlign: TextAlign? = null
    ) {
        Text(
            text,
            modifier,
            style = textStyle ?: TextStyle(),
            fontFamily = textStyle?.fontFamily,
            fontWeight = textStyle?.fontWeight,
            fontSize = textStyle?.fontSize ?: TextUnit.Unspecified,
            textAlign = textAlign
        )
    }

    @Composable
    private fun GenerateDescription(testText: String, modifier: Modifier) {
        Row(
            modifier = modifier, verticalAlignment = Alignment.CenterVertically
        ) {
            val txStyle = TextStyles.labelTextStyle.copy(
                color = colorResource(id = R.color.primary_medium_blue),
                fontSize = TextStyles.FontSize_16
            )
            RowCell(
                stringResource(id = R.string.recommended_investigations),
                textStyle = TextStyles.labelTextStyle,
                modifier = Modifier
                    .weight(0.4f)
                    .padding(end = 2.dp)
            )
            RowCell(
                stringResource(id = R.string.separator_colon),
                textStyle = TextStyles.labelTextStyle,
            )
            val toFindText = " and"
            RowCell(
                if (testText.contains(toFindText))
                    testText.replace(toFindText, ",") else testText,
                textStyle = txStyle,
                modifier = Modifier
                    .weight(0.6f)
                    .padding(start = 10.dp)
            )
        }
    }

    @Composable
    private fun DrawBottomBar(modifier: Modifier) {
        DialogComposeUtils.CardBottomView(modifier = modifier) {
            closeDialog()
        }
    }

    private fun closeDialog() {
        this@LipidsNudgesDialog.dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.density * DefinedParams.DialogWidth).roundToInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    @Composable
    fun NonLazyGrid(
        columns: Int,
        itemCount: Int,
        modifier: Modifier = Modifier,
        content: @Composable() (Int) -> Unit
    ) {
        Column(modifier = modifier) {
            var rows = (itemCount / columns)
            if (itemCount.mod(columns) > 0) {
                rows += 1
            }

            for (rowId in 0 until rows) {
                val firstIndex = rowId * columns
                Row {
                    for (columnId in 0 until columns) {
                        val index = firstIndex + columnId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (index < itemCount) {
                                content(index)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
    }

}