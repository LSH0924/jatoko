# JaToKo - Astah, SVG 용 일본어 번역 도구

- JaToKo는 Astah Professional 다이어그램 파일(.asta) 및 SVG 파일에서 일본어 텍스트를 자동으로 추출하고 번역하는 웹 애플리케이션입니다.
- Astah 파일 번역시 원문 / 번역문 으로 표시합니다. 
- SVG 파일 번역시 원문에 마우스 hover 시 번역문을 표시합니다.

## 주요 기능

- ✅ **Astah 파일 지원**: `.asta` 파일의 모든 모델 요소 및 다이어그램 텍스트 번역
- ✅ **SVG 파일 지원**: `<text>` 및 `<foreignObject>` 요소 내 텍스트 번역
- ✅ **자동 번역**: DeepL API를 통한 고품질 일본어 → 한국어 번역
- ✅ **웹 UI**: React 기반 드래그 앤 드롭 파일 업로드 인터페이스

## 시스템 요구사항

- **Docker & Docker Compose** (필수)
- **Java 21** (로컬 개발 시)
- **Node.js 18+** (로컬 개발 시)
- **Astah Professional 라이선스** (Astah SDK 사용 시 필요)
- **DeepL API 키** (자동 번역 사용 시 필요)

## 빠른 시작 (Makefile 사용)

### 1. 환경변수 설정
```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집 (실제 API 키 입력)
# DEEPL_AUTH_KEY=your-deepl-auth-key-here
# DEEPL_GLOSSARY_ID=your-glossary-id-here
```

### 2. 서비스 실행
```bash
# 전체 스택 실행 (자동으로 .env 확인)
make up

# 또는 Docker Compose 직접 사용
docker compose up -d
```

### 3. 접속
- **백엔드**: http://localhost:8080
- **프론트엔드**: http://localhost:3000

### 4. 유용한 명령어
```bash
make help          # 사용 가능한 명령어 목록
make logs          # 전체 로그 확인
make logs-backend  # 백엔드 로그만
make logs-frontend # 프론트엔드 로그만
make restart       # 서비스 재시작
make down          # 서비스 종료
make ps            # 실행 중인 컨테이너 확인
make build         # Docker 이미지 재빌드
make clean         # 모든 리소스 삭제
```

## 사용 방법

### 웹 UI 사용
1. 브라우저에서 http://localhost:3000 접속
2. `.asta` 또는 `.svg` 파일을 드래그 앤 드롭 또는 클릭하여 업로드
3. "번역 시작" 버튼 클릭
4. 번역 완료 후 자동으로 파일 다운로드


## 프로젝트 구조

```
jatoko/
├── backend/                    # Spring Boot 백엔드
│   ├── src/main/java/com/jatoko/
│   │   ├── controller/        # REST API 컨트롤러
│   │   ├── service/           # 비즈니스 로직
│   │   │   ├── BaseParserService.java       # 공통 파서 로직 (템플릿 메서드)
│   │   │   ├── AstahParserService.java      # Astah 파일 처리
│   │   │   ├── SvgParserService.java        # SVG 파일 처리
│   │   │   ├── extractor/                   # 텍스트 추출 컴포넌트
│   │   │   ├── applier/                     # 번역 적용 컴포넌트
│   │   │   └── translator/                  # DeepL 번역 통합
│   │   ├── model/             # 데이터 모델
│   │   └── util/              # 유틸리티 (일본어 감지 등)
│   └── libs/                  # Astah SDK jar 파일
│
├── frontend/                   # React 프론트엔드
│   ├── src/
│   │   ├── components/        # UI 컴포넌트
│   │   ├── stores/            # Zustand 상태 관리
│   │   ├── services/          # API 클라이언트
│   │   └── types/             # TypeScript 타입 정의
│   └── package.json
│
├── docker-compose.yml         # Docker 통합 설정
├── Makefile                   # Docker 명령어 단축키
├── .env.example               # 환경변수 템플릿
└── README.md
```

## 기술 스택

### 백엔드
- **Spring Boot 3.5.7** - 웹 프레임워크
- **Astah Professional SDK** - `.asta` 파일 파싱 및 수정
- **Apache Batik 1.19** - SVG DOM 파싱 및 조작
- **DeepL Java SDK 1.11.0** - 번역 API
- **Java 21** - 프로그래밍 언어

