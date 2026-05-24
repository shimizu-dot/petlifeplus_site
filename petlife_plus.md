
# hshimizu プロジェクトアーカイブ（最新版）

## サービス名
ペットライフプラス（Pet Life Plus）

---

## コンセプト
忙しい飼い主でも、ペットの健康状態を日常的に把握し、早期の異変に気づき、適切なケアや診療につなげることができる「ペット健康管理プラットフォーム」を提供する。

キャッチコピー：
「ペットの健康を守る、新しい習慣。」

---

## ターゲットユーザー

### 主ターゲット
都市部で働く20〜40代の犬・猫の飼い主（共働き・単身世帯）。

### ユーザー像

1. 忙しく通院の時間が取りにくい会社員
2. ペットを家族として大切にしており健康意識が高い層
3. スマートフォンアプリやオンラインサービスに抵抗がないデジタル利用者

---

## 解決する課題と提供価値

### 課題

1. ペットの体調変化を日常的に把握しづらい
2. 症状が出たときに受診すべきか判断が難しい
3. 健康記録・相談・診療予約が分断されている

### 提供するもの

ペットの健康データを一元管理し、AI症状チェック、オンライン診療、訪問ケアを組み合わせた統合型健康管理サービス。

---

## もたらすベネフィット

・日常的な健康データの可視化
・体調変化の早期発見
・受診判断のサポート
・通院の負担軽減
・継続的な健康管理

---

## 具体的なサービスフロー

1. アプリ登録
2. ペット情報登録
3. 日常健康データ記録（体重・食事・運動）
4. AI症状チェック
5. オンライン相談
6. 必要に応じてオンライン診療
7. 訪問ケアまたは動物病院受診
8. 健康履歴の継続管理

※ AI症状チェックはStandard以上、ZOOMオンライン診療はPremiumのみ利用可能。

---

## 提供機能

### 健康管理機能

・体重記録
・食事記録
・運動記録
・ワクチン履歴
・通院履歴

### 医療サポート

・AI症状チェック
・オンライン診療
・獣医師相談
・訪問ケア
・Slackbot相談（無料Slack App連携）
・ZOOMオンライン診療（プレミアム優先サポート）

### AI症状チェック仕様（実装）
・Standard以上の会員が利用可能  
・入力項目：症状、発症時期、補足メモ  
・OpenAI API（モデル設定値 `openai.model`）を利用して応答  
・重症度（LOW/MEDIUM/HIGH）と推奨対応（OBSERVE/CONSULT/VISIT）を保存  
・履歴はペット単位で参照可能

### 管理機能

・健康データの可視化
・診療履歴管理
・通知・リマインド

---

## 市場と背景

ペットの家族化が進み、ペット関連市場は拡大している。特に健康管理、予防医療、ペット保険、オンライン相談などの需要が高まっている。

一方で、日常の健康記録、症状チェック、相談、診療を一体化したサービスは少なく、デジタルによる統合管理のニーズが高まっている。

---

## 競合

主な競合は以下の領域に分かれる。

1. ペット健康管理アプリ
2. 動物病院予約サービス
3. オンライン獣医相談サービス
4. ペット保険サービス

これらは単機能サービスが多く、健康管理から診療まで統合されたサービスはまだ少ない。

---

## 提供するソリューション

ペットの健康管理を「記録 → 分析 → 相談 → 診療」まで一貫してサポートするプラットフォームを提供する。

### ソリューションの特徴

1. 健康データの一元管理
2. AIによる症状チェック
3. オンライン診療と訪問ケアの連携
4. 継続的な健康管理サポート

---

## サービスの特徴

### 1. 日常健康データの蓄積
体重・食事・運動などの健康データを日常的に記録し、長期的な健康状態を可視化する。

### 2. AI症状チェック
ペットの症状を入力すると、受診の必要性や考えられる原因をAIが提示する。

### 3. オンライン診療
通院が難しい場合でも獣医師に相談できるオンライン診療を提供。

### 4. 訪問ケア
必要に応じて獣医師や専門スタッフが自宅訪問しケアを行う。

### 5. Slackbot相談（新規アップデート）
Slackの無料プランで利用可能なSlackbotを提供し、日常的な相談導線を強化する。  
ユーザーの相談メッセージを受け取り、症状キーワードに応じた一次案内を自動返信する。

### 6. ZOOMオンライン診療（新規アップデート）
プレミアム会員向け優先サポートとして、ZOOMによるオンライン診療予約を提供する。  
予約時にZOOM参加リンクを自動発行し、当日のオンライン診療へスムーズに接続できる。

