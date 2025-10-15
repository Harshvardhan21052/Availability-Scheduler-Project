package com.scheduler.app.ui.availability

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scheduler.app.data.model.BusySlotResponse
import com.scheduler.app.databinding.ItemBusySlotBinding

class BusySlotAdapter(
    private val onEdit: (BusySlotResponse) -> Unit,
    private val onDelete: (BusySlotResponse) -> Unit
) : ListAdapter<BusySlotResponse, BusySlotAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemBusySlotBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(slot: BusySlotResponse) {
            binding.tvDate.text      = slot.date
            binding.tvTimeRange.text = "${slot.startTime} – ${slot.endTime}"
            binding.btnEdit.setOnClickListener   { onEdit(slot) }
            binding.btnDelete.setOnClickListener { onDelete(slot) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBusySlotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<BusySlotResponse>() {
        override fun areItemsTheSame(old: BusySlotResponse, new: BusySlotResponse) =
            old.id == new.id
        override fun areContentsTheSame(old: BusySlotResponse, new: BusySlotResponse) =
            old == new
    }
}
