package com.example.grocerygo

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
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
    SEARCH,
    CART,
    ORDERS,
    ADMIN,
    PROFILE
}

@Composable
fun GroceryNavigation(
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(NavigationTab.HOME)
    }

    Scaffold(
        bottomBar = {
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
                    selected = selectedTab == NavigationTab.SEARCH,
                    onClick = {
                        selectedTab = NavigationTab.SEARCH
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = {
                        Text("Search")
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
    ) { paddingValues ->

        when (selectedTab) {

            NavigationTab.HOME -> {
                HomeScreen( )
            }

            NavigationTab.SEARCH -> {
                SearchScreen( )
            }

            NavigationTab.CART -> {
                CartScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            NavigationTab.ORDERS -> {
                OrderScreen( )
            }

            NavigationTab.ADMIN -> {
                AdminScreen(
                    onBackClick = {
                        selectedTab = NavigationTab.HOME
                    },
                    onLogout = onLogout
                )
            }

            NavigationTab.PROFILE -> {
                ProfileScreen( )
            }
        }
    }
}