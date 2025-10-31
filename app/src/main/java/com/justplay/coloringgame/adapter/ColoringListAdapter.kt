package com.justplay.coloringgame.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.justplay.coloringgame.R
import com.justplay.coloringgame.databinding.ItemStageBinding

class ColoringListAdapter: ListAdapter<Int, ViewHolder>(ListDiffUtil) {
    var onItemClick: ((Int) -> Unit)? = null
    private class ListViewHolder(
        val binding: ItemStageBinding
    ): ViewHolder(binding.root) {
        fun bind(
            item: Int,
            position: Int,
            onItemClick: ((Int) -> Unit)?
        ) = with(binding) {
            tvTitle.text = String.format(
                root.context.getString(R.string.title_stage) + "%d", position + 1
            )
            imgStage.setImageResource(item)

            root.setOnClickListener {
                onItemClick?.invoke(position)
            }
        }
        companion object {
            fun from(parent: ViewGroup) = ListViewHolder(
                ItemStageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
            )
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ListViewHolder.from(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        when(holder) {
            is ListViewHolder -> holder.bind(item, position, onItemClick)
        }
    }

    companion object {
        private val ListDiffUtil = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(
                oldItem: Int,
                newItem: Int
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: Int,
                newItem: Int
            ): Boolean = oldItem == newItem
        }
    }
}