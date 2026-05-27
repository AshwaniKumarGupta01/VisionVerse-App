package com.example.visionverse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionverse.data.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ExamViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Holds the list of questions. The UI will watch this variable.
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    // Holds the loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchQuestionsForSubject(subjectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Navigates to Subjects -> [SubjectName] -> Questions
                val snapshot = db.collection("Subjects")
                    .document(subjectId)
                    .collection("Questions")
                    .get()
                    .await()

                val fetchedQuestions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Question::class.java)
                }

                _questions.value = fetchedQuestions
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error (e.g., show a toast)
            } finally {
                _isLoading.value = false
            }
        }
    }
}