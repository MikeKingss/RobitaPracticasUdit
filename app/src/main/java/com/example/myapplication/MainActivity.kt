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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaControl()
            }
        }
    }
}

@Composable
fun PantallaControl() {
    // CAMBIA ESTA IP A LA DE TU RASPBERRY PI
    val ipRaspberry = "10.42.0.1"
    val puerto = "5000"
    var respuesta by remember { mutableStateOf("Sin conexión") }
    var conectado by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun hacerPeticion(endpoint: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$ipRaspberry:$puerto$endpoint")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    respuesta = response
                    conectado = true

                    // Mostrar mensaje específico según el endpoint
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
            // Título
            Text(
                text = "Control Raspberry Pi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // IP del servidor
            Text(
                text = "Servidor: $ipRaspberry:$puerto",
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Estado de conexión
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

            // Botones
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

            // Respuesta del servidor
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