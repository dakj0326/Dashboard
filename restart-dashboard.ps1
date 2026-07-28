param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectPath,

    [Parameter(Mandatory = $true)]
    [long]$ProcessId
)

$ErrorActionPreference = "Stop"
$logPath = Join-Path $ProjectPath "dashboard-restart.log"

try {
    Wait-Process -Id $ProcessId -ErrorAction SilentlyContinue
    Set-Location -LiteralPath $ProjectPath
    Start-Process `
        -FilePath ".\gradlew.bat" `
        -ArgumentList "run" `
        -WorkingDirectory $ProjectPath `
        -WindowStyle Hidden
}
catch {
    "[$(Get-Date -Format o)] ERROR: $($_.Exception.Message)" |
        Out-File -LiteralPath $logPath -Append -Encoding utf8
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        "$($_.Exception.Message)`n`nDetails were written to:`n$logPath",
        "Dashboard restart failed",
        "OK",
        "Error"
    ) | Out-Null
}
