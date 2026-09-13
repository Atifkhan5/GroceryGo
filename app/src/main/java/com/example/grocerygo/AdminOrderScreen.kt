package com.example.grocerygo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderScreen(
    onBackClick: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var orders by remember { mutableStateOf<List<GroceryOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<GroceryOrder?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }

    val statusOptions = listOf("Pending", "Confirmed", "Preparing", "Out for Delivery", "Delivered", "Cancelled")

    LaunchedEffect(Unit) {
        firestore.collection("orders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Failed to load orders"
                    isLoading = false
                    return@addSnapshotListener
                }
                orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val rawItems = doc.get("items") as? List<*> ?: emptyList<Any>()
                        val items = rawItems.mapNotNull { rawItem ->
                            val item = rawItem as? Map<*, *> ?: return@mapNotNull null
                            GroceryOrderItem(
                                productId = item["productId"]?.toString() ?: "",
                                productName = item["productName"]?.toString() ?: "Product",
                                price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                                quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                imageUrl = item["imageUrl"]?.toString() ?: ""
                            )
                        }
                        GroceryOrder(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            orderNumber = doc.getString("orderNumber") ?: doc.id,
                            status = doc.getString("status") ?: "Pending",
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            subtotal = doc.getDouble("subtotal") ?: 0.0,
                            deliveryFee = doc.getDouble("deliveryFee") ?: 0.0,
                            address = doc.getString("address") ?: "",
                            phone = doc.getString("phone") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: "Cash on Delivery",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            items = items
                        )
                    } catch (ex: Exception) { null }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage All Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAF8)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    AdminOrderCard(
                        order = order,
                        onStatusClick = {
                            selectedOrder = order
                            showStatusDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showStatusDialog && selectedOrder != null) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    statusOptions.forEach { status ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                firestore.collection("orders").document(selectedOrder!!.id)
                                    .update("status", status)
                                showStatusDialog = false
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (selectedOrder!!.status == status), onClick = null)
                            Text(status, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun AdminOrderCard(order: GroceryOrder, onStatusClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold, color = GroceryDark)
                Text("PKR ${order.totalAmount.toInt()}", fontWeight = FontWeight.Bold, color = GroceryGreen)
            }
            Spacer(Modifier.height(8.dp))
            Text("Customer ID: ${order.userId.take(8)}...", fontSize = 12.sp, color = GroceryGray)
            Text("Phone: ${order.phone}", fontSize = 12.sp, color = GroceryGray)
            Text("Address: ${order.address}", fontSize = 12.sp, color = GroceryGray, maxLines = 1)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(order.status)
                Button(onClick = onStatusClick, colors = ButtonDefaults.buttonColors(containerColor = GroceryLightGreen, contentColor = GroceryGreen)) {
                    Text("Update Status", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "delivered" -> Color(0xFF16A34A)
        "cancelled" -> Color(0xFFDC2626)
        "pending" -> Color(0xFFD97706)
        else -> Color(0xFF2563EB)
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
