package com.example.smartwatchapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smartwatchapp.data.HealthData
import com.example.smartwatchapp.data.HealthDatabase
import com.example.smartwatchapp.databinding.ActivityMainBinding
import com.qc.watch.sdk.BleOperateManager
import com.qc.watch.sdk.CommandHandle
import com.qc.watch.sdk.listener.OutDeviceListener
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: HealthDatabase
    private var isConnected = false
    private var currentHeartRate: Int? = null
    private var currentSteps: Int? = null
    private var currentBloodOxygen: Int? = null

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startBluetoothScan()
        } else {
            Toast.makeText(this, "Permissions required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = HealthDatabase.getDatabase(this)
        
        initializeBluetooth()
        setupClickListeners()
        setupDeviceListener()
        startPeriodicDataStorage()
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun setupClickListeners() {
        binding.scanButton.setOnClickListener {
            if (checkPermissions()) {
                startBluetoothScan()
            } else {
                requestPermissionLauncher.launch(bluetoothPermissions)
            }
        }
    }

    private fun setupDeviceListener() {
        BleOperateManager.getInstance().addOutDeviceListener(object : OutDeviceListener {
            override fun onConnectSuccess() {
                runOnUiThread {
                    isConnected = true
                    binding.connectionStatus.text = "Status: Connected"
                    binding.scanButton.text = "Disconnect"
                    startDataCollection()
                }
            }

            override fun onConnectFailed() {
                runOnUiThread {
                    binding.connectionStatus.text = "Status: Connection Failed"
                    Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDisconnect() {
                runOnUiThread {
                    isConnected = false
                    binding.connectionStatus.text = "Status: Disconnected"
                    binding.scanButton.text = "Scan for Devices"
                }
            }
        })
    }

    private fun startBluetoothScan() {
        if (isConnected) {
            BleOperateManager.getInstance().disconnect()
            return
        }

        // Start scanning for devices
        BleOperateManager.getInstance().startScan { deviceList ->
            if (deviceList.isNotEmpty()) {
                // Connect to the first found device
                val device = deviceList[0]
                BleOperateManager.getInstance().connectDirectly(device.address)
            }
        }
    }

    private fun startDataCollection() {
        // Request heart rate data
        CommandHandle.getInstance().executeReqCmd(CommandHandle.CMD_GET_HEART_RATE) { success, data ->
            if (success && data != null) {
                currentHeartRate = data.toInt()
                runOnUiThread {
                    binding.heartRateValue.text = "$currentHeartRate BPM"
                }
            }
        }

        // Request steps data
        CommandHandle.getInstance().executeReqCmd(CommandHandle.CMD_GET_STEPS) { success, data ->
            if (success && data != null) {
                currentSteps = data.toInt()
                runOnUiThread {
                    binding.stepsValue.text = "$currentSteps steps"
                }
            }
        }

        // Request blood oxygen data
        CommandHandle.getInstance().executeReqCmd(CommandHandle.CMD_GET_BLOOD_OXYGEN) { success, data ->
            if (success && data != null) {
                currentBloodOxygen = data.toInt()
                runOnUiThread {
                    binding.bloodOxygenValue.text = "$currentBloodOxygen%"
                }
            }
        }
    }

    private fun startPeriodicDataStorage() {
        lifecycleScope.launch {
            while (true) {
                if (isConnected) {
                    storeCurrentData()
                }
                kotlinx.coroutines.delay(5 * 60 * 1000) // Store data every 5 minutes
            }
        }
    }

    private fun storeCurrentData() {
        if (currentHeartRate != null || currentSteps != null || currentBloodOxygen != null) {
            val healthData = HealthData(
                timestamp = Date(),
                heartRate = currentHeartRate,
                steps = currentSteps,
                bloodOxygen = currentBloodOxygen
            )
            
            lifecycleScope.launch {
                database.healthDataDao().insert(healthData)
                Toast.makeText(this@MainActivity, "Data stored successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
} 