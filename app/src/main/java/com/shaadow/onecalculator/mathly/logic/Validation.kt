package com.shaadow.onecalculator.mathly.logic

object Validation {
    private val mathKeywords = listOf(
        // Math keywords
        "integrate", "derivative", "derive", "solve", "limit", "simplify", "factor", "expand",
        "evaluate", "differentiate", "find", "calculate", "+", "-", "*", "/", "=", "^", "log", "ln",
        "sin", "cos", "tan", "cot", "sec", "csc", "matrix", "determinant", "abs", "pi", "sqrt",
        // Natural language math phrases
        "square root", "cube root", "percentage", "percent", "sum", "product", "difference", "quotient",
        "power", "root", "modulus", "mod", "divide", "multiply", "add", "subtract", "minus", "plus", "times",
        "of", "what is", "equals", "is", "value", "result",
        // Math problem and question related terms
        "problem", "question", "exercise", "example", "write", "create", "generate", "complex", "simple",
        "math", "mathematical", "algebra", "geometry", "calculus", "trigonometry", "statistics", "probability",
        "equation", "formula", "expression", "solution", "answer", "step", "steps", "explain", "explanation",
        "how to", "show me", "give me", "provide", "help", "assist", "teach", "learn", "understand",
        // Common legitimate questions
        "who are you", "what are you", "how are you", "what can you do", "what do you do",
        "who is mathly", "what is mathly", "tell me about yourself", "introduce yourself",
        "what's your name", "what is your name", "how do you work", "how does this work",
        "what is this", "what's this", "explain yourself", "describe yourself",
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

    fun containsInappropriateContent(input: String): Boolean {
        return ContentFilter.containsInappropriateContent(input)
    }

    fun getFilteredMessage(input: String): String {
        val baseMessage = ContentFilter.getDetailedFilteredMessage(input)

        // Add educational suggestions for better user experience
        return if (ContentFilter.containsInappropriateContent(input)) {
            baseMessage + ContentFilter.getEducationalSuggestions()
        } else {
            baseMessage
        }
    }

    /**
     * Get content severity level for logging/analytics
     */
    fun getContentSeverity(input: String): Int {
        return ContentFilter.getSeverityLevel(input)
    }

    /**
     * Check if content might be a false positive
     */
    fun isLikelyFalsePositive(input: String): Boolean {
        return ContentFilter.isLikelyFalsePositive(input)
    }
} 