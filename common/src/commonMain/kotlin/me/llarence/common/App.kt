package me.llarence.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient

fun initDatabase(): Firestore {
    val serviceAccount = credURL().openStream()

    val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).setDatabaseUrl("https://tasks-1d295-default-rtdb.firebaseio.com").build()

    FirebaseApp.initializeApp(options)

    return FirestoreClient.getFirestore()
}

val database = initDatabase()
val texts = mutableListOf("Hello, World!1", "Hello, World!2", "Hello, World!3")

@Composable
fun app() {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Button(onClick = {
                database.collection("tasks").add("A" to "b")
            }) {
                Text("Click Me!")
            }
        }

        items(texts) {
            Text(it)
        }
    }
}
