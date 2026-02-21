package com.meowcat.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.meowcat.app.ui.theme.MeowCatTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Определяем список ярких цветов
val RainbowColors = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color(0xFF8A2BE2), // Фиолетовый
    Color(0xFFFF6600), // Оранжевый
    Color(0xFF00FFFF) // Голубой
)

/**
 * Модель данных для отпечатка лапки.
 * Добавлены Animatable для вращения и смещения по X.
 */
data class Footprint(
    val id: Long,
    val position: Offset,
    val color: Color,
    val opacity: Animatable<Float, *> = Animatable(1f),
    val rotation: Animatable<Float, *> = Animatable(0f), // Для качания
    val translationX: Animatable<Float, *> = Animatable(0f), // Для смещения
    var cleanupJob: Job? = null
)

/**
 * Генерирует AnnotatedString со случайным регистром и цветом для каждого символа.
 */
fun generateDynamicText(baseText: String): AnnotatedString {
    return buildAnnotatedString {
        baseText.forEach { char ->
            val randomCaseChar = if (Random.nextBoolean()) char.uppercaseChar() else char.lowercaseChar()
            val randomColor = RainbowColors.random()
            withStyle(style = SpanStyle(color = randomColor)) {
                append(randomCaseChar)
            }
        }
    }
}

/**
 * Функция для рисования одной лапки (один большой круг и ЧЕТЫРЕ маленьких)
 * ИСПРАВЛЕНО: Добавлен четвертый палец слева для большей реалистичности.
 */
fun drawFootprint(scope: androidx.compose.ui.graphics.drawscope.DrawScope, fp: Footprint) = scope.apply {
    val alpha = fp.opacity.value
    val largeRadius = 30f // Размер основной подушечки
    val smallRadius = 13f // Размер маленьких пальчиков (увеличен)

    // Применяем трансформации для качания (смещение и вращение)
    withTransform({
        translate(left = fp.translationX.value, top = 0f)
        rotate(degrees = fp.rotation.value, pivot = fp.position)
    }) {

        // --- Настройки смещения для формы лапки ---
        // Сдвигаем все элементы вниз, чтобы "пальчики" были выше "подушечки"
        val verticalOffset = 15f
        val baseCenter = fp.position.copy(y = fp.position.y + verticalOffset)

        // Смещение пальцев относительно центра лапки
        val fingerOffsetY = 40f // Насколько высоко пальцы над центром подушечки
        val fingerOffsetX = 23f // Насколько широко разведены боковые пальцы


        // 1. Основная подушечка (БОЛЬШОЙ КРУГ)
        drawCircle(
            color = fp.color.copy(alpha = alpha),
            center = baseCenter, // Используем смещенный центр
            radius = largeRadius,
            style = Fill
        )

        // 2. ЧЕТЫРЕ пальчика (МАЛЕНЬКИЕ КРУГИ)

        // Пальчик 1 (Центральный верхний)
        drawCircle(
            color = fp.color.copy(alpha = alpha),
            center = baseCenter.copy(y = baseCenter.y - fingerOffsetY),
            radius = smallRadius,
            style = Fill
        )

        // Пальчик 2 (Левый верхний)
        drawCircle(
            color = fp.color.copy(alpha = alpha),
            center = baseCenter.copy(x = baseCenter.x - fingerOffsetX, y = baseCenter.y - fingerOffsetY * 0.8f),
            radius = smallRadius,
            style = Fill
        )

        // Пальчик 3 (Правый верхний)
        drawCircle(
            color = fp.color.copy(alpha = alpha),
            center = baseCenter.copy(x = baseCenter.x + fingerOffsetX, y = baseCenter.y - fingerOffsetY * 0.8f),
            radius = smallRadius,
            style = Fill
        )

        // Пальчик 4 (НОВЫЙ! Дополнительный палец слева)
        drawCircle(
            color = fp.color.copy(alpha = alpha),
            center = baseCenter.copy(x = baseCenter.x - fingerOffsetX * 1.5f, y = baseCenter.y - fingerOffsetY * 0.2f),
            radius = smallRadius,
            style = Fill
        )
    }
}
// ----------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowCatTheme {
                // !!! ГЛАВНОЕ ИЗМЕНЕНИЕ: Запускаем AppNavigation !!!
                AppNavigation()
            }
        }
    }
}

