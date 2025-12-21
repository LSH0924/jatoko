<div align="center">
  <img src="frontend/public/logo.svg" width="400">
</div>

**[English](README.md)** | **[한국어](README.ko.md)** | **日本語**

# JaToKo（日韓翻訳ツール）

**JaToKo**は、Astahダイアグラムファイル（.asta）およびSVGファイルに含まれる日本語テキストを自動的に抽出し、韓国語に翻訳するWebアプリケーションです。

- **Astahファイル**: モデル要素（クラス、属性など）やダイアグラム内のテキストを翻訳し、新しいファイルとして保存します。
- **SVGファイル**: `<text>`、`<p>`、`<h1>`などのforeignObject内のテキストを検出して翻訳し、マウスオーバー時に翻訳文を表示する機能を追加します。
- **バッチ処理**: 複数のファイルを一度にアップロードして翻訳できます。

## SVG翻訳結果
![svg-result](./sample-svg.gif)

## Astah翻訳結果
![astah-result](./sample-astah.png)

## ✨ 主な機能

- 📂 **複数ファイル形式対応**: `.asta`（Astah Professional）、`.svg`ファイルに対応
- 🤖 **自動翻訳**: DeepL APIを活用した高品質な日本語→韓国語翻訳
- 📦 **バッチ操作**: 複数ファイルのアップロード、一括翻訳、一括削除に対応
- 🖥 **直感的なUI**: React製のドラッグ＆ドロップインターフェースとファイルステータス（アップロード済み、翻訳済み）の可視化
- 🔄 **バージョン管理**: 同一ファイルの複数翻訳結果を管理
- 📊 **リアルタイム進捗表示**: SSE（Server-Sent Events）による翻訳進捗のリアルタイム表示
- 🔍 **SVGアウトライン検出**: テキストがアウトライン化されたSVGファイルの自動検出と警告

## 🛠 技術スタック

### バックエンド
- **Java 21**
- **Spring Boot 3.5.7**
- **Astah Professional SDK**（Astahファイルのパース）
- **Apache Batik**（SVG処理）
- **DeepL Java Library**（翻訳API）

### フロントエンド
- **React 19.2.3** + **React DOM 19.2.3**
- **TypeScript 5.8**
- **Vite 6.3**（ビルドツール）
- **Zustand 5.0**（状態管理）
- **Tailwind CSS 4.1**（スタイリング）
- **Axios**（HTTPクライアント）
- **Day.js**（日付・時刻フォーマット）
- **SSE（Server-Sent Events）**（リアルタイム進捗表示）

### インフラ
- **Docker & Docker Compose**

## 🚀 はじめに

### 1. 必要条件
- **Docker & Docker Compose**
- **DeepL APIキー**（FreeまたはPro）
- **Astah Professional**（SDKライブラリ抽出用、オプションだが推奨）

### 2. インストールとセットアップ

1. **リポジトリをクローン**
   ```bash
   git clone <repository-url>
   cd jatoko
   ```

