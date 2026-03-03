param(
    [ValidateSet("dev", "prod")]
    [string]$Mode = "dev",
    [string]$BackupDir = "./backups",
    [int]$RetentionDays = 7
)

$ErrorActionPreference = "Stop"

switch ($Mode) {
    "dev" {
        $ContainerName = "mintstack-postgres"
        $DbName = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) { "mintstack_finance" } else { $env:POSTGRES_DB }
        $DbUser = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) { "mintstack" } else { $env:POSTGRES_USER }
    }
    "prod" {
        $ContainerName = "mintstack-postgres-prod"
        $DbName = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) { "mintstack_finance" } else { $env:POSTGRES_DB }
        $DbUser = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) { "mintstack" } else { $env:POSTGRES_USER }
    }
}

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $BackupDir "${Mode}_${DbName}_${timestamp}.sql"

Write-Host "Creating backup: $backupFile"
$backupCmd = "docker exec $ContainerName pg_dump -U $DbUser $DbName > `"$backupFile`""
cmd /c $backupCmd
if ($LASTEXITCODE -ne 0) {
    throw "Backup failed with exit code $LASTEXITCODE"
}

Write-Host "Backup completed: $backupFile"
Write-Host "Cleaning backups older than $RetentionDays days in $BackupDir"
Get-ChildItem -Path $BackupDir -File -Filter "${Mode}_${DbName}_*.sql" `
| Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } `
| Remove-Item -Force

Write-Host "Retention cleanup completed."
