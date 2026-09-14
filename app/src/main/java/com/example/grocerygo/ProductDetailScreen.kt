package com.example.grocerygo

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onAddToCart: (GroceryProduct, Int) -> Unit
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var product by remember {
        mutableStateOf<GroceryProduct?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var quantity by remember {
        mutableIntStateOf(1)
    }

    var isFavorite by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    LaunchedEffect(productId, auth.currentUser?.uid) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .collection("wishlist").document(productId)
                .get()
                .addOnSuccessListener { isFavorite = it.exists() }
        }
    }

    fun toggleFavorite(groceryProduct: GroceryProduct) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Please login to add to wishlist", Toast.LENGTH_SHORT).show()
            return
        }
        
        val wishlistRef = firestore.collection("users").document(user.uid)
            .collection("wishlist").document(productId)
            
        if (isFavorite) {
            wishlistRef.delete()
                .addOnSuccessListener { 
                    isFavorite = false
                    Toast.makeText(context, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to remove: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            val favData = hashMapOf(
                "name" to groceryProduct.name,
                "category" to groceryProduct.category,
                "price" to groceryProduct.price,
                "discount" to groceryProduct.discount,
                "imageUrl" to groceryProduct.imageUrl,
                "stock" to groceryProduct.stock,
                "unit" to groceryProduct.unit,
                "addedAt" to System.currentTimeMillis()
            )
            wishlistRef.set(favData)
                .addOnSuccessListener { 
                    isFavorite = true
                    Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to add: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    LaunchedEffect(productId) {

        isLoading = true
        errorMessage = ""

        firestore
            .collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    try {

                        product = GroceryProduct(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            category = document.getString("category") ?: "",
                            price = document.getDouble("price") ?: 0.0,
                            discount = document.getDouble("discount") ?: 0.0,
                            description = document.getString("description") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            featured = document.getBoolean("featured") ?: false,
                            bestSeller = document.getBoolean("bestSeller") ?: false,
                            dailyOffer = document.getBoolean("dailyOffer") ?: false,
                            unit = document.getString("unit") ?: "piece",
                            stock = (document.get("stock") as? Number)?.toInt() ?: 0,
                            createdAt = (document.get("createdAt") as? Number)?.toLong() ?: 0L
                        )

                    } catch (exception: Exception) {

                        errorMessage =
                            exception.message
                                ?: "Unable to read product details."
                    }

                } else {

                    errorMessage = "Product does not exist."
                }

                isLoading = false
            }
            .addOnFailureListener { exception ->

                isLoading = false

                errorMessage =
                    exception.message
                        ?: "Unable to load product."
            }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Product Details",
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
                }
            )
        }

    ) { paddingValues ->

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = GroceryGreen
                )
            }

            return@Scaffold
        }

        if (errorMessage.isNotBlank()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Unable to Load Product",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        color = GroceryGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GroceryGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text("Go Back")
                    }
                }
            }

            return@Scaffold
        }

        product?.let { groceryProduct ->

            val discountedPrice =
                groceryProduct.discountedPrice

            val totalPrice =
                discountedPrice * quantity

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAF8))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(GroceryLightGreen),
                    contentAlignment = Alignment.Center
                ) {

                    if (groceryProduct.imageUrl.isNotBlank()) {

                        AsyncImage(
                            model = groceryProduct.imageUrl,
                            contentDescription = groceryProduct.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = GroceryGreen,
                            modifier = Modifier.size(90.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            toggleFavorite(groceryProduct)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                Color.White,
                                RoundedCornerShape(50)
                            )
                    ) {

                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            tint = if (isFavorite) {
                                Color(0xFFD32F2F)
                            } else {
                                GroceryDark
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        text = groceryProduct.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = GroceryGreen,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            text = groceryProduct.category,
                            fontSize = 14.sp,
                            color = GroceryGray
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Sold per ${groceryProduct.unit}",
                        fontSize = 13.sp,
                        color = GroceryGray
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = String.format(
                                Locale.US,
                                "PKR %.0f",
                                discountedPrice
                            ),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = GroceryGreen
                        )

                        if (groceryProduct.discount > 0) {

                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "PKR %.0f",
                                    groceryProduct.price
                                ),
                                fontSize = 14.sp,
                                color = GroceryGray,
                                textDecoration = TextDecoration.LineThrough
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFFFE5E5),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 5.dp
                                    )
                            ) {

                                Text(
                                    text = "${groceryProduct.discount.toInt()}% OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    if (
                        groceryProduct.featured ||
                        groceryProduct.bestSeller ||
                        groceryProduct.dailyOffer
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            if (groceryProduct.featured) {
                                ProductDetailTag(
                                    text = "Featured"
                                )
                            }

                            if (groceryProduct.bestSeller) {
                                ProductDetailTag(
                                    text = "Best Seller"
                                )
                            }

                            if (groceryProduct.dailyOffer) {
                                ProductDetailTag(
                                    text = "Daily Offer"
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    Text(
                        text = "Description",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroceryDark
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = groceryProduct.description,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = GroceryGray
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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

                            Text(
                                text = "Quantity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GroceryDark
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "Available: ${groceryProduct.stock} ${groceryProduct.unit}",
                                fontSize = 13.sp,
                                color = GroceryGray
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                IconButton(
                                    onClick = {
                                        if (quantity > 1) {
                                            quantity--
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            GroceryLightGreen,
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = GroceryGreen
                                    )
                                }

                                Text(
                                    text = quantity.toString(),
                                    modifier = Modifier.width(50.dp),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GroceryDark,
                                    textAlign = TextAlign.Center
                                )

                                IconButton(
                                    onClick = {
                                        if (quantity < groceryProduct.stock) {
                                            quantity++
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            GroceryLightGreen,
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = if (
                                            quantity < groceryProduct.stock
                                        ) {
                                            GroceryGreen
                                        } else {
                                            GroceryGray
                                        }
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.weight(1f)
                                )

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {

                                    Text(
                                        text = "Total",
                                        fontSize = 12.sp,
                                        color = GroceryGray
                                    )

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "PKR %.0f",
                                            totalPrice
                                        ),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GroceryGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            if (groceryProduct.stock > 0) {
                                onAddToCart(groceryProduct, quantity)
                            } else {
                                Toast.makeText(context, "Out of stock", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = groceryProduct.stock > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GroceryGreen,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = if (groceryProduct.stock > 0) {
                                "Add to Cart"
                            } else {
                                "Out of Stock"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailTag(
    text: String
) {

    Box(
        modifier = Modifier
            .background(
                GroceryLightGreen,
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {

        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GroceryGreen
        )
    }
}