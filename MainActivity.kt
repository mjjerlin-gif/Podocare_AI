package com.example.podocareai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    //================ UI ===================

    private lateinit var statusText: TextView
    private lateinit var tempText: TextView
    private lateinit var pressText: TextView
    private lateinit var spo2Text: TextView
    private lateinit var hrText: TextView
    private lateinit var resultText: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var countText: TextView
    private lateinit var anomalyText: TextView

    //================ AI ===================

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var modelReady = false

    //================ BLE ===================

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    //================ Statistics ===================

    private var totalReadings = 0
    private var anomalyCount = 0

    companion object {

        val SERVICE_UUID =
            UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")

        val CHAR_UUID =
            UUID.fromString("00005678-0000-1000-8000-00805f9b34fb")

        val CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // CHANGE ONLY THIS WHEN YOUR PC IP CHANGES
        const val PC_IP = "10.91.56.178"

        private const val TAG = "PodoCare"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        tempText = findViewById(R.id.tempText)
        pressText = findViewById(R.id.pressText)
        spo2Text = findViewById(R.id.spo2Text)
        hrText = findViewById(R.id.hrText)
        resultText = findViewById(R.id.resultText)
        resultCard = findViewById(R.id.resultCard)
        countText = findViewById(R.id.countText)
        anomalyText = findViewById(R.id.anomalyText)

        requestPermissions()

        Thread {

            loadONNX()

        }.start()
    }

    //================ Permissions ===================

    private fun requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(

                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

                ),

                100

            )

        } else {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(

                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

                ),

                100

            )

        }

    }

    override fun onRequestPermissionsResult(

        requestCode: Int,

        permissions: Array<out String>,

        grantResults: IntArray

    ) {

        super.onRequestPermissionsResult(

            requestCode,

            permissions,

            grantResults

        )

        Handler(Looper.getMainLooper()).postDelayed({

            startScan()

        }, 1000)

    }

    //================ Load ONNX ===================

    private fun loadONNX() {

        try {

            updateStatus("Loading AI Model...")

            ortEnv = OrtEnvironment.getEnvironment()

            val model = assets.open("podocareai.onnx").readBytes()

            ortSession = ortEnv!!.createSession(

                model,

                OrtSession.SessionOptions()

            )

            modelReady = true

            updateStatus("AI Ready ✓")

            Log.d(TAG, "Model Loaded Successfully")

        } catch (e: Exception) {

            modelReady = false

            Log.e(TAG, e.message ?: "Unknown")

            updateStatus("AI Model Failed")

        }

    }
    //================ BLE Scan ===================

    private fun startScan() {

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        if (!bluetoothAdapter.isEnabled) {
            updateStatus("Please enable Bluetooth")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            updateStatus("Bluetooth permission denied")
            return
        }

        if (isScanning) return

        isScanning = true

        updateStatus("🔍 Scanning for PodoCare...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter.bluetoothLeScanner.startScan(
            null,
            settings,
            scanCallback
        )
    }

    private fun stopScan() {

        if (!isScanning) return

        isScanning = false

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)

        }
    }


//================ Scan Callback ===================

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) return

            val name = result.device.name ?: return

            Log.d(TAG, "Found Device : $name")

            if (name == "PodoCare") {

                stopScan()

                updateStatus("Connecting...")

                connectDevice(result.device)

            }

        }

        override fun onScanFailed(errorCode: Int) {

            isScanning = false

            updateStatus("Scan Failed ($errorCode)")

            Handler(Looper.getMainLooper()).postDelayed({

                startScan()

            }, 3000)

        }

    }


//================ Connect ===================

    private fun connectDevice(device: BluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        bluetoothGatt = device.connectGatt(

            this,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE

        )

    }