---

## プラン別提供機能（最新版）

| 機能 | Light | Standard | Premium |
|---|:---:|:---:|:---:|
| 健康記録・ペット管理 | ◎ | ◎ | ◎ |
| 基本通知・リマインダー | ◎ | ◎ | ◎ |
| AI症状チェック | ✖ | ◎ | ◎ |
| チャットボット相談 | ✖ | ◎ | ◎ |
| Slack bot 連携 | ✖ | ◎ | ◎ |
| LINE bot 連携 | ✖ | ◎ | ◎ |
| Zoom オンライン診療 | ✖ | ✖ | ◎ |

機能コード（`plan_features` テーブルで管理）：  
`AI_SYMPTOM` / `SLACK_BOT` / `LINE_BOT` / `ZOOM_CONSULT`

---

## 外部連携仕様（実装）

### Slack bot
・Slack Events API 受信エンドポイント：`/api/slack/events`  
・メッセージイベントに対して自動返信（症状キーワード案内）  
・管理者 Slack への通知送信対応  
・利用設定：`SLACK_BOT_TOKEN`、`SLACK_SIGNING_SECRET`、`ADMIN_SLACK_USER_IDS`  
・プラン条件：Standard 以上（`SLACK_BOT` feature）

### LINE Messaging API
・LINE Webhook 受信エンドポイント：`/api/line/events`  
・テキストメッセージに対して自動返信  
・管理者 LINE への通知送信対応  
・利用設定：`LINE_CHANNEL_TOKEN`、`LINE_CHANNEL_SECRET`、`ADMIN_LINE_USER_IDS`  
・プラン条件：Standard 以上（`LINE_BOT` feature）  
・ユーザーへの紐づけ：`users.line_user_id`（管理画面で設定）

### Zoom オンライン診療
・Premium 会員のみ予約可能（`ZOOM_CONSULT` feature）  
・`/app/premium/online-care` から予約作成  
・予約時に Server-to-Server OAuth で Zoom 会議を自動作成し参加リンクを発行  
・利用設定：`ZOOM_ACCOUNT_ID`、`ZOOM_CLIENT_ID`、`ZOOM_CLIENT_SECRET`

### OpenAI（AI症状チェック）
・`/app/pets/{petId}/health-records` の症状チェックタブから実行  
・Standard 以上の会員が利用可能（`AI_SYMPTOM` feature）  
・未設定時はキーワードベースのフォールバック動作  
・利用設定：`OPENAI_API_KEY`、`OPENAI_MODEL`（デフォルト：`gpt-4.1-mini`）

### SendGrid（パスワード再設定メール）
・`/app/forgot-password` からトークン付きリセットメールを送信  
・SMTP 経由（host: smtp.sendgrid.net / port: 587）  
・利用設定：`SENDGRID_API_KEY`、`sendgrid.from-email`  
・未設定時は送信スキップ（ログのみ）

---

## ロールアクセスマトリクス

### ロール定義

| ロールコード | 説明 | users.role_id |
|---|---|:---:|
| ADMIN | システム管理者（ユーザー管理・運営管理） | 1 |
| USER | 一般飼い主ユーザー（プラン別機能利用） | 2 |
| VET | 獣医師（診療記録・予約管理・ユーザー閲覧） | 3 |
| STAFF | スタッフ（診療・予約・ユーザー管理・運営管理） | 4 |

### スタッフ系ロール別アクセス権限

| 機能 | URL | 管理者 | 獣医師 | スタッフ | 備考 |
|---|---|:---:|:---:|:---:|---|
| 診療記録 | `/app/consultations/**` | ✖ | ◎ | ◎ | ADMIN は閲覧不可 |
| 診療予約 管理 | `/app/appointments/**` | ✖ | ◎ | ◎ | 承認・却下は VET/STAFF のみ |
| カレンダー | `/app/calendar/**` | ✖ | ◎ | ◎ | ADMIN は閲覧不可 |
| お知らせ管理 | `/app/admin/announcements/**` | ◎ | ✖ | ◎ | VET は閲覧不可 |
| ユーザ管理 | `/app/admin/users/**` | ◎（フル） | △（一覧閲覧のみ） | ◎（作成・編集） | 削除は ADMIN のみ |
| 予約枠管理 | `/app/admin/appointment-slots/**` | ◎ | ✖ | ◎ | VET は閲覧不可 |
| サービス統計 | `/app/reports/**` | ◎ | ✖ | ✖ | ADMIN 専用 |
| 通知 | `/app/notifications/**` | ◎ | ◎ | ◎ | 全ロール共通 |

