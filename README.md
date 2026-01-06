# CompetencyPassport

CompetencyPassport is an Android-first MVP that stores competencies and evidence in Azure, authenticates with Microsoft Entra ID (MSAL), and produces time-limited Share Pack links with a public read-only viewer.

## Architecture
- **Android app**: Kotlin + Jetpack Compose + MSAL for Entra ID login.
- **Backend**: ASP.NET Core .NET 8 Minimal API + EF Core + Azure SQL + Azure Blob Storage.
- **Share viewer**: Static HTML/JS app calling the public share endpoints.
- **Infra**: Bicep to provision Azure resources.

## Prerequisites
- Azure CLI (`az`)
- .NET SDK 8
- Android Studio (for Android SDK)
- Azure subscription and Entra ID tenant

## Provision Azure (Bicep)
```powershell
az login
az account set --subscription <SUBSCRIPTION_ID>

./infra/deploy.ps1 \
  -ResourceGroupName <RG_NAME> \
  -Location <AZURE_REGION> \
  -SqlAdminLogin <SQL_ADMIN_LOGIN> \
  -SqlAdminPassword <SQL_ADMIN_PASSWORD> \
  -ApiAppName <API_APP_NAME> \
  -ShareAppName <SHARE_APP_NAME> \
  -StorageAccountName <STORAGE_ACCOUNT_NAME> \
  -AppInsightsName <APP_INSIGHTS_NAME> \
  -SqlServerName <SQL_SERVER_NAME>
```

The deployment creates:
- Azure SQL Server + Database
- Storage Account + `evidence` container
- App Service Plan
- API Web App (with system-assigned Managed Identity)
- Share Viewer Web App
- Application Insights

## Register Entra ID apps
### 1) API App Registration
1. Azure Portal ? Entra ID ? App registrations ? New registration.
2. Single tenant.
3. Set **Application ID URI** to `api://<API_APP_CLIENT_ID>`.
4. Expose an API ? Add scope `access_as_user`.
5. Copy **Tenant ID** and **Application (client) ID**.

### 2) Android App Registration
1. New registration (public client).
2. Add redirect URI: `msauth://com.competencypassport/<YOUR_SIGNATURE_HASH>`.
3. API permissions ? add the API scope `access_as_user`.
4. Copy **Application (client) ID**.

## Backend configuration
Set App Service configuration (or local environment variables):
```
AzureAd__TenantId=<TENANT_ID>
AzureAd__Audience=api://<API_APP_CLIENT_ID>
Sql__ConnectionString=Server=tcp:<SQL_SERVER_NAME>.database.windows.net,1433;Initial Catalog=competencypassport;Persist Security Info=False;User ID=<SQL_ADMIN_LOGIN>;Password=<SQL_ADMIN_PASSWORD>;MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;
Storage__AccountName=<STORAGE_ACCOUNT>
Storage__ContainerName=evidence
ShareViewer__BaseUrl=https://<SHARE_VIEWER_HOST>
```

Notes:
- Storage access uses Managed Identity when `Storage__ConnectionString` is not set.
- SQL currently uses a connection string for MVP; you can later move to AAD auth.

### Run backend locally
```powershell
cd backend
# Update backend/appsettings.json with local values or set env vars
dotnet ef database update
dotnet run
```

## Share viewer
1. Update `share-viewer/config.js`:
```js
window.SHARE_API_BASE = "https://<YOUR_API_HOST>";
```
2. Deploy the `share-viewer/` folder to the Share Viewer Web App (zip deploy or copy files).

## Android app setup
1. Update Android config placeholders:
- `android/app/src/main/res/raw/msal_config.json`
- `android/app/build.gradle.kts` (`API_BASE_URL`, `API_SCOPE`)
2. Build and run:
```powershell
cd android
./gradlew assembleDebug
```

## API Endpoints
Authenticated:
- `GET /me`
- `GET /competencies`
- `POST /competencies`
- `GET /competencies/{id}`
- `PUT /competencies/{id}`
- `POST /competencies/{id}/evidence`
- `POST /sharepacks`

Public:
- `GET /share/{token}`
- `GET /share/{token}/download/{evidenceId}`

## CI/CD
GitHub Actions builds:
- .NET backend
- Android debug APK

Optional deployment steps can be added once app settings and service principals are defined.
