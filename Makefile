.PHONY: help up down restart logs logs-backend logs-frontend build clean env-check setup-astah

help: ## 사용 가능한 명령어 목록 표시
	@echo "JaToKo - Astah 번역 도구"
	@echo ""
	@echo "사용 가능한 명령어:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36mmake %-15s\033[0m %s\n", $$1, $$2}'

env-check: ## .env 파일 존재 확인
	@if [ ! -f .env ]; then \
		echo "❌ .env 파일이 없습니다."; \
		echo ""; \
		echo "다음 명령어로 .env 파일을 생성하세요:"; \
		echo "  cp .env.example .env"; \
		echo ""; \
		echo "그리고 .env 파일에 실제 API 키를 입력하세요."; \
		exit 1; \
	fi
	@echo "✅ .env 파일 확인 완료"

setup-astah: ## Astah SDK 라이브러리를 backend/libs/에 복사 (ASTAH_PATH 지정 가능)
	@echo "🔍 Astah SDK 설정 중..."
	@mkdir -p backend/libs
	@if [ -n "$(ASTAH_PATH)" ]; then \
		ASTAH_DIR="$(ASTAH_PATH)"; \
	else \
		if [ -d "/Applications/astah professional.app/Contents/Java" ]; then \
			ASTAH_DIR="/Applications/astah professional.app/Contents/Java"; \
		elif [ -d "/Applications/astah professional" ]; then \
			ASTAH_DIR="/Applications/astah professional"; \
		else \
			echo "❌ Astah Professional 설치 경로를 찾을 수 없습니다."; \
			echo ""; \
			echo "다음 중 하나를 시도하세요:"; \
			echo "  1. Astah Professional을 설치하세요"; \
			echo "  2. 경로를 지정하여 실행하세요:"; \
			echo "     make setup-astah ASTAH_PATH=/path/to/astah"; \
			exit 1; \
		fi; \
	fi; \
	echo "📁 Astah SDK 경로: $$ASTAH_DIR"; \
	MISSING_FILES=""; \
	for jar in astah-api.jar astah-professional.jar rlm-1601.jar; do \
		if [ -f "$$ASTAH_DIR/$$jar" ]; then \
			cp "$$ASTAH_DIR/$$jar" backend/libs/; \
			echo "  ✅ $$jar 복사 완료"; \
		else \
			echo "  ❌ $$jar 파일을 찾을 수 없습니다"; \
			MISSING_FILES="$$MISSING_FILES $$jar"; \
		fi; \
	done; \
	if [ -n "$$MISSING_FILES" ]; then \
		echo ""; \
		echo "⚠️  일부 파일을 찾을 수 없습니다:$$MISSING_FILES"; \
		echo "정확한 경로를 지정하여 다시 시도하세요."; \
		exit 1; \
	fi; \
	echo ""; \
	echo "✅ Astah SDK 설정 완료!"

up: env-check setup-astah ## Docker Compose 실행 (백엔드 + 프론트엔드)
	docker compose up -d
	@echo ""
	@echo "✅ 서비스가 시작되었습니다:"
	@echo "  - 백엔드:    http://localhost:8080"
	@echo "  - 프론트엔드: http://localhost:3000"
	@echo ""
	@echo "로그 확인: make logs"

down: ## Docker Compose 종료
	docker compose down
	@echo "✅ 서비스가 종료되었습니다."

restart: down up ## 서비스 재시작

logs: ## 전체 로그 확인 (실시간)
	docker compose logs -f

logs-backend: ## 백엔드 로그만 확인
	docker compose logs -f backend

logs-frontend: ## 프론트엔드 로그만 확인
	docker compose logs -f frontend

build: ## Docker 이미지 재빌드
	docker compose build --no-cache

clean: down ## 컨테이너, 이미지, 볼륨 모두 삭제
	docker compose down -v --rmi all
	@echo "✅ 모든 Docker 리소스가 삭제되었습니다."

ps: ## 실행 중인 컨테이너 확인
	docker compose ps