//================ GATT ===================

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(

            gatt: BluetoothGatt,
            status: Int,
            newState: Int

        ) {

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {

                    updateStatus("Connected ✓")

                    Log.d(TAG, "Connected")

                    if (ActivityCompat.checkSelfPermission(

                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT

                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        Handler(Looper.getMainLooper()).postDelayed({

                            gatt.requestMtu(185)

                        }, 300)

                    }

                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    bluetoothGatt = null

                    updateStatus("Disconnected")

                    Log.d(TAG, "Disconnected")

                    Handler(Looper.getMainLooper()).postDelayed({

                        startScan()

                    }, 2000)

                }

            }

        }

        override fun onMtuChanged(

            gatt: BluetoothGatt,
            mtu: Int,
            status: Int

        ) {

            if (ActivityCompat.checkSelfPermission(

                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT

                ) == PackageManager.PERMISSION_GRANTED
            ) {

                gatt.discoverServices()

            }

        }

        override fun onServicesDiscovered(

            gatt: BluetoothGatt,
            status: Int

        ) {

            if (status != BluetoothGatt.GATT_SUCCESS) {

                updateStatus("Service Discovery Failed")

                return

            }

            val service = gatt.getService(SERVICE_UUID)

            if (service == null) {

                updateStatus("Service Missing")

                return

            }

            val characteristic = service.getCharacteristic(CHAR_UUID)

            if (characteristic == null) {

                updateStatus("Characteristic Missing")

                return

            }

            if (ActivityCompat.checkSelfPermission(

                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT

                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return

            }

            gatt.setCharacteristicNotification(

                characteristic,

                true

            )

            val descriptor = characteristic.getDescriptor(CCCD_UUID)

            descriptor?.value =

                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            descriptor?.let {

                gatt.writeDescriptor(it)

            }

            updateStatus("🦶 Monitoring Foot Health")

            Log.d(TAG, "Notifications Enabled")

        }

        override fun onCharacteristicChanged(

            gatt: BluetoothGatt,

            characteristic: BluetoothGattCharacteristic

        ) {

            val data = characteristic.getStringValue(0) ?: return

            Log.d(TAG, "BLE : $data")

            processSensorData(data)

        }

    }
    //================ Process BLE Data ===================

    private fun processSensorData(raw: String) {

        try {

            Log.d(TAG, "Received : $raw")

            val parts = raw.trim().split(",")

            if (parts.size < 4) {
                Log.e(TAG, "Invalid Data")
                return
            }

            val temperature =
                parts[0].substringAfter(":").toFloat()

            val pressure =
                parts[1].substringAfter(":").toFloat()

            val spo2 =
                parts[2].substringAfter(":").toFloat()

            val heartRate =
                parts[3].substringAfter(":").toFloat()

            val decision =
                runAI(
                    temperature,
                    pressure,
                    spo2
                )

            totalReadings++

            if (decision == "ANOMALY")
                anomalyCount++

            updateUI(

                temperature,

                pressure,

                spo2,

                heartRate,

                decision

            )

            sendToPC(

                temperature,

                pressure,

                spo2,

                heartRate,

                decision

            )

        } catch (e: Exception) {

            Log.e(TAG, "Parse Error : ${e.message}")

        }

    }


//================ ONNX ===================

    private fun runAI(

        temp: Float,

        pressure: Float,

        spo2: Float

    ): String {

        if (!modelReady)

            return fallbackDecision(

                temp,

                pressure,

                spo2

            )

        return try {

            val input = OnnxTensor.createTensor(

                ortEnv,

                arrayOf(

                    floatArrayOf(

                        temp,

                        pressure,

                        spo2

                    )

                )

            )

            val result = ortSession!!.run(

                mapOf(

                    "float_input" to input

                )

            )

            val output = result[0].value

            val prediction = when (output) {

                is LongArray -> output[0]

                is Array<*> ->

                    (output as Array<LongArray>)[0][0]

                else -> 1L

            }

            input.close()

            result.close()

            if (prediction == 1L)

                "NORMAL"
            else

                "ANOMALY"

        } catch (e: Exception) {

            Log.e(TAG, "AI Error : ${e.message}")

            fallbackDecision(

                temp,

                pressure,

                spo2

            )

        }

    }


