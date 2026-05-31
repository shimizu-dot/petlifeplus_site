# CHANGELOG

## [2026-05-31]

### バグ修正

#### B-17 — ログイン後ダッシュボード: スマートフォン表示のレイアウト崩れを修正
- **ファイル:**
  - `backend/src/main/resources/templates/dashboard/index.html`
  - `backend/src/main/resources/static/css/app.css`
- **変更内容:**
  - ダッシュボード内のインライン指定を `.topbar-greeting` / `.plan-summary` / `.quick-actions` / `.admin-db-actions` に置換
  - `@media (max-width: 768px)` でアプリ共通レイアウトを縦積みに変更（`layout`/`sidebar`/`topbar`/`content`/`card`）
  - クイックアクセスおよび管理ボタンをスマホで1列・全幅表示にし、タップしやすさを改善

#### B-18 — ダッシュボードメニュー: ドロップダウン表示に変更
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
  - `backend/src/main/resources/static/css/app.css`
- **変更内容:**
  - サイドメニューを `details/summary` ベースのドロップダウン構造に変更
  - PC表示は従来どおりメニューを常時表示、スマホ表示（`max-width: 768px`）のみ「☰ メニューを開く」で展開する動作を追加
  - 既存のメニュー項目・権限分岐ロジックは変更せず、表示方式のみ切り替え

#### B-19 — ダッシュボードメニュー: メニュー非表示不具合を修正
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更内容:**
  - `details.sidebar-dropdown` に `open` 属性を追加し、初期表示時にメニューが非表示になる不具合を解消

#### B-20 — ダッシュボードメニュー: 項目選択後にメニューを自動で閉じるよう修正
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更内容:**
  - サイドバー内メニューリンク（`.sidebar-nav a`）クリック時に、`details.sidebar-dropdown` の `open` を `false` にするスクリプトを追加
  - スマホ表示でメニュー選択後にドロップダウンが開いたまま残る挙動を解消

#### B-21 — 予約枠管理: `is_blocked` 列欠落で 500 になる不具合を修正
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/config/SchemaCompatibilityInitializer.java`
- **変更内容:**
  - 既存DB向け互換DDLとして `ALTER TABLE appointment_slots ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE` を追加
  - 旧スキーマ環境で `/app/admin/appointment-slots` アクセス時に `column "is_blocked" does not exist` が発生する問題を解消

#### B-22 — ダッシュボードメニュー: 初期表示で開いたままになる不具合を修正
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更内容:**
  - `details.sidebar-dropdown` の `open` 属性を削除
  - スマホ表示時にメニューが初期状態で開いたままになる挙動を解消（PC表示はCSSで常時表示のまま維持）

#### B-23 — ダッシュボードメニュー: デスクトップ表示でメニューが見えない不具合を修正
- **ファイル:**
  - `backend/src/main/resources/static/css/app.css`
- **変更内容:**
  - `@media (min-width: 769px)` で `.sidebar-dropdown:not([open]) .sidebar-dropdown-content` を `display: block` に固定
  - `details` の `open` なし状態でも、デスクトップではメニュー本体が表示されるよう修正

#### B-24 — ダッシュボードメニュー: 初期アクセス時のメニュー非表示を修正
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更内容:**
  - `details.sidebar-dropdown` に `open` 属性を追加
  - 初期アクセス直後にデスクトップ環境でメニューが表示されないケースを解消

#### B-25 — ダッシュボードメニュー: スマホ初期表示への影響を回避
- **ファイル:**
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更内容:**
  - `details.sidebar-dropdown` の `open` 属性を削除
  - `DOMContentLoaded` 時に `matchMedia('(min-width: 769px)')` で desktop のみ `open = true` を設定
  - desktop 初期表示を維持しつつ、スマホでは初期状態を閉じたままに修正

#### B-9 — 請求通知: 文字化け対策と請求書リンク導線の修正
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingNotificationService.java`
  - `backend/src/main/resources/templates/notifications/index.html`
- **変更内容:**
  - 通知本文生成時に銀行名・支店名・名義の文字化けパターンを検知し、既定値へフォールバックする `safeText()` を追加
  - アプリ内通知の請求書リンクを絶対 URL 依存から相対パス（`/app/invoices/{id}`）に変更
  - 通知一覧画面で本文中 URL を自動リンク化し、`localhost` 固定 URL は現在のアクセス元ホストへ補正して開くように変更

#### B-10 — 請求管理: 期限超過時の停止通知とアカウントロック処理を有効化
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingService.java`
  - `backend/src/main/java/com/example/petlife/service/OverdueInvoiceScheduler.java`（新規）
- **変更内容:**
  - 期限超過（`due_date < CURRENT_DATE` かつ `UNPAID/PARTIAL`）請求を走査し、停止通知送信 + ユーザー `SUSPENDED` 化を行う `processOverdueInvoices()` を追加
  - 毎日 02:00 実行の `OverdueInvoiceScheduler` を追加して定期処理を接続

#### B-11 — 予約枠管理: 未承認予約が「予約済み」表示される不具合を修正
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/AppointmentMapper.java`
  - `backend/src/main/java/com/example/petlife/service/AppointmentSlotService.java`
  - `backend/src/main/resources/templates/appointments/slot-management.html`
  - `backend/src/main/java/com/example/petlife/dto/appointment/DaySlotRow.java`
- **変更内容:**
  - `REQUESTED` 時刻取得クエリを追加し、予約枠管理画面では `REQUESTED` と `CONFIRMED` を分離
  - 画面表示ステータスに `AUTO_REQUESTED` / `EXTRA_REQUESTED`（申請中）を追加
  - `CONFIRMED` のみ「予約済み」として表示するよう調整

#### B-12 — 請求管理: 入金登録時の Thymeleaf 500 エラーを修正
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/controller/BillingController.java`
- **変更内容:**
  - 入金登録 POST のフォーム引数を `@ModelAttribute("form")` に明示
  - バリデーションエラー時に `model` へ `form` を再設定
  - `#fields.hasErrors(...)` 評価時に `form` が見つからず 500 になる不具合を解消

#### B-13 — 請求管理: 入金登録が反映されない場合の検知と画面エラー表示を追加
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingService.java`
  - `backend/src/main/java/com/example/petlife/controller/BillingController.java`
- **変更内容:**
  - `BillingService.registerPayment()` で入金 INSERT 結果（`paymentId`）を検証し、失敗時は `BadRequestException` を送出
  - 請求ステータス UPDATE 件数を検証し、0 件更新時は明示的にエラー化
  - `BillingController` で `BadRequestException` を捕捉し、詳細画面に `error` メッセージを表示して原因が見えるよう改善

#### B-14 — 通知一覧: 領収書リンク文言の誤表示を修正
- **ファイル:**
  - `backend/src/main/resources/templates/notifications/index.html`
- **変更内容:**
  - 通知本文URLの自動リンク化処理で、リンク文言が常に「請求書を開く」になる問題を修正
  - 同一行に「領収書」を含む場合は「領収書を開く」、それ以外は「請求書を開く」を表示するように変更

#### B-15 — サブスクリプション: 更新申請後のステータス表記を「申請中」に修正
- **ファイル:**
  - `backend/src/main/resources/templates/subscriptions/index.html`
- **変更内容:**
  - 更新申請済み（`pendingRenewalIds` に含まれる契約）は、一覧のステータス列を `有効` ではなく `申請中` バッジで表示
  - DB の `subscriptions.status` は変更せず、既存の利用可否ロジックへ副作用を出さない形で表示のみ修正

#### B-16 — サブスクリプション更新履歴: 入金完了時に「承認済み」へ反映
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/dto/subscription/RenewalHistoryRow.java`
  - `backend/src/main/java/com/example/petlife/mapper/NotificationMapper.java`
  - `backend/src/main/resources/templates/subscriptions/index.html`
- **変更内容:**
  - 更新申請履歴DTOに `status`（`REQUESTED` / `APPROVED`）を追加
  - 履歴取得SQLで、申請通知作成以降の請求書 `payment_status` を参照し、`PAID` の場合は `APPROVED` と判定
  - 申請中ID取得SQLも同様に `PAID` を除外し、入金後に「申請中」表示が残らないよう修正
  - 履歴テーブルのステータス表示を `承認済み`（緑）/`申請済み`（黄）に出し分け

