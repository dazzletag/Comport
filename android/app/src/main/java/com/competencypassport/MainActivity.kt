@file:OptIn(ExperimentalMaterial3Api::class)

package com.competencypassport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.Date

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

    var screen by remember { mutableStateOf(Screen.Login) }
    var competencies by remember { mutableStateOf<List<CompetencySummary>>(emptyList()) }
    var selectedCompetency by remember { mutableStateOf<CompetencyDetail?>(null) }
    var shareLink by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadCompetencies() {
        scope.launch {
            errorMessage = null
            try {
                competencies = api.getCompetencies()
            } catch (ex: Exception) {
                errorMessage = "Failed to load competencies."
            }
        }
    }

    fun ensureLoggedIn() {
        if (tokenStore.accessToken != null) {
            screen = Screen.List
            loadCompetencies()
        }
    }

    LaunchedEffect(Unit) {
        ensureLoggedIn()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("CompetencyPassport") })
    }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screen) {
                Screen.Login -> {
                    LoginScreen(
                        onLogin = {
                            authManager.signIn(activity, object : MsalAuthManager.AuthCallback {
                                override fun onSuccess(result: IAuthenticationResult) {
                                    tokenStore.save(result.accessToken)
                                    screen = Screen.List
                                    loadCompetencies()
                                }

                                override fun onError(error: MsalException) {
                                    errorMessage = "Login failed: ${error.message}"
                                }
                            })
                        },
                        errorMessage = errorMessage
                    )
                }
                Screen.List -> {
                    CompetencyListScreen(
                        competencies = competencies,
                        errorMessage = errorMessage,
                        onRefresh = { loadCompetencies() },
                        onSelect = {
                            scope.launch {
                                try {
                                    selectedCompetency = api.getCompetency(it)
                                    screen = Screen.Detail
                                } catch (ex: Exception) {
                                    errorMessage = "Failed to load competency."
                                }
                            }
                        },
                        onAdd = {
                            selectedCompetency = null
                            screen = Screen.Edit
                        },
                        onShare = {
                            shareLink = null
                            screen = Screen.Share
                        }
                    )
                }
                Screen.Detail -> {
                    val detail = selectedCompetency
                    if (detail != null) {
                        CompetencyDetailScreen(
                            competency = detail,
                            onBack = {
                                screen = Screen.List
                                loadCompetencies()
                            },
                            onUpload = { uri ->
                                scope.launch {
                                    try {
                                        val filePart = createMultipartFromUri(context, uri)
                                        api.uploadEvidence(detail.id, filePart)
                                        selectedCompetency = api.getCompetency(detail.id)
                                    } catch (ex: Exception) {
                                        errorMessage = "Evidence upload failed."
                                    }
                                }
                            }
                        )
                    }
                }
                Screen.Edit -> {
                    CompetencyEditScreen(
                        initial = selectedCompetency,
                        onCancel = { screen = Screen.List },
                        onSave = { title, description, expiry ->
                            scope.launch {
                                try {
                                    if (selectedCompetency == null) {
                                        api.createCompetency(CompetencyUpsertRequest(title, description, expiry))
                                    } else {
                                        api.updateCompetency(selectedCompetency!!.id, CompetencyUpsertRequest(title, description, expiry))
                                    }
                                    screen = Screen.List
                                    loadCompetencies()
                                } catch (ex: Exception) {
                                    errorMessage = "Failed to save competency."
                                }
                            }
                        }
                    )
                }
                Screen.Share -> {
                    SharePackScreen(
                        competencies = competencies,
                        shareLink = shareLink,
                        onBack = {
                            screen = Screen.List
                        },
                        onCreate = { expiryDays, selected ->
                            scope.launch {
                                try {
                                    val response = api.createSharePack(SharePackCreateRequest(expiryDays, selected))
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
    }
}

enum class Screen {
    Login,
    List,
    Detail,
    Edit,
    Share
}

@Composable
fun LoginScreen(onLogin: () -> Unit, errorMessage: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
    onAdd: () -> Unit,
    onShare: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAdd) { Text("Add") }
            OutlinedButton(onClick = onShare) { Text("Share Pack") }
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(competencies) { competency ->
                Card(onClick = { onSelect(competency.id) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(competency.title, fontWeight = FontWeight.Bold)
                        if (!competency.description.isNullOrBlank()) {
                            Text(competency.description)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text(competency.status) })
                            AssistChip(onClick = {}, label = { Text("Evidence ${competency.evidenceCount}") })
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
    onBack: () -> Unit,
    onUpload: (Uri) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onUpload(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = { picker.launch("*/*") }) { Text("Upload Evidence") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(competency.title, style = MaterialTheme.typography.titleLarge)
        if (!competency.description.isNullOrBlank()) {
            Text(competency.description)
        }
        Text("Status: ${competency.status}")
        Spacer(modifier = Modifier.height(12.dp))
        Text("Evidence", style = MaterialTheme.typography.titleMedium)
        if (competency.evidence.isEmpty()) {
            Text("No evidence yet.")
        } else {
            competency.evidence.forEach { evidence ->
                Text("- ${evidence.fileName}")
            }
        }
    }
}

@Composable
fun CompetencyEditScreen(
    initial: CompetencyDetail?,
    onCancel: () -> Unit,
    onSave: (String, String?, Date) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var expiryInput by remember { mutableStateOf(initial?.expiresAt?.substring(0, 10) ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = {
                try {
                    val date = LocalDate.parse(expiryInput)
                    val expiry = Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC))
                    onSave(title, description.ifBlank { null }, expiry)
                } catch (ex: DateTimeParseException) {
                    error = "Expiry must be yyyy-MM-dd"
                }
            }) { Text("Save") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        OutlinedTextField(value = expiryInput, onValueChange = { expiryInput = it }, label = { Text("Expiry (yyyy-MM-dd)") })
        if (!error.isNullOrBlank()) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SharePackScreen(
    competencies: List<CompetencySummary>,
    shareLink: String?,
    onBack: () -> Unit,
    onCreate: (Int, List<String>) -> Unit
) {
    var expiryDays by remember { mutableStateOf("30") }
    val selections = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = {
                val days = expiryDays.toIntOrNull() ?: 30
                val selected = selections.filter { it.value }.keys.toList()
                onCreate(days, selected)
            }) { Text("Generate") }
        }
        OutlinedTextField(value = expiryDays, onValueChange = { expiryDays = it }, label = { Text("Expiry days") })
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(competencies) { competency ->
                val checked = selections[competency.id] ?: false
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { selections[competency.id] = it })
                    Text(competency.title)
                }
            }
        }
        if (!shareLink.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Share link:")
            Text(shareLink)
            Button(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Share link", shareLink))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Text("Copy")
            }
        }
    }
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
}

