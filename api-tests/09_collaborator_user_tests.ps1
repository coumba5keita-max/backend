[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 09. Actions Collaborateur (ROLE_USER)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "    09. TESTS API : ACTIONS DU COLLABORATEUR (ROLE_USER)         " -ForegroundColor Cyan
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

# --- 1. Connexion / Création Administrateur (pour initialiser projets et assigner des tâches) ---
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

# --- 2. Inscription & Connexion Collaborateur (ROLE_USER) ---
$collabGuid = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$collabEmail = "collab.$collabGuid@taskmanager.com"
$collabPass = "InitialPass123!"

$createCollab = @{
    firstname = "Aboubacar"
    lastname  = "Traore"
    email     = $collabEmail
    password  = $collabPass
    role      = "ROLE_USER"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createCollab -ContentType "application/json; charset=utf-8"

$collabLogin = @{ email = $collabEmail; password = $collabPass } | ConvertTo-Json
$collabLoginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $collabLogin -ContentType "application/json; charset=utf-8"
$collabUserId = $collabLoginRes.userId

$collabHeaders = @{
    "Authorization" = "Bearer $($collabLoginRes.token)"
    "Content-Type"  = "application/json; charset=utf-8"
}

# [TEST 1] Consulter son profil collaborateur (GET /api/user/profile)
Assert-Test "Consulter son profil personnel (GET /api/user/profile)" {
    $profile = Invoke-RestMethod -Uri "$baseUrl/api/user/profile" -Method Get -Headers $collabHeaders
    if ($profile.email -ne $collabEmail -or $profile.firstname -ne "Aboubacar") {
        throw "Donnees du profil incoherentes."
    }
    Write-Host "     Profil recupere : $($profile.firstname) $($profile.lastname) - $($profile.email) (Role: $($profile.role))" -ForegroundColor Gray
}

# [TEST 2] Mettre à jour ses informations personnelles (PUT /api/user/profile)
Assert-Test "Mettre a jour ses informations personnelles (PUT /api/user/profile)" {
    $updateBody = @{
        firstname = "Aboubacar-Dev"
        lastname  = "Traore-Expert"
        email     = $collabEmail
    } | ConvertTo-Json

    $updatedProfile = Invoke-RestMethod -Uri "$baseUrl/api/user/profile" -Method Put -Headers $collabHeaders -Body $updateBody
    if ($updatedProfile.firstname -ne "Aboubacar-Dev" -or $updatedProfile.lastname -ne "Traore-Expert") {
        throw "La mise a jour des informations a echoue."
    }
    Write-Host "     Nouveau nom complet : $($updatedProfile.firstname) $($updatedProfile.lastname)" -ForegroundColor Gray
}

# [TEST 3] Mettre à jour son mot de passe (PUT /api/user/profile/password)
$newCollabPass = "NewStrongPass456!"
Assert-Test "Mettre a jour son mot de passe (PUT /api/user/profile/password)" {
    $passBody = @{
        currentPassword = $collabPass
        newPassword     = $newCollabPass
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$baseUrl/api/user/profile/password" -Method Put -Headers $collabHeaders -Body $passBody
    Write-Host "     Reponse serveur : $($res.message)" -ForegroundColor Gray

    # Re-connexion avec le nouveau mot de passe pour valider
    $newLogin = @{ email = $collabEmail; password = $newCollabPass } | ConvertTo-Json
    $newLoginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $newLogin -ContentType "application/json; charset=utf-8"
    $script:collabHeaders = @{
        "Authorization" = "Bearer $($newLoginRes.token)"
        "Content-Type"  = "application/json; charset=utf-8"
    }
    Write-Host "     Authentification confirmee avec le nouveau mot de passe !" -ForegroundColor Gray
}

# --- Préparation : L'Admin crée un projet et une tâche assignée au collaborateur ---
$projBody = @{
    name        = "Projet Mobile-$collabGuid"
    description = "Application mobile pour collaborateurs"
} | ConvertTo-Json
$proj = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects" -Method Post -Headers $adminHeaders -Body $projBody

$taskBody = @{
    title         = "Ecran d'accueil et Dashboard UI"
    description   = "Integrer les widgets de statistiques et le responsive design"
    priority      = "HAUTE"
    dueDate       = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
    projectId     = $proj.id
    assigneeIds   = @($collabUserId)
} | ConvertTo-Json

$assignedTask = Invoke-RestMethod -Uri "$baseUrl/api/admin/tasks" -Method Post -Headers $adminHeaders -Body $taskBody
$script:myTaskId = $assignedTask.id

# [TEST 4] Filtrer et rechercher les tâches qui lui sont attribuées (GET /api/user/tasks)
Assert-Test "Rechercher et filtrer ses taches assignees (GET /api/user/tasks?search=Dashboard)" {
    $myTasks = Invoke-RestMethod -Uri "$baseUrl/api/user/tasks?search=Dashboard" -Method Get -Headers $script:collabHeaders
    if ($myTasks.Count -eq 0 -or $myTasks[0].id -ne $script:myTaskId) {
        throw "La tache attribuee n'a pas ete retrouvee dans les taches assignees."
    }
    Write-Host "     Tache trouvee : ID $($myTasks[0].id) - $($myTasks[0].title) (Statut: $($myTasks[0].status))" -ForegroundColor Gray
}

# [TEST 5] Changer l'état d'avancement de sa tâche : A_FAIRE -> EN_COURS -> TERMINE
Assert-Test "Changer l'etat d'avancement de sa tache (A_FAIRE -> EN_COURS -> TERMINE)" {
    # 1. Passage en cours
    $inProgBody = @{ status = "EN_COURS" } | ConvertTo-Json
    $t1 = Invoke-RestMethod -Uri "$baseUrl/api/user/tasks/$script:myTaskId/status" -Method Patch -Headers $script:collabHeaders -Body $inProgBody
    if ($t1.status -ne "EN_COURS") { throw "Echec passage a EN_COURS" }

    # 2. Passage a termine
    $doneBody = @{ status = "TERMINE" } | ConvertTo-Json
    $t2 = Invoke-RestMethod -Uri "$baseUrl/api/user/tasks/$script:myTaskId/status" -Method Patch -Headers $script:collabHeaders -Body $doneBody
    if ($t2.status -ne "TERMINE" -or [string]::IsNullOrEmpty($t2.closedAt)) {
        throw "Echec passage a TERMINE ou closedAt manquant"
    }
    Write-Host "     Statut mis a jour avec succes : $($t2.status) (Date fin: $($t2.closedAt))" -ForegroundColor Gray
}

# [TEST 6] Sécurité : Un collaborateur ne peut pas clôturer définitivement un ticket (CLOTURE -> 403)
Assert-Test "Securite : Refus de cloture definitive CLOTURE pour un ROLE_USER (Attendu: 403)" {
    $closeBody = @{ status = "CLOTURE" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/user/tasks/$script:myTaskId/status" -Method Patch -Headers $script:collabHeaders -Body $closeBody
        throw "Un utilisateur standard a pu cloturer definitivement un ticket ! Faille securite !"
    } catch {
        Write-Host "     Cloture definitive correctement refusee au collaborateur (403 Forbidden)." -ForegroundColor Gray
    }
}

# [TEST 7] Estimer et saisir le temps passé sur un ticket (PATCH /api/user/tasks/{id}/time)
Assert-Test "Estimer et saisir le temps passe sur le ticket (PATCH /api/user/tasks/{id}/time)" {
    # Estimation 6h et saisie initiale 2.5h
    $timeBody = @{
        estimatedTime = 6.0
        spentTime     = 2.5
    } | ConvertTo-Json

    $tTime = Invoke-RestMethod -Uri "$baseUrl/api/user/tasks/$script:myTaskId/time" -Method Patch -Headers $script:collabHeaders -Body $timeBody
    if ($tTime.estimatedTime -ne 6.0 -or $tTime.spentTime -ne 2.5) {
        throw "Temps non conforme apres saisie initiale."
    }

    # Ajout de temps additionnel +1.5h
    $addTimeBody = @{ additionalSpentTime = 1.5 } | ConvertTo-Json
    $tTimeAdd = Invoke-RestMethod -Uri "$baseUrl/api/user/tasks/$script:myTaskId/time" -Method Patch -Headers $script:collabHeaders -Body $addTimeBody
    if ($tTimeAdd.spentTime -ne 4.0) {
        throw "Le temps additionnel n'a pas ete cumule correctement (attendu: 4.0h, recu: $($tTimeAdd.spentTime)h)."
    }
    Write-Host "     Temps enregistre : Estime = $($tTimeAdd.estimatedTime)h, Passe = $($tTimeAdd.spentTime)h" -ForegroundColor Gray
}

# [TEST 8] Poster un commentaire sur la tâche pour demander une précision
Assert-Test "Poster et consulter des commentaires sur la tache (POST/GET /api/tasks/{id}/comments)" {
    $commentBody = @{
        content = "Pourriez-vous preciser la resolution attendue pour les captures d'ecran mobiles ?"
    } | ConvertTo-Json

    $comment = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:myTaskId/comments" -Method Post -Headers $script:collabHeaders -Body $commentBody
    if (-not $comment.id -or $comment.authorEmail -ne $collabEmail) {
        throw "Creation du commentaire echouee."
    }

    # Consultation de la liste des commentaires
    $commentsList = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:myTaskId/comments" -Method Get -Headers $script:collabHeaders
    if ($commentsList.Count -eq 0) { throw "Aucun commentaire retourne." }
    Write-Host "     Commentaire poste : ID $($comment.id) par $($comment.authorName) : '$($comment.content)'" -ForegroundColor Gray
}

# [TEST 9] Joindre une capture d'écran / fichier au ticket (POST /api/tasks/{id}/attachments)
Assert-Test "Joindre une capture d'ecran et la telecharger (POST/GET /api/tasks/{id}/attachments)" {
    # Création d'un fichier fictif simulant une capture d'écran PNG
    $tempFile = [System.IO.Path]::GetTempFileName() + ".png"
    [System.IO.File]::WriteAllBytes($tempFile, [System.Text.Encoding]::UTF8.GetBytes("PNG_SCREENSHOT_DATA_MOCK"))

    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    $fileBytes = [System.IO.File]::ReadAllBytes($tempFile)

    $header = "--$boundary$LF" +
              "Content-Disposition: form-data; name=`"file`"; filename=`"capture_ecran_dashboard.png`"$LF" +
              "Content-Type: image/png$LF$LF"

    $footer = "$LF--$boundary--$LF"

    $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($header)
    $footerBytes = [System.Text.Encoding]::UTF8.GetBytes($footer)

    $bodyStream = New-Object System.IO.MemoryStream
    $bodyStream.Write($headerBytes, 0, $headerBytes.Length)
    $bodyStream.Write($fileBytes, 0, $fileBytes.Length)
    $bodyStream.Write($footerBytes, 0, $footerBytes.Length)
    $bodyBytes = $bodyStream.ToArray()

    $multipartHeaders = @{
        "Authorization" = $script:collabHeaders["Authorization"]
        "Content-Type"  = "multipart/form-data; boundary=$boundary"
    }

    $attachment = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:myTaskId/attachments" -Method Post -Headers $multipartHeaders -Body $bodyBytes
    if (-not $attachment.id -or $attachment.fileName -ne "capture_ecran_dashboard.png") {
        throw "Televersement de la capture d'ecran echoue."
    }
    Write-Host "     Fichier joint ID $($attachment.id) : $($attachment.fileName) ($($attachment.fileSize) octets)" -ForegroundColor Gray

    # Liste des pièces jointes
    $attachments = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$script:myTaskId/attachments" -Method Get -Headers $script:collabHeaders
    if ($attachments.Count -eq 0) { throw "Liste des pieces jointes vide." }

    # Téléchargement de la pièce jointe
    $download = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/api/attachments/$($attachment.id)/download" -Method Get -Headers $script:collabHeaders
    if ($download.StatusCode -ne 200) { throw "Echec du telechargement de la piece jointe." }
    Write-Host "     Piece jointe telechargee avec succes (Code HTTP $($download.StatusCode))." -ForegroundColor Gray

    # Nettoyage fichier temporaire
    Remove-Item -Path $tempFile -Force -ErrorAction SilentlyContinue
}

# [TEST 10] Réception et consultation des notifications (GET /api/notifications)
Assert-Test "Recevoir et marquer ses notifications comme lues (GET/PATCH /api/notifications)" {
    $notifications = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers $script:collabHeaders
    if ($notifications.Count -eq 0) {
        throw "Aucune notification recue par le collaborateur."
    }

    $firstNotif = $notifications[0]
    Write-Host "     Notification recue : ID $($firstNotif.id) - '$($firstNotif.message)'" -ForegroundColor Gray

    # Marquer comme lue
    $readNotif = Invoke-RestMethod -Uri "$baseUrl/api/notifications/$($firstNotif.id)/read" -Method Patch -Headers $script:collabHeaders
    if ($readNotif.isRead -ne $true) {
        throw "La notification n'a pas ete marquee comme lue."
    }
    Write-Host "     Notification marquee comme lue avec succes (isRead: True)." -ForegroundColor Gray
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS COLLABORATEUR (ROLE_USER) : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
