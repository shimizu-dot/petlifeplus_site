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

## 起動できないとき

`Cannot connect to the Docker daemon` が出る場合は、Docker Desktop が起動していません。macOS では先に Docker を起動してください。

```bash
open -a Docker
docker info
```

`docker info` が通ったあとで、あらためて `docker compose up -d` を実行します。
