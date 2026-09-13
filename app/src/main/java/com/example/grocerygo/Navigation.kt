package com.example.grocerygo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class NavigationTab {
    HOME,
    PRODUCTS,
    CART,
    ORDERS,
    ADMIN,
    PROFILE,
    CHECKOUT,
    ADMIN_ORDERS,
    WISHLIST
}

@Composable
fun GroceryNavigation(
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(NavigationTab.HOME)
    }

    var selectedProductId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Scaffold(
        bottomBar = {
            if (selectedTab != NavigationTab.CHECKOUT && selectedProductId == null) {
                NavigationBar {

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.HOME,
                        onClick = {
                            selectedTab = NavigationTab.HOME
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.PRODUCTS,
                        onClick = {
                            selectedTab = NavigationTab.PRODUCTS
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Products"
                            )
                        },
                        label = {
                            Text("Products")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.CART,
                        onClick = {
                            selectedTab = NavigationTab.CART
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        },
                        label = {
                            Text("Cart")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.ORDERS,
                        onClick = {
                            selectedTab = NavigationTab.ORDERS
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Orders"
                            )
                        },
                        label = {
                            Text("Orders")
                        }
                    )

                    if (isAdmin) {
                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.ADMIN,
                            onClick = {
                                selectedTab = NavigationTab.ADMIN
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin"
                                )
                            },
                            label = {
                                Text("Admin")
                            }
                        )
                    }

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.PROFILE,
                        onClick = {
                            selectedTab = NavigationTab.PROFILE
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile"
                            )
                        },
                        label = {
                            Text("Profile")
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            when (selectedTab) {

                NavigationTab.HOME -> {
                    HomeScreen(
                        onProductClick = { product ->
                            selectedProductId = product.id
                        },
                        onSearchClick = {
                            selectedTab = NavigationTab.PRODUCTS
                        },
                        onProfileClick = {
                            selectedTab = NavigationTab.PROFILE
                        },
                        onCartClick = {
                            selectedTab = NavigationTab.CART
                        }
                    )
                }

                NavigationTab.PRODUCTS -> {
                    SearchScreen(
                        onProductClick = { product ->
                            selectedProductId = product.id
                        }
                    )
                }

                NavigationTab.CART -> {
                    CartScreen(
                        onCheckoutClick = {
                            selectedTab = NavigationTab.CHECKOUT
                        }
                    )
                }

                NavigationTab.ORDERS -> {
                    OrderScreen()
                }

                NavigationTab.ADMIN -> {
                    if (isAdmin) {
                        AdminScreen(
                            onBackClick = {
                                selectedTab = NavigationTab.HOME
                            },
                            onLogout = onLogout,
                            onManageOrdersClick = {
                                selectedTab = NavigationTab.ADMIN_ORDERS
                            }
                        )
                    } else {
                        selectedTab = NavigationTab.HOME
                    }
                }

                NavigationTab.PROFILE -> {
                    ProfileScreen(
                        onLogout = onLogout,
                        onWishlistClick = {
                            selectedTab = NavigationTab.WISHLIST
                        },
                        onOrdersClick = {
                            selectedTab = NavigationTab.ORDERS
                        }
                    )
                }

                NavigationTab.CHECKOUT -> {
                    CheckoutScreen(
                        onBackClick = {
                            selectedTab = NavigationTab.CART
                        },
                        onOrderPlaced = {
                            CartManager.clearCart()
                            selectedTab = NavigationTab.ORDERS
                        }
                    )
                }

                NavigationTab.ADMIN_ORDERS -> {
                    if (isAdmin) {
                        AdminOrderScreen(
                            onBackClick = {
                                selectedTab = NavigationTab.ADMIN
                            }
                        )
                    } else {
                        selectedTab = NavigationTab.HOME
                    }
                }

                NavigationTab.WISHLIST -> {
                    WishlistScreen(
                        onBackClick = {
                            selectedTab = NavigationTab.PROFILE
                        },
                        onProductClick = { productId ->
                            selectedProductId = productId
                        }
                    )
                }
            }

            selectedProductId?.let { id ->
                ProductDetailScreen(
                    productId = id,
                    onBackClick = {
                        selectedProductId = null
                    },
                    onAddToCart = { product, quantity ->
                        CartManager.addToCart(product, quantity)
                        selectedProductId = null
                        selectedTab = NavigationTab.CART
                    }
                )
            }
        }
    }
}
