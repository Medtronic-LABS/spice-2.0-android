package com.medtroniclabs.opensource.ui.common.composeui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.medtroniclabs.opensource.R

object TextStyles {

    val FontSize_14 = 14.sp//-6 from usual
    val FontSize_15 = 15.sp
    val FontSize_16 = 16.sp
    val TableHeader = FontSize_14

    val interFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.W400),
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_bold, FontWeight.W700),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_medium, FontWeight.W500),
        Font(R.font.inter_medium, FontWeight.Medium),
    )

    val rowHeaderStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF001E46),
        fontSize = FontSize_14
    )

    val cellsTextStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF001E46),
        fontSize = TableHeader
    )

    val labelTextStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF000000),
        fontSize = TableHeader
    )

    val buttonTextStyle = TextStyle(
        fontFamily =  interFamily,
        fontWeight = FontWeight.Normal,
        color = Color(0xFFFFFFFF),
        fontSize = FontSize_16
    )
}