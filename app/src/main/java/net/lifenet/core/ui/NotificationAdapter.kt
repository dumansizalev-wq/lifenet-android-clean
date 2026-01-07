package net.lifenet.core.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.lifenet.core.R

data class Notification(
    val title: String,
    val message: String,
    val time: String,
    val icon: String
)

class NotificationAdapter(private val notifications: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.notificationTitle)
        val txtMessage: TextView = view.findViewById(R.id.notificationMessage)
        val txtTime: TextView = view.findViewById(R.id.notificationTime)
        val txtIcon: TextView = view.findViewById(R.id.notificationIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.txtTitle.text = notification.title
        holder.txtMessage.text = notification.message
        holder.txtTime.text = notification.time
        holder.txtIcon.text = notification.icon
    }

    override fun getItemCount() = notifications.size
}
