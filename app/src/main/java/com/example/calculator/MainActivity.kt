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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.TextUnit

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calculator() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val displayFontSize = if (isLandscape) 36.sp else 60.sp
        val exprFontSize = if (isLandscape) 18.sp else 32.sp
        val buttonFontSize = if (isLandscape) 18.sp else 34.sp
        val opButtonFontSize = if (isLandscape) 16.sp else 30.sp
        val buttonPadding = if (isLandscape) 2.dp else 8.dp
        val rowWeight = if (isLandscape) 0.7f else 1f
        val displayWeight = if (isLandscape) 1f else 1.5f
        val buttonsWeight = if (isLandscape) 3.5f else 4.5f
        val historyExprFontSize = if (isLandscape) 14.sp else 22.sp
        val historyResFontSize = if (isLandscape) 18.sp else 28.sp

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

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                verticalArrangement = Arrangement.spacedBy(buttonPadding)
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
                        .weight(displayWeight),
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
                            fontSize = exprFontSize,
                            color = buttonColors.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Результат (копируем по нажатию)
                        Text(
                            text = display,
                            fontSize = displayFontSize,
                            fontWeight = FontWeight.Bold,
                            color = buttonColors.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                Spacer(modifier = Modifier.height(buttonPadding))
                // Кнопки
                Column(
                    modifier = Modifier.weight(buttonsWeight),
                    verticalArrangement = Arrangement.spacedBy(buttonPadding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(rowWeight),
                        horizontalArrangement = Arrangement.spacedBy(buttonPadding)
                    ) {
                        CalculatorM3Button("C", buttonColors.errorContainer, buttonColors.onErrorContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onClearClick() }
                        CalculatorM3Button("±", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onPlusMinusClick() }
                        CalculatorM3Button("%", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onPercentClick() }
                        CalculatorM3Button("÷", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onOperatorClick("÷") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(rowWeight),
                        horizontalArrangement = Arrangement.spacedBy(buttonPadding)
                    ) {
                        CalculatorM3Button("7", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("7") }
                        CalculatorM3Button("8", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("8") }
                        CalculatorM3Button("9", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("9") }
                        CalculatorM3Button("×", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onOperatorClick("×") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(rowWeight),
                        horizontalArrangement = Arrangement.spacedBy(buttonPadding)
                    ) {
                        CalculatorM3Button("4", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("4") }
                        CalculatorM3Button("5", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("5") }
                        CalculatorM3Button("6", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("6") }
                        CalculatorM3Button("-", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onOperatorClick("-") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(rowWeight),
                        horizontalArrangement = Arrangement.spacedBy(buttonPadding)
                    ) {
                        CalculatorM3Button("1", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("1") }
                        CalculatorM3Button("2", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("2") }
                        CalculatorM3Button("3", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onNumberClick("3") }
                        CalculatorM3Button("+", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onOperatorClick("+") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(rowWeight),
                        horizontalArrangement = Arrangement.spacedBy(buttonPadding)
                    ) {
                        CalculatorM3Button("0", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(2f)) { onNumberClick("0") }
                        CalculatorM3Button(".", buttonColors.surface, buttonColors.onSurface, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onDotClick() }
                        CalculatorM3Button("=", buttonColors.primary, buttonColors.onPrimary, buttonShape, buttonFontSize, opButtonFontSize, Modifier.weight(1f)) { onEqualsClick() }
                    }
                }
            }
            // История как BottomSheet
            if (showHistory) {
                ModalBottomSheet(
                    onDismissRequest = { showHistory = false },
                    sheetState = sheetState,
                    dragHandle = {},
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "История вычислений",
                                style = MaterialTheme.typography.titleLarge,
                                color = buttonColors.primary
                            )
                            TextButton(onClick = { showHistory = false }) {
                                Text("Закрыть")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("История пуста", color = buttonColors.onSurfaceVariant)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                history.forEach { (expr, res) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            expr,
                                            color = buttonColors.onSurfaceVariant,
                                            fontSize = historyExprFontSize,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            res,
                                            color = buttonColors.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = historyResFontSize,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun CalculatorM3Button(
    text: String,
    color: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
    fontSize: TextUnit,
    opFontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().padding(0.dp),
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
            fontSize = if (text in listOf("+", "-", "×", "÷", "=", "C", "±", "%")) opFontSize else fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

