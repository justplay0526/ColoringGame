package com.justplay.coloringgame.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.justplay.coloringgame.R
import com.justplay.coloringgame.databinding.FragmentColoringPlayBinding

class ColoringPlayFragment : Fragment() {
    private val binding by lazy { FragmentColoringPlayBinding.inflate(layoutInflater) }
    private val args : ColoringPlayFragmentArgs by navArgs()
    private val cv by lazy { binding.coloringView }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stage = args.stage

        binding.mtToolbar.apply {
            title = getString(R.string.title_stage) + "%d".format(stage + 1)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        val outlineResource = intArrayOf(R.raw.outline, R.raw.outline_other, R.raw.outline_44)
        val regionIdMapResource = intArrayOf(R.raw.region_id_map, R.raw.region_id_map_other, R.raw.region_id_map_44)

        val outline = loadBitmap(outlineResource[stage])
        val idMap = loadBitmap(regionIdMapResource[stage])
        cv.setBitmaps(outline, idMap)

        // Build simple palette
        val palette: LinearLayout = binding.palette
        val colors = listOf(
            "#F94144".toColorInt(),
            "#F3722C".toColorInt(),
            "#F9C74F".toColorInt(),
            "#90BE6D".toColorInt(),
            "#577590".toColorInt(),
            "#9B5DE5".toColorInt()
        )
        colors.forEach { col ->
            val swatch = View(requireContext()).apply {
                setBackgroundColor(col)
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { setMargins(8,8,8,8) }
                setOnClickListener { cv.setSelectedColor(col) }
                contentDescription = "color"
            }
            palette.addView(swatch)
        }
    }

    private fun loadBitmap(resId: Int): Bitmap {
        val opt = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(resources, resId, opt)
    }
}