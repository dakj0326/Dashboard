param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectPath,

    [Parameter(Mandatory = $true)]
    [long]$ProcessId
)

$ErrorActionPreference = "Stop"
$logPath = Join-Path $ProjectPath "dashboard-update.log"

try {
    Wait-Process -Id $ProcessId -ErrorAction SilentlyContinue
    Set-Location -LiteralPath $ProjectPath

    & git pull --ff-only origin main *>> $logPath
    if ($LASTEXITCODE -ne 0) {
        throw "Git could not fast-forward the local checkout."
    }

    & ".\gradlew.bat" clean test *>> $logPath
    if ($LASTEXITCODE -ne 0) {
        throw "The downloaded version did not compile successfully."
    }

    Start-Process `
        -FilePath ".\gradlew.bat" `
        -ArgumentList "run" `
        -WorkingDirectory $ProjectPath `
        -WindowStyle Hidden
}
catch {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        "$($_.Exception.Message)`n`nDetails were written to:`n$logPath",
        "Dashboard update failed",
        "OK",
        "Error"
    ) | Out-Null
}