### 프론트엔드
- **React 19.2.0** - UI 프레임워크
- **Zustand 5.0.8** - 상태 관리 (Flux 패턴)
- **Axios 1.13.2** - HTTP 클라이언트
- **Vite 6.3.5** - 빌드 도구
- **TypeScript 5.8.3** - 타입 안전성

## 개발 가이드

### 백엔드 테스트

```bash
cd backend

# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests com.jatoko.service.AstahParserServiceTest

# .asta 파일 분석 (개발용)
./gradlew analyzeTestAsta
```

### 프론트엔드 개발

```bash
cd frontend

# 개발 서버 (Hot Module Replacement)
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 결과 미리보기
npm run preview

# 린트 검사
npm run lint
```

### Astah SDK 라이브러리 설치

Astah Professional을 설치한 후, SDK jar 파일을 `backend/libs/` 디렉토리에 복사:
(파일이 없다면 요청해주세요.)

```bash
# macOS 예시
cp "/Applications/astah professional/astah-api.jar" backend/libs/
cp "/Applications/astah professional/astah-professional.jar" backend/libs/
cp "/Applications/astah professional/rlm-1601.jar" backend/libs/
```

### DeepL API 키 설정

**Docker Compose 사용 시** (권장):
```bash
# 1. .env 파일 생성
cp .env.example .env

# 2. .env 파일 편집
# DEEPL_AUTH_KEY=your-actual-key
# DEEPL_GLOSSARY_ID=your-actual-glossary-id

# 3. 실행
make up
```

**로컬 개발 시**:
```bash
export DEEPL_AUTH_KEY="your-api-key-here"
export DEEPL_GLOSSARY_ID="your-glossary-id"

# 백엔드 실행
cd backend
./gradlew bootRun
```

**환경변수 우선순위**:
1. `.env` 파일 (Docker Compose)
2. 시스템 환경변수 (`export`)
3. `application.yml`의 기본값 (없으면 오류)

### Java 버전 불일치

```bash
# 현재 Java 버전 확인
java -version

# Java 21로 전환 (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
source ~/.zshrc

# 확인
java -version
```

### Astah 라이선스 오류

```
LicenseException: Astah Professional license not found
```

→ Astah Professional이 설치되어 있고 유효한 라이선스가 활성화되어 있는지 확인

### DeepL API 오류

```
DeepLException: Authentication failed
```

→ `DEEPL_API_KEY` 환경 변수가 올바르게 설정되어 있는지 확인

## API 엔드포인트 전체 목록

### Astah 파일 처리 (AstahController)

| 메서드 | 경로 | 설명 | 요청 본문/파라미터 |
|--------|------|------|--------------------|
| POST | `/api/upload/astah` | .asta 파일 업로드 | `file`: MultipartFile |
| POST | `/api/apply-translation-integrated` | 추출 + 번역 + 적용 통합 | `{"sessionId": "..."}` |
| GET | `/api/download/{sessionId}` | 번역된 .asta 다운로드 | `sessionId`: 경로 변수 |

### SVG 파일 처리 (SvgController)

| 메서드 | 경로 | 설명 | 요청 본문/파라미터 |
|--------|------|------|--------------------|
| POST | `/api/svg/upload` | SVG 파일 업로드 | `file`: MultipartFile |
| POST | `/api/svg/apply-translation-integrated` | 추출 + 번역 + 적용 통합 | `{"sessionId": "..."}` |
| GET | `/api/svg/download/{sessionId}` | 번역된 SVG 다운로드 | `sessionId`: 경로 변수 |

### 파일 관리 (DirectoryController)

| 메서드 | 경로 | 설명 | 요청 본문/파라미터 |
|--------|------|------|--------------------|
| GET | `/api/files/{type}` | 특정 타입 디렉토리의 파일 목록 조회 | `type`: uploads/target/translated |
| POST | `/api/files/target` | target 디렉토리에 파일 업로드 | `file`: MultipartFile |
| GET | `/api/files/translated/{fileName}` | translated 디렉토리에서 파일 다운로드 | `fileName`: 파일명 |
| POST | `/api/translate-file` | target 디렉토리의 파일 번역 | `{"fileName": "..."}` |
| DELETE | `/api/files/{type}/{fileName}` | 특정 디렉토리의 파일 삭제 | `type`, `fileName`: 경로 변수 |

## 라이선스

이 프로젝트는 개인 목적으로 사용 가능합니다. Astah Professional SDK는 별도의 라이선스가 필요합니다.

## 기여

버그 리포트 및 기능 제안은 이슈 트래커를 통해 제출해주세요.
