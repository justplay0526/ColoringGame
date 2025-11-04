package com.justplay.coloringgame.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.justplay.coloringgame.R
import com.justplay.coloringgame.adapter.ColoringListAdapter
import com.justplay.coloringgame.databinding.FragmentColoringListBinding

class ColoringListFragment : Fragment() {
    private val binding by lazy { FragmentColoringListBinding.inflate(layoutInflater) }
    private val outlineRes = intArrayOf(R.raw.outline, R.raw.outline_other, R.raw.outline_44)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ColoringListAdapter()
        binding.rvList.adapter = adapter
        adapter.submitList(outlineRes.toList())
        adapter.onItemClick = {
            val action = ColoringListFragmentDirections.actionColoringListFragmentToColoringPlayFragment(it)
            findNavController().navigate(action)
        }
    }
}