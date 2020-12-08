package com.example.malortmobile.activities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.malortmobile.R
import com.example.malortmobile.bluetooth.BluetoothConnectionListener
import com.example.malortmobile.bluetooth.BluetoothManager
import com.example.malortmobile.data.BluetoothDeviceData
import com.example.malortmobile.data.ClockInformationData
import com.example.malortmobile.database.ClockEventManager
import com.example.malortmobile.utilities.ByteArrayParser
import java.lang.Exception
import java.sql.Timestamp
import java.time.LocalTime

class DebugBluetooth : AppCompatActivity() {
    companion object {
        const val TAG_NAME = "BTTEST"
        const val REQUEST_CODE_BLUETOOTH_ENABLE = 22
        const val REQUEST_CODE_COARSE_LOCATION = 11
    }

    private lateinit var _devicesListView:ListView
    private var reciever:BroadcastReceiver? = null
    private val nearbyBluetoothDevices = mutableListOf<BluetoothDeviceData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_bluetooth)
        findViewById<Button>(R.id.button_test_bt).setOnClickListener {
            _onBluetoothTestButton(it)
        }
        _devicesListView = findViewById<ListView>(R.id.list_devices).apply {
            setOnItemClickListener { _, _, position, _ ->
                if(nearbyBluetoothDevices[position].adress != null) {
                    _onTestConnect(nearbyBluetoothDevices[position].adress!!)
                }
            }
        }
        BluetoothManager.setListener(object : BluetoothConnectionListener {
            override fun onConnect(address: String, name: String?) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Connected to $address", Toast.LENGTH_LONG).show()

                    /*val testValue = ClockInformationData(
                        "123",
                        true,
                        "1",
                        1.1f,
                        true
                    )
                    BluetoothManager.write(ByteArrayParser.fromDataClassToByteArray(testValue))*/
                }
            }

            override fun onDisconnect() {
                TODO("Not yet implemented")
            }

            override fun onError(exception: Exception) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error $exception", Toast.LENGTH_LONG).show()
                }
                Log.e(TAG_NAME, exception.localizedMessage)
            }

            override fun onRetrievedData(data: ByteArray) {
                runOnUiThread {
                    val clockEvent: ClockInformationData? =
                        ByteArrayParser.fromByteArrayToDataClass(data)
                    Log.d(TAG_NAME, clockEvent?.userId)
                    if (clockEvent != null) {
                        val time = com.google.firebase.Timestamp.now()
                        ClockEventManager().addClockEvent(
                            clockEvent.userId!!,
                            clockEvent.radiation!!,
                            time,
                            clockEvent.clockedIn!!
                        ) { result ->
                            result.onSuccess {

                            }
                            result.onFailure {
                                Log.e(TAG_NAME, "Didn't Send any data")
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CODE_BLUETOOTH_ENABLE -> {
                if(resultCode == Activity.RESULT_OK) {
                    _onBluetoothTestButton(null)
                }
            }
            REQUEST_CODE_COARSE_LOCATION -> {
                if(resultCode == Activity.RESULT_OK) {
                    _onBluetoothTestButton(null)
                }
            }
        }
    }
    private fun _onTestConnect(address:String) {
        BluetoothManager.connectToDevice(address) { result ->
            result.onSuccess { device ->
                Toast.makeText(this, "Attempting connect: ${device.adress}, name ${device.name}", Toast.LENGTH_SHORT).show()
            }
            result.onFailure { error ->
                Toast.makeText(this, "Failed attempt! ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun _onBluetoothTestButton(view: View?) {
        Log.d(TAG_NAME, "Bluetooth is on? ${BluetoothManager.isBluetoothEnabled()}")
        if(!BluetoothManager.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_ENABLE)
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_COARSE_LOCATION)
            }
            _testBluetooth()
        }
    }
    private fun _testBluetooth() {
        reciever = BluetoothManager.getNearbyDevices(10) { result ->
            Log.d(TAG_NAME, "Done! ${result}")
            result.onSuccess { deviceList ->
                nearbyBluetoothDevices.clear()
                for (device in deviceList) {
                    nearbyBluetoothDevices.add(device)
                }
                _devicesListView.adapter =
                    ArrayAdapter<BluetoothDeviceData>(this, android.R.layout.simple_list_item_1, nearbyBluetoothDevices)
            }
            result.onFailure { error ->
                Log.e("ERROR", error.localizedMessage)
            }
            unregisterReceiver(reciever)
        }
        registerReceiver(reciever, BluetoothManager.getIntentFilter())
    }

}