# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

JaToKo는 Astah Professional 다이어그램 파일(.asta) 및 SVG 파일에서 일본어 텍스트를 추출하고 번역을 적용하는 풀스택 애플리케이션입니다.

- **백엔드**: Spring Boot 3.5.7 REST API (Java 21)
- **프론트엔드**: React + Vite + Zustand
- **번역 엔진**: DeepL API 통합

## 빌드 및 실행

### 필수 환경
- **Java 21** (OpenJDK 21.0.9 이상) - Java 17이 설치되어 있지만 프로젝트는 Java 21 필요
- **Astah Professional SDK** - `backend/libs/`에 jar 파일 포함
- Gradle Wrapper 사용 (시스템 gradle 불필요)

### Java 21 설정
```bash
# Java 21 설치 확인
java -version

# macOS: Java 21로 전환
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
source ~/.zshrc
```

### 백엔드 빌드 및 실행
```bash
cd backend

# 빌드
./gradlew clean build

# 실행 (http://localhost:8080)
./gradlew bootRun

# 테스트
./gradlew test

# .asta 파일 분석 (개발용)
./gradlew analyzeTestAsta
```

### 프론트엔드 빌드 및 실행
```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행 (http://localhost:5173)
npm run dev

# 프로덕션 빌드
npm run build
```

### Docker Compose 실행
```bash
# 전체 스택 실행
docker compose up -d

# 로그 확인
docker compose logs -f

# 종료
docker compose down
```

### 포트 충돌 해결
```bash
lsof -ti:8080 | xargs kill -9  # 백엔드
lsof -ti:5173 | xargs kill -9  # 프론트엔드
```

## 핵심 아키텍처

### 템플릿 메서드 패턴 기반 파서 계층 구조

프로젝트는 `.asta`와 `.svg` 파일 모두를 지원하며, 공통 로직은 `BaseParserService` 추상 클래스에 구현되어 있습니다:

```
BaseParserService<T>  (추상 클래스)
├── AstahParserService (DiagramNode)
└── SvgParserService (SvgTextNode)
```

**공통 워크플로우** (`BaseParserService`):
1. `extractNodes()`: 파일에서 일본어 텍스트 추출
2. `translateNodes()`: DeepL API로 번역 (중복 제거 최적화)
3. `applyTranslationsInternal()`: 번역을 파일에 적용

### Astah SDK 통합 패턴

**중요한 제약사항:**
1. **동기적 처리 필수**: Astah SDK의 `ProjectAccessor`는 동시에 하나의 프로젝트만 열 수 있음
2. **세션 기반 워크플로우**: 파일 업로드 → 추출 → 번역 → 적용 → 다운로드 순서 필수
3. **재귀적 모델 탐색**: 패키지 → 클래스 → 속성/메서드 순으로 트리 구조 탐색

**핵심 컴포넌트** (backend/src/main/java/com/jatoko/service/):
- `NodeExtractor`: 재귀적으로 모델 트리를 탐색하며 일본어 포함 노드 추출
- `ModelTranslationApplier`: 모델 요소(클래스, 속성, 메서드)에 번역 적용
- `DiagramTranslationApplier`: 다이어그램 프레젠테이션 레이어에 번역 적용

### SVG 파서 아키텍처

**Apache Batik 사용** (`SvgParserService`):
- `<text>` 요소: 직접 텍스트 수정
- `<foreignObject>` 내부: HTML 텍스트 노드 수정
- 번역은 호버 시 오버레이로 표시 (원문 유지)

**핵심 컴포넌트**:
- `SvgDocumentLoader`: SVG DOM 파싱 및 저장
- `SvgTextExtractor`: `<text>` 및 `<foreignObject>` 텍스트 추출
- `SvgTranslationApplier`: DOM 조작으로 번역 적용

### 일본어 감지 로직

**`JapaneseDetector`** (backend/src/main/java/com/jatoko/util/JapaneseDetector.java):
- 히라가나: `\u3040-\u309F`
- 가타카나: `\u30A0-\u30FF`
- 한자: `\u4E00-\u9FAF`

### 세션 관리

- 메모리 기반 (`sessionId` → 파일 경로 매핑)
- 서버 재시작 시 초기화됨 (영속화 없음)
- 디렉토리 구조:
  - `uploads/`: 원본 파일
  - `target/`: 추출 결과 JSON
  - `translated/`: 번역된 파일

## API 워크플로우

### 통합 API (권장 - DeepL 자동 번역)

