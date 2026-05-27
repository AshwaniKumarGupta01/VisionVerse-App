package com.example.visionverse.ui.auth

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val firstName = mutableStateOf("")
    val lastName = mutableStateOf("")
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")
    val phone = mutableStateOf("")
    // 🔥 New state for Profile Image
    val selectedImageUri = mutableStateOf<Uri?>(null)

    private val _registerState = MutableStateFlow<UiState>(UiState.Idle)
    val registerState: StateFlow<UiState> get() = _registerState

    private val _loginState = MutableStateFlow<UiState>(UiState.Idle)
    val loginState: StateFlow<UiState> get() = _loginState

    val errorMessage = mutableStateOf("")
    val successMessage = mutableStateOf("")

    fun register() {
        val fName = firstName.value.trim()
        val lName = lastName.value.trim()
        val mail = email.value.trim()
        val ph = phone.value.trim()
        val pass = password.value
        val cPass = confirmPassword.value

// Existing Validation Logic
        if (fName.length < 3) { errorMessage.value = "First name must be at least 3 characters"; return }
        if (lName.length < 3) { errorMessage.value = "Last name must be at least 3 characters"; return }
        if (!mail.contains("@") || !mail.contains(".")) { errorMessage.value = "Enter a valid email"; return }
        if (ph.length < 10) { errorMessage.value = "Enter a valid phone number"; return }
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$")
        if (!passwordRegex.matches(pass)) {
            errorMessage.value = "Password must include uppercase, lowercase, number, special character & be 8+ characters."
            return
        }
        if (pass != cPass) { errorMessage.value = "Passwords do not match"; return }

        errorMessage.value = ""
        _registerState.value = UiState.Loading

// Step 1: Create User
        auth.createUserWithEmailAndPassword(mail, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
// Step 2: Check if image is selected, if so upload to Storage
                if (selectedImageUri.value != null) {
                    uploadProfileImage(uid, fName, lName, mail, ph)
                } else {
                    saveUserToFirestore(uid, fName, lName, mail, ph, "")
                }
            }
            .addOnFailureListener { exc ->
                _registerState.value = UiState.Error(exc.message ?: "Registration failed")
            }
    }

    private fun uploadProfileImage(uid: String, fName: String, lName: String, mail: String, ph: String) {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        selectedImageUri.value?.let { uri ->
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserToFirestore(uid, fName, lName, mail, ph, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    _registerState.value = UiState.Error("Image upload failed")
                }
        }
    }

    private fun saveUserToFirestore(uid: String, fName: String, lName: String, mail: String, ph: String, imageUrl: String) {
        val userData = hashMapOf(
            "uid" to uid,
            "firstName" to fName,
            "lastName" to lName,
            "email" to mail,
            "phone" to ph,
            "profileImageUrl" to imageUrl,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                successMessage.value = "Registration successfully Done"
                _registerState.value = UiState.Success("Registration successfully Done")
            }
            .addOnFailureListener { _registerState.value = UiState.Error(it.message ?: "Firestore error") }
    }

    fun login() {
        val mail = email.value.trim()
        val pass = password.value.trim()
        if (mail.isEmpty() || pass.isEmpty()) { errorMessage.value = "Fields cannot be empty"; return }

        _loginState.value = UiState.Loading
        auth.signInWithEmailAndPassword(mail, pass)
            .addOnSuccessListener { _loginState.value = UiState.Success("Login successful") }
            .addOnFailureListener { _loginState.value = UiState.Error(it.message ?: "Login failed") }
    }

    fun clearAuthStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
        errorMessage.value = ""
        successMessage.value = ""
        email.value = ""
        password.value = ""
        selectedImageUri.value = null
    }
}
