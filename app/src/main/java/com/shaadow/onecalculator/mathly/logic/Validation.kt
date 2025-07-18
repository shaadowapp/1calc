package com.shaadow.onecalculator.mathly.logic

object Validation {
    private val mathKeywords = listOf(
        // Math keywords
        "integrate", "derivative", "derive", "solve", "limit", "simplify", "factor", "expand",
        "evaluate", "differentiate", "find", "calculate", "+", "-", "*", "/", "=", "^", "log", "ln",
        "sin", "cos", "tan", "cot", "sec", "csc", "matrix", "determinant", "abs", "pi", "sqrt",
        // Greetings
        "hi", "hey", "hello", "greetings", "good morning", "good evening", "good afternoon"
    )
    private val greetingKeywords = listOf(
        "hi", "hey", "hello", "greetings", "good morning", "good evening", "good afternoon"
    )

    fun isMathQuery(input: String): Boolean {
        // Accept if contains math keyword or symbols
        return mathKeywords.any { input.contains(it, ignoreCase = true) }
    }

    fun isGreeting(input: String): Boolean {
        return greetingKeywords.any { input.equals(it, ignoreCase = true) || input.trim().startsWith(it, ignoreCase = true) }
    }
} 