param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectPath,

    [Parameter(Mandatory = $true)]
    [long]$ProcessId
)

$ErrorActionPreference = "Stop"
$logPath = Join-Path $ProjectPath "dashboard-update.log"

function Invoke-NativeLogged {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,

        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    # Windows PowerShell can promote ordinary native stderr output (such as
    # Git's "From github.com..." message) to a terminating PowerShell error.
    # Native tools communicate success through their exit code, so temporarily
    # allow their output and evaluate that code explicitly.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $Command @Arguments *>> $logPath
        return $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }
}

try {
    Wait-Process -Id $ProcessId -ErrorAction SilentlyContinue
    Set-Location -LiteralPath $ProjectPath

    "[$(Get-Date -Format o)] Starting Dashboard update." |
        Out-File -LiteralPath $logPath -Encoding utf8

    $gitExitCode = Invoke-NativeLogged git pull --ff-only origin main
    if ($gitExitCode -ne 0) {
        throw "Git could not fast-forward the local checkout."
    }

    $buildExitCode = Invoke-NativeLogged ".\gradlew.bat" clean test
    if ($buildExitCode -ne 0) {
        throw "The downloaded version did not compile successfully."
    }

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
        "Dashboard update failed",
        "OK",
        "Error"
    ) | Out-Null
}
