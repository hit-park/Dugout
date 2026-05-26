.DEFAULT_GOAL := help
.PHONY: help dev stack down api ai ios api-test ai-test ios-build seed-check clean

help:
	@echo "Dugout 개발 명령 — 자세한 워크플로우는 README 참고"
	@echo ""
	@echo "  make dev         ★ stack + api + ai 통합 실행 (Procfile.dev / overmind)"
	@echo ""
	@echo "  make stack       postgres + redis docker 시작 (백그라운드)"
	@echo "  make down        postgres + redis 중지"
	@echo "  make api         dugout-api Spring Boot 시작 (foreground, --profile local)"
	@echo "  make ai          dugout-ai FastAPI 시작 (foreground, port 8001)"
	@echo "  make ios         Xcode 워크스페이스 열기"
	@echo ""
	@echo "  make api-test    dugout-api 컴파일 점검"
	@echo "  make ai-test     dugout-ai pytest"
	@echo "  make ios-build   dugout-ios 빌드 점검 (warnings 0)"
	@echo ""
	@echo "  make seed-check  백엔드 + AI 서비스 health check"
	@echo "  make clean       Gradle / DerivedData / __pycache__ 정리"
	@echo ""
	@echo "일일 워크플로우 (권장):"
	@echo "  T1:  make dev          # api + ai 통합 로그"
	@echo "  T2:  make ios          # 별도 터미널에서 Xcode"
	@echo ""
	@echo "  overmind 보조 명령 (make dev 실행 중인 별도 셸에서):"
	@echo "    overmind connect api    # api 패널 attach (디버거/입력)"
	@echo "    overmind restart api    # api만 재시작 (ai는 유지)"
	@echo "    overmind stop ai        # ai만 정지"
	@echo ""
	@echo "수동 분리 워크플로우 (3 터미널):"
	@echo "  T1:  make stack && make api"
	@echo "  T2:  make ai"
	@echo "  T3:  make ios"

dev: stack
	@command -v overmind >/dev/null 2>&1 || { \
		echo ""; \
		echo "✗ overmind 미설치"; \
		echo "  설치:  brew install overmind tmux"; \
		echo ""; \
		exit 1; \
	}
	@test -d dugout-ai/.venv || { \
		echo ""; \
		echo "✗ dugout-ai/.venv 없음 — Procfile.dev가 .venv/bin/uvicorn을 호출합니다"; \
		echo "  생성:  cd dugout-ai && python -m venv .venv && .venv/bin/pip install -r requirements.txt"; \
		echo ""; \
		exit 1; \
	}
	@echo ""
	@echo "✓ stack ready · starting api + ai via overmind (Ctrl+C로 전체 종료)"
	@echo ""
	overmind start

stack:
	docker compose -f infra/docker-compose.yml up -d
	@echo ""
	@echo "✓ postgres (5432) + redis (6379) up"
	@echo "  로그: docker compose -f infra/docker-compose.yml logs -f"

down:
	docker compose -f infra/docker-compose.yml down

api:
	cd dugout-api && ./gradlew bootRun --args='--spring.profiles.active=local'

ai:
	cd dugout-ai && .venv/bin/uvicorn app.main:app --reload --port 8001

ios:
	open dugout-ios/Dugout.xcworkspace

api-test:
	cd dugout-api && ./gradlew compileKotlin compileTestKotlin --quiet

ai-test:
	cd dugout-ai && .venv/bin/pytest

ios-build:
	cd dugout-ios && xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
		-configuration Debug \
		-destination 'generic/platform=iOS Simulator' \
		-quiet build

seed-check:
	@printf "Backend  (8080): "
	@curl -s -o /dev/null -w 'HTTP %{http_code}\n' http://localhost:8080/api/v1/health || echo "DOWN"
	@printf "AI       (8001): "
	@curl -s -o /dev/null -w 'HTTP %{http_code}\n' http://localhost:8001/docs || echo "DOWN"
	@printf "Postgres (5432): "
	@docker compose -f infra/docker-compose.yml ps --status running --format "{{.Service}}" 2>/dev/null | grep -q postgres && echo "UP" || echo "DOWN"
	@printf "Redis    (6379): "
	@docker compose -f infra/docker-compose.yml ps --status running --format "{{.Service}}" 2>/dev/null | grep -q redis && echo "UP" || echo "DOWN"

clean:
	-cd dugout-api && ./gradlew clean
	-rm -rf ~/Library/Developer/Xcode/DerivedData/Dugout-*
	-find dugout-ai -name "__pycache__" -type d -prune -exec rm -rf {} + 2>/dev/null
	@echo "✓ cleaned"
