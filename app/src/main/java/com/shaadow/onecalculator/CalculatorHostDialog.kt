package com.shaadow.onecalculator

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.shaadow.onecalculator.calculators.algebra.*
import com.shaadow.onecalculator.calculators.geometry.*
import com.shaadow.onecalculator.calculators.finance.*
import com.shaadow.onecalculator.calculators.insurance.*
import com.shaadow.onecalculator.calculators.health.*
import com.shaadow.onecalculator.calculators.datetime.*
import com.shaadow.onecalculator.calculators.converters.*
import com.shaadow.onecalculator.calculators.others.*

class CalculatorHostDialog : DialogFragment() {
    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String): CalculatorHostDialog {
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            val dialog = CalculatorHostDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_calculator_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString(ARG_TYPE) ?: return
        val fragment: Fragment = getFragmentForCalculatorType(type)
        
        childFragmentManager.beginTransaction()
            .replace(R.id.calculator_fragment_container, fragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun getFragmentForCalculatorType(calculatorType: String): Fragment {
        return when (calculatorType) {
            // Algebra
            "Linear Equations" -> DynamicCalculatorFragment.newInstance("algb01lin01")
            "Quadratic Equations" -> DynamicCalculatorFragment.newInstance("algb01qua01")
            "System of Equations" -> DynamicCalculatorFragment.newInstance("algb01sys01")
            "Polynomials" -> DynamicCalculatorFragment.newInstance("algb01pol01")
            "Factoring" -> DynamicCalculatorFragment.newInstance("algb01fac01")
            "Complex Numbers" -> DynamicCalculatorFragment.newInstance("algb01com01")
            
            // Geometry
            "Area Calculator" -> DynamicCalculatorFragment.newInstance("geom01are01")
            "Perimeter Calculator" -> DynamicCalculatorFragment.newInstance("geom01per01")
            "Volume Calculator" -> DynamicCalculatorFragment.newInstance("geom01vol01")
            "Surface Area" -> DynamicCalculatorFragment.newInstance("geom01sur01")
            "Pythagorean Theorem" -> DynamicCalculatorFragment.newInstance("geom01pyt01")
            "Trigonometry" -> DynamicCalculatorFragment.newInstance("geom01tri01")
            
            // Finance
            "Simple Interest" -> DynamicCalculatorFragment.newInstance("finc01sim01")
            "Compound Interest" -> DynamicCalculatorFragment.newInstance("finc01com01")
            "Loan Calculator" -> DynamicCalculatorFragment.newInstance("finc01loa01")
            "Mortgage Calculator" -> DynamicCalculatorFragment.newInstance("finc01mor01")
            "Investment Calculator" -> DynamicCalculatorFragment.newInstance("finc01inv01")
            "Tax Calculator" -> DynamicCalculatorFragment.newInstance("finc01tax01")
            
            // Insurance
            "Life Insurance" -> DynamicCalculatorFragment.newInstance("insr01lif01")
            "Health Insurance" -> DynamicCalculatorFragment.newInstance("insr01hea01")
            "Auto Insurance" -> DynamicCalculatorFragment.newInstance("insr01aut01")
            "Home Insurance" -> DynamicCalculatorFragment.newInstance("insr01hom01")
            "Premium Calculator" -> DynamicCalculatorFragment.newInstance("insr01pre01")
            "Coverage Calculator" -> DynamicCalculatorFragment.newInstance("insr01cov01")
            
            // Health
            "BMI Calculator" -> DynamicCalculatorFragment.newInstance("hlth01bmi01")
            "Calorie Calculator" -> DynamicCalculatorFragment.newInstance("hlth01cal01")
            "BMR Calculator" -> DynamicCalculatorFragment.newInstance("hlth01bmr01")
            "Body Fat Calculator" -> DynamicCalculatorFragment.newInstance("hlth01bod01")
            "Ideal Weight" -> DynamicCalculatorFragment.newInstance("hlth01ide01")
            "Health Score" -> DynamicCalculatorFragment.newInstance("hlth01sco01")
            
            // Date & Time
            "Date Calculator" -> DynamicCalculatorFragment.newInstance("date01dat01")
            "Time Calculator" -> DynamicCalculatorFragment.newInstance("date01tim01")
            "Age Calculator" -> DynamicCalculatorFragment.newInstance("date01age01")
            "Countdown Timer" -> DynamicCalculatorFragment.newInstance("date01cou01")
            "Time Zone Converter" -> DynamicCalculatorFragment.newInstance("date01zon01")
            "Calendar Calculator" -> DynamicCalculatorFragment.newInstance("date01cal01")
            
            // Unit Converters
            "Length Converter" -> DynamicCalculatorFragment.newInstance("unit01len01")
            "Weight Converter" -> DynamicCalculatorFragment.newInstance("unit01wei01")
            "Temperature Converter" -> DynamicCalculatorFragment.newInstance("unit01tem01")
            "Area Converter" -> DynamicCalculatorFragment.newInstance("unit01are01")
            "Volume Converter" -> DynamicCalculatorFragment.newInstance("unit01vol01")
            "Speed Converter" -> DynamicCalculatorFragment.newInstance("unit01spe01")
            
            // Others
            "Percentage Calculator" -> DynamicCalculatorFragment.newInstance("othr01per01")
            "Ratio Calculator" -> DynamicCalculatorFragment.newInstance("othr01rat01")
            "Statistics Calculator" -> DynamicCalculatorFragment.newInstance("othr01sta01")
            "Probability Calculator" -> DynamicCalculatorFragment.newInstance("othr01pro01")
            "Scientific Calculator" -> DynamicCalculatorFragment.newInstance("othr01sci01")
            "Matrix Calculator" -> DynamicCalculatorFragment.newInstance("othr01mat01")
            
            else -> LinearEquationsFragment() // fallback
        }
    }
}
