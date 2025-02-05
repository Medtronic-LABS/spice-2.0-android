package com.medtroniclabs.opensource.ui.common.composeui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.medtroniclabs.opensource.R

object DialogComposeUtils {

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

    @Composable
    fun CardBottomView(modifier: Modifier, onClick: (() -> Unit?)? = null) {
        Surface(modifier = modifier) {
            DividerWidget()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Button(
                    modifier = Modifier.widthIn(min = 100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cobalt_blue),
                        contentColor = colorResource(id = R.color.white)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    onClick = {
                        onClick?.invoke()
                    }
                ) {
                    TextStyles.buttonTextStyle.let {
                        Text(
                            stringResource(id = R.string.ok),
                            style = it,
                            fontFamily = it.fontFamily,
                            fontWeight = it.fontWeight,
                            fontSize = it.fontSize
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DividerWidget(modifier: Modifier = Modifier) {
        Divider(
            modifier = modifier,
            color = colorResource(id = R.color.button_disabled),
            thickness = 1.dp
        )
    }
}