//================ Backup AI ===================

    private fun fallbackDecision(

        temp: Float,

        pressure: Float,

        spo2: Float

    ): String {

        return if (

            temp > 37 ||

            pressure > 400 ||

            spo2 < 93

        )

            "ANOMALY"
        else

            "NORMAL"

    }


//================ HTTP ===================

    private fun sendToPC(

        temp: Float,

        pressure: Float,

        spo2: Float,

        heart: Float,

        decision: String

    ) {

        Thread {

            try {

                val risk =

                    if (decision == "ANOMALY")

                        75
                    else

                        15

                val time =

                    SimpleDateFormat(

                        "yyyy-MM-dd'T'HH:mm:ss",

                        Locale.getDefault()

                    ).format(Date())

                val json = JSONObject()

                json.put(

                    "device_id",

                    "PodoCare-001"

                )

                json.put(

                    "timestamp",

                    time

                )

                json.put(

                    "temperature",

                    temp

                )

                json.put(

                    "pressure",

                    pressure

                )

                json.put(

                    "spo2",

                    spo2

                )

                json.put(

                    "heart_rate",

                    heart

                )

                json.put(

                    "risk_score",

                    risk

                )

                json.put(

                    "ai_decision",

                    decision

                )

                val url = URL(

                    "http://$PC_IP:5000/data"

                )

                val conn =

                    url.openConnection()

                            as HttpURLConnection

                conn.requestMethod = "POST"

                conn.connectTimeout = 3000

                conn.readTimeout = 3000

                conn.doOutput = true

                conn.setRequestProperty(

                    "Content-Type",

                    "application/json"

                )

                OutputStreamWriter(

                    conn.outputStream

                ).use {

                    it.write(

                        json.toString()

                    )

                }

                val code =

                    conn.responseCode

                val response =

                    BufferedReader(

                        InputStreamReader(

                            conn.inputStream

                        )

                    ).readText()

                Log.d(

                    TAG,

                    "HTTP $code : $response"

                )

                conn.disconnect()

            } catch (e: Exception) {

                Log.e(

                    TAG,

                    "HTTP Error : ${e.message}"

                )

            }

        }.start()

    }
    //================ UI ===================

    private fun updateUI(

        temp: Float,

        pressure: Float,

        spo2: Float,

        heart: Float,

        decision: String

    ) {

        runOnUiThread {

            tempText.text = String.format(Locale.getDefault(), "%.1f °C", temp)

            pressText.text = String.format(Locale.getDefault(), "%.0f kPa", pressure)

            spo2Text.text = String.format(Locale.getDefault(), "%.0f %%", spo2)

            hrText.text = String.format(Locale.getDefault(), "%.0f bpm", heart)

            countText.text = "Readings : $totalReadings"

            anomalyText.text = "Anomalies : $anomalyCount"

            if (decision == "NORMAL") {

                resultCard.setBackgroundColor(

                    Color.parseColor("#0D2B1A")

                )

                resultText.text = "✅ NORMAL"

                resultText.setTextColor(

                    Color.parseColor("#00FF7F")

                )

            } else {

                resultCard.setBackgroundColor(

                    Color.parseColor("#2B0D0D")

                )

                resultText.text = "🚨 ANOMALY DETECTED"

                resultText.setTextColor(

                    Color.parseColor("#FF4444")

                )

            }

        }

    }


//================ Status ===================

    private fun updateStatus(

        msg: String

    ) {

        runOnUiThread {

            statusText.text = msg

        }

    }


//================ Cleanup ===================

    override fun onDestroy() {

        super.onDestroy()

        try {

            if (ActivityCompat.checkSelfPermission(

                    this,

                    Manifest.permission.BLUETOOTH_CONNECT

                ) == PackageManager.PERMISSION_GRANTED
            ) {

                bluetoothGatt?.disconnect()

                bluetoothGatt?.close()

            }

        } catch (e: Exception) {

            Log.e(TAG, e.message ?: "")

        }

        try {

            ortSession?.close()

            ortEnv?.close()

        } catch (e: Exception) {

            Log.e(TAG, e.message ?: "")

        }

    }
}