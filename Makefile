.PHONY: help up down restart logs logs-backend logs-frontend build clean env-check

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

up: env-check ## Docker Compose 실행 (백엔드 + 프론트엔드)
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
