package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.ui.theme.CalculatorTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF17181A)
                ) {
                    Calculator()
                }
            }
        }
    }
}

@Composable
fun Calculator() {
    var display by remember { mutableStateOf("0") }
    var currentNumber by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var previousNumber by remember { mutableStateOf("") }
    var isNewCalculation by remember { mutableStateOf(true) }
    var expression by remember { mutableStateOf("") }
    val buttonShape = RoundedCornerShape(16.dp)
    val buttonSpacing = 8.dp
    val buttonHeight = 64.dp
    val buttonColors = MaterialTheme.colorScheme

    // История вычислений
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Pair<String, String>>() } // (выражение, результат)

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Функции для обработки логики
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
            // Заменяем символы на стандартные для парсинга
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
            // Добавляем в историю только если выражение не пустое и не ошибка
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Верхняя панель с кнопкой истории
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showHistory = true }) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "История вычислений",
                        tint = buttonColors.primary
                    )
                }
            }
            // Дисплей
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                colors = CardDefaults.cardColors(containerColor = buttonColors.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Выражение
                    Text(
                        text = expression,
                        fontSize = 22.sp,
                        color = buttonColors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Результат (копируем по нажатию)
                    Text(
                        text = display,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonColors.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(display))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Скопировано!")
                                }
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Кнопки калькулятора
            Column(
                verticalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                // Первый ряд: C, ±, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button(
                        text = "C",
                        color = buttonColors.errorContainer,
                        contentColor = buttonColors.onErrorContainer,
                        shape = buttonShape,
                        height = buttonHeight,
                        modifier = Modifier.weight(1f)
                    ) { onClearClick() }
                    CalculatorM3Button(
                        text = "±",
                        color = buttonColors.secondaryContainer,
                        contentColor = buttonColors.onSecondaryContainer,
                        shape = buttonShape,
                        height = buttonHeight,
                        modifier = Modifier.weight(1f)
                    ) { onPlusMinusClick() }
                    CalculatorM3Button(
                        text = "%",
                        color = buttonColors.secondaryContainer,
                        contentColor = buttonColors.onSecondaryContainer,
                        shape = buttonShape,
                        height = buttonHeight,
                        modifier = Modifier.weight(1f)
                    ) { onPercentClick() }
                    CalculatorM3Button(
                        text = "÷",
                        color = buttonColors.primaryContainer,
                        contentColor = buttonColors.onPrimaryContainer,
                        shape = buttonShape,
                        height = buttonHeight,
                        modifier = Modifier.weight(1f)
                    ) { onOperatorClick("÷") }
                }
                // Второй ряд: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("7", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("7") }
                    CalculatorM3Button("8", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("8") }
                    CalculatorM3Button("9", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("9") }
                    CalculatorM3Button("×", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonHeight, Modifier.weight(1f)) { onOperatorClick("×") }
                }
                // Третий ряд: 4, 5, 6, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("4", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("4") }
                    CalculatorM3Button("5", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("5") }
                    CalculatorM3Button("6", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("6") }
                    CalculatorM3Button("-", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonHeight, Modifier.weight(1f)) { onOperatorClick("-") }
                }
                // Четвертый ряд: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("1", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("1") }
                    CalculatorM3Button("2", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("2") }
                    CalculatorM3Button("3", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onNumberClick("3") }
                    CalculatorM3Button("+", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonHeight, Modifier.weight(1f)) { onOperatorClick("+") }
                }
                // Пятый ряд: 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("0", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(2f)) { onNumberClick("0") }
                    CalculatorM3Button(".", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonHeight, Modifier.weight(1f)) { onDotClick() }
                    CalculatorM3Button("=", buttonColors.primary, buttonColors.onPrimary, buttonShape, buttonHeight, Modifier.weight(1f)) { onEqualsClick() }
                }
            }
        }
        // Диалог истории
        if (showHistory) {
            AlertDialog(
                onDismissRequest = { showHistory = false },
                title = { Text("История вычислений") },
                text = {
                    if (history.isEmpty()) {
                        Text("История пуста")
                    } else {
                        Column(modifier = Modifier.heightIn(max = 320.dp)) {
                            history.forEach { (expr, res) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(expr, color = buttonColors.onSurfaceVariant)
                                    Text(res, color = buttonColors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistory = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CalculatorM3Button(
    text: String,
    color: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            fontSize = if (text in listOf("+", "-", "×", "÷", "=", "C", "±", "%")) 22.sp else 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// === ВНЕ КЛАССА ===
// Простой парсер выражения с поддержкой +, -, *, /
fun evaluate(expr: String): Double {
    val tokens = expr.replace(" ", "")
    val numbers = mutableListOf<Double>()
    val ops = mutableListOf<Char>()
    var i = 0
    while (i < tokens.length) {
        when {
            tokens[i].isDigit() || tokens[i] == '.' || (tokens[i] == '-' && (i == 0 || !tokens[i-1].isDigit())) -> {
                val sb = StringBuilder()
                if (tokens[i] == '-') {
                    sb.append('-')
                    i++
                }
                while (i < tokens.length && (tokens[i].isDigit() || tokens[i] == '.')) {
                    sb.append(tokens[i])
                    i++
                }
                numbers.add(sb.toString().toDouble())
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

