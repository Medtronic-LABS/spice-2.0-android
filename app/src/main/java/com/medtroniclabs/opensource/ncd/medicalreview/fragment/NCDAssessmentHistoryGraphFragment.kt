package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.FragmentNcdAssessmentHistoryGraphBinding
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.data.BPLogList
import com.medtroniclabs.opensource.ncd.data.GlucoseLogList
import com.medtroniclabs.opensource.ncd.data.GraphModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.BG_TAG
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.BP_TAG
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.FBS
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.HbA1c
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.RBS
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.Systolic
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.TAG
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.bg
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.bp
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.fbs
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.fbs_code
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.fbs_rbs_code
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.hba1c_code
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.mgdl
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.mmhg
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.mmoll
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.percentage
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.rbs
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.rbs_code
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDBpAndBgViewModel
import com.medtroniclabs.opensource.ui.BaseFragment

class NCDAssessmentHistoryGraphFragment : BaseFragment(), OnChartGestureListener,
    OnChartValueSelectedListener, View.OnClickListener {

    private var graphType: String? = null
    private var graphDetails: Any? = null
    private var systolicXYValues: ArrayList<Entry>? = null
    private var diastolicXYValues: ArrayList<Entry>? = null
    private var hbA1CXYValues: ArrayList<Entry>? = null
    private var lineDataSets: ArrayList<ILineDataSet>? = null
    private var dateList: ArrayList<Triple<Int, String, String>>? = null
    private var unitValue: ArrayList<Pair<Int, String?>>? = null
    private lateinit var binding: FragmentNcdAssessmentHistoryGraphBinding
    private val viewModel: NCDBpAndBgViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNcdAssessmentHistoryGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance() =
            NCDAssessmentHistoryGraphFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        graphDetails?.let { details ->
            val bpbgDetails = details as? BPBGListModel
            bpbgDetails?.let {
                setGraphProperty(
                    binding.lineChart,
                    1,
                    bpDetails = it.takeIf { graphType?.equals(bp, true) == true },
                    bgDetails = it.takeIf { graphType?.equals(bp, true) != true })
            }
        }

        attachObserver()
    }

    private fun attachObserver() {
        viewModel.selectedBGDropDown.observe(viewLifecycleOwner) { selectedBG ->
            if (graphType != bp) {
                (graphDetails as? BPBGListModel)?.let { list ->
                    setGraphProperty(
                        binding.lineChart,
                        1,
                        bgDetails = list,
                        selectedBGDropDown = selectedBG
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            graphType = when (getString(TAG)) {
                BP_TAG -> bp
                BG_TAG -> bg
                else -> null
            }
            graphDetails = getString(NCDMRUtil.graphDetails)?.let {
                Gson().fromJson(
                    it,
                    BPBGListModel::class.java
                )
            }
        }
    }

    override fun onClick(v: View?) {
        /**
         * this method is not used
         */
    }

    override fun onChartGestureStart(
        me: MotionEvent?,
        lastPerformedGesture: ChartTouchListener.ChartGesture?
    ) {
        /**
         * this method is not used
         */
    }

    private fun convertDate(bpResponse: BPLogList): String {
        return DateUtils.convertDateTimeToDate(
            bpResponse.bpTakenOn,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_FORMAT_ddMMyy_GRAPH
        ).ifEmpty { getString(R.string.separator_hyphen) }
    }

    private fun getLimitLine(
        lineSetOne: Float,
        lineSetOneTitle: String,
        pos: LimitLine.LimitLabelPosition,
        size: Float = 12F,
        color: Int
    ): LimitLine {
        return LimitLine(lineSetOne, lineSetOneTitle).apply {
            lineWidth = 1f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = pos
            textSize = size
            textColor = requireContext().getColor(color)
            lineColor = requireContext().getColor(color)
        }
    }

    private fun setGraphProperty(
        lineChart: LineChart,
        option: Int,
        bpDetails: BPBGListModel? = null,
        bgDetails: BPBGListModel? = null,
        selectedBGDropDown: Int = 3
    ) {
        if (!lineChart.isEmpty) { // reset graph when frequency changed
            lineChart.moveViewToX(0f)
            lineChart.resetZoom()
            lineChart.fitScreen()
            lineChart.clearValues()
            lineChart.clear()
        }
        lineChart.description.isEnabled = false
        // enable touch gestures
        // enable touch gestures
        lineChart.setTouchEnabled(true)
        lineChart.dragDecelerationFrictionCoef = 0.9f
        // enable scaling and dragging
        // enable scaling and dragging
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)

        lineChart.isScaleXEnabled = true
        lineChart.setBackgroundColor(Color.parseColor("#ffffff")) //set whatever color you prefer

        lineChart.setDrawGridBackground(false) // this is a must

        lineChart.setDrawBorders(false)
        lineChart.setOnChartValueSelectedListener(this)

        lineChart.setNoDataText(getString(R.string.no_data_found))
        val paint = lineChart.getPaint(Chart.PAINT_INFO)
        paint.color = ContextCompat.getColor(requireContext(), R.color.cobalt_blue)
        paint.textSize = 40f
        lineChart.setNoDataTextTypeface(
            ResourcesCompat.getFont(
                requireContext(),
                R.font.inter_regular
            )
        )

        // set offsets to display label values without clip.
        lineChart.zoom(1.0f, 0f, 1f, 0f)
        lineChart.invalidate()
        loadChartData(bpDetails, bgDetails)
        plotGraph(bpDetails, bgDetails, selectedBGDropDown)
        // get the legend (only possible after setting data)
        val legend = lineChart.legend
        legend.yOffset = 20f
        legend.isEnabled = false
        val markerView = NCDAssessmentMarkerView(
            requireContext(),
            R.layout.custom_marker_view_layout,
            systolicXYValues,
            diastolicXYValues,
            hbA1CXYValues,
            selectedBGDropDown,
            dateList,
            unitValue,
            graphType
        )
        lineChart.marker = markerView
        val xAxis = lineChart.xAxis
        xAxis.setDrawGridLines(true)
        xAxis.axisMinimum = 0f
        xAxis.setCenterAxisLabels(false)
        xAxis.spaceMax = 1f
        xAxis.isEnabled = true
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.typeface = Typeface.SANS_SERIF
        xAxis.typeface = Typeface.DEFAULT_BOLD
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.graph_xaxis_textcolor)
        xAxis.setDrawAxisLine(true)
        xAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.graph_limit_linecolor)
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f
        binding.lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override
            fun getFormattedValue(value: Float): String {
                dateList?.let { list ->
                    val containedList = list.filter { it.first.toFloat() == value }
                    return if (containedList.isNotEmpty()) {
                        containedList[0].second
                    } else {
                        ""
                    }
                } ?: kotlin.run {
                    return ""
                }
            }
        }

        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.setDrawAxisLine(true)
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = when (graphType) {
            bp -> 350f
            bg -> getUpperLimit()
            else -> {
                0.0f
            }
        }
        yAxisLeft.yOffset = 1f
        yAxisLeft.isEnabled = true
        yAxisLeft.typeface = Typeface.SANS_SERIF
        yAxisLeft.typeface = Typeface.DEFAULT_BOLD
        yAxisLeft.textColor =
            ContextCompat.getColor(requireContext(), R.color.graph_xaxis_textcolor)
        yAxisLeft.gridColor =
            ContextCompat.getColor(requireContext(), R.color.graph_limit_linecolor)
        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = false
        yAxisLeft.removeAllLimitLines() // reset all limit lines to avoid overlapping lines
        binding.lineChart.isScaleYEnabled = false
        var lineSetOneTitle = ""
        var lineSetTwoTitle = ""
        var lineSetThreeTitle = ""
        var lineSetOne = 0f
        var lineSetTwo = 0f
        var lineSetThree = 0f
        bpDetails?.bpThreshold?.let {
            lineSetOneTitle = "(Dia: ${it.diastolic} ${mmhg})"
            lineSetTwoTitle = "(Sys: ${it.systolic} ${mmhg})"
            lineSetOne = it.diastolic.toFloat()
            lineSetTwo = it.systolic.toFloat()

            yAxisLeft.addLimitLine(
                getLimitLine(
                    lineSetOne,
                    lineSetOneTitle,
                    LimitLine.LimitLabelPosition.LEFT_TOP,
                    color = R.color.ncd_accent
                )
            )
            yAxisLeft.addLimitLine(
                getLimitLine(
                    lineSetTwo,
                    lineSetTwoTitle,
                    LimitLine.LimitLabelPosition.LEFT_TOP,
                    color = R.color.primary_medium_blue
                )
            )
        }
        bgDetails?.glucoseThreshold?.let { thresholdList ->
            bgDetails.glucoseLogList?.find { it.glucoseUnit?.equals(mmoll, true) == true }?.let {
                thresholdList.find { it.unit.equals(mmoll, true) }?.let { res ->
                    lineSetOneTitle = "(FBS: ${res.fbs} ${mmoll})"
                    lineSetTwoTitle = "(RBS: ${res.rbs} ${mmoll})"
                    lineSetOne = res.fbs.toFloat()
                    lineSetTwo = res.rbs.toFloat()

                    yAxisLeft.addLimitLine(
                        getLimitLine(
                            lineSetOne,
                            lineSetOneTitle,
                            LimitLine.LimitLabelPosition.LEFT_TOP,
                            color = R.color.ncd_accent
                        )
                    )

                    yAxisLeft.addLimitLine(
                        getLimitLine(
                            lineSetTwo,
                            lineSetTwoTitle,
                            LimitLine.LimitLabelPosition.LEFT_TOP,
                            color = R.color.primary_medium_blue
                        )
                    )
                }
            }
            bgDetails.glucoseLogList?.find { it.glucoseUnit?.equals(mgdl, true) == true }?.let {
                thresholdList.find { it.unit.equals(mgdl, true) }?.let { res ->
                    lineSetOneTitle = "(FBS: ${res.fbs} $mgdl)"
                    lineSetTwoTitle = "(RBS: ${res.rbs} $mgdl)"
                    lineSetOne = res.fbs.toFloat()
                    lineSetTwo = res.rbs.toFloat()

                    yAxisLeft.addLimitLine(
                        getLimitLine(
                            lineSetOne,
                            lineSetOneTitle,
                            LimitLine.LimitLabelPosition.LEFT_TOP,
                            color = R.color.ncd_accent
                        )
                    )
                    yAxisLeft.addLimitLine(
                        getLimitLine(
                            lineSetTwo,
                            lineSetTwoTitle,
                            LimitLine.LimitLabelPosition.LEFT_TOP,
                            color = R.color.primary_medium_blue
                        )
                    )
                }
            }
            thresholdList.find { it.unit == percentage }?.let {
                if (selectedBGDropDown == hba1c_code) {
                    lineSetThreeTitle = "(HbA1c: ${it.hba1c} ${percentage})"
                    lineSetThree = it.hba1c
                    yAxisLeft.addLimitLine(
                        getLimitLine(
                            lineSetThree,
                            lineSetThreeTitle,
                            LimitLine.LimitLabelPosition.LEFT_TOP,
                            color = R.color.purple_200
                        )
                    )
                    yAxisLeft.axisMaximum = 50f
                }
            }
        }

        var limit = 0f
        bpDetails?.bpLogList?.let {
            limit = it.size.toFloat()
        }
        bgDetails?.glucoseLogList?.let {
            limit = it.size.toFloat()
        }
        if (option == 1) {
            lineChart.setVisibleXRangeMaximum(7f)
        } else {
            binding.lineChart.zoomToCenter(limit / 7f, 0f)
        }
        binding.lineChart.moveViewToX(limit)
        lineChart.invalidate()
    }

    private fun loadChartData(
        bpDetails: BPBGListModel? = null,
        bgDetails: BPBGListModel? = null
    ) {
        diastolicXYValues = ArrayList()
        systolicXYValues = ArrayList()
        hbA1CXYValues = ArrayList()
        dateList = ArrayList()
        unitValue = ArrayList()
        bpDetails?.bpLogList?.let { list ->
            list.forEachIndexed { index, bpResponse ->
                systolicXYValues?.add(
                    Entry(
                        (index + 1).toFloat(),
                        bpResponse.avgSystolic?.toFloat() ?: 0.0f
                    )
                )
                diastolicXYValues?.add(
                    Entry(
                        (index + 1).toFloat(),
                        bpResponse.avgDiastolic?.toFloat() ?: 0.0f
                    )
                )
                val date = convertDate(bpResponse)
                dateList?.add(Triple(index + 1, date, Systolic))
            }
        }
        bgDetails?.glucoseLogList?.let { list ->
            list.forEachIndexed { index, bgResponse ->
                if (viewModel.selectedBGDropDown.value == hba1c_code) {
                    populateBgDetails(index, bgResponse)
                } else {
                    bgResponse.glucoseValue?.let { glucoseValue ->
                        var isfbs: String = getGlucoseType(bgResponse, glucoseValue, index)
                        val date = DateUtils.convertDateTimeToDate(
                            bgResponse.glucoseDateTime,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_FORMAT_ddMMyy_GRAPH
                        )
                        dateList?.add(Triple(index + 1, date, isfbs))
                        unitValue?.add(Pair(index + 1, bgResponse.glucoseUnit))
                    }
                }
            }
        }
    }

    private fun populateBgDetails(index: Int, bgResponse: GlucoseLogList) {
        bgResponse.hba1c?.let { hba1cValue ->
            hbA1CXYValues?.add(
                Entry(
                    (index + 1).toFloat(),
                    hba1cValue,
                    bgResponse.encounterId
                )
            )
            val bgDate =
                if (!bgResponse.hba1cDateTime.isNullOrEmpty()) bgResponse.hba1cDateTime else bgResponse.glucoseDateTime
            if (!bgDate.isNullOrEmpty()) {
                val date = DateUtils.convertDateTimeToDate(
                    bgDate,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMyy_GRAPH
                )
                dateList?.add(Triple(index + 1, date, HbA1c))
            }
        }
    }

    private fun getGlucoseType(
        bgResponse: GlucoseLogList,
        glucoseValue: Float,
        index: Int
    ): String {
        val type: String
        if (bgResponse.glucoseType?.equals(rbs, true) == true) {
            type = RBS
            systolicXYValues?.add(
                Entry(
                    (index + 1).toFloat(),
                    glucoseValue,
                    bgResponse.encounterId
                )
            )
        } else {
            type = FBS
            diastolicXYValues?.add(
                Entry(
                    (index + 1).toFloat(),
                    glucoseValue,
                    bgResponse.encounterId
                )
            )
        }
        return type
    }

    private fun getUpperLimit(): Float {
        var upperLimit = 40f
        graphDetails?.let { graph ->
            (graph as? BPBGListModel)?.let { data ->
                data.glucoseLogList?.filter { it.glucoseUnit?.equals(mgdl, true) == true }?.let {
                    if (it.isNotEmpty())
                        upperLimit = 600f
                }
            }
        }
        return upperLimit
    }

    private fun plotGraph(
        bpDetails: BPBGListModel? = null,
        bgDetails: BPBGListModel? = null,
        selectedBGDropDown: Int
    ) {
        var dataSetOneTitle = ""
        var dataSetTwoTitle = ""
        var dataSetThreeTitle = ""
        bpDetails?.bpThreshold?.let {
            dataSetOneTitle = "Sys: ${it.systolic} (Optimal)"
            dataSetTwoTitle = "Dia: ${it.diastolic} (Optimal)"
        }
        bgDetails?.glucoseThreshold?.get(0)?.let {
            dataSetOneTitle = "RBS: ${it.rbs} (Optimal)"
            dataSetTwoTitle = "FBS: ${it.fbs} (Optimal)"
            dataSetThreeTitle = "HbA1c: ${it.hba1c} (Optimal)"
        }

        val dataSetOne = LineDataSet(systolicXYValues, dataSetOneTitle)
        dataSetOne.color = requireContext().getColor(R.color.primary_medium_blue)
        dataSetOne.lineWidth = 2f
        //Change color on selction of point , 2. Drawing VerticalLine on selection
        //Change color on selction of point , 2. Drawing VerticalLine on selection
        dataSetOne.highLightColor =
            requireContext().getColor(R.color.primary_medium_blue)
        dataSetOne.setDrawHorizontalHighlightIndicator(false)

        dataSetOne.circleRadius = 5f

        dataSetOne.setDrawCircleHole(true)
        //for different circle color around
        dataSetOne.setCircleColors(requireContext().getColor(R.color.primary_medium_blue))

        //Removing values
        dataSetOne.setDrawValues(false)
        dataSetOne.axisDependency = YAxis.AxisDependency.LEFT
        dataSetOne.color = requireContext().getColor(R.color.primary_medium_blue)
        dataSetOne.lineWidth = 1.5f
        dataSetOne.setDrawCircles(true)
        dataSetOne.fillAlpha = 0
        dataSetOne.fillColor = requireContext().getColor(R.color.primary_medium_blue)
        dataSetOne.highLightColor =
            ContextCompat.getColor(requireContext(), R.color.primary_medium_blue)
        dataSetOne.setDrawCircleHole(false)

        val dataSetTwo = LineDataSet(diastolicXYValues, dataSetTwoTitle)
        dataSetTwo.color =
            ContextCompat.getColor(requireContext(), R.color.ncd_accent)
        dataSetTwo.lineWidth = 2f
        //Change color on selction of point , 2. Drawing VerticalLine on selection
        //Change color on selction of point , 2. Drawing VerticalLine on selection
        dataSetTwo.highLightColor =
            ContextCompat.getColor(requireContext(), R.color.ncd_accent)
        dataSetTwo.setDrawHorizontalHighlightIndicator(false)
        dataSetTwo.circleRadius = 5f

        dataSetTwo.setDrawCircleHole(false)
        //for different circle color around
        //for different circle color around
        dataSetTwo.setCircleColors(requireContext().getColor(R.color.ncd_accent))
        dataSetTwo.setDrawValues(false)

        val dataSetThree = LineDataSet(hbA1CXYValues, dataSetThreeTitle)
        dataSetThree.color =
            ContextCompat.getColor(requireContext(), R.color.purple_200)
        dataSetThree.lineWidth = 2f
        dataSetThree.highLightColor =
            ContextCompat.getColor(requireContext(), R.color.purple_200)
        dataSetThree.setDrawHorizontalHighlightIndicator(false)
        dataSetThree.circleRadius = 5f

        dataSetThree.setDrawCircleHole(false)
        dataSetThree.setCircleColors(requireContext().getColor(R.color.purple_200))
        dataSetThree.setDrawValues(false)

        lineDataSets = ArrayList()
        // create a data object with the datasets
        // Don't change the Data Set order added here sys, dia, pulse as its dependant for marker.
        when {
            (graphType?.equals(bp, true) == true || selectedBGDropDown == fbs_rbs_code) -> {
                addLineDataSets(dataSetOne, dataSetTwo, lineDataSets)
            }

            (graphType?.equals(bg, true) == true) -> {
                if (selectedBGDropDown == rbs_code && dataSetOne.entryCount > 0) {
                    lineDataSets?.add(dataSetOne)
                }
                if (selectedBGDropDown == fbs_code && dataSetTwo.entryCount > 0) {
                    lineDataSets?.add(dataSetTwo)
                }
                if (selectedBGDropDown == hba1c_code && dataSetThree.entryCount > 0) {
                    lineDataSets?.add(dataSetThree)
                }
            }
        }
        val data = LineData(lineDataSets)
        // set data
        lineDataSets?.let {
            if (it.size > 0)
                binding.lineChart.data = data
        }
    }

    private fun addLineDataSets(
        dataSetOne: LineDataSet,
        dataSetTwo: LineDataSet,
        lineDataSets: ArrayList<ILineDataSet>?
    ) {
        if (dataSetOne.entryCount > 0)
            lineDataSets?.add(dataSetOne)
        if (dataSetTwo.entryCount > 0)
            lineDataSets?.add(dataSetTwo)
    }

    override fun onChartGestureEnd(
        me: MotionEvent?,
        lastPerformedGesture: ChartTouchListener.ChartGesture?
    ) {
        /**
         * this method is not used
         */
    }

    override fun onChartLongPressed(me: MotionEvent?) {
        /**
         * this method is not used
         */
    }

    override fun onChartDoubleTapped(me: MotionEvent?) {
        /**
         * this method is not used
         */
    }

    override fun onChartSingleTapped(me: MotionEvent?) {
        /**
         * this method is not used
         */
    }

    override fun onChartFling(
        me1: MotionEvent?,
        me2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ) {
        /**
         * this method is not used
         */
    }

    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
        /**
         * this method is not used
         */
    }

    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
        /**
         * this method is not used
         */
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        val dataSetIndex = binding.lineChart.data.getDataSetForEntry(e)
        val entryIndex = dataSetIndex.getEntryIndex(e)
        when {
            graphType?.equals(bp, true) == true -> {
                (graphDetails as? BPBGListModel?)?.bpLogList?.let {
                    onBPValueSelect(it, entryIndex)
                }
            }

            graphType?.equals(bg, true) == true -> {
                val dropdownSelected = viewModel.selectedBGDropDown.value
                var type: String? = null
                when (dropdownSelected) {
                    fbs_code -> type = fbs
                    rbs_code -> type = rbs
                    hba1c_code -> type = HbA1c
                }
                when (dropdownSelected) {
                    fbs_rbs_code -> {
                        (graphDetails as? BPBGListModel?)?.glucoseLogList?.filter {
                            (it.glucoseType?.equals(
                                FBS,
                                ignoreCase = true
                            ) == true) || (it.glucoseType?.equals(RBS, true) == true)
                        }?.let { bgList ->
                            refreshView(bgList, e)
                        }
                    }

                    hba1c_code -> {
                        (graphDetails as? BPBGListModel?)?.glucoseLogList?.filter { (it.hba1c != null) }
                            ?.let { bgList ->
                                refreshView(bgList, e)
                            }
                    }

                    else -> {
                        (graphDetails as? BPBGListModel?)?.glucoseLogList?.filter {
                            it.glucoseType.equals(
                                type,
                                ignoreCase = true
                            )
                        }?.let { bgList ->
                            if (bgList.isNotEmpty())
                                refreshView(bgList, e)
                        }
                    }
                }
            }
        }
    }

    private fun refreshView(bgList: List<GlucoseLogList>, e: Entry?) {
        e?.data?.let { entryData ->
            entryData as Long
            bgList.indexOfFirst { it.encounterId == entryData }.let {
                if (it != -1) {
                    viewModel.onBGValueSelectedObserver.value =
                        GraphModel(null, bgList[it], it, bgList.size)
                }
            }
        }
    }

    private fun onBPValueSelect(list: ArrayList<BPLogList>, entryIndex: Int) {
        if (entryIndex >= 0 && entryIndex < list.size)
            viewModel.onBPValueSelectedObserver.value =
                GraphModel(list[entryIndex], null, entryIndex, list.size)
    }

    override fun onNothingSelected() {
        /**
         * this method is not used
         */
    }
}