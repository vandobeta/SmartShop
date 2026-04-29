package com.smartshop.app.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Scanner : Screen("scanner")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object Settings : Screen("settings")
    object ManagerDashboard : Screen("manager_dashboard")
}