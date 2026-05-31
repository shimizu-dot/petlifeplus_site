# AI 機能 技術・インターフェイス整理

## 概要：2つの独立した機能

| | AI 症状チェック | チャットボット相談 |
|---|---|---|
| **目的** | 症状を一度入力 → 重症度判定＋アドバイスを即時返却 | マルチターン会話で情報収集 → 受診判断をサポート |
| **ペット紐付け** | あり（`pet_id`） | なし（`user_id` のみ） |
| **会話モデル** | 1 ターン完結 | 複数ターン（最大 50 件履歴） |
| **API 設定** | `openai.*`（gpt-4.1-mini） | `chatbot.*`（llama-3.1-8b-instant / Groq、OpenAI互換API差し替え可） |
| **temperature** | 0.2（判定の安定性優先） | 0.7（自然な会話優先） |
| **プランゲート** | `canUseAiSymptom()` — STANDARD 以上 | なし（全プラン利用可） |

> **注意:** 2つの機能はコードを共有していない。API 設定キーも `openai.*` と `chatbot.*` で独立しており、Service / Mapper / DTO / テンプレートもすべて独立。

---

## 1. AI 症状チェック（Symptom Check）

### 役割

ペット詳細画面に組み込まれた診断フォーム。「症状の種類・発症時期・メモ」を送信すると、AI または
ヒューリスティックが **severity（重症度）** と **recommendation（推奨対応）** を判定し、
結果を `symptom_checks` テーブルに保存してペット詳細ページへリダイレクトする。

### ファイル構成

| 役割 | クラス / ファイル |
|---|---|
| Controller | `PetController` — `GET/POST /app/pets/{id}` および `POST /app/pets/{id}/symptom-check` |
| Service | `SymptomCheckService` |
| Mapper | `SymptomCheckMapper` |
| DTO | `SymptomCheckForm` |
| Entity | `SymptomCheckEntity` |
| テンプレート | `templates/pets/detail.html`（ペット詳細ページ内フォーム） |

### 入力インターフェイス

```
POST /app/pets/{petId}/symptom-check
Content-Type: application/x-www-form-urlencoded

フォームクラス: SymptomCheckForm
  symptomType  String @NotBlank @Size(max=100)  例: "食欲不振"
  onsetText    String @Size(max=100)             例: "昨夜から"（任意）
  memo         String @Size(max=500)             例: "嘔吐1回"（任意）
```

### 出力インターフェイス

```
成功時: redirect:/app/pets/{petId}
  FlashAttribute: symptomAdvice  = ガイダンステキスト（guidance フィールド）
  FlashAttribute: success        = "AI症状チェックを実行しました"

ページに表示する変数（GET /app/pets/{id}）:
  symptomChecks  List<SymptomCheckEntity>   直近の検査履歴
  symptomForm    SymptomCheckForm           フォームバインディング用
  canUseAiSymptom boolean                  プラン判定結果（UI 制御用）
```

### DB テーブル

```sql
symptom_checks
  id                   BIGINT PK
  pet_id               BIGINT  NOT NULL  -- どのペットの症状か
  requested_by_user_id BIGINT  NOT NULL
  symptom_type         VARCHAR(100)
  onset_text           VARCHAR(100)
  memo                 VARCHAR(500)
  severity             VARCHAR(10)  CHECK IN ('LOW','MEDIUM','HIGH')
  recommendation       VARCHAR(10)  CHECK IN ('OBSERVE','CONSULT','VISIT')
  guidance             TEXT         -- 日本語アドバイス全文
  ai_model             VARCHAR(100) -- 使用モデル名（"free-local" ならフォールバック）
  created_at           TIMESTAMP
  deleted_at           TIMESTAMP    -- 論理削除
```

### AI 呼び出し詳細

```
エンドポイント : ${openai.base-url}/chat/completions
モデル        : ${openai.model}  (デフォルト: gpt-4.1-mini)
temperature   : 0.2
出力形式      : JSON  { "severity": "HIGH|MEDIUM|LOW", "recommendation": "VISIT|CONSULT|OBSERVE", "guidance": "..." }
```

### フォールバック階層

| 条件 | 処理 | aiModel 記録値 |
|---|---|---|
| API キー設定あり | OpenAI API 呼び出し → JSON パース | モデル名（例: gpt-4.1-mini） |
| API キーなし or API エラー | `heuristic()` — キーワード 3 分類 | モデル名 or `"local-heuristic"` |
| プラン外ユーザー | `heuristic()` を強制実行 | `"free-local"` |

**ヒューリスティック分類キーワード（`heuristic()`）**

| severity / recommendation | キーワード |
|---|---|
| HIGH / VISIT | 血、呼吸、痙攣、ぐったり |
| MEDIUM / CONSULT | 吐、下痢、食欲、咳 |
| LOW / OBSERVE | その他 |

---

## 2. チャットボット相談（Consult Chatbot）

### 役割

独立したチャット画面。ユーザーがメッセージを送るたびに履歴を DB に蓄積し、
AI（または多段フォールバック）が会話履歴を踏まえた返答を生成する。
症状情報を段階的に収集し、最終的に受診判断を提示する。

### ファイル構成

