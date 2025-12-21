<div align="center">
  <img src="frontend/public/logo.svg" width="400">
</div>

**English** | **[한국어](README.ko.md)** | **[日本語](README.ja.md)**

# JaToKo (Japanese-to-Korean Translator)

**JaToKo** is a web application that automatically extracts Japanese text from Astah diagram files (.asta) and SVG files and translates them into Korean.

- **Astah Files**: Translates model elements (classes, attributes, etc.) and text within diagrams, saving them as new files.
- **SVG Files**: Detects text in `<text>`, `<p>`, `<h1>` and other foreignObject elements, translates them, and adds hover-to-show translation functionality.
- **Batch Processing**: Upload and translate multiple files at once.

## SVG Translation Result
![svg-result](./sample-svg.gif)

## Astah Translation Result
![astah-result](./sample-astah.png)

## ✨ Key Features

- 📂 **Multiple File Format Support**: `.asta` (Astah Professional), `.svg` files
- 🤖 **Automatic Translation**: High-quality Japanese → Korean translation using DeepL API
- 📦 **Batch Operations**: Multiple file upload, batch translation, and batch deletion
- 🖥 **Intuitive UI**: React-based drag-and-drop interface with file status visualization (uploaded, translated)
- 🔄 **Version Management**: Manage multiple translation results for the same file
- 📊 **Real-time Progress**: Live translation progress display via SSE (Server-Sent Events)
- 🔍 **SVG Outline Detection**: Automatic detection and warning for SVG files with outlined text

## 🛠 Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.5.7**
- **Astah Professional SDK** (Astah file parsing)
- **Apache Batik** (SVG processing)
- **DeepL Java Library** (Translation API)

### Frontend
- **React 19.2.3** + **React DOM 19.2.3**
- **TypeScript 5.8**
- **Vite 6.3** (Build tool)
- **Zustand 5.0** (State management)
- **Tailwind CSS 4.1** (Styling)
- **Axios** (HTTP client)
- **Day.js** (Date/time formatting)
- **SSE (Server-Sent Events)** (Real-time progress display)

### Infrastructure
- **Docker & Docker Compose**

## 🚀 Getting Started

### 1. Prerequisites
- **Docker & Docker Compose**
- **DeepL API Key** (Free or Pro)
- **Astah Professional** (For SDK library extraction, optional but recommended)

### 2. Installation and Setup

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd jatoko
   ```

2. **Set Environment Variables**
   Copy the `.env.example` file to create a `.env` file and enter your DeepL API key.
   ```bash
   cp .env.example .env
   ```
   `.env` file contents:
   ```env
   DEEPL_AUTH_KEY=your_deepl_api_key_here
   # DEEPL_GLOSSARY_ID=optional_glossary_id
   ```

   **Using Glossary (Optional)**

   The included `glossary_for-iconnect.csv` file is a Japanese-Korean translation dictionary in DeepL glossary format. If you want to improve translation consistency for industry terms or specific expressions, you can register this file as a glossary in your DeepL account.

   1. Log in to your [DeepL account](https://www.deepl.com/your-account/glossaries)
   2. Upload the CSV file in the Glossary menu
   3. Set the generated Glossary ID in `DEEPL_GLOSSARY_ID` in your `.env` file

   Using a glossary ensures specific terms are translated consistently, reducing awkward translations.

3. **Set Up Astah SDK Libraries**
   If Astah Professional is installed, you can automatically set up the SDK libraries:
   ```bash
   make setup-astah
   ```

   Or to fetch from a specific path:
   ```bash
   make setup-astah ASTAH_PATH=/path/to/astah
   ```

   To set up manually, copy the following files to the `backend/libs` directory:
   - `astah-api.jar`
   - `astah-professional.jar`
   - `rlm-1601.jar`

<details>
<summary>🔑 How to Get a DeepL API Key (Click)</summary>

1. Visit the [DeepL API signup page](https://www.deepl.com/pro-api)
2. Select the **DeepL API Free** plan (500,000 characters/month free)
3. Sign up and register a card (Free plan won't actually charge)
4. Copy the **'API Key'** from your account management page

</details>

### 3. Running (Docker Compose)

You can easily run the application using the `Makefile`.

```bash
# Start services (backend + frontend)
make up

# Stop services
make down

# View logs
make logs
```

After starting, access via browser:
- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)

### 4. Local Development

**Backend**
```bash
cd backend
./gradlew bootRun
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

### 5. Test Files

Sample files for testing translation features are included in the project root:

| File | Description |
|------|-------------|
| `test.asta` | Astah Professional diagram sample (contains Japanese classes/attributes) |
| `test.svg` | SVG text element sample |
| `UseCase.svg` | Use case diagram SVG sample |

