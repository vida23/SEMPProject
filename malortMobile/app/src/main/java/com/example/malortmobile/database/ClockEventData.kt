package com.example.malortmobile.database

import com.google.firebase.Timestamp

data class ClockEventData (
    var userId:String?,
    var radiationLevel:Number?,
    var clockedIn:Boolean?,
    var eventTime:Timestamp?
)