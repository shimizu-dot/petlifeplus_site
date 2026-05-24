# petlifeplus-site フォルダ構成

最終更新: 2026-05-24

## 目的
- フロントエンド（静的サイト）とバックエンド（Spring Boot）の責務を分離する
- ファイルの追加・修正時に置き場所に迷わないようにする

---

## トップレベル

```
petlifeplus_site/
├── frontend/                  # 静的マーケティングサイト（ビルド不要）
├── backend/                   # Spring Boot Webアプリ
├── docs/                      # プロジェクト全体の設計ドキュメント
├── scripts/                   # 運用スクリプト（PowerShell / SQL）
├── _archive/                  # 不使用ファイルのアーカイブ（参照のみ）
├── backups/                   # DBバックアップSQL（gitignore対象）
├── logs/                      # ルートレベルのログ（gitignore対象）
├── backup.sql                 # 最新のDBスナップショット
├── Dockerfile                 # コンテナビルド定義
├── CLAUDE.md                  # Claude Code 向けプロジェクト説明
├── FOLDER_STRUCTURE.md        # このファイル
└── petlife_plus.md            # サービス企画・要件・起動手順まとめ
```

---

## frontend/

静的HTMLサイト。ビルドツール不使用。ブラウザで直接開くか静的サーバーで配信。

```
frontend/
├── public/                    # 公開ファイル一式
│   ├── index.html             # トップページ
│   ├── f_service.html         # サービス紹介
│   ├── f_flow.html            # ご利用の流れ
│   ├── f_contact.html         # お問い合わせ
│   ├── f_info.html            # 運営情報
│   ├── webapp.html            # Webアプリ紹介・ログイン導線
│   └── assets/
│       ├── css/
│       │   └── style.css      # サイト全体のスタイル
│       ├── js/
│       │   └── main.js        # ナビゲーション・インタラクション
│       └── img/               # 画像素材（ペット写真・UI画像など）
└── docs/
    ├── ui-design.md           # UIデザインガイド（色・フォント・余白・ブレークポイント）
    └── prompt_front.md        # フロントエンド開発用 AI プロンプト
```

---

## backend/

Spring Boot（Java 17）+ MyBatis + Thymeleaf + PostgreSQL。

