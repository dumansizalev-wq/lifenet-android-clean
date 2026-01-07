package net.lifenet.core.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.lifenet.core.R
import java.text.SimpleDateFormat
import java.util.*


class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtContactName)
        val txtLastMessage: TextView = view.findViewById(R.id.txtLastMessage)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val badgeUnread: TextView = view.findViewById(R.id.badgeUnread)
        val statusIcon: android.widget.ImageView = view.findViewById(R.id.statusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conv = conversations[position]
        holder.txtName.text = conv.contactName
        holder.txtLastMessage.text = conv.lastMessage
        holder.txtTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conv.timestamp))
        
        if (conv.unreadCount > 0) {
            holder.badgeUnread.visibility = View.VISIBLE
            holder.badgeUnread.text = conv.unreadCount.toString()
        } else {
            holder.badgeUnread.visibility = View.GONE
        }

        if (conv.isSentByMe) {
            holder.statusIcon.visibility = View.VISIBLE
            val iconRes = when (conv.deliveryStatus) {
                net.lifenet.core.data.DeliveryStatus.READ -> android.R.drawable.presence_online
                net.lifenet.core.data.DeliveryStatus.DELIVERED -> android.R.drawable.checkbox_on_background
                else -> android.R.drawable.ic_menu_send
            }
            holder.statusIcon.setImageResource(iconRes)
        } else {
            holder.statusIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(conv) }
    }

    override fun getItemCount() = conversations.size
}
