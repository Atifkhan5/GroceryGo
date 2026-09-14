package com.example.grocerygo

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GroceryOrderItem(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
) {
    val total: Double
        get() = price * quantity
}

data class GroceryOrder(
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val status: String = "Pending",
    val totalAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val address: String = "",
    val phone: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val createdAt: Long = 0L,
    val items: List<GroceryOrderItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    onBackClick: () -> Unit = {},
    onOrderClick: (GroceryOrder) -> Unit = {},
    onContinueShopping: () -> Unit = {}
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    var orders by remember {
        mutableStateOf<List<GroceryOrder>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var expandedOrderId by remember {
        mutableStateOf<String?>(null)
    }

    var orderToCancel by remember {
        mutableStateOf<GroceryOrder?>(null)
    }

    var isCancelling by remember {
        mutableStateOf(false)
    }

    var listenerRegistration by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    LaunchedEffect(currentUser?.uid) {

        if (currentUser == null) {

            isLoading = false

            errorMessage =
                "Please log in to view your orders."

            return@LaunchedEffect
        }

        listenerRegistration =
            firestore
                .collection("orders")
                .whereEqualTo(
                    "userId",
                    currentUser.uid
                )
                .addSnapshotListener { snapshot, exception ->

                    if (exception != null) {

                        errorMessage =
                            exception.message
                                ?: "Unable to load orders."

                        isLoading = false

                        return@addSnapshotListener
                    }

                    orders =
                        snapshot
                            ?.documents
                            ?.mapNotNull { document ->

                                try {

                                    val rawItems =
                                        document.get(
                                            "items"
                                        ) as? List<*>
                                            ?: emptyList<Any>()

                                    val items =
                                        rawItems.mapNotNull { rawItem ->

                                            val item =
                                                rawItem as? Map<*, *>
                                                    ?: return@mapNotNull null

                                            GroceryOrderItem(
                                                productId =
                                                    item["productId"]
                                                        ?.toString()
                                                        ?: "",

                                                productName =
                                                    item["productName"]
                                                        ?.toString()
                                                        ?: item["name"]
                                                            ?.toString()
                                                        ?: "Product",

                                                price =
                                                    (item["price"] as? Number)
                                                        ?.toDouble()
                                                        ?: 0.0,

                                                quantity =
                                                    (item["quantity"] as? Number)
                                                        ?.toInt()
                                                        ?: 1,

                                                imageUrl =
                                                    item["imageUrl"]
                                                        ?.toString()
                                                        ?: ""
                                            )
                                        }

                                    GroceryOrder(
                                        id =
                                            document.id,

                                        userId =
                                            document.getString(
                                                "userId"
                                            )
                                                ?: "",

                                        orderNumber =
                                            document.getString(
                                                "orderNumber"
                                            )
                                                ?: document.id,

                                        status =
                                            document.getString(
                                                "status"
                                            )
                                                ?: "Pending",

                                        totalAmount =
                                            document.getDouble(
                                                "totalAmount"
                                            )
                                                ?: 0.0,

                                        subtotal =
                                            document.getDouble(
                                                "subtotal"
                                            )
                                                ?: 0.0,

                                        deliveryFee =
                                            document.getDouble(
                                                "deliveryFee"
                                            )
                                                ?: 0.0,

                                        address =
                                            document.getString(
                                                "address"
                                            )
                                                ?: "",

                                        phone =
                                            document.getString(
                                                "phone"
                                            )
                                                ?: "",

                                        paymentMethod =
                                            document.getString(
                                                "paymentMethod"
                                            )
                                                ?: "Cash on Delivery",

                                        createdAt =
                                            document.getLong(
                                                "createdAt"
                                            )
                                                ?: 0L,

                                        items = items
                                    )

                                } catch (
                                    e: Exception
                                ) {

                                    null
                                }
                            }
                            ?.sortedByDescending {
                                it.createdAt
                            }
                            ?: emptyList()

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

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "My Orders",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = GroceryDark
                        )

                        if (orders.isNotEmpty()) {

                            Text(
                                text =
                                    "${orders.size} order${if (orders.size == 1) "" else "s"}",
                                fontSize = 11.sp,
                                color = GroceryGray
                            )
                        }
                    }
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
                        onClick = {
                            isLoading = true
                            errorMessage = ""
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Refresh,
                            contentDescription =
                                "Refresh",
                            tint = GroceryDark
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        when {

            isLoading -> {

                OrderLoadingState(
                    paddingValues = paddingValues
                )
            }

            errorMessage.isNotBlank() -> {

                OrderErrorState(
                    paddingValues = paddingValues,
                    message = errorMessage,
                    onRetry = {

                        isLoading = true
                        errorMessage = ""

                        if (currentUser == null) {

                            isLoading = false

                            errorMessage =
                                "Please log in to view your orders."
                        }
                    }
                )
            }

            orders.isEmpty() -> {

                EmptyOrdersState(
                    paddingValues = paddingValues,
                    onContinueShopping =
                        onContinueShopping
                )
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFFF8FAF8)
                        )
                        .padding(paddingValues),
                    contentPadding =
                        PaddingValues(
                            top = 14.dp,
                            bottom = 30.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {

                    items(
                        items = orders,
                        key = {
                            it.id
                        }
                    ) { order ->

                        OrderCard(

                            order = order,

                            expanded =
                                expandedOrderId ==
                                        order.id,

                            onExpand = {

                                expandedOrderId =
                                    if (
                                        expandedOrderId ==
                                        order.id
                                    ) {
                                        null
                                    } else {
                                        order.id
                                    }
                            },

                            onClick = {
                                onOrderClick(order)
                            },

                            onCancel = {

                                orderToCancel =
                                    order
                            }
                        )
                    }
                }
            }
        }
    }

    orderToCancel?.let { order ->

        AlertDialog(

            onDismissRequest = {

                if (!isCancelling) {
                    orderToCancel = null
                }
            },

            icon = {

                Icon(
                    imageVector =
                        Icons.Default.Warning,
                    contentDescription =
                        null,
                    tint =
                        Color(0xFFD32F2F)
                )
            },

            title = {

                Text(
                    text = "Cancel Order?"
                )
            },

            text = {

                Text(
                    text =
                        "Are you sure you want to cancel order ${getDisplayOrderNumber(order)}?"
                )
            },

            confirmButton = {

                TextButton(
                    enabled = !isCancelling,
                    onClick = {

                        if (currentUser == null) {
                            return@TextButton
                        }

                        isCancelling = true

                        firestore
                            .collection("orders")
                            .document(order.id)
                            .update(
                                "status",
                                "Cancelled"
                            )
                            .addOnSuccessListener {

                                isCancelling = false
                                orderToCancel = null
                            }
                            .addOnFailureListener {

                                isCancelling = false
                                errorMessage =
                                    it.message
                                        ?: "Unable to cancel order."
                            }
                    }
                ) {

                    if (isCancelling) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color =
                                Color(0xFFD32F2F)
                        )

                    } else {

                        Text(
                            text = "Cancel Order",
                            color =
                                Color(0xFFD32F2F)
                        )
                    }
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !isCancelling,
                    onClick = {
                        orderToCancel = null
                    }
                ) {

                    Text(
                        text = "Keep Order",
                        color = GroceryGreen
                    )
                }
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: GroceryOrder,
    expanded: Boolean,
    onExpand: () -> Unit,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp
            ),
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

        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpand()
                    }
                    .padding(15.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OrderStatusIcon(
                    status = order.status
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            getDisplayOrderNumber(
                                order
                            ),
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            GroceryDark
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            formatOrderDate(
                                order.createdAt
                            ),
                        fontSize = 11.sp,
                        color =
                            GroceryGray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    OrderStatusBadge(
                        status =
                            order.status
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            formatPrice(
                                order.totalAmount
                            ),
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            GroceryGreen
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "${order.items.size} item${if (order.items.size == 1) "" else "s"}",
                        fontSize = 10.sp,
                        color =
                            GroceryGray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Icon(
                        imageVector =
                            if (expanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                        contentDescription =
                            if (expanded) {
                                "Collapse"
                            } else {
                                "Expand"
                            },
                        tint =
                            GroceryGray,
                        modifier =
                            Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded
            ) {

                Column {

                    Divider(
                        color =
                            Color(0xFFEAEAEA)
                    )

                    OrderDetails(
                        order = order,
                        onClick = onClick,
                        onCancel = onCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetails(
    order: GroceryOrder,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {

    Column(
        modifier =
            Modifier.padding(15.dp)
    ) {

        Text(
            text = "Order Details",
            fontSize = 15.sp,
            fontWeight =
                FontWeight.Bold,
            color = GroceryDark
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        if (order.items.isEmpty()) {

            Text(
                text =
                    "No item details available.",
                fontSize = 12.sp,
                color =
                    GroceryGray
            )

        } else {

            order.items.forEach { item ->

                OrderItemRow(
                    item = item
                )

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )
            }
        }

        Divider(
            color =
                Color(0xFFEAEAEA)
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OrderInfoRow(
            icon =
                Icons.Default.LocationOn,
            title = "Delivery Address",
            value =
                if (
                    order.address.isNotBlank()
                ) {
                    order.address
                } else {
                    "Address not provided"
                }
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        OrderInfoRow(
            icon =
                Icons.Default.ShoppingBag,
            title = "Payment",
            value =
                order.paymentMethod
        )

        if (order.phone.isNotBlank()) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OrderInfoRow(
                icon =
                    Icons.Default.Schedule,
                title = "Phone",
                value =
                    order.phone
            )
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        PriceSummary(
            order = order
        )

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = onClick,
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "View Details",
                    fontSize = 12.sp
                )
            }

            if (canCancelOrder(order.status)) {

                Button(
                    onClick = onCancel,
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Cancel Order",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    item: GroceryOrderItem
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(
                    RoundedCornerShape(10.dp)
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
                    item.productName,
                tint =
                    GroceryGreen,
                modifier =
                    Modifier.size(25.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    item.productName,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    GroceryDark,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    "${item.quantity} × ${formatPrice(item.price)}",
                fontSize = 10.sp,
                color =
                    GroceryGray
            )
        }

        Text(
            text =
                formatPrice(item.total),
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                GroceryDark
        )
    }
}

@Composable
private fun PriceSummary(
    order: GroceryOrder
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        if (order.subtotal > 0) {

            PriceRow(
                title = "Subtotal",
                amount =
                    order.subtotal
            )
        }

        if (order.deliveryFee > 0) {

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            PriceRow(
                title = "Delivery Fee",
                amount =
                    order.deliveryFee
            )
        }

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Divider(
            color =
                Color(0xFFEAEAEA)
        )

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Total",
                modifier =
                    Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    GroceryDark
            )

            Text(
                text =
                    formatPrice(
                        order.totalAmount
                    ),
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    GroceryGreen
            )
        }
    }
}

@Composable
private fun PriceRow(
    title: String,
    amount: Double
) {

    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text = title,
            modifier =
                Modifier.weight(1f),
            fontSize = 12.sp,
            color =
                GroceryGray
        )

        Text(
            text =
                formatPrice(amount),
            fontSize = 12.sp,
            color =
                GroceryDark
        )
    }
}

@Composable
private fun OrderInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.Top
    ) {

        Icon(
            imageVector = icon,
            contentDescription =
                null,
            tint =
                GroceryGreen,
            modifier =
                Modifier.size(19.dp)
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 10.sp,
                color =
                    GroceryGray
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text = value,
                fontSize = 12.sp,
                color =
                    GroceryDark
            )
        }
    }
}

@Composable
private fun OrderStatusIcon(
    status: String
) {

    val statusColor =
        getStatusColor(status)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                statusColor.copy(
                    alpha = 0.12f
                )
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                getStatusIcon(status),
            contentDescription =
                status,
            tint =
                statusColor,
            modifier =
                Modifier.size(25.dp)
        )
    }
}