After starting the application, upload these test files to test the translation functionality.

## 📡 API Reference

Uses an integrated controller (`DirectoryController`) for file management.

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/files/metadata` | Get all file list and status (translation status, version, outline status, etc.) |
| `GET` | `/api/files/{type}` | Get file list from specified directory (`target` or `translated`) |
| `POST` | `/api/files/target` | Upload file (Multipart) - also returns outline status |
| `POST` | `/api/translate-file` | Single file translation request (Body: `{"fileName": "...", "clientId": "..."}`) |
| `POST` | `/api/translate/batch` | Batch file translation request (Body: `{"fileNames": ["...", "..."]}`) |
| `GET` | `/api/download/translated/{targetFileName}` | Download latest translated file for target filename |
| `GET` | `/api/files/translated/{fileName}` | Download specific translated file |
| `DELETE` | `/api/files/{type}/{fileName}` | Delete file (deleting target type also removes translated files and metadata) |
| `POST` | `/api/files/batch-delete` | Batch file deletion (Body: `{"fileNames": ["...", "..."]}`) |
| `GET` | `/api/progress/subscribe/{clientId}` | SSE: Subscribe to real-time translation progress (using EventSource) |

## 📂 Project Structure

```
jatoko/
├── backend/                           # Spring Boot Application (Java 21)
│   ├── src/main/java/com/jatoko/
│   │   ├── controller/                # REST API Controllers
│   │   │   └── DirectoryController    # File management and translation API entry point
│   │   ├── service/                   # Business Logic
│   │   │   ├── DirectoryService       # Integrated file management service
│   │   │   ├── ProgressService        # SSE progress management
│   │   │   ├── MetadataService        # File metadata management
│   │   │   ├── BaseParserService      # Common parsing logic (abstract class)
│   │   │   ├── AstahParserService     # Astah file parsing and translation
│   │   │   ├── SvgParserService       # SVG file parsing and translation
│   │   │   ├── extractor/             # Text extraction logic
│   │   │   │   ├── NodeExtractor      # Astah model recursive traversal and extraction
│   │   │   │   ├── DiagramExtractor   # Per-diagram extraction strategy
│   │   │   │   └── SvgTextExtractor   # SVG text node extraction
│   │   │   ├── translator/            # Translation engine
│   │   │   │   ├── Translator         # DeepL API integration
│   │   │   │   └── TranslationMapBuilder # Translation mapping optimization
│   │   │   ├── applier/               # Translation application logic
│   │   │   │   ├── ModelTranslationApplier    # Astah model element translation
│   │   │   │   ├── DiagramTranslationApplier  # Astah diagram translation
│   │   │   │   └── SvgTranslationApplier      # SVG DOM translation
│   │   │   └── svg/                   # SVG processing utilities
│   │   │       ├── SvgDocumentLoader  # SVG DOM parsing and saving
│   │   │       └── SvgStyleManager    # SVG style management
│   │   ├── dto/                       # Data Transfer Objects
│   │   ├── model/                     # Domain Models
│   │   ├── config/                    # Configuration Classes
│   │   ├── util/                      # Utility Classes
│   │   │   ├── JapaneseDetector       # Japanese text detection
│   │   │   ├── KoreanDetector         # Korean text detection
│   │   │   └── SvgOutlineDetector     # SVG outline detection
│   │   └── exception/                 # Exception Handling
│   └── libs/                          # Astah SDK jar files
│       ├── astah-api.jar
│       ├── astah-professional.jar
│       └── rlm-1601.jar
├── frontend/                          # React Application
│   ├── src/
│   │   ├── components/                # UI Components
│   │   │   ├── FileListPanel.tsx      # Main file management panel
│   │   │   └── FileList/              # File list sub-components
│   │   ├── stores/                    # Zustand State Management
│   │   │   └── translationStore.ts    # Translation task state management
│   │   ├── services/                  # API Communication and Business Logic
│   │   │   └── api.ts                 # Axios-based API client
│   │   └── hooks/                     # Custom React Hooks
│   │       └── useFileManagement.ts   # File management logic
│   └── package.json
├── target/                            # Uploaded original file storage
├── translated/                        # Translated file storage
├── docker-compose.yml                 # Docker container orchestration
├── Makefile                           # Build and run command shortcuts
├── .env.example                       # Environment variable template
├── CLAUDE.md                          # AI Coding Guide
└── LICENSE                            # AGPL-3.0 License
```

## 📝 License

This project is distributed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. See the [LICENSE](LICENSE) file for details.

> **Note**: This project uses the **Astah Professional SDK**. The copyright and licensing policies of the Astah SDK and related libraries belong to their respective copyright holders (Change Vision, Inc.), and must be complied with.