2. **環境変数を設定**
   `.env.example`ファイルをコピーして`.env`ファイルを作成し、DeepL APIキーを入力します。
   ```bash
   cp .env.example .env
   ```
   `.env`ファイルの内容：
   ```env
   DEEPL_AUTH_KEY=your_deepl_api_key_here
   # DEEPL_GLOSSARY_ID=optional_glossary_id
   ```

   **用語集（Glossary）の利用（オプション）**

   プロジェクトに含まれている`glossary_for-iconnect.csv`ファイルは、DeepL用語集形式の日韓翻訳辞書です。業界用語や特定の表現の翻訳一貫性を向上させたい場合は、このファイルをDeepLアカウントに用語集として登録できます。

   1. [DeepLアカウント](https://www.deepl.com/your-account/glossaries)にログイン
   2. 用語集メニューでCSVファイルをアップロード
   3. 生成されたGlossary IDを`.env`ファイルの`DEEPL_GLOSSARY_ID`に設定

   用語集を使用すると、特定の用語が一貫して翻訳され、不自然な翻訳を減らすことができます。

3. **Astah SDKライブラリのセットアップ**
   Astah Professionalがインストールされている場合、自動的にSDKライブラリをセットアップできます：
   ```bash
   make setup-astah
   ```

   または特定のパスから取得する場合：
   ```bash
   make setup-astah ASTAH_PATH=/path/to/astah
   ```

   手動でセットアップする場合は、以下のファイルを`backend/libs`ディレクトリにコピーしてください：
   - `astah-api.jar`
   - `astah-professional.jar`
   - `rlm-1601.jar`

<details>
<summary>🔑 DeepL APIキーの取得方法（クリック）</summary>

1. [DeepL API登録ページ](https://www.deepl.com/pro-api)にアクセス
2. **DeepL API Free**プランを選択（月50万文字まで無料）
3. 会員登録とカード登録（Freeプランは実際には請求されません）
4. アカウント管理ページから**「APIキー」**をコピー

</details>

### 3. 実行（Docker Compose）

`Makefile`を使って簡単に実行できます。

```bash
# サービス開始（バックエンド＋フロントエンド）
make up

# サービス停止
make down

# ログ確認
make logs
```

起動後、ブラウザでアクセス：
- **フロントエンド**: [http://localhost:3000](http://localhost:3000)
- **バックエンドAPI**: [http://localhost:8080](http://localhost:8080)

### 4. ローカル開発

**バックエンド**
```bash
cd backend
./gradlew bootRun
```

**フロントエンド**
```bash
cd frontend
npm install
npm run dev
```

### 5. テストファイル

プロジェクトルートに翻訳機能をテストするためのサンプルファイルが含まれています：

| ファイル | 説明 |
|----------|------|
| `test.asta` | Astah Professionalダイアグラムサンプル（日本語クラス・属性を含む） |
| `test.svg` | SVGテキスト要素サンプル |
| `UseCase.svg` | ユースケースダイアグラムSVGサンプル |

アプリケーション起動後、これらのテストファイルをアップロードして翻訳機能をテストできます。

## 📡 APIリファレンス

ファイル管理用の統合コントローラー（`DirectoryController`）を使用します。

### 主なエンドポイント

| メソッド | エンドポイント | 説明 |
|----------|----------------|------|
| `GET` | `/api/files/metadata` | 全ファイル一覧とステータス（翻訳状況、バージョン、アウトライン有無など）を取得 |
| `GET` | `/api/files/{type}` | 指定ディレクトリ（`target`または`translated`）のファイル一覧を取得 |
| `POST` | `/api/files/target` | ファイルアップロード（Multipart）- アウトライン有無も返却 |
| `POST` | `/api/translate-file` | 単一ファイル翻訳リクエスト（Body: `{"fileName": "...", "clientId": "..."}`） |
| `POST` | `/api/translate/batch` | バッチファイル翻訳リクエスト（Body: `{"fileNames": ["...", "..."]}`） |
| `GET` | `/api/download/translated/{targetFileName}` | targetファイル名に対応する最新翻訳ファイルをダウンロード |
| `GET` | `/api/files/translated/{fileName}` | 特定の翻訳ファイルをダウンロード |
| `DELETE` | `/api/files/{type}/{fileName}` | ファイル削除（targetタイプ削除時は翻訳ファイルとメタデータも削除） |
| `POST` | `/api/files/batch-delete` | バッチファイル削除（Body: `{"fileNames": ["...", "..."]}`） |
| `GET` | `/api/progress/subscribe/{clientId}` | SSE: 翻訳進捗のリアルタイム購読（EventSource使用） |

## 📂 プロジェクト構造

```
jatoko/
├── backend/                           # Spring Boot Application (Java 21)
│   ├── src/main/java/com/jatoko/
│   │   ├── controller/                # REST APIコントローラー
│   │   │   └── DirectoryController    # ファイル管理と翻訳APIエントリーポイント
│   │   ├── service/                   # ビジネスロジック
│   │   │   ├── DirectoryService       # 統合ファイル管理サービス
│   │   │   ├── ProgressService        # SSE進捗管理
│   │   │   ├── MetadataService        # ファイルメタデータ管理
│   │   │   ├── BaseParserService      # 共通パースロジック（抽象クラス）
│   │   │   ├── AstahParserService     # Astahファイルパースと翻訳
│   │   │   ├── SvgParserService       # SVGファイルパースと翻訳
│   │   │   ├── extractor/             # テキスト抽出ロジック
│   │   │   │   ├── NodeExtractor      # Astahモデル再帰探索と抽出
│   │   │   │   ├── DiagramExtractor   # ダイアグラム別抽出戦略
│   │   │   │   └── SvgTextExtractor   # SVGテキストノード抽出
│   │   │   ├── translator/            # 翻訳エンジン
│   │   │   │   ├── Translator         # DeepL API統合
│   │   │   │   └── TranslationMapBuilder # 翻訳マッピング最適化
│   │   │   ├── applier/               # 翻訳適用ロジック
│   │   │   │   ├── ModelTranslationApplier    # Astahモデル要素翻訳
│   │   │   │   ├── DiagramTranslationApplier  # Astahダイアグラム翻訳
│   │   │   │   └── SvgTranslationApplier      # SVG DOM翻訳
│   │   │   └── svg/                   # SVG処理ユーティリティ
│   │   │       ├── SvgDocumentLoader  # SVG DOMパースと保存
│   │   │       └── SvgStyleManager    # SVGスタイル管理
│   │   ├── dto/                       # データ転送オブジェクト
│   │   ├── model/                     # ドメインモデル
│   │   ├── config/                    # 設定クラス
│   │   ├── util/                      # ユーティリティクラス
│   │   │   ├── JapaneseDetector       # 日本語テキスト検出
│   │   │   ├── KoreanDetector         # 韓国語テキスト検出
│   │   │   └── SvgOutlineDetector     # SVGアウトライン検出
│   │   └── exception/                 # 例外処理
│   └── libs/                          # Astah SDK jarファイル
│       ├── astah-api.jar
│       ├── astah-professional.jar
│       └── rlm-1601.jar
├── frontend/                          # React Application
│   ├── src/
│   │   ├── components/                # UIコンポーネント
│   │   │   ├── FileListPanel.tsx      # メインファイル管理パネル
│   │   │   └── FileList/              # ファイル一覧サブコンポーネント
│   │   ├── stores/                    # Zustand状態管理
│   │   │   └── translationStore.ts    # 翻訳タスク状態管理
│   │   ├── services/                  # API通信とビジネスロジック
│   │   │   └── api.ts                 # AxiosベースAPIクライアント
│   │   └── hooks/                     # カスタムReact Hooks
│   │       └── useFileManagement.ts   # ファイル管理ロジック
│   └── package.json
├── target/                            # アップロード済み元ファイル保存場所
├── translated/                        # 翻訳済みファイル保存場所
├── docker-compose.yml                 # Dockerコンテナオーケストレーション
├── Makefile                           # ビルド・実行コマンドショートカット
├── .env.example                       # 環境変数テンプレート
├── CLAUDE.md                          # AIコーディングガイド
└── LICENSE                            # AGPL-3.0ライセンス
```

## 📝 ライセンス

このプロジェクトは**GNU Affero General Public License v3.0（AGPL-3.0）**の下で配布されています。詳細は[LICENSE](LICENSE)ファイルをご覧ください。

> **注意**: このプロジェクトは**Astah Professional SDK**を使用しています。Astah SDKおよび関連ライブラリの著作権とライセンスポリシーは、それぞれの著作権者（Change Vision, Inc.）に帰属し、これを遵守する必要があります。
