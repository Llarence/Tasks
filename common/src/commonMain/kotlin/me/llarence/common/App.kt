package me.llarence.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.WindowPosition.PlatformDefault.x
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.awt.SystemColor.text
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Task(val day: Int, val hour: Float, val duration: Float)

fun initDatabase(): Firestore {
    val serviceAccount = credURL().openStream()

    val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).setDatabaseUrl("https://tasks-1d295-default-rtdb.firebaseio.com").build()

    FirebaseApp.initializeApp(options)

    return FirestoreClient.getFirestore()
}

val database = initDatabase()


// TODO: Move some of the repeated math into functions
@OptIn(ExperimentalTextApi::class)
@Composable
fun app() {
    val textMeasurer = rememberTextMeasurer()

    var scroll by mutableStateOf(Float.POSITIVE_INFINITY)

    val tasks = remember { mutableStateListOf(Task(4, 3.2f, 1.2f)) }

    var textBuffer by mutableStateOf(0f)
    var daySize by mutableStateOf(0f)

    var grabbedTaskIndex by mutableStateOf<Int?>(null)
    var grabbedOffset by mutableStateOf(0f)

        Canvas(Modifier
        .fillMaxSize()
        .padding(10.dp)
        .clipToBounds()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                scroll += dragAmount.y
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { startPos ->
                    grabbedTaskIndex = null

                    for (i in (tasks.size - 1) downTo 0) {
                        val task = tasks[i]
                        val x = textBuffer + ((daySize + DAY_PADDING) * task.day)
                        val relaX = x - startPos.x
                        if (0 >= relaX && relaX + daySize > 0) {
                            val y = (HOUR_SIZE * task.hour).dp.toPx() + scroll
                            val relaY = y - startPos.y
                            if (0 >= relaY && relaY + (task.duration * HOUR_SIZE).dp.toPx() > 0) {
                                grabbedTaskIndex = i
                                grabbedOffset = relaY
                                break
                            }
                        }
                    }
                },

                onDragEnd = {
                    grabbedTaskIndex = null
                },

                onDrag = { change, dragAmount ->
                    change.consume()

                    if (grabbedTaskIndex == null) {
                        scroll += dragAmount.y
                    } else {
                        val task = tasks[grabbedTaskIndex!!]

                        val day = min(max(((-textBuffer + change.position.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS)
                        val hour = min(max((grabbedOffset + change.position.y - scroll) / HOUR_SIZE, 0f), 24f - task.duration)

                        tasks[grabbedTaskIndex!!] = Task(day, hour, task.duration)
                    }
                }
            )
        }
    ) {
        drawRect(Color.Cyan)

        var textWidth = 0
        for (i in 0 until HOURS) {
            val text = "$i.00"

            if (i == 0) {
                // Maybe having scroll changed here doesn't make a lot of sense
                val textMeasure = textMeasurer.measure(AnnotatedString(text))
                scroll = min(max(scroll, -(HOURS * HOUR_SIZE).dp.toPx() + size.height), textMeasure.size.height / 2f)
            }

            val textMeasure = textMeasurer.measure(AnnotatedString(text))
            val y = (HOUR_SIZE * i).dp.toPx() - textMeasure.size.height / 2f + scroll
            if (size.height > y) {
                drawText(textMeasurer, text, Offset(0f, y))
            }

            if (textMeasure.size.width > textWidth) {
                textWidth = textMeasure.size.width
            }
        }

        textBuffer = textWidth + TEXT_PADDING
        daySize = max((size.width + DAY_PADDING - textBuffer) / DAYS - DAY_PADDING, 0f)

        for (i in 0 until DAYS) {
            val startX = textBuffer + ((daySize + DAY_PADDING) * i)
            drawRect(Color.LightGray, Offset(startX.dp.toPx(), scroll), Size(daySize.dp.toPx(), (HOURS * HOUR_SIZE).dp.toPx()))
        }

        for (i in 0 until HOURS) {
            val y = (HOUR_SIZE * i).dp.toPx() + scroll
            drawLine(Color.Gray, Offset(textBuffer, y), Offset(size.width, y))
        }

        for (i in tasks.indices) {
            val task = tasks[i]
            if (i == grabbedTaskIndex) {
                drawRoundRect(Color.Red, Offset(textBuffer + ((daySize + DAY_PADDING) * task.day) - GRAB_EXPAND, (HOUR_SIZE * task.hour).dp.toPx() + scroll - GRAB_EXPAND), Size(daySize + (GRAB_EXPAND * 2), (task.duration * HOUR_SIZE).dp.toPx() + (GRAB_EXPAND * 2)), CornerRadius(CORNER_RADIUS.dp.toPx()))
            } else {
                drawRoundRect(Color.Red, Offset(textBuffer + ((daySize + DAY_PADDING) * task.day), (HOUR_SIZE * task.hour).dp.toPx() + scroll), Size(daySize, (task.duration * HOUR_SIZE).dp.toPx()), CornerRadius(CORNER_RADIUS.dp.toPx()))
            }
        }
    }
}
