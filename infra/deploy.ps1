param(
  [Parameter(Mandatory=$true)][string]$ResourceGroupName,
  [Parameter(Mandatory=$true)][string]$Location,
  [Parameter(Mandatory=$true)][string]$SqlAdminLogin,
  [Parameter(Mandatory=$true)][string]$SqlAdminPassword,
  [Parameter(Mandatory=$true)][string]$ApiAppName,
  [Parameter(Mandatory=$true)][string]$ShareAppName,
  [Parameter(Mandatory=$true)][string]$StorageAccountName,
  [Parameter(Mandatory=$true)][string]$AppInsightsName,
  [Parameter(Mandatory=$true)][string]$SqlServerName,
  [string]$AadAdminObjectId = '',
  [string]$AadAdminLogin = ''
)

az group create --name $ResourceGroupName --location $Location | Out-Null

az deployment group create `
  --resource-group $ResourceGroupName `
  --template-file .\infra\main.bicep `
  --parameters resourceGroupName=$ResourceGroupName `
               location=$Location `
               sqlAdminLogin=$SqlAdminLogin `
               sqlAdminPassword=$SqlAdminPassword `
               apiAppName=$ApiAppName `
               shareAppName=$ShareAppName `
               storageAccountName=$StorageAccountName `
               appInsightsName=$AppInsightsName `
               sqlServerName=$SqlServerName `
               aadAdminObjectId=$AadAdminObjectId `
               aadAdminLogin=$AadAdminLogin
