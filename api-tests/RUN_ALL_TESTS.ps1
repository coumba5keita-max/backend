[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "SUITE DE TESTS API COMPLETE - TaskManager"

Clear-Host
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "                 SUITE DE TESTS API COMPLETE - TASKMANAGER                      " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "Verification de l'etat du serveur http://localhost:8080..." -ForegroundColor Gray

# Verification de la disponibilite du backend
try {
    $null = Invoke-RestMethod -Uri "http://localhost:8080/api/projects" -Method Get -TimeoutSec 5
    Write-Host "Serveur operationnel et pret a recevoir les requetes !" -ForegroundColor Green
} catch {
    Write-Host "ERREUR : Le serveur Spring Boot ne repond pas sur http://localhost:8080." -ForegroundColor Red
    Write-Host "Veuillez verifier que Docker est demarre ('docker compose up -d')." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
$scriptsDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$suites = @(
    @{ File = "01_auth_tests.ps1"; Name = "01. Authentification & Comptes" },
    @{ File = "02_admin_users_tests.ps1"; Name = "02. Gestion des Comptes (ROLE_ADMIN)" },
    @{ File = "03_admin_teams_tests.ps1"; Name = "03. Gestion des Equipes (ROLE_ADMIN)" },
    @{ File = "04_admin_projects_tests.ps1"; Name = "04. Projets & Membres (ROLE_ADMIN)" },
    @{ File = "05_tasks_tests.ps1"; Name = "05. Gestion des Taches" },
    @{ File = "06_notifications_tests.ps1"; Name = "06. Notifications" }
)

$results = @()
$startTime = Get-Date

foreach ($suite in $suites) {
    Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkCyan
    Write-Host " EXECUTION : $($suite.Name)" -ForegroundColor Cyan
    Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkCyan
    
    $scriptPath = Join-Path $scriptsDir $suite.File
    $suiteStartTime = Get-Date
    
    try {
        & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptPath
        $exitCode = $LASTEXITCODE
        $status = if ($exitCode -eq 0) { "SUCCES" } else { "AVERTISSEMENT" }
    } catch {
        $status = "ECHEC"
    }
    
    $elapsed = [Math]::Round(((Get-Date) - $suiteStartTime).TotalSeconds, 1)
    $results += [PSCustomObject]@{
        Suite   = $suite.Name
        Statut  = $status
        Duree   = "$elapsed s"
    }
    Write-Host ""
}

$totalElapsed = [Math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "                      TABLEAU RECAPITULATIF GENERAL                             " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
$results | Format-Table -AutoSize

Write-Host "Temps total d'execution : $totalElapsed secondes" -ForegroundColor Gray
Write-Host "================================================================================" -ForegroundColor Cyan
