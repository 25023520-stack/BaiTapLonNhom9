COMPOSE = docker compose -f docker.yml

.PHONY: up down restart logs ps build clean

up:
	$(COMPOSE) up --build

down:
	$(COMPOSE) down

restart: down up

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

build:
	$(COMPOSE) build

clean:
	$(COMPOSE) down -v
