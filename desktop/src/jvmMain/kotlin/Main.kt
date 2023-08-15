import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import me.llarence.common.app

fun main() = application {
    Window(::exitApplication, WindowState(width = width, height = height), title = title) {
        app()
    }
}
