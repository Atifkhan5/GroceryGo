package com.example.grocerygo

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class GroceryUserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val role: String = "user",
    val accountType: String = "Personal",
    val accountStatus: String = "Active"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    var profile by remember {
        mutableStateOf<GroceryUserProfile?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showPasswordDialog by remember {
        mutableStateOf(false)
    }

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    var showSecurityDialog by remember {
        mutableStateOf(false)
    }

    var listenerRegistration by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    fun loadProfile() {
        val user = auth.currentUser

        if (user == null) {
            isLoading = false
            profile = null
            errorMessage = "Please log in to view your profile."
            return
        }

        isLoading = true
        errorMessage = ""

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    profile = GroceryUserProfile(
                        uid = user.uid,
                        fullName = document.getString("fullName")
                            ?: user.displayName
                            ?: "",
                        email = document.getString("email")
                            ?: user.email
                            ?: "",
                        phone = document.getString("phone")
                            ?: "",
                        address = document.getString("address")
                            ?: "",
                        role = document.getString("role")
                            ?: "user",
                        accountType = document.getString("accountType")
                            ?: "Personal",
                        accountStatus = document.getString("accountStatus")
                            ?: "Active"
                    )
                } else {
                    profile = GroceryUserProfile(
                        uid = user.uid,
                        fullName = user.displayName ?: "",
                        email = user.email ?: "",
                        phone = "",
                        role = "user",
                        accountType = "Personal",
                        accountStatus = "Active"
                    )
                }

                isLoading = false
            }
            .addOnFailureListener { exception ->
                isLoading = false
                errorMessage =
                    exception.message ?: "Unable to load profile."
            }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = auth.currentUser

        if (user == null) {
            isLoading = false
            errorMessage = "Please log in to view your profile."
            return@LaunchedEffect
        }

        listenerRegistration = firestore
            .collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, exception ->

                if (exception != null) {
                    isLoading = false
                    errorMessage =
                        exception.message ?: "Unable to load profile."
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    profile = GroceryUserProfile(
                        uid = user.uid,
                        fullName = snapshot.getString("fullName")
                            ?: user.displayName
                            ?: "",
                        email = snapshot.getString("email")
                            ?: user.email
                            ?: "",
                        phone = snapshot.getString("phone")
                            ?: "",
                        address = snapshot.getString("address")
                            ?: "",
                        role = snapshot.getString("role")
                            ?: "user",
                        accountType = snapshot.getString("accountType")
                            ?: "Personal",
                        accountStatus = snapshot.getString("accountStatus")
                            ?: "Active"
                    )
                } else {
                    profile = GroceryUserProfile(
                        uid = user.uid,
                        fullName = user.displayName ?: "",
                        email = user.email ?: "",
                        phone = "",
                        role = "user",
                        accountType = "Personal",
                        accountStatus = "Active"
                    )
                }

                isLoading = false
                errorMessage = ""
            }
    }

    DisposableEffect(currentUser?.uid) {
        onDispose {
            listenerRegistration?.remove()
            listenerRegistration = null
        }
    }

    fun logoutAndOpenMainActivity() {
        listenerRegistration?.remove()
        listenerRegistration = null

        auth.signOut()

        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        context.startActivity(intent)

        onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GroceryDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            loadProfile()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = GroceryDark
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAF8))
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GroceryGreen
                    )
                }
            }

            errorMessage.isNotBlank() && profile == null -> {
                ProfileErrorState(
                    paddingValues = paddingValues,
                    message = errorMessage,
                    onRetry = {
                        loadProfile()
                    }
                )
            }

            else -> {
                ProfileContent(
                    paddingValues = paddingValues,
                    profile = profile
                        ?: GroceryUserProfile(
                            email = currentUser?.email ?: ""
                        ),
                    onEditProfile = {
                        showEditDialog = true
                    },
                    onChangePassword = {
                        showPasswordDialog = true
                    },
                    onOrdersClick = onOrdersClick,
                    onWishlistClick = onWishlistClick,
                    onAccountSecurity = {
                        showSecurityDialog = true
                    },
                    onLogout = {
                        showLogoutDialog = true
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile
                ?: GroceryUserProfile(
                    email = currentUser?.email ?: ""
                ),
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    showEditDialog = false
                }
            },
            onSave = { fullName, phone, address ->

                val user = auth.currentUser

                if (user == null) {
                    errorMessage = "Please log in again."
                    return@EditProfileDialog
                }

                isSaving = true

                val updates = hashMapOf<String, Any>(
                    "fullName" to fullName,
                    "phone" to phone,
                    "address" to address
                )

                firestore
                    .collection("users")
                    .document(user.uid)
                    .update(updates)
                    .addOnSuccessListener {

                        isSaving = false
                        showEditDialog = false

                        Toast.makeText(
                            context,
                            "Profile updated successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { exception ->

                        isSaving = false

                        errorMessage =
                            exception.message
                                ?: "Unable to update profile."
                    }
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    showPasswordDialog = false
                }
            },
            onChangePassword = { currentPassword, newPassword ->

                val user = auth.currentUser

                if (user == null) {
                    errorMessage = "Please log in again."
                    return@ChangePasswordDialog
                }

                val email = user.email

                if (email.isNullOrBlank()) {
                    errorMessage =
                        "Password change is only available for email/password accounts."

                    return@ChangePasswordDialog
                }

                isSaving = true

                val credential =
                    EmailAuthProvider.getCredential(
                        email,
                        currentPassword
                    )

                user
                    .reauthenticate(credential)
                    .addOnSuccessListener {

                        user
                            .updatePassword(newPassword)
                            .addOnSuccessListener {

                                isSaving = false
                                showPasswordDialog = false

                                Toast.makeText(
                                    context,
                                    "Password changed successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->

                                isSaving = false

                                errorMessage =
                                    exception.message
                                        ?: "Unable to change password."
                            }
                    }
                    .addOnFailureListener {

                        isSaving = false
                        errorMessage =
                            "Current password is incorrect."
                    }
            }
        )
    }

    if (showSecurityDialog) {
        AccountSecurityDialog(
            profile = profile
                ?: GroceryUserProfile(
                    email = currentUser?.email ?: ""
                ),
            onDismiss = {
                showSecurityDialog = false
            },
            onChangePassword = {
                showSecurityDialog = false
                showPasswordDialog = true
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showLogoutDialog = false
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = GroceryGreen
                )
            },
            title = {
                Text(
                    text = "Logout?"
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of your GroceryGo account?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        logoutAndOpenMainActivity()
                    }
                ) {
                    Text(
                        text = "Logout",
                        color = Color(0xFFD32F2F)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = GroceryGreen
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    paddingValues: PaddingValues,
    profile: GroceryUserProfile,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOrdersClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAccountSecurity: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF8))
            .padding(paddingValues),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 35.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ProfileHeader(
                profile = profile,
                onEdit = onEditProfile
            )
        }

        item {
            AccountInformationCard(
                profile = profile
            )
        }

        item {
            ProfileSectionTitle(
                title = "Account"
            )
        }

        item {
            ProfileOption(
                icon = Icons.Default.ShoppingBag,
                title = "My Orders",
                subtitle = "View your previous and current orders",
                onClick = onOrdersClick
            )
        }

        item {
            ProfileOption(
                icon = Icons.Default.Favorite,
                title = "My Wishlist",
                subtitle = "View your favorite grocery products",
                onClick = onWishlistClick
            )
        }

        item {
            ProfileOption(
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                subtitle = "Update your name and phone number",
                onClick = onEditProfile
            )
        }

        item {
            ProfileOption(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                onClick = onChangePassword
            )
        }

        item {
            ProfileSectionTitle(
                title = "Security"
            )
        }

        item {
            ProfileOption(
                icon = Icons.Default.Security,
                title = "Account Security",
                subtitle = "View your account authentication and security status",
                onClick = onAccountSecurity
            )
        }

        item {
            ProfileSectionTitle(
                title = "Session"
            )
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Logout",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Text(
                text = "GroceryGo",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = GroceryGray
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: GroceryUserProfile,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = GroceryGreen
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(
                        profile.fullName,
                        profile.email
                    ),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryGreen
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = if (profile.fullName.isNotBlank()) {
                    profile.fullName
                } else {
                    "GroceryGo User"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = profile.email.ifBlank {
                    "No email available"
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileHeaderBadge(
                    text = if (
                        profile.role.equals(
                            "admin",
                            ignoreCase = true
                        )
                    ) {
                        "Admin"
                    } else {
                        "Customer"
                    }
                )

                ProfileHeaderBadge(
                    text = profile.accountStatus
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "Edit Profile"
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderBadge(
    text: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Color.White.copy(alpha = 0.18f)
            )
            .padding(
                horizontal = 11.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun AccountInformationCard(
    profile: GroceryUserProfile
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                text = "Account Information",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            ProfileInfoRow(
                icon = Icons.Default.Email,
                title = "Email",
                value = profile.email.ifBlank {
                    "Not available"
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEAEAEA)
            )

            ProfileInfoRow(
                icon = Icons.Default.Phone,
                title = "Phone",
                value = profile.phone.ifBlank {
                    "Not provided"
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEAEAEA)
            )

            ProfileInfoRow(
                icon = Icons.Default.LocationOn,
                title = "Default Address",
                value = profile.address.ifBlank {
                    "Not provided"
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEAEAEA)
            )

            ProfileInfoRow(
                icon = Icons.Default.AccountCircle,
                title = "Account Type",
                value = profile.accountType
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEAEAEA)
            )

            ProfileInfoRow(
                icon = Icons.Default.CheckCircle,
                title = "Account Status",
                value = profile.accountStatus
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GroceryLightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GroceryGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = GroceryGray
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = value,
                fontSize = 13.sp,
                color = GroceryDark,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileSectionTitle(
    title: String
) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = GroceryGray
    )
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(GroceryLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GroceryGreen,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = GroceryGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: GroceryUserProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var fullName by remember {
        mutableStateOf(profile.fullName)
    }

    var phone by remember {
        mutableStateOf(profile.phone)
    }

    var address by remember {
        mutableStateOf(profile.address)
    }

    var validationError by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Full Name"
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = profile.email,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Email"
                        )
                    },
                    singleLine = true,
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Phone Number"
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Delivery Address"
                        )
                    },
                    minLines = 2,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen
                    )
                )

                if (validationError.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text = validationError,
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val trimmedName = fullName.trim()
                    val trimmedPhone = phone.trim()

                    when {
                        trimmedName.length < 2 -> {
                            validationError =
                                "Please enter your full name."
                        }

                        trimmedPhone.isNotBlank() &&
                                trimmedPhone.length < 7 -> {
                            validationError =
                                "Please enter a valid phone number."
                        }

                        else -> {
                            onSave(
                                trimmedName,
                                trimmedPhone,
                                address.trim()
                            )
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = GroceryGreen
                    )
                } else {
                    Text(
                        text = "Save",
                        color = GroceryGreen
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = GroceryGray
                )
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var currentPassword by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var showCurrentPassword by remember {
        mutableStateOf(false)
    }

    var showNewPassword by remember {
        mutableStateOf(false)
    }

    var showConfirmPassword by remember {
        mutableStateOf(false)
    }

    var validationError by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Password",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                PasswordField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        validationError = ""
                    },
                    label = "Current Password",
                    visible = showCurrentPassword,
                    onVisibilityChange = {
                        showCurrentPassword = !showCurrentPassword
                    }
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                PasswordField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        validationError = ""
                    },
                    label = "New Password",
                    visible = showNewPassword,
                    onVisibilityChange = {
                        showNewPassword = !showNewPassword
                    }
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                PasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        validationError = ""
                    },
                    label = "Confirm New Password",
                    visible = showConfirmPassword,
                    onVisibilityChange = {
                        showConfirmPassword = !showConfirmPassword
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Password must contain at least 6 characters.",
                    fontSize = 10.sp,
                    color = GroceryGray
                )

                if (validationError.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text = validationError,
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    when {
                        currentPassword.isBlank() -> {
                            validationError =
                                "Enter your current password."
                        }

                        newPassword.length < 6 -> {
                            validationError =
                                "New password must contain at least 6 characters."
                        }

                        newPassword != confirmPassword -> {
                            validationError =
                                "New passwords do not match."
                        }

                        currentPassword == newPassword -> {
                            validationError =
                                "New password must be different."
                        }

                        else -> {
                            onChangePassword(
                                currentPassword,
                                newPassword
                            )
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = GroceryGreen
                    )
                } else {
                    Text(
                        text = "Change Password",
                        color = GroceryGreen
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = GroceryGray
                )
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label
            )
        },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onVisibilityChange
            ) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (visible) {
                        "Hide password"
                    } else {
                        "Show password"
                    }
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GroceryGreen
        )
    )
}

