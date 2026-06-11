# PetLifePlus

ローカル開発環境は Docker Compose で起動します。

## 起動方法

プロジェクト直下で実行します。

```bash
docker compose up --build
```

- 初回起動・再ビルド時: `docker compose up --build`
- 通常起動: `docker compose up -d`
- 停止: `docker compose down`
- DB ボリュームも含めて削除: `docker compose down -v`

起動後のアクセス先:

- アプリ: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

### Spring Boot を直接起動する場合

`backend` ディレクトリで Maven Wrapper を使って起動します。PowerShell では引数が崩れないように、`-D` オプションを引用してください。
この方法では PostgreSQL を別途起動しておく必要があります。先に `docker compose up -d db` を実行するか、`docker compose up --build` でアプリと DB をまとめて起動してください。

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### パスワード再設定メールの運用設定

パスワード再設定メールのリンク先は `app.base-url=${APP_BASE_URL:http://localhost:8080}` で決まります。  
運用環境では、必ず `APP_BASE_URL` を実際の公開URLに設定してください。未設定のままだと、メール本文に `http://localhost:8080` が入ります。

## 起動できないとき

`Cannot connect to the Docker daemon` が出る場合は、Docker Desktop が起動していません。macOS では先に Docker を起動してください。

```bash
open -a Docker
docker info
```

`docker info` が通ったあとで、あらためて `docker compose up -d` を実行します。
