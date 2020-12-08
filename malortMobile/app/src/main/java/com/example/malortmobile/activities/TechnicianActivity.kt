package com.example.malortmobile.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.malortmobile.R
import com.example.malortmobile.bluetooth.BluetoothConnectionListener
import com.example.malortmobile.bluetooth.BluetoothManager
import com.example.malortmobile.foregroundservice.RadiationTimer
import com.example.malortmobile.utilities.ByteArrayParser
import kotlinx.android.synthetic.main.activity_technician.*
import java.lang.Exception

class TechnicianActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technician)

        val hazmat = intent.getBooleanExtra(PairingActivity.CLOCK_EVENT_HAZMAT,false)
        val radiationLevel = intent.getFloatExtra(PairingActivity.CLOCK_EVENT_RADIATION,0f)
        val userId = intent.getStringExtra(PairingActivity.CLOCK_EVENT_USERID)
        val clockedIn = intent.getBooleanExtra(PairingActivity.CLOCK_EVENT_CLOCKED_IN,false)
        val radiationTimer = RadiationTimer(hazmat,radiationLevel,this)

        if(clockedIn){
            radiationTimer.run()
        }
        val radiationPerSec = radiationTimer.getRadiation()
        totalRadiation.text  = "Radiation per second: $radiationPerSec"

        BluetoothManager.setListener(object : BluetoothConnectionListener{
            override fun onConnect(address: String, name: String?) {}

            override fun onDisconnect() {
                finish()
            }

            override fun onRetrievedData(data: ByteArray) {
                val clockEvent = ByteArrayParser.fromByteArrayToDataClass(data)
                if(clockEvent != null){
                   if(!clockEvent.clockedIn!!){
                       _leaveActivity(Activity.RESULT_OK)
                   }
                }
            }

            override fun onError(exception: Exception) {
                runOnUiThread {
                    finish()
                }
            }
        })
    }

    override fun onBackPressed() {
        _leaveActivity(Activity.RESULT_OK)
    }

    private fun _leaveActivity(result:Int) {
        BluetoothManager.disconnectCurrentDevice()
        setResult(result)
    }
}