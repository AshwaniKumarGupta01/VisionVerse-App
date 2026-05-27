package com.example.visionverse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionverse.data.ExamResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResultViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _pastResults = MutableStateFlow<List<ExamResult>>(emptyList())
    val pastResults: StateFlow<List<ExamResult>> = _pastResults

    private val _topScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val topScores: StateFlow<Map<String, Int>> = _topScores

    fun saveAndFetchResults(subjectId: String, score: Int, total: Int) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // 1. Save the new result (ONLY if it's a real exam, not just viewing history)
            if (subjectId != "history" && subjectId.isNotEmpty()) {
                val newResult = ExamResult(subjectId, score, total, System.currentTimeMillis())
                try {
                    db.collection("users").document(userId)
                        .collection("results").add(newResult).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Fetch all past results from newest to oldest
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("results")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()

                val resultsList = snapshot.toObjects(ExamResult::class.java)
                _pastResults.value = resultsList

                // 3. Calculate Top Scores for the big cards
                val highestScores = mutableMapOf<String, Int>()
                for (result in resultsList) {
                    val currentTop = highestScores[result.subjectId] ?: 0
                    if (result.score > currentTop) {
                        highestScores[result.subjectId] = result.score
                    }
                }
                _topScores.value = highestScores

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}