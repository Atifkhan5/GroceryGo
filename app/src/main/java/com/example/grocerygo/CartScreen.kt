package com.example.grocerygo

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class CartItem(
    val product: GroceryProduct,
    val quantity: Int
)

object CartManager {

    private val items = mutableStateListOf<CartItem>()

    fun getItems(): SnapshotStateList<CartItem> {
        return items
    }

    fun addToCart(product: GroceryProduct, quantity: Int = 1) {

        if (product.stock <= 0) {
            return
        }

        val existingIndex = items.indexOfFirst {
            it.product.id == product.id
        }

        if (existingIndex >= 0) {

            val existingItem = items[existingIndex]
            val newQuantity = (existingItem.quantity + quantity).coerceAtMost(product.stock)

            items[existingIndex] = existingItem.copy(
                quantity = newQuantity
            )

        } else {

            items.add(
                CartItem(
                    product = product,
                    quantity = quantity.coerceAtMost(product.stock)
                )
            )
        }
    }

    fun increaseQuantity(productId: String) {

        val index = items.indexOfFirst {
            it.product.id == productId
        }

        if (index >= 0) {

            val item = items[index]

            if (item.quantity < item.product.stock) {

                items[index] = item.copy(
                    quantity = item.quantity + 1
                )
            }
        }
    }

    fun decreaseQuantity(productId: String) {

        val index = items.indexOfFirst {
            it.product.id == productId
        }

        if (index >= 0) {

            val item = items[index]

            if (item.quantity > 1) {

                items[index] = item.copy(
                    quantity = item.quantity - 1
                )

            } else {

                items.removeAt(index)
            }
        }
    }

    fun removeFromCart(productId: String) {

        items.removeAll {
            it.product.id == productId
        }
    }

    fun clearCart() {

        items.clear()
    }
}

@Composable
fun CartScreen(
    onCheckoutClick: () -> Unit = {}
) {

    val cartItems = CartManager.getItems()

    val subtotal = cartItems.sumOf {
        it.product.discountedPrice * it.quantity
    }

    val deliveryFee = if (cartItems.isEmpty()) {
        0.0
    } else {
        150.0
    }

    val total = subtotal + deliveryFee

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Cart",
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(30.dp)
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = "My Cart",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (cartItems.isNotEmpty()) {

                Text(
                    text = "${cartItems.sumOf { it.quantity }} items",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        if (cartItems.isEmpty()) {

            EmptyCartContent()

        } else {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }

                items(
                    items = cartItems,
                    key = {
                        it.product.id
                    }
                ) { cartItem ->

                    CartItemCard(
                        cartItem = cartItem,
                        onIncrease = {

                            CartManager.increaseQuantity(
                                cartItem.product.id
                            )
                        },
                        onDecrease = {

                            CartManager.decreaseQuantity(
                                cartItem.product.id
                            )
                        },
                        onRemove = {

                            CartManager.removeFromCart(
                                cartItem.product.id
                            )
                        }
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }
            }

            OrderSummary(
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                total = total,
                onCheckoutClick = onCheckoutClick
            )
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {

    val product = cartItem.product

    val discountedPrice = product.discountedPrice

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {

                if (product.imageUrl.isNotBlank()) {

                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Product",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 2
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                if (product.discount > 0) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "PKR ${"%.0f".format(discountedPrice)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Text(
                            text = "PKR ${"%.0f".format(product.price)}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }

                } else {

                    Text(
                        text = "PKR ${"%.0f".format(discountedPrice)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(30.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease quantity",
                            tint = Color(0xFF334155)
                        )
                    }

                    Text(
                        text = cartItem.quantity.toString(),
                        modifier = Modifier.width(30.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(30.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase quantity",
                            tint = if (cartItem.quantity < product.stock) {
                                Color(0xFF16A34A)
                            } else {
                                Color(0xFF94A3B8)
                            }
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                IconButton(
                    onClick = onRemove
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = Color(0xFFEF4444)
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "PKR ${
                        "%.0f".format(
                            discountedPrice * cartItem.quantity
                        )
                    }",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

@Composable
private fun EmptyCartContent() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.RemoveShoppingCart,
                contentDescription = "Empty cart",
                modifier = Modifier.size(90.dp),
                tint = Color(0xFFCBD5E1)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Your cart is empty",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Add some groceries to your cart and they will appear here.",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun OrderSummary(
    subtotal: Double,
    deliveryFee: Double,
    total: Double,
    onCheckoutClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Text(
                text = "Order Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            SummaryRow(
                label = "Subtotal",
                value = "PKR ${"%.0f".format(subtotal)}"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            SummaryRow(
                label = "Delivery Fee",
                value = "PKR ${"%.0f".format(deliveryFee)}"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Total",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "PKR ${"%.0f".format(total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A)
                )
            ) {

                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Proceed to Checkout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155)
        )
    }
}