## [2026-05-30]

### ドキュメント修正

#### F-17 — 請求書・領収書ビュー + 通知への URL 埋め込み + 入金後の有効期限延長
- **新規ファイル:**
  - `controller/UserInvoiceController.java` — `GET /app/invoices/{id}` 請求書/領収書ビュー（オーナー本人または ADMIN のみアクセス可）
  - `templates/invoices/view.html` — 請求書/領収書テンプレート（印刷対応・`window.print()`）。PAID 時は有効期限を表示
- **変更ファイル:**
  - `dto/billing/InvoiceRow.java` — `subscriptionEndDate`（`LocalDate`）フィールドを追加
  - `mapper/InvoiceMapper.java` — `findByIdWithDetails()` / `findAllWithDetails()` に `s.end_date AS "subscriptionEndDate"` を追加
  - `mapper/SubscriptionMapper.java` — `findEndDateById()` / `updateEndDate()` を追加
  - `service/BillingService.java` — `registerPayment()` の PAID 処理でサブスクリプション `end_date` を1ヶ月延長
  - `service/BillingNotificationService.java`:
    - `appBaseUrl`（`${app.base-url}`）を注入して `invoiceUrl()` ヘルパーを追加
    - `buildInvoiceIssuedText()` — 末尾に「📄 請求書ダウンロード: URL」を追加
    - `buildInvoiceIssuedHtml()` — 「📄 請求書を確認する」ボタンを追加
    - `buildInvoiceIssuedLine()` — 末尾に請求書 URL を追加
    - `buildPaymentConfirmedText()` — 新規。「契約料金のお支払いを確認しました。〇〇年〇〇月〇〇日まで有効になりました。🧾 領収書ダウンロード: URL」
    - `buildPaymentConfirmedHtml()` — 有効期限行・「🧾 領収書をダウンロードする」ボタンを追加

---

#### F-16 — 請求通知メッセージに振込先・PayPay 情報を追加
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingNotificationService.java`
  - `backend/src/main/resources/application.properties`
  - `backend/src/main/resources/META-INF/additional-spring-configuration-metadata.json`
  - `.env.example`
- **変更内容:**
  - **新規プロパティ（環境変数で上書き可）:**
    - `billing.bank-name` / `BILLING_BANK_NAME` — 振込先銀行名
    - `billing.bank-branch` / `BILLING_BANK_BRANCH` — 支店名
    - `billing.bank-account-number` / `BILLING_BANK_ACCOUNT_NUMBER` — 口座番号
    - `billing.bank-account-holder` / `BILLING_BANK_ACCOUNT_HOLDER` — 口座名義
    - `billing.paypay-qr-url` / `BILLING_PAYPAY_QR_URL` — PayPay QR 画像 URL（メール HTML に `<img>` 埋め込み）
    - `billing.paypay-id` / `BILLING_PAYPAY_ID` — PayPay 送金先 ID（QR URL 未設定時のテキスト案内）
  - **`buildInvoiceIssuedText()`** 追加 — アプリ内通知・LINE 用プレーンテキスト本文（振込先 + PayPay 情報を含む）
  - **`buildInvoiceIssuedHtml()`** 更新 — 振込先（銀行名・支店・口座番号・名義）と PayPay（QR 画像 or ID or テキスト案内）を含む HTML メール
  - **`buildInvoiceIssuedLine()`** 更新 — 振込先・PayPay 情報をテキストで含む LINE メッセージ
  - **通知フォールバック:** QR URL あり → メールに `<img>` / PayPay ID あり → ID テキスト / どちらもなし → 「管理者よりご案内」テキスト

---

#### F-15 — 請求通知の信頼性改善・再送機能追加
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingNotificationService.java`
  - `backend/src/main/java/com/example/petlife/service/BillingService.java`
  - `backend/src/main/java/com/example/petlife/controller/BillingController.java`
  - `backend/src/main/java/com/example/petlife/controller/SubscriptionController.java`
  - `backend/src/main/resources/templates/admin/billing/detail.html`
- **根本原因:** アプリ内通知を `@Async` スレッドで作成していたため、スレッドの失敗が完全にサイレントになっていた。加えて `SubscriptionController` が `createInvoice()` の例外を握りつぶしてユーザーに「送信しました」と表示していた
- **変更内容:**
  1. **`BillingNotificationService` を同期/非同期に分離:**
     - `sendInvoiceIssuedInApp()` / `sendPaymentConfirmedInApp()` — **同期メソッド**（新規）。呼び出し元スレッドで実行し、失敗は即座に例外として伝わる
     - `notifyInvoiceIssued()` / `notifyPaymentConfirmed()` — **`@Async` はメール・LINE のみ**に限定
  2. **`BillingService` の呼び出し順を変更:**
     - `createInvoice()` と `registerPayment()` で「同期アプリ内通知 → 非同期メール/LINE」の順に実行
  3. **`SubscriptionController.renewRequest()` のエラーハンドリングを修正:**
     - `createInvoice()` 例外発生時はエラーメッセージをユーザーに表示してリダイレクト（サイレント握りつぶし廃止）
     - 成功メッセージを「通知センターをご確認ください」に変更
  4. **通知再送エンドポイントを追加:**
     - `BillingService.resendNotification(invoiceId)` — 既存請求書に対して同期アプリ内通知 + 非同期メール/LINE を再送
     - `POST /app/admin/billing/{id}/resend-notification` （`BillingController`）
     - `detail.html` に「📨 お支払い案内を再送する」ボタンを追加

#### D-15 — ドキュメント更新（F-015 通知改善）
- **ファイル:** `CLAUDE.md`、`backend/docs/requirements.md`、`docs/07-specification.html`
- **修正内容:**
  - URL構造に `POST /app/admin/billing/{id}/resend-notification` を追加
  - F-011 の通知フロー表に「実行方式（同期/非同期）」列を追加し、処理フロー・エラー動作を実装に合わせて更新
  - 再送ボタンの説明を各ドキュメントに追記

---

#### F-14 — 予約枠管理: 過去日付の入力を制限
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/controller/AppointmentSlotController.java`
  - `backend/src/main/resources/templates/appointments/slot-management.html`
- **変更内容:**
  - `page()` に過去日チェック（`targetDate.isBefore(LocalDate.now())`）を追加。過去日の場合は `error` を model に追加し `daySlots` を渡さない
  - テンプレートのスロット一覧・追加枠フォームを `th:if="${daySlots != null}"` で制御。過去日では非表示
  - 日付ピッカーに `th:min="${today}"` を付与し、ブラウザレベルでも過去日選択を防止

#### F-13 — 請求発行時にアプリ内通知を追加
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/BillingNotificationService.java`
- **変更内容:**
  - `NotificationMapper` を DI し、`sendInAppNotification()` プライベートメソッドを追加
  - `notifyInvoiceIssued()`: メール・LINE に加えてユーザーのアプリ内通知センターにも「💳 お支払いのご案内」を送信
  - `notifyPaymentConfirmed()`: 同様に「✅ お支払い確認のご連絡」をアプリ内通知として送信
  - SendGrid・LINE 未設定の環境でも通知センター (`/app/notifications`) 経由で必ず届くようになった
  - 各チャンネル（アプリ内/メール/LINE）の失敗は独立して catch され、他チャンネルの送信に影響しない

