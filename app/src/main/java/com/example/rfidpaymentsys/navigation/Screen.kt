package com.example.rfidpaymentsys.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("SPLASH")
    object Welcome : Screen("WELCOME")
    object AdminAuth : Screen("ADMIN_AUTH")
    object OperatorAuth : Screen("OPERATOR_AUTH")
    object ClientAuth : Screen("CLIENT_AUTH")
    object AdminPanel : Screen("ADMIN_PANEL")
    object OperatorTerminal : Screen("OPERATOR_TERMINAL")
    object ClientDashboard : Screen("CLIENT_DASHBOARD")
    object ClientSupport : Screen("CLIENT_SUPPORT")
    object OperatorSupport : Screen("OPERATOR_SUPPORT")
    object CardScanner : Screen("CARD_SCANNER")
    object OperatorProfit : Screen("OPERATOR_PROFIT")
    object OperatorCashOut : Screen("OPERATOR_CASH_OUT")
}