@Composable
private fun OrderStatusBadge(
    status: String
) {

    val statusColor =
        getStatusColor(status)

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(7.dp)
            )
            .background(
                statusColor.copy(
                    alpha = 0.12f
                )
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text =
                normalizeStatus(status),
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                statusColor
        )
    }
}

@Composable
private fun OrderLoadingState(
    paddingValues: PaddingValues
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF8FAF8)
            )
            .padding(paddingValues),
        contentAlignment =
            Alignment.Center
    ) {

        CircularProgressIndicator(
            color =
                GroceryGreen
        )
    }
}

@Composable
private fun OrderErrorState(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF8FAF8)
            )
            .padding(paddingValues),
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
                    Icons.Default.ErrorOutline,
                contentDescription =
                    null,
                tint =
                    Color(0xFFD32F2F),
                modifier =
                    Modifier.size(58.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "Unable to load orders",
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    GroceryDark
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    message,
                fontSize = 12.sp,
                color =
                    GroceryGray
            )

            Spacer(
                modifier =
                    Modifier.height(15.dp)
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

@Composable
private fun EmptyOrdersState(
    paddingValues: PaddingValues,
    onContinueShopping: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF8FAF8)
            )
            .padding(paddingValues),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier.padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        GroceryLightGreen
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ShoppingBag,
                    contentDescription =
                        null,
                    tint =
                        GroceryGreen,
                    modifier =
                        Modifier.size(45.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text =
                    "No orders yet",
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    GroceryDark
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    "Your grocery orders will appear here after you place an order.",
                fontSize = 12.sp,
                color =
                    GroceryGray
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Button(
                onClick =
                    onContinueShopping
            ) {

                Text(
                    text =
                        "Continue Shopping"
                )
            }
        }
    }
}

