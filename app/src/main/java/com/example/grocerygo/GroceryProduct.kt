package com.example.grocerygo

data class GroceryProduct(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val discount: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val unit: String = "piece",
    val featured: Boolean = false,
    val bestSeller: Boolean = false,
    val dailyOffer: Boolean = false,
    val createdAt: Long = 0L
) {
    val discountedPrice: Double
        get() = price - (price * discount / 100.0)
}