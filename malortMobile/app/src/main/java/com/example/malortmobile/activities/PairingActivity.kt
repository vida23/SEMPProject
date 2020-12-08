package com.example.malortmobile.activities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.malortmobile.R
import com.example.malortmobile.bluetooth.BluetoothConnectionListener
import com.example.malortmobile.bluetooth.BluetoothManager
import com.example.malortmobile.data.ClockInformationData
import com.example.malortmobile.database.ClockEventManager
import com.example.malortmobile.utilities.ByteArrayParser
import kotlinx.android.synthetic.main.activity_pairing.*
import java.io.IOException
import java.lang.Exception

class PairingActivity : AppCompatActivity() {
    companion object{
        const val SEARCH_TIMEOUT : Long = 4
        //test "18:CF:5E:AC:FF:C5" safe "98:D3:32:31:0A:8A"
        const val MAC_ADDRESS_SAFETY_CONSOLE = "98:D3:32:31:0A:8A"
        const val TAG_NAME                   = "BT"
        const val CLOCK_EVENT_HAZMAT         = "Clock event hazmat"
        const val CLOCK_EVENT_USERID         = "Clock event user id"
        const val CLOCK_EVENT_RADIATION      = "Clock event radiation"
        const val CLOCK_EVENT_CLOCKED_IN     = "Clock event clocked in"
        const val REQUEST_ON_TECHNICIAN_FINISH = 23
    }
    private var _receiver : BroadcastReceiver? = null

    private fun _tryToConnectToDevices() {
        if(_receiver != null) {
            unregisterReceiver(_receiver)
        }

        _receiver = BluetoothManager.getNearbyDevices(SEARCH_TIMEOUT) { result ->
            result.onSuccess { devices->
                var deviceFound = false
                for(device in devices) {
                    if(device.adress == MAC_ADDRESS_SAFETY_CONSOLE) {
                        deviceFound = true
                        BluetoothManager.connectToDevice(MAC_ADDRESS_SAFETY_CONSOLE) { connectionAttempt ->
                            connectionAttempt.onSuccess {
                                _setStatusText("Connecting to safety console")
                            }
                            connectionAttempt.onFailure {
                                Toast.makeText(this,"Failed the attempt to connect to device",Toast.LENGTH_SHORT).show()
                            }
                        }
                        break
                    }
                }
                if(!deviceFound) {
                    _tryToConnectToDevices()
                }
            }
            result.onFailure {
                Toast.makeText(this,"Did not search at all. Do this device support bluetooth?",Toast.LENGTH_SHORT).show()
            }
        }
        registerReceiver(_receiver,BluetoothManager.getIntentFilter())
    }

    private fun _attemptConnect(view: View?) {
        Log.d(DebugBluetooth.TAG_NAME, "Bluetooth is on? ${BluetoothManager.isBluetoothEnabled()}")
        if(!BluetoothManager.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, DebugBluetooth.REQUEST_CODE_BLUETOOTH_ENABLE)
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    DebugBluetooth.REQUEST_CODE_COARSE_LOCATION
                )
            }
            _tryToConnectToDevices()
        }
    }

    private fun _setStatusText(newText:String) {
        findViewById<ProgressBar>(R.id.ProgressBarSearching).visibility = View.GONE
        findViewById<TextView>(R.id.TextViewWaitForTag).text = newText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing)

        BluetoothManager.setListener( object : BluetoothConnectionListener {
            override fun onConnect(address: String, name: String?) {
                runOnUiThread {
                    _setStatusText("Waiting for tag...")
                }
            }

            override fun onDisconnect() {}

            override fun onRetrievedData(data: ByteArray) {
                val clockEvent: ClockInformationData? =
                    ByteArrayParser.fromByteArrayToDataClass(data)

                runOnUiThread {
                    if (clockEvent != null) {
                        if(clockEvent.clockedIn!!) {
                            val time = com.google.firebase.Timestamp.now()
                            ClockEventManager().addClockEvent(
                                clockEvent.userId!!,
                                clockEvent.radiation!!,
                                time,
                                clockEvent.clockedIn!!
                            ) { result ->
                                result.onSuccess {
                                    val intent =
                                        Intent(baseContext, TechnicianActivity::class.java).apply {
                                            putExtra(CLOCK_EVENT_USERID, clockEvent.userId)
                                            putExtra(CLOCK_EVENT_RADIATION, clockEvent.radiation!!)
                                            putExtra(CLOCK_EVENT_HAZMAT, clockEvent.hazmat)
                                            putExtra(CLOCK_EVENT_CLOCKED_IN, clockEvent.clockedIn!!)
                                        }

                                    startActivityForResult(intent, REQUEST_ON_TECHNICIAN_FINISH)
                                }
                                result.onFailure {
                                    Log.e(TAG_NAME, "Didn't Send any data")
                                }
                            }
                        }
                    }
                }
            }
            override fun onError(exception: Exception) {
                runOnUiThread {
                    _tryToConnectToDevices()
                }
            }
        })

        _attemptConnect(null)
    }

    override fun onPause() {
        if(_receiver != null) {
            unregisterReceiver(_receiver)
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            DebugBluetooth.REQUEST_CODE_BLUETOOTH_ENABLE -> {
                if(resultCode == Activity.RESULT_OK) {
                    _tryToConnectToDevices()
                }
            }
            DebugBluetooth.REQUEST_CODE_COARSE_LOCATION -> {
                if(resultCode == Activity.RESULT_OK) {
                    _tryToConnectToDevices()
                }
            }
            REQUEST_ON_TECHNICIAN_FINISH -> {
                finish()
            }
        }
    }
}