### 一般ユーザー（USER）の機能制限

| 機能 | Light | Standard | Premium |
|---|:---:|:---:|:---:|
| ペット・健康記録 | ◎ | ◎ | ◎ |
| カレンダー | ◎ | ◎ | ◎ |
| 診療予約 | ✖ | ◎ | ◎ |
| AI症状チェック | ✖ | ◎ | ◎ |
| Slackbot 連携 | ✖ | ◎ | ◎ |
| LINE 連携 | ✖ | ◎ | ◎ |
| Zoom オンライン診療 | ✖ | ✖ | ◎ |

### 実装クラス

- 権限判定：`LoginUser.canManageClinical()` / `canManageOperations()` / `hasStaffAccess()`
- プランゲート：`PlanAccessService.canUseAiSymptom()` / `canUseSlack()` / `canUseLine()` / `canUseZoom()`
- 統合ステータス：`PlanAccessService.resolveIntegrationStatus()` → `UserIntegrationStatus` DTO
- URL レベル制御：`SecurityConfig`（`hasAnyRole()` で path ごとに設定）

---

## 将来拡張

・ペット保険連携
・健康データ分析
・予防医療プログラム
・IoTデバイス連携
・地域動物病院ネットワーク

## サイトマップ
トップページ
├── 総合案内
│ ├── ペットライフプラスとは
│ ├── サービス詳細
│ ├── 料金説明
│ ├── 活用情報
│ └── ご利用までの案内
├── 導入事例と実績
├── ログイン / Webアプリ接続
├── お問い合わせ
└── 運営情報・規約

## Webアプリケーション構造図
ログイン前
├── ログイン
│ ├── メールアドレス/パスワード入力
│ ├── ログイン実行
│ └── パスワードを忘れた場合
├── パスワード再設定
│ ├── 再設定メール送信
│ └── 新パスワード登録
└── お知らせ/メンテナンス情報

ログイン後（一般ユーザー USER）
├── ダッシュボード
├── ペット管理（CRUD）
│ ├── ペット一覧・登録・編集・削除
│ ├── 健康記録（体重/食事/運動/スコア）
│ ├── ケア記録（ワクチン・フィラリア等）
│ └── AI症状チェック（Standard 以上）
├── カレンダー（ペットイベントシール）
├── 診療予約（Standard 以上）
│ ├── 予約一覧・新規作成
│ └── Zoom 診療（Premium のみ）
├── チャットボット相談（Standard 以上）
├── 動物病院案内
├── 通知・リマインダー
├── サブスクリプション確認
└── パスワード変更

ログイン後（獣医師 VET）
├── ダッシュボード
├── 診療・予約
│ ├── カレンダー
│ ├── 診療記録（CRUD）
│ └── 診療予約（一覧・承認・却下）
├── 利用者管理（閲覧のみ）
├── 通知
└── パスワード変更

ログイン後（スタッフ STAFF）
├── ダッシュボード
├── 診療・予約
│ ├── カレンダー
│ ├── 診療記録（CRUD）
│ └── 診療予約（一覧・承認・却下）
├── 利用者管理（ユーザー作成・編集）
├── 運営管理
│ ├── お知らせ管理
│ └── 予約枠管理
├── 通知
└── パスワード変更

ログイン後（管理者 ADMIN）
├── ダッシュボード
├── 利用者管理（ユーザー CRUD・ロール・プラン変更）
├── 運営管理
│ ├── お知らせ管理（作成・公開切替・削除）
│ └── 予約枠管理（手動枠追加・削除）
├── システム統計（サービスレポート）
├── 通知
└── パスワード変更


##Webサイト ページ一覧
- ページ名：トップページ
  主要コンテンツ：キャッチコピー、サービス概要、課題提起、主要機能導線、無料相談CTA

- ページ名：サービス紹介（f_service）
  主要コンテンツ：健康管理機能（体重・食事・運動記録）、AI症状チェック、オンライン診療、訪問ケアの説明

- ページ名：ご利用の流れ（f_flow）
  主要コンテンツ：登録手順、初期設定、利用開始フロー、サポート案内

- ページ名：運営情報（f_info）
  主要コンテンツ：運営情報、コンセプト、ミッション、利用規約、プライバシーポリシー

- ページ名：お問い合わせ（f_contact）
  主要コンテンツ：問い合わせフォーム、連絡先、返信目安

- ページ名：Webアプリ紹介（webapp）
  主要コンテンツ：機能概要、主要画面導線、ログイン導線


