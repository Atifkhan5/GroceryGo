package com.example.grocerygo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

data class GroceryProduct(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val discount: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val featured: Boolean = false,
    val bestSeller: Boolean = false,
    val dailyOffer: Boolean = false,
    val createdAt: Long = 0L
) {
    val discountedPrice: Double
        get() = price - (price * discount / 100.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var isCheckingAdmin by remember {
        mutableStateOf(true)
    }

    var isAdmin by remember {
        mutableStateOf(false)
    }

    var products by remember {
        mutableStateOf<List<GroceryProduct>>(emptyList())
    }

    var isLoadingProducts by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var showAddEditDialog by remember {
        mutableStateOf(false)
    }

    var selectedProduct by remember {
        mutableStateOf<GroceryProduct?>(null)
    }

    var productToDelete by remember {
        mutableStateOf<GroceryProduct?>(null)
    }

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    var listenerRegistration by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    LaunchedEffect(Unit) {

        val user = auth.currentUser

        if (user == null) {
            isCheckingAdmin = false
            isAdmin = false
            return@LaunchedEffect
        }

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                val role = document.getString("role")

                isAdmin = role == "admin"
                isCheckingAdmin = false

                if (role == "admin") {

                    listenerRegistration = firestore
                        .collection("products")
                        .addSnapshotListener { snapshot, exception ->

                            if (exception != null) {
                                errorMessage =
                                    exception.message
                                        ?: "Failed to load products."
                                isLoadingProducts = false
                                return@addSnapshotListener
                            }

                            products = snapshot?.documents?.mapNotNull { document ->

                                try {

                                    GroceryProduct(
                                        id = document.id,
                                        name = document.getString("name") ?: "",
                                        category = document.getString("category") ?: "",
                                        price = document.getDouble("price") ?: 0.0,
                                        discount = document.getDouble("discount") ?: 0.0,
                                        description = document.getString("description") ?: "",
                                        imageUrl = document.getString("imageUrl") ?: "",
                                        stock = document.getLong("stock")?.toInt() ?: 0,
                                        featured = document.getBoolean("featured") ?: false,
                                        bestSeller = document.getBoolean("bestSeller") ?: false,
                                        dailyOffer = document.getBoolean("dailyOffer") ?: false,
                                        createdAt = document.getLong("createdAt") ?: 0L
                                    )

                                } catch (e: Exception) {
                                    null
                                }

                            }?.sortedByDescending {
                                it.createdAt
                            } ?: emptyList()

                            isLoadingProducts = false
                        }
                } else {
                    isLoadingProducts = false
                }
            }
            .addOnFailureListener { exception ->
                isCheckingAdmin = false
                isAdmin = false
                isLoadingProducts = false
                errorMessage =
                    exception.message
                        ?: "Unable to verify admin account."
            }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    val filteredProducts = products.filter { product ->

        val query = searchQuery.trim()

        query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)
    }

    if (isCheckingAdmin) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                color = GroceryGreen
            )
        }

        return
    }

    if (!isAdmin) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(60.dp)
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Access Denied",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryDark
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "You do not have administrator permissions.",
                    fontSize = 14.sp,
                    color = GroceryGray
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GroceryGreen
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Go Back")
                }
            }
        }

        return
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Admin Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = GroceryDark
                        )

                        Text(
                            text = "Grocery Management",
                            fontSize = 12.sp,
                            color = GroceryGray
                        )
                    }
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

                    var menuExpanded by remember {
                        mutableStateOf(false)
                    }

                    Box {

                        IconButton(
                            onClick = {
                                menuExpanded = true
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = GroceryDark
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Logout")
                                },
                                onClick = {
                                    menuExpanded = false
                                    showLogoutDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },

        floatingActionButton = {

            androidx.compose.material3.FloatingActionButton(
                onClick = {

                    selectedProduct = null
                    showAddEditDialog = true
                },
                containerColor = GroceryGreen,
                contentColor = Color.White
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Grocery"
                )
            }
        }

    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAF8))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    AdminStatCard(
                        title = "Products",
                        value = products.size.toString(),
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f)
                    )

                    AdminStatCard(
                        title = "Featured",
                        value = products.count {
                            it.featured
                        }.toString(),
                        icon = Icons.Default.Storefront,
                        modifier = Modifier.weight(1f)
                    )

                    AdminStatCard(
                        title = "Offers",
                        value = products.count {
                            it.dailyOffer
                        }.toString(),
                        icon = Icons.Default.LocalOffer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search groceries")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {

                        if (searchQuery.isNotBlank()) {

                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GroceryGreen,
                        unfocusedBorderColor = Color(0xFFD6D6D6)
                    )
                )
            }

            item {

                Text(
                    text = if (searchQuery.isBlank()) {
                        "All Groceries"
                    } else {
                        "Search Results"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryDark
                )
            }

            if (isLoadingProducts) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = GroceryGreen
                        )
                    }
                }

            } else if (filteredProducts.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = GroceryGreen,
                                modifier = Modifier.size(50.dp)
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "No groceries added yet"
                                } else {
                                    "No groceries found"
                                },
                                fontWeight = FontWeight.Bold,
                                color = GroceryDark
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Tap + to add your first grocery."
                                } else {
                                    "Try another product name or category."
                                },
                                fontSize = 13.sp,
                                color = GroceryGray
                            )
                        }
                    }
                }

            } else {

                items(
                    items = filteredProducts,
                    key = {
                        it.id
                    }
                ) { product ->

                    AdminProductCard(
                        product = product,
                        onEdit = {
                            selectedProduct = product
                            showAddEditDialog = true
                        },
                        onDelete = {
                            productToDelete = product
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(80.dp)
                )
            }
        }
    }

    if (showAddEditDialog) {

        AddEditGroceryDialog(
            product = selectedProduct,
            onDismiss = {
                showAddEditDialog = false
                selectedProduct = null
            },
            onSaved = {
                showAddEditDialog = false
                selectedProduct = null
            }
        )
    }

    productToDelete?.let { product ->

        AlertDialog(
            onDismissRequest = {
                productToDelete = null
            },
            title = {
                Text(
                    text = "Delete Grocery",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${product.name}\"? This action cannot be undone."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        firestore.collection("products")
                            .document(product.id)
                            .delete()
                            .addOnSuccessListener {
                                productToDelete = null
                            }
                            .addOnFailureListener { exception ->
                                errorMessage =
                                    exception.message
                                        ?: "Failed to delete product."
                                productToDelete = null
                            }
                    }
                ) {

                    Text(
                        text = "Delete",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        productToDelete = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {

        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to logout?")
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        auth.signOut()
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {

                    Text(
                        text = "Logout",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    if (errorMessage.isNotBlank()) {

        AlertDialog(
            onDismissRequest = {
                errorMessage = ""
            },
            title = {
                Text("Error")
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        errorMessage = ""
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GroceryGreen,
                modifier = Modifier.size(24.dp)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Text(
                text = title,
                fontSize = 11.sp,
                color = GroceryGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminProductCard(
    product: GroceryProduct,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            color = GroceryLightGreen,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = GroceryGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = product.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = product.category,
                        fontSize = 12.sp,
                        color = GroceryGray
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = String.format(
                                Locale.US,
                                "PKR %.0f",
                                product.discountedPrice
                            ),
                            fontWeight = FontWeight.Bold,
                            color = GroceryGreen
                        )

                        if (product.discount > 0) {

                            Spacer(
                                modifier = Modifier.width(6.dp)
                            )

                            Text(
                                text = "${product.discount.toInt()}% OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    IconButton(
                        onClick = onEdit
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = GroceryGreen
                        )
                    }

                    IconButton(
                        onClick = onDelete
                    ) {

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                ProductTag(
                    text = "Stock: ${product.stock}",
                    enabled = true
                )

                if (product.featured) {

                    ProductTag(
                        text = "Featured",
                        enabled = true
                    )
                }

                if (product.bestSeller) {

                    ProductTag(
                        text = "Best Seller",
                        enabled = true
                    )
                }

                if (product.dailyOffer) {

                    ProductTag(
                        text = "Offer",
                        enabled = true
                    )
                }
            }

            if (product.stock <= 5) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(5.dp)
                    )

                    Text(
                        text = if (product.stock == 0) {
                            "Out of stock"
                        } else {
                            "Low stock"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductTag(
    text: String,
    enabled: Boolean
) {

    Box(
        modifier = Modifier
            .background(
                color = if (enabled) {
                    GroceryLightGreen
                } else {
                    Color(0xFFF1F1F1)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) {
                GroceryGreen
            } else {
                GroceryGray
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGroceryDialog(
    product: GroceryProduct?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val isEditing = product != null

    var name by remember {
        mutableStateOf(product?.name ?: "")
    }

    var category by remember {
        mutableStateOf(product?.category ?: "")
    }

    var price by remember {
        mutableStateOf(
            if (product != null) {
                if (product.price == 0.0) {
                    ""
                } else {
                    product.price.toString()
                }
            } else {
                ""
            }
        )
    }

    var discount by remember {
        mutableStateOf(
            if (product != null) {
                if (product.discount == 0.0) {
                    ""
                } else {
                    product.discount.toString()
                }
            } else {
                ""
            }
        )
    }

    var stock by remember {
        mutableStateOf(
            if (product != null) {
                product.stock.toString()
            } else {
                ""
            }
        )
    }

    var description by remember {
        mutableStateOf(product?.description ?: "")
    }

    var imageUrl by remember {
        mutableStateOf(product?.imageUrl ?: "")
    }

    var featured by remember {
        mutableStateOf(product?.featured ?: false)
    }

    var bestSeller by remember {
        mutableStateOf(product?.bestSeller ?: false)
    }

    var dailyOffer by remember {
        mutableStateOf(product?.dailyOffer ?: false)
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val categories = listOf(
        "Fruits & Vegetables",
        "Dairy Products",
        "Snacks",
        "Beverages",
        "Bakery",
        "Household Items",
        "Personal Care",
        "Meat & Chicken",
        "Frozen Foods",
        "Other"
    )

    val priceValue = price.toDoubleOrNull() ?: 0.0
    val discountValue = discount.toDoubleOrNull() ?: 0.0

    val calculatedPrice =
        priceValue - (priceValue * discountValue / 100.0)

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {

            Text(
                text = if (isEditing) {
                    "Edit Grocery"
                } else {
                    "Add Grocery"
                },
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )
        },
        text = {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {

                    Text(
                        text = if (isEditing) {
                            "Update the grocery information below."
                        } else {
                            "Enter the grocery information below."
                        },
                        fontSize = 13.sp,
                        color = GroceryGray
                    )
                }

                item {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Product Name")
                        },
                        placeholder = {
                            Text("e.g. Fresh Red Apples")
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GroceryGreen,
                            unfocusedBorderColor = Color(0xFFD6D6D6)
                        )
                    )
                }

                item {

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = {
                            if (!isSaving) {
                                categoryExpanded = !categoryExpanded
                            }
                        }
                    ) {

                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = {
                                Text("Category")
                            },
                            placeholder = {
                                Text("Select category")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoryExpanded
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null
                                )
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GroceryGreen,
                                unfocusedBorderColor = Color(0xFFD6D6D6)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = {
                                categoryExpanded = false
                            }
                        ) {

                            categories.forEach { item ->

                                DropdownMenuItem(
                                    text = {
                                        Text(item)
                                    },
                                    onClick = {
                                        category = item
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedTextField(
                            value = price,
                            onValueChange = {
                                if (
                                    it.isEmpty() ||
                                    it.matches(Regex("^\\d*(\\.\\d*)?$"))
                                ) {
                                    price = it
                                    errorMessage = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text("Price")
                            },
                            placeholder = {
                                Text("450")
                            },
                            singleLine = true,
                            enabled = !isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            leadingIcon = {
                                Text(
                                    text = "PKR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GroceryGreen,
                                unfocusedBorderColor = Color(0xFFD6D6D6)
                            )
                        )

                        OutlinedTextField(
                            value = discount,
                            onValueChange = {
                                if (
                                    it.isEmpty() ||
                                    it.matches(Regex("^\\d*(\\.\\d*)?$"))
                                ) {
                                    discount = it
                                    errorMessage = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text("Discount %")
                            },
                            placeholder = {
                                Text("10")
                            },
                            singleLine = true,
                            enabled = !isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GroceryGreen,
                                unfocusedBorderColor = Color(0xFFD6D6D6)
                            )
                        )
                    }
                }

                item {

                    OutlinedTextField(
                        value = stock,
                        onValueChange = {
                            if (
                                it.isEmpty() ||
                                it.all { character ->
                                    character.isDigit()
                                }
                            ) {
                                stock = it
                                errorMessage = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Stock Quantity")
                        },
                        placeholder = {
                            Text("50")
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GroceryGreen,
                            unfocusedBorderColor = Color(0xFFD6D6D6)
                        )
                    )
                }

                item {

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = {
                            imageUrl = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Image URL")
                        },
                        placeholder = {
                            Text("https://example.com/apple.jpg")
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GroceryGreen,
                            unfocusedBorderColor = Color(0xFFD6D6D6)
                        )
                    )
                }

                item {

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            errorMessage = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = {
                            Text("Description")
                        },
                        placeholder = {
                            Text("Describe the grocery product...")
                        },
                        enabled = !isSaving,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GroceryGreen,
                            unfocusedBorderColor = Color(0xFFD6D6D6)
                        )
                    )
                }

                item {

                    Text(
                        text = "Product Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark
                    )
                }

                item {

                    AdminSwitchRow(
                        title = "Featured Product",
                        description = "Show this product in featured items.",
                        checked = featured,
                        enabled = !isSaving,
                        onCheckedChange = {
                            featured = it
                        }
                    )
                }

                item {

                    AdminSwitchRow(
                        title = "Best Seller",
                        description = "Show this product among best sellers.",
                        checked = bestSeller,
                        enabled = !isSaving,
                        onCheckedChange = {
                            bestSeller = it
                        }
                    )
                }

                item {

                    AdminSwitchRow(
                        title = "Daily Offer",
                        description = "Show this product in daily offers.",
                        checked = dailyOffer,
                        enabled = !isSaving,
                        onCheckedChange = {
                            dailyOffer = it
                        }
                    )
                }

                if (priceValue > 0 && discountValue > 0) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = GroceryLightGreen
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Text(
                                    text = "Price Preview",
                                    fontWeight = FontWeight.Bold,
                                    color = GroceryDark
                                )

                                Spacer(
                                    modifier = Modifier.height(5.dp)
                                )

                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Original: PKR %.0f",
                                        priceValue
                                    ),
                                    fontSize = 13.sp,
                                    color = GroceryGray
                                )

                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Discounted: PKR %.0f",
                                        calculatedPrice
                                    ),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GroceryGreen
                                )
                            }
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {

                    val cleanName = name.trim()
                    val cleanCategory = category.trim()
                    val cleanDescription = description.trim()
                    val cleanImageUrl = imageUrl.trim()

                    val priceNumber = price.toDoubleOrNull()
                    val discountNumber =
                        if (discount.isBlank()) {
                            0.0
                        } else {
                            discount.toDoubleOrNull()
                        }

                    val stockNumber = stock.toIntOrNull()

                    when {

                        cleanName.isBlank() -> {
                            errorMessage = "Please enter a product name."
                        }

                        cleanCategory.isBlank() -> {
                            errorMessage = "Please select a category."
                        }

                        priceNumber == null || priceNumber <= 0 -> {
                            errorMessage = "Please enter a valid price."
                        }

                        discountNumber == null ||
                                discountNumber < 0 ||
                                discountNumber > 100 -> {
                            errorMessage =
                                "Discount must be between 0 and 100."
                        }

                        stockNumber == null || stockNumber < 0 -> {
                            errorMessage =
                                "Please enter a valid stock quantity."
                        }

                        cleanDescription.isBlank() -> {
                            errorMessage =
                                "Please enter a product description."
                        }

                        else -> {

                            isSaving = true
                            errorMessage = ""

                            val currentTime =
                                System.currentTimeMillis()

                            val productData =
                                hashMapOf<String, Any>(
                                    "name" to cleanName,
                                    "category" to cleanCategory,
                                    "price" to priceNumber,
                                    "discount" to discountNumber,
                                    "description" to cleanDescription,
                                    "imageUrl" to cleanImageUrl,
                                    "stock" to stockNumber,
                                    "featured" to featured,
                                    "bestSeller" to bestSeller,
                                    "dailyOffer" to dailyOffer
                                )

                            if (isEditing) {

                                productData["updatedAt"] = currentTime

                                firestore.collection("products")
                                    .document(product!!.id)
                                    .update(productData)
                                    .addOnSuccessListener {

                                        isSaving = false
                                        onSaved()
                                    }
                                    .addOnFailureListener { exception ->

                                        isSaving = false

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to update grocery."
                                    }

                            } else {

                                productData["createdAt"] =
                                    currentTime

                                firestore.collection("products")
                                    .add(productData)
                                    .addOnSuccessListener {

                                        isSaving = false
                                        onSaved()
                                    }
                                    .addOnFailureListener { exception ->

                                        isSaving = false

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to add grocery."
                                    }
                            }
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroceryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {

                if (isSaving) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text = if (isEditing) {
                            "Save Changes"
                        } else {
                            "Add Grocery"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {

                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = GroceryDark
            )

            Text(
                text = description,
                fontSize = 11.sp,
                color = GroceryGray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GroceryGreen
            )
        )
    }
}