#### Bug Fix — 請求管理テンプレートの `totalPages` エラーを修正
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/dto/common/PageResponse.java`
- **変更内容:**
  - `totalPages()`・`hasPrev()`・`hasNext()` の3メソッドを `PageResponse<T>` record に追加
  - `admin/billing/index.html` が参照していた `page.totalPages`・`page.hasPrev`・`page.hasNext` が Thymeleaf SpEL で解決できず 500 エラーになっていたのを修正

#### D-14 — docs/07-specification.html を実装に整合（F-006・F-011）
- **ファイル:** `docs/07-specification.html`
- **修正内容:**

  **F-006 予約管理**
  - 「予約枠管理」ブロックを新規追加:自動生成/追加枠/ブロック枠の3種類の説明・`is_blocked` フラグの意味・過去日付制限の動作を記載

  **F-011 請求・決済管理**
  - `機能概要` ヒントを「スキーマ実装済み・UI未実装」→「実装済み（BillingController / BillingService / BillingNotificationService）」に更新
  - 「通知チャンネルと優先順位」ブロックを追加: ①アプリ内通知（必須）→ ②メール（SendGrid 設定時）→ ③LINE（設定時）の3段階と条件を明記
  - 通知トリガー表を更新: 「通知先」列を「通知チャンネル」に変更し、アプリ内通知を追加
  - 「処理フロー」を実装コード（`BillingService`・`BillingNotificationService`・@Async）に合わせて全面改訂
  - 「エラー時の動作」を更新: 通知チャンネルが独立して失敗する設計を明記

---

#### D-13 — ドキュメント一括更新（過去日制限・請求通知・PageResponse）
- **ファイル:** `CLAUDE.md`、`backend/docs/requirements.md`
- **修正内容:**
  - `requirements.md` F-006: 予約枠管理の過去日付制限・追加枠/ブロック枠の仕様を追記
  - `requirements.md` F-011: 実装状況を「スキーマのみ」→「実装済み」に更新。通知フロー表にアプリ内通知（必須）を追加し、チャンネル優先順位を明記
  - `CLAUDE.md` URL構造: `/app/admin/appointment-slots/**` に過去日制限の説明を追記
  - `CLAUDE.md` 実装状況 F-011: 通知チャンネルの記述を更新（アプリ内通知必須・メール/LINE は設定時のみ）

---

#### F-11 — 診療情報印刷に AI 症状チェック履歴を追加
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/controller/HealthRecordController.java`
  - `backend/src/main/resources/templates/health/print.html`
- **変更内容:**
  - `HealthRecordController.printMedicalSummary()` に `SymptomCheckMapper` を DI し、`symptomCheckMapper.findRecentByPetId(petId, 100)` を model に追加
  - `print.html` に「AI症状チェック履歴」セクションを追加（診療履歴と健康記録の間）
  - 出力項目: 実施日時・症状の種類・発症時期・重症度（HIGH=赤/MEDIUM=橙/LOW=緑）・推奨対応（受診/相談/様子見 を色分け）・AIガイダンス本文

#### D-13 — ドキュメント一括更新（印刷機能・予約枠管理）
- **ファイル:**
  - `CLAUDE.md`
  - `backend/docs/requirements.md`
  - `backend/docs/db-design.md`
  - `docs/07-specification.html`
- **修正内容:**

  **CLAUDE.md（3件）**
  1. URL構造に `GET /app/pets/{petId}/health-records/print` を追加
  2. Implementation Status の「Schema only」を修正: F-013 診療情報印刷は実装済みとして記載（AI症状チェック履歴を含む）、汎用 PDF ダウンロードのみ未実装に変更

  **backend/docs/requirements.md（2件）**
  1. F-009 AI症状チェック: 印刷出力への連携（print エンドポイントに履歴が含まれること）を追記
  2. F-013: 「未実装」→「部分実装」に改訂。診療情報印刷の実装詳細（エンドポイント・出力項目5種）と、汎用 PDF ダウンロードが未実装であることを分離して記載

  **backend/docs/db-design.md（1件）**
  1. `appointment_slots` テーブル: `is_blocked BOOLEAN NOT NULL DEFAULT FALSE` カラムを追加。テーブルの役割説明も「追加枠 / ブロック枠」の二種類を明記

  **docs/07-specification.html（2件）**
  1. F-009 セクション: 「印刷出力への連携」ブロックを追加（print エンドポイント・出力項目を記載）
  2. F-013 セクション: タイトルを「PDF出力」→「印刷・PDF出力」に変更。実装済み（診療情報印刷）と未実装（汎用 PDF）を分離して記載。出力項目5種を番号付きリストで明示

---

#### E-3 — Docker Compose によるローカル起動サポート
- **新規ファイル:**
  - `docker-compose.yml` — PostgreSQL 16 + Spring Boot アプリの2サービス構成
  - `.env.example` — 環境変数のサンプル（`.env` にコピーして使用）
- **変更ファイル:**
  - `application.properties` — `spring.sql.init.mode=always` → `${SQL_INIT_MODE:always}` に変更（環境変数で上書き可能）
  - `CLAUDE.md` — 起動方法を Docker 中心に全面更新
- **docker-compose.yml の設計:**
  - `db` サービス: `postgres:16-alpine` / `pgdata` ボリュームでデータ永続化 / healthcheck で起動完了を確認
  - `app` サービス: 既存 `Dockerfile` を使用（本番 Render 共通） / `db` の healthcheck 通過後に起動 / `entrypoint` 上書きでポートを 8080 に変更
  - `.env` ファイル経由で全外部サービス（OpenAI / Groq / SendGrid / Slack / LINE / Zoom）を注入可能
- **起動コマンド:**
  ```bash
  docker compose up --build          # 初回 / 再ビルド
  docker compose up -d               # バックグラウンド起動
  docker compose down -v && docker compose up --build  # DB 含め完全再構築
  ```

---

#### E-2 — DB 再構築: schema.sql + data.sql による完全自動セットアップ
- **ファイル:**
  - `backend/src/main/resources/data.sql`
  - `backend/src/main/resources/application.properties`
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/java/com/example/petlife/config/DataInitializer.java`
  - `CLAUDE.md`
- **変更内容:**

  **根本原因:** `spring.sql.init.mode=never` かつ `DataInitializer` (CommandLineRunner) がユーザーを作成していたため、`data.sql` の pets/subscriptions INSERT が JOIN 失敗していた（ユーザーが存在しない状態で data.sql が実行される順序問題）

  **data.sql の再構築（全面改訂）**
  - ユーザー 7 件（super/admin/vet1/staff1/owner1/owner2/owner3）を BCrypt ハッシュ付きで追加。`ON CONFLICT (email) DO NOTHING` で冪等性を保証
  - 実行順序を明示: roles → users → plans → plan_features → pets → health_records → pet_care_records → subscriptions
  - シーケンスフィックスに `users` テーブルを追加
  - 開発者アカウント（h4mizoo@gmail.com）も同セクションに統合

  **application.properties**
  - `spring.sql.init.mode=never` → `spring.sql.init.mode=always` に変更
  - 両 SQL ファイルは冪等のため毎起動実行しても安全

  **schema.sql のコメント整理**
  - ヘッダーを「自動実行される」旨に更新。DB 再構築コマンドを追記
  - payments・appointment_slots の既存 DB 向け ALTER TABLE コメントを「新規 DB では不要」と明記

  **DataInitializer の簡略化**
  - `updateSeedUserByEmail`（毎起動 BCrypt 再計算）を廃止
  - `insertIfAbsent()`（存在しない場合のみ挿入）に変更
  - 通常は data.sql が先に実行されるため何もしない。セーフティネットとして残存

  **CLAUDE.md のセットアップ手順を全面更新**
  - 初回セットアップ・完全再構築・手動適用・本番無効化の手順を追記
  - Default Credentials セクションを「seeded by DataInitializer」→「seeded by data.sql」に更新

  **DB 再構築手順:**
  ```bash
  dropdb -U postgres petlifeplus
  createdb -U postgres petlifeplus
  mvn spring-boot:run   # schema.sql → data.sql → DataInitializer の順で自動実行
  ```

---

#### F-10 — 予約枠管理: 自動生成ベース + スタッフによる追加・ブロック機能
- **ファイル:**
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/java/com/example/petlife/entity/AppointmentSlotEntity.java`
  - `backend/src/main/java/com/example/petlife/mapper/AppointmentSlotMapper.java`
  - `backend/src/main/java/com/example/petlife/service/AppointmentService.java`
  - `backend/src/main/java/com/example/petlife/service/AppointmentSlotService.java`
  - `backend/src/main/java/com/example/petlife/controller/AppointmentSlotController.java`
  - `backend/src/main/resources/templates/appointments/slot-management.html`
- **変更内容:**
  - `appointment_slots` テーブルに `is_blocked BOOLEAN NOT NULL DEFAULT FALSE` カラムを追加。既存 DB への手動 ALTER 手順もコメントに追記
  - **`is_blocked = false`（追加枠）:** 自動生成（9:30-17:00 30分刻み）に上乗せするスロット。業務時間外や特別診療枠に使用
  - **`is_blocked = true`（ブロック枠）:** 特定の自動生成スロットを非表示にする。休診・院内研修など
  - `AppointmentService.generateAvailableSlots()` に `AppointmentSlotMapper` を DI し、3段階マージロジックを実装:
    1. 9:30-17:00 を自動生成（ベース）
    2. `findBlockedOnDate()` でブロック枠を取得し除外
    3. `findExtraOnDate()` で追加枠を取得してマージ（重複1つに集約・ソート）
  - `AppointmentSlotMapper` に `findBlockedOnDate(LocalDate)` / `findExtraOnDate(LocalDate)` を追加。既存 SELECT クエリに `is_blocked` を追加
  - `AppointmentSlotService.create()` / `AppointmentSlotController.create()` に `isBlocked` パラメータを追加
  - `slot-management.html` を全面改修: 種別（追加枠/ブロック枠）のカード型セレクター・仕組み説明・一覧表に種別バッジを追加
- **アクセス制御:** 既存の `canManageOperations()` を継承（ADMIN/SUPER/VET/STAFF が操作可）

---

#### D-12 — 第2回全体監査: コード実態調査による不整合修正
- **ファイル:**
  - `CLAUDE.md`
  - `backend/docs/requirements.md`（F-006）
  - `backend/docs/test_report.md`
- **調査方法:** Explore エージェントによる全コントローラー・サービス・マッパー・スキーマの実態調査
- **修正内容:**

  **CLAUDE.md（4件）**
  1. **アーキテクチャ説明の修正** — 「no REST API — Thymeleaf renders all views」→「Thymeleaf renders all app views」に変更し、`/api/appointments` に `@RestController` の JSON REST API が存在することを注記
  2. **予約サブエンドポイントを URL 構造に追加** — `approve`/`reject`（VET/STAFF/ADMIN/SUPER のみ）/`cancel`（オーナーのみ）/`delete-selected` の4エンドポイントが欠落していたため追加
  3. **`POST /app/pets/{id}/decease`（ペット永眠マーク）を追加** — `PetController` に実装済みだがドキュメント未記載
  4. **Default Credentials テーブルを修正** — `DataInitializer` はユーザーのみ作成（サブスクリプション/プラン紐付けは `data.sql` が必要）という実態を明記。Light プランが予約ページにアクセスできないことを注記

  **backend/docs/requirements.md F-006（3件）**
  1. **プランアクセス制限を追記** — `AppointmentPageController.ensureAccessible()` により Light プランユーザーは 400 エラーとなる（STANDARD以上のみ利用可）を明記
  2. **エンドポイント一覧を追加** — `approve`/`reject`/`cancel`/`delete-selected` の4エンドポイントと操作可能ロールを整理
  3. **空き枠自動生成の説明を追加** — 9:30-17:00、30分刻みで `generateAvailableSlots()` が自動生成（手動枠は未使用）

  **backend/docs/test_report.md（2件）**
  1. **F-006 テスト対象に予約承認・却下・キャンセルを追加** — 機能別テスト項目表を更新
  2. **テストケース TC-036〜TC-040 を追加** — 承認/却下/権限外/キャンセル/Light プランアクセス制限

---

#### D-11 — backend/docs/* vs docs/* 相違点チェック・修正
- **ファイル:**
  - `backend/docs/db-design.md`
  - `docs/08-db-design.html`
  - `backend/docs/test_report.md`
- **修正内容:**

  **backend/docs/db-design.md（6件）**
  1. `users` テーブルに `slack_user_id` / `line_user_id` カラムを追加（schema.sql に存在するが欠落していた）
  2. `payments.payment_method` の CHECK制約を `(CARD/BANK/OTHER)` → `(BANK/PAYPAY/OTHER)` に修正（CHANGELOG D-9 のスキーマ変更に追随）
  3. `payments` テーブルに `deleted_at` / `updated_at` カラムを追加（schema.sql に存在するが欠落していた）
  4. `plan_features` テーブル定義を新規追加（定義が丸ごと欠落していた）
  5. `password_reset_tokens` テーブル定義を新規追加（定義が丸ごと欠落していた）
  6. ER図リレーション一覧に `[users] 1 ──── * [password_reset_tokens]` を追加

  **docs/08-db-design.html（4件）**
  1. `invoices` テーブルのnote: 「スキーマのみ実装済み（コントローラー・サービス未実装）」→「実装済み（BillingController / BillingService）」
  2. `payments` テーブルのnote: 同様に実装済みに修正
  3. `payments.payment_method` の CHECK制約を `CARD/BANK/OTHER` → `BANK/PAYPAY/OTHER` に修正（テーブル定義）
  4. Mermaid ER図内 `payments` エンティティの `payment_method "CARD/BANK/OTHER"` → `"BANK/PAYPAY/OTHER"` に修正
  5. `payments` テーブルに `deleted_at` / `updated_at` 行を追加

  **backend/docs/test_report.md（3件）**
  1. テスト対象機能一覧: F-022 チャットボット相談を追加。F-016「Zoom オンライン診療」の機能IDを requirements.md と整合（要件定義上のIDなし → F-Premium 表記）
  2. 機能別テスト項目表: F-022 行を追加（マルチターン会話・Groq API・フォールバック・全プランアクセス）
  3. テストケース一覧: TC-031〜TC-035 を追加（F-022 正常/異常/境界値/フォールバック）

---

#### D-10 — プロジェクト全体監査: ドキュメント vs 実装 整合性修正
- **ファイル:**
  - `CLAUDE.md`
  - `backend/docs/ai-features.md`
- **修正内容:**

  **CLAUDE.md（7件）**
  1. **URL構造** — 欠落していた4エンドポイントを追加:
     - `/app/forgot-password`, `/app/reset-password`（`ForgotPasswordController`）
     - `/app/admin/billing/**`（`BillingController`）
     - `/app/admin/line/push`（`LinePushController`）
     - `/app/admin/**` を分解し各パスのロール制御を明記
  2. **ロールアクセス制御** — `/app/consultations/**` の許可ロールを "VET / STAFF / ADMIN" → "SUPER / VET / STAFF" に修正（`SecurityConfig` 実態に合わせる）。`/app/admin/users/**` 等の個別ロールも追記
  3. **Authentication** — `SUPER` ロールが欠落していたため追加。SUPER は ADMIN と同等のアクセス権を持つことを明記
  4. **External Service Configuration** — 欠落していた2サービスを追加:
     - Chatbot (Groq): `CHATBOT_API_KEY`, `CHATBOT_MODEL`, `CHATBOT_BASE_URL`（F-022専用）
     - SendGrid: `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`（請求メール・パスワードリセットメール）
     - OpenAI行を「F-009 のみ」に修正（チャットボットは `chatbot.*` を使用する旨を分離）
     - LINE行に `LinePushController` を追記
  5. **Database Schema** — テーブル数を23→24に修正。`password_reset_tokens` を Auth カテゴリに追加
  6. **Implementation Status** — 請求・決済（F-011）を「Schema only」→「Implemented」に修正。`BillingController`・`BillingService`・`BillingNotificationService` が実装済み
  7. **Implementation Status** — チャットボット（F-022）の記述を修正: プランゲートなし（全プラン利用可）・`chatbot.*` 設定使用に更新。テーブル・マッパー数を22→24に修正

  **backend/docs/ai-features.md（3件）**
  1. **概要表** — `OpenAI temperature` 行を `API 設定` + `temperature` の2行に分解。チャットボットが `chatbot.*`（Groq）を使用することを明記
  2. **概要表** — プランゲート行: チャットボットを "STANDARD 以上" → "なし（全プラン利用可）" に修正
  3. **§2 AI 呼び出し詳細** — エンドポイント・モデルを `${openai.*}` → `${chatbot.*}` に修正（実装の `ConsultChatService` と一致させる）
  4. **§3 設定プロパティ** — F-009（openai.*）と F-022（chatbot.*）を別セクションに分離。プランゲートも F-009 のみ適用と明記

---

### 環境設定

#### E-1 — チャットボット用 Groq API キーを application-local.properties に設定
- **ファイル:** `backend/src/main/resources/application-local.properties`（gitignore 対象）
- **変更:** `CHATBOT_API_KEY=gsk_...` を追記。`application.properties` のプレースホルダー `${CHATBOT_API_KEY:}` 経由で `chatbot.api-key` に反映される
- **動作:** チャットボット相談（`/app/consult/chatbot`）が Groq `llama-3.1-8b-instant` による LLM 応答に切り替わる。全プランのユーザーが AI 応答を利用可能

---

### ドキュメント修正

#### D-2 — style.css の二重管理を解消・CLAUDE.md に同期手順を明記
- **ファイル:**
  - `backend/src/main/resources/static/assets/css/style.css`
  - `CLAUDE.md`
- **変更:**
  - `frontend/public/assets/css/style.css`（正規ファイル）と `backend/src/main/resources/static/assets/css/style.css`（コピー）の差分を解消。欠落していた `.gallery-section .image-card img.gallery-fill` ルールおよび `#pricing` スクロールマージン定義をバックエンド側に反映
  - `CLAUDE.md` の「Frontend」セクションに、2 ファイルの役割・同期手順の注記を追加
- **原因:** ドキュメント監査で、フロントエンド CSS を更新した際にバックエンド側のコピーが更新されておらず差分が発生していたことを確認

#### F-8 — チャットボット相談を全プラン無料開放・Groq API（無料枠）対応
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/ConsultChatService.java`
  - `backend/src/main/resources/application.properties`
  - `backend/src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- **変更:**
  - `ConsultChatService` からプランゲート（`assertEligible()` / `PlanAccessService` 依存）を完全除去。チャットボット相談を全ユーザーが利用可能に
  - API 設定を `openai.*` から独立した `chatbot.*` プロパティに分離。デフォルトを Groq 無料枠（`llama-3.1-8b-instant` / `https://api.groq.com/openai/v1`）に設定
  - `application.properties` に `chatbot.api-key` / `chatbot.model` / `chatbot.base-url` を追加
  - `additional-spring-configuration-metadata.json` に `chatbot.*` 3プロパティを登録（IDE 警告を解消）
- **動作:**
  - `CHATBOT_API_KEY` 未設定 → キーワードベース `fallbackReply()`（全5段階フロー）で応答。API コストゼロ
  - `CHATBOT_API_KEY` 設定済み → Groq API（または任意の OpenAI 互換 API）で LLM 応答
  - AI 症状チェック（F-009）は引き続き `openai.*` を使用し、プランゲートも維持

#### F-9 — 請求・決済管理を実装（F-011）
- **新規ファイル:**
  - `dto/billing/InvoiceRow.java` — 請求書一覧・詳細用 JOIN DTO（invoice + subscription + plan + user）
  - `dto/billing/PaymentForm.java` — 入金登録フォーム DTO（paidAmount / paidAt / paymentMethod / transactionRef）
  - `service/BillingNotificationService.java` — メール（SendGrid SMTP）+ LINE push 通知サービス
  - `service/BillingService.java` — 請求発行・入金登録・状態更新のコアサービス
  - `controller/BillingController.java` — `GET/POST /app/admin/billing/**`（ADMIN のみ）
  - `templates/admin/billing/index.html` — 請求一覧画面
  - `templates/admin/billing/detail.html` — 請求詳細 + 入金登録フォーム
- **変更ファイル:**
  - `mapper/InvoiceMapper.java` — `findAllWithDetails()` / `findByIdWithDetails()` を追加（JOIN クエリ）
  - `mapper/SubscriptionMapper.java` — `findMonthlyFeeBySubscriptionId()` を追加
  - `controller/SubscriptionController.java` — 更新申請時に `BillingService.createInvoice()` を呼び出し請求書を発行。成功メッセージを「お支払い案内をメール・LINEでお送りします」に更新
  - `templates/fragments/nav.html` — 運営管理セクションに「💳 請求管理」リンクを追加
- **フロー:**
  1. ユーザーが `POST /app/subscriptions/{id}/renew-request` → 請求書を自動発行 → 顧客へメール（HTML）+ LINE でお支払い案内を非同期送信
  2. 管理者が `/app/admin/billing/{id}` で入金登録 → 累計入金額が請求額以上で PAID → 領収確認メールを自動送信
- **支払い方法:** `BANK`（銀行振り込み）/ `PAYPAY` / `OTHER`

#### D-9 — F-011 請求・決済管理の仕様を確定（通知フロー・PayPay 対応）
- **ファイル:**
  - `backend/src/main/resources/schema.sql`
  - `backend/docs/requirements.md`
  - `docs/07-specification.html`
- **変更内容:**
  - `schema.sql`: `payments.payment_method` の CHECK 制約を `('CARD','BANK','OTHER')` → `('BANK','PAYPAY','OTHER')` に変更（CARD 削除・PAYPAY 追加）。既存 DB への手動 ALTER TABLE 手順もコメントに追記
  - `requirements.md` F-011: 仕様を全面更新。通知トリガー表（新規申し込み・契約更新申請・入金確認完了）、支払い方法表（BANK / PAYPAY / OTHER）、通知連携（メール F-015・LINE）、処理フロー、入金登録の入力項目を追加
  - `07-specification.html` F-011: 同内容を HTML 形式で反映（テーブル形式の通知トリガー・支払い方法・処理フロー・入力項目）
- **確定仕様:**
  - 通知タイミング: 新規申し込み時・契約更新申請時（end_date 30 日前）・入金確認完了時
  - 通知手段: メール（F-015）+ LINE（LINE Messaging API）
  - 支払い方法: 銀行振り込み（BANK）/ PayPay（PAYPAY）

#### D-8 — 実装との乖離（重大度：高）3件をドキュメントに反映
- **ファイル:**
  - `backend/docs/requirements.md`
  - `docs/07-specification.html`
  - `petlife_plus.md`
- **変更内容:**

  **3-1: F-003 ユーザー管理 — 連携ステータス表示が未記載**
  - `requirements.md` F-003: 機能説明・画面レイアウト・入力項目に LINE/Slack/Zoom 連携ステータスチップ（`.int-chip`）の説明と Slack User ID / LINE User ID フィールドを追加
  - `07-specification.html` F-003: 同内容を反映（機能概要・画面レイアウト更新、入力テーブルに2行追加）

  **3-2: F-006 予約管理 — 承認・却下フローが未記載**
  - `requirements.md` F-006: ステータス遷移表（REQUESTED→CONFIRMED/CANCELED/COMPLETED）、承認・却下エンドポイント、来院/オンライン選択、役割ベースのアクセス制御を追加
  - `07-specification.html` F-006: 同内容を反映（ステータス遷移表・承認却下フロー・予約方法フィールド追加）

  **3-3: Slack 一斉送信 — 未実装なのに LINE と対称であるかのように読める**
  - `petlife_plus.md`: Slack bot セクションに「一斉送信（Broadcast）は未実装」を明記。LINE Messaging API セクションに「Broadcast 実装済み（`LineBotService.broadcastMessage()`）」を明記。将来の実装方針も付記
- **原因:** 実装済み機能（連携ステータス表示・承認却下フロー）がドキュメントに反映されておらず、かつ未実装の Slack Broadcast が LINE と対称に見える記述になっていた

#### D-7 — docs/07-specification.html を requirements.md に整合させて全面修正
- **ファイル:** `docs/07-specification.html`
- **変更内容（8箇所）:**
  1. **システム構成** — DB を「H2 / MySQL」→「PostgreSQL」、アーキテクチャを「Controller → Service → Repository」→「Controller → Service → Mapper (MyBatis)」に修正
  2. **機能一覧テーブル** — F-011 を「通知・リマインド配信」から「請求・決済管理」に変更し F-012〜F-015 を renumber、F-022（チャットボット相談）を追加
  3. **画面遷移図** — 「症状チェック画面」→「チャットボット相談画面」に更新（AI症状チェックはペット詳細画面内のため独立画面ではない）
  4. **F-009 機能詳細** — 概要・画面レイアウト・処理フローを実装に合わせて修正（独立画面→ペット詳細ページ内フォーム）
  5. **F-009 入力項目** — 症状種別を Enum 必須→ String @NotBlank、発症時期を必須→任意 に修正
  6. **F-007 添付ファイル上限** — 5MB → 10MB に修正
  7. **セキュリティ要件** — 「JPA/Repository」→「MyBatis Mapper の `#{}`」に修正
  8. **機能詳細セクション** — F-011 請求・決済管理を新規追加、F-012〜F-015 のタイトルを renumber、F-022 チャットボット相談セクションを新規追加
- **原因:** 07-specification.html（Ver 1.0 / 2026-05-14）が requirements.md の後続更新に追随しておらず、機能番号・DB・ORM・F-009 仕様がすべて実装と乖離していた

#### D-6 — requirements.md に F-022（チャットボット相談）を正式追加・整理
- **ファイル:** `backend/docs/requirements.md`
- **変更:**
  - 機能一覧テーブルの F-022 行を F-009 直後（誤った位置）から F-015 末尾に移動し、番号順（F-001〜F-015, F-022）に修正
  - 機能詳細セクションの F-022 も同様に F-015 直後に移動
  - 詳細仕様内の「OpenAI API（またはフォールバック）」を「Groq API（またはキーワードベースフォールバック）」に修正（Groq 対応後の実態に合わせる）
  - F-022 詳細に F-009 との比較表（ターン数・ペット紐付け・利用プラン・AI エンジン）を追加し、2機能の区別を明確化
- **原因:** D-5 で F-022 を挿入した際に F-009 直後という誤った位置に配置し、また Groq 移行後の記述更新が漏れていた

#### D-5 — AI症状チェックとチャットボット相談の技術・インターフェイスを整理
- **ファイル:**
  - `backend/docs/ai-features.md` *(新規)*
  - `backend/docs/requirements.md`
  - `CLAUDE.md`
- **変更:**
  - `backend/docs/ai-features.md` を新規作成。AI症状チェック（F-009）とチャットボット相談（F-022）の目的・ファイル構成・入出力インターフェイス・DBテーブル・OpenAI パラメータ・フォールバック階層を表形式で整理
  - `requirements.md` の F-009 詳細仕様を実装に合わせて更新（入力フィールド名・バリデーション・画面遷移先）
  - `requirements.md` に F-022（チャットボット相談）を新規追加。要件表と詳細仕様セクションの両方に記載
  - `CLAUDE.md` の OpenAI 外部連携行・実装状況セクションで両機能を明確に区別して記載
- **原因:** 2機能が同じ OpenAI 設定・同じプランゲートを使用するため、ドキュメント上で混同されていた。また F-022 は実装済みにもかかわらず requirements.md に要件項目が存在しなかった

#### D-4 — ui-design.md と CLAUDE.md のカラー・レイアウト定義を実装に合わせて修正
- **ファイル:**
  - `frontend/docs/ui-design.md`
  - `CLAUDE.md`
- **変更:**
  - `ui-design.md` のカラーパレット・ボタン CSS サンプル・フォームフォーカス色・レイアウトルール（コンテンツ幅・余白・角丸・シャドウ・ブレークポイント）を `style.css` の実装値に合わせて全面更新
  - CSS変数セクションを実際の `:root` 定義（`--brand`, `--accent`, `--bg` 等）に差し替え
  - `CLAUDE.md` の「Frontend / UI conventions」カラー記載を `#1D4ED8`（青）から `#FF8FB1`（ブランドピンク）に修正し、スペーシング・角丸・ブレークポイントも実装値に更新
- **原因:** `ui-design.md` が「参照元: docs/06-design-guide.html」と記載しながら内容が古い青系スキームのままだった。`style.css` の実測値（`--brand: #ff8fb1`、`--space-section: 36px`、カード角丸 26px 等）はすべて `06-design-guide.html` のピンク系スキームと一致しており、`ui-design.md` と CLAUDE.md が誤情報を伝播させていた

#### D-3 — フロントエンド assets の二重管理を自動化（Method A + B）
- **ファイル:**
  - `backend/src/main/resources/application.properties`
  - `backend/pom.xml`
- **変更:**
  - `spring.web.resources.static-locations` の順序を変更。`file:` パスを先頭に移動し `classpath:/static/` を末尾へ。開発時はフロントの `frontend/public/` を直接配信するため、CSS/JS/画像の編集が即反映される（ホットリロード）
  - `pom.xml` に `maven-resources-plugin` を追加。`prepare-package` フェーズで `frontend/public/assets/`（css・js・img）を `target/classes/static/assets/` へ自動コピー。`mvn package` 実行時は常に最新フロントファイルが JAR に含まれる
- **原因:** `style.css`・`main.js`・画像ファイルがフロントとバックエンドの両方に存在し、手動同期が必要な状態だった。開発時は file: パス優先（Method A）、本番ビルド時は Maven 自動コピー（Method B）の組み合わせで二重管理を解消

---

## [2026-05-27]

### UI改善

#### F-7 — 診療予約: 来院 / オンライン選択をカード型UIに変更
- **ファイル:** `backend/src/main/resources/templates/appointments/index.html`
- **変更:**
  - 予約方法セレクターをプレーンなラジオボタン → カード型セレクターに刷新
    - 来院: 🏥 アイコン付き、選択時はティール（#0EA5A4）ハイライト
    - オンライン: 💻 アイコン付き、選択時はインディゴ（#6366F1）ハイライト
    - スタンダードプラン以下のユーザーはオンラインカードを🔒アイコン付きでグレーアウト表示
  - 予約一覧の「方法」列を画像アイコン単体 → 「🏥 来院」「💻 オンライン」バッジ表示に変更
  - 解説ガイド内のアイコン表示もバッジに統一
  - CSS: `.channel-opt`（カードUI）・`.ch-badge`（一覧バッジ）スタイルを追加

### 機能追加

#### F-1 — LINE プッシュメッセージ送信機能
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/line/LineBotService.java`
  - `backend/src/main/java/com/example/petlife/controller/line/LinePushController.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/mapper/UserMapper.java`
  - `backend/src/main/resources/templates/admin/line-push.html` *(新規)*
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更:**
  - `LineBotService` に `pushMessage()`（個別送信）・`multicastMessage()`（最大500人ずつ一斉送信）を追加
  - `UserMapper` に `findAllWithLineId()`（LINE ID 登録済みアクティブユーザー一覧）・`saveLineUserId()` を追加
  - `LinePushController` を新規作成（`GET/POST /app/admin/line/push`）
  - 管理画面テンプレート `admin/line-push.html` を新規作成（登録人数表示・送信フォーム・使い方ガイド）
  - サイドバーの運営管理メニューに「💬 LINE 一斉送信」リンクを追加
- **動作:** ADMIN が `/app/admin/line/push` から任意のメッセージを入力すると、`line_user_id` が設定されている全アクティブユーザーに LINE メッセージが送信される。500名超は自動分割送信

#### F-2 — LINE follow イベント: 友達追加時ウェルカムメッセージ
- **ファイル:** `backend/src/main/java/com/example/petlife/controller/line/LineEventController.java`
- **変更:** `follow` イベントタイプを処理するブランチを追加。友達追加時にウェルカムメッセージを自動返信
- **補足:** `message` イベントのみ処理していた既存ロジックを `eventType` 分岐に整理

#### F-3 — 相談チャットボット: OpenAI マルチターン対話対応
- **ファイル:** `backend/src/main/java/com/example/petlife/service/ConsultChatService.java`
- **変更:**
  - OpenAI API 連携を追加（`@Value` で `openai.*` 設定を注入、`RestTemplate` で呼び出し）
  - `generateReply()` を会話履歴付き OpenAI 呼び出し → フォールバックの構成に刷新
  - OpenAI システムプロンプト: 情報収集フェーズでは毎回トリアージを出さず、情報が揃った段階で初めて判定するよう指示
  - フォールバック（APIキー未設定時）: ターン数を計測し、症状→時期→頻度→全身状態の順に自然な一問一答で収集後、初めてトリアージ判定を提示するロジックに変更
  - キーワードセットをクラス定数（`SYMPTOM_WORDS` 等）に整理
- **原因:** 毎ターン「受け取った内容を一次トリアージしました」という同一テンプレートを返しており、会話が進展しなかった

#### F-4 — 相談チャットボット: 食欲不振 2 日以上で即時受診ルール
- **ファイル:** `backend/src/main/java/com/example/petlife/service/ConsultChatService.java`
- **変更:**
  - `APPETITE_LOSS_WORDS`（食べない・食欲がない・食欲不振など）と `TWO_OR_MORE_DAYS_WORDS`（2日・ふつか・3日間・48時間など）の定数セットを追加
  - フォールバック: 両セットが会話履歴内で揃った場合、緊急ワードと同等の即時受診メッセージを返す
  - OpenAI システムプロンプト: 食欲不振 2 日以上 → 「今すぐ受診」強く推奨のルールを追記

#### D-2 — docs/test_report_document.xlsx: テスト計画・テストケース一覧を Ver 2.0 に更新
- **ファイル:** `docs/test_report_document.xlsx`
- **変更:**
  - Sheet 1（テスト計画サマリー）: テスト対象範囲を F-001〜F-015 → F-001〜F-021 に更新
  - Sheet 2（テストケース一覧）: 30 件 → 55 件に全面更新。`docs/09-test-report.html` Ver 2.0 と内容を同期
  - Sheet 3（機能別テスト観点）: F-015 まで → F-001〜F-021 全 21 機能に拡張（LINE Broadcast・Slack・Zoom・予約枠・お知らせ・連携アイコン・統計を追加）
  - Sheet 4（テスト環境）: 変更なし
- **ツール:** Python + openpyxl で更新スクリプト（`docs/update_test_report.py`）を実行

#### D-1 — docs/09-test-report.html: テスト計画・テストケース一覧を Ver 2.0 に更新
- **ファイル:** `docs/09-test-report.html`
- **変更:**
  - ヘッダーを Ver 1.0 → Ver 2.0、更新日 2026-05-27 に変更
  - テスト計画（対象範囲）を現行実装（F-001〜F-021）に合わせて全面改訂。対象外欄に「請求書・決済・メール送信・PDF（未実装）」を明記
  - テストケースを 30 件 → 55 件に拡張。追加機能ごとの TC を網羅:
    - F-003 連携ステータスアイコン（TC-011/012）
    - F-006 予約承認ワークフロー・カレンダー反映・ロール別アクセス（TC-022〜027）
    - F-010 チャットボットマルチターン・食欲不振ルール（TC-034/035）
    - F-011 カレンダー複数種シール・VET/STAFF 予約表示（TC-037〜039）
    - F-015 LINE Broadcast・友達追加ウェルカム（TC-045〜048）
    - F-016 Slack Bot（TC-049）
    - F-017 Zoom オンライン診療（TC-050/051）
    - F-018 予約枠管理ロール制御（TC-052/053）
    - F-019 お知らせ管理・F-021 統計（TC-054/055）

#### B-8 — カレンダー: 承認済み予約が VET/STAFF のカレンダーに反映されない
- **ファイル:** `backend/src/main/java/com/example/petlife/service/CalendarService.java`
- **変更:** `buildMonthView()` 内の3箇所の条件を `isAdmin()` → `hasStaffAccess()` に変更
  - 予約サイドバー（`appointmentsByDate`）: ADMIN のみ → ADMIN/VET/STAFF 全員に拡張
  - 確定済み予約シール: `!isAdmin()` → `!hasStaffAccess()`（VET/STAFF が owner_user_id で誤検索していた問題を解消）
  - 予約枠表示（`availableSlotsByDate`）: `!isAdmin()` → `!hasStaffAccess()`（VET/STAFF に空き枠が誤表示されていた問題を解消）
- **原因:** スタッフ系ロール（VET/STAFF）が `isAdmin()` の判定でどちらの分岐にも正しく入らず、カレンダーに予約情報がゼロ表示になっていた。また `!isAdmin()` 側では `owner_user_id = 自分の ID` で検索するためスタッフには該当件数がゼロになり、かつユーザー向けの「空き枠」バナーも誤表示されていた

#### F-6 — LINE 一斉送信: Multicast → Broadcast に切り替え
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/line/LineBotService.java`
  - `backend/src/main/java/com/example/petlife/controller/line/LinePushController.java`
  - `backend/src/main/resources/templates/admin/line-push.html`
- **変更:**
  - `LineBotService` に `broadcastMessage()` を追加（`POST /v2/bot/message/broadcast`、`to` フィールドなし）
  - `LinePushController` を `multicastMessage()` から `broadcastMessage()` に切り替え。`UserMapper` 依存を削除
  - `line-push.html` の UI を Broadcast 向けに刷新。「LINE 登録ユーザー数」表示を廃止し「Bot の全フォロワーに届く」説明に変更。送信ボタンの無効条件を `!lineConfigured` のみに簡略化
- **理由:** Multicast は DB に `line_user_id` が登録済みのユーザーにしか届かなかった。Broadcast は Bot を友達追加した全ユーザーが対象でユーザーIDの個別登録が不要なため切り替え
- **補足:** `multicastMessage()` は将来の個別送信用途向けに `LineBotService` 内に保持

#### F-5 — ユーザー管理: LINE / Slack / Zoom 連携ステータスアイコン表示
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/PlanAccessService.java`
  - `backend/src/main/java/com/example/petlife/dto/user/UserResponse.java`
  - `backend/src/main/java/com/example/petlife/service/UserService.java`
  - `backend/src/main/java/com/example/petlife/controller/UserController.java`
  - `backend/src/main/resources/static/css/app.css`
  - `backend/src/main/resources/templates/admin/users/list.html`
  - `backend/src/main/resources/templates/admin/users/form.html`
- **変更:**
  - `PlanAccessService` に `resolveIntegrationStatusForUser(userId, roleId, slackUserId, lineUserId)` を追加。ロール 3（一般ユーザー）はプラン購読から機能可否を判定し、スタッフ系ロール（ADMIN/VET/STAFF）は全機能有効扱い
  - `UserResponse` に `integrationStatus` フィールドを追加
  - `UserService.toResponse()` で `planAccessService.resolveIntegrationStatusForUser()` を呼び出し、レスポンスに含める
  - `UserController.editForm` に `integrationStatus` モデル属性を追加
  - CSS に `.int-chip` / `.int-chip-line` / `.int-chip-slack` / `.int-chip-zoom` / `.int-chip-off` スタイルを追加
  - ユーザー一覧（table/list/grid 全ビュー）に連携チップを表示。table ビューは既存の Slack/LINE ID 生テキスト列を「連携」1列に集約
  - ユーザー編集フォームに連携ステータス欄を追加（Slack/LINE ID 入力欄の直下）
- **表示ロジック:**
  - LINE・Slack: プランに機能が含まれ、かつ該当 ID が登録済み → 色付き / それ以外（プラン外 or ID 未登録）→ グレー
  - Zoom: プランに機能が含まれる → 色付き / 未対応プラン → グレー
  - ホバーで「ID未登録」「プラン対象外」などのツールチップを表示

---

### バグ修正

#### B-1 — pets/list.html: canManagePets() 呼び出しによるレンダリングエラー
- **ファイル:** `backend/src/main/resources/templates/pets/list.html`
- **変更:** `currentUser.canManagePets()` → `currentUser.hasStaffAccess()` に4箇所置換（リストビュー・グリッドビュー・テーブルヘッダー・テーブルセル）
- **原因:** CHANGELOG L-2（2026-05-26）で `LoginUser.canManagePets()` を「使用箇所ゼロ」として削除したが、テンプレートの4箇所に残存。Thymeleaf がメソッド未定義で 500 エラーを返し、ユーザー系アカウントがペット一覧を表示・操作できなくなっていた
- **補足:** `canManagePets()` の意味（ADMIN/VET/STAFF のみサービスレベルバッジを表示）は `hasStaffAccess()` で完全に代替可能

#### B-2 — ダッシュボードテーマ: 管理系とユーザー系の色が区別しにくい
- **ファイル:** `backend/src/main/resources/static/css/app.css`
- **変更:**
  - `body.role-admin .sidebar`: 青グラデーション（`#1D4ED8 → #1E3A8A`）を明示
  - `body.role-user`: teal（`#0B8585`）→ **緑系**（`#16A34A` エメラルドグリーン）に変更
  - `body.role-user .sidebar`: 緑グラデーション（`#16A34A → #15803D`）
  - `body.role-user .btn-primary`: 緑グラデーション（`#22C55E → #15803D`）
- **原因:** ユーザー系テーマが teal（青緑）のため、管理系の青と視覚的に区別しにくかった。管理系=青、ユーザー系=緑 の設計意図を CSS で明確に表現

---

## [2026-05-26]

### セキュリティ・バグ修正

#### #7 — SymptomCheckMapper: ソフトデリートフィルター欠落
- **ファイル:** `backend/src/main/java/com/example/petlife/mapper/SymptomCheckMapper.java`
- **変更:** `findRecentByPetId` クエリに `AND deleted_at IS NULL` を追加
- **理由:** 全テーブルがソフトデリートを採用しているが、`symptom_checks` のクエリのみフィルターが抜けており、削除済みレコードが取得されていた

#### #8 — AppointmentPageController: 権限チェック不統一
- **ファイル:** `backend/src/main/java/com/example/petlife/controller/AppointmentPageController.java`
- **変更:**
  - `approve` / `reject`: `canManageClinical()` → `hasStaffAccess()`（ADMIN を含むよう修正）
  - `cancel`: `canManageClinical()` → `hasStaffAccess()`（ADMIN のすり抜けを防止）
  - エラーメッセージを実態に合わせて修正
- **理由:** `canManageClinical()` は VET + STAFF のみで ADMIN を除外するため、承認・却下操作が ADMIN にできなかった。キャンセルエンドポイントでは逆に ADMIN がオーナーとして処理されるバグがあった

#### #9 — DashboardController: N+1 クエリ
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/HealthRecordMapper.java`
  - `backend/src/main/java/com/example/petlife/controller/DashboardController.java`
- **変更:**
  - `HealthRecordMapper` に `countByOwnerUserId` (JOIN クエリ) を追加
  - `DashboardController` のペット件数ループを `countByOwnerUserId` の 1 クエリに置換
  - 不要な `List<PetEntity>` / `java.util.List` のインポートを削除
- **理由:** ペット数分だけ `countByPetId` を繰り返し呼ぶ N+1 が発生していた

#### #10 — index.html: 外部 QR コード API 依存
- **ファイル:** `frontend/public/index.html`
- **変更:** `<img src="https://api.qrserver.com/...">` を削除し、`qrcode.js` によるブラウザ内 QR コード生成に置換
- **理由:** LINE ID がクエリパラメータとして外部サービスに送信されていた。外部障害で機能停止するリスクもあった
- **備考:** `qrcode.js` は現在 jsDelivr CDN から読み込み。完全自己ホスティングには `assets/js/qrcode.min.js` を配置して参照先を変更すること

#### #11 — admin/users/list.html: ロール ID 数値ハードコード
- **ファイル:** `backend/src/main/resources/templates/admin/users/list.html`
- **変更:** `user.roleId == 1` 等の数値比較（3 箇所）を `user.roleDisplay` 文字列比較に統一
- **理由:** ロール ID が DB の物理値に依存しており、変更に弱かった

---

#### H-1 — ADMIN がアクセス不可ページで 403 のみ返す問題
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/config/SecurityConfig.java`
  - `backend/src/main/java/com/example/petlife/controller/AccessDeniedPageController.java` *(新規)*
  - `backend/src/main/resources/templates/error/access-denied.html` *(新規)*
- **変更:** `.exceptionHandling(e -> e.accessDeniedPage("/app/access-denied"))` を追加。アクセス拒否専用ページ（コントローラー + テンプレート）を新規作成
- **背景仕様:** 管理者アカウントはシステム管理専用。診療予約・カレンダー・診療記録へのアクセス不可は設計通り。ただし生の 403 ではなく説明メッセージを表示する必要があった

#### H-2 — AppointmentService: VET/STAFF が自分の予約しか見えない
- **ファイル:** `backend/src/main/java/com/example/petlife/service/AppointmentService.java`
- **変更:** `list()` / `listForApp()` / `deleteSelected()` の `isAdmin()` → `canManageClinical()` に変更
- **背景仕様:**
  - SUPER / VET / STAFF → 全予約を表示・操作
  - USER → 自分の予約のみ
  - ADMIN → 予約機能自体にアクセス不可（H-1 で制御）
- **理由:** `isAdmin()` では VET / STAFF / SUPER が自分の予約しか参照できず、承認・却下業務が実質不可能だった

---

#### M-3 — パスワードリセット: 古いトークンが無効化されない
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/PasswordResetTokenMapper.java`
  - `backend/src/main/java/com/example/petlife/service/PasswordResetService.java`
- **変更:**
  - `PasswordResetTokenMapper` に `invalidateByUserId` を追加（同ユーザーの未使用トークンを `used_at = NOW()` でマーク）
  - `initiateReset()` で新トークン INSERT 前に `invalidateByUserId` を呼び出し
- **理由:** 連続リクエストで `password_reset_tokens` テーブルが無制限に増加し、古いリンクも有効なままになっていた

---

### リファクタリング・設計改善

#### L-1 — ReportController: サービス層なし・ロール ID ハードコード
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/UserMapper.java`
  - `backend/src/main/java/com/example/petlife/dto/report/ReportStats.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/service/ReportService.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/controller/ReportController.java`
  - `backend/src/main/resources/templates/reports/index.html`
- **変更:**
  - `UserMapper` に `countByRoleCode(String)` を追加（roles テーブル JOIN でロールコード文字列で集計）
  - `ReportStats` record（全統計値を保持する DTO）を新規作成
  - `ReportService` を新規作成し、5 マッパーの注入と集計ロジックを集約
  - `ReportController` を `ReportService` 1本のみ注入する薄いコントローラーに変更
  - テンプレートの参照を `${userCount}` → `${stats.userCount}` 等に統一
- **理由:** `countByRoleId(1L)` 等の数値がハードコードされており、ビジネスロジックがコントローラーに混在していた

#### L-2 — LoginUser: @Deprecated メソッド残存
- **ファイル:** `backend/src/main/java/com/example/petlife/config/LoginUser.java`
- **変更:** 使用箇所ゼロの `canManagePets()` を削除
- **理由:** 廃止メソッドが残存し、どのメソッドを使うべきか混乱を招いていた

#### L-5 — AppointmentService.deleteSelected(): N+1 削除
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/AppointmentMapper.java`
  - `backend/src/main/java/com/example/petlife/service/AppointmentService.java`
- **変更:**
  - `AppointmentMapper` に `softDeleteByIds(List<Long>, LocalDateTime)` を追加（MyBatis `<foreach>` で `WHERE id IN (...)` の一括 UPDATE）
  - `deleteSelected()` の個別 `softDelete` ループを `softDeleteByIds` の 1 呼び出しに置換
- **理由:** 削除件数分だけ UPDATE を個別発行する N+1 が発生していた

#### L-4 — payments テーブル: deleted_at / updated_at 欠落
- **ファイル:**
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/java/com/example/petlife/entity/PaymentEntity.java`
  - `backend/src/main/java/com/example/petlife/mapper/PaymentMapper.java`
- **変更:**
  - `schema.sql` の `payments` テーブルに `deleted_at TIMESTAMP` と `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` を追加
  - `PaymentEntity` record に `deletedAt` / `updatedAt` フィールドを追加
  - `PaymentMapper` の全 SELECT に `deleted_at IS NULL` フィルターと新カラムを追加
  - `updateStatus` の `WHERE` 句に `AND deleted_at IS NULL` を追加（旧実装は `updated_at` カラム不存在のため実行時エラー）
  - `softDelete` メソッドを新規追加
- **理由:** 他の全テーブルがソフトデリートを採用しているが `payments` のみ `deleted_at` が欠落。さらに `updated_at` も存在しないにもかかわらず `updateStatus` が参照しており、実行時エラーになる潜在バグがあった
- **備考:** `CREATE TABLE IF NOT EXISTS` では既存 DB にカラムは追加されない。既存 DB には以下を手動実行すること:
  ```sql
  ALTER TABLE payments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
  ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
  ```

#### L-3 — AppointmentSlotController: サービス層なし
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/AppointmentSlotService.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/controller/AppointmentSlotController.java`
- **変更:**
  - `AppointmentSlotService` を新規作成（`list` / `create` / `delete` + `ensureAccess` を集約）
  - `delete` に存在確認（`findById`）を追加（旧実装で欠落）
  - コントローラーは `AppointmentSlotService` 1本のみ注入し、`BadRequestException` をキャッチして `FlashAttribute` に変換するだけ
- **理由:** マッパーを直接注入し、バリデーション・認可チェックがコントローラーに混在していた
