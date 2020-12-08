package com.example.malortmobile.database

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class UserAuthentication() {
    private val _authentication = Firebase.auth

    fun loginUser(userEmail:String, userPassword:String,callback: (Result<FirebaseUser>) -> Unit) {
        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            callback.invoke(Result.failure(Exception("Empty email or password")))
        } else {
            _authentication.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnSuccessListener { authResult ->
                    callback.invoke(Result.success(authResult.user!!))
                    Log.d("TAG","Successfully signed in")
                }
                .addOnFailureListener { authError ->
                    Log.d("TAG","Signed in failed")
                    callback.invoke(Result.failure(authError))
                }
        }
    }

    fun logoutUser(){
        _authentication.signOut()
        Log.d("TAG","Signed out")
    }


}