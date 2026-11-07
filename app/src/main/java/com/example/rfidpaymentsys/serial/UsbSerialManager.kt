package com.example.rfidpaymentsys.serial

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException

class UsbSerialManager(
    private val context: Context,
    private val onConnectionStatusChange: (Boolean) -> Unit,
    private val onDataReceived: (String) -> Unit,
) : SerialInputOutputManager.Listener {

    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null

    fun openConnection() {
        if (usbSerialPort != null) {
            Log.d("UsbSerial", "Connection already open, skipping...")
            onConnectionStatusChange(true)
            return
        }

        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            onConnectionStatusChange(false)
            return
        }

        val driver = availableDrivers[0]
        if (driver.ports.isEmpty()) {
            onConnectionStatusChange(false)
            return
        }

        val connection = manager.openDevice(driver.device)
        if (connection == null) {
            onConnectionStatusChange(false)
            return
        }

        val port = driver.ports[0]
        try {
            port.open(connection)
            // Updated to 115200 to match user's Arduino code
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            
            val ioManager = SerialInputOutputManager(port, this)
            usbIoManager = ioManager
            ioManager.start()
            
            onConnectionStatusChange(true)
            Log.d("UsbSerial", "Serial Port Opened at 115200 baud")
        } catch (e: IOException) {
            Log.e("UsbSerial", "Error opening serial port", e)
            onConnectionStatusChange(false)
            closeConnection()
        }
    }

    fun writeData(data: String) {
        try {
            usbSerialPort?.write(data.toByteArray(), 1000)
            Log.d("UsbSerial", "Sent to Arduino: $data")
        } catch (e: IOException) {
            Log.e("UsbSerial", "Error writing to serial port", e)
        }
    }

    fun closeConnection() {
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (_: IOException) {}
        usbSerialPort = null
        onConnectionStatusChange(false)
    }

    private val buffer = StringBuilder()

    override fun onNewData(data: ByteArray?) {
        data?.let {
            val received = String(it)
            Log.d("UsbSerial", "Raw data received: $received")
            buffer.append(received)
            
            if (buffer.contains("\n") || buffer.contains("\r")) {
                val lines = buffer.toString().split(Regex("[\r\n]+"))
                
                for (i in 0 until (lines.size - 1)) {
                    val line = lines[i].trim()
                    if (line.isNotEmpty()) {
                        Log.d("UsbSerial", "Processed Line: $line")
                        onDataReceived(line)
                    }
                }
                val lastPart = lines.last()
                buffer.setLength(0)
                buffer.append(lastPart)
            }
        }
    }

    override fun onRunError(e: Exception?) {
        Log.e("UsbSerial", "Serial run error", e)
        onConnectionStatusChange(false)
    }
}
