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

//Version 2 Actualizacion: Para poder utilizar datos moviles mientras se esta conectado al Raspberry Pi //
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {
    //Version 2 Actualizacion: Variable para guardar referencia a la red WiFi del robot
    var robotNetwork: Network? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Version 2 Actualizacion: Inicializar conexion a red del robot (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            conectarRedRobot(this)
        }

        setContent {
            MaterialTheme {
                PantallaControl()
            }
        }
    }

    //Version 2 Actualizacion: Funcion para detectar y conectar a la red WiFi del robot
    @RequiresApi(Build.VERSION_CODES.M)
    fun conectarRedRobot(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Solicitar red WiFi sin requerir internet
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

@Composable
fun PantallaControl() {
    val ipRaspberry = "10.42.0.1"
    val puerto = "5000"
    var respuesta by remember { mutableStateOf("Sin conexión") }
    var conectado by remember { mutableStateOf(false) }
    val context = LocalContext.current

    //Version 2 Actualizacion: Funcion de peticion modificada para usar red especifica del robot
    fun hacerPeticion(endpoint: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$ipRaspberry:$puerto$endpoint")

                //Version 2 Actualizacion: Obtener MainActivity para acceder a robotNetwork
                val activity = context as? MainActivity

                //Version 2 Actualizacion: Usar red del robot si esta disponible, sino usar conexion normal
                val connection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity?.robotNetwork != null) {
                    activity.robotNetwork!!.openConnection(url) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }

                connection.requestMethod = "GET"
                connection.connectTimeout = 3000

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    respuesta = response
                    conectado = true

                    val mensaje = when(endpoint) {
                        "/" -> "Conectado al servidor"
                        "/hola" -> "Mensaje recibido"
                        "/led/on" -> "LED Encendido"
                        "/led/off" -> "LED Apagado"
                        else -> "Comando enviado"
                    }
                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                }

                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    respuesta = "Error: ${e.message}"
                    conectado = false
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Control Raspberry Pi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Text(
                text = "Servidor: $ipRaspberry:$puerto",
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

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
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { hacerPeticion("/") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Conectar al Servidor", color = Color.White)
            }

            Button(
                onClick = { hacerPeticion("/hola") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Text("Enviar Saludo")
            }

            Button(
                onClick = { hacerPeticion("/led/on") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("LED ON", color = Color.White)
            }

            Button(
                onClick = { hacerPeticion("/led/off") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("LED OFF", color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Respuesta del servidor:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Text(
                    text = respuesta,
                    modifier = Modifier.padding(16.dp),
                    color = Color.DarkGray
                )
            }
        }
    }
}