.PHONY: all start stop dev build test cluster-info create-topic

REDPANDA := modia-soknadsstatus-redpanda-1

all: start

start: build dev
	docker compose up -d --build
	make create-topic
	docker compose up

stop:
	docker compose down --remove-orphans

dev:
	docker compose up -d activemq redpanda console postgres

build:
	./gradlew build

test:
	./gradlew test

cluster-info:
	docker exec -it ${REDPANDA} rpk cluster info

create-topic:
	docker exec -it ${REDPANDA} rpk topic create arena-infotrygd-soknadsstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create arena-infotrygd-soknadsstatus-dlq --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create aapen-sob-oppgaveHendelse-v1 --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create aapen-sob-oppgaveHendelse-v1-dlq --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create pleiepenger-soknadsstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create modia-soknadsstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create modia-soknadsstatus-dlq --brokers=localhost:9092