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

| 色の種類 | HEX | 使用場面 | 選定理由（色彩心理） |
|---|---|---|---|
| メインカラー | `#1D4ED8` | ヘッダー、見出し、主要UI | 青は信頼・誠実・安心を想起させ、医療/健康領域の不安を下げる。 |
| サブカラー | `#0EA5A4` | 補助ボタン、ステップ、ガイド要素 | 青緑は安心・調和・回復の印象があり、伴走感を表現しやすい。 |
| アクセントカラー | `#F59E0B` | CTA、注意喚起、強調 | 橙は注意喚起・行動促進に有効で、導線を見逃しにくくする。 |
| 背景色 | `#F8FAFC` | ページ背景、余白 | 高明度の淡色で圧迫感を減らし、長時間閲覧でも疲れにくい。 |
| テキスト色（メイン） | `#0F172A` | 本文、重要情報 | 高い可読性を確保。 |
| テキスト色（サブ） | `#64748B` | 補助説明、注釈 | 情報の優先度を視覚的に分離。 |

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
- 角丸: `14px`
- 種別: プライマリ / セカンダリ / デンジャー
- ホバー: 背景色・枠色を段階的に変化

```css
.btn {
  font-family: "M PLUS Rounded 1c", sans-serif;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.2;
  border-radius: 14px;
  padding: 10px 16px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color .2s ease, color .2s ease, border-color .2s ease, transform .1s ease;
}
.btn:active { transform: translateY(1px); }
.btn-primary { background:#1D4ED8; color:#fff; border-color:#1D4ED8; }
.btn-primary:hover { background:#1E40AF; border-color:#1E40AF; }
.btn-secondary { background:#fff; color:#1D4ED8; border-color:#1D4ED8; }
.btn-secondary:hover { background:#EFF6FF; }
.btn-danger { background:#fff; color:#DC2626; border-color:#DC2626; }
.btn-danger:hover { background:#FEE2E2; color:#B91C1C; border-color:#B91C1C; }
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
- フォーカス時: `border-color` をメインカラーに変更 + `box-shadow`
- エラー時: 赤系背景 + 赤枠 + エラーメッセージ表示

```css
.control {
  border: 1px solid #CBD5E1;
  border-radius: 10px;
  padding: 10px 12px;
  transition: border-color .2s ease, box-shadow .2s ease, background-color .2s ease;
}
.control:focus {
  outline: none;
  border-color: #1D4ED8;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.18);
  background: #F8FAFF;
}
.control.is-error {
  border-color: #DC2626;
  background: #FEF2F2;
}
```

### 4.3 カード・テーブル・アラート

- カード: 画像 + タイトル + 説明 + ボタン
- テーブル: ヘッダー付き、ストライプ表示（偶数行背景）
- アラート: 情報 / 成功 / 警告 / エラー

---

## 5. レイアウトルール

| 項目 | ルール |
|---|---|
| コンテンツ最大幅 | `1200px`（推奨 `92vw`） |
| グリッド | PC: 12カラム / タブレット: 8カラム / スマホ: 4カラム |
| ガター幅 | PC/Tab: `24px` / SP: `16px` |
| セクション間余白 | PC: `64px` / Tab: `48px` / SP: `32px` |
| 要素間余白 | 8pxスケール（`8 / 12 / 16 / 24 / 32`） |
| 角丸 | 入力: `10px` / ボタン: `14px` / カード: `14px` / モーダル: `16px` |
| シャドウ | カード: `0 8px 20px rgba(15, 23, 42, 0.05)` |
|  | 強調カード: `0 12px 24px rgba(37, 99, 235, 0.10)` |
|  | モーダル: `0 20px 40px rgba(15, 23, 42, 0.18)` |
| ブレークポイント | PC: `>=1024px` / Tab: `768–1023px` / SP: `<=767px` |

### CSS変数例

```css
:root {
  --content-max: 1200px;
  --gutter-pc: 24px;
  --gutter-sp: 16px;
  --section-gap-pc: 64px;
  --section-gap-tab: 48px;
  --section-gap-sp: 32px;
  --radius-input: 10px;
  --radius-button: 14px;
  --radius-card: 14px;
  --shadow-card: 0 8px 20px rgba(15, 23, 42, 0.05);
}
@media (max-width: 1023px) { /* tablet */ }
@media (max-width: 767px) { /* smartphone */ }
```
