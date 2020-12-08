package com.example.malortmobile.bluetooth

import java.lang.Exception

interface BluetoothConnectionListener {
    fun onConnect(address:String, name:String?)
    fun onDisconnect()
    fun onError(exception: Exception)
    fun onRetrievedData(data: ByteArray)
}