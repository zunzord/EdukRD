package com.edukrd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.edukrd.app.navigation.NavGraph
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        var startDestination = "login"

        if (currentUser != null && currentUser.isEmailVerified) {
            val userId = currentUser.uid
            lifecycleScope.launch {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        startDestination = if (document.exists()) "home" else "error_screen"
                        setContent {
                            val navController = rememberNavController()
                            NavGraph(navController = navController, startDestination = startDestination)
                        }
                    }
                    .addOnFailureListener {
                        startDestination = "error_screen"
                        setContent {
                            val navController = rememberNavController()
                            NavGraph(navController = navController, startDestination = startDestination)
                        }
                    }
            }
        } else {
            setContent {
                val navController = rememberNavController()
                NavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}