```
backend/
├── pom.xml                    # Maven依存関係定義
├── mvnw / mvnw.cmd            # Maven Wrapper（Java環境があれば実行可能）
├── .mvn/wrapper/
│   └── maven-wrapper.properties
├── docs/
│   ├── requirements.md        # 機能要件・MoSCoW優先度・画面仕様
│   ├── db-design.md           # テーブル定義・ER図・インデックス一覧
│   ├── test_report.md         # テスト計画（テストコードではなく仕様書）
│   └── prompt_backend.md      # バックエンド開発用 AI プロンプト
├── logs/                      # アプリ・監査・運用ログ（gitignore対象）
│   ├── app.log                # アプリケーションログ（ローテーション付き）
│   ├── audit.log              # 認証・操作監査ログ
│   └── ops.log                # 運用ログ
├── uploads/                   # ユーザーアップロード画像（gitignore対象、.gitkeepのみ管理）
│   ├── pets/                  # ペットプロフィール画像（UUID.png）
│   └── health-records/        # 健康記録添付画像（UUID.png）
└── src/
    ├── main/
    │   ├── java/com/example/petlife/
    │   │   ├── PetlifeApplication.java        # Spring Boot エントリポイント
    │   │   │
    │   │   ├── config/                        # Spring 設定クラス
    │   │   │   ├── SecurityConfig.java        # Spring Security（フォームログイン・認可ルール）
    │   │   │   ├── UserDetailsServiceImpl.java # メールでユーザーを検索して認証
    │   │   │   ├── LoginUser.java             # 認証済みユーザーのラッパー（ロール判定など）
    │   │   │   ├── DataInitializer.java       # 起動時の初期データ投入（roles / users）
    │   │   │   ├── MyBatisConfig.java         # MyBatis 設定
    │   │   │   ├── WebMvcConfig.java          # uploads/ の静的ファイル配信設定
    │   │   │   ├── WebResourceConfig.java     # その他 Web リソース設定
    │   │   │   └── logging/
    │   │   │       └── RequestLoggingFilter.java  # HTTPリクエストのログ出力フィルター
    │   │   │
    │   │   ├── controller/                    # Thymeleaf コントローラー（/app/**）
    │   │   │   ├── RootController.java        # GET /  → フロントエンドへリダイレクト
    │   │   │   ├── AuthController.java        # GET|POST /app/login, /app/logout
    │   │   │   ├── PasswordResetController.java  # GET|POST /app/password-resets
    │   │   │   ├── DashboardController.java   # GET /app/dashboard
    │   │   │   ├── PetController.java         # CRUD /app/pets/**
    │   │   │   ├── HealthRecordController.java # CRUD /app/pets/{id}/health-records/**
    │   │   │   ├── AppointmentPageController.java # CRUD /app/appointments/** (Thymeleaf)
    │   │   │   ├── AppointmentController.java # REST /api/appointments (JSON)
    │   │   │   ├── AppointmentSlotController.java # CRUD /app/admin/appointment-slots
    │   │   │   ├── CalendarController.java    # GET /app/calendar, POST /app/calendar/marks/**
    │   │   │   ├── ConsultationController.java # CRUD /app/consultations/** (VET/STAFF/ADMIN)
    │   │   │   ├── ConsultChatController.java # GET|POST /app/consult/chatbot
    │   │   │   ├── ClinicGuideController.java # GET /app/clinic-guide
    │   │   │   ├── NotificationController.java # GET /app/notifications, POST 既読・dismiss
    │   │   │   ├── SubscriptionController.java # GET /app/subscriptions
    │   │   │   ├── PremiumSupportController.java # GET|POST /app/premium/online-care
    │   │   │   ├── ReportController.java      # GET /app/reports (ADMIN)
    │   │   │   ├── UserController.java        # CRUD /app/admin/users (ADMIN)
    │   │   │   ├── AnnouncementController.java # CRUD /app/admin/announcements (ADMIN)
    │   │   │   ├── GlobalControllerAdvice.java # 全ビューへの共通モデル属性（未読数など）
    │   │   │   ├── line/
    │   │   │   │   └── LineEventController.java  # POST /api/line/events (LINE Messaging API)
    │   │   │   └── slack/
    │   │   │       └── SlackEventController.java # POST /api/slack/events (Slack Events API)
    │   │   │
    │   │   ├── dto/                           # 画面・API のデータ転送オブジェクト
    │   │   │   ├── appointment/               # AppointmentCreateRequest, ListRow, Form など
    │   │   │   ├── auth/                      # LoginRequest, LoginResponse
    │   │   │   ├── calendar/                  # AppointmentCalendarRow, CalendarMarkForm
    │   │   │   ├── chat/                      # ConsultChatForm
    │   │   │   ├── common/                    # ApiErrorResponse, FieldErrorDetail, PageResponse
    │   │   │   ├── consultation/              # ConsultationForm, MedicalHistoryRow
    │   │   │   ├── dashboard/                 # DashCalDay
    │   │   │   ├── health/                    # HealthRecordForm, CreateRequest, Response など
    │   │   │   ├── notification/              # NotificationRow
    │   │   │   ├── pet/                       # PetForm, CreateRequest, Response など
    │   │   │   ├── premium/                   # PremiumOnlineCareForm
    │   │   │   ├── subscription/              # SubscriptionRow, RenewalHistoryRow
    │   │   │   ├── symptom/                   # SymptomCheckForm
    │   │   │   └── user/                      # UserForm, CreateRequest, Response など
    │   │   │
    │   │   ├── entity/                        # DB行をマッピングする POJO（MyBatis結果型）
    │   │   │   ├── AnnouncementEntity.java
    │   │   │   ├── AppointmentEntity.java
    │   │   │   ├── AppointmentSlotEntity.java
    │   │   │   ├── CalendarMarkEntity.java
    │   │   │   ├── ConsultChatMessageEntity.java
    │   │   │   ├── EmailMessageEntity.java
    │   │   │   ├── EmailTemplateEntity.java
    │   │   │   ├── HealthRecordEntity.java
    │   │   │   ├── HealthRecordPetDateEntity.java  # 集計用カスタム結果型
    │   │   │   ├── InvoiceEntity.java
    │   │   │   ├── MedicalAttachmentEntity.java
    │   │   │   ├── MedicalHistoryEntity.java
    │   │   │   ├── NotificationEntity.java
    │   │   │   ├── PaymentEntity.java
    │   │   │   ├── PetCareRecordEntity.java
    │   │   │   ├── PetEntity.java
    │   │   │   ├── RoleEntity.java
    │   │   │   ├── SymptomCheckEntity.java
    │   │   │   └── UserEntity.java
    │   │   │
    │   │   ├── mapper/                        # MyBatis マッパーインターフェース（SQL アノテーション）
    │   │   │   ├── AnnouncementMapper.java
    │   │   │   ├── AppointmentMapper.java
    │   │   │   ├── AppointmentSlotMapper.java
    │   │   │   ├── AuthMapper.java
    │   │   │   ├── CalendarMarkMapper.java
    │   │   │   ├── ConsultChatMapper.java
    │   │   │   ├── DismissedReminderMapper.java
    │   │   │   ├── EmailMessageMapper.java    # スキーマのみ・未使用
    │   │   │   ├── EmailTemplateMapper.java   # スキーマのみ・未使用
    │   │   │   ├── HealthRecordMapper.java
    │   │   │   ├── InvoiceMapper.java         # スキーマのみ・未使用
    │   │   │   ├── MedicalAttachmentMapper.java # スキーマのみ・未使用
    │   │   │   ├── MedicalHistoryMapper.java
    │   │   │   ├── NotificationMapper.java
    │   │   │   ├── PaymentMapper.java         # スキーマのみ・未使用
    │   │   │   ├── PetCareRecordMapper.java
    │   │   │   ├── PetMapper.java
    │   │   │   ├── RoleMapper.java
    │   │   │   ├── SubscriptionMapper.java
    │   │   │   ├── SymptomCheckMapper.java
    │   │   │   └── UserMapper.java
    │   │   │
    │   │   ├── service/                       # ビジネスロジック
    │   │   │   ├── AnnouncementService.java
    │   │   │   ├── AppointmentService.java
    │   │   │   ├── AuthService.java           # 補助的なログイン処理（コントローラーからは未呼び出し）
    │   │   │   ├── CalendarService.java
    │   │   │   ├── ConsultChatService.java    # チャットボット応答ロジック
    │   │   │   ├── HealthRecordImageStorageService.java  # 健康記録画像のアップロード・削除
    │   │   │   ├── HealthRecordService.java
    │   │   │   ├── ImageOrientationUtil.java  # EXIF Orientation 補正ユーティリティ
    │   │   │   ├── PetCareRecordService.java
    │   │   │   ├── PetImageStorageService.java # ペット画像のアップロード・削除
    │   │   │   ├── PetService.java
    │   │   │   ├── PlanAccessService.java     # プラン別機能ゲート（AI症状チェック可否など）
    │   │   │   ├── SymptomCheckService.java   # OpenAI API 呼び出し（未設定時はキーワードフォールバック）
    │   │   │   ├── UserService.java
    │   │   │   ├── ZoomLinkService.java       # Zoom Server-to-Server OAuth でミーティングURL生成
    │   │   │   ├── line/
    │   │   │   │   ├── LineBotService.java    # LINE メッセージ送受信ロジック
    │   │   │   │   └── LineRequestVerifier.java  # LINE 署名検証
    │   │   │   └── slack/
    │   │   │       ├── SlackBotService.java   # Slack メッセージ送受信ロジック
    │   │   │       └── SlackRequestVerifier.java # Slack 署名検証
    │   │   │
    │   │   └── exception/                     # 例外クラス・ハンドラー
    │   │       ├── BadRequestException.java
    │   │       ├── NotFoundException.java
    │   │       └── GlobalExceptionHandler.java # @RestControllerAdvice でエラーレスポンス統一
    │   │
    │   └── resources/
    │       ├── application.properties         # 本番・共通設定
    │       ├── application-local.properties   # ローカル用APIキー（gitignore対象・手動作成）
    │       ├── schema.sql                     # DDL（22テーブル）
    │       ├── data.sql                       # 初期データ（roles / users / plans など）
    │       ├── logback-spring.xml             # ログローテーション設定
    │       ├── META-INF/
    │       │   └── additional-spring-configuration-metadata.json  # IDE補完用設定キーメタデータ
    │       ├── static/                        # バックエンドが配信する静的ファイル（classpath:/static/）
    │       │   ├── css/
    │       │   │   └── app.css               # Webアプリ用スタイル
    │       │   ├── assets/css/style.css       # フロントと共用のスタイル（参照用コピー）
    │       │   ├── assets/js/main.js          # フロントと共用のJS（参照用コピー）
    │       │   └── img/                       # UIアイコン（icon-memo.svg など）
    │       └── templates/                     # Thymeleaf テンプレート
    │           ├── fragments/
    │           │   └── nav.html              # 共通ナビゲーション（全画面から include）
    │           ├── auth/
    │           │   ├── login.html            # ログイン画面
    │           │   └── password-change.html  # パスワード変更画面
    │           ├── dashboard/
    │           │   └── index.html            # ダッシュボード
    │           ├── pets/
    │           │   ├── list.html             # ペット一覧
    │           │   ├── detail.html           # ペット詳細
    │           │   └── form.html             # ペット登録・編集フォーム
    │           ├── health/
    │           │   ├── list.html             # 健康記録一覧
    │           │   ├── form.html             # 健康記録登録・編集フォーム
    │           │   └── print.html            # 印刷用レイアウト
    │           ├── appointments/
    │           │   ├── index.html            # 予約一覧・新規予約フォーム
    │           │   └── slot-management.html  # 予約枠管理（ADMIN）
    │           ├── calendar/
    │           │   └── index.html            # ペットカレンダー
    │           ├── consultations/
    │           │   ├── list.html             # 診療・相談履歴一覧
    │           │   └── form.html             # 診療・相談履歴登録・編集フォーム
    │           ├── consult/
    │           │   └── chatbot.html          # チャットボット相談画面
    │           ├── clinic/
    │           │   └── index.html            # 動物病院案内
    │           ├── notifications/
    │           │   └── index.html            # 通知一覧・リマインダー
    │           ├── subscriptions/
    │           │   └── index.html            # サブスクリプション一覧
    │           ├── premium/
    │           │   └── online-care.html      # プレミアムオンライン診療（Zoom）
    │           ├── reports/
    │           │   └── index.html            # 管理レポート（ADMIN）
    │           └── admin/
    │               ├── announcements.html    # お知らせ管理（ADMIN）
    │               └── users/
    │                   ├── list.html         # ユーザー一覧（ADMIN）
    │                   └── form.html         # ユーザー登録・編集フォーム（ADMIN）
    │
    └── test/
        └── java/com/example/petlife/
            ├── PetlifeApplicationTests.java
            ├── controller/slack/
            │   └── SlackEventControllerTest.java
            └── service/
                ├── ZoomLinkServiceTest.java
                └── slack/
                    └── SlackRequestVerifierTest.java

```

