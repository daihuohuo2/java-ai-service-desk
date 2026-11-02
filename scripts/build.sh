#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
chmod +x backend/mvnw
(cd backend && ./mvnw clean verify)
(cd frontend && npm ci && npm test && npm run build)
