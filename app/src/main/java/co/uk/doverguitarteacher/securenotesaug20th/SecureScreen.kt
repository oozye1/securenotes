package co.uk.doverguitarteacher.securenotesaug20th

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun SecureScreen(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    // DisposableEffect runs when the composable enters the screen
    // and cleans up when it leaves.
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window

        // Set the SECURE flag when the screen is shown
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // onDispose is called when the screen is left
        onDispose {
            // Clear the SECURE flag when leaving the screen
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Display the actual content of the screen
    content()
}