private fun canCancelOrder(
    status: String
): Boolean {

    return when (
        status.trim().lowercase(
            Locale.getDefault()
        )
    ) {

        "pending" -> true
        "placed" -> true
        "confirmed" -> true

        else -> false
    }
}

private fun normalizeStatus(
    status: String
): String {

    val value =
        status.trim()

    if (value.isBlank()) {
        return "Pending"
    }

    return value
        .lowercase(
            Locale.getDefault()
        )
        .split(
            " ",
            "_",
            "-"
        )
        .joinToString(" ") { word ->

            word.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(
                        Locale.getDefault()
                    )
                } else {
                    it.toString()
                }
            }
        }
}

private fun getStatusColor(
    status: String
): Color {

    return when (
        status.trim().lowercase(
            Locale.getDefault()
        )
    ) {

        "pending" ->
            Color(0xFFF59E0B)

        "placed" ->
            Color(0xFFF59E0B)

        "confirmed" ->
            Color(0xFF2563EB)

        "processing" ->
            Color(0xFF2563EB)

        "packed" ->
            Color(0xFF7C3AED)

        "out for delivery" ->
            Color(0xFF0891B2)

        "out_for_delivery" ->
            Color(0xFF0891B2)

        "delivered" ->
            GroceryGreen

        "completed" ->
            GroceryGreen

        "cancelled" ->
            Color(0xFFD32F2F)

        "canceled" ->
            Color(0xFFD32F2F)

        "rejected" ->
            Color(0xFFD32F2F)

        else ->
            GroceryGray
    }
}

