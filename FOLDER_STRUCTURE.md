# petlifeplus-site フォルダ設計

## 目的
- フロントエンド成果物とバックエンド実装を分離し、責務を明確化する。
- 画面改修とAPI/業務ロジック改修の影響範囲を切り分ける。

## 構成
```
petlifeplus-site/
├─ frontend/
│  ├─ public/
│  │  ├─ index.html
│  │  ├─ f_service.html
│  │  ├─ f_flow.html
│  │  ├─ f_contact.html
│  │  ├─ f_info.html
│  │  ├─ webapp.html
│  │  └─ assets/
│  │     ├─ css/
│  │     │  └─ style.css
│  │     ├─ img/
│  │     │  ├─ dog-app.png
│  │     │  ├─ dog-main.png
│  │     │  ├─ dog-sub.png
│  │     │  ├─ gallery-01.jpg
│  │     │  ├─ gallery-02.jpg
│  │     │  ├─ gallery-03.jpg
│  │     │  ├─ hero-dashboard.png
│  │     │  ├─ hero-mobile.png
│  │     │  └─ unused/
│  │     └─ js/
│  │        └─ main.js
│  └─ docs/
│     ├─ prompt_front.md
│     └─ ui-design.md
├─ backend/
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/
│  │  │  └─ resources/
│  │  └─ test/
│  └─ docs/
│     ├─ db-design.md
│     ├─ prompt_backend.md
│     ├─ requirements.md
│     └─ test_report.md
├─ FOLDER_STRUCTURE.md
└─ FOLDER_STRUCTURE_OLD.md
```

## 運用ルール
- 画面HTML/CSS/JSの変更は `frontend/` 配下で実施する。
- API/ドメイン/DB連携の実装は `backend/` 配下で実施する。
- 設計メモは責務に応じて `frontend/docs` または `backend/docs` に保存する。
