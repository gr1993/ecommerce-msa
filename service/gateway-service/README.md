# gateway-service
Gateway 서비스는 프론트엔드 서버가 단일 진입점으로서 모든 요청을 전달할 수 있도록 구성된 서비스이다.  
또한 인증, 인가와 같이 모든 서비스에 공통으로 적용되어야 하는 로직을 중앙에서 처리하여 각 서비스의 부담을 줄인다.

### Health Check vs Circuit Breaker
별도의 LoadBalancer Health Check 기능을 사용하지 않는 경우, API Gateway는 기본적으로 Eureka의  
상태 정보에 의존한다. Eureka는 인스턴스가 하트비트를 전송하지 않거나 Actuator Health 상태가 DOWN일  
경우 해당 인스턴스를 서비스 목록에서 제외한다. 그러나 이 방식은 장애 감지 속도가 느리고, 부분 장애나  
순간적인 장애를 즉시 반영하지 못한다는 한계가 있다.  

이러한 문제를 보완하기 위해 Gateway 자체에서 LoadBalancer 수준의 Health Check를 추가할 수 있다.  
Gateway는 Eureka로부터 전달받은 인스턴스 목록을 기준으로 별도의 Health Check를 수행하고, 사전에  
비정상 인스턴스로의 요청을 차단할 수 있다.  

하지만 실무 환경, 특히 **MSA 환경에서는** 주기적인 Health Check 방식보다 **Circuit Breaker** 기반의  
Health 라우팅 방식이 더 많이 사용된다. 이 방식은 인스턴스의 상태를 사전에 판단하는 대신, 실제 요청 시 실패를  
감지하여 Fallback 처리를 수행하고 Circuit Breaker를 동작시킴으로써 장애 인스턴스로의 요청을 차단한다.  
결과적으로 이는 Health 기반 라우팅과 유사한 효과를 제공하며, Spring Cloud Gateway에서의 Health 기반  
라우팅은 장애를 미리 탐지하는 구조라기보다는 실패를 빠르게 감지하고 우회하는 구조로 활용된다.