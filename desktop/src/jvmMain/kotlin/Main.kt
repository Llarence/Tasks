import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.llarence.common.app

fun main() = application {
    Window(::exitApplication, title = "Tasks") {
        app()
    }
}
