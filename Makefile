.PHONY: all start stop dev build test cluster-info create-topic

REDPANDA := modia-soknadsstatus-redpanda-1

all: start

start: build
	make dev
	sleep 2
	make create-topic
	docker compose build
	docker compose up

stop:
	docker compose down --remove-orphans

dev:
	docker compose up -d activemq redpanda console postgres-arena-infotrygd postgres-soknadsstatus-api postgres-soknadsstatus-hendelse-transform

build:
	./gradlew build

test:
	./gradlew test

cluster-info:
	docker exec -it ${REDPANDA} rpk cluster info

create-topic:
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-arena-infotrygd-oppdatering --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-arena-infotrygd-oppdatering-dlq --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-hendelse --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-hendelse-dlq --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-oppdatering --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-behandling-oppdatering --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-oppdatering-dlq --brokers=localhost:9092
	-docker exec -it ${REDPANDA} rpk topic create personoversikt.modia-soknadsstatus-behandling-oppdatering-dlq --brokers=localhost:9092