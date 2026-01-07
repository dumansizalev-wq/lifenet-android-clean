package net.lifenet.core.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.lifenet.core.R

data class NearbyDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: String // "BLE" or "Wi-Fi"
)

class DeviceAdapter(private val devices: List<NearbyDevice>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtDetails: TextView = view.findViewById(R.id.txtDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.txtName.text = device.name
        holder.txtDetails.text = "${device.type} | RSSI: ${device.rssi} dBm | ${device.address}"
    }

    override fun getItemCount() = devices.size
}
