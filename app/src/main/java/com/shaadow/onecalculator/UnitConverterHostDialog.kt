package com.shaadow.onecalculator

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.shaadow.onecalculator.calculators.converters.*

class UnitConverterHostDialog : DialogFragment() {
    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String): UnitConverterHostDialog {
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            val dialog = UnitConverterHostDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_unit_converter_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString(ARG_TYPE) ?: return
        val fragment: Fragment = when (type) {
            "Length Converter" -> LengthConverterFragment()
            "Weight Converter" -> WeightConverterFragment()
            "Temperature Converter" -> TemperatureConverterFragment()
            "Area Converter" -> AreaConverterFragment()
            "Volume Converter" -> VolumeConverterFragment()
            "Speed Converter" -> SpeedConverterFragment()
            else -> LengthConverterFragment()
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.unit_converter_fragment_container, fragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
