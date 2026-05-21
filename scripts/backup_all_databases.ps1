#Requires -Version 5.1
<#
.SYNOPSIS
    PostgreSQL サーバー上の全データベース（ロール・グローバル設定を含む）をバックアップします。

.DESCRIPTION
    pg_dumpall で plain SQL を出力し、backups/ ディレクトリに保存します。
    直近 $KeepGenerations 件のみ保持し、古いものは自動削除します。

    単一 DB のバックアップには db_backup.ps1 を使用してください。

.USAGE
    .\scripts\backup_all_databases.ps1

.RESTORE
    # 全 DB を復元する場合:
    #   $env:PGPASSWORD = "hs0512"
    #   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f backups\<yyyyMMdd_HHmmss>_all_databases.sql

.ENVIRONMENT
    DB_HOST      (default: localhost)
    DB_PORT      (default: 5432)
    DB_USER      (default: postgres)
    DB_PASSWORD  (default: hs0512)
#>

$ErrorActionPreference = "Stop"

# ─── 設定 ──────────────────────────────────────────────────────────────────────

$DbHost     = if ($env:DB_HOST)     { $env:DB_HOST }     else { "localhost" }
$DbPort     = if ($env:DB_PORT)     { $env:DB_PORT }     else { "5432" }
$DbUser     = if ($env:DB_USER)     { $env:DB_USER }     else { "postgres" }
$DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "hs0512" }

$RootDir         = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackupDir       = Join-Path $RootDir "backups"
$Date            = Get-Date -Format "yyyyMMdd_HHmmss"
$OutFile         = Join-Path $BackupDir "${Date}_all_databases.sql"
$KeepGenerations = 7

# ─── pg_dumpall パス解決 ────────────────────────────────────────────────────────

$PgDumpAllCmd = $null

$CandidateDirs = @(
    "C:\Program Files\PostgreSQL\17\bin",
    "C:\Program Files\PostgreSQL\16\bin",
    "C:\Program Files\PostgreSQL\15\bin",
    "C:\Program Files\PostgreSQL\14\bin"
)
foreach ($dir in $CandidateDirs) {
    $exe = Join-Path $dir "pg_dumpall.exe"
    if (Test-Path $exe) {
        $PgDumpAllCmd = $exe
        break
    }
}

if (-not $PgDumpAllCmd) {
    $found = Get-Command pg_dumpall -ErrorAction SilentlyContinue
    if ($found) { $PgDumpAllCmd = $found.Source }
}

if (-not $PgDumpAllCmd) {
    throw "pg_dumpall が見つかりません。PostgreSQL をインストールし、上記パスか PATH に pg_dumpall.exe を配置してください。"
}

Write-Host "pg_dumpall: $PgDumpAllCmd"

# ─── バックアップ実行 ───────────────────────────────────────────────────────────

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

$env:PGPASSWORD = $DbPassword
try {
    & $PgDumpAllCmd -h $DbHost -p $DbPort -U $DbUser --file $OutFile

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dumpall が終了コード $LASTEXITCODE で失敗しました。"
    }

    $SizeMB = [math]::Round((Get-Item $OutFile).Length / 1MB, 2)
    Write-Host "Backup created: $OutFile  ($SizeMB MB)"
}
catch {
    if (Test-Path $OutFile) {
        Remove-Item -LiteralPath $OutFile -Force -ErrorAction SilentlyContinue
    }
    throw
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

# ─── 世代管理 ───────────────────────────────────────────────────────────────────

$files = Get-ChildItem -Path $BackupDir -Filter "*_all_databases.sql" |
         Sort-Object LastWriteTime -Descending

if ($files.Count -gt $KeepGenerations) {
    $removed = $files | Select-Object -Skip $KeepGenerations
    $removed | Remove-Item -Force
    Write-Host "Removed $($removed.Count) old backup(s). Kept latest $KeepGenerations."
} else {
    Write-Host "Retention: $($files.Count) / $KeepGenerations backups kept."
}