##Webアプリ 画面一覧
No.	画面名	画面の目的	主要機能
1	ログイン画面	利用者認証を行い安全にシステムへアクセスさせる	メール/パスワード入力、ログイン、パスワード再設定導線
2	ダッシュボード画面	全体状況をひと目で把握し次の操作へ誘導する	KPI表示、予約予定、通知一覧、未対応タスク表示
3	ユーザー管理画面	飼い主ユーザー情報を管理する	一覧表示、検索、詳細表示、新規登録、編集、削除（CRUD）
4	ペット管理画面	ペット情報を一元管理する	一覧表示、絞り込み検索、詳細表示、登録・編集・削除（CRUD）
5	健康記録管理画面	日々の健康データを管理・確認する	体重/食事/運動記録の登録、履歴表示、期間検索、編集・削除
6	症状チェック結果管理画面	AI症状チェック結果を確認し対応判断を支援する	結果一覧、重症度フィルタ、詳細確認、対応ステータス更新
7	予約管理画面	診療・相談予約を管理する	予約一覧、日付検索、新規予約、変更、キャンセル、リマインド設定
8	診療・相談履歴画面	診療/相談内容を記録し継続対応に活用する	履歴一覧、キーワード検索、詳細記録、添付、編集
9	通知配信管理画面	ユーザー向け通知を作成・配信する	通知作成、一斉配信、セグメント配信、配信履歴確認

##システムとして必要な機能（優先度）

- 機能名：ユーザー認証（ログイン/ログアウト）
  概要：メールアドレスとパスワードで利用者を認証し、安全に管理画面へアクセスさせる。
  優先度：Must

- 機能名：飼い主ユーザー管理（CRUD）
  概要：飼い主情報の登録・一覧・検索・編集・削除を行い、契約や履歴と紐づけて管理する。
  優先度：Must

- 機能名：ペット情報管理（CRUD）
  概要：ペットの基本情報（名前、種類、年齢、既往歴など）を登録・更新し、健康記録の基盤データとして利用する。
  優先度：Must

- 機能名：健康記録管理（CRUD）
  概要：体重・食事・運動などの日次データを登録・参照し、時系列で健康状態を可視化する。
  優先度：Must

- 機能名：AI症状チェック
  概要：入力された症状情報から受診目安や推奨アクションを提示し、初期判断を支援する。
  優先度：Must

- 機能名：予約管理（相談/診療）
  概要：オンライン相談・診療の予約作成、変更、キャンセルを行い、利用者と運営側の予定を管理する。
  優先度：Must

- 機能名：診療・相談履歴管理
  概要：相談内容、対応結果、次回アクションを履歴として保存し、継続的なケアに活用する。
  優先度：Should

- 機能名：通知・リマインド配信
  概要：予約前通知、記録入力促進、ワクチン時期案内などを配信し、継続利用を促進する。
  優先度：Should

- 機能名：レポート出力（PDF）
  概要：健康記録や運用実績を月次・顧客別に集計し、CSV/PDFで出力できるようにする。
  優先度：Could


##ユーザーフロー
ペルソナ：佐藤 美咲、32歳、ITベンチャー企業のマーケティング担当
目的：忙しい日常でも、愛犬の健康状態を一元管理し、必要時にオンライン相談・診療までスムーズに利用する

ステップ	行動	表示ページ	次のアクション
1	SNS/検索で「ペット 健康管理 アプリ」を調べる	記事LP・検索結果	公式サイトを開く
2	サービス概要と特徴を確認する	トップページ	サービス詳細へ進む
3	機能（健康記録・AI症状チェック・オンライン診療）を確認する	サービス紹介ページ	利用の流れを確認する
4	登録〜利用開始までの手順を確認する	ご利用の流れページ	運営情報を確認する
5	運営体制・利用方針を確認する	運営情報ページ	問い合わせ/相談へ進む
6	不明点を問い合わせる	お問い合わせページ	Webアプリ詳細を確認する
7	新規登録を行う（アカウント作成）	申込み・登録ページ	初期設定を開始する
8	ペット情報を登録し、体重/食事/運動の記録を開始する	初期設定・ペット情報登録ページ	日常利用を継続する
9	体調が気になる日にAI症状チェックを実施する	症状チェックページ	必要ならオンライン相談を予約する
10	オンライン相談/診療を受け、履歴を確認する

