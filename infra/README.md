# infra

mysql 및 kafka 등을 도커 컨테이너로 구동할 수 있는 디렉토리이다. docker-compose 명령어를 통해  
쉽게 박신사몰을 운영할 수 있는 환경을 구성할 수 있다.  


### 인프라 구축 명령어

```shell
# 서비스 실행
docker-compose --project-name service up -d
# 재빌드 후 서비스 실행
docker-compose --project-name service up --build -d
# 특정 서비스만 재빌드
docker-compose --project-name service up --build -d user-service

# 저장소 구축
docker-compose -f docker-compose.infra.yml up -d
```


### MongoDB 구축 시
MongoDB에서 트랜잭션을 사용하려면 Replica Set 설정이 필수이다. 아래 명령어는 현재 인스턴스를 Primary로  
초기화하여 mongo-express 등 클라이언트에서 접근할 수 있도록 한다.  

```shell
# MongoDB 실행
docker exec -it mongodb mongosh

# MongoDB Replica Set을 초기화하고, 현재 노드를 Primary로 설정하는 명령어
rs.initiate()
```


### Elasticsearch 구축 시
Elasticsearch에서 nori 분석기를 사용하기 때문에 구동 후 아래 명령어로 분석기를 설치하여야 한다.

```shell
# 도커 컨테이너 접속
docker compose exec -it elasticsearch /bin/bash

# 분석기 설치 명령어(실행 후 제실행)
bin/elasticsearch-plugin install analysis-nori
```


### Kafka 토픽 생성

아래는 Kafka 클러스터가 구축되고 난 후 파티션 수를 지정하기 위해 직접 토픽 생성 명령어를 실행하였다.

```shell
docker exec -it kafka1 kafka-topics --create --topic user.registered --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic product.created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic product.updated --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic category.created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic category.updated --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic category.deleted --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic keyword.created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic keyword.deleted --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic order.created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic order.cancelled --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic payment.confirmed --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic payment.cancelled --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
```