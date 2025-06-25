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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Calculator() {
    val logic = remember { CalculatorLogic() }
    val display = logic.display
    val expression = logic.expression
    val history = logic.history
    var showHistory by remember { mutableStateOf(false) }
    val buttonShape = RoundedCornerShape(16.dp)
    val buttonSpacing = 8.dp
    val buttonHeight = 64.dp
    val buttonColors = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f),
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
                    Text(
                        text = expression,
                        fontSize = 32.sp,
                        color = buttonColors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = display,
                        fontSize = 60.sp,
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
            Spacer(modifier = Modifier.height(buttonSpacing))
            Column(
                modifier = Modifier.weight(4.5f),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("C", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onClearClick() }
                    IconButton(
                        onClick = { logic.onDeleteClick() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = buttonColors.secondaryContainer,
                            contentColor = buttonColors.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Удалить символ",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    CalculatorM3Button("(", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("(") }
                    CalculatorM3Button(")", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick(")") }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("7", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("7") }
                    CalculatorM3Button("8", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("8") }
                    CalculatorM3Button("9", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("9") }
                    CalculatorM3Button("÷", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onOperatorClick("÷") }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("4", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("4") }
                    CalculatorM3Button("5", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("5") }
                    CalculatorM3Button("6", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("6") }
                    CalculatorM3Button("×", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onOperatorClick("×") }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("1", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("1") }
                    CalculatorM3Button("2", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("2") }
                    CalculatorM3Button("3", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("3") }
                    CalculatorM3Button("+", buttonColors.primaryContainer, buttonColors.onPrimaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onOperatorClick("+") }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorM3Button("±", buttonColors.secondaryContainer, buttonColors.onSecondaryContainer, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onPlusMinusClick() }
                    CalculatorM3Button("0", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onNumberClick("0") }
                    CalculatorM3Button(".", buttonColors.surface, buttonColors.onSurface, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onDotClick() }
                    CalculatorM3Button("=", buttonColors.primary, buttonColors.onPrimary, buttonShape, 34.sp, 30.sp, Modifier.weight(1f)) { logic.onEqualsClick() }
                }
            }
        }
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
                        IconButton(
                            onClick = { logic.clearHistory() },
                            enabled = history.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить историю",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = { showHistory = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть историю",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (history.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(history.size) { index ->
                                val (expr, res) = history[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString("$expr = $res"))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Скопировано!")
                                                }
                                            }
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        expr,
                                        color = buttonColors.onSurfaceVariant,
                                        fontSize = 22.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        res,
                                        color = buttonColors.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
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
    fontSize: TextUnit,
    opFontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            fontSize = if (text in listOf("+", "-", "×", "÷", "=", "C", "±", "%", "(", ")")) opFontSize else fontSize,
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

