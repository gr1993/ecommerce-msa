# config-server
설정 서버(Config Server) 프로젝트는 원래 내부 프로젝트에서만 사용되므로 외부에 공개되지 않는 것이 일반적이다.  
하지만 이번 사이드 프로젝트 특성상, 학습과 실습을 위해 공개 저장소로 구성하였다.  
이로써 누구나 설정 서버의 구조와 설정 파일을 확인하고, MSA 환경에서의 설정 관리 방식을 이해할 수 있다.  


### 기본 명령어
설정 서버를 통해서 설정 파일 내용을 확인할 수 있다.

```shell
# http://localhost:8888/{application}/{profile}[/{label}]
http://localhost:8888/user-service/default
```


### 암호화
설정 파일을 Git에 업로드할 때는 **시크릿 정보에 대해 반드시 Cipher 암호화를 적용**해야 한다.  
이러한 경우, 설정 서버를 통해 암호화를 수행한 후, 암호화된 값을 설정 파일에 반영하여 Git에 업로드해야 한다.

```shell
# 암호화 명령어
curl -X POST http://localhost:8888/encrypt -d 'mySecretPassword'
```