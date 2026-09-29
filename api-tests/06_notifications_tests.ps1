[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 06. Notifications"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "             06. TESTS API : NOTIFICATIONS                       " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Serveur cible : http://localhost:8080" -ForegroundColor Gray
Write-Host ""

$baseUrl = "http://localhost:8080"
$successCount = 0
$totalTests = 0

function Assert-Test {
    param(
        [string]$TestName,
        [scriptblock]$Action
    )
    $script:totalTests++
    Write-Host "[TEST $script:totalTests] $TestName..." -ForegroundColor Yellow
    try {
        & $Action
        Write-Host "  -> SUCCES : Validation reussie !" -ForegroundColor Green
        $script:successCount++
    } catch {
        Write-Host "  -> ECHEC  : $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                Write-Host "  -> Details serveur : $($reader.ReadToEnd())" -ForegroundColor DarkRed
            }
        }
    }
    Write-Host ""
}

# --- Connexion Admin ---
$adminLogin = @{
    email    = "admin@taskmanager.com"
    password = "AdminPassword123!"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json; charset=utf-8"
} catch {
    $createAdmin = @{
        firstname = "Admin"
        lastname  = "Manager"
        email     = "admin@taskmanager.com"
        password  = "AdminPassword123!"
        role      = "ROLE_ADMIN"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createAdmin -ContentType "application/json; charset=utf-8"
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json; charset=utf-8"
}

$adminHeaders = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "Content-Type"  = "application/json; charset=utf-8"
}

# 1. Protection sans authentification
Assert-Test "Protection RBAC : Rejet des requetes non authentifiees (GET /api/notifications)" {
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get
        throw "L'endpoint aurait du rejeter l'acces sans token."
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden) {
            Write-Host "     Rejet 403 Forbidden confirme." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 2. Récupérer les notifications
$script:notifId = $null
Assert-Test "Recuperer mes notifications (GET /api/notifications)" {
    $notifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers $adminHeaders
    Write-Host "     Nombre de notifications pour l'admin : $($notifs.Count)" -ForegroundColor Gray
    if ($notifs.Count -gt 0) {
        $script:notifId = $notifs[0].id
        Write-Host "     Exemple de notification : '$($notifs[0].message)' (Lue: $($notifs[0].isRead))" -ForegroundColor Gray
    }
}

# 3. Marquer une notification comme lue (si une existe ou en créer une via une tâche)
Assert-Test "Marquer une notification comme lue (PATCH /api/notifications/{id}/read)" {
    if (-not $script:notifId) {
        # Créer une tâche pour forcer la création d'une notification
        $projects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get
        $projId = $projects[0].id
        $taskBody = @{
            title       = "Tache pour test notification"
            projectId   = $projId
            assigneeIds = @($loginRes.userId)
        } | ConvertTo-Json
        $newTask = Invoke-RestMethod -Uri "$baseUrl/api/tasks" -Method Post -Headers $adminHeaders -Body $taskBody
        
        $notifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers $adminHeaders
        $script:notifId = $notifs[0].id
    }

    $marked = Invoke-RestMethod -Uri "$baseUrl/api/notifications/$script:notifId/read" -Method Patch -Headers $adminHeaders
    if ($marked.isRead -ne $true) { throw "La notification n'est pas marquee comme lue." }
    Write-Host "     Notification ID $($marked.id) marquee comme lue avec succes (isRead: $($marked.isRead))" -ForegroundColor Gray
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS NOTIFICATIONS : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
