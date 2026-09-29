[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 05. Gestion des Taches"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "             05. TESTS API : GESTION DES TACHES                  " -ForegroundColor Cyan
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

# Récupérer un projet valide
$projects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get
if ($projects.Count -eq 0) {
    $newProj = @{ name = "Projet Taches"; description = "Projet test" } | ConvertTo-Json
    $p = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Body $newProj -ContentType "application/json; charset=utf-8"
    $targetProjectId = $p.id
} else {
    $targetProjectId = $projects[0].id
}

$script:createdTaskId = $null
$taskTitle = "Tache Automatique-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)

# 1. Créer une tâche
Assert-Test "Creer une nouvelle tache avec assignation (POST /api/tasks)" {
    $taskBody = @{
        title         = $taskTitle
        description   = "Implementer la suite de tests automatises et verifier les notifications"
        status        = "A_FAIRE"
        priority      = "HAUTE"
        projectId     = $targetProjectId
        assigneeIds   = @($loginRes.userId)
        estimatedTime = 4.5
        spentTime     = 0.0
    } | ConvertTo-Json

    $task = Invoke-RestMethod -Uri "$baseUrl/api/tasks" -Method Post -Headers $adminHeaders -Body $taskBody
    $script:createdTaskId = $task.id
    if (-not $task.id -or $task.title -ne $taskTitle) { throw "Creation de la tache echouee." }
    Write-Host "     Tache creee : ID $($task.id), Titre: $($task.title), Statut: $($task.status), Priorite: $($task.priority)" -ForegroundColor Gray
}

# 2. Lister toutes les tâches
Assert-Test "Lister toutes les taches (GET /api/tasks)" {
    $tasks = Invoke-RestMethod -Uri "$baseUrl/api/tasks" -Method Get -Headers $adminHeaders
    if ($tasks.Count -eq 0) { throw "La liste des taches est vide." }
    Write-Host "     Nombre total de taches : $($tasks.Count)" -ForegroundColor Gray
}

# 3. Consulter une tâche par ID
Assert-Test "Recuperer une tache par ID (GET /api/tasks/{id})" {
    $task = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:createdTaskId" -Method Get -Headers $adminHeaders
    if ($task.id -ne $script:createdTaskId) { throw "L'identifiant ne correspond pas." }
    Write-Host "     Tache recuperee : ID $($task.id), Titre: $($task.title)" -ForegroundColor Gray
}

# 4. Mettre à jour le statut vers EN_COURS
Assert-Test "Changer le statut vers EN_COURS (PATCH /api/tasks/{id}/status)" {
    $statusBody = @{ status = "EN_COURS" } | ConvertTo-Json
    $updated = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:createdTaskId/status" -Method Patch -Headers $adminHeaders -Body $statusBody
    if ($updated.status -ne "EN_COURS") { throw "Le statut n'est pas EN_COURS." }
    Write-Host "     Statut mis a jour : $($updated.status)" -ForegroundColor Gray
}

# 5. Mettre à jour le statut vers TERMINE
Assert-Test "Changer le statut vers TERMINE (PATCH /api/tasks/{id}/status)" {
    $statusBody = @{ status = "TERMINE" } | ConvertTo-Json
    $updated = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:createdTaskId/status" -Method Patch -Headers $adminHeaders -Body $statusBody
    if ($updated.status -ne "TERMINE") { throw "Le statut n'est pas TERMINE." }
    Write-Host "     Statut mis a jour : $($updated.status)" -ForegroundColor Gray
}

# 6. Supprimer la tâche (par l'Admin)
Assert-Test "Supprimer la tache creee (DELETE /api/tasks/{id})" {
    Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:createdTaskId" -Method Delete -Headers $adminHeaders

    # Vérification que la tâche est supprimée
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:createdTaskId" -Method Get -Headers $adminHeaders
        throw "La tache est toujours accessible apres suppression."
    } catch {
        Write-Host "     Suppression confirmee : la tache n'existe plus." -ForegroundColor Gray
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS GESTION DES TACHES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
