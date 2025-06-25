package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CalculatorLogic {
    var display by mutableStateOf("0")
    var currentNumber by mutableStateOf("")
    var operator by mutableStateOf("")
    var previousNumber by mutableStateOf("")
    var isNewCalculation by mutableStateOf(true)
    var expression by mutableStateOf("")
    var history = mutableListOf<Pair<String, String>>()

    fun updateDisplay(text: String) {
        display = if (text.isEmpty()) "0" else text
    }

    fun updateExpression(number: String = "", op: String = "") {
        expression = when {
            op.isNotEmpty() -> "$expression $op"
            number.isNotEmpty() -> if (expression.isEmpty() || isNewCalculation) number else "$expression$number"
            else -> ""
        }
    }

    fun onNumberClick(number: String) {
        if (isNewCalculation) {
            currentNumber = number
            isNewCalculation = false
            expression = number
        } else {
            if (currentNumber.length < 10) {
                currentNumber += number
                updateExpression(number)
            }
        }
        updateDisplay(currentNumber)
    }

    fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.8f", result).trimEnd('0').trimEnd('.')
        }
    }

    fun calculateExpression(expr: String): String {
        try {
            val cleanExpr = expr.replace('×', '*').replace('÷', '/')
            val result = evaluate(cleanExpr)
            return formatResult(result)
        } catch (e: Exception) {
            return "Ошибка"
        }
    }

    fun onOperatorClick(newOperator: String) {
        if (currentNumber.isNotEmpty()) {
            if (!isNewCalculation) {
                currentNumber = calculateExpression(expression)
                updateDisplay(currentNumber)
            }
            previousNumber = currentNumber
        }
        operator = newOperator
        updateExpression(op = newOperator)
        currentNumber = ""
        isNewCalculation = false
    }

    fun onEqualsClick() {
        if (expression.isNotEmpty()) {
            val result = calculateExpression(expression)
            updateDisplay(result)
            if (result != "Ошибка") {
                history.add(0, expression to result)
            }
            currentNumber = result
            previousNumber = ""
            operator = ""
            isNewCalculation = true
            expression = result
        }
    }

    fun onClearClick() {
        currentNumber = ""
        previousNumber = ""
        operator = ""
        expression = ""
        isNewCalculation = true
        updateDisplay("0")
    }

    fun onDotClick() {
        if (isNewCalculation) {
            currentNumber = "0."
            expression = "0."
            isNewCalculation = false
        } else if (currentNumber.isEmpty()) {
            currentNumber = "0."
            updateExpression("0.")
        } else if (!currentNumber.contains(".")) {
            currentNumber += "."
            updateExpression(".")
        }
        updateDisplay(currentNumber)
    }

    fun onPlusMinusClick() {
        if (currentNumber.isNotEmpty() && currentNumber != "0") {
            currentNumber = if (currentNumber.startsWith("-")) {
                currentNumber.substring(1)
            } else {
                "-$currentNumber"
            }
            expression = if (expression.startsWith("-")) {
                expression.substring(1)
            } else {
                "-$expression"
            }
            updateDisplay(currentNumber)
        }
    }

    fun onPercentClick() {
        if (currentNumber.isNotEmpty()) {
            val number = currentNumber.toDoubleOrNull()
            if (number != null) {
                val result = number / 100
                currentNumber = formatResult(result)
                expression = currentNumber
                updateDisplay(currentNumber)
            }
        }
    }

    fun onDeleteClick() {
        if (currentNumber.isNotEmpty()) {
            currentNumber = currentNumber.dropLast(1)
            updateDisplay(currentNumber)
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
            }
            if (currentNumber.isEmpty()) {
                updateDisplay("")
            }
        }
    }

    fun clearHistory() {
        history.clear()
    }

    // --- Парсер выражения с поддержкой скобок ---
    fun evaluate(expr: String): Double {
        val tokens = expr.replace(" ", "")
        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var i = 0
        fun parseNumber(): Double {
            val sb = StringBuilder()
            if (i < tokens.length && tokens[i] == '-') {
                sb.append('-')
                i++
            }
            while (i < tokens.length && (tokens[i].isDigit() || tokens[i] == '.')) {
                sb.append(tokens[i])
                i++
            }
            return sb.toString().toDouble()
        }
        while (i < tokens.length) {
            when {
                tokens[i] == '(' -> {
                    ops.add('(')
                    i++
                }
                tokens[i] == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') {
                        val b = numbers.removeAt(numbers.lastIndex)
                        val a = numbers.removeAt(numbers.lastIndex)
                        val op = ops.removeAt(ops.lastIndex)
                        numbers.add(applyOp(a, b, op))
                    }
                    if (ops.isNotEmpty() && ops.last() == '(') ops.removeAt(ops.lastIndex)
                    i++
                }
                tokens[i].isDigit() || tokens[i] == '-' && (i == 0 || tokens[i-1] in "+-*/(") -> {
                    numbers.add(parseNumber())
                }
                tokens[i] in "+-*/" -> {
                    while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(tokens[i])) {
                        val b = numbers.removeAt(numbers.lastIndex)
                        val a = numbers.removeAt(numbers.lastIndex)
                        val op = ops.removeAt(ops.lastIndex)
                        numbers.add(applyOp(a, b, op))
                    }
                    ops.add(tokens[i])
                    i++
                }
                else -> i++
            }
        }
        while (ops.isNotEmpty()) {
            val b = numbers.removeAt(numbers.lastIndex)
            val a = numbers.removeAt(numbers.lastIndex)
            val op = ops.removeAt(ops.lastIndex)
            numbers.add(applyOp(a, b, op))
        }
        return numbers[0]
    }

    fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        else -> 0
    }

    fun applyOp(a: Double, b: Double, op: Char): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) throw ArithmeticException("Деление на ноль") else a / b
        else -> 0.0
    }
} 