@Composable
fun MainCatScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Для запуска корутин анимации лапок

    // --- ЛОГИКА ДИНАМИЧЕСКОГО ТЕКСТА ---
    var dynamicText by remember {
        mutableStateOf(generateDynamicText("ДОБРО ПОЖАЛОВАТЬ В MEOW CAT"))
    }

    LaunchedEffect(Unit) {
        val baseString = "ДОБРО ПОЖАЛОВАТЬ В MEOW CAT"
        while (true) {
            delay(1000) // Задержка в 1 секунду
            dynamicText = generateDynamicText(baseString)
        }
    }

    // --- ЛОГИКА АНИМИРОВАННЫХ ЛАПОК ---
    var activeFootprints by remember { mutableStateOf(listOf<Footprint>()) }
    val lifespan = 3000L
    val maxFootprints = 5

    // Логика удаления старых лапок, если лимит превышен
    LaunchedEffect(activeFootprints.size) {
        if (activeFootprints.size > maxFootprints) {
            activeFootprints.firstOrNull()?.cleanupJob?.cancel()
            activeFootprints = activeFootprints.drop(1)
        }
    }

    Surface(
        color = Color(0xFFFFCC80), // Светлый оранжевый фон
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { // <-- ОТСЛЕЖИВАЕМ НАЖАТИЯ ДЛЯ ЛАПОК
                detectTapGestures { offset ->
                    val newFootprint = Footprint(
                        id = System.currentTimeMillis(),
                        position = offset,
                        color = RainbowColors.random()
                    )

                    activeFootprints = activeFootprints + newFootprint

                    newFootprint.cleanupJob = scope.launch {
                        // Запускаем анимацию качания (вращения)
                        launch {
                            newFootprint.rotation.animateTo(15f, tween(durationMillis = 200, easing = LinearEasing))
                            newFootprint.rotation.animateTo(-15f, tween(durationMillis = 400, easing = LinearEasing))
                            newFootprint.rotation.animateTo(0f, tween(durationMillis = 200, easing = LinearEasing))
                        }
                        // Запускаем анимацию смещения по X (боковой "прыжок")
                        launch {
                            newFootprint.translationX.animateTo(10f, tween(durationMillis = 200, easing = LinearEasing))
                            newFootprint.translationX.animateTo(-10f, tween(durationMillis = 400, easing = LinearEasing))
                            newFootprint.translationX.animateTo(0f, tween(durationMillis = 200, easing = LinearEasing))
                        }

                        delay(lifespan) // Ждем, пока лапка "покачается"

                        // Анимация плавного исчезновения
                        newFootprint.opacity.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 500)
                        )
                        activeFootprints = activeFootprints.filter { it.id != newFootprint.id }
                    }
                }
            }
    ) {

        // --- РЕНДЕРИНГ ЛАПОК (Canvas) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            activeFootprints.forEach { fp ->
                drawFootprint(this, fp)
            }
        }

        // --- ОСНОВНОЙ КОНТЕНТ (Логотип, текст, кнопки) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Логотип кота
            Image(
                painter = painterResource(id = R.drawable.cat_logo),
                contentDescription = "Meow Cat Logo",
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .clickable {
                        Toast
                            .makeText(context, "МЯУ!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .background(Color.Transparent)
            )

            // Динамический текст
            Text(
                text = dynamicText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = 48.dp
                )
            )

            // 1. КНОПКА РЕГИСТРАЦИИ
            Button(
                onClick = {
                    // Переход на экран регистрации
                    navController.navigate(Screen.Registration.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                // Стилизация (как договаривались)
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF6600)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Начать знакомство! (Регистрация)",
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. КНОПКА ВХОДА
            Button(
                onClick = {
                    // Переход на экран аутентификации
                    navController.navigate(Screen.Auth.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                // Стилизация
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Уже есть аккаунт?",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainCatScreenPreview() {
    MeowCatTheme {
        MainCatScreen(navController = rememberNavController())
    }
}