@Composable
private fun AccountSecurityDialog(
    profile: GroceryUserProfile,
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val user = auth.currentUser

    val providers = user
        ?.providerData
        ?.filter {
            it.providerId != "firebase"
        }
        ?.map {
            when (it.providerId) {
                EmailAuthProvider.PROVIDER_ID -> "Email & Password"
                "google.com" -> "Google"
                "phone" -> "Phone"
                else -> it.providerId
            }
        }
        ?.distinct()
        ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = GroceryGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Account Security",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecurityStatusRow(
                    title = "Account Status",
                    value = profile.accountStatus
                )

                SecurityStatusRow(
                    title = "Authentication",
                    value = if (providers.isNotEmpty()) {
                        providers.joinToString(", ")
                    } else {
                        "Firebase Authentication"
                    }
                )

                SecurityStatusRow(
                    title = "Email",
                    value = if (user?.isEmailVerified == true) {
                        "Verified"
                    } else {
                        "Not verified"
                    }
                )

                SecurityStatusRow(
                    title = "User ID",
                    value = user?.uid ?: "Unavailable"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onChangePassword
            ) {
                Text(
                    text = "Change Password",
                    color = GroceryGreen
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Close",
                    color = GroceryGray
                )
            }
        }
    )
}

@Composable
private fun SecurityStatusRow(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            color = GroceryGray
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GroceryDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileErrorState(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF8))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(58.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Unable to load profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = message,
                fontSize = 12.sp,
                color = GroceryGray,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Button(
                onClick = onRetry
            ) {
                Text(
                    text = "Try Again"
                )
            }
        }
    }
}

private fun getInitials(
    fullName: String,
    email: String
): String {
    val name = fullName.trim()

    if (name.isNotBlank()) {
        val parts = name.split(
            Regex("\\s+")
        )

        return when {
            parts.size >= 2 -> {
                "${parts[0].first()}${parts[1].first()}"
                    .uppercase()
            }

            else -> {
                parts[0]
                    .take(2)
                    .uppercase()
            }
        }
    }

    return email
        .substringBefore("@")
        .take(2)
        .uppercase()
        .ifBlank {
            "GG"
        }
}