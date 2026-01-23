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


### Kafka 토픽 생성

아래는 Kafka 클러스터가 구축되고 난 후 파티션 수를 지정하기 위해 직접 토픽 생성 명령어를 실행하였다.

```shell
docker exec -it kafka1 kafka-topics --create --topic user.registered --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic product.created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic product.updated --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
```