package com.example.aplikasi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.aplikasi.ui.theme.AplikasiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AplikasiTheme {
                SensorAppScreen()
            }
        }
    }
}

/**
 * Main Screen Composable yang mengelola pembacaan sensor, manajemen lifecycle,
 * dan perubahan tema dinamis (gelap/terang) berdasarkan sensor cahaya.
 */
@Composable
fun SensorAppScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State untuk Ambient Light Sensor
    var luxValue by remember { mutableFloatStateOf(0f) }
    var isLightSensorAvailable by remember { mutableStateOf(value = true) }

    // State untuk 3-Axis Accelerometer
    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }
    var accelZ by remember { mutableFloatStateOf(0f) }
    var isAccelAvailable by remember { mutableStateOf(value = true) }

    // State untuk melacak status Lifecycle saat ini
    var currentLifecycleEvent by remember { mutableStateOf("INITIALIZING") }

    // ---------------------------------------------------------------------------------
    // Manajemen Lifecycle menggunakan DisposableEffect & LifecycleEventObserver
    // ---------------------------------------------------------------------------------
    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        isLightSensorAvailable = (lightSensor != null)
        isAccelAvailable = (accelSensor != null)

        // Callback listener untuk pembacaan data sensor secara real-time
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.values.isNotEmpty()) {
                        when (it.sensor.type) {
                            Sensor.TYPE_LIGHT -> {
                                luxValue = it.values[0]
                            }
                            Sensor.TYPE_ACCELEROMETER -> {
                                if (it.values.size >= 3) {
                                    accelX = it.values[0]
                                    accelY = it.values[1]
                                    accelZ = it.values[2]
                                }
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Tidak ada aksi khusus yang diperlukan saat akurasi berubah
            }
        }

        // Observer untuk merespons event ON_RESUME dan ON_PAUSE dari Lifecycle Owner
        val observer = LifecycleEventObserver { _, event ->
            currentLifecycleEvent = event.name
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Mendaftarkan sensor listener saat aplikasi aktif di layar (ON_RESUME)
                    lightSensor?.let {
                        sensorManager.registerListener(
                            sensorEventListener,
                            it,
                            SensorManager.SENSOR_DELAY_UI,
                        )
                    }
                    accelSensor?.let {
                        sensorManager.registerListener(
                            sensorEventListener,
                            it,
                            SensorManager.SENSOR_DELAY_UI,
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Melepaskan sensor listener saat aplikasi tidak aktif/ditinggalkan (ON_PAUSE)
                    sensorManager.unregisterListener(sensorEventListener)
                }
                else -> {}
            }
        }

        // Pendaftaran observer ke lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // Pembersihan saat composable di-dispose
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // ---------------------------------------------------------------------------------
    // Logika Efek Langsung pada UI (Dynamic Dark / Light Theme)
    // Syarat: Jika nilai lux < 20 maka beralih ke Mode Gelap (Dark Mode)
    // ---------------------------------------------------------------------------------
    val isDarkMode = luxValue < 20f

    // Transisi warna animasi yang halus
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF121212) else Color(0xFFF6F8FA),
        animationSpec = tween(durationMillis = 500),
        label = "backgroundColorAnim",
    )

    val cardBackgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
        animationSpec = tween(durationMillis = 500),
        label = "cardBackgroundColorAnim"
    )

    val textColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFFF0F0F0) else Color(0xFF1C1B1F),
        animationSpec = tween(durationMillis = 500),
        label = "textColorAnim"
    )

    val subtitleColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFFA0A0A0) else Color(0xFF666666),
        animationSpec = tween(durationMillis = 500),
        label = "subtitleColorAnim"
    )

    val accentColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF6200EE)

    // Layout Utama
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Aplikasi
            Text(
                text = "Real-Time Sensor Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "Aplikasi otomatis menyesuaikan tema berdasarkan kecerahan ruangan.",
                fontSize = 14.sp,
                color = subtitleColor
            )

            // Dynamic Theme Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE8F0FE)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkMode) "🌙" else "☀️",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isDarkMode) "Mode Gelap (Dark Mode)" else "Mode Terang (Light Theme)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Text(
                            text = if (isDarkMode) "Intensitas Cahaya < 20 Lux" else "Intensitas Cahaya ≥ 20 Lux",
                            fontSize = 13.sp,
                            color = subtitleColor
                        )
                    }
                }
            }

            // -----------------------------------------------------------------------------
            // 1. CARD: Ambient Light Sensor (Kategori Environment)
            // -----------------------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Light Sensor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                        Text(
                            text = "Environment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier
                                .background(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLightSensorAvailable) {
                        Text(
                            text = "%.1f Lux".format(luxValue),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Indicator visualisasi kecerahan (Max skala referensi 100 Lux)
                        val progress = (luxValue / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = accentColor,
                            trackColor = subtitleColor.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ambang batas Mode Gelap: 20 Lux",
                            fontSize = 12.sp,
                            color = subtitleColor
                        )
                    } else {
                        Text(
                            text = "Sensor Cahaya tidak tersedia pada perangkat ini.",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // -----------------------------------------------------------------------------
            // 2. CARD: 3-Axis Accelerometer (Kategori Motion)
            // -----------------------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📐 3-Axis Accelerometer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                        Text(
                            text = "Motion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier
                                .background(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAccelAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AxisCard(
                                axisName = "Sumbu X",
                                value = accelX,
                                textColor = textColor,
                                subtitleColor = subtitleColor,
                                accentColor = Color(0xFFEF5350),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AxisCard(
                                axisName = "Sumbu Y",
                                value = accelY,
                                textColor = textColor,
                                subtitleColor = subtitleColor,
                                accentColor = Color(0xFF66BB6A),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AxisCard(
                                axisName = "Sumbu Z",
                                value = accelZ,
                                textColor = textColor,
                                subtitleColor = subtitleColor,
                                accentColor = Color(0xFF42A5F5),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = "Accelerometer tidak tersedia pada perangkat ini.",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // -----------------------------------------------------------------------------
            // 3. CARD: Status Lifecycle & Manajemen Daya
            // -----------------------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚡ Status Lifecycle Sensor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (currentLifecycleEvent == "ON_RESUME") Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentLifecycleEvent,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLifecycleEvent == "ON_RESUME")
                                "Listener Aktif (Menerima data real-time)"
                            else
                                "Listener Dilepas (Menghemat baterai)",
                            fontSize = 12.sp,
                            color = subtitleColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sub-composable untuk menampilkan komponen nilai sumbu X, Y, Z secara terstruktur.
 */
@Composable
fun AxisCard(
    axisName: String,
    value: Float,
    textColor: Color,
    subtitleColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = axisName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "%.2f".format(value),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = "m/s²",
            fontSize = 10.sp,
            color = subtitleColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SensorAppPreview() {
    AplikasiTheme {
        SensorAppScreen()
    }
}