##サブシナリオ
ステップ	行動	表示ページ	次のアクション
1	夜間に愛犬の軽い不調（食欲低下）に気づく	-	すぐに公式サイトへアクセス
2	緊急時対応の可否を確認する	トップページ	医療サポート詳細へ進む
3	AI症状チェックとオンライン相談の流れを確認する	サービス紹介ページ（医療サポート）	利用の流れを確認する
4	利用開始手順と必要情報を確認する	ご利用の流れページ	不安点を問い合わせる
5	夜間相談可否や受診目安を問い合わせる	お問い合わせページ	相談申込みへ進む
6	最短でアカウント登録しペット情報を入力する	申込み・登録ページ	症状チェックを開始する
7	症状を入力して受診推奨度を確認する	AI症状チェックページ	オンライン相談予約へ進む
8	直近枠でオンライン相談を予約する	予約ページ	問診情報を事前送信する
9	オンライン相談で対応方針（自宅経過観察/受診）を確認する	オンライン相談画面	必要時は訪問ケア/病院受診を手配する
10	相談内容と今後のケアを履歴で確認し、通知設定を有効化する	診療履歴ページ・通知設定ページ	継続的な健康記録運用へ移行する

## URL設計（実装済み）

### 認証・パスワード（公開）

| URL | メソッド | 画面/機能 |
|---|---|---|
| `/app/login` | GET / POST | ログイン |
| `/app/logout` | POST | ログアウト |
| `/app/forgot-password` | GET / POST | パスワード再設定メール送信 |
| `/app/forgot-password/sent` | GET | 送信完了案内 |
| `/app/reset-password` | GET / POST | 新パスワード設定（トークン付き） |

### 一般ユーザー（要ログイン）

| URL | メソッド | 画面/機能 | ロール制限 |
|---|---|---|---|
| `/app/dashboard` | GET | ダッシュボード | 全ロール |
| `/app/pets` | GET / POST | ペット一覧・登録 | 全ロール |
| `/app/pets/{id}` | GET / PATCH / DELETE | ペット詳細・編集・削除 | 全ロール |
| `/app/pets/{id}/health-records` | GET / POST | 健康記録 | 全ロール |
| `/app/pets/{id}/health-records/{rid}` | PATCH / DELETE | 健康記録編集・削除 | 全ロール |
| `/app/calendar` | GET | ペットカレンダー | USER / VET / STAFF |
| `/app/calendar/marks/add` | POST | カレンダーシール追加 | USER / VET / STAFF |
| `/app/calendar/marks/{id}/delete` | POST | シール削除 | USER / VET / STAFF |
| `/app/appointments` | GET / POST | 診療予約一覧・新規作成 | USER / VET / STAFF |
| `/app/appointments/{id}/approve` | POST | 予約承認 | VET / STAFF |
| `/app/appointments/{id}/reject` | POST | 予約却下 | VET / STAFF |
| `/app/appointments/{id}/cancel` | POST | 予約キャンセル | USER |
| `/app/appointments/delete-selected` | POST | 選択削除 | USER / VET / STAFF |
| `/app/consult/chatbot` | GET / POST | チャットボット相談 | Standard 以上 |
| `/app/clinic-guide` | GET | 動物病院案内 | 全ロール |
| `/app/notifications` | GET | 通知・リマインダー一覧 | 全ロール |
| `/app/notifications/{id}/read` | POST | 既読 | 全ロール |
| `/app/notifications/read-all` | POST | 全既読 | 全ロール |
| `/app/notifications/reminders/dismiss` | POST | リマインダー非表示 | 全ロール |
| `/app/subscriptions` | GET | サブスクリプション確認 | 全ロール（ADMIN は全件） |
| `/app/password-resets` | GET / POST | パスワード変更（要現在PW） | 全ロール |
| `/app/premium/online-care` | GET / POST | Zoom オンライン診療予約 | Premium のみ |

### 診療・予約管理（VET / STAFF）

| URL | メソッド | 画面/機能 |
|---|---|---|
| `/app/consultations` | GET / POST | 診療記録一覧・登録 |
| `/app/consultations/{id}/edit` | GET | 診療記録編集フォーム |
| `/app/consultations/{id}` | PATCH / DELETE | 診療記録更新・削除 |

### 管理者・スタッフ（`/app/admin/**`）

