targetScope = 'resourceGroup'

param location string = resourceGroup().location
param resourceGroupName string = resourceGroup().name
param sqlAdminLogin string
@secure()
param sqlAdminPassword string
param aadAdminObjectId string = ''
param aadAdminLogin string = ''
param appServicePlanSku string = 'B1'
param apiAppName string
param shareAppName string
param storageAccountName string
param appInsightsName string
param sqlServerName string
param sqlDbName string = 'competencypassport'
param evidenceContainerName string = 'evidence'

// App Service plan
resource appPlan 'Microsoft.Web/serverfarms@2022-03-01' = {
  name: '${resourceGroupName}-plan'
  location: location
  sku: {
    name: appServicePlanSku
  }
  properties: {
    reserved: false
  }
}

// App Insights
resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
  }
}

// Storage account
resource storage 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-01-01' = {
  parent: storage
  name: 'default'
}

resource evidenceContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobService
  name: evidenceContainerName
  properties: {
    publicAccess: 'None'
  }
}

// SQL server + DB
resource sqlServer 'Microsoft.Sql/servers@2022-05-01-preview' = {
  name: sqlServerName
  location: location
  properties: {
    administratorLogin: sqlAdminLogin
    administratorLoginPassword: sqlAdminPassword
    version: '12.0'
    minimalTlsVersion: '1.2'
    publicNetworkAccess: 'Enabled'
  }
}

resource sqlDb 'Microsoft.Sql/servers/databases@2022-05-01-preview' = {
  parent: sqlServer
  name: sqlDbName
  location: location
  sku: {
    name: 'S0'
    tier: 'Standard'
  }
}

resource sqlFirewall 'Microsoft.Sql/servers/firewallRules@2022-05-01-preview' = {
  parent: sqlServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// Optional AAD admin for SQL
resource sqlAadAdmin 'Microsoft.Sql/servers/administrators@2022-05-01-preview' = if (aadAdminObjectId != '' && aadAdminLogin != '') {
  parent: sqlServer
  name: 'ActiveDirectory'
  properties: {
    administratorType: 'ActiveDirectory'
    login: aadAdminLogin
    sid: aadAdminObjectId
    tenantId: subscription().tenantId
  }
}

// API Web App
resource apiApp 'Microsoft.Web/sites@2022-03-01' = {
  name: apiAppName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: appPlan.id
    httpsOnly: true
    siteConfig: {
      windowsFxVersion: 'DOTNET|8.0'
      appSettings: [
        {
          name: 'APPINSIGHTS_INSTRUMENTATIONKEY'
          value: appInsights.properties.InstrumentationKey
        }
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: appInsights.properties.ConnectionString
        }
        {
          name: 'Storage__AccountName'
          value: storage.name
        }
        {
          name: 'Storage__ContainerName'
          value: evidenceContainerName
        }
        {
          name: 'Sql__Server'
          value: sqlServer.name
        }
        {
          name: 'Sql__Database'
          value: sqlDb.name
        }
      ]
    }
  }
}

// Share Viewer Web App
resource shareApp 'Microsoft.Web/sites@2022-03-01' = {
  name: shareAppName
  location: location
  properties: {
    serverFarmId: appPlan.id
    httpsOnly: true
  }
}

// Role assignment for API -> Storage Blob Data Contributor
resource storageRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storage.id, apiApp.name, 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
  scope: storage
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
    principalId: apiApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

output apiUrl string = 'https://${apiApp.properties.defaultHostName}'
output shareUrl string = 'https://${shareApp.properties.defaultHostName}'
output sqlServerFqdn string = '${sqlServer.name}.database.windows.net'
output storageAccount string = storage.name
