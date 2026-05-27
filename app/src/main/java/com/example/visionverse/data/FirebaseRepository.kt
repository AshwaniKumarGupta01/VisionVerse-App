package com.example.visionverse.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 🔐 SIGN UP
    fun registerUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }

    // 🔐 LOGIN
    fun loginUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }

    // 🚪 LOGOUT
    fun logout() {
        auth.signOut()
    }

    // 📊 ADD QUESTION
    fun addQuestion(question: String) {
        val data = hashMapOf("question" to question)
        db.collection("questions").add(data)
    }

    // 📥 GET QUESTIONS
    fun getQuestions(onResult: (List<String>) -> Unit) {
        db.collection("questions").get()
            .addOnSuccessListener { result ->
                val list = result.map { it.getString("question") ?: "" }
                onResult(list)
            }
    }
}