| URL | メソッド | アクセス可能ロール | 画面/機能 |
|---|---|---|---|
| `/app/admin/users` | GET | ADMIN / VET / STAFF | ユーザー一覧 |
| `/app/admin/users/new` | GET / POST | ADMIN / STAFF | ユーザー新規登録 |
| `/app/admin/users/{id}/edit` | GET / PATCH | ADMIN / STAFF | ユーザー編集 |
| `/app/admin/users/{id}` | DELETE | ADMIN | ユーザー削除 |
| `/app/admin/announcements` | GET / POST | ADMIN / STAFF | お知らせ管理 |
| `/app/admin/announcements/{id}/toggle` | POST | ADMIN / STAFF | 公開・非公開切替 |
| `/app/admin/announcements/{id}/delete` | POST | ADMIN / STAFF | お知らせ削除 |
| `/app/admin/appointment-slots` | GET / POST | ADMIN / STAFF | 予約枠管理 |
| `/app/admin/appointment-slots/{id}/delete` | POST | ADMIN / STAFF | 予約枠削除 |

### ADMIN 専用

| URL | メソッド | 画面/機能 |
|---|---|---|
| `/app/reports` | GET | サービス統計（ユーザー数・ペット数・予約数・サブスク数） |

### 外部 API

| URL | メソッド | 説明 |
|---|---|---|
| `/api/slack/events` | POST | Slack Events API Webhook 受信 |
| `/api/line/events` | POST | LINE Messaging API Webhook 受信 |
| `/api/appointments` | REST | 予約 REST API（内部用） |

## Controller 一覧（実装済み）

| クラス | マッピング | 主な機能 |
|---|---|---|
| `AuthController` | `/app/login` | ログインフォーム表示（認証処理は Spring Security） |
| `ForgotPasswordController` | `/app/forgot-password`, `/app/reset-password` | パスワード再設定メール送信・新PW設定 |
| `PasswordResetController` | `/app/password-resets` | ログイン中ユーザーのパスワード変更 |
| `DashboardController` | `/app/dashboard` | KPI・通知・予定表示 |
| `PetController` | `/app/pets` | ペット CRUD・画像アップロード |
| `HealthRecordController` | `/app/pets/{id}/health-records` | 健康記録 CRUD・AI症状チェック |
| `CalendarController` | `/app/calendar` | ペットカレンダー・マーク追加削除 |
| `AppointmentPageController` | `/app/appointments` | 診療予約 CRUD・承認（VET/STAFF）・却下 |
| `AppointmentController` | `/api/appointments` | 予約 REST API |
| `ConsultationController` | `/app/consultations` | 診療記録 CRUD（VET/STAFF のみ） |
| `ConsultChatController` | `/app/consult/chatbot` | チャットボット相談（Standard 以上） |
| `PremiumSupportController` | `/app/premium/online-care` | Zoom オンライン診療（Premium のみ） |
| `ClinicGuideController` | `/app/clinic-guide` | 動物病院案内 |
| `NotificationController` | `/app/notifications` | 通知・リマインダー閲覧・既読・非表示 |
| `SubscriptionController` | `/app/subscriptions` | サブスクリプション確認 |
| `UserController` | `/app/admin/users` | ユーザー管理（ADMIN フル / STAFF 作成・編集 / VET 閲覧） |
| `AnnouncementController` | `/app/admin/announcements` | お知らせ管理（ADMIN / STAFF） |
| `AppointmentSlotController` | `/app/admin/appointment-slots` | 予約枠管理（ADMIN / STAFF） |
| `ReportController` | `/app/reports` | サービス統計（ADMIN のみ） |
| `SlackEventController` | `/api/slack/events` | Slack Webhook 受信 |
| `LineEventController` | `/api/line/events` | LINE Webhook 受信 |
| `RootController` | `/` | フロントエンドへリダイレクト |

## ワイヤーフレーム（トップページ）

### PCワイヤー（12カラム基準）

- コンテナ幅：min(1200px, 92vw)
- 基本グリッド：12カラム
- セクション間余白：64px
- 構成：2カラム中心（情報 + ビジュアル）

1. ヘッダー（高さ72px）
   - 左3/12：ロゴ
   - 中央6/12：ナビ（サービス紹介/ご利用の流れ/運営情報/お問い合わせ）
   - 右3/12：ログイン + 主CTA
2. ヒーロー（高さ560px）
   - 左6/12：見出し、説明、CTA2種
   - 右6/12：キービジュアル（アプリ画面 + ペット写真）
3. サービス紹介（高さ520px）
   - 上段12/12：セクション見出し
   - 下段：4/12 × 3カード（健康記録 / AI症状チェック / オンライン診療・訪問ケア）
4. 実績（高さ420px）
   - 左7/12：事例カード2件（縦積み）
   - 右5/12：数値実績（KPIブロック）
5. CTA（高さ300px）
   - 中央寄せ8/12：訴求文 + 主CTA + 副CTA