---

## docs/

プロジェクト全体の設計ドキュメント（HTMLレポート・Excel・Markdown）。

```
docs/
├── 01-proposal.html           # サービス企画書
├── 02-market-research.html    # 市場調査
├── 03-persona.html            # ペルソナ設計
├── 04-sitemap.html            # サイトマップ
├── 05-wireframe.html          # ワイヤーフレーム
├── 06-design-guide.html       # デザインガイド
├── 07-specification.html      # 機能仕様書
├── 08-db-design.html          # DB設計書（HTML版）
├── 09-test-report.html        # テストレポート（HTML版）
├── 10-retrospective.html      # 振り返り
├── sitemap-to-url-design.html # サイトマップ → URL設計対応表
├── production-checklist.md    # 本番リリース前チェックリスト
└── *.xlsx                     # 各設計書のExcel版
```

---

## scripts/

運用・DB管理スクリプト。

```
scripts/
├── db_backup.ps1              # PostgreSQL バックアップを backups/ に出力
├── backup_all_databases.ps1   # 全DB一括バックアップ
└── patch_appointment_slots.sql # 予約枠の手動パッチSQL
```

---

## _archive/

不使用になったファイルの保管場所。削除はせず参照のみ。

```
_archive/
├── images-unused/             # 採用されなかった画像素材
├── scripts-shell/             # 旧バッシュスクリプト（db_backup.sh / db_restore.sh）
└── vscode-hooks/              # 旧VSCode Hooksの設定ファイル
```

