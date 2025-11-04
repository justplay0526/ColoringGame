package com.justplay.coloringgame.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.justplay.coloringgame.databinding.ItemPaletteBinding

class PaletteAdapter: ListAdapter<Int, ViewHolder>(ListDiffUtil) {
    var onItemClick: ((Int) -> Unit)? = null
    private class ListViewHolder(
        val binding: ItemPaletteBinding
    ): ViewHolder(binding.root) {
        fun bind(
            item: Int,
            onItemClick: ((Int) -> Unit)?
        ) = with(binding) {
            imgPalette.setBackgroundColor(item)

            root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
        companion object {
            fun from(parent: ViewGroup) = ListViewHolder(
                ItemPaletteBinding.inflate(
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
            is ListViewHolder -> holder.bind(item, onItemClick)
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