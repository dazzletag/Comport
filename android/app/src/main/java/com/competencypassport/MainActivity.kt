
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
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.core.content.ContextCompat
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
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.SignOutCallback
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
import retrofit2.http.DELETE
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
    var revalidationScreen by remember { mutableStateOf(RevalidationScreen.Dashboard) }
    var revalidationSummary by remember { mutableStateOf<RevalidationSummary?>(null) }
    var practiceHours by remember { mutableStateOf<List<PracticeHour>>(emptyList()) }
    var cpdEntries by remember { mutableStateOf<List<CpdEntry>>(emptyList()) }
    var feedbackEntries by remember { mutableStateOf<List<FeedbackEntry>>(emptyList()) }
    var reflectionEntries by remember { mutableStateOf<List<ReflectionEntry>>(emptyList()) }
    var discussion by remember { mutableStateOf<Discussion?>(null) }
    var declarations by remember { mutableStateOf<Declaration?>(null) }
    var isExporting by remember { mutableStateOf(false) }

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

    fun loadRevalidationData() {
        scope.launch {
            errorMessage = null
            try {
                refreshTokenIfNeeded(authManager, tokenStore)?.let { tokenStore.save(it) }
                revalidationSummary = api.getRevalidationSummary()
                practiceHours = api.getPracticeHours()
                cpdEntries = api.getCpdEntries()
                feedbackEntries = api.getFeedbackEntries()
                reflectionEntries = api.getReflections()
                discussion = api.getDiscussion()
                declarations = api.getDeclarations()
            } catch (ex: Exception) {
                errorMessage = "Failed to load revalidation data."
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
                        MainTab.Revalidation -> when (revalidationScreen) {
                            RevalidationScreen.Dashboard -> "Revalidation"
                            RevalidationScreen.PracticeHours -> "Practice hours"
                            RevalidationScreen.CpdLog -> "CPD log"
                            RevalidationScreen.Feedback -> "Practice feedback"
                            RevalidationScreen.Reflections -> "Reflective accounts"
                            RevalidationScreen.Discussions -> "Discussions"
                            RevalidationScreen.Declarations -> "Declarations"
                            RevalidationScreen.Export -> "Evidence pack"
                        }
                        MainTab.Profile -> "Professional Profile"
                        MainTab.References -> "Quick References"
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(topBarTitle) },
                                navigationIcon = if (
                                    (activeTab == MainTab.Passport && passportScreen != PassportScreen.List) ||
                                    (activeTab == MainTab.Revalidation && revalidationScreen != RevalidationScreen.Dashboard)
                                ) {
                                    {
                                        IconButton(onClick = {
                                            if (activeTab == MainTab.Passport) {
                                                passportScreen = PassportScreen.List
                                                loadCompetencies()
                                            } else {
                                                revalidationScreen = RevalidationScreen.Dashboard
                                            }
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
                                    selected = activeTab == MainTab.Revalidation,
                                    onClick = {
                                        activeTab = MainTab.Revalidation
                                        revalidationScreen = RevalidationScreen.Dashboard
                                        loadRevalidationData()
                                    },
                                    icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = "Revalidation") },
                                    label = { Text("Revalidation") }
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
                                                    },
                                                    onDeleteEvidence = { evidenceId ->
                                                        scope.launch {
                                                            try {
                                                                api.deleteEvidence(detail.id, evidenceId)
                                                                selectedCompetency = api.getCompetency(detail.id)
                                                            } catch (ex: Exception) {
                                                                errorMessage = "Failed to delete evidence."
                                                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
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
                                                onSave = { title, description, achieved, expiry, category, pendingEvidence ->
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
                                                            val saved = selectedCompetency
                                                            if (saved != null && pendingEvidence.isNotEmpty()) {
                                                                pendingEvidence.forEach { pending ->
                                                                    val filePart = createMultipartFromUri(context, pending.uri)
                                                                    val notePart = pending.note?.trim()
                                                                        ?.takeIf { it.isNotBlank() }
                                                                        ?.toRequestBody("text/plain".toMediaTypeOrNull())
                                                                    api.uploadEvidence(saved.id, filePart, notePart)
                                                                }
                                                                selectedCompetency = api.getCompetency(saved.id)
                                                            }
                                                            passportScreen = PassportScreen.Detail
                                                        } catch (ex: HttpException) {
                                                            if (ex.code() == 401) {
                                                                authManager.signOut()
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
                                                },
                                                onDeleteEvidence = { competencyId, evidenceId ->
                                                    scope.launch {
                                                        try {
                                                            api.deleteEvidence(competencyId, evidenceId)
                                                            selectedCompetency = api.getCompetency(competencyId)
                                                        } catch (ex: Exception) {
                                                            errorMessage = "Failed to delete evidence."
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
                                MainTab.Revalidation -> {
                                    RevalidationHost(
                                        summary = revalidationSummary,
                                        practiceHours = practiceHours,
                                        cpdEntries = cpdEntries,
                                        feedbackEntries = feedbackEntries,
                                        reflectionEntries = reflectionEntries,
                                        discussion = discussion,
                                        declarations = declarations,
                                        isExporting = isExporting,
                                        onRefresh = { loadRevalidationData() },
                                        onNavigate = { revalidationScreen = it },
                                        currentScreen = revalidationScreen,
                                        onAddPracticeHour = { request ->
                                            scope.launch {
                                                try {
                                                    api.createPracticeHour(request)
                                                    practiceHours = api.getPracticeHours()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to add practice hours."
                                                }
                                            }
                                        },
                                        onDeletePracticeHour = { id ->
                                            scope.launch {
                                                try {
                                                    api.deletePracticeHour(id)
                                                    practiceHours = api.getPracticeHours()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to delete practice hours."
                                                }
                                            }
                                        },
                                        onAddCpd = { request ->
                                            scope.launch {
                                                try {
                                                    api.createCpdEntry(request)
                                                    cpdEntries = api.getCpdEntries()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to add CPD entry."
                                                }
                                            }
                                        },
                                        onDeleteCpd = { id ->
                                            scope.launch {
                                                try {
                                                    api.deleteCpdEntry(id)
                                                    cpdEntries = api.getCpdEntries()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to delete CPD entry."
                                                }
                                            }
                                        },
                                        onUploadCpdEvidence = { id, uri ->
                                            scope.launch {
                                                try {
                                                    val filePart = createMultipartFromUri(context, uri)
                                                    api.uploadCpdEvidence(id, filePart)
                                                    cpdEntries = api.getCpdEntries()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to upload CPD evidence."
                                                }
                                            }
                                        },
                                        onDeleteCpdEvidence = { id ->
                                            scope.launch {
                                                try {
                                                    api.deleteCpdEvidence(id)
                                                    cpdEntries = api.getCpdEntries()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to remove CPD evidence."
                                                }
                                            }
                                        },
                                        onAddFeedback = { request ->
                                            scope.launch {
                                                try {
                                                    api.createFeedback(request)
                                                    feedbackEntries = api.getFeedbackEntries()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to add feedback."
                                                }
                                            }
                                        },
                                        onDeleteFeedback = { id ->
                                            scope.launch {
                                                try {
                                                    api.deleteFeedback(id)
                                                    feedbackEntries = api.getFeedbackEntries()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to delete feedback."
                                                }
                                            }
                                        },
                                        onUploadFeedbackEvidence = { id, uri ->
                                            scope.launch {
                                                try {
                                                    val filePart = createMultipartFromUri(context, uri)
                                                    api.uploadFeedbackEvidence(id, filePart)
                                                    feedbackEntries = api.getFeedbackEntries()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to upload feedback evidence."
                                                }
                                            }
                                        },
                                        onDeleteFeedbackEvidence = { id ->
                                            scope.launch {
                                                try {
                                                    api.deleteFeedbackEvidence(id)
                                                    feedbackEntries = api.getFeedbackEntries()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to remove feedback evidence."
                                                }
                                            }
                                        },
                                        onAddReflection = { request ->
                                            scope.launch {
                                                try {
                                                    api.createReflection(request)
                                                    reflectionEntries = api.getReflections()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to add reflection."
                                                }
                                            }
                                        },
                                        onDeleteReflection = { id ->
                                            scope.launch {
                                                try {
                                                    api.deleteReflection(id)
                                                    reflectionEntries = api.getReflections()
                                                    revalidationSummary = api.getRevalidationSummary()
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to delete reflection."
                                                }
                                            }
                                        },
                                        onSaveDiscussion = { request ->
                                            scope.launch {
                                                try {
                                                    discussion = api.updateDiscussion(request)
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to save discussion."
                                                }
                                            }
                                        },
                                        onSaveDeclarations = { request ->
                                            scope.launch {
                                                try {
                                                    declarations = api.updateDeclarations(request)
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to save declarations."
                                                }
                                            }
                                        },
                                        onExportPack = { uri ->
                                            scope.launch {
                                                isExporting = true
                                                try {
                                                    val body = api.downloadRevalidationPack()
                                                    context.contentResolver.openOutputStream(uri)?.use { output ->
                                                        body.byteStream().use { input ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                } catch (ex: Exception) {
                                                    errorMessage = "Failed to generate evidence pack."
                                                } finally {
                                                    isExporting = false
                                                }
                                            }
                                        }
                                    )
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
    Revalidation,
    Profile,
    References
}

enum class RevalidationScreen {
    Dashboard,
    PracticeHours,
    CpdLog,
    Feedback,
    Reflections,
    Discussions,
    Declarations,
    Export
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
    onUpload: (Uri, String?) -> Unit,
    onDeleteEvidence: (String) -> Unit
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val cameraUriState = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUriState.value
        if (success && uri != null) {
            onUpload(uri, note)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        val uri = cameraUriState.value
        if (granted && uri != null) {
            cameraLauncher.launch(uri)
        } else if (!granted) {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
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

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
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
                            val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                competency.evidence.forEach { evidence ->
                    EvidenceCard(competency.id, evidence, accessToken, onDeleteEvidence)
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
    onSave: (String, String?, Date, Date, String, List<PendingEvidence>) -> Unit,
    onDeleteEvidence: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var achievedDate by remember { mutableStateOf(parseLocalDate(initial?.achievedAt)) }
    var expiryDate by remember { mutableStateOf(parseLocalDate(initial?.expiresAt)) }
    var category by remember { mutableStateOf(initial?.category ?: "Mandatory") }
    var error by remember { mutableStateOf<String?>(null) }
    var evidenceNote by remember { mutableStateOf("") }
    val pendingEvidence = remember { mutableStateListOf<PendingEvidence>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val cameraUriState = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUriState.value
        if (success && uri != null) {
            pendingEvidence.add(PendingEvidence(uri, evidenceNote.ifBlank { null }))
            evidenceNote = ""
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        val uri = cameraUriState.value
        if (granted && uri != null) {
            cameraLauncher.launch(uri)
        } else if (!granted) {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingEvidence.add(PendingEvidence(uri, evidenceNote.ifBlank { null }))
            evidenceNote = ""
        }
    }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingEvidence.add(PendingEvidence(uri, evidenceNote.ifBlank { null }))
            evidenceNote = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Evidence (optional)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = evidenceNote,
                            onValueChange = { evidenceNote = it },
                            label = { Text("Evidence note") },
                            placeholder = { Text("e.g. Signed by Ward Manager") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val uri = createImageUri(context)
                                cameraUriState.value = uri
                                val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
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
                        if (pendingEvidence.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            pendingEvidence.forEachIndexed { index, item ->
                                val isImage = isImageUri(context, item.uri)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isImage) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Description, contentDescription = null)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        getDisplayName(context, item.uri) ?: "Evidence file",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(onClick = { pendingEvidence.removeAt(index) }) {
                                        Icon(Icons.Outlined.Close, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove")
                                    }
                                }
                                item.note?.let { note ->
                                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (initial != null && initial.evidence.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Existing evidence", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            initial.evidence.forEach { evidence ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        evidence.fileName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(onClick = { onDeleteEvidence(initial.id, evidence.id) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
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
                        onSave(title.trim(), description.ifBlank { null }, achieved, expiry, category, pendingEvidence.toList())
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
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Text("Selected competencies", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            competencies.forEach { competency ->
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
    }
}

@Composable
fun RegistrationTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Registration type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        val options = listOf("RN Adult", "RN Mental Health", "RN Learning Disability", "RN Child")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.take(2).forEach { option ->
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
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.drop(2).forEach { option ->
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
    var pinExpiryDate by remember(profile) { mutableStateOf(parseLocalDate(profile?.pinExpiryDate)) }
    var revalidationStart by remember(profile) { mutableStateOf(parseLocalDate(profile?.revalidationCycleStart)) }
    var revalidationEnd by remember(profile) { mutableStateOf(parseLocalDate(profile?.revalidationCycleEnd)) }
    var pushEnabled by remember(profile) { mutableStateOf(profile?.pushNotificationsEnabled ?: false) }
    var emailEnabled by remember(profile) { mutableStateOf(profile?.emailRemindersEnabled ?: false) }
    var reminderCadence by remember(profile) { mutableStateOf(profile?.reminderCadence ?: "Quarterly") }
    val email = profile?.email ?: ""
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Professional identity", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = preferredName, onValueChange = { preferredName = it }, label = { Text("Preferred name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nmcPin, onValueChange = { nmcPin = it }, label = { Text("NMC PIN (confidential, e.g. 99A9999A)") }, modifier = Modifier.fillMaxWidth())
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

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Revalidation cycle", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                DatePickerField(label = "PIN expiry / revalidation due date", date = pinExpiryDate, onDateSelected = { pinExpiryDate = it })
                DatePickerField(label = "Cycle start date", date = revalidationStart, onDateSelected = { revalidationStart = it })
                DatePickerField(label = "Cycle end date", date = revalidationEnd, onDateSelected = { revalidationEnd = it })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                    Text("Push notifications")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = emailEnabled, onCheckedChange = { emailEnabled = it })
                    Text("Email reminders")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reminder cadence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Quarterly", "Monthly").forEach { option ->
                        AssistChip(
                            onClick = { reminderCadence = option },
                            label = { Text(option) },
                            colors = if (reminderCadence == option) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("90/60/30 days").forEach { option ->
                        AssistChip(
                            onClick = { reminderCadence = option },
                            label = { Text(option) },
                            colors = if (reminderCadence == option) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!error.isNullOrBlank()) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val pin = nmcPin.trim()
                val pinValid = Regex("^\\d{2}[A-Za-z]\\d{4}[A-Za-z]$").matches(pin)
                error = when {
                    fullName.isBlank() -> "Full name is required."
                    registrationType.isBlank() -> "Select a registration type."
                    !pinValid -> "NMC PIN must match the 99A9999A format."
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
                            bio.ifBlank { null },
                            pinExpiryDate?.let { localDateToDate(it) },
                            revalidationStart?.let { localDateToDate(it) },
                            revalidationEnd?.let { localDateToDate(it) },
                            pushEnabled,
                            emailEnabled,
                            reminderCadence.ifBlank { null }
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save profile")
        }
    }
}

@Composable
fun ReferenceScreen() {
    val context = LocalContext.current
    val links = listOf(
        QuickLink("NMC Code", "Professional standards of practice and behaviour.", "https://www.nmc.org.uk/standards/code/"),
        QuickLink("NMC Revalidation", "Guidance for three-year revalidation cycles.", "https://www.nmc.org.uk/revalidation/"),
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
fun RevalidationHost(
    summary: RevalidationSummary?,
    practiceHours: List<PracticeHour>,
    cpdEntries: List<CpdEntry>,
    feedbackEntries: List<FeedbackEntry>,
    reflectionEntries: List<ReflectionEntry>,
    discussion: Discussion?,
    declarations: Declaration?,
    isExporting: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (RevalidationScreen) -> Unit,
    currentScreen: RevalidationScreen,
    onAddPracticeHour: (PracticeHourUpsertRequest) -> Unit,
    onDeletePracticeHour: (String) -> Unit,
    onAddCpd: (CpdEntryUpsertRequest) -> Unit,
    onDeleteCpd: (String) -> Unit,
    onUploadCpdEvidence: (String, Uri) -> Unit,
    onDeleteCpdEvidence: (String) -> Unit,
    onAddFeedback: (FeedbackUpsertRequest) -> Unit,
    onDeleteFeedback: (String) -> Unit,
    onUploadFeedbackEvidence: (String, Uri) -> Unit,
    onDeleteFeedbackEvidence: (String) -> Unit,
    onAddReflection: (ReflectionUpsertRequest) -> Unit,
    onDeleteReflection: (String) -> Unit,
    onSaveDiscussion: (DiscussionUpdateRequest) -> Unit,
    onSaveDeclarations: (DeclarationUpdateRequest) -> Unit,
    onExportPack: (Uri) -> Unit
) {
    var pendingCpdEvidenceId by remember { mutableStateOf<String?>(null) }
    var pendingFeedbackEvidenceId by remember { mutableStateOf<String?>(null) }

    val cpdEvidenceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val targetId = pendingCpdEvidenceId
        if (uri != null && targetId != null) {
            onUploadCpdEvidence(targetId, uri)
        }
        pendingCpdEvidenceId = null
    }
    val feedbackEvidenceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val targetId = pendingFeedbackEvidenceId
        if (uri != null && targetId != null) {
            onUploadFeedbackEvidence(targetId, uri)
        }
        pendingFeedbackEvidenceId = null
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            onExportPack(uri)
        }
    }

    when (currentScreen) {
        RevalidationScreen.Dashboard -> {
            RevalidationDashboardScreen(
                summary = summary,
                practiceHours = practiceHours,
                cpdEntries = cpdEntries,
                feedbackEntries = feedbackEntries,
                reflectionEntries = reflectionEntries,
                onNavigate = onNavigate,
                onRefresh = onRefresh
            )
        }
        RevalidationScreen.PracticeHours -> {
            PracticeHoursScreen(
                entries = practiceHours,
                onAdd = onAddPracticeHour,
                onDelete = onDeletePracticeHour
            )
        }
        RevalidationScreen.CpdLog -> {
            CpdLogScreen(
                entries = cpdEntries,
                onAdd = onAddCpd,
                onDelete = onDeleteCpd,
                onUploadEvidence = { id ->
                    pendingCpdEvidenceId = id
                    cpdEvidenceLauncher.launch(
                        arrayOf(
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                onDeleteEvidence = onDeleteCpdEvidence
            )
        }
        RevalidationScreen.Feedback -> {
            FeedbackScreen(
                entries = feedbackEntries,
                onAdd = onAddFeedback,
                onDelete = onDeleteFeedback,
                onUploadEvidence = { id ->
                    pendingFeedbackEvidenceId = id
                    feedbackEvidenceLauncher.launch(
                        arrayOf(
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                onDeleteEvidence = onDeleteFeedbackEvidence
            )
        }
        RevalidationScreen.Reflections -> {
            ReflectionScreen(
                entries = reflectionEntries,
                onAdd = onAddReflection,
                onDelete = onDeleteReflection
            )
        }
        RevalidationScreen.Discussions -> {
            DiscussionScreen(
                discussion = discussion,
                onSave = onSaveDiscussion
            )
        }
        RevalidationScreen.Declarations -> {
            DeclarationScreen(
                declaration = declarations,
                onSave = onSaveDeclarations
            )
        }
        RevalidationScreen.Export -> {
            EvidencePackScreen(
                isExporting = isExporting,
                onGenerate = {
                    val fileName = "revalidation_pack_${LocalDate.now(ZoneOffset.UTC)}.zip"
                    exportLauncher.launch(fileName)
                }
            )
        }
    }
}

@Composable
fun RevalidationDashboardScreen(
    summary: RevalidationSummary?,
    practiceHours: List<PracticeHour>,
    cpdEntries: List<CpdEntry>,
    feedbackEntries: List<FeedbackEntry>,
    reflectionEntries: List<ReflectionEntry>,
    onNavigate: (RevalidationScreen) -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val expiryDate = parseLocalDate(summary?.pinExpiryDate)
    val daysRemaining = expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(ZoneOffset.UTC), it) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Revalidation overview", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    expiryDate?.let { "PIN expiry ${formatLocalDate(it)}" } ?: "PIN expiry date not set",
                    style = MaterialTheme.typography.bodyMedium
                )
                daysRemaining?.let {
                    val status = when {
                        it < 0 -> "Overdue"
                        it <= 30 -> "Due in $it days"
                        it <= 180 -> "Due in ${it / 30} months"
                        else -> "Due in ${it / 30} months"
                    }
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                summary?.cycleStart?.let {
                    Text("Cycle: ${formatDate(it)} to ${formatDate(summary.cycleEnd ?: it)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onRefresh,
                        label = { Text("Refresh data") }
                    )
                    AssistChip(
                        onClick = { onNavigate(RevalidationScreen.Export) },
                        label = { Text("Generate pack") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (summary?.atRisk == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("You may be at risk of missing requirements. Review your CPD and reflections.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SummaryProgressCard(
            title = "Practice hours",
            current = practiceHours.sumOf { it.hours },
            target = summary?.practiceHoursTarget ?: 450.0,
            detail = "Target 450 hours across three years"
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryProgressCard(
            title = "CPD hours",
            current = cpdEntries.sumOf { it.hours },
            target = summary?.cpdTarget ?: 35.0,
            detail = "At least 20 participatory hours",
            secondaryCurrent = cpdEntries.filter { it.isParticipatory }.sumOf { it.hours },
            secondaryLabel = "Participatory"
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryCountCard(
            title = "Practice feedback",
            count = feedbackEntries.size,
            target = summary?.feedbackTarget ?: 5,
            detail = "Five practice-related feedback items"
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryCountCard(
            title = "Reflective accounts",
            count = reflectionEntries.size,
            target = summary?.reflectionTarget ?: 5,
            detail = "Five reflective accounts mapped to the Code"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Evidence modules", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        ModuleNavCard(
            title = "Practice hours",
            subtitle = "Log shifts and total hours",
            trailing = "${practiceHours.sumOf { it.hours }.toInt()}h",
            onClick = { onNavigate(RevalidationScreen.PracticeHours) }
        )
        ModuleNavCard(
            title = "CPD log",
            subtitle = "Structured learning and evidence",
            trailing = "${cpdEntries.size} entries",
            onClick = { onNavigate(RevalidationScreen.CpdLog) }
        )
        ModuleNavCard(
            title = "Practice feedback",
            subtitle = "Compliments, audits, peer feedback",
            trailing = "${feedbackEntries.size} items",
            onClick = { onNavigate(RevalidationScreen.Feedback) }
        )
        ModuleNavCard(
            title = "Reflective accounts",
            subtitle = "Map learning to the NMC Code",
            trailing = "${reflectionEntries.size} entries",
            onClick = { onNavigate(RevalidationScreen.Reflections) }
        )
        ModuleNavCard(
            title = "Discussions",
            subtitle = "Reflective & confirmation discussion",
            trailing = "Complete",
            onClick = { onNavigate(RevalidationScreen.Discussions) }
        )
        ModuleNavCard(
            title = "Declarations",
            subtitle = "Health & character + indemnity",
            trailing = "Review",
            onClick = { onNavigate(RevalidationScreen.Declarations) }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SummaryProgressCard(
    title: String,
    current: Double,
    target: Double,
    detail: String,
    secondaryCurrent: Double? = null,
    secondaryLabel: String? = null
) {
    val progress = if (target > 0) (current / target).coerceIn(0.0, 1.0).toFloat() else 0f
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${current.toInt()} / ${target.toInt()} hours", style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            if (secondaryCurrent != null && secondaryLabel != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("$secondaryLabel: ${secondaryCurrent.toInt()}h", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SummaryCountCard(title: String, count: Int, target: Int, detail: String) {
    val progress = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count / $target complete", style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ModuleNavCard(title: String, subtitle: String, trailing: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PracticeHoursScreen(entries: List<PracticeHour>, onAdd: (PracticeHourUpsertRequest) -> Unit, onDelete: (String) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var role by remember { mutableStateOf("") }
    var setting by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val totalHours = entries.sumOf { it.hours }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        SummaryProgressCard(
            title = "Practice hours",
            current = totalHours,
            target = 450.0,
            detail = "Log practice hours towards your 450 requirement"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add hours", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = date, onDateSelected = { date = it })
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / band") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = setting, onValueChange = { setting = it }, label = { Text("Setting") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours worked") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    val hoursValue = hours.toDoubleOrNull()
                    error = when {
                        date == null -> "Select a date."
                        hoursValue == null || hoursValue <= 0 -> "Enter valid hours."
                        else -> null
                    }
                    if (error == null) {
                        onAdd(
                            PracticeHourUpsertRequest(
                                localDateToDate(date!!),
                                role.ifBlank { null },
                                setting.ifBlank { null },
                                hoursValue!!,
                                notes.ifBlank { null }
                            )
                        )
                        date = null
                        role = ""
                        setting = ""
                        hours = ""
                        notes = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add hours")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logged hours", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        entries.sortedByDescending { parseLocalDate(it.date) ?: LocalDate.MIN }.forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatDate(entry.date), style = MaterialTheme.typography.bodyMedium)
                        Text("${entry.hours} hours", style = MaterialTheme.typography.bodySmall)
                        listOfNotNull(entry.role, entry.setting).takeIf { it.isNotEmpty() }?.let {
                            Text(it.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        entry.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun CpdLogScreen(
    entries: List<CpdEntry>,
    onAdd: (CpdEntryUpsertRequest) -> Unit,
    onDelete: (String) -> Unit,
    onUploadEvidence: (String) -> Unit,
    onDeleteEvidence: (String) -> Unit
) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var topic by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var participatory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val totalHours = entries.sumOf { it.hours }
    val totalParticipatory = entries.filter { it.isParticipatory }.sumOf { it.hours }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        SummaryProgressCard(
            title = "CPD hours",
            current = totalHours,
            target = 35.0,
            detail = "Minimum 20 participatory hours",
            secondaryCurrent = totalParticipatory,
            secondaryLabel = "Participatory"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add CPD entry", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = date, onDateSelected = { date = it })
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = participatory, onCheckedChange = { participatory = it })
                    Text("Participatory learning")
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    val hoursValue = hours.toDoubleOrNull()
                    error = when {
                        date == null -> "Select a date."
                        topic.isBlank() -> "Enter a topic."
                        hoursValue == null || hoursValue <= 0 -> "Enter valid hours."
                        else -> null
                    }
                    if (error == null) {
                        onAdd(
                            CpdEntryUpsertRequest(
                                localDateToDate(date!!),
                                topic.trim(),
                                hoursValue!!,
                                participatory,
                                notes.ifBlank { null }
                            )
                        )
                        date = null
                        topic = ""
                        hours = ""
                        notes = ""
                        participatory = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add CPD entry")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logged CPD", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        entries.sortedByDescending { parseLocalDate(it.date) ?: LocalDate.MIN }.forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.topic, style = MaterialTheme.typography.bodyMedium)
                            Text("${formatDate(entry.date)} • ${entry.hours}h", style = MaterialTheme.typography.bodySmall)
                        }
                        if (entry.isParticipatory) {
                            AssistChip(onClick = {}, label = { Text("Participatory") })
                        }
                    }
                    entry.notes?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (entry.evidenceFileName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.evidenceFileName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                entry.evidenceUploadedAt?.let { Text("Uploaded ${formatDate(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            OutlinedButton(onClick = { onDeleteEvidence(entry.id) }) {
                                Text("Remove")
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { onUploadEvidence(entry.id) }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload evidence")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onDelete(entry.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackScreen(
    entries: List<FeedbackEntry>,
    onAdd: (FeedbackUpsertRequest) -> Unit,
    onDelete: (String) -> Unit,
    onUploadEvidence: (String) -> Unit,
    onDeleteEvidence: (String) -> Unit
) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var source by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val sources = listOf("Patient", "Colleague", "Audit", "Complaint", "Compliment")

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        SummaryCountCard(
            title = "Practice feedback",
            count = entries.size,
            target = 5,
            detail = "Capture five practice-related feedback entries"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add feedback", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = date, onDateSelected = { date = it })
                Text("Source", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.take(3).forEach { option ->
                        AssistChip(
                            onClick = { source = option },
                            label = { Text(option) },
                            colors = if (source == option) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.drop(3).forEach { option ->
                        AssistChip(
                            onClick = { source = option },
                            label = { Text(option) },
                            colors = if (source == option) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    error = when {
                        date == null -> "Select a date."
                        source.isBlank() -> "Select a source."
                        summary.isBlank() -> "Add a short summary."
                        else -> null
                    }
                    if (error == null) {
                        onAdd(
                            FeedbackUpsertRequest(
                                localDateToDate(date!!),
                                source,
                                summary.trim()
                            )
                        )
                        date = null
                        source = ""
                        summary = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add feedback")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Feedback entries", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        entries.sortedByDescending { parseLocalDate(it.date) ?: LocalDate.MIN }.forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.source, style = MaterialTheme.typography.bodyMedium)
                    Text(formatDate(entry.date), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(entry.summary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (entry.evidenceFileName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.evidenceFileName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                entry.evidenceUploadedAt?.let { Text("Uploaded ${formatDate(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            OutlinedButton(onClick = { onDeleteEvidence(entry.id) }) {
                                Text("Remove")
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { onUploadEvidence(entry.id) }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload evidence")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onDelete(entry.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReflectionScreen(entries: List<ReflectionEntry>, onAdd: (ReflectionUpsertRequest) -> Unit, onDelete: (String) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var whatHappened by remember { mutableStateOf("") }
    var whatLearned by remember { mutableStateOf("") }
    var howChanged by remember { mutableStateOf("") }
    var selectedThemes by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val themes = listOf(
        "Prioritise people",
        "Practise effectively",
        "Preserve safety",
        "Promote professionalism and trust"
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(scrollState)) {
        SummaryCountCard(
            title = "Reflective accounts",
            count = entries.size,
            target = 5,
            detail = "Link each reflection to the NMC Code themes"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add reflection", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = date, onDateSelected = { date = it })
                OutlinedTextField(
                    value = whatHappened,
                    onValueChange = { whatHappened = it },
                    label = { Text("What happened") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = whatLearned,
                    onValueChange = { whatLearned = it },
                    label = { Text("What you learned") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = howChanged,
                    onValueChange = { howChanged = it },
                    label = { Text("How this changed your practice") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("NMC Code themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                themes.forEach { theme ->
                    val selected = theme in selectedThemes
                    AssistChip(
                        onClick = {
                            selectedThemes = if (selected) {
                                selectedThemes - theme
                            } else {
                                selectedThemes + theme
                            }
                        },
                        label = { Text(theme) },
                        colors = if (selected) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    error = when {
                        date == null -> "Select a date."
                        whatHappened.isBlank() -> "Describe what happened."
                        whatLearned.isBlank() -> "Describe what you learned."
                        howChanged.isBlank() -> "Describe how practice changed."
                        selectedThemes.isEmpty() -> "Select at least one Code theme."
                        else -> null
                    }
                    if (error == null) {
                        onAdd(
                            ReflectionUpsertRequest(
                                localDateToDate(date!!),
                                whatHappened.trim(),
                                whatLearned.trim(),
                                howChanged.trim(),
                                selectedThemes.joinToString(", ")
                            )
                        )
                        date = null
                        whatHappened = ""
                        whatLearned = ""
                        howChanged = ""
                        selectedThemes = emptySet()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add reflection")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Reflective accounts", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        entries.sortedByDescending { parseLocalDate(it.date) ?: LocalDate.MIN }.forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(formatDate(entry.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(entry.whatHappened, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(entry.whatLearned, style = MaterialTheme.typography.bodySmall)
                    Text(entry.howChanged, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Code themes: ${entry.codeThemes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onDelete(entry.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscussionScreen(discussion: Discussion?, onSave: (DiscussionUpdateRequest) -> Unit) {
    var reflectiveDate by remember(discussion) { mutableStateOf(parseLocalDate(discussion?.reflectiveDiscussionDate)) }
    var registrantName by remember(discussion) { mutableStateOf(discussion?.reflectiveRegistrantName ?: "") }
    var registrantPin by remember(discussion) { mutableStateOf(discussion?.reflectiveRegistrantPin ?: "") }
    var confirmationDate by remember(discussion) { mutableStateOf(parseLocalDate(discussion?.confirmationDate)) }
    var confirmerName by remember(discussion) { mutableStateOf(discussion?.confirmerName ?: "") }
    var confirmerRole by remember(discussion) { mutableStateOf(discussion?.confirmerRole ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Reflective discussion", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = reflectiveDate, onDateSelected = { reflectiveDate = it })
                OutlinedTextField(value = registrantName, onValueChange = { registrantName = it }, label = { Text("Registrant name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = registrantPin, onValueChange = { registrantPin = it }, label = { Text("Registrant NMC PIN") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Confirmation discussion", style = MaterialTheme.typography.titleSmall)
                DatePickerField(label = "Date", date = confirmationDate, onDateSelected = { confirmationDate = it })
                OutlinedTextField(value = confirmerName, onValueChange = { confirmerName = it }, label = { Text("Confirmer name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confirmerRole, onValueChange = { confirmerRole = it }, label = { Text("Confirmer role") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSave(
                    DiscussionUpdateRequest(
                        reflectiveDate?.let { localDateToDate(it) },
                        registrantName.ifBlank { null },
                        registrantPin.ifBlank { null },
                        confirmationDate?.let { localDateToDate(it) },
                        confirmerName.ifBlank { null },
                        confirmerRole.ifBlank { null }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save discussion details")
        }
    }
}

@Composable
fun DeclarationScreen(declaration: Declaration?, onSave: (DeclarationUpdateRequest) -> Unit) {
    var health by remember(declaration) { mutableStateOf(declaration?.healthAndCharacter ?: false) }
    var indemnity by remember(declaration) { mutableStateOf(declaration?.indemnity ?: false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Declarations", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = health, onCheckedChange = { health = it })
                    Text("Health & character declaration completed")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = indemnity, onCheckedChange = { indemnity = it })
                    Text("Professional indemnity confirmed")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSave(DeclarationUpdateRequest(health, indemnity)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save declarations")
        }
    }
}

@Composable
fun EvidencePackScreen(isExporting: Boolean, onGenerate: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Evidence pack builder", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Generate a structured evidence pack suitable for managers, confirmers, or NMC audit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), enabled = !isExporting) {
                    Text(if (isExporting) "Generating..." else "Generate evidence pack")
                }
                if (isExporting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun EvidenceCard(competencyId: String, evidence: Evidence, accessToken: String?, onDeleteEvidence: (String) -> Unit) {
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
            TextButton(onClick = { onDeleteEvidence(evidence.id) }) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
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
    val expiryDate = parseLocalDate(expiresAt)
    if (expiryDate != null) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val days = ChronoUnit.DAYS.between(today, expiryDate)
        return when {
            days < 0 -> "Expired ${kotlin.math.abs(days)} days ago"
            days == 0L -> "Expires today"
            days == 1L -> "Expires in 1 day"
            else -> "Expires in $days days"
        }
    }
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
    val local = parseLocalDate(value)
    if (local != null) {
        return formatLocalDate(local)
    }
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

fun localDateToDate(value: LocalDate): Date {
    return Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant())
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

    suspend fun signOut(): Boolean = suspendCancellableCoroutine { cont ->
        val currentApp = app
        if (currentApp == null) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }
        currentApp.signOut(object : SignOutCallback {
            override fun onSignOut() {
                cont.resume(true)
            }

            override fun onError(exception: MsalException) {
                cont.resume(false)
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

    @DELETE("competencies/{id}/evidence/{evidenceId}")
    suspend fun deleteEvidence(@Path("id") id: String, @Path("evidenceId") evidenceId: String)

    @POST("sharepacks")
    suspend fun createSharePack(@Body request: SharePackCreateRequest): SharePackResponse

    @GET("profile")
    suspend fun getProfile(): NurseProfile

    @PUT("profile")
    suspend fun updateProfile(@Body request: NurseProfileUpdateRequest): NurseProfile

    @GET("revalidation/summary")
    suspend fun getRevalidationSummary(): RevalidationSummary

    @GET("revalidation/practice-hours")
    suspend fun getPracticeHours(): List<PracticeHour>

    @POST("revalidation/practice-hours")
    suspend fun createPracticeHour(@Body request: PracticeHourUpsertRequest): PracticeHour

    @PUT("revalidation/practice-hours/{id}")
    suspend fun updatePracticeHour(@Path("id") id: String, @Body request: PracticeHourUpsertRequest)

    @DELETE("revalidation/practice-hours/{id}")
    suspend fun deletePracticeHour(@Path("id") id: String)

    @GET("revalidation/cpd")
    suspend fun getCpdEntries(): List<CpdEntry>

    @POST("revalidation/cpd")
    suspend fun createCpdEntry(@Body request: CpdEntryUpsertRequest): CpdEntry

    @PUT("revalidation/cpd/{id}")
    suspend fun updateCpdEntry(@Path("id") id: String, @Body request: CpdEntryUpsertRequest)

    @DELETE("revalidation/cpd/{id}")
    suspend fun deleteCpdEntry(@Path("id") id: String)

    @Multipart
    @POST("revalidation/cpd/{id}/evidence")
    suspend fun uploadCpdEvidence(@Path("id") id: String, @Part file: MultipartBody.Part): CpdEntry

    @DELETE("revalidation/cpd/{id}/evidence")
    suspend fun deleteCpdEvidence(@Path("id") id: String)

    @GET("revalidation/feedback")
    suspend fun getFeedbackEntries(): List<FeedbackEntry>

    @POST("revalidation/feedback")
    suspend fun createFeedback(@Body request: FeedbackUpsertRequest): FeedbackEntry

    @PUT("revalidation/feedback/{id}")
    suspend fun updateFeedback(@Path("id") id: String, @Body request: FeedbackUpsertRequest)

    @DELETE("revalidation/feedback/{id}")
    suspend fun deleteFeedback(@Path("id") id: String)

    @Multipart
    @POST("revalidation/feedback/{id}/evidence")
    suspend fun uploadFeedbackEvidence(@Path("id") id: String, @Part file: MultipartBody.Part): FeedbackEntry

    @DELETE("revalidation/feedback/{id}/evidence")
    suspend fun deleteFeedbackEvidence(@Path("id") id: String)

    @GET("revalidation/reflections")
    suspend fun getReflections(): List<ReflectionEntry>

    @POST("revalidation/reflections")
    suspend fun createReflection(@Body request: ReflectionUpsertRequest): ReflectionEntry

    @PUT("revalidation/reflections/{id}")
    suspend fun updateReflection(@Path("id") id: String, @Body request: ReflectionUpsertRequest)

    @DELETE("revalidation/reflections/{id}")
    suspend fun deleteReflection(@Path("id") id: String)

    @GET("revalidation/discussions")
    suspend fun getDiscussion(): Discussion

    @PUT("revalidation/discussions")
    suspend fun updateDiscussion(@Body request: DiscussionUpdateRequest): Discussion

    @GET("revalidation/declarations")
    suspend fun getDeclarations(): Declaration

    @PUT("revalidation/declarations")
    suspend fun updateDeclarations(@Body request: DeclarationUpdateRequest): Declaration

    @GET("revalidation/export")
    suspend fun downloadRevalidationPack(): okhttp3.ResponseBody
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
    val bio: String?,
    val pinExpiryDate: String?,
    val revalidationCycleStart: String?,
    val revalidationCycleEnd: String?,
    val pushNotificationsEnabled: Boolean,
    val emailRemindersEnabled: Boolean,
    val reminderCadence: String?
)

data class NurseProfileUpdateRequest(
    val fullName: String,
    val preferredName: String?,
    val nmcPin: String?,
    val registrationType: String?,
    val employer: String?,
    val roleBand: String?,
    val phone: String?,
    val bio: String?,
    val pinExpiryDate: Date? = null,
    val revalidationCycleStart: Date? = null,
    val revalidationCycleEnd: Date? = null,
    val pushNotificationsEnabled: Boolean = false,
    val emailRemindersEnabled: Boolean = false,
    val reminderCadence: String? = null
)

data class QuickLink(
    val title: String,
    val description: String,
    val url: String
)

data class PendingEvidence(
    val uri: Uri,
    val note: String?
)

data class RevalidationSummary(
    val pinExpiryDate: String?,
    val cycleStart: String?,
    val cycleEnd: String?,
    val practiceHoursTotal: Double,
    val practiceHoursTarget: Double,
    val cpdTotal: Double,
    val cpdParticipatoryTotal: Double,
    val cpdTarget: Double,
    val cpdParticipatoryTarget: Double,
    val feedbackCount: Int,
    val feedbackTarget: Int,
    val reflectionCount: Int,
    val reflectionTarget: Int,
    val atRisk: Boolean
)

data class PracticeHour(
    val id: String,
    val date: String,
    val role: String?,
    val setting: String?,
    val hours: Double,
    val notes: String?
)

data class PracticeHourUpsertRequest(
    val date: Date,
    val role: String?,
    val setting: String?,
    val hours: Double,
    val notes: String?
)

data class CpdEntry(
    val id: String,
    val date: String,
    val topic: String,
    val hours: Double,
    val isParticipatory: Boolean,
    val evidenceFileName: String?,
    val evidenceContentType: String?,
    val evidenceSize: Long?,
    val evidenceUploadedAt: String?,
    val notes: String?
)

data class CpdEntryUpsertRequest(
    val date: Date,
    val topic: String,
    val hours: Double,
    val isParticipatory: Boolean,
    val notes: String?
)

data class FeedbackEntry(
    val id: String,
    val date: String,
    val source: String,
    val summary: String,
    val evidenceFileName: String?,
    val evidenceContentType: String?,
    val evidenceSize: Long?,
    val evidenceUploadedAt: String?
)

data class FeedbackUpsertRequest(
    val date: Date,
    val source: String,
    val summary: String
)

data class ReflectionEntry(
    val id: String,
    val date: String,
    val whatHappened: String,
    val whatLearned: String,
    val howChanged: String,
    val codeThemes: String
)

data class ReflectionUpsertRequest(
    val date: Date,
    val whatHappened: String,
    val whatLearned: String,
    val howChanged: String,
    val codeThemes: String
)

data class Discussion(
    val reflectiveDiscussionDate: String?,
    val reflectiveRegistrantName: String?,
    val reflectiveRegistrantPin: String?,
    val confirmationDate: String?,
    val confirmerName: String?,
    val confirmerRole: String?
)

data class DiscussionUpdateRequest(
    val reflectiveDiscussionDate: Date?,
    val reflectiveRegistrantName: String?,
    val reflectiveRegistrantPin: String?,
    val confirmationDate: Date?,
    val confirmerName: String?,
    val confirmerRole: String?
)

data class Declaration(
    val healthAndCharacter: Boolean,
    val indemnity: Boolean
)

data class DeclarationUpdateRequest(
    val healthAndCharacter: Boolean,
    val indemnity: Boolean
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

fun isImageUri(context: Context, uri: Uri): Boolean {
    val type = context.contentResolver.getType(uri)
    return type?.startsWith("image/") == true
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
