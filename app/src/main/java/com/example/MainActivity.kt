package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.data.PintarHitungRepository
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.PintarHitungTheme
import com.example.viewmodel.PintarHitungViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PintarHitungTheme {
                PintarHitungAppRoot()
            }
        }
    }
}

@Composable
private fun PintarHitungAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val appContext = LocalContext.current.applicationContext
        val firestore by produceState<FirebaseFirestore?>(initialValue = null, key1 = appContext) {
            value = withContext(Dispatchers.IO) {
                val databaseId = appContext.getString(R.string.firestore_database_id)
                FirebaseFirestore.getInstance(databaseId)
            }
        }

        val readyDb = firestore
        if (readyDb == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val uid = user.uid
            val displayName = user.displayName ?: "Adik Pintar"
            key(uid) {
                val viewModel: PintarHitungViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            PintarHitungViewModel(
                                repository = PintarHitungRepository(readyDb),
                                currentUserId = uid,
                                defaultDisplayName = displayName
                            )
                        }
                    }
                )
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}
