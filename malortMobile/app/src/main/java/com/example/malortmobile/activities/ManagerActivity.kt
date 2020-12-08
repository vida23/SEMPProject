package com.example.malortmobile.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.malortmobile.R
import com.example.malortmobile.RecyclerViewHistoryAdapter
import com.example.malortmobile.database.ClockEventData
import com.example.malortmobile.database.ClockEventManager
import com.example.malortmobile.database.UserAuthentication
import kotlinx.android.synthetic.main.activity_manager.*

class ManagerActivity : AppCompatActivity() {
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _viewAdapter: RecyclerView.Adapter<*>
    private lateinit var _viewManager: RecyclerView.LayoutManager
    private val _userAuth: UserAuthentication = UserAuthentication()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)
        var dataset : List<ClockEventData>

        ClockEventManager().getAll { result ->
            result.onSuccess { clockEvents ->
                dataset = clockEvents
                _viewManager = LinearLayoutManager(this)
                _viewAdapter = RecyclerViewHistoryAdapter(dataset)
                _recyclerView =
                    findViewById<RecyclerView>(R.id.historyRecyclerView).apply {
                        setHasFixedSize(true)

                        layoutManager = _viewManager

                        adapter = _viewAdapter
                    }
            }
            result.onFailure {
                Toast.makeText(this,"Failed to fetch",Toast.LENGTH_SHORT).show()
            }
        }

        val buttonLogout = buttonLogoutManager
        buttonLogout.setOnClickListener {
            _userAuth.logoutUser()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}