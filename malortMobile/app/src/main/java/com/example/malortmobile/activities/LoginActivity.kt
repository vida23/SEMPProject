package com.example.malortmobile.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.malortmobile.R
import com.example.malortmobile.database.UserAuthentication
import kotlinx.android.synthetic.main.activity_login_acitivty.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_acitivty)
        val buttonLogin = buttonLoginManager
        val auth = UserAuthentication()
        buttonLogin.setOnClickListener {
            val username = managerUsername.text.toString()
            val password = managerPassword.text.toString()
            auth.loginUser(username, password) {result ->
                result.onSuccess {
                    val intent = Intent(this,ManagerActivity::class.java)
                    startActivity(intent)
                }
                result.onFailure {
                    Toast.makeText(this,"Login failed",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}