.PHONY: all start stop dev build test cluster-info create-topic

REDPANDA := modia-soknadstatus-redpanda-1

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
	docker exec -it ${REDPANDA} rpk topic create infotrygd-soknadstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create aapen-sob-oppgaveHendelse-v1 --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create pleiepenger-soknadstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create arena-soknadstatus --brokers=localhost:9092
	docker exec -it ${REDPANDA} rpk topic create modia-soknadstatus --brokers=localhost:9092