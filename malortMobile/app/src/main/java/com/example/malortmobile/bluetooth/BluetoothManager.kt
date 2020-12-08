package com.example.malortmobile.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import com.example.malortmobile.data.BluetoothDeviceData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*

object BluetoothManager{
    //Constants
    const val BYTE_ARRAY_SIZE = 16
    private const val UUID_SAFETY_CONSOLE_STRING = "00001101-0000-1000-8000-00805f9b34fb"

    private var _connectionListener:BluetoothConnectionListener? = null

    class ConnectThread(device: BluetoothDevice) : Thread() {
        private val _clientSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(_connectionUUID)
        }

        private val _inStream : InputStream  = _clientSocket!!.inputStream
        private val _outStream: OutputStream = _clientSocket!!.outputStream
        private val _buffer: ByteArray = ByteArray(BYTE_ARRAY_SIZE)

        private val _partialBuffer = mutableListOf<Byte>()

        override fun run() {
            _bluetoothAdapter?.cancelDiscovery()
            _clientSocket.use { socket ->
                try {
                    socket?.connect()
                    _connectionListener?.onConnect(socket?.remoteDevice?.address ?: "Unknown", socket?.remoteDevice?.name)

                    while(true) {
                        _inStream.read(_buffer)
                        //Send the read to the bytearray converter
                        if(_partialBuffer.count() == 0) {
                            _partialBuffer.add(_buffer[0])
                        }
                        else if (_partialBuffer.count() > 0) {
                            for (byteIndex in 0 until BYTE_ARRAY_SIZE) {
                                if(_buffer[byteIndex] != 0.toByte()) {
                                    _partialBuffer.add(_buffer[byteIndex])
                                }
                                if(_buffer[byteIndex] == (-1).toByte()) {
                                    break
                                }
                            }
                        }
                        if(_partialBuffer.count() == BYTE_ARRAY_SIZE) {
                            val byteArray = ByteArray(BYTE_ARRAY_SIZE)

                            for((index, byte) in _partialBuffer.withIndex()) {
                                byteArray[index] = byte
                            }

                            _connectionListener?.onRetrievedData(byteArray)
                        }
                        if (_partialBuffer.count() >= BYTE_ARRAY_SIZE) {
                            _partialBuffer.clear()
                        }
                    }
                } catch (socketConnectError: IOException) {
                    _connectionListener?.onError(socketConnectError)
                    _connectionListener?.onDisconnect()
                }
            }

        }
        fun write(bytes: ByteArray){
            try {
                _outStream.write(bytes)
            } catch (streamWriteError : IOException) {
                _connectionListener?.onError(streamWriteError)
            }
        }
        fun cancel() {
            try {
                _clientSocket?.close()
                _connectionListener?.onDisconnect()
            } catch (socketCancelError: IOException) {
                _connectionListener?.onError(socketCancelError)
            }
        }
    }

    private var _currentReceiver:BroadcastReceiver? = null
    private val _bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val _connectionUUID: UUID? = UUID.fromString(UUID_SAFETY_CONSOLE_STRING)

    private var _currentConnectThread : ConnectThread? = null

    private val _nearbyDevices = mutableListOf<BluetoothDeviceData>()

    fun isBluetoothEnabled(): Boolean {
        return _bluetoothAdapter?.isEnabled ?: false
    }
    fun setListener(listener: BluetoothConnectionListener) {
        _connectionListener = listener
    }
    fun getNearbyDevices(timeOutSeconds:Long, callback: (Result<List<BluetoothDeviceData>>) -> Unit) : BroadcastReceiver {
        _bluetoothAdapter?.startDiscovery() ?: callback.invoke(Result.failure(Exception("Device does not support bluetooth")))
        _nearbyDevices.clear()

        _currentReceiver =  object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                         _nearbyDevices.add(
                            BluetoothDeviceData(
                                device.name,
                                device.address
                            )
                        )
                    }
                }
            }
        }
        Handler().postDelayed({
            _bluetoothAdapter?.cancelDiscovery()
            callback(Result.success(_nearbyDevices))
        }, timeOutSeconds * 1000)

        return _currentReceiver as BroadcastReceiver
    }

    //Start the search for connections
    fun connectToDevice(address:String, callback: (Result<BluetoothDeviceData>) -> Unit) {
        val device = _bluetoothAdapter?.getRemoteDevice(address)

        if (device == null) {
            callback.invoke(Result.failure(Exception("Device does not support bluetooth")))
        } else {
            _currentConnectThread = ConnectThread(
                device
            ).apply {
                this.start()
            }
            callback.invoke (
                Result.success(
                    BluetoothDeviceData(
                        device.name,
                        device.address
                    )
                )
            )
        }
    }
    fun getIntentFilter(): IntentFilter {
        return IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        }
    }
    fun disconnectCurrentDevice() {
        _currentConnectThread?.cancel()
    }
    fun writeToCurrentDevice(message: ByteArray) {
        _currentConnectThread?.write(message)
    }
}