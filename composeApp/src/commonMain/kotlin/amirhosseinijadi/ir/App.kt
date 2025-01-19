package amirhosseinijadi.ir


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    var detectedText by remember { mutableStateOf<String?>("") }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                onTextGenerated = {
                    detectedText = it
                })

            Text(
                modifier = Modifier
                    .heightIn(min = 100.dp)
                    .align(Alignment.BottomCenter),
                text = "Detected Text = $detectedText"
            )
        }
    }
}