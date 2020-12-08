package com.example.malortmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.malortmobile.database.ClockEventData
import kotlinx.android.synthetic.main.recycler_view_item.view.*

class RecyclerViewHistoryAdapter(private val _dataset:List<ClockEventData>) : RecyclerView.Adapter<RecyclerViewHistoryAdapter.Viewholder>(){

    class Viewholder(v: View) : RecyclerView.ViewHolder(v){
        private var view: View = v

        fun bindInfo(clockEvent : ClockEventData){
            val clockedInOrOutString = when(clockEvent.clockedIn) {
                true ->  " Clocked in "
                else -> " Clocked out "
            }
            view.clockEventTextView.text = "Name: " + clockEvent.userId +", Radiation level: " + clockEvent.radiationLevel + clockedInOrOutString +
                " Event time: " + clockEvent.eventTime?.toDate()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ):Viewholder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item,parent,false)
        return Viewholder(view)
    }
    override fun onBindViewHolder(holder:Viewholder, position: Int) {
        holder.bindInfo(_dataset[position])
    }
    override fun getItemCount(): Int {
        return _dataset.size
    }


}