private fun getStatusIcon(
    status: String
): androidx.compose.ui.graphics.vector.ImageVector {

    return when (
        status.trim().lowercase(
            Locale.getDefault()
        )
    ) {

        "delivered",
        "completed" ->
            Icons.Default.CheckCircle

        "cancelled",
        "canceled",
        "rejected" ->
            Icons.Default.Close

        "processing",
        "packed" ->
            Icons.Default.Inventory

        "out for delivery",
        "out_for_delivery" ->
            Icons.Default.LocationOn

        else ->
            Icons.Default.Schedule
    }
}

private fun formatPrice(
    amount: Double
): String {

    return String.format(
        Locale.US,
        "PKR %.0f",
        amount
    )
}

private fun formatOrderDate(
    timestamp: Long
): String {

    if (timestamp <= 0L) {
        return "Date unavailable"
    }

    return try {

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

        formatter.format(
            Date(timestamp)
        )

    } catch (
        e: Exception
    ) {

        "Date unavailable"
    }
}

private fun getDisplayOrderNumber(
    order: GroceryOrder
): String {

    return if (
        order.orderNumber.isNotBlank()
    ) {

        if (
            order.orderNumber
                .startsWith("#")
        ) {
            order.orderNumber
        } else {
            "#${order.orderNumber}"
        }

    } else {

        "#${order.id.take(8).uppercase()}"
    }
}