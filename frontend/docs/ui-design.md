# UI Design Guide

参照元: `docs/06-design-guide.html`

## 1. デザインコンセプト

### キーワード
1. **信頼感**
- 理由: 健康管理・診療導線を扱うため、安心して情報入力・相談できる印象を最優先する必要がある。
- 表現方法: 青・白を基調にした配色、十分な余白、見出し階層の明確化、実績や監修情報を上部に配置する。

2. **伴走感**
- 理由: 忙しい飼い主が「ひとりで判断しなくてよい」と感じる体験が継続利用につながるため。
- 表現方法: ミント系の補助色、ガイド文・次アクションの明示、ステップ表示で進行状況を可視化する。

3. **俊敏性**
- 理由: 体調不安時は素早い判断・予約が必要で、迷うUIは離脱や不安増大につながるため。
- 表現方法: 主CTAを目立たせる、1画面1目的の構成、入力項目の最小化、検索/予約導線を固定配置する。

---

## 2. カラーパレット

> **注意:** このカラーパレットはフロントエンド（マーケティングサイト）用です。バックエンドアプリ（`app.css`）は別の青系スキームを使用します。

| 色の種類 | HEX | CSS変数 | 使用場面 | 選定理由（色彩心理） |
|---|---|---|---|---|
| メインカラー | `#FF8FB1` | `--brand` | 主要CTA、強調ラベル、ブランド訴求 | 親しみと行動喚起を両立し、相談導線を見つけやすくする。 |
| ブランドダーク | `#FF6E9A` | `--brand-dark` | ホバー時のボタン背景・枠 | メインカラーの濃いバリエーション。 |
| アクセントカラー | `#8C6EE6` | `--accent` | 補助強調、バナー、アクセント導線 | 情報の区切りや補助強調に使い、視線誘導を安定させる。 |
| 背景色 | `#FFF9FB` | `--bg` | ページ背景、余白領域 | 高明度でやわらかい背景にし、長時間閲覧でも疲れにくい。 |
| カード背景 | `#FFFFFF` | `--card` | カード・パネル背景 | |
| ボーダー | `#F2DBE7` | `--line` | 区切り線、カード枠 | |
| テキスト色（メイン） | `#4A2D3C` | `--text` | 本文、重要情報 | コントラストを確保しつつ、全体トーンを統一する。 |
| テキスト色（サブ） | `#7A6170` | `--muted` | 補助説明、注釈、ヒント文 | 情報の優先度を視覚的に分離。 |

---

## 3. タイポグラフィ

### フォント方針（Google Fonts / 日本語対応）
- 見出しフォント: `Fredoka`
- 本文フォント: `M PLUS Rounded 1c`

### ルール

| 要素 | フォント | サイズ | 太さ | 行間 | letter-spacing |
|---|---|---:|---:|---:|---:|
| h1 | Fredoka | 30px | 700 | 1.18 | 0.01em |
| h2 | Fredoka | 22px | 600 | 1.3 | 0em |
| h3 | Fredoka | 16px | 600 | 1.3 | 0em |
| 本文 | M PLUS Rounded 1c | 16px | 400 | 1.6 | 0em |
| キャプション | M PLUS Rounded 1c | 13px | 600 | 1.5 | 0em |
| ボタン | M PLUS Rounded 1c | 14px | 700 | 1.2 | 0.01em |

---

## 4. UIコンポーネント

### 4.1 ボタン
- 角丸: `999px`（ピル型）
- 種別: プライマリ / セカンダリ / デンジャー
- ホバー: 背景色・枠色を段階的に変化

```css
.btn {
  font-family: "M PLUS Rounded 1c", sans-serif;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.2;
  border-radius: 999px;
  padding: 10px 16px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color .2s ease, color .2s ease, border-color .2s ease, transform .1s ease;
}
.btn:active { transform: translateY(1px); }
.btn-primary { background: #FF8FB1; color: #fff; border-color: #FF8FB1; }
.btn-primary:hover { background: #FF6E9A; border-color: #FF6E9A; }
.btn-secondary { background: #fff; color: #FF8FB1; border-color: #FF8FB1; }
.btn-secondary:hover { background: #FFF3F9; }
.btn-danger { background: #fff; color: #DC2626; border-color: #DC2626; }
.btn-danger:hover { background: #FEE2E2; color: #B91C1C; border-color: #B91C1C; }
```

### 4.2 フォーム要素
対象:
- テキスト入力
- セレクト
- テキストエリア
- チェックボックス
- ラジオボタン
- トグルスイッチ

状態:
- フォーカス時: `border-color` をメインカラー（`#FF8FB1`）に変更 + `box-shadow`
- エラー時: 赤系背景 + 赤枠 + エラーメッセージ表示

```css
.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #FF8FB1;
}
.control {
  border: 1px solid #CBD5E1;
  border-radius: 10px;
  padding: 10px 12px;
  color: #4A2D3C;
  transition: border-color .2s ease, box-shadow .2s ease, background-color .2s ease;
}
.control:focus {
  outline: none;
  border-color: #FF8FB1;
  box-shadow: 0 0 0 3px rgba(255, 143, 177, 0.25);
  background: #FFF8FC;
}
.control.is-error {
  border-color: #DC2626;
  background: #FEF2F2;
}
```

### 4.3 カード・テーブル・アラート

- カード: 画像 + タイトル + 説明 + ボタン、角丸 `26px`、シャドウ `0 10px 20px rgba(221, 136, 170, 0.12)`
- テーブル: ヘッダー付き、ストライプ表示（偶数行背景）
- アラート: 情報 / 成功 / 警告 / エラー

---

## 5. レイアウトルール

| 項目 | ルール |
|---|---|
| コンテンツ最大幅 | `960px`（推奨 `82vw`） |
| グリッド | PC(768px+): hero は `1.2fr / 0.8fr`、カード・統計は3列 / SP: 1列 |
| セクション間余白 | `--space-section: 36px` |
| 要素間余白 | `--space-block: 22px`（ブロック間）、`--space-tight: 14px`（密接要素） |
| 角丸 | 入力: `10px` / ボタン: `999px`（ピル型） / カード系: `26px` / 画像: `18–20px` |
| シャドウ | カード: `0 10px 20px rgba(221, 136, 170, 0.12)` |
| ブレークポイント | PC: `≥768px` / SP: `<768px`（主要）、補助: `≥900px` |

### CSS変数例

```css
:root {
  --brand: #ff8fb1;
  --brand-dark: #ff6e9a;
  --accent: #8c6ee6;
  --bg: #fff9fb;
  --card: #ffffff;
  --line: #f2dbe7;
  --text: #4a2d3c;
  --muted: #7a6170;
  --space-section: 36px;
  --space-block: 22px;
  --space-tight: 14px;
}
.container { width: 82vw; max-width: 960px; }
.card { border-radius: 26px; box-shadow: 0 10px 20px rgba(221, 136, 170, 0.12); }
@media (min-width: 768px) {
  .hero-grid { grid-template-columns: 1.2fr .8fr; gap: 24px; }
  .card-grid, .stats-grid { grid-template-columns: repeat(3, 1fr); }
}
```
