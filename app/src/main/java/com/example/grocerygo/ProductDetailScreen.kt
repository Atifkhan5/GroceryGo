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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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

private enum class SearchSortOption(
    val title: String
) {
    RELEVANCE("Relevance"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    DISCOUNT_HIGH_TO_LOW("Highest Discount"),
    NAME_A_TO_Z("Name: A-Z"),
    NAME_Z_TO_A("Name: Z-A")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit = {},
    onProductClick: (GroceryProduct) -> Unit = {},
    onCartClick: () -> Unit = {}
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

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var onlyInStock by remember {
        mutableStateOf(false)
    }

    var onlyDiscounted by remember {
        mutableStateOf(false)
    }

    var selectedSort by remember {
        mutableStateOf(SearchSortOption.RELEVANCE)
    }

    var showFilterDialog by remember {
        mutableStateOf(false)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
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
                            ?: "Failed to load products."

                    isLoading = false

                    return@addSnapshotListener
                }

                products =
                    snapshot?.documents
                        ?.mapNotNull { document ->

                            try {

                                GroceryProduct(
                                    id = document.id,
                                    name =
                                        document.getString(
                                            "name"
                                        ) ?: "",
                                    category =
                                        document.getString(
                                            "category"
                                        ) ?: "",
                                    price =
                                        document.getDouble(
                                            "price"
                                        ) ?: 0.0,
                                    discount =
                                        document.getDouble(
                                            "discount"
                                        ) ?: 0.0,
                                    description =
                                        document.getString(
                                            "description"
                                        ) ?: "",
                                    imageUrl =
                                        document.getString(
                                            "imageUrl"
                                        ) ?: "",
                                    stock =
                                        document.getLong(
                                            "stock"
                                        )?.toInt() ?: 0,
                                    featured =
                                        document.getBoolean(
                                            "featured"
                                        ) ?: false,
                                    bestSeller =
                                        document.getBoolean(
                                            "bestSeller"
                                        ) ?: false,
                                    dailyOffer =
                                        document.getBoolean(
                                            "dailyOffer"
                                        ) ?: false,
                                    createdAt =
                                        document.getLong(
                                            "createdAt"
                                        ) ?: 0L
                                )

                            } catch (
                                e: Exception
                            ) {

                                null
                            }
                        }
                        ?.sortedBy {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
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

    val normalizedQuery =
        searchQuery.trim().lowercase(
            Locale.getDefault()
        )

    var filteredProducts =
        products.filter { product ->

            val matchesSearch =

                normalizedQuery.isBlank() ||

                        product.name
                            .lowercase(
                                Locale.getDefault()
                            )
                            .contains(
                                normalizedQuery
                            ) ||

                        product.category
                            .lowercase(
                                Locale.getDefault()
                            )
                            .contains(
                                normalizedQuery
                            ) ||

                        product.description
                            .lowercase(
                                Locale.getDefault()
                            )
                            .contains(
                                normalizedQuery
                            )

            val matchesCategory =
                selectedCategory == "All" ||
                        product.category.equals(
                            selectedCategory,
                            ignoreCase = true
                        )

            val matchesStock =
                !onlyInStock ||
                        product.stock > 0

            val matchesDiscount =
                !onlyDiscounted ||
                        product.discount > 0

            matchesSearch &&
                    matchesCategory &&
                    matchesStock &&
                    matchesDiscount
        }

    filteredProducts =
        when (selectedSort) {

            SearchSortOption.RELEVANCE -> {

                if (normalizedQuery.isBlank()) {

                    filteredProducts.sortedBy {
                        it.name.lowercase(
                            Locale.getDefault()
                        )
                    }

                } else {

                    filteredProducts.sortedWith(
                        compareBy<GroceryProduct> {

                            when {

                                it.name
                                    .equals(
                                        searchQuery.trim(),
                                        ignoreCase = true
                                    ) -> 0

                                it.name
                                    .startsWith(
                                        searchQuery.trim(),
                                        ignoreCase = true
                                    ) -> 1

                                it.name.contains(
                                    searchQuery.trim(),
                                    ignoreCase = true
                                ) -> 2

                                it.category.contains(
                                    searchQuery.trim(),
                                    ignoreCase = true
                                ) -> 3

                                else -> 4
                            }
                        }.thenBy {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
                        }
                    )
                }
            }

            SearchSortOption.PRICE_LOW_TO_HIGH -> {

                filteredProducts.sortedBy {
                    it.discountedPrice
                }
            }

            SearchSortOption.PRICE_HIGH_TO_LOW -> {

                filteredProducts.sortedByDescending {
                    it.discountedPrice
                }
            }

            SearchSortOption.DISCOUNT_HIGH_TO_LOW -> {

                filteredProducts.sortedByDescending {
                    it.discount
                }
            }

            SearchSortOption.NAME_A_TO_Z -> {

                filteredProducts.sortedBy {
                    it.name.lowercase(
                        Locale.getDefault()
                    )
                }
            }

            SearchSortOption.NAME_Z_TO_A -> {

                filteredProducts.sortedByDescending {
                    it.name.lowercase(
                        Locale.getDefault()
                    )
                }
            }
        }

    val hasActiveFilters =
        selectedCategory != "All" ||
                onlyInStock ||
                onlyDiscounted ||
                selectedSort !=
                SearchSortOption.RELEVANCE

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Search Groceries",
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
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Back",
                            tint = GroceryDark
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = onCartClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.FilterList,
                            contentDescription =
                                "Cart",
                            tint = GroceryDark
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF8FAF8)
                )
                .padding(paddingValues)
        ) {

            OutlinedTextField(

                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),

                placeholder = {

                    Text(
                        text =
                            "Search by name, category..."
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

                    if (
                        searchQuery.isNotBlank()
                    ) {

                        IconButton(
                            onClick = {
                                searchQuery = ""
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Close,
                                contentDescription =
                                    "Clear search"
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
                            Color(0xFFD4D4D4),
                        focusedContainerColor =
                            Color.White,
                        unfocusedContainerColor =
                            Color.White
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Box {

                    OutlinedButton(
                        onClick = {
                            showSortMenu = true
                        },
                        modifier =
                            Modifier.height(42.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Sort,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text(
                            text =
                                selectedSort.title,
                            fontSize = 11.sp
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = {
                            showSortMenu = false
                        }
                    ) {

                        SearchSortOption.entries
                            .forEach { option ->

                                DropdownMenuItem(

                                    text = {

                                        Text(
                                            text =
                                                option.title
                                        )
                                    },

                                    onClick = {

                                        selectedSort =
                                            option

                                        showSortMenu =
                                            false
                                    }
                                )
                            }
                    }
                }

                OutlinedButton(
                    onClick = {
                        showFilterDialog = true
                    },
                    modifier =
                        Modifier.height(42.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.FilterList,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(5.dp)
                    )

                    Text(
                        text = "Filters",
                        fontSize = 11.sp
                    )

                    if (hasActiveFilters) {

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(
                                    RoundedCornerShape(
                                        50
                                    )
                                )
                                .background(
                                    GroceryGreen
                                )
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (categories.isNotEmpty()) {

                LazyRow(
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    item {

                        FilterChip(
                            selected =
                                selectedCategory ==
                                        "All",
                            onClick = {
                                selectedCategory =
                                    "All"
                            },
                            label = {
                                Text("All")
                            }
                        )
                    }

                    items(
                        items = categories
                    ) { category ->

                        FilterChip(
                            selected =
                                selectedCategory
                                    .equals(
                                        category,
                                        ignoreCase =
                                            true
                                    ),
                            onClick = {

                                selectedCategory =
                                    category
                            },
                            label = {

                                Text(
                                    text = category,
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (hasActiveFilters) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Filters applied",
                        fontSize = 12.sp,
                        color = GroceryGray,
                        modifier =
                            Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {

                            selectedCategory =
                                "All"

                            onlyInStock =
                                false

                            onlyDiscounted =
                                false

                            selectedSort =
                                SearchSortOption.RELEVANCE
                        }
                    ) {

                        Text(
                            text = "Clear filters",
                            color = GroceryGreen
                        )
                    }
                }
            }

            when {

                isLoading -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
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
                            .padding(30.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Warning,
                                contentDescription =
                                    null,
                                tint =
                                    Color(0xFFD32F2F),
                                modifier =
                                    Modifier.size(55.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    "Unable to load products",
                                fontSize = 19.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = GroceryDark
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    errorMessage,
                                fontSize = 12.sp,
                                color =
                                    GroceryGray
                            )
                        }
                    }
                }

                products.isEmpty() -> {

                    SearchEmptyState(
                        title =
                            "No products available",
                        message =
                            "The admin has not added any groceries yet."
                    )
                }

                filteredProducts.isEmpty() -> {

                    SearchEmptyState(
                        title =
                            "No matching products",
                        message =
                            "Try another search or remove some filters.",
                        showClearButton = true,
                        onClear = {

                            searchQuery = ""

                            selectedCategory =
                                "All"

                            onlyInStock =
                                false

                            onlyDiscounted =
                                false

                            selectedSort =
                                SearchSortOption.RELEVANCE
                        }
                    )
                }

                else -> {

                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                top = 4.dp,
                                bottom = 30.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        item {

                            Text(
                                text =
                                    "${filteredProducts.size} product${if (filteredProducts.size == 1) "" else "s"} found",
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp
                                    ),
                                fontSize = 13.sp,
                                color =
                                    GroceryGray
                            )
                        }

                        items(
                            items =
                                filteredProducts,
                            key = {
                                it.id
                            }
                        ) { product ->

                            SearchProductCard(
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
        }
    }

    if (showFilterDialog) {

        FilterDialog(

            onlyInStock =
                onlyInStock,

            onlyDiscounted =
                onlyDiscounted,

            onOnlyInStockChange = {
                onlyInStock = it
            },

            onOnlyDiscountedChange = {
                onlyDiscounted = it
            },

            onDismiss = {
                showFilterDialog = false
            },

            onClear = {

                onlyInStock = false
                onlyDiscounted = false
                selectedCategory = "All"
                selectedSort =
                    SearchSortOption.RELEVANCE

                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun SearchProductCard(
    product: GroceryProduct,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp
            )
            .clickable {
                onClick()
            },
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            SearchProductImage(
                product = product
            )

            Spacer(
                modifier =
                    Modifier.width(13.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        product.name,
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        GroceryDark,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Category,
                        contentDescription =
                            null,
                        tint =
                            GroceryGreen,
                        modifier =
                            Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )

                    Text(
                        text =
                            product.category,
                        fontSize = 11.sp,
                        color =
                            GroceryGray
                    )
                }

                if (
                    product.description
                        .isNotBlank()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            product.description,
                        fontSize = 11.sp,
                        color =
                            GroceryGray,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "PKR %.0f",
                                product.discountedPrice
                            ),
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            GroceryGreen
                    )

                    if (
                        product.discount > 0
                    ) {

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(
                            text =
                                String.format(
                                    Locale.US,
                                    "PKR %.0f",
                                    product.price
                                ),
                            fontSize = 10.sp,
                            color =
                                GroceryGray
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            if (
                                product.stock > 0
                            ) {
                                "In stock"
                            } else {
                                "Out of stock"
                            },
                        fontSize = 10.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            if (
                                product.stock > 0
                            ) {
                                GroceryGreen
                            } else {
                                Color(0xFFD32F2F)
                            }
                    )

                    if (
                        product.discount > 0
                    ) {

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.LocalOffer,
                                contentDescription =
                                    null,
                                tint =
                                    Color(0xFFD32F2F),
                                modifier =
                                    Modifier.size(
                                        12.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(3.dp)
                            )

                            Text(
                                text =
                                    "${product.discount.toInt()}% OFF",
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchProductImage(
    product: GroceryProduct
) {

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                GroceryLightGreen
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.Inventory,
            contentDescription =
                product.name,
            tint =
                GroceryGreen,
            modifier =
                Modifier.size(42.dp)
        )
    }
}

@Composable
private fun SearchEmptyState(
    title: String,
    message: String,
    showClearButton: Boolean = false,
    onClear: () -> Unit = {}
) {

    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier.padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Search,
                contentDescription =
                    null,
                tint =
                    GroceryGreen,
                modifier =
                    Modifier.size(60.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold,
                color = GroceryDark
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text = message,
                fontSize = 12.sp,
                color = GroceryGray
            )

            if (showClearButton) {

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )

                Button(
                    onClick = onClear
                ) {

                    Text(
                        text = "Clear Search & Filters"
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    onlyInStock: Boolean,
    onlyDiscounted: Boolean,
    onOnlyInStockChange: (Boolean) -> Unit,
    onOnlyDiscountedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.FilterList,
                    contentDescription =
                        null,
                    tint =
                        GroceryGreen
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text = "Filters",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        text = {

            Column {

                Text(
                    text =
                        "Availability & Offers",
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        GroceryDark
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                FilterChip(
                    selected =
                        onlyInStock,
                    onClick = {
                        onOnlyInStockChange(
                            !onlyInStock
                        )
                    },
                    label = {
                        Text(
                            text =
                                "Only show in-stock products"
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                FilterChip(
                    selected =
                        onlyDiscounted,
                    onClick = {
                        onOnlyDiscountedChange(
                            !onlyDiscounted
                        )
                    },
                    label = {
                        Text(
                            text =
                                "Only show discounted products"
                        )
                    }
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Done",
                    color = GroceryGreen
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onClear
            ) {

                Text(
                    text = "Clear",
                    color = GroceryGray
                )
            }
        }
    )
}