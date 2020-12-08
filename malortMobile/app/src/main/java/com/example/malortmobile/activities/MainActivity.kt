package com.example.malortmobile.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.malortmobile.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonLoginManager= buttonManager
        buttonLoginManager.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val buttonTechnician = buttonTechnician
        buttonTechnician.setOnClickListener {
            val intent = Intent(this, PairingActivity::class.java)
            startActivity(intent)
        }
    }
}
