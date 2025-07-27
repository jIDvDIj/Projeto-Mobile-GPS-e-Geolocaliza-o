package com.example.geolocalizador


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.example.geolocalizador.ui.theme.GeolocalizadorTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            GeolocalizadorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MapsScreen(fusedLocationClient)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(fusedLocationClient: FusedLocationProviderClient) {
    val localIFPE = LatLng(-8.8768, -36.4631)
    val localPadrao = LatLng(-8.05, -34.9)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(localPadrao, 12f)
    }

    var localUsuario by remember { mutableStateOf<LatLng?>(null) }
    var destino by remember { mutableStateOf<LatLng?>(null) }

    val context = LocalContext.current
    val permissionState = rememberUpdatedState(
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    localUsuario = latLng
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionState.value) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    localUsuario = latLng
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionState.value),
            onMapClick = { clickedLatLng ->
                destino = clickedLatLng
            }
        ) {
            destino?.let {
                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.MarkerState(position = it),
                    title = "Destino selecionado"
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Bottom
        ) {
            destino?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f) // Fundo branco com transparência
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Coordenadas: Lat ${"%.5f".format(it.latitude)}, Lng ${"%.5f".format(it.longitude)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }

            // Botão superior – traçar rota até o IFPE
            Button(
                onClick = {
                    localUsuario?.let { origem ->
                        val uri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&origin=${origem.latitude},${origem.longitude}&destination=${localIFPE.latitude},${localIFPE.longitude}&travelmode=driving"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rota até o IFPE")
            }

            Button(
                onClick = {
                    localUsuario?.let { origem ->
                        destino?.let { destinoLatLng ->
                            val uri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&origin=${origem.latitude},${origem.longitude}&destination=${destinoLatLng.latitude},${destinoLatLng.longitude}&travelmode=driving"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("Traçar rota até o destino")
            }
        }
    }
}