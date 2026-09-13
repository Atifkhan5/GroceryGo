package com.example.grocerygo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerygo.ui.theme.GroceryDark
import com.example.grocerygo.ui.theme.GroceryGray
import com.example.grocerygo.ui.theme.GroceryGreen
import com.example.grocerygo.ui.theme.GroceryLightGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val cartItems = CartManager.getItems()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash on Delivery") }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val subtotal = cartItems.sumOf { it.product.discountedPrice * it.quantity }
    val deliveryFee = if (cartItems.isEmpty()) 0.0 else 150.0
    val total = subtotal + deliveryFee

    val paymentMethods = listOf("Cash on Delivery", "Credit/Debit Card", "Google Pay")

    fun placeOrder() {
        val user = auth.currentUser ?: return
        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
            errorMessage = "Please fill in all details"
            return
        }

        isPlacingOrder = true
        errorMessage = ""

        val orderItems = cartItems.map {
            hashMapOf(
                "productId" to it.product.id,
                "productName" to it.product.name,
                "price" to it.product.discountedPrice,
                "quantity" to it.quantity,
                "imageUrl" to it.product.imageUrl
            )
        }

        val orderData = hashMapOf(
            "userId" to user.uid,
            "orderNumber" to "GG${System.currentTimeMillis().toString().takeLast(6)}",
            "status" to "Pending",
            "totalAmount" to total,
            "subtotal" to subtotal,
            "deliveryFee" to deliveryFee,
            "address" to address,
            "phone" to phone,
            "paymentMethod" to selectedPaymentMethod,
            "createdAt" to System.currentTimeMillis(),
            "items" to orderItems
        )

        firestore.runBatch { batch ->
            // 1. Save the order
            val orderRef = firestore.collection("orders").document()
            batch.set(orderRef, orderData)

            // 2. Decrement stock for each item
            cartItems.forEach { item ->
                val productRef = firestore.collection("products").document(item.product.id)
                val newStock = (item.product.stock - item.quantity).coerceAtLeast(0)
                batch.update(productRef, "stock", newStock)
            }
        }.addOnSuccessListener {
            isPlacingOrder = false
            onOrderPlaced()
        }.addOnFailureListener {
            isPlacingOrder = false
            errorMessage = it.message ?: "Failed to place order"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAF8))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Shipping Address", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GroceryDark)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Detailed Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(30.dp))
            
            Text("Payment Method", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GroceryDark)
            Spacer(Modifier.height(16.dp))
            
            paymentMethods.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isPlacingOrder) { selectedPaymentMethod = method }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedPaymentMethod == method),
                        onClick = { selectedPaymentMethod = method },
                        enabled = !isPlacingOrder,
                        colors = RadioButtonDefaults.colors(selectedColor = GroceryGreen)
                    )
                    Text(
                        text = method,
                        modifier = Modifier.padding(start = 12.dp),
                        fontSize = 16.sp,
                        color = GroceryDark
                    )
                }
            }
            
            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold)
                        Text("PKR ${total.toInt()}", fontWeight = FontWeight.Bold, color = GroceryGreen)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            
            Button(
                onClick = { placeOrder() },
                enabled = !isPlacingOrder && cartItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroceryGreen)
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Place Order", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
