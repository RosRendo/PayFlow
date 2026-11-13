# PayFlow - Start Local PostgreSQL & Redis Infrastructure
Write-Host "Starting PayFlow PostgreSQL 16 & Redis 7 containers..." -ForegroundColor Cyan
docker-compose up -d

Write-Host "Services status:" -ForegroundColor Green
docker-compose ps
