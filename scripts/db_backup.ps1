#Requires -Version 5.1
<#
.SYNOPSIS
    petlifeplus データベースのバックアップを plain SQL 形式で作成します。

.DESCRIPTION
    pg_dump で plain SQL を出力し、backups/ ディレクトリに保存します。
    直近 $KeepGenerations 件のみ保持し、古いものは自動削除します。

.USAGE
    .\scripts\db_backup.ps1

.RESTORE
    # データベースを再作成して復元する場合:
    #   $env:PGPASSWORD = "hs0512"
    #   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -c "DROP DATABASE IF EXISTS petlifeplus WITH (FORCE);"
    #   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -c "CREATE DATABASE petlifeplus;"
    #   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d petlifeplus -f backups\<yyyyMMdd_HHmmss>_backup.sql

.ENVIRONMENT
    DB_HOST      (default: localhost)
    DB_PORT      (default: 5432)
    DB_NAME      (default: petlifeplus)
    DB_USER      (default: postgres)
    DB_PASSWORD  (default: hs0512)
#>

$ErrorActionPreference = "Stop"

# ─── 設定 ──────────────────────────────────────────────────────────────────────

$DbHost     = if ($env:DB_HOST)     { $env:DB_HOST }     else { "localhost" }
$DbPort     = if ($env:DB_PORT)     { $env:DB_PORT }     else { "5432" }
$DbName     = if ($env:DB_NAME)     { $env:DB_NAME }     else { "petlifeplus" }
$DbUser     = if ($env:DB_USER)     { $env:DB_USER }     else { "postgres" }
$DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "hs0512" }

$RootDir         = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackupDir       = Join-Path $RootDir "backups"
$Date            = Get-Date -Format "yyyyMMdd_HHmmss"
$OutFile         = Join-Path $BackupDir "${Date}_backup.sql"
$KeepGenerations = 7

# ─── pg_dump パス解決 ──────────────────────────────────────────────────────────

$PgDumpCmd = $null

# PostgreSQL インストール先を新しいバージョン順に探す
$CandidateDirs = @(
    "C:\Program Files\PostgreSQL\17\bin",
    "C:\Program Files\PostgreSQL\16\bin",
    "C:\Program Files\PostgreSQL\15\bin",
    "C:\Program Files\PostgreSQL\14\bin"
)
foreach ($dir in $CandidateDirs) {
    $exe = Join-Path $dir "pg_dump.exe"
    if (Test-Path $exe) {
        $PgDumpCmd = $exe
        break
    }
}

# PATH に pg_dump があればそちらも使う
if (-not $PgDumpCmd) {
    $found = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($found) { $PgDumpCmd = $found.Source }
}

if (-not $PgDumpCmd) {
    throw "pg_dump が見つかりません。PostgreSQL をインストールし、上記パスか PATH に pg_dump.exe を配置してください。"
}

Write-Host "pg_dump: $PgDumpCmd"

# ─── バックアップ実行 ───────────────────────────────────────────────────────────

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

$env:PGPASSWORD = $DbPassword
try {
    & $PgDumpCmd -h $DbHost -p $DbPort -U $DbUser -d $DbName `
        --format=plain `
        --no-owner `
        --no-acl `
        --file $OutFile

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump が終了コード $LASTEXITCODE で失敗しました。"
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

$files = Get-ChildItem -Path $BackupDir -Filter "*_backup.sql" |
         Sort-Object LastWriteTime -Descending

if ($files.Count -gt $KeepGenerations) {
    $removed = $files | Select-Object -Skip $KeepGenerations
    $removed | Remove-Item -Force
    Write-Host "Removed $($removed.Count) old backup(s). Kept latest $KeepGenerations."
} else {
    Write-Host "Retention: $($files.Count) / $KeepGenerations backups kept."
}
