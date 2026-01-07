
@file:OptIn(ExperimentalMaterial3Api::class)

package com.competencypassport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.coroutines.resume
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompetencyPassportApp()
        }
    }
}

@Composable
fun CompetencyPassportApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val authManager = remember { MsalAuthManager(context) }
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).service }

    var screen by remember { mutableStateOf(AppScreen.Login) }
    var activeTab by remember { mutableStateOf(MainTab.Passport) }
    var passportScreen by remember { mutableStateOf(PassportScreen.List) }
    var competencies by remember { mutableStateOf<List<CompetencySummary>>(emptyList()) }
    var selectedCompetency by remember { mutableStateOf<CompetencyDetail?>(null) }
    var shareLink by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<NurseProfile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadCompetencies() {
        scope.launch {
            errorMessage = null
            try {
                refreshTokenIfNeeded(authManager, tokenStore)?.let { tokenStore.save(it) }
                competencies = api.getCompetencies()
            } catch (ex: Exception) {
                errorMessage = "Failed to load competencies."
            }
        }
    }

    fun loadProfile() {
        scope.launch {
            errorMessage = null
            try {
                refreshTokenIfNeeded(authManager, tokenStore)?.let { tokenStore.save(it) }
                profile = api.getProfile()
            } catch (ex: Exception) {
                errorMessage = "Failed to load profile."
            }
        }
    }

    fun ensureLoggedIn() {
        if (tokenStore.accessToken != null) {
            screen = AppScreen.Home
            loadCompetencies()
            loadProfile()
        }
    }

    LaunchedEffect(Unit) {
        ensureLoggedIn()
    }

    CompetencyPassportTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLogin = {
                            authManager.signIn(activity, object : MsalAuthManager.AuthCallback {
                                override fun onSuccess(result: IAuthenticationResult) {
                                    tokenStore.save(result.accessToken)
                                    logTokenDetails("AuthToken", result.accessToken)
                                    screen = AppScreen.Home
                                    loadCompetencies()
                                    loadProfile()
                                }

                                override fun onError(error: MsalException) {
                                    errorMessage = "Login failed: ${error.message}"
                                }
                            })
                        },
                        errorMessage = errorMessage
                    )
                }
                AppScreen.Home -> {
                    val topBarTitle = when (activeTab) {
                        MainTab.Passport -> when (passportScreen) {
                            PassportScreen.List -> "Competency Passport"
                            PassportScreen.Detail -> "Competency Record"
                            PassportScreen.Edit -> "Record Update"
                            PassportScreen.Share -> "Share Pack"
                        }
                        MainTab.Profile -> "Professional Profile"
                        MainTab.References -> "Quick References"
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(topBarTitle) },
                                navigationIcon = if (activeTab == MainTab.Passport && passportScreen != PassportScreen.List) {
                                    {
                                        IconButton(onClick = {
                                            passportScreen = PassportScreen.List
                                            loadCompetencies()
                                        }) {
                                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                } else {
                                    {}
                                },
                                actions = {
                                    if (activeTab == MainTab.Passport && passportScreen == PassportScreen.List) {
                                        IconButton(onClick = {
                                            shareLink = null
                                            passportScreen = PassportScreen.Share
                                        }) {
                                            Icon(Icons.Outlined.Share, contentDescription = "Share Pack")
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = activeTab == MainTab.Passport,
                                    onClick = {
                                        activeTab = MainTab.Passport
                                        passportScreen = PassportScreen.List
                                    },
                                    icon = { Icon(Icons.Outlined.Assignment, contentDescription = "Passport") },
                                    label = { Text("Passport") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == MainTab.Profile,
                                    onClick = {
                                        activeTab = MainTab.Profile
                                        loadProfile()
                                    },
                                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == MainTab.References,
                                    onClick = { activeTab = MainTab.References },
                                    icon = { Icon(Icons.Outlined.Link, contentDescription = "References") },
                                    label = { Text("References") }
                                )
                            }
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.background,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        ) {
                            when (activeTab) {
                                MainTab.Passport -> {
                                    when (passportScreen) {
                                        PassportScreen.List -> {
                                            CompetencyListScreen(
                                                competencies = competencies,
                                                errorMessage = errorMessage,
                                                onRefresh = { loadCompetencies() },
                                                onSelect = {
                                                    scope.launch {
                                                        try {
                                                            selectedCompetency = api.getCompetency(it)
                                                            passportScreen = PassportScreen.Detail
                                                        } catch (ex: Exception) {
                                                            errorMessage = "Failed to load competency."
                                                        }
                                                    }
                                                },
                                                onAdd = {
                                                    selectedCompetency = null
                                                    passportScreen = PassportScreen.Edit
                                                }
                                            )
                                        }
                                        PassportScreen.Detail -> {
                                            val detail = selectedCompetency
                                            if (detail != null) {
                                                CompetencyDetailScreen(
                                                    competency = detail,
                                                    accessToken = tokenStore.accessToken,
                                                    isUploading = isUploading,
                                                    onEdit = { passportScreen = PassportScreen.Edit },
                                                    onUpload = { uri, note ->
                                                        scope.launch {
                                                            isUploading = true
                                                            try {
                                                                val filePart = createMultipartFromUri(context, uri)
                                                                val notePart = note?.trim().takeIf { !it.isNullOrBlank() }
                                                                    ?.toRequestBody("text/plain".toMediaTypeOrNull())
                                                                api.uploadEvidence(detail.id, filePart, notePart)
                                                                selectedCompetency = api.getCompetency(detail.id)
                                                            } catch (ex: Exception) {
                                                                errorMessage = "Evidence upload failed."
                                                            } finally {
                                                                isUploading = false
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        PassportScreen.Edit -> {
                                            CompetencyEditScreen(
                                                initial = selectedCompetency,
                                                errorMessage = errorMessage,
                                                onCancel = { passportScreen = PassportScreen.List },
                                                onSave = { title, description, achieved, expiry, category ->
                                                    scope.launch {
                                                        errorMessage = null
                                                        try {
                                                            val refreshed = refreshTokenIfNeeded(authManager, tokenStore)
                                                            if (refreshed == null && tokenStore.accessToken.isNullOrBlank()) {
                                                                screen = AppScreen.Login
                                                                val message = "Session expired. Please sign in again."
                                                                errorMessage = message
                                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                                return@launch
                                                            }
                                                            refreshed?.let { tokenStore.save(it) }
                                                            val request = CompetencyUpsertRequest(
                                                                title,
                                                                description,
                                                                achieved,
                                                                expiry,
                                                                category
                                                            )
                                                            if (selectedCompetency == null) {
                                                                selectedCompetency = api.createCompetency(request)
                                                            } else {
                                                                api.updateCompetency(selectedCompetency!!.id, request)
                                                                selectedCompetency = api.getCompetency(selectedCompetency!!.id)
                                                            }
                                                            passportScreen = PassportScreen.Detail
                                                        } catch (ex: HttpException) {
                                                            if (ex.code() == 401) {
                                                                tokenStore.clear()
                                                                screen = AppScreen.Login
                                                                val message = "Session expired. Please sign in again. (Check API scope/audience if this repeats.)"
                                                                errorMessage = message
                                                                Log.e("CompetencySave", message, ex)
                                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                                return@launch
                                                            }
                                                            val detail = ex.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
                                                            val message = detail ?: "Failed to save competency."
                                                            errorMessage = message
                                                            Log.e("CompetencySave", message, ex)
                                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                        } catch (ex: Exception) {
                                                            errorMessage = "Failed to save competency."
                                                            Log.e("CompetencySave", "Unexpected error", ex)
                                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        PassportScreen.Share -> {
                                            SharePackScreen(
                                                competencies = competencies,
                                                profile = profile,
                                                shareLink = shareLink,
                                                onCreate = { expiryDays, selected, includePin ->
                                                    scope.launch {
                                                        try {
                                                            val response = api.createSharePack(
                                                                SharePackCreateRequest(expiryDays, selected, includePin)
                                                            )
                                                            shareLink = response.shareUrl
                                                        } catch (ex: Exception) {
                                                            errorMessage = "Failed to create share pack."
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                MainTab.Profile -> {
                                    ProfileScreen(
                                        profile = profile,
                                        onSave = { update ->
                                            scope.launch {
                                                try {
                                                    profile = api.updateProfile(update)
                                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to save profile."
                                                }
                                            }
                                        }
                                    )
                                }
                                MainTab.References -> {
                                    ReferenceScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppScreen {
    Login,
    Home
}

enum class MainTab {
    Passport,
    Profile,
    References
}

enum class PassportScreen {
    List,
    Detail,
    Edit,
    Share
}

@Composable
fun LoginScreen(onLogin: () -> Unit, errorMessage: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in with Microsoft Entra ID", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogin) {
            Text("Sign In")
        }
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun CompetencyListScreen(
    competencies: List<CompetencySummary>,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your professional record", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Keep evidence current for appraisal, revalidation, and practice assurance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAdd) { Text("Add competency") }
                    OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                }
            }
        }
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(competencies) { competency ->
                Card(
                    onClick = { onSelect(competency.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(competency.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        if (!competency.description.isNullOrBlank()) {
                            Text(
                                competency.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(competency.status)
                            AssistChip(onClick = {}, label = { Text(competency.category) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(expiryLabel(competency.expiresAt), style = MaterialTheme.typography.bodySmall)
                            Text("Evidence ${competency.evidenceCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompetencyDetailScreen(
    competency: CompetencyDetail,
    accessToken: String?,
    isUploading: Boolean,
    onEdit: () -> Unit,
    onUpload: (Uri, String?) -> Unit
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }

    val cameraUriState = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUriState.value
        if (success && uri != null) {
            onUpload(uri, note)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onUpload(uri, note)
        }
    }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onUpload(uri, note)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(competency.title, style = MaterialTheme.typography.titleLarge)
                if (!competency.description.isNullOrBlank()) {
                    Text(
                        competency.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(competency.status)
                    AssistChip(onClick = {}, label = { Text(competency.category) })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Achieved ${formatDate(competency.achievedAt)}", style = MaterialTheme.typography.bodySmall)
                Text(expiryLabel(competency.expiresAt), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Edit record") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Evidence capture", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Upload signed evidence or accredited training documentation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional note") },
                    placeholder = { Text("e.g. Signed by Ward Manager") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = createImageUri(context)
                            cameraUriState.value = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture photo")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo library")
                    }
                    OutlinedButton(
                        onClick = { docLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload file")
                    }
                }
                if (isUploading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Evidence record", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (competency.evidence.isEmpty()) {
            Text("No evidence uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(competency.evidence) { evidence ->
                    EvidenceCard(competency.id, evidence, accessToken)
                }
            }
        }
    }
}

@Composable
fun CompetencyEditScreen(
    initial: CompetencyDetail?,
    errorMessage: String?,
    onCancel: () -> Unit,
    onSave: (String, String?, Date, Date, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var achievedDate by remember { mutableStateOf(parseLocalDate(initial?.achievedAt)) }
    var expiryDate by remember { mutableStateOf(parseLocalDate(initial?.expiresAt)) }
    var category by remember { mutableStateOf(initial?.category ?: "Mandatory") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Competency details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                DatePickerField(
                    label = "Achieved date",
                    date = achievedDate,
                    onDateSelected = { achievedDate = it }
                )
                DatePickerField(
                    label = "Expiry date",
                    date = expiryDate,
                    onDateSelected = { expiryDate = it }
                )
                CategorySelector(selected = category, onSelect = { category = it })
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
                if (!error.isNullOrBlank()) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    Button(onClick = {
                        if (title.isBlank()) {
                            error = "Title is required."
                            return@Button
                        }
                        val achievedLocal = achievedDate
                        val expiryLocal = expiryDate
                        if (achievedLocal == null || expiryLocal == null) {
                            error = "Select achieved and expiry dates."
                            return@Button
                        }
                        val achieved = Date.from(achievedLocal.atStartOfDay().toInstant(ZoneOffset.UTC))
                        val expiry = Date.from(expiryLocal.atStartOfDay().toInstant(ZoneOffset.UTC))
                        onSave(title.trim(), description.ifBlank { null }, achieved, expiry, category)
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mandatory", "Clinical", "Specialist").forEach { item ->
                AssistChip(
                    onClick = { onSelect(item) },
                    label = { Text(item) },
                    colors = if (item == selected) {
                        AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }
    }
}

@Composable
fun SharePackScreen(
    competencies: List<CompetencySummary>,
    profile: NurseProfile?,
    shareLink: String?,
    onCreate: (Int, List<String>, Boolean) -> Unit
) {
    var expiryDays by remember { mutableStateOf("30") }
    var includeNmcPin by remember { mutableStateOf(false) }
    val selections = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Share pack", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Read-only pack for employers, mentors, or placement leads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (profile != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nurse: ${profile.fullName}", style = MaterialTheme.typography.bodySmall)
                    if (!profile.registrationType.isNullOrBlank()) {
                        Text("Registration: ${profile.registrationType}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = expiryDays, onValueChange = { expiryDays = it }, label = { Text("Expiry days") })
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeNmcPin, onCheckedChange = { includeNmcPin = it })
                    Column {
                        Text("Include NMC PIN (optional)")
                        Text("Off by default to protect confidential registration details.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val days = expiryDays.toIntOrNull() ?: 30
                    val selected = selections.filter { it.value }.keys.toList()
                    onCreate(days, selected, includeNmcPin)
                }) { Text("Generate share pack") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Selected competencies", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(competencies) { competency ->
                val checked = selections[competency.id] ?: false
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { selections[competency.id] = it })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(competency.title, fontWeight = FontWeight.Medium)
                            Text(competency.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (!shareLink.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Share link ready", style = MaterialTheme.typography.titleSmall)
                    Text(shareLink, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Share link", shareLink))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy link")
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Registration type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        val options = listOf("RN Adult", "RN Mental Health", "RN Learning Disability", "RN Child")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                AssistChip(
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = if (option == selected) {
                        AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val initialMillis = date?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val display = date?.let { formatLocalDate(it) } ?: "Select date"

    OutlinedTextField(
        value = display,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = null)
            }
        }
    )

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selected)
                    }
                    open = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun ProfileScreen(profile: NurseProfile?, onSave: (NurseProfileUpdateRequest) -> Unit) {
    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var preferredName by remember(profile) { mutableStateOf(profile?.preferredName ?: "") }
    var nmcPin by remember(profile) { mutableStateOf(profile?.nmcPin ?: "") }
    var registrationType by remember(profile) { mutableStateOf(profile?.registrationType ?: "") }
    var employer by remember(profile) { mutableStateOf(profile?.employer ?: "") }
    var roleBand by remember(profile) { mutableStateOf(profile?.roleBand ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    val email = profile?.email ?: ""
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Professional identity", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = preferredName, onValueChange = { preferredName = it }, label = { Text("Preferred name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nmcPin, onValueChange = { nmcPin = it }, label = { Text("NMC PIN (confidential)") }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Your NMC PIN is stored securely and only shared if you explicitly include it in a share pack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                RegistrationTypeSelector(selected = registrationType, onSelect = { registrationType = it })
                OutlinedTextField(value = employer, onValueChange = { employer = it }, label = { Text("Employer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = roleBand, onValueChange = { roleBand = it }, label = { Text("Role / Band") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Contact", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email (read-only)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Professional bio", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Short bio (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!error.isNullOrBlank()) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = {
            val pin = nmcPin.trim()
            val pinValid = Regex("^[A-Za-z]{2}\\d{6}$").matches(pin)
            error = when {
                fullName.isBlank() -> "Full name is required."
                registrationType.isBlank() -> "Select a registration type."
                !pinValid -> "NMC PIN must be 2 letters followed by 6 digits."
                else -> null
            }
            if (error == null) {
                onSave(
                    NurseProfileUpdateRequest(
                        fullName.trim(),
                        preferredName.ifBlank { null },
                        pin,
                        registrationType.trim(),
                        employer.ifBlank { null },
                        roleBand.ifBlank { null },
                        phone.ifBlank { null },
                        bio.ifBlank { null }
                    )
                )
            }
        }) {
            Text("Save profile")
        }
    }
}

@Composable
fun ReferenceScreen() {
    val context = LocalContext.current
    val links = listOf(
        QuickLink("NMC Code", "Professional standards of practice and behaviour.", "https://www.nmc.org.uk/standards/code/"),
        QuickLink("NMC Revalidation", "Guidance for five-year revalidation cycles.", "https://www.nmc.org.uk/revalidation/"),
        QuickLink("NICE Guidance", "Clinical guidelines and evidence summaries.", "https://www.nice.org.uk/guidance"),
        QuickLink("NHS England", "National policy updates and operational guidance.", "https://www.england.nhs.uk/"),
        QuickLink("BNF / Medicines", "Medicines guidance and prescribing information.", "https://bnf.nice.org.uk/"),
        QuickLink("Local policy", "Placeholder for local trust or care-home policy.", "https://www.nhs.uk")
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Quick references", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(links) { link ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(link.title, fontWeight = FontWeight.Medium)
                            Text(link.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Outlined.Link, contentDescription = "Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvidenceCard(competencyId: String, evidence: Evidence, accessToken: String?) {
    val context = LocalContext.current
    val isImage = evidence.contentType?.startsWith("image/") == true
    val downloadUrl = "${BuildConfig.API_BASE_URL.trimEnd('/')}/competencies/$competencyId/evidence/${evidence.id}/download"
    val headers = if (!accessToken.isNullOrBlank()) {
        Headers.Builder().add("Authorization", "Bearer $accessToken").build()
    } else {
        Headers.Builder().build()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(downloadUrl)
                        .headers(headers)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(evidence.fileName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(evidence.uploadedAt), style = MaterialTheme.typography.bodySmall)
                if (!evidence.note.isNullOrBlank()) {
                    Text(evidence.note!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "Expired" -> "Expired" to MaterialTheme.colorScheme.error
        "ExpiringSoon" -> "Expiring soon" to Color(0xFFB26A00)
        else -> "Valid" to MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            val icon = when (status) {
                "Expired" -> Icons.Outlined.EventBusy
                "ExpiringSoon" -> Icons.Outlined.WarningAmber
                else -> Icons.Outlined.CheckCircle
            }
            Icon(icon, contentDescription = null)
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.12f), labelColor = color)
    )
}

fun expiryLabel(expiresAt: String): String {
    val now = Instant.now()
    val expiry = parseInstant(expiresAt)
    val days = ChronoUnit.DAYS.between(now, expiry)
    return when {
        days < 0 -> "Expired ${kotlin.math.abs(days)} days ago"
        days == 0L -> "Expires today"
        days == 1L -> "Expires in 1 day"
        else -> "Expires in $days days"
    }
}

fun formatDate(value: String): String {
    val date = parseInstant(value)
    return DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC).format(date)
}

fun parseLocalDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }
    return runCatching { LocalDate.parse(value.substring(0, 10)) }.getOrNull()
}

fun formatLocalDate(value: LocalDate): String {
    return DateTimeFormatter.ofPattern("d MMM yyyy").format(value)
}

fun parseInstant(value: String): Instant {
    return try {
        Instant.parse(value)
    } catch (ex: Exception) {
        Instant.now()
    }
}

fun createImageUri(context: Context): Uri {
    val file = File.createTempFile("evidence_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

suspend fun refreshTokenIfNeeded(authManager: MsalAuthManager, tokenStore: TokenStore): String? {
    return authManager.acquireTokenSilent() ?: tokenStore.accessToken
}

fun logTokenDetails(tag: String, token: String?) {
    if (token.isNullOrBlank()) {
        Log.w(tag, "No access token available.")
        return
    }
    val parts = token.split(".")
    if (parts.size < 2) {
        Log.w(tag, "Invalid token format.")
        return
    }
    val payload = try {
        val decoded = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        String(decoded, StandardCharsets.UTF_8)
    } catch (ex: Exception) {
        Log.w(tag, "Unable to decode token payload.", ex)
        return
    }
    val json = try {
        JSONObject(payload)
    } catch (ex: Exception) {
        Log.w(tag, "Unable to parse token payload.", ex)
        return
    }
    val aud = json.optString("aud")
    val scp = json.optString("scp")
    Log.i(tag, "Token aud=$aud scp=$scp")
}

class TokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "competencypassport_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String? = prefs.getString("access_token", null)
        private set

    fun save(token: String) {
        accessToken = token
        prefs.edit().putString("access_token", token).apply()
    }

    fun clear() {
        accessToken = null
        prefs.edit().remove("access_token").apply()
    }
}

class MsalAuthManager(private val context: Context) {
    interface AuthCallback {
        fun onSuccess(result: IAuthenticationResult)
        fun onError(error: MsalException)
    }

    private var app: ISingleAccountPublicClientApplication? = null
    private var initErrorMessage: String? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    app = application
                }

                override fun onError(exception: MsalException) {
                    initErrorMessage = exception.message ?: "Failed to initialize MSAL."
                }
            }
        )
    }

    fun signIn(activity: ComponentActivity, callback: AuthCallback) {
        val scopes = arrayOf(BuildConfig.API_SCOPE)
        val currentApp = app
        if (currentApp == null) {
            val message = initErrorMessage ?: "MSAL is still initializing. Try again."
            callback.onError(MsalClientException("app_not_ready", message))
            return
        }
        currentApp.signIn(activity, null, scopes, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                callback.onSuccess(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                callback.onError(exception)
            }

            override fun onCancel() {
            }
        })
    }

    suspend fun acquireTokenSilent(): String? = suspendCancellableCoroutine { cont ->
        val scopes = arrayOf(BuildConfig.API_SCOPE)
        val currentApp = app
        if (currentApp == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        currentApp.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount == null) {
                    cont.resume(null)
                    return
                }
                currentApp.acquireTokenSilentAsync(scopes, activeAccount.authority, object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        logTokenDetails("SilentToken", authenticationResult.accessToken)
                        cont.resume(authenticationResult.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        cont.resume(null)
                    }
                })
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
            }

            override fun onError(exception: MsalException) {
                cont.resume(null)
            }
        })
    }
}

class ApiClient(private val tokenStore: TokenStore) {
    private val moshi = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = tokenStore.accessToken
        val newRequest = if (!token.isNullOrBlank()) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            Log.w("ApiAuth", "Missing access token for ${request.method} ${request.url}")
            request
        }
        chain.proceed(newRequest)
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttp)
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)
}

interface ApiService {
    @GET("competencies")
    suspend fun getCompetencies(): List<CompetencySummary>

    @POST("competencies")
    suspend fun createCompetency(@Body request: CompetencyUpsertRequest): CompetencyDetail

    @GET("competencies/{id}")
    suspend fun getCompetency(@Path("id") id: String): CompetencyDetail

    @PUT("competencies/{id}")
    suspend fun updateCompetency(@Path("id") id: String, @Body request: CompetencyUpsertRequest)

    @Multipart
    @POST("competencies/{id}/evidence")
    suspend fun uploadEvidence(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @Part("note") note: RequestBody?
    ): Evidence

    @POST("sharepacks")
    suspend fun createSharePack(@Body request: SharePackCreateRequest): SharePackResponse

    @GET("profile")
    suspend fun getProfile(): NurseProfile

    @PUT("profile")
    suspend fun updateProfile(@Body request: NurseProfileUpdateRequest): NurseProfile
}

data class CompetencySummary(
    val id: String,
    val title: String,
    val description: String?,
    val achievedAt: String,
    val expiresAt: String,
    val category: String,
    val status: String,
    val evidenceCount: Int
)

data class CompetencyDetail(
    val id: String,
    val title: String,
    val description: String?,
    val achievedAt: String,
    val expiresAt: String,
    val category: String,
    val status: String,
    val evidence: List<Evidence>
)

data class Evidence(
    val id: String,
    val fileName: String,
    val contentType: String?,
    val note: String?,
    val size: Long,
    val uploadedAt: String
)

data class CompetencyUpsertRequest(
    val title: String,
    val description: String?,
    val achievedAt: Date,
    val expiresAt: Date,
    val category: String
)

data class SharePackCreateRequest(
    val expiryDays: Int,
    val competencyIds: List<String>,
    val includeNmcPin: Boolean
)

data class SharePackResponse(
    val token: String,
    val expiresAt: String,
    val shareUrl: String
)

data class NurseProfile(
    val fullName: String,
    val preferredName: String?,
    val nmcPin: String?,
    val registrationType: String?,
    val employer: String?,
    val roleBand: String?,
    val email: String?,
    val phone: String?,
    val bio: String?
)

data class NurseProfileUpdateRequest(
    val fullName: String,
    val preferredName: String?,
    val nmcPin: String?,
    val registrationType: String?,
    val employer: String?,
    val roleBand: String?,
    val phone: String?,
    val bio: String?
)

data class QuickLink(
    val title: String,
    val description: String,
    val url: String
)

suspend fun createMultipartFromUri(context: Context, uri: Uri): MultipartBody.Part {
    return withContext(Dispatchers.IO) {
        val fileName = getDisplayName(context, uri) ?: "evidence_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val temp = File.createTempFile("upload", null, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        val body = temp.asRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", fileName, body)
    }
}

fun getDisplayName(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val cursor: Cursor? = resolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return uri.lastPathSegment
}

@Composable
fun CompetencyPassportTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF1B4B40),
        onPrimary = Color.White,
        secondary = Color(0xFF496A5F),
        primaryContainer = Color(0xFFD7E8E1),
        background = Color(0xFFF5F4F1),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5E2DD),
        error = Color(0xFFB13A2A),
        onSurfaceVariant = Color(0xFF5E6A63)
    )

    val typography = MaterialTheme.typography.copy(
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.2.sp),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.SansSerif)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
