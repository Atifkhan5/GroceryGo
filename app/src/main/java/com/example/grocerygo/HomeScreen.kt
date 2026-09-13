package com.example.grocerygo

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProductClick: (GroceryProduct) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var products by remember {
        mutableStateOf<List<GroceryProduct>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var listenerRegistration by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    LaunchedEffect(Unit) {
        listenerRegistration = firestore
            .collection("products")
            .addSnapshotListener { snapshot, exception ->

                if (exception != null) {
                    errorMessage =
                        exception.message
                            ?: "Failed to load groceries."

                    isLoading = false
                    return@addSnapshotListener
                }

                products =
                    snapshot?.documents
                        ?.mapNotNull { document ->

                            try {
                                GroceryProduct(
                                    id = document.id,
                                    name = document.getString("name") ?: "",
                                    category = document.getString("category") ?: "",
                                    price =
                                        (document.get("price") as? Number)
                                            ?.toDouble()
                                            ?: 0.0,
                                    discount =
                                        (document.get("discount") as? Number)
                                            ?.toDouble()
                                            ?: 0.0,
                                    description =
                                        document.getString("description")
                                            ?: "",
                                    imageUrl =
                                        document.getString("imageUrl")
                                            ?: "",
                                    stock =
                                        (document.get("stock") as? Number)
                                            ?.toInt()
                                            ?: 0,
                                    unit =
                                        document.getString("unit")
                                            ?: "piece",
                                    featured =
                                        document.getBoolean("featured")
                                            ?: false,
                                    bestSeller =
                                        document.getBoolean("bestSeller")
                                            ?: false,
                                    dailyOffer =
                                        document.getBoolean("dailyOffer")
                                            ?: false,
                                    createdAt =
                                        (document.get("createdAt") as? Number)
                                            ?.toLong()
                                            ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        ?.sortedByDescending {
                            it.createdAt
                        }
                        ?: emptyList()

                isLoading = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    val filteredProducts =
        products.filter { product ->

            val query = searchQuery.trim()

            query.isBlank() ||
                    product.name.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    product.category.contains(
                        query,
                        ignoreCase = true
                    )
        }

    val categories =
        products
            .map {
                it.category.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .sorted()

    val featuredProducts =
        filteredProducts.filter {
            it.featured && it.stock > 0
        }

    val bestSellerProducts =
        filteredProducts.filter {
            it.bestSeller && it.stock > 0
        }

    val dailyOfferProducts =
        filteredProducts.filter {
            it.dailyOffer &&
                    it.discount > 0 &&
                    it.stock > 0
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "GroceryGo",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = GroceryDark
                        )

                        Text(
                            text = "Fresh groceries, delivered.",
                            fontSize = 11.sp,
                            color = GroceryGray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCartClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = GroceryDark
                        )
                    }

                    IconButton(
                        onClick = onProfileClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
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
                        .background(Color.White)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GroceryGreen
                    )
                }
            }

            errorMessage.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(55.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = "Unable to load groceries",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GroceryDark
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = GroceryGray
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAF8))
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        bottom = 30.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(18.dp)
                ) {

                    item {
                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            placeholder = {
                                Text(
                                    text = "Search groceries"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Search,
                                    contentDescription =
                                        "Search",
                                    tint = GroceryGray
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
                                            imageVector =
                                                Icons.Default.Close,
                                            contentDescription =
                                                "Clear"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape =
                                RoundedCornerShape(14.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor =
                                        GroceryGreen,
                                    unfocusedBorderColor =
                                        Color(0xFFD6D6D6),
                                    focusedContainerColor =
                                        Color.White,
                                    unfocusedContainerColor =
                                        Color.White
                                )
                        )
                    }

                    if (searchQuery.isBlank()) {
                        item {
                            HomeWelcomeCard(
                                productCount =
                                    products.size
                            )
                        }
                    }

                    if (
                        searchQuery.isBlank() &&
                        categories.isNotEmpty()
                    ) {
                        item {
                            HomeSectionTitle(
                                title =
                                    "Grocery Categories"
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding =
                                    PaddingValues(
                                        horizontal = 16.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        12.dp
                                    )
                            ) {
                                items(
                                    items = categories
                                ) { category ->
                                    CategoryCard(
                                        category = category,
                                        onClick = {
                                            onCategoryClick(
                                                category
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (searchQuery.isNotBlank()) {
                        item {
                            HomeSectionTitle(
                                title =
                                    "Search Results"
                            )
                        }

                        if (filteredProducts.isEmpty()) {
                            item {
                                EmptyProductsCard(
                                    title =
                                        "No groceries found",
                                    message =
                                        "Try another product name or category."
                                )
                            }
                        } else {
                            items(
                                items = filteredProducts,
                                key = {
                                    it.id
                                }
                            ) { product ->
                                HomeProductCard(
                                    product = product,
                                    onClick = {
                                        onProductClick(
                                            product
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (
                        searchQuery.isBlank() &&
                        featuredProducts.isNotEmpty()
                    ) {
                        item {
                            HomeSectionTitle(
                                title =
                                    "Featured Products"
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding =
                                    PaddingValues(
                                        horizontal = 16.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        14.dp
                                    )
                            ) {
                                items(
                                    items =
                                        featuredProducts.take(
                                            10
                                        ),
                                    key = {
                                        "featured_${it.id}"
                                    }
                                ) { product ->
                                    HorizontalProductCard(
                                        product = product,
                                        onClick = {
                                            onProductClick(
                                                product
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (
                        searchQuery.isBlank() &&
                        bestSellerProducts.isNotEmpty()
                    ) {
                        item {
                            HomeSectionTitle(
                                title =
                                    "Best-Selling Products"
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding =
                                    PaddingValues(
                                        horizontal = 16.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        14.dp
                                    )
                            ) {
                                items(
                                    items =
                                        bestSellerProducts.take(
                                            10
                                        ),
                                    key = {
                                        "best_${it.id}"
                                    }
                                ) { product ->
                                    HorizontalProductCard(
                                        product = product,
                                        onClick = {
                                            onProductClick(
                                                product
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (
                        searchQuery.isBlank() &&
                        dailyOfferProducts.isNotEmpty()
                    ) {
                        item {
                            HomeSectionTitle(
                                title =
                                    "Daily Offers & Discounts"
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding =
                                    PaddingValues(
                                        horizontal = 16.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        14.dp
                                    )
                            ) {
                                items(
                                    items =
                                        dailyOfferProducts.take(
                                            10
                                        ),
                                    key = {
                                        "offer_${it.id}"
                                    }
                                ) { product ->
                                    OfferProductCard(
                                        product = product,
                                        onClick = {
                                            onProductClick(
                                                product
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (
                        products.isNotEmpty() &&
                        searchQuery.isBlank() &&
                        featuredProducts.isEmpty() &&
                        bestSellerProducts.isEmpty() &&
                        dailyOfferProducts.isEmpty()
                    ) {
                        item {
                            EmptyProductsCard(
                                title =
                                    "Products available",
                                message =
                                    "Products have been added by the admin. Mark products as Featured, Best Seller, or Daily Offer from the Admin screen to display them in these sections."
                            )
                        }
                    }

                    if (products.isEmpty()) {
                        item {
                            EmptyProductsCard(
                                title =
                                    "No groceries available",
                                message =
                                    "The admin has not added any grocery products yet."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWelcomeCard(
    productCount: Int
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Fresh groceries",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text =
                        if (productCount > 0) {
                            "$productCount products available"
                        } else {
                            "Fresh groceries at your fingertips"
                        },
                    fontSize = 13.sp,
                    color = Color.White.copy(
                        alpha = 0.9f
                    )
                )
            }

            Icon(
                imageVector =
                    Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(45.dp)
            )
        }
    }
}

@Composable
private fun HomeSectionTitle(
    title: String
) {
    Text(
        text = title,
        modifier = Modifier.padding(
            horizontal = 16.dp
        ),
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = GroceryDark
    )
}

@Composable
private fun CategoryCard(
    category: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        GroceryLightGreen
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Category,
                    contentDescription = null,
                    tint = GroceryGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = category,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GroceryDark,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeProductCard(
    product: GroceryProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ProductImage(
                imageUrl = product.imageUrl,
                modifier = Modifier.size(90.dp)
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryDark,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = product.category,
                    fontSize = 12.sp,
                    color = GroceryGray
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (product.stock > 0) {
                            "In stock: ${product.stock} ${product.unit}"
                        } else {
                            "Out of stock"
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (product.stock > 0) {
                            GroceryGreen
                        } else {
                            Color(0xFFD32F2F)
                        }
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                ProductPrice(
                    product = product
                )
            }

            Icon(
                imageVector =
                    Icons.Default.ArrowForward,
                contentDescription = null,
                tint = GroceryGray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HorizontalProductCard(
    product: GroceryProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                if (product.discount > 0) {
                    Text(
                        text =
                            "${product.discount.toInt()}% OFF",
                        modifier = Modifier
                            .padding(8.dp)
                            .background(
                                GroceryGreen,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 7.dp,
                                vertical = 4.dp
                            ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryDark,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "${product.unit} • ${product.stock} available",
                    fontSize = 11.sp,
                    color = GroceryGray
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                ProductPrice(
                    product = product
                )
            }
        }
    }
}

@Composable
private fun OfferProductCard(
    product: GroceryProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(205.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            Color(0xFFFF7043),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text =
                            "${product.discount.toInt()}% OFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GroceryDark,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "${product.stock} ${product.unit} available",
                    fontSize = 11.sp,
                    color = GroceryGray
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                ProductPrice(
                    product = product
                )
            }
        }
    }
}

@Composable
private fun ProductPrice(
    product: GroceryProduct
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                "PKR ${String.format(
                    Locale.US,
                    "%.0f",
                    product.discountedPrice
                )}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = GroceryGreen
        )

        if (product.discount > 0) {
            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Text(
                text =
                    "PKR ${String.format(
                        Locale.US,
                        "%.0f",
                        product.price
                    )}",
                fontSize = 11.sp,
                color = GroceryGray
            )
        }
    }
}

@Composable
private fun EmptyProductsCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector =
                    Icons.Default.Inventory,
                contentDescription = null,
                tint = GroceryGreen,
                modifier = Modifier.size(48.dp)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = message,
                fontSize = 12.sp,
                color = GroceryGray
            )
        }
    }
}

@Composable
private fun ProductImage(
    imageUrl: String,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                GroceryLightGreen
            ),
        contentAlignment =
            Alignment.Center
    ) {
        if (imageUrl.isBlank()) {
            Icon(
                imageVector =
                    Icons.Default.Inventory,
                contentDescription = null,
                tint = GroceryGreen,
                modifier = Modifier.size(42.dp)
            )
        } else {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}