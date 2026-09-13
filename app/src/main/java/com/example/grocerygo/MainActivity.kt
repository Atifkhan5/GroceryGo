package com.example.grocerygo

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.grocerygo.ui.theme.GroceryGoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

enum class Screen {
    LOGIN,
    REGISTER
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GroceryGoTheme {

                val context = LocalContext.current
                val auth = remember {
                    FirebaseAuth.getInstance()
                }

                var currentScreen by remember {
                    mutableStateOf(
                        if (auth.currentUser != null) {
                            Screen.LOGIN
                        } else {
                            Screen.LOGIN
                        }
                    )
                }

                var isLoggedIn by remember {
                    mutableStateOf(false)
                }

                var isAdmin by remember {
                    mutableStateOf(false)
                }

                var showForgotPasswordDialog by remember {
                    mutableStateOf(false)
                }

                if (isLoggedIn) {

                    LaunchedEffect(Unit) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            val firestore = FirebaseFirestore.getInstance()
                            firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        isAdmin = document.getString("role") == "admin"
                                    }
                                }
                        }
                    }

                    GroceryNavigation(
                        isAdmin = isAdmin,
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                            currentScreen = Screen.LOGIN
                        }
                    )

                } else {

                    if (showForgotPasswordDialog) {

                        ForgotPasswordDialog(
                            onDismiss = {
                                showForgotPasswordDialog = false
                            },
                            onSubmit = { email ->

                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->

                                        if (task.isSuccessful) {

                                            Toast.makeText(
                                                context,
                                                "Password reset email sent!",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } else {

                                            Toast.makeText(
                                                context,
                                                task.exception?.message
                                                    ?: "Unable to send reset email.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                showForgotPasswordDialog = false
                            }
                        )
                    }

                    when (currentScreen) {

                        Screen.LOGIN -> {

                            LoginScreen(
                                onLoginSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Login Successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    isLoggedIn = true
                                },

                                onRegisterClick = {
                                    currentScreen = Screen.REGISTER
                                },

                                onForgotPasswordClick = {
                                    showForgotPasswordDialog = true
                                }
                            )
                        }

                        Screen.REGISTER -> {

                            RegisterScreen(
                                onRegisterSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Registration Successful! Please login.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    currentScreen = Screen.LOGIN
                                },

                                onLoginClick = {
                                    currentScreen = Screen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
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

    fun loginUser() {

        errorMessage = ""

        if (email.isBlank()) {

            errorMessage = "Please enter your email address."
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {

            errorMessage = "Please enter a valid email address."
            return
        }

        if (password.isBlank()) {

            errorMessage = "Please enter your password."
            return
        }

        if (password.length < 6) {

            errorMessage = "Password must contain at least 6 characters."
            return
        }

        isLoading = true

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->

            if (!task.isSuccessful) {

                isLoading = false

                val message = task.exception?.message.orEmpty()

                errorMessage = when {

                    message.contains(
                        "no user record",
                        ignoreCase = true
                    ) -> {
                        "No account found with this email."
                    }

                    message.contains(
                        "password is invalid",
                        ignoreCase = true
                    ) -> {
                        "Incorrect password."
                    }

                    message.contains(
                        "invalid credential",
                        ignoreCase = true
                    ) -> {
                        "Incorrect email or password."
                    }

                    message.contains(
                        "badly formatted",
                        ignoreCase = true
                    ) -> {
                        "Please enter a valid email address."
                    }

                    message.contains(
                        "user-disabled",
                        ignoreCase = true
                    ) -> {
                        "This account has been disabled."
                    }

                    else -> {
                        "Login failed. Please check your email and password."
                    }
                }

                return@addOnCompleteListener
            }

            val user = auth.currentUser

            if (user == null) {

                isLoading = false
                errorMessage = "Login failed. Please try again."

                return@addOnCompleteListener
            }

            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->

                    if (!document.exists()) {

                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "email" to (
                                    user.email
                                        ?: email.trim()
                                    ),
                            "name" to "",
                            "phone" to "",
                            "address" to "",
                            "role" to "customer"
                        )

                        firestore.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {

                                isLoading = false
                                onLoginSuccess()
                            }
                            .addOnFailureListener {

                                isLoading = false

                                errorMessage =
                                    "Login successful, but account data could not be created."
                            }

                    } else {

                        isLoading = false
                        onLoginSuccess()
                    }
                }
                .addOnFailureListener {

                    isLoading = false

                    errorMessage =
                        "Unable to load your account data. Please try again."
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
                modifier = Modifier.height(55.dp)
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
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "Welcome Back!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Login to continue shopping for fresh groceries",
                fontSize = 15.sp,
                color = GroceryGray,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Email Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
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

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Password",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = password,

                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },

                    modifier = Modifier.fillMaxWidth(),

                    placeholder = {
                        Text("Enter your password")
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
                            }
                        ) {

                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },

                                contentDescription =
                                    if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    }
                            )
                        }
                    },

                    visualTransformation =
                        if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },

                    singleLine = true,

                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLeadingIconColor = GroceryGreen,
                        unfocusedLeadingIconColor = GroceryGray
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(
                        onClick = onForgotPasswordClick
                    ) {

                        Text(
                            text = "Forgot Password?",
                            color = GroceryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (errorMessage.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Button(
                onClick = {
                    loginUser()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                enabled = !isLoading,

                shape = RoundedCornerShape(14.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = GroceryGreen,
                    disabledContainerColor =
                        GroceryGreen.copy(alpha = 0.6f)
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(23.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )

                } else {

                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Don't have an account?",
                    color = GroceryGray,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                TextButton(
                    onClick = onRegisterClick
                ) {

                    Text(
                        text = "Register",
                        color = GroceryGreen,
                        fontSize = 14.sp,
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
                modifier = Modifier.height(25.dp)
            )
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Reset Password",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                Text(
                    text = "Enter your email address to receive a password reset link.",
                    fontSize = 14.sp,
                    color = GroceryGray
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                OutlinedTextField(
                    value = email,

                    onValueChange = {
                        email = it
                        error = ""
                    },

                    placeholder = {
                        Text("Email Address")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp),

                    singleLine = true,

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen
                    )
                )

                if (error.isNotEmpty()) {

                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        Patterns.EMAIL_ADDRESS
                            .matcher(email.trim())
                            .matches()
                    ) {

                        onSubmit(email.trim())

                    } else {

                        error =
                            "Please enter a valid email address."
                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = GroceryGreen
                )
            ) {

                Text("Send Link")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Cancel",
                    color = GroceryGray
                )
            }
        },

        containerColor = Color.White,

        shape = RoundedCornerShape(20.dp)
    )
}