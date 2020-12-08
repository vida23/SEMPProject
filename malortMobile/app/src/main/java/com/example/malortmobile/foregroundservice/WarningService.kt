package com.example.malortmobile.foregroundservice

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.malortmobile.R
import com.example.malortmobile.activities.MainActivity

class WarningService : Service() {
    companion object{
        const val SECONDS = 60
        const val SERVICE_ID = 523578
        const val EXTRA_SECONDS = "seconds"
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val secondsTilEnd = intent!!.getLongExtra(EXTRA_SECONDS, SECONDS.toLong())
        var timeTilEnd: Long = 0
        var minOrSec = ""

        if(secondsTilEnd in 1..SECONDS){
            timeTilEnd = secondsTilEnd
            minOrSec = "Second(s) left"
        }else if(secondsTilEnd.toInt() == 0){
            timeTilEnd = 0
            minOrSec = "Second left please leave the building"
        }else{
            timeTilEnd = secondsTilEnd / 60
            minOrSec = "Minute(s) left"
        }

        _createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java) //Main or Technician??
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification: Notification = NotificationCompat.Builder(
            this,
            RadiationTimer.CHANNEL_ID
        )
            .setContentTitle("Warning")
            .setContentText("You have $timeTilEnd $minOrSec")
            .setChannelId(RadiationTimer.CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_android_black_24dp)
            .build()

        startForeground(SERVICE_ID, notification)

        return START_NOT_STICKY
    }

    private fun _createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                RadiationTimer.CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}