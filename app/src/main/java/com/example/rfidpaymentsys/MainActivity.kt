package com.example.rfidpaymentsys
import com.example.rfidpaymentsys.ui.theme.BrandGradient
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.SolidColor
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import com.example.rfidpaymentsys.data.local.SessionStore
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.example.rfidpaymentsys.data.database.AppDatabase
import com.example.rfidpaymentsys.data.repository.UserRepository
import com.example.rfidpaymentsys.serial.UsbSerialManager
import com.example.rfidpaymentsys.ui.components.*
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.screens.*
import com.example.rfidpaymentsys.ui.theme.RFIDPaymentSysTheme
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import com.example.rfidpaymentsys.viewmodel.MainViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy {
        UserRepository(
            database.userDao(),
            database.transactionDao(),
            database.auditLogDao(),
            database.validationLogDao(),
            database.incomeReportDao()
        )
    }
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository, sessionStore)
    }
    private lateinit var usbSerialManager: UsbSerialManager
    private val actionUsbPermission = "com.example.rfidpaymentsys.USB_PERMISSION"
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        usbSerialManager = UsbSerialManager(this, { connected ->
            runOnUiThread { viewModel.updateConnectionStatus(connected) }
        }) { data ->
            runOnUiThread { viewModel.onUidScanned(data) }
        }

        viewModel.onResponseToArduino = { response ->
            lifecycleScope.launch(Dispatchers.IO) {
                usbSerialManager.writeData(response)
            }
        }

        viewModel.onPlaySound = { type ->
            playSound(type)
        }

        viewModel.onConnectRequested = {
            requestUsbPermission()
        }


        enableEdgeToEdge()
        setContent {
            RFIDPaymentSysTheme {
                val onBrandScreen = viewModel.currentScreen == Screen.Splash.route ||
                    viewModel.currentScreen == Screen.Welcome.route
                val darkTheme = isSystemInDarkTheme()
                val view = LocalView.current
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false // white icons on the blue bar
                    controller.isAppearanceLightNavigationBars = !onBrandScreen && !darkTheme
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (onBrandScreen) BrandGradient else SolidColor(MaterialTheme.colorScheme.background))
                ) {
                // Keeps every screen inside the safe area: below the status bar,
                // above the navigation bar, and above the keyboard.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                ) {
                when (viewModel.currentScreen) {
                    Screen.Splash.route -> SplashScreen(viewModel = viewModel)
                    Screen.Welcome.route -> WelcomeScreen(viewModel = viewModel)
                    Screen.AdminAuth.route -> AdminAuthScreen(viewModel = viewModel)
                    Screen.OperatorAuth.route -> OperatorAuthScreen(viewModel = viewModel)
                    Screen.ClientAuth.route -> ClientAuthScreen(viewModel = viewModel)
                    Screen.OperatorTerminal.route -> HomeScreen(viewModel = viewModel)
                    Screen.OperatorProfit.route -> OperatorProfitScreen(viewModel = viewModel)
                    Screen.OperatorCashOut.route -> OperatorCashOutScreen(viewModel = viewModel)
                    Screen.AdminPanel.route -> AdminScreen(viewModel = viewModel)
                    Screen.ClientDashboard.route -> ClientDashboard(viewModel = viewModel)
                    Screen.ClientSupport.route -> UserSupportScreen(viewModel = viewModel)
                    Screen.OperatorSupport.route -> UserSupportScreen(viewModel = viewModel)
                    Screen.CardScanner.route -> CardScannerScreen(viewModel = viewModel)
                    else -> WelcomeScreen(viewModel = viewModel)
                }
                ForgotPasswordChat(viewModel)
                WelcomeBanner(viewModel, Modifier.align(Alignment.TopCenter))
                }
                // The phone's status bar area takes the same blue as the top bars
                if (!onBrandScreen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(brandBarColor())
                    )
                }
                }

                // Global Dialogs
                ForgotPasswordDialog(viewModel = viewModel)
                ChangePasswordDialog(viewModel = viewModel)
                LogoutConfirmationDialog(viewModel = viewModel)
            }
        }
    }

    private fun requestUsbPermission() {
        try {
            viewModel.scannerStatus = "Searching..."
            val manager = getSystemService(USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            
            if (availableDrivers.isEmpty()) {
                viewModel.updateConnectionStatus(false)
                viewModel.scannerStatus = "No Device Found"
                return
            }

            val driver = availableDrivers[0]
            val device = driver.device

            if (manager.hasPermission(device)) {
                usbSerialManager.openConnection()
            } else {
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission), flags)
                
                if (!isReceiverRegistered) {
                    val filter = IntentFilter(actionUsbPermission)
                    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                    isReceiverRegistered = true
                }
                
                manager.requestPermission(device, permissionIntent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "USB Error", e)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (actionUsbPermission == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply { usbSerialManager.openConnection() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = getSystemService(USB_SERVICE) as UsbManager
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (drivers.isNotEmpty() && manager.hasPermission(drivers[0].device)) {
            usbSerialManager.openConnection()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun playSound(type: MainViewModel.SoundType) {
        val resId = when (type) {
            MainViewModel.SoundType.SCAN -> R.raw.beep_scan
            MainViewModel.SoundType.SUCCESS -> R.raw.beep_success
            MainViewModel.SoundType.ERROR -> R.raw.beep_error
        }
        
        try {
            MediaPlayer.create(this, resId)?.apply {
                setOnCompletionListener { mp -> mp.release() }
                start()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error playing sound", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbSerialManager.closeConnection()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(usbReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
    }
}
