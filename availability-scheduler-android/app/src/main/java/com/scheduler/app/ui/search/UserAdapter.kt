package com.scheduler.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scheduler.app.data.model.UserResponse
import com.scheduler.app.databinding.ItemUserBinding

class UserAdapter : ListAdapter<UserResponse, UserAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemUserBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserResponse) {
            binding.tvUsername.text = "@${user.username}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<UserResponse>() {
        override fun areItemsTheSame(old: UserResponse, new: UserResponse) = old.id == new.id
        override fun areContentsTheSame(old: UserResponse, new: UserResponse) = old == new
    }
}
