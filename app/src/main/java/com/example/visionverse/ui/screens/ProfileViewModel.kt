package com.example.visionverse.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val memberSince: String = ""
)

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> get() = _userProfile

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _toastMessage = MutableStateFlow("")
    val toastMessage: StateFlow<String> get() = _toastMessage

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _isLoading.value = false
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("firstName") ?: ""
                    val lName = document.getString("lastName") ?: ""
                    val mail = document.getString("email") ?: ""
                    val ph = document.getString("phone") ?: ""
                    val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()

                    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val memberDate = sdf.format(Date(createdAt))

                    _userProfile.value = UserProfile(fName, lName, mail, ph, memberDate)
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _toastMessage.value = "Failed to load profile"
                _isLoading.value = false
            }
    }

    // 🔥 UPDATED: Now updates all 4 pieces of information
    fun updateProfile(newFirstName: String, newLastName: String, newEmail: String, newPhone: String) {
        val uid = auth.currentUser?.uid ?: return

        if (newFirstName.isBlank() || newLastName.isBlank() || newEmail.isBlank() || newPhone.isBlank()) {
            _toastMessage.value = "Fields cannot be empty"
            return
        }

        _isLoading.value = true

        db.collection("users").document(uid)
            .update(
                mapOf(
                    "firstName" to newFirstName.trim(),
                    "lastName" to newLastName.trim(),
                    "email" to newEmail.trim(),
                    "phone" to newPhone.trim()
                )
            )
            .addOnSuccessListener {
                _userProfile.value = _userProfile.value?.copy(
                    firstName = newFirstName.trim(),
                    lastName = newLastName.trim(),
                    email = newEmail.trim(),
                    phone = newPhone.trim()
                )
                _toastMessage.value = "Profile updated successfully!"
                _isLoading.value = false
            }
            .addOnFailureListener {
                _toastMessage.value = "Failed to update profile"
                _isLoading.value = false
            }
    }

    fun clearToast() {
        _toastMessage.value = ""
    }

    fun logout(onLogoutComplete: () -> Unit) {
        auth.signOut()
        onLogoutComplete()
    }
}