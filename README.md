# JaToKo (Japanese-to-Korean Translator)

**JaToKo**는 Astah 다이어그램 파일(.asta) 및 SVG 파일에 포함된 일본어 텍스트를 자동으로 추출하여 한국어로 번역해주는 웹 애플리케이션입니다.

- **Astah 파일**: 모델 요소(클래스, 속성 등)와 다이어그램 내 텍스트를 번역하여 새로운 파일로 저장합니다.
- **SVG 파일**: `<text>` 요소를 감지하여 번역하고, 마우스 오버 시 번역문을 보여주는 기능을 추가합니다.
- **일괄 처리**: 여러 파일을 한 번에 업로드하고 번역할 수 있습니다.

## ✨ 주요 기능

- 📂 **다양한 파일 지원**: `.asta` (Astah Professional), `.svg` 파일 지원
- 🤖 **자동 번역**: DeepL API를 활용한 고품질 일본어 → 한국어 번역
- 📦 **배치 작업**: 다중 파일 업로드, 일괄 번역, 일괄 삭제 지원
- 🖥 **직관적인 UI**: React 기반의 드래그 앤 드롭 인터페이스 및 파일 상태(업로드됨, 번역됨) 시각화
- 🔄 **버전 관리**: 동일 파일에 대한 여러 번의 번역 결과 관리

## 🛠 기술 스택

### Backend
- **Java 21**
- **Spring Boot 3.5.7**
- **Astah Professional SDK** (Astah 파일 파싱)
- **Apache Batik** (SVG 처리)
- **DeepL Java Library** (번역 API)

### Frontend
- **React 19**
- **TypeScript**
- **Vite**
- **Zustand** (상태 관리)
- **Tailwind CSS** (스타일링)

### Infrastructure
- **Docker & Docker Compose**

## 🚀 시작하기

### 1. 필수 요구사항
- **Docker & Docker Compose**
- **DeepL API Key** (Free 또는 Pro)
- **Astah Professional** (SDK 라이브러리 추출용, 선택 사항이지만 권장)

### 2. 설치 및 설정

1. **저장소 클론**
   ```bash
   git clone <repository-url>
   cd jatoko
   ```

2. **환경 변수 설정**
   `.env.example` 파일을 복사하여 `.env` 파일을 생성하고 DeepL API 키를 입력합니다.
   ```bash
   cp .env.example .env
   ```
   `.env` 파일 내용:
   ```env
   DEEPL_AUTH_KEY=your_deepl_api_key_here
   # DEEPL_GLOSSARY_ID=optional_glossary_id
   ```

3. **Astah SDK 라이브러리 설정**
   `backend/libs` 디렉토리에 다음 Astah Professional jar 파일들이 있어야 합니다. (없을 경우 빌드 실패 가능성 있음)
   - `astah-api.jar`
   - `astah-professional.jar`
   - `rlm-1601.jar`

### 3. 실행 (Docker Compose)

`Makefile`을 사용하여 간편하게 실행할 수 있습니다.

```bash
# 서비스 시작 (백엔드 + 프론트엔드)
make up

# 서비스 종료
make down

# 로그 확인
make logs
```

실행 후 브라우저 접속:
- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)

### 4. 로컬 개발 실행

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

## 📡 API 명세

파일 관리를 위한 통합 컨트롤러(`DirectoryController`)를 사용합니다.

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/files/metadata` | 전체 파일 목록 및 상태(번역 여부 등) 조회 |
| `POST` | `/api/files/target` | 파일 업로드 (Multipart) |
| `POST` | `/api/translate-file` | 단일 파일 번역 요청 |
| `POST` | `/api/translate/batch` | 다중 파일 일괄 번역 요청 |
| `GET` | `/api/download/translated/{fileName}` | 최신 번역된 파일 다운로드 |
| `DELETE` | `/api/files/{type}/{fileName}` | 파일 삭제 |

## 📂 프로젝트 구조

```
jatoko/
├── backend/            # Spring Boot Application
│   ├── src/main/java/com/jatoko/
│   │   ├── controller/ # DirectoryController (API 진입점)
│   │   ├── service/    # DirectoryService, AstahParserService, SvgParserService
│   │   └── ...
├── frontend/           # React Application
│   ├── src/
│   │   ├── components/ # FileListPanel 등 UI 컴포넌트
│   │   ├── services/   # API 통신 로직
│   │   └── stores/     # Zustand 상태 관리
├── translated/         # 번역 결과물 저장소 (Docker 볼륨)
├── uploads/            # 업로드 파일 저장소 (Docker 볼륨)
├── docker-compose.yml
└── Makefile
```

## 📝 라이선스

이 프로젝트는 **GNU Affero General Public License v3.0 (AGPL-3.0)** 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하십시오.

> **주의**: 이 프로젝트는 **Astah Professional SDK**를 사용합니다. Astah SDK 및 관련 라이브러리의 저작권 및 라이선스 정책은 해당 저작권자(Change Vision, Inc.)에게 있으며, 이를 준수해야 합니다.