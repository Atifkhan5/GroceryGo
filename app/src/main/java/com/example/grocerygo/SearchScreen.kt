package com.example.grocerygo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onProductClick: (GroceryProduct) -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var products by remember { mutableStateOf<List<GroceryProduct>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("products").get()
            .addOnSuccessListener { snapshot ->
                products = snapshot.documents.mapNotNull { doc ->
                    try {
                        GroceryProduct(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            discount = doc.getDouble("discount") ?: 0.0,
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            stock = doc.getLong("stock")?.toInt() ?: 0,
                            featured = doc.getBoolean("featured") ?: false,
                            bestSeller = doc.getBoolean("bestSeller") ?: false,
                            dailyOffer = doc.getBoolean("dailyOffer") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                isLoading = false
            }
    }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        placeholder = { Text("Search groceries...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GroceryGreen,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GroceryGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts) { product ->
                    SearchProductCard(product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}

@Composable
private fun SearchProductCard(product: GroceryProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(GroceryLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, null, tint = GroceryGreen)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GroceryDark)
                Text(product.category, fontSize = 12.sp, color = GroceryGray)
                Text("PKR ${product.discountedPrice.toInt()}", fontWeight = FontWeight.Bold, color = GroceryGreen)
            }
        }
    }
}
