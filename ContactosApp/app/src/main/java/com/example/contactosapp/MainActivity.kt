package com.example.contactosapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.contactosapp.ui.theme.ContactosAppTheme

class MainActivity : ComponentActivity() {
    // Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, la pantalla de contactos lo manejará internamente
        } else {
            // Permiso denegado, puedes mostrar un mensaje o manejar el caso
            Toast.makeText(this, "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso al inicio si no está concedido
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        enableEdgeToEdge()
        setContent {
            ContactosAppTheme {
                ContactListScreen()
            }
        }
    }
}

// Modelo de datos para Contacto
data class Contacto(
    val id: String,
    val nombre: String,
    val telefono: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen() {
    val context = LocalContext.current
    var contactos by remember { mutableStateOf(listOf<Contacto>()) }
    var permisoConcedido by remember { mutableStateOf(false) }

    // Solicitar permisos al cargar la pantalla
    LaunchedEffect(Unit) {
        permisoConcedido = checkContactPermission(context)
        if (permisoConcedido) {
            contactos = obtenerContactos(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Contactos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (!permisoConcedido) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Permiso de contactos no concedido",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (contactos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay contactos disponibles",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(contactos) { contacto ->
                    ContactoItemView(contacto)
                }
            }
        }
    }
}

@Composable
fun ContactoItemView(contacto: Contacto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Ícono de contacto",
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp)
            )

            Column {
                Text(
                    text = contacto.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = contacto.telefono,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Función para verificar permiso de contactos
fun checkContactPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

// Función para obtener contactos del dispositivo
fun obtenerContactos(context: Context): List<Contacto> {
    val contactos = mutableListOf<Contacto>()

    // Columnas que queremos recuperar
    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.HAS_PHONE_NUMBER
    )

    // Consultar contactos
    val cursor = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.Contacts.DISPLAY_NAME + " ASC"
    )

    cursor?.use { contactsCursor ->
        // Obtener índices de columnas con verificación segura
        val idIndex = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val nameIndex = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
        val hasPhoneIndex = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

        while (contactsCursor.moveToNext()) {
            // Obtener ID y nombre
            val id = contactsCursor.getString(idIndex) ?: continue
            val nombre = contactsCursor.getString(nameIndex) ?: continue

            // Verificar si el contacto tiene teléfono
            val hasPhoneNumber = contactsCursor.getInt(hasPhoneIndex)

            // Si tiene número de teléfono, buscar los teléfonos
            if (hasPhoneNumber > 0) {
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )

                phoneCursor?.use { phoneCursorInner ->
                    val phoneNumberIndex = phoneCursorInner.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                    while (phoneCursorInner.moveToNext()) {
                        val phoneNumber = phoneCursorInner.getString(phoneNumberIndex)
                            ?: continue

                        // Agregar contacto solo si tiene nombre y teléfono
                        contactos.add(Contacto(id, nombre, phoneNumber))
                    }
                }
            }
        }
    }
    return contactos
}

// Función de solicitud de permiso (usa un callback o launcher de permisos en tu Activity)
fun solicitarPermisoContactos(activity: Context) {
    ActivityCompat.requestPermissions(
        activity as ComponentActivity,
        arrayOf(Manifest.permission.READ_CONTACTS),
        CONTACT_PERMISSION_REQUEST_CODE
    )
}

// Constante para el código de solicitud de permiso
const val CONTACT_PERMISSION_REQUEST_CODE = 100