6. フッター（高さ220px）
   - 3/12 × 4列（サービス / 会社情報 / 法務 / サポート）

### 配置意図（PC）

- ヘッダー
  - なぜその位置か：最上部固定で主要導線へ常時アクセスできるようにするため。
  - ユーザー効果：迷子を防ぎ、相談/ログインへの遷移を促進。
- ヒーロー
  - なぜその位置か：ファーストビューで価値訴求とCTAを同時提示するため。
  - ユーザー効果：自分向けサービスかを短時間で判断しやすい。
- サービス紹介
  - なぜその位置か：興味獲得直後に機能を具体化するため。
  - ユーザー効果：理解コストを下げ、比較検討しやすくなる。
- 実績
  - なぜその位置か：機能理解後に信頼材料を提示するため。
  - ユーザー効果：導入不安の軽減と意思決定の後押し。
- CTA
  - なぜその位置か：納得感が高まるタイミングで行動喚起するため。
  - ユーザー効果：検討から申込みへの遷移率向上。
- フッター
  - なぜその位置か：末尾で法務情報と補助導線を集約するため。
  - ユーザー効果：信頼確認がしやすく、安心して申込み判断できる。

### スマートフォンワイヤー（1カラム基準）

- コンテナ幅：92vw
- 基本グリッド：1カラム
- セクション間余白：32px
- 構成：完全1カラム（縦スクロール最適化）

1. ヘッダー（高さ56px）
   - 左：ロゴ
   - 右：ハンバーガー + 小CTA
2. ヒーロー（高さ520px）
   - 上：見出し・説明
   - 中：CTA縦並び
   - 下：ビジュアル
3. サービス紹介
   - 機能カード3枚を縦積み
4. 実績
   - KPI（2列→小画面は1列）+ ユーザー声カード縦積み
5. CTA（高さ260px）
   - 主CTA・副CTAを幅100%で縦並び
6. フッター
   - アコーディオン式リンク群 + 法務リンク

### レスポンシブ切替ルール

1. 768px以下で2カラム→1カラム
2. CTA横並び→縦並び
3. カード3列→1列


### backup/restore
バックアップファイル: backups/yyyymmdd_backup.sql

追加した機能:

scripts/db_backup.sh
実行で backups/yyyyMMdd_backup.sql を作成
scripts/db_restore.sh <backup_sql_file>

指定SQLからリストア
実行例:
./scripts/db_backup.sh
./scripts/db_restore.sh backups/20260518_backup.sql

---

## 環境構築・起動手順

### 前提条件
- PostgreSQL 17（`C:\Program Files\PostgreSQL\17`）が起動していること
- Java 21 以上がインストールされていること（プロジェクト指定: Java 21）

### 1. データベース準備

#### 初回または復元時
```powershell
# DB を削除して再作成（既存接続を強制切断）
$env:PGPASSWORD = "hs0512"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "DROP DATABASE IF EXISTS petlifeplus WITH (FORCE);"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE petlifeplus OWNER postgres ENCODING 'UTF8';"

# backup.sql から復元
Get-Content "c:\Projects\petlifeplus_site\backup.sql" | & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -d petlifeplus
```

### 2. 環境変数（APIキー）の設定

`backend/src/main/resources/application-local.properties` を作成（Git 管理外）：

```properties
# Zoom（Server-to-Server OAuth）
zoom.account-id=（Account ID）
zoom.client-id=（Client ID）
zoom.client-secret=（Client Secret）

# Slack
slack.bot-token=xoxb-...
slack.signing-secret=...
admin.slack-user-ids=U01234567,U09876543

# LINE Messaging API
line.channel-token=...
line.channel-secret=...
admin.line-user-ids=Uxxxxxxxxxxxxxxxxxx

# OpenAI（未設定でもキーワードベースフォールバック動作）
openai.api-key=sk-...
# openai.model=gpt-4.1-mini  # デフォルト

# SendGrid（パスワード再設定メール）
sendgrid.api-key=SG....
sendgrid.from-email=noreply@petlifeplus.com
app.base-url=http://localhost:8080
```

### 3. アプリ起動

