package amirhosseinijadi.ir

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraScreen(
    modifier: Modifier,
    onTextGenerated: (String?) -> Unit
)