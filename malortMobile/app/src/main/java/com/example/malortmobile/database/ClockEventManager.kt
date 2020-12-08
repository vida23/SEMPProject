package com.example.malortmobile.database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ClockEventManager {
    companion object {
        const val COLLECTION_PATH = "clock_event"

        const val ATTRIBUTE_USER_ID = "user_id"
        const val ATTRIBUTE_EVENT_TIME = "event_time"
        const val ATTRIBUTE_RADIATION_LEVEL = "radiation_level"
        const val ATTRIBUTE_CLOCKED_IN = "clocked_in"
    }

    val _dbInstance = FirebaseFirestore.getInstance()

    fun addClockEvent(userId:String, radiationLevel:Float, currentTime:Timestamp, clockedIn:Boolean, callback: (Result<DocumentReference>) -> Unit) {
        val clockOutDataMap = mapOf (
            ATTRIBUTE_USER_ID to userId,
            ATTRIBUTE_EVENT_TIME to currentTime,
            ATTRIBUTE_RADIATION_LEVEL to radiationLevel,
            ATTRIBUTE_CLOCKED_IN to clockedIn
        )
        _dbInstance.collection(COLLECTION_PATH)
            .add(clockOutDataMap)
            .addOnSuccessListener { clockInReference ->
                callback.invoke(Result.success(clockInReference))
            }
            .addOnFailureListener { exception ->
                callback.invoke(Result.failure(exception))
            }
    }
    fun getAll(callback: (Result<List<ClockEventData>>) -> Unit){
        _dbInstance.collection(COLLECTION_PATH)
            .get()
            .addOnSuccessListener { clockEvents ->
                callback.invoke(Result.success(_getParsedList(clockEvents)))
            }
            .addOnFailureListener { exception ->
                callback.invoke(Result.failure(exception))
            }
    }
    fun getAllByUserId(userId:String, callback: (Result<List<ClockEventData>>) -> Unit) {
        _dbInstance.collection(COLLECTION_PATH)
            .whereEqualTo(ATTRIBUTE_USER_ID, userId)
            .get()
            .addOnSuccessListener { clockEvents ->
                callback.invoke(Result.success(_getParsedList(clockEvents)))
            }
            .addOnFailureListener { exception ->
                callback.invoke(Result.failure(exception))
            }
    }
    private fun _getParsedList(retrievedSnapshots: QuerySnapshot) : MutableList<ClockEventData> {
        val returningList = mutableListOf<ClockEventData>()
        //Parses every snapshot to data class
        for (clockEvent in retrievedSnapshots) {
            returningList.add (
                ClockEventData(
                    clockEvent.getString(ATTRIBUTE_USER_ID),
                    clockEvent.getLong(ATTRIBUTE_RADIATION_LEVEL),
                    clockEvent.getBoolean(ATTRIBUTE_CLOCKED_IN),
                    clockEvent.getTimestamp(ATTRIBUTE_EVENT_TIME)
                )
            )
        }
        return returningList
    }
}