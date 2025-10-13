package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {
    var robotNetwork: Network? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            conectarRedRobot(this)
        }

        setContent {
            MaterialTheme {
                PantallaMicelio()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun conectarRedRobot(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                robotNetwork = network
                println("Red del robot encontrada y guardada")
            }

            override fun onLost(network: Network) {
                if (robotNetwork == network) {
                    robotNetwork = null
                    println("Conexion con red del robot perdida")
                }
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }
}

data class SensorData(
    val temperature: Int = 0,
    val humidity: Int = 0,
    val co2: Int = 0,
    val heatOn: Boolean = false,
    val humidifierOn: Boolean = false,
    val fanOn: Boolean = false
)

@Composable
fun PantallaMicelio() {
    val ipRaspberry = "10.129.40.199"
    val puerto = "5000"
    var sensorData by remember { mutableStateOf(SensorData()) }
    var conectado by remember { mutableStateOf(false) }
    var ultimaActualizacion by remember { mutableStateOf("Nunca") }
    val context = LocalContext.current

    fun obtenerDatos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$ipRaspberry:$puerto/api/sensors/summary")
                val activity = context as? MainActivity

                val connection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity?.robotNetwork != null) {
                    activity.robotNetwork!!.openConnection(url) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }

                connection.requestMethod = "GET"
                connection.connectTimeout = 3000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    sensorData = SensorData(
                        temperature = json.getInt("temperature"),
                        humidity = json.getInt("humidity"),
                        co2 = json.getInt("CO2"),
                        heatOn = json.getJSONObject("actuators").getBoolean("heat"),
                        humidifierOn = json.getJSONObject("actuators").getBoolean("humidifier"),
                        fanOn = json.getJSONObject("actuators").getBoolean("fan")
                    )
                    conectado = true
                    ultimaActualizacion = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                    Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show()
                }

                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    conectado = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Auto-actualizar cada 5 segundos
    LaunchedEffect(Unit) {
        while (true) {
            obtenerDatos()
            kotlinx.coroutines.delay(5000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Sistema de Micelio",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 5.dp)
            )

            Text(
                text = "Ultima actualizacion: $ultimaActualizacion",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Estado de conexion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (conectado) Color(0xFF81C784) else Color(0xFFE57373)
                )
            ) {
                Text(
                    text = if (conectado) "✓ Conectado" else "✗ Desconectado",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Sensores
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sensores",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Temperatura", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                "${sensorData.temperature}°C",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFFE91E63)
                            )
                        }

                        Column {
                            Text("Humedad", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                "${sensorData.humidity}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFF2196F3)
                            )
                        }

                        Column {
                            Text("CO2", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                "${sensorData.co2}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Text("ppm", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Actuadores
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Actuadores",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )

                    ActuadorRow("Calentador", sensorData.heatOn, Color(0xFFFF5722))
                    ActuadorRow("Humidificador", sensorData.humidifierOn, Color(0xFF2196F3))
                    ActuadorRow("Ventilador", sensorData.fanOn, Color(0xFF4CAF50))
                }
            }

            // Boton actualizar manual
            Button(
                onClick = { obtenerDatos() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Actualizar Datos", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de navegacion (proximamente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Proximamente", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f).padding(horizontal = 5.dp)
                ) {
                    Text("Diagnostico", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { Toast.makeText(context, "Proximamente", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f).padding(horizontal = 5.dp)
                ) {
                    Text("Config", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ActuadorRow(nombre: String, activo: Boolean, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nombre, fontSize = 16.sp)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (activo) color else Color.LightGray
            )
        ) {
            Text(
                text = if (activo) "ON" else "OFF",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}