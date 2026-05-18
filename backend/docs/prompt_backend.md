あなたはシニアバックエンドエンジニアです。以下の参照資料を根拠に、Java / Spring Boot で実装を行ってください。

【参照資料】
- 仕様書: `docs/requirements.md`
- DB設計書: `docs/db-design.md`

【目的】
Pet Life Plus の業務要件（ユーザー管理、ペット管理、健康記録、予約、診療履歴、請求決済、通知など）に対応するバックエンドAPIを、Spring Boot + MyBatis で実装する。

【技術要件】
1. フレームワーク/言語
- Java 21
- Spring Boot 4.x
- MyBatis
- PostgreSQL

2. API設計
- RESTful API で設計する
- 命名規則:
  - `GET /api/users`
  - `GET /api/users/{id}`
  - `POST /api/users`
  - `PUT /api/users/{id}`
  - `DELETE /api/users/{id}`（論理削除）
- 同様に `pets`, `health-records`, `appointments`, `medical-histories`, `invoices`, `payments`, `notifications` など主要リソースを実装

3. アーキテクチャ
- レイヤード構成
  - controller
  - service
  - repository (mybatis mapper)
  - domain/entity
  - dto (request/response)
  - config
  - exception
- OpenAPI（Swagger）を有効化し、API仕様を自動生成

4. バリデーション
- Bean Validation（jakarta validation）を使用
- 例:
  - `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@Pattern`
- リクエストDTOに付与し、Controllerで `@Valid` を適用

5. エラーハンドリング
- `@RestControllerAdvice` によるグローバル例外ハンドリング
- エラーレスポンスは統一形式にする:
  - `timestamp`
  - `status`
  - `errorCode`
  - `message`
  - `path`
- バリデーションエラー時はフィールド別エラー配列も返却

6. セキュリティ（Spring Security）
- Spring Security を導入
- 認証: フォームログインまたはJWT（どちらかを採用し理由を明記）
- 認可: ロールベース（ADMIN / USER / VET / STAFF）
- パスワードは BCrypt でハッシュ化
- CSRF, XSS, SQLインジェクション対策を考慮
- 公開APIと認証必須APIを明確に分離

7. DB連携（MyBatis）
- `Mapper` インターフェース + XML（またはアノテーション）でSQL管理
- N+1回避、ページング対応（検索・ソート・ページネーション）
- 論理削除カラム `deleted_at` があるテーブルは検索時に除外

8. テスト（JUnit）
- JUnit 5 + Spring Boot Test
- 単体テスト（Service層）
- APIテスト（Controller層: MockMvc）
- 正常系/異常系/境界値を含める
- カバレッジレポート（JaCoCo）を設定

【実装対象（最低限）】
- 優先順位: Must機能（F-001〜F-008）を先行し、Should/Could機能は段階的に実装する
- 認証API（ログイン、ログアウト、ユーザー作成）
- ユーザー管理CRUD
- ペット管理CRUD
- 健康記録CRUD
- 予約CRUD
- 診療履歴CRUD + 添付メタ情報登録
- 請求・決済参照/更新API
- 通知一覧/登録API

【成果物】
1. 実装コード一式
2. `README.md`（セットアップ手順、起動方法、主要API一覧、テスト実行方法）
3. DBマイグレーション（`schema.sql`, `data.sql` または Flyway）
4. API定義（Swagger UIで確認可能）
5. テストコード

【Git運用要件】
1. GitHubに新規リポジトリを作成（例: `petlifeplus-backend`）
2. ローカルから初回コミット・Push
3. ブランチ戦略:
- `main`: 安定版
- `develop`: 開発統合
- `feature/*`: 機能開発
4. Pull Requestテンプレートを用意（変更概要、確認観点、テスト結果）

【出力ルール】
- まず全体アーキテクチャとパッケージ構成を提示
- 次に主要エンドポイント一覧を提示
- その後、実装コードをファイル単位で出力
- 最後にテストコードとGitHub Push手順を提示

【注意】
- 仕様書・DB設計書にない項目を追加する場合は「追加理由」を明記する
- 命名規則は一貫させる（camelCase / snake_case の使い分け）
- 可読性と保守性を優先し、過度に複雑な実装は避ける
