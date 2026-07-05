package com.example.safehome.data

data class SensorItem(
    val id: String,
    val name: String,
    val value: String,
    val status: String,
    val statusColor: String, // e.g. "#22C55E" for green, "#EF4444" for red
    val iconResId: Int,      // e.g. R.drawable.ic_person_safehome
    val iconBgColor: String, // e.g. "#F0F7FF"
    val iconTintColor: String // e.g. "#4F8DF7"
)
