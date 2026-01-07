package net.lifenet.core.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.lifenet.core.R
import net.lifenet.core.data.Message
import net.lifenet.core.data.MessageType
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<Message>,
    private val onVoicePlayClick: (Message) -> Unit = {}
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtContent: TextView = itemView.findViewById(R.id.txtMessageContent)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTimestamp)
        private val btnPlay: ImageButton? = itemView.findViewById(R.id.btnPlayVoice)
        private val txtStatus: TextView? = itemView.findViewById(R.id.txtDeliveryStatus)

        fun bind(message: Message) {
            if (message.messageType == MessageType.PTT) {
                txtContent.text = "🎤 Voice Message"
                btnPlay?.visibility = View.VISIBLE
                btnPlay?.setOnClickListener { onVoicePlayClick(message) }
            } else {
                txtContent.text = message.content
                btnPlay?.visibility = View.GONE
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            txtTime.text = sdf.format(Date(message.timestamp))
            txtStatus?.text = message.deliveryStatus.name
        }
    }
}
