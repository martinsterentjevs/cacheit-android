package com.martinsterentjevs.cacheit.ui.error

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItRadius
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.TypeBody
import com.martinsterentjevs.cacheit.ui.theme.TypeLabel

@Preview(showBackground = true)
@Composable
fun ErrorScreen(title:String = "Error",
                message:String = "An unknown error has occurred"){
    Column(
       modifier = Modifier.safeDrawingPadding()
           .padding(CacheItSpacing.lg)
           .fillMaxSize()
    ) {
        ShowError(title,message)
    }
}
@Composable
fun ShowError(title:String,message: String){
    Text(
        title,
        style = TypeLabel
    )
    Text(message,
        style = TypeBody
    )
}
