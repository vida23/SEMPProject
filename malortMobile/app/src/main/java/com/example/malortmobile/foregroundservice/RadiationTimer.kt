package com.example.malortmobile.foregroundservice

import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.malortmobile.R
import kotlin.math.roundToLong


class RadiationTimer(hazmatOn:Boolean, radiationLevel: Float, activity: AppCompatActivity) :Thread() {

    companion object {
        const val MAX_RADIATION_UNITS = 500000
        const val HAZMAT_ON_VALUE = 5f
        const val CHANNEL_ID = "ForegroundService"
        const val CLOCKCONSTANT : Long = 1000
    }

    private var _currentRad: Float? = null
    private var _hazmatOn: Boolean? = hazmatOn
    private var _originalTimeTilEnd: Long? = null
    private var _secondsUntilFinished: Long = 0
    private var _radiationClock: RadiationClock? = null
    private var _activity: AppCompatActivity = activity

    init {
        var radiationPerSec: Float? = null

        when (_hazmatOn){
            true -> radiationPerSec = radiationLevel / HAZMAT_ON_VALUE
            false -> radiationPerSec = radiationLevel
        }
        Log.d("TIME",radiationPerSec.toString())
        _currentRad = radiationPerSec
        _originalTimeTilEnd = (MAX_RADIATION_UNITS / radiationPerSec!!).toLong()
        _secondsUntilFinished = (MAX_RADIATION_UNITS / radiationPerSec).toLong()
        _radiationClock = RadiationClock()
    }

    override fun run() {
        super.run()
        _radiationClock?.start()
    }
    fun setRadiation(radiation: Float) {
        _currentRad = radiation
    }

    fun setHazmat(equipped: Boolean) {
        _hazmatOn = equipped
    }

    fun getRadiation(): Float? {
        return _currentRad
    }

    fun getTimeLeft(): Long {
        return _secondsUntilFinished
    }

    fun getHazmat(): Boolean? {
        return _hazmatOn
    }
    fun stopClock(){
        _radiationClock?.cancel()
    }

    inner class RadiationClock : CountDownTimer(_originalTimeTilEnd!!* CLOCKCONSTANT, CLOCKCONSTANT){

        override fun onTick(millisUntilFinished: Long) {
            _secondsUntilFinished = millisUntilFinished / CLOCKCONSTANT
            val minutesLeft = (_secondsUntilFinished%3600) / WarningService.SECONDS
            val hoursLeft = _secondsUntilFinished / 3600
            val secondsLeft = _secondsUntilFinished% WarningService.SECONDS
            val textView: TextView = _activity.findViewById(R.id.timeLeft)
            textView.setText("Hours: " + hoursLeft.toString()+ " Minutes: " + minutesLeft.toString()+ " Seconds: " + secondsLeft.toString()+ " until you need to leave")


            var shallStartService = when (_secondsUntilFinished){
                (_originalTimeTilEnd!!*0.75).roundToLong() -> true

                (_originalTimeTilEnd!!*0.5).roundToLong() -> true

                (_originalTimeTilEnd!!*0.25).roundToLong()-> true

                else -> false
            }

            when (shallStartService){
                true -> {
                    val intent = Intent(_activity,WarningService::class.java).apply {
                        putExtra(WarningService.EXTRA_SECONDS,_secondsUntilFinished)
                    }
                    _activity.startService(intent)
                }
            }
        }

        override fun onFinish() {
            val intent = Intent(_activity,WarningService::class.java).apply {
                putExtra(WarningService.EXTRA_SECONDS,0.toLong())
            }
            _activity.startService(intent)
        }
    }
}