package com.shaadow.onecalculator.model

/**
 * Data class representing a unit calculator item.
 * Used to display calculator options in the home screen.
 *
 * @param id Unique identifier for the calculator
 * @param title Display name of the calculator
 * @param description Brief description of the calculator's purpose
 * @param icon Resource ID for the calculator's icon
 * @author Calculator Team
 * @since 1.0
 */
data class UnitCalculator(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int
)

/**
 * Data class representing a section of calculators with a title and list of calculators.
 * Used to group related calculators together for display.
 *
 * @param title The title of the calculator section (e.g., "Geometry", "Finance")
 * @param calculators The list of UnitCalculator items in this section
 * @author Calculator Team
 * @since 1.0
 */
data class CalculatorSection(
    val title: String,
    val calculators: List<UnitCalculator>
)