---

## 運用メモ

### ファイルを追加するときの置き場所

| 追加するもの | 置き場所 |
|---|---|
| フロントのHTML/CSS/JS | `frontend/public/` |
| フロントの画像 | `frontend/public/assets/img/` |
| バックエンドの画面ロジック | `backend/src/main/java/.../controller/` |
| ビジネスロジック | `backend/src/main/java/.../service/` |
| DB操作SQL | `backend/src/main/java/.../mapper/` |
| 画面テンプレート | `backend/src/main/resources/templates/` |
| アプリ共通CSSなど | `backend/src/main/resources/static/` |
| DBスキーマ変更 | `backend/src/main/resources/schema.sql` + `backend/docs/db-design.md` |
| 設計ドキュメント（全体） | `docs/` |
| 設計ドキュメント（バックエンド固有） | `backend/docs/` |
| 設計ドキュメント（フロントエンド固有） | `frontend/docs/` |
| 運用スクリプト | `scripts/` |

### gitignore対象（コミットしない）

- `backend/src/main/resources/application-local.properties` — APIキー
- `backend/uploads/` 配下の実ファイル（`.gitkeep` のみ管理）
- `backend/logs/`, `logs/` — ログファイル
- `backups/` — DBバックアップSQL
- `backend/target/` — ビルド成果物
