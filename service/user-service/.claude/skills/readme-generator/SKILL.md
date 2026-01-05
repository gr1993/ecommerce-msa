---
name: readme-generator
description: Java/Spring 마이크로서비스용 README.md 파일 생성 스킬. README 작성, README 만들어줘, 문서화, 프로젝트 문서 작성 요청 시 자동으로 트리거됨. 프로젝트 구조와 기술 스택을 분석하여 체계적인 README를 생성함.
---

# README Generator

Java/Spring 마이크로서비스의 README.md 파일을 생성하는 스킬.

## 작업 흐름

### 1. 프로젝트 분석

프로젝트 루트에서 다음 파일/디렉토리를 확인:
- `build.gradle` 또는 `pom.xml` - 의존성 및 기술 스택 파악
- `src/main/java/**/` - 패키지 구조 확인
- `src/main/resources/application.yml` - 설정 확인
- 기존 `CLAUDE.md` - 프로젝트 컨텍스트 참고

### 2. README 구조

```
# 서비스명
한 줄 설명


### 프로젝트 패키지 구조
패키지 트리 (코드 블록, 각 패키지에 # 주석으로 역할 설명)


### 백엔드 기술
* 기술1
* 기술2 : 사용 목적 설명
...


### REST API

REST API 명세는 다음 방법으로 확인할 수 있다.

1. 브라우저에서 Swagger UI 열기: `/swagger-ui.html`
2. 정적 문서 확인: [`openapi.json`](./openapi.json)


### Events (Kafka 사용 시)

이벤트 상세 명세는 [`asyncapi.yaml`](./asyncapi.yaml) 파일을 참고하면 된다.

| 구분 | 설명 |
|-----|------|
| 발행(Published) | event.name |
| 구독(Subscribed) | - |
```

### 3. 내용 작성 가이드라인

**서비스명:**
- `# 서비스명` 형식으로 작성
- 바로 아래에 한 줄로 서비스 역할 설명

**패키지 구조:**
- 실제 `src/main/java` 하위 패키지 구조 반영
- 각 패키지 옆에 `# 역할 설명` 주석 추가
- 트리 문자(├── └── │) 사용

**백엔드 기술:**
- `build.gradle` 또는 `pom.xml`에서 주요 의존성 추출
- 버전 명시 (예: Spring Boot 3.5.9)
- 특수 목적 라이브러리는 `: 설명` 추가

**REST API:**
- Swagger UI 경로와 정적 문서 링크 제공

**Events:**
- Kafka 사용 시에만 포함
- 발행/구독 이벤트를 표로 정리
- asyncapi.yaml 링크 제공

## 작성 스타일

- 문체: 한국어 평서형 (~이다)
- 기술 용어는 영문 그대로 사용
- 코드 블록으로 패키지 구조 표현
- 섹션 구분은 `###` 사용

## 예시

예시 README는 [references/example-readme.md](references/example-readme.md) 참조.