class MsalAuthManager(private val context: Context) {
    interface AuthCallback {
        fun onSuccess(result: IAuthenticationResult)
        fun onError(error: MsalException)
    }

    private var app: ISingleAccountPublicClientApplication? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    app = application
                }

                override fun onError(exception: MsalException) {
                }
            }
        )
    }

    fun signIn(activity: ComponentActivity, callback: AuthCallback) {
        val scopes = arrayOf(BuildConfig.API_SCOPE)
        val currentApp = app ?: return
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
}

class ApiClient(private val tokenStore: TokenStore) {
    private val moshi = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = tokenStore.accessToken
        val newRequest = if (!token.isNullOrBlank()) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
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
    suspend fun uploadEvidence(@Path("id") id: String, @Part file: MultipartBody.Part): Evidence

    @POST("sharepacks")
    suspend fun createSharePack(@Body request: SharePackCreateRequest): SharePackResponse
}

data class CompetencySummary(
    val id: String,
    val title: String,
    val description: String?,
    val expiresAt: String,
    val status: String,
    val evidenceCount: Int
)

data class CompetencyDetail(
    val id: String,
    val title: String,
    val description: String?,
    val expiresAt: String,
    val status: String,
    val evidence: List<Evidence>
)

data class Evidence(
    val id: String,
    val fileName: String,
    val contentType: String?,
    val size: Long,
    val uploadedAt: String
)

data class CompetencyUpsertRequest(
    val title: String,
    val description: String?,
    val expiresAt: Date
)

data class SharePackCreateRequest(
    val expiryDays: Int,
    val competencyIds: List<String>
)

data class SharePackResponse(
    val token: String,
    val expiresAt: String,
    val shareUrl: String
)

suspend fun createMultipartFromUri(context: Context, uri: Uri): MultipartBody.Part {
    return withContext(Dispatchers.IO) {
        val fileName = "upload_${System.currentTimeMillis()}"
        val temp = File.createTempFile("upload", null, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        val body = temp.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", fileName, body)
    }
}