| 役割 | クラス / ファイル |
|---|---|
| Controller | `ConsultChatController` — `GET/POST /app/consult/chatbot` |
| Service | `ConsultChatService` |
| Mapper | `ConsultChatMapper` |
| DTO | `ConsultChatForm` |
| Entity | `ConsultChatMessageEntity` |
| テンプレート | `templates/consult/chatbot.html` |

### 入力インターフェイス

```
POST /app/consult/chatbot
Content-Type: application/x-www-form-urlencoded

フォームクラス: ConsultChatForm
  message  String @NotBlank @Size(max=1000)  例: "昨日から食欲がなく、朝に少し吐きました"
```

### 出力インターフェイス

```
成功時: redirect:/app/consult/chatbot

ページに表示する変数（GET /app/consult/chatbot）:
  messages      List<ConsultChatMessageEntity>  直近 50 件（USER + BOT）
  flowProgress  List<FlowStepProgress>          情報収集の進捗（段階ラベル + 完了フラグ）
  quickPrompts  List<String>                    クイック入力ボタン用テキスト 4 件
  form          ConsultChatForm                 フォームバインディング用
```

**flowProgress の段階定義**

| ステップ | 検出キーワードセット |
|---|---|
| 1. 症状の種類 | `SYMPTOM_WORDS`（吐、下痢、咳、食欲、など） |
| 2. いつから | `TIMING_WORDS`（から、昨日、今日、時間、日前、など） |
| 3. 頻度 | `FREQUENCY_WORDS`（回、毎、たまに、続い、など） |
| 4. 食欲・元気・便/尿の変化 | `CONDITION_WORDS`（食欲、元気、水、飲、便、尿、など） |

### DB テーブル

```sql
consult_chat_messages
  id          BIGINT PK
  user_id     BIGINT  NOT NULL  -- ユーザー単位で管理（pet_id なし）
  sender_type VARCHAR(10)  CHECK IN ('USER','BOT')
  message     VARCHAR(1000)
  created_at  TIMESTAMP
```

> ペット情報は保存しない。チャット内容はユーザーが入力したテキストのみ。

### AI 呼び出し詳細

```
エンドポイント : ${chatbot.base-url}/chat/completions  (デフォルト: https://api.groq.com/openai/v1)
モデル        : ${chatbot.model}  (デフォルト: llama-3.1-8b-instant)
temperature   : 0.7
max_tokens    : 400
入力          : systemPrompt + 直近 50 件の会話履歴 + 今回のユーザーメッセージ
出力形式      : 自然言語テキスト（JSON 解析なし）
```

> OpenAI 互換の API エンドポイントであれば `CHATBOT_BASE_URL` / `CHATBOT_MODEL` で任意に差し替え可能。

**システムプロンプトの制約**
- 診断名を断言しない
- 処方・投薬を指示しない
- 緊急ワード検出時は即時受診を最優先
- 食事 2 日以上取れない場合は「今すぐ受診」を強く推奨

### フォールバック階層（`fallbackReply()`）

| 優先順位 | 条件 | 返答内容 |
|---|---|---|
| 1 | `EMERGENCY_WORDS` を検出 | ⚠️ 緊急サイン → 今すぐ受診 |
| 2 | `APPETITE_LOSS_WORDS` ∩ `TWO_OR_MORE_DAYS_WORDS` 両方を検出 | ⚠️ 脱水リスク → 今すぐ受診 |
| 3 | `SYMPTOM_WORDS` が未収集 | 「どのような症状ですか？」 |
| 4 | `TIMING_WORDS` が未収集 | 「いつ頃から始まりましたか？」 |
| 5 | `FREQUENCY_WORDS` が未収集 | 「どのくらいの頻度ですか？」 |
| 6 | `CONDITION_WORDS` が未収集 | 「食欲・元気・便の様子は？」 |
| 7 | 全情報収集済み | risk スコア計算 → 受診推奨 or 経過観察 |

---

## 3. 設定プロパティ

### AI症状チェック（F-009）

```properties
# application.properties — SymptomCheckService が使用
openai.api-key  = ${OPENAI_API_KEY:}
openai.model    = ${OPENAI_MODEL:gpt-4.1-mini}
openai.base-url = ${OPENAI_BASE_URL:https://api.openai.com/v1}
```

### チャットボット相談（F-022）

```properties
# application.properties — ConsultChatService が使用
chatbot.api-key  = ${CHATBOT_API_KEY:}
chatbot.model    = ${CHATBOT_MODEL:llama-3.1-8b-instant}
chatbot.base-url = ${CHATBOT_BASE_URL:https://api.groq.com/openai/v1}
```

> どちらの API キーも未設定のまま起動・動作する。呼び出し失敗時は各機能が独自フォールバックで応答する。

### プランアクセス制御

**AI症状チェック（F-009）のみ** プランゲートあり:

```
PlanAccessService.canUseAiSymptom(LoginUser user)
  → ADMIN / VET / STAFF  : 常に true
  → 一般ユーザー          : subscriptions × plan_features テーブルで AI_SYMPTOM 機能コードを確認
                           STANDARD 以上で true
```

**チャットボット相談（F-022）** はプランゲートなし（全プラン・全ユーザー利用可）。
