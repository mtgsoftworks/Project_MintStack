param(
    [ValidateSet("dev", "prod")]
    [string]$Mode = "dev",
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
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

if (-not (Test-Path -Path $BackupFile -PathType Leaf)) {
    throw "Backup file not found: $BackupFile"
}

$resolvedFile = (Resolve-Path -Path $BackupFile).Path
Write-Host "Restoring backup file: $resolvedFile"
Write-Host "Target: container=$ContainerName db=$DbName user=$DbUser"

$restoreCmd = "type `"$resolvedFile`" | docker exec -i $ContainerName psql -U $DbUser -d $DbName"
cmd /c $restoreCmd
if ($LASTEXITCODE -ne 0) {
    throw "Restore failed with exit code $LASTEXITCODE"
}

Write-Host "Restore completed."