**Astah 파일**:
1. `POST /api/upload/astah` - .asta 파일 업로드, `sessionId` 반환
2. `POST /api/apply-translation-integrated` - 추출 + 번역 + 적용 통합 실행
3. `GET /api/download/{sessionId}` - 번역된 .asta 파일 다운로드

**SVG 파일**:
1. `POST /api/svg/upload` - SVG 파일 업로드, `sessionId` 반환
2. `POST /api/svg/apply-translation-integrated` - 추출 + 번역 + 적용 통합 실행
3. `GET /api/svg/download/{sessionId}` - 번역된 SVG 파일 다운로드

### 수동 API (JSON 편집이 필요한 경우)

**Astah 파일**:
1. `POST /api/upload/astah` - .asta 파일 업로드
2. `POST /api/extract` - 일본어 노드 추출
3. `POST /api/download/json` - 추출 결과 JSON 다운로드
4. (사용자가 JSON의 `translatedName` 필드 수동 편집)
5. `POST /api/upload/translation?sessionId={id}` - 번역 JSON 업로드
6. `POST /api/apply-translation` - 번역을 .asta 파일에 적용
7. `GET /api/download/{sessionId}` - 번역된 .asta 파일 다운로드

**SVG 파일**: `/api/svg/*` 엔드포인트로 동일한 흐름

### API 테스트

**curl 예제 (통합 API)**:
```bash
# Astah 파일 업로드
SESSION_ID=$(curl -X POST http://localhost:8080/api/upload/astah \
  -F "file=@test.asta" -s | jq -r '.sessionId')

# 번역 적용 (추출 + 번역 + 적용)
curl -X POST http://localhost:8080/api/apply-translation-integrated \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\": \"$SESSION_ID\"}" -s

# 다운로드
curl -X GET "http://localhost:8080/api/download/$SESSION_ID" \
  -o "translated.asta"
```

## 주요 의존성

**백엔드**:
- **Spring Boot 3.5.7** (Java 21 필수)
- **Astah Professional SDK** - `backend/libs/` 디렉토리에 수동 배치:
  - `astah-api.jar`
  - `astah-professional.jar`
  - `rlm-1601.jar`
- **Google Guava 33.0.0-jre** - Astah SDK 내부 의존성
- **Apache Batik 1.19** - SVG DOM 파싱 및 조작
- **DeepL Java 1.11.0** - 번역 API

**프론트엔드**:
- **React 19.2.0** + **React DOM 19.2.0**
- **Zustand 5.0.8** - 상태 관리 (Flux 패턴)
- **Axios 1.13.2** - HTTP 클라이언트
- **Vite 6.3.5** - 빌드 도구

## 프론트엔드 상태 관리 패턴

**Zustand Store 분리 패턴** (`frontend/src/stores/translationStore.ts`):
```typescript
// State: 데이터만 포함 (불변)
type TranslationStore = {
  sessionId: string;
  selectedFile: File | null;
  // ...
};

// Actions: 별도 객체로 분리
const store = {
  setSelectedFile: (file: File | null) => { /* ... */ },
  uploadFile: async () => { /* ... */ },
  // ...
};

// Export
export const useTranslationStore = create<TranslationStore>(/* ... */);
export const translationStore = store;
```

컴포넌트에서 사용:
```typescript
// State 읽기
const status = useTranslationStore((s) => s.status);

// Action 호출
translationStore.uploadFile();
```

## 개발 시 주의사항

### Astah SDK 라이선스
- Astah Professional 라이선스 필요
- SDK 사용 시 자동으로 시스템의 Astah 라이선스 확인
- 라이선스 없으면 `LicenseException` 발생

### 파일 크기 제한
- 최대 50MB (`application.yml`의 `spring.servlet.multipart.max-file-size`)

### 에러 처리 패턴
**백엔드**: 부분 성공 허용
```java
try {
    // Astah API 호출
} catch (Exception e) {
    // 일부 요소 접근 실패 시에도 계속 진행
    System.err.println("Warning: " + e.getMessage());
}
```

**프론트엔드**: AxiosError 타입 체크
```typescript
catch (error) {
  const axiosError = error as AxiosError<ApiErrorResponse>;
  console.error(axiosError.response?.data?.error);
}
```

### DeepL API 키 설정
`backend/src/main/resources/application.yml`:
```yaml
deepl:
  api-key: ${DEEPL_API_KEY:your-api-key-here}
```

환경 변수 설정:
```bash
export DEEPL_API_KEY="your-actual-key"
```

## Docker 지원

`docker-compose.yml` - 프론트엔드 + 백엔드 통합:
- 백엔드: http://localhost:8080
- 프론트엔드: http://localhost:3000
- 볼륨 마운트: `uploads/`, `target/`, `translated/`
