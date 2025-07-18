package com.shaadow.onecalculator.parser

import kotlin.math.*

object Expression {
    fun calculate(expression: String): Double {
        val sanitized = sanitize(expression)
        val withMultiplication = insertImplicitMultiplication(sanitized)
        val postfix = infixToPostfix(withMultiplication)
        return evaluatePostfix(postfix)
    }

    private fun sanitize(expr: String): String {
        return expr
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
    }

    internal fun insertImplicitMultiplication(expr: String): String {
        val sb = StringBuilder()
        for (i in expr.indices) {
            sb.append(expr[i])

            if (i < expr.length - 1) {
                val curr = expr[i]
                val next = expr[i + 1]

                if ((curr.isDigit() || curr == ')' || curr == '!') &&
                    (next == '(' || next == '√' || next.isLetter() || next == 'π' || next == 'e')) {
                    sb.append("*")
                }

                if ((curr == ')' || curr.isLetter() || curr == 'π' || curr == 'e') &&
                    (next.isDigit() || next == '(' || next == '√')) {
                    sb.append("*")
                }
            }
        }
        return sb.toString()
    }

    private fun precedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/", "%" -> 2
            "^" -> 3
            "√", "!" -> 4
            else -> 0
        }
    }

    private fun isOperator(token: String): Boolean {
        return token in listOf("+", "-", "*", "/", "%", "^", "√", "!")
    }

    private fun infixToPostfix(expression: String): List<String> {
        val output = mutableListOf<String>()
        val stack = mutableListOf<String>()
        val tokens = tokenize(expression)

        for (token in tokens) {
            when {
                token.isDouble() -> output.add(token)
                token == "(" -> stack.add(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.last() != "(") {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                    if (stack.isNotEmpty() && stack.last() == "(") {
                        stack.removeAt(stack.size - 1)
                    }
                }
                isOperator(token) -> {
                    while (stack.isNotEmpty() && precedence(token) <= precedence(stack.last())) {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                    stack.add(token)
                }
            }
        }

        while (stack.isNotEmpty()) {
            output.add(stack.removeAt(stack.size - 1))
        }

        return output
    }

    private fun evaluatePostfix(postfix: List<String>): Double {
        val stack = mutableListOf<Double>()

        for (token in postfix) {
            when {
                token.isDouble() -> stack.add(token.toDouble())
                token == "+" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a + b)
                }
                token == "-" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a - b)
                }
                token == "*" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a * b)
                }
                token == "/" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a / b)
                }
                token == "%" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a % b)
                }
                token == "^" -> {
                    val b = stack.removeAt(stack.lastIndex)
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(a.pow(b))
                }
                token == "√" -> {
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(sqrt(a))
                }
                token == "!" -> {
                    val a = stack.removeAt(stack.lastIndex)
                    stack.add(factorial(a.toInt()))
                }
            }
        }

        return stack.lastOrNull() ?: 0.0
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var num = ""

        for (ch in expr) {
            when {
                ch.isDigit() || ch == '.' -> num += ch
                ch in "+-*/%^()√!" -> {
                    if (num.isNotEmpty()) {
                        tokens.add(num)
                        num = ""
                    }
                    tokens.add(ch.toString())
                }
                ch == ',' -> {
                    // Ignore commas - they should be removed before calculation
                    // but handle them gracefully if they somehow reach here
                    continue
                }
                ch.isWhitespace() -> {
                    // Ignore whitespace
                    continue
                }
                else -> {
                    // Log the invalid character but don't crash
                    println("Warning: Invalid character '$ch' in expression, ignoring")
                    continue
                }
            }
        }
        if (num.isNotEmpty()) tokens.add(num)

        return tokens
    }

    private fun String.isDouble(): Boolean {
        return this.toDoubleOrNull() != null
    }

    private fun factorial(n: Int): Double {
        require(n >= 0) { "Factorial of negative number is undefined" }
        return if (n == 0 || n == 1) 1.0 else n * factorial(n - 1)
    }
}
