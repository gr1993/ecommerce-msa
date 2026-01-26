package com.example.catalogservice.config;

import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정
 * - JsonDeserializer를 사용한 자동 역직렬화
 * - 재시도 및 DLQ 처리는 @RetryableTopic 어노테이션에서 처리
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Kafka Consumer Factory 설정
     *
     * 주요 설정 옵션:
     * - buildConsumerProperties(): application.yml의 spring.kafka.consumer 설정을 자동으로 로드
     * - KEY_DESERIALIZER: 메시지 키를 String으로 역직렬화
     * - VALUE_DESERIALIZER: 메시지 값을 JSON에서 Java 객체로 자동 역직렬화
     * - TRUSTED_PACKAGES: 역직렬화를 허용할 패키지 (보안상 명시 필요)
     * - TYPE_MAPPINGS: Kafka 메시지의 타입 헤더와 Java 클래스 매핑 정의
     *
     * @return ConsumerFactory<String, Object>
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // application.yml의 spring.kafka.consumer 설정을 자동으로 Map으로 변환
        // 포함 항목: bootstrap-servers, group-id, auto-offset-reset 등
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));

        // Key Deserializer: 메시지 키를 문자열로 역직렬화
        // Kafka는 기본적으로 key-value 구조를 사용하며, key는 파티셔닝에 사용됨
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value Deserializer: 메시지 본문을 JSON에서 Java 객체로 자동 역직렬화
        // StringDeserializer 대신 JsonDeserializer를 사용하면 수동 파싱 불필요
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Trusted Packages: 역직렬화를 허용할 패키지 명시 (보안 설정)
        // JsonDeserializer는 보안상 신뢰할 수 있는 패키지만 역직렬화하도록 제한
        // "com.example.*"는 모든 com.example 하위 패키지를 신뢰함
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.*");

        // Type Mappings: Kafka 메시지 헤더의 타입 이름과 실제 Java 클래스 매핑
        // Producer가 보낸 메시지의 __TypeId__ 헤더 값을 보고 어떤 클래스로 역직렬화할지 결정
        // 형식: "타입ID:전체클래스명,타입ID2:전체클래스명2"
        // 예: Producer가 __TypeId__="productCreatedEvent"로 보내면 ProductCreatedEvent로 역직렬화
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "productCreatedEvent:" + ProductCreatedEvent.class.getName() + "," +
                "productUpdatedEvent:" + ProductUpdatedEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka Listener Container Factory 설정
     *
     * 역할:
     * - @KafkaListener 어노테이션이 붙은 메서드들의 실행 환경 제공
     * - 메시지 수신, 역직렬화, 메서드 호출, 예외 처리 등을 담당
     *
     * 설정:
     * - 재시도 로직: @RetryableTopic 어노테이션에서 선언적으로 처리
     * - ACK 모드: 기본값 사용 (BATCH - 리스너 메서드 정상 완료 시 자동 커밋)
     * - 동시성: 기본값 사용 (1) - 필요시 setConcurrency()로 조정 가능
     *
     * 참고:
     * - 간결한 설정 유지
     * - 재시도/DLQ는 @RetryableTopic에서 처리하므로 여기서는 설정 불필요
     *
     * @return ConcurrentKafkaListenerContainerFactory<String, Object>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 기본 설정 사용
        // 필요시 아래 옵션들을 추가할 수 있음:
        // factory.setConcurrency(3); // 동시 처리 스레드 수 (파티션 수에 맞게 조정)
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // 메시지별 커밋

        return factory;
    }
}
