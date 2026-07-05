package com.example.safehome.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R

class NotificationAdapter(
    private val onNotificationClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val notifications = mutableListOf<NotificationItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position], onNotificationClick)
    }

    override fun getItemCount(): Int = notifications.size

    fun submitNotifications(items: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(items)
        notifyDataSetChanged()
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconType: ImageView = itemView.findViewById(R.id.imgNotificationType)
        private val txtBadge: TextView = itemView.findViewById(R.id.txtNotificationBadge)
        private val unreadDot: View = itemView.findViewById(R.id.viewUnreadDot)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtNotificationTitle)
        private val txtMessage: TextView = itemView.findViewById(R.id.txtNotificationMessage)
        private val txtCreatedAt: TextView = itemView.findViewById(R.id.txtNotificationCreatedAt)

        fun bind(item: NotificationItem, onNotificationClick: (NotificationItem) -> Unit) {
            txtTitle.text = item.title
            txtMessage.text = item.body
            txtCreatedAt.text = item.createdAt
            unreadDot.visibility = if (item.isRead) View.INVISIBLE else View.VISIBLE
            itemView.setOnClickListener {
                onNotificationClick(item)
            }

            when (item.type) {
                NotificationType.INFO -> {
                    iconType.setImageResource(R.drawable.ic_notification_info)
                    iconType.setBackgroundResource(R.drawable.bg_notification_icon_info)
                    txtBadge.text = "INFO"
                    txtBadge.setTextColor(itemView.context.getColor(R.color.safehome_blue_deep))
                    txtBadge.setBackgroundResource(R.drawable.bg_notification_badge_info)
                }

                NotificationType.WARNING -> {
                    iconType.setImageResource(R.drawable.ic_notification_warning)
                    iconType.setBackgroundResource(R.drawable.bg_notification_icon_warning)
                    txtBadge.text = "CẢNH BÁO"
                    txtBadge.setTextColor(itemView.context.getColor(R.color.safehome_warning_text))
                    txtBadge.setBackgroundResource(R.drawable.bg_notification_badge_warning)
                }

                NotificationType.DANGER -> {
                    iconType.setImageResource(R.drawable.ic_notification_danger)
                    iconType.setBackgroundResource(R.drawable.bg_notification_icon_danger)
                    txtBadge.text = "NGUY HIỂM"
                    txtBadge.setTextColor(itemView.context.getColor(R.color.safehome_danger_text))
                    txtBadge.setBackgroundResource(R.drawable.bg_notification_badge_danger)
                }
            }
        }
    }
}

data class NotificationItem(
    val id: Long?,
    val title: String,
    val body: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: String
)

enum class NotificationType {
    INFO,
    WARNING,
    DANGER;

    companion object {
        fun from(value: String?): NotificationType {
            return when (value?.uppercase()) {
                "WARNING" -> WARNING
                "DANGER" -> DANGER
                else -> INFO
            }
        }
    }
}
