package com.example.grocerygo

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    fun registerUser() {
        errorMessage = ""

        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        when {
            trimmedName.isBlank() -> {
                errorMessage = "Please enter your name."
                return
            }

            trimmedName.length < 2 -> {
                errorMessage = "Please enter a valid name."
                return
            }

            trimmedEmail.isBlank() -> {
                errorMessage = "Please enter your email address."
                return
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(trimmedEmail)
                .matches() -> {
                errorMessage = "Please enter a valid email address."
                return
            }

            password.isBlank() -> {
                errorMessage = "Please enter your password."
                return
            }

            password.length < 6 -> {
                errorMessage =
                    "Password must contain at least 6 characters."
                return
            }

            confirmPassword.isBlank() -> {
                errorMessage = "Please confirm your password."
                return
            }

            password != confirmPassword -> {
                errorMessage = "Passwords do not match."
                return
            }
        }

        isLoading = true

        auth.createUserWithEmailAndPassword(
            trimmedEmail,
            password
        ).addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                isLoading = false

                errorMessage = when {
                    task.exception?.message?.contains(
                        "email address is already in use",
                        ignoreCase = true
                    ) == true -> {
                        "An account already exists with this email."
                    }

                    task.exception?.message?.contains(
                        "already exists",
                        ignoreCase = true
                    ) == true -> {
                        "An account already exists with this email."
                    }

                    task.exception?.message?.contains(
                        "badly formatted",
                        ignoreCase = true
                    ) == true -> {
                        "Please enter a valid email address."
                    }

                    task.exception?.message?.contains(
                        "password is too weak",
                        ignoreCase = true
                    ) == true -> {
                        "Password is too weak. Use at least 6 characters."
                    }

                    task.exception?.message?.contains(
                        "network",
                        ignoreCase = true
                    ) == true -> {
                        "Network error. Please check your internet connection."
                    }

                    else -> {
                        task.exception?.message
                            ?: "Registration failed. Please try again."
                    }
                }

                return@addOnCompleteListener
            }

            val user = auth.currentUser

            if (user == null) {
                isLoading = false
                errorMessage =
                    "Account was created, but user information could not be loaded."
                return@addOnCompleteListener
            }

            val userData = hashMapOf(
                "uid" to user.uid,
                "fullName" to trimmedName,
                "name" to trimmedName,
                "email" to trimmedEmail,
                "phone" to "",
                "address" to "",
                "role" to "customer",
                "accountType" to "Personal",
                "accountStatus" to "Active",
                "createdAt" to System.currentTimeMillis()
            )

            firestore
                .collection("users")
                .document(user.uid)
                .set(userData)
                .addOnSuccessListener {

                    isLoading = false

                    onRegisterSuccess()
                }
                .addOnFailureListener {

                    isLoading = false

                    errorMessage =
                        "Account was created, but profile data could not be saved."

                    user.delete()
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        color = GroceryLightGreen,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.app_logo
                    ),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Join GroceryGo today for fresh groceries",
                fontSize = 14.sp,
                color = GroceryGray,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Full Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter your name")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name"
                        )
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Email Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter your email")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email"
                        )
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Password",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Create a password")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password"
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Confirm Password",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Confirm your password")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm Password"
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    registerUser()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroceryGreen,
                    disabledContainerColor = GroceryGreen.copy(
                        alpha = 0.6f
                    )
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Register",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    color = GroceryGray,
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = onLoginClick,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Login",
                        color = GroceryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Fresh groceries delivered to your door",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}