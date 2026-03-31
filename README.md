# Bizkit Backend

명함 기반 네트워킹 서비스 **Bizkit**의 백엔드 서버입니다.
명함 생성·수집·OCR 등록, 실시간 채팅, AI 이미지 생성 등의 기능을 제공합니다.

---

## 목차

1. [기술 스택](#기술-스택)
2. [프로젝트 구조](#프로젝트-구조)
3. [도메인 목록](#도메인-목록)
4. [실행 방법](#실행-방법)
5. [환경변수](#환경변수)
6. [API 문서](#api-문서)
7. [설계 문서](#설계-문서)

---

## 기술 스택

### Core
| 항목 | 버전 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.9 |
| Gradle | - |

### 데이터
| 항목 | 설명 |
|---|---|
| MySQL 8.0 | 메인 데이터베이스 |
| Redis | 캐시 / 세션 저장 |
| Spring Data JPA | ORM |
| Kafka | 비동기 이벤트 처리 |

### 인증 / 보안
| 항목 | 설명 |
|---|---|
| Spring Security | 인증·인가 처리 |
| Kakao OAuth | 소셜 로그인 |
| JWT (jjwt 0.12.5) | Access / Refresh Token |

### 외부 서비스
| 항목 | 설명 |
|---|---|
| AWS S3 (SDK v2) | 이미지 저장 (명함, QR, AI, OCR) |
| AI Server | 명함 이미지 생성 (WebFlux 연동) |

### 실시간 / 비동기
| 항목 | 설명 |
|---|---|
| WebSocket / STOMP | 실시간 채팅 |
| Spring Kafka | Kafka Streams |
| Spring Batch | 배치 처리 |

### 모니터링 / 문서화
| 항목 | 설명 |
|---|---|
| Swagger (springdoc 2.8.6) | API 문서 자동화 |
| Prometheus / Micrometer | 메트릭 수집 |
| Spring Actuator | 헬스 체크 |

---

## 프로젝트 구조

```
src/main/java/com/caro/bizkit/
├── common/
│   ├── config/          # OpenAPI, Redis, Observability 설정
│   ├── exception/       # CustomException, GlobalExceptionHandler
│   ├── security/        # Spring Security 설정
│   ├── S3/              # S3 업로드 서비스 및 카테고리 관리
│   ├── entity/          # 공통 JPA BaseEntity (Auditing)
│   ├── monitoring/      # 메트릭 관련
│   └── ApiResponse.java # 표준 응답 형식
└── domain/
    ├── auth/            # 인증 (Kakao OAuth, JWT)
    ├── user/            # 사용자 계정
    ├── userdetail/      # 사용자 상세 정보
    ├── card/            # 명함 (생성, 수집, OCR)
    ├── chat/            # 실시간 채팅
    ├── review/          # 리뷰
    ├── ai/              # AI 서버 연동
    └── withdrawl/       # 회원 탈퇴
```

---

## 도메인 목록

| 도메인 | 주요 기능 |
|---|---|
| **auth** | Kakao OAuth 로그인, JWT 발급/재발급, 회원가입 |
| **user** | 프로필 조회/수정 |
| **userdetail** | 사용자 상세 정보 관리 |
| **card** | 명함 생성·수정·삭제, 지갑(수집), OCR 명함 등록 |
| **chat** | WebSocket 기반 실시간 채팅 |
| **review** | 명함 보유자에 대한 리뷰 작성·조회 |
| **ai** | AI 명함 이미지 생성 요청 처리 |
| **withdrawl** | 회원 탈퇴 처리 |

---

## 실행 방법

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- 환경변수 설정 (아래 [환경변수](#환경변수) 섹션 참고)

### 로컬 실행

```bash
# 1. MySQL + Redis 컨테이너 시작
docker-compose up -d

# 2. dev 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 빌드

```bash
./gradlew build
java -jar build/libs/*.jar
```

### Docker Compose 서비스

| 서비스 | 포트 | 비고 |
|---|---|---|
| MySQL 8.0 | 3306 | 문자 인코딩: utf8mb4 |
| Redis | 6379 | - |

---

## 환경변수

### 데이터베이스

| 변수 | 설명 |
|---|---|
| `SPRING_DATASOURCE_URL` | MySQL 연결 주소 |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | DDL 전략 (create / update / validate) |

### Redis

| 변수 | 설명 |
|---|---|
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PASSWORD` | Redis 비밀번호 |

### 인증

| 변수 | 설명 | 기본값 |
|---|---|---|
| `KAKAO_CLIENT_ID` | 카카오 앱 ID | - |
| `KAKAO_CLIENT_SECRET` | 카카오 앱 시크릿 | - |
| `KAKAO_REDIRECT_URI` | OAuth 콜백 URL | - |
| `KAKAO_ADMIN_KEY` | 카카오 관리자 키 | - |
| `JWT_SECRET` | JWT 서명 키 | - |
| `JWT_ACCESS_TOKEN_VALIDITY_SECONDS` | Access Token 유효기간 | 1800 (30분) |
| `JWT_REFRESH_TOKEN_VALIDITY_SECONDS` | Refresh Token 유효기간 | 1209600 (14일) |

### AWS S3

| 변수 | 설명 | 기본값 |
|---|---|---|
| `AWS_S3_BUCKET` | S3 버킷명 | - |
| `AWS_REGION` | AWS 리전 | - |
| `AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS` | Presigned URL 만료 시간 | 300 (5분) |

### AI 서버

| 변수 | 설명 | 기본값 |
|---|---|---|
| `AI_SERVER_URL` | AI 서버 주소 | http://localhost:8000 |
| `AI_TIMEOUT_SECONDS` | 요청 타임아웃 | 60 |

### 기타

| 변수 | 설명 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 (dev / prod) |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 |
| `COOKIE_SECURE` | 쿠키 Secure 속성 (기본: true) |
| `COOKIE_SAME_SITE` | SameSite 속성 (기본: None) |

---

## API 문서

서버 실행 후 아래 URL에서 Swagger UI를 통해 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

---

