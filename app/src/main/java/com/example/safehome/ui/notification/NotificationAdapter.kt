package com.example.safehome.ui.notification

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.safehome.R

class NotificationAdapter(
    private val context: Context,
    private val notifications: List<NotificationItem>
) : BaseAdapter() {

    override fun getCount(): Int = notifications.size

    override fun getItem(position: Int): NotificationItem = notifications[position]

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view = if (convertView == null) {
            val inflatedView = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false)
            holder = ViewHolder(inflatedView)
            inflatedView.tag = holder
            inflatedView
        } else {
            holder = convertView.tag as ViewHolder
            convertView
        }

        holder.bind(getItem(position))
        return view
    }

    private class ViewHolder(view: View) {
        private val txtTitle: TextView = view.findViewById(R.id.txtNotificationTitle)
        private val txtMessage: TextView = view.findViewById(R.id.txtNotificationMessage)
        private val txtType: TextView = view.findViewById(R.id.txtNotificationType)
        private val txtCreatedAt: TextView = view.findViewById(R.id.txtNotificationCreatedAt)
        private val txtReadState: TextView = view.findViewById(R.id.txtNotificationReadState)

        fun bind(item: NotificationItem) {
            txtTitle.text = item.title
            txtMessage.text = item.message
            txtType.text = item.type.name
            txtCreatedAt.text = item.createdAt
            txtReadState.text = if (item.isRead) "Da doc" else "Chua doc"

            val colors = typeColors(item.type)
            txtType.setTextColor(colors.textColor)
            txtType.setBackgroundColor(colors.backgroundColor)
            txtReadState.alpha = if (item.isRead) 0.72f else 1f
        }

        private fun typeColors(type: NotificationType): TypeColors {
            return when (type) {
                NotificationType.INFO -> TypeColors(
                    textColor = Color.parseColor("#2563EB"),
                    backgroundColor = Color.parseColor("#EAF2FF")
                )

                NotificationType.WARNING -> TypeColors(
                    textColor = Color.parseColor("#B45309"),
                    backgroundColor = Color.parseColor("#FEF3C7")
                )

                NotificationType.DANGER -> TypeColors(
                    textColor = Color.parseColor("#B91C1C"),
                    backgroundColor = Color.parseColor("#FEE2E2")
                )
            }
        }
    }

    private data class TypeColors(
        val textColor: Int,
        val backgroundColor: Int
    )
}

data class NotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: String
)

enum class NotificationType {
    INFO,
    WARNING,
    DANGER
}