```powershell
cd c:\Projects\petlifeplus_site\backend
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

- `local` プロファイルを付けると `application-local.properties` が読み込まれ外部APIキーが有効になる
- 付けない場合は Zoom/Slack/OpenAI がフォールバック動作
- `spring.sql.init.mode=never` のため、起動時に schema.sql / data.sql は自動適用されない

### 4. アクセス

| URL | 説明 |
|---|---|
| http://localhost:8080 | トップ（フロントエンドへリダイレクト） |
| http://localhost:8080/app/login | ログイン画面 |
| http://localhost:8080/app/dashboard | ダッシュボード（要ログイン） |

### デフォルトログイン情報

| メールアドレス | パスワード | ロール | プラン |
|---|---|---|---|
| admin@petlifeplus.local | admin123 | ADMIN | — |
| vet1@petlifeplus.local | vet123 | VET | — |
| staff1@petlifeplus.local | staff123 | STAFF | — |
| owner1@petlifeplus.local | user123 | USER | Standard |
| owner2@petlifeplus.local | user123 | USER | Standard |
| owner.light@petlifeplus.local | light123 | USER | Light |
| owner.standard@petlifeplus.local | standard123 | USER | Standard |
| owner.premium@petlifeplus.local | premium123 | USER | Premium |

### 外部サービス連携状況

| サービス | 状態 | 備考 |
|---|---|---|
| Zoom | application-local.properties に要設定 | Server-to-Server OAuth |
| Slack | application-local.properties に要設定 | Bot Token / Signing Secret |
| LINE | application-local.properties に要設定 | Channel Token / Channel Secret |
| OpenAI | 未設定でも動作（フォールバック） | API キー設定で精度向上 |
| SendGrid | 未設定でも動作（メール送信スキップ） | パスワード再設定メール用 |

---

## 実装状況

### 実装済み（Must）

| 機能 | 実装クラス |
|---|---|
| ログイン・ログアウト（Spring Security） | `SecurityConfig`、`UserDetailsServiceImpl` |
| パスワード変更（要現在PW） | `PasswordResetController` |
| パスワード再設定（メール送信 / トークン） | `ForgotPasswordController`、`PasswordResetService` |
| ユーザー管理 CRUD | `UserController`、`UserService`、`UserMapper` |
| ロールアクセス制御 | `SecurityConfig`、`LoginUser`、各 Controller |
| ペット管理 CRUD・画像アップロード | `PetController`、`PetService` |
| 健康記録管理 CRUD | `HealthRecordController` |
| ワクチン・ケア記録管理 | `PetController`（pet_care_records） |
| AI症状チェック（OpenAI / フォールバック） | `SymptomCheckService` |
| 診療予約 CRUD・承認・却下 | `AppointmentPageController`、`AppointmentService` |
| 診療記録 CRUD（VET / STAFF） | `ConsultationController` |
| ペットカレンダー | `CalendarController`、`CalendarService` |
| 通知・リマインダー | `NotificationController` |
| サブスクリプション確認 | `SubscriptionController` |
| チャットボット相談 | `ConsultChatController` |
| Zoom オンライン診療（Premium） | `PremiumSupportController`、`ZoomLinkService` |
| Slack Webhook 受信・返信 | `SlackEventController`、`SlackBotService` |
| LINE Webhook 受信・返信 | `LineEventController` |
| お知らせ管理（ADMIN / STAFF） | `AnnouncementController` |
| 予約枠管理（ADMIN / STAFF） | `AppointmentSlotController` |
| サービス統計（ADMIN） | `ReportController` |
| プラン別機能ゲート | `PlanAccessService`、`plan_features` テーブル |
| ユーザー統合ステータス（Slack/LINE/Zoom） | `UserIntegrationStatus`、`PlanFeatureMapper` |

### スキーマのみ・未実装

| 機能 | テーブル | 備考 |
|---|---|---|
| 請求書 | `invoices` | Mapper のみ、UI なし |
| 支払 | `payments` | Mapper のみ、UI なし |
| メールテンプレート | `email_templates`、`email_messages` | Mapper のみ、UI なし |
| 医療添付ファイル | `medical_attachments` | Mapper のみ、UI なし |
| レポート CSV/PDF 出力 | — | 統計画面のみ実装 |

### データベース構成（23 テーブル）

| ドメイン | テーブル |
|---|---|
| 認証 | `users`、`roles`、`password_reset_tokens` |
| ペット・健康 | `pets`、`health_records`、`pet_care_records`、`symptom_checks`、`medical_histories`、`medical_attachments` |
| プラン・請求 | `plans`、`plan_features`、`subscriptions`、`invoices`、`payments` |
| 予約 | `appointments`、`appointment_slots` |
| メッセージ | `notifications`、`notification_recipients`、`email_templates`、`email_messages`、`consult_chat_messages` |
| UX | `pet_calendar_marks`、`dismissed_reminders`、`announcements` |