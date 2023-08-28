import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import me.llarence.common.app
import me.llarence.common.height
import me.llarence.common.title
import me.llarence.common.width

fun main() = application {
    Window(::exitApplication, WindowState(width = width, height = height), title = title) {
        app()
    }
}
