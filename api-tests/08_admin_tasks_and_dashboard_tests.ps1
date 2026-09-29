[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 08. Taches, Dashboard & Reporting (ROLE_ADMIN)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   08. TESTS API : GESTION DES TACHES & DASHBOARD (ROLE_ADMIN)   " -ForegroundColor Cyan
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

# --- 1. Connexion Administrateur ---
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

$adminUserId = $loginRes.userId

# --- 2. Création / Récupération du collaborateur Aboubacar ---
$aboubacarEmail = "aboubacar.traore@taskmanager.com"
$aboubacarPassword = "UserPassword123!"
$aboubacarLogin = @{ email = $aboubacarEmail; password = $aboubacarPassword } | ConvertTo-Json

try {
    $abouRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $aboubacarLogin -ContentType "application/json; charset=utf-8"
    $aboubacarId = $abouRes.userId
} catch {
    $createAbou = @{
        firstname = "Aboubacar"
        lastname  = "Traore"
        email     = $aboubacarEmail
        password  = $aboubacarPassword
        role      = "ROLE_USER"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createAbou -ContentType "application/json; charset=utf-8"
    $abouRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $aboubacarLogin -ContentType "application/json; charset=utf-8"
    $aboubacarId = $abouRes.userId
}

$aboubacarHeaders = @{
    "Authorization" = "Bearer $($abouRes.token)"
    "Content-Type"  = "application/json; charset=utf-8"
}

# --- 3. Création / Récupération du second collaborateur Fatou ---
$fatouEmail = "fatou.diop@taskmanager.com"
$fatouPassword = "UserPassword123!"
$fatouLogin = @{ email = $fatouEmail; password = $fatouPassword } | ConvertTo-Json

try {
    $fatouRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $fatouLogin -ContentType "application/json; charset=utf-8"
    $fatouId = $fatouRes.userId
} catch {
    $createFatou = @{
        firstname = "Fatou"
        lastname  = "Diop"
        email     = $fatouEmail
        password  = $fatouPassword
        role      = "ROLE_USER"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createFatou -ContentType "application/json; charset=utf-8"
    $fatouRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $fatouLogin -ContentType "application/json; charset=utf-8"
    $fatouId = $fatouRes.userId
}

# --- 4. Création d'un projet pour les tests ---
$projName = "Projet Reporting-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)
$projBody = @{
    name        = $projName
    description = "Projet dedie a la validation des taches admin et dashboards"
} | ConvertTo-Json

$testProject = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects" -Method Post -Headers $adminHeaders -Body $projBody
$testProjectId = $testProject.id

$script:task1Id = $null
$script:task2Id = $null

# [TEST 1] Créer une tâche et l'attribuer à un collaborateur (Aboubacar)
Assert-Test "Creer une tache et l'attribuer a Aboubacar (POST /api/admin/tasks)" {
    $body = @{
        title         = "Audit de securite API"
        description   = "Verification des endpoints sensibles et du chiffrement"
        priority      = "HAUTE"
        dueDate       = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
        projectId     = $testProjectId
        assigneeIds   = @($aboubacarId)
    } | ConvertTo-Json

    $task = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks" -Method Post -Headers $adminHeaders -Body $body
    $script:task1Id = $task.id
    if (-not $task.id -or -not ($task.assigneeIds -contains $aboubacarId)) {
        throw "La tache n'a pas ete assignee a Aboubacar."
    }
    Write-Host "     Tache creee ID: $($task.id), Titre: $($task.title), Assignes: $($task.assigneeIds -join ',')" -ForegroundColor Gray
}

# [TEST 2] Créer une tâche et l'attribuer à plusieurs collaborateurs (Aboubacar + Admin lui-même)
Assert-Test "Attribuer une tache a plusieurs collaborateurs dont l'admin (POST /api/admin/tasks)" {
    $body = @{
        title         = "Refonte Architecture Microservices"
        description   = "Travail collaboratif entre Aboubacar et l'Administrateur"
        priority      = "HAUTE"
        dueDate       = (Get-Date).AddDays(14).ToString("yyyy-MM-dd")
        projectId     = $testProjectId
        assigneeIds   = @($aboubacarId, $adminUserId)
    } | ConvertTo-Json

    $task = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks" -Method Post -Headers $adminHeaders -Body $body
    $script:task2Id = $task.id
    if ($task.assigneeIds.Count -lt 2) {
        throw "La tache devrait avoir au moins 2 collaborateurs assignes."
    }
    Write-Host "     Tache creee ID: $($task.id), Nombre d'assignes: $($task.assigneeIds.Count)" -ForegroundColor Gray
}

# [TEST 3] Réaffecter une tâche d'un membre à un autre (de Aboubacar à Fatou)
Assert-Test "Reaffecter une tache d'un membre a un autre (PUT /api/admin/tasks/{id}/reassign)" {
    $reassignBody = @{
        assigneeIds = @($fatouId)
        reason      = "Reaffectation suite a reallocation de charge de travail"
    } | ConvertTo-Json

    $reassigned = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks/$script:task1Id/reassign" -Method Put -Headers $adminHeaders -Body $reassignBody
    if (-not ($reassigned.assigneeIds -contains $fatouId) -or ($reassigned.assigneeIds -contains $aboubacarId)) {
        throw "La reaffectation a echoue : Fatou n'est pas l'unique nouvelle assignee."
    }
    Write-Host "     Tache ID $script:task1Id reaffectee a Fatou (ID $fatouId) avec succes." -ForegroundColor Gray
}

# [TEST 4] Modifier l'échéance (Due date) d'une tâche
Assert-Test "Modifier l'echeance d'une tache (PATCH /api/admin/tasks/{id}/due-date)" {
    $newDate = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
    $dueBody = @{ dueDate = $newDate } | ConvertTo-Json

    $updated = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks/$script:task1Id/due-date" -Method Patch -Headers $adminHeaders -Body $dueBody
    if ($updated.dueDate -ne $newDate) {
        throw "L'echeance n'a pas ete modifiee (attendu: $newDate, recu: $($updated.dueDate))"
    }
    Write-Host "     Nouvelle echeance confirmee : $($updated.dueDate)" -ForegroundColor Gray
}

# [TEST 5] Clôturer définitivement un ticket (statut CLOTURE)
Assert-Test "Cloturer definitivement un ticket (PATCH /api/admin/tasks/{id}/close)" {
    $closeBody = @{
        resolutionNote = "Resolution validee et certifiee par le Responsable."
    } | ConvertTo-Json

    $closed = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks/$script:task1Id/close" -Method Patch -Headers $adminHeaders -Body $closeBody
    if ($closed.status -ne "CLOTURE" -or [string]::IsNullOrEmpty($closed.closedAt)) {
        throw "Le ticket n'est pas marque comme CLOTURE ou closedAt est vide."
    }
    Write-Host "     Ticket cloture ID $($closed.id), Statut: $($closed.status), ClosedAt: $($closed.closedAt)" -ForegroundColor Gray
}

# [TEST 6] Filtrer les tâches par statut clôturé
Assert-Test "Filtrer les taches par statut CLOTURE (GET /api/admin/tasks?status=CLOTURE)" {
    $closedTasks = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks?status=CLOTURE" -Method Get -Headers $adminHeaders
    $found = $closedTasks | Where-Object { $_.id -eq $script:task1Id }
    if (-not $found) {
        throw "La tache cloturee n'a pas ete retrouvee dans le filtre CLOTURE."
    }
    Write-Host "     Taches cloturees trouvees : $($closedTasks.Count)" -ForegroundColor Gray
}

# [TEST 7] Consulter les métriques globales et la charge par collaborateur
Assert-Test "Consulter les metriques globales du dashboard (GET /api/admin/dashboard/metrics)" {
    $metrics = Invoke-RestMethod -Uri "$baseUrl/api/admin/dashboard/metrics" -Method Get -Headers $adminHeaders
    if ($metrics.totalTasks -lt 2) {
        throw "Nombre total de taches incoherent dans les metriques."
    }
    Write-Host "     --- SYNTHESE DES METRIQUES ---" -ForegroundColor Gray
    Write-Host "     Total Taches      : $($metrics.totalTasks)" -ForegroundColor Gray
    Write-Host "     Tickets Clotures  : $($metrics.closedTickets)" -ForegroundColor Gray
    Write-Host "     Taches en Retard  : $($metrics.overdueTasksCount)" -ForegroundColor Gray
    Write-Host "     Taux d'Achevement : $($metrics.completionRate) %" -ForegroundColor Gray
    Write-Host "     Total Projets     : $($metrics.totalProjects)" -ForegroundColor Gray
    Write-Host "     Total Utilisateurs: $($metrics.totalUsers)" -ForegroundColor Gray

    if ($metrics.collaboratorWorkloads.Count -eq 0) {
        throw "Aucune donnee de charge de travail retournee."
    }
    Write-Host "     --- CHARGE DE TRAVAIL PAR COLLABORATEUR ---" -ForegroundColor Gray
    foreach ($w in $metrics.collaboratorWorkloads) {
        Write-Host "     * $($w.fullName) ($($w.email)) : $($w.assignedTasksCount) taches assignees ($($w.workloadPercentage) % de la charge)" -ForegroundColor Gray
    }
}

# [TEST 8] Exporter le rapport d'activité au format CSV
Assert-Test "Exporter le rapport d'activite en CSV (GET /api/admin/reports/export/csv)" {
    $csvResponse = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/api/admin/reports/export/csv" -Method Get -Headers $adminHeaders
    $csvContent = if ($csvResponse.Content -is [string]) { $csvResponse.Content } else { [System.Text.Encoding]::UTF8.GetString($csvResponse.Content) }

    if (-not $csvContent.Contains("Titre") -or -not $csvContent.Contains("Statut")) {
        throw "Le contenu du fichier CSV est invalide ou incomplet."
    }
    $lineCount = ($csvContent -split "`n").Count
    Write-Host "     Fichier CSV genere avec succes : $lineCount lignes exportees." -ForegroundColor Gray
}

# [TEST 9] Exporter le rapport d'activité au format PDF
Assert-Test "Exporter le rapport d'activite en PDF (GET /api/admin/reports/export/pdf)" {
    $pdfResponse = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/api/admin/reports/export/pdf" -Method Get -Headers $adminHeaders
    $bytes = $pdfResponse.Content
    # Verification de la signature magique PDF (%PDF-)
    $magic = [System.Text.Encoding]::ASCII.GetString($bytes, 0, 5)

    if ($magic -ne "%PDF-") {
        throw "Le fichier retourne n'est pas un document PDF valide (signature: $magic)."
    }
    Write-Host "     Rapport PDF valide genere avec succes (Taille: $($bytes.Length) octets)." -ForegroundColor Gray
}

# [TEST 10] Sécurité RBAC : Vérifier que ROLE_USER ne peut pas accéder aux métriques d'admin
Assert-Test "Securite RBAC : Refus d'acces aux metriques admin pour ROLE_USER (Attendu: 403)" {
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/dashboard/metrics" -Method Get -Headers $aboubacarHeaders
        throw "Un utilisateur standard a pu acceder aux metriques confidentielles d'administration !"
    } catch {
        Write-Host "     Acces correctement refuse conformement a la politique RBAC (403 Forbidden)." -ForegroundColor Gray
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS TACHES & DASHBOARD : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
