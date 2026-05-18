# 本番運用チェックリスト（Pet Life Plus）

## 1. セキュリティ
- `OPENAI_API_KEY` `SLACK_BOT_TOKEN` `SLACK_SIGNING_SECRET` `ZOOM_*` を環境変数管理し、`application.properties` に平文固定しない
- 本番DBパスワードを `hs0512` から変更済み
- HTTPS終端（TLS1.2+）とHSTS有効化
- Slack署名検証が有効で、無効署名で `401` になることを確認
- CSRF/CORS設定が要件通り（管理画面は同一オリジン前提）
- 管理者アカウントの初期パスワード変更強制
- 監査ログ（`AUDIT`）にユーザー作成/更新/削除・プラン変更が出ることを確認
- 不要ポート/管理エンドポイント未公開（FW/SGで制限）
- 依存ライブラリ脆弱性スキャン（`mvn dependency-check` 等）を定期実施

## 2. 認証・認可
- 月額契約有効期限ロジックで `ROLE_USER` の `isEnabled` が正しく切替
- Light/Standard/Premium の機能制御を再確認
- Premium以外で Zoom 優先サポートURLへ直接アクセスした場合に拒否されること
- 退会/停止ユーザーでログイン不可を確認

## 3. 外部連携
- OpenAI: 実在モデル名（現行 `gpt-4.1-mini`）で疎通確認
- Slack: Event Subscriptions の Request URL検証とメッセージ返信確認
- Zoom: OAuthトークン取得→会議作成→`join_url` 取得の実運用テスト
- Zoom API失敗時、画面にフォールバック理由が表示されることを確認

## 4. 監視・ログ運用
- `logback-spring.xml` のローテーションが想定どおり動作
  - `logs/app.log` `logs/audit.log` `logs/ops.log`
- ログ保存先ディスク容量監視（閾値アラート）
- 主要アラート設定
  - 5xx急増
  - Slack署名不正急増
  - Zoom API失敗率上昇
  - DB接続エラー
- ログの時刻同期（NTP）とタイムゾーン統一（運用基準）

## 5. DB・データ保護
- 日次バックアップジョブ（`scripts/db_backup.sh`）をcron/systemdで運用
- バックアップ保存先を本体サーバ外へ複製（オブジェクトストレージ等）
- バックアップファイル暗号化・アクセス制御
- スキーマ変更時のマイグレーション手順を標準化
- 重要テーブル（users/subscriptions/appointments/health_records）の件数・整合性監視

## 6. バックアップ復元訓練（必須）
- 月次で復元訓練を実施し記録
- 手順:
  - バックアップ取得: `./scripts/db_backup.sh`
  - 検証環境へ復元: `./scripts/db_restore.sh backups/YYYYMMDD_backup.sql`
  - アプリ起動・ログイン・主要機能（AI症状/Slack/Zoom/健康記録）を確認
- RTO/RPOを測定し、目標内か評価
- 復元訓練結果を運用台帳に残す（日時/担当/所要時間/課題）

## 7. リリース・品質
- 本番デプロイ前に `mvn test`（少なくとも追加済み重要テスト）実行
- ステージングでE2E確認
  - ログイン
  - プラン別導線
  - 健康記録（5段階評価・画像）
  - AI症状チェック
  - Slack返信
  - Premium Zoom予約
- ロールバック手順（アプリ/DB）を事前確認
- リリース後30分の重点監視（エラー率・応答時間・外部API失敗率）

## 8. 運用ドキュメント
- 障害一次切り分けRunbook（OpenAI/Slack/Zoom/DB）
- 秘密情報ローテーション手順
- 管理者操作ポリシー（監査ログレビュー頻度含む）
- 問い合わせ対応テンプレート（Zoomフォールバック通知時の案内文）
