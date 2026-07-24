# AI 분석 요청 시스템 구현 계획 (동기 방식)

## 개요
사용자 프로필 정보가 업데이트되면 AI 서버로 분석 요청을 보내고, `User.description`을 업데이트하는 시스템

## 핵심 요구사항
- **트리거**:
  - User의 company, department, position 변경 시
  - Project 변경 시 (생성/수정/삭제)
  - Activity 변경 시 (생성/수정/삭제)
- **Batching**: 여러 번 수정해도 5분 단위로 모아서 처리
- **결과 저장**: AI 응답의 `introduction` → `User.description`
- **처리 방식**: 동기 API 사용 (1회 호출로 결과 즉시 반환)

---

## AI 서버 API (동기)

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 분석 요청 (동기) | POST | `/ai/job/analyze/sync` | 즉시 결과 반환 |

### Request
```json
{
  "user_id": 123,
  "name": "홍길동",
  "company": "카카오",
  "department": "AI워크플로우",
  "position": "Backend Engineer",
  "projects": [
    {"name": "정산 시스템", "content": "정합성/배치 설계", "period_months": 6}
  ],
  "awards": [
    {"name": "사내 해커톤 우수상", "year": 2025}
  ]
}
```

### Response
```json
{
  "message": "ok",
  "data": {
    "introduction": "안녕하세요, 저는 카카오 AI워크플로우에서...",
    "search_confidence": 0.78
  }
}
```

---

## 패키지 구조

```
src/main/java/com/caro/bizkit/
└── domain/ai/
    ├── config/
    │   └── AiClientProperties.java         # AI 서버 URL 설정
    ├── client/
    │   └── AiAnalysisClient.java           # AI 서버 API 호출
    ├── dto/
    │   ├── AiJobAnalyzeRequest.java        # 분석 요청 DTO
    │   └── AiJobAnalyzeResponse.java       # 분석 응답 DTO
    ├── entity/
    │   ├── AiAnalysisTask.java             # 분석 작업 Entity
    │   └── AiAnalysisStatus.java           # 상태 Enum
    ├── repository/
    │   └── AiAnalysisTaskRepository.java
    ├── event/
    │   ├── UserProfileUpdatedEvent.java    # 이벤트 객체
    │   └── UserProfileUpdateListener.java  # 이벤트 리스너
    └── service/
        └── AiAnalysisService.java          # 배치 + 비즈니스 로직
```

---

## 신규 생성 파일 (9개)

### 1. 설정
| 파일 | 설명 |
|-----|------|
| `AiClientProperties.java` | AI 서버 URL, timeout 설정 |

### 2. DTO (2개)
| 파일 | 설명 |
|-----|------|
| `AiJobAnalyzeRequest.java` | 분석 요청 본문 |
| `AiJobAnalyzeResponse.java` | 분석 응답 (introduction 포함) |

### 3. Entity (2개)
| 파일 | 설명 |
|-----|------|
| `AiAnalysisTask.java` | 분석 작업 이력 관리 (BaseTimeEntity 상속: createdAt, updatedAt) |
| `AiAnalysisStatus.java` | 상태 Enum (PENDING, COMPLETED, FAILED) |

### 4. Repository
| 파일 | 설명 |
|-----|------|
| `AiAnalysisTaskRepository.java` | Task 조회/저장 |

### 5. Event (2개)
| 파일 | 설명 |
|-----|------|
| `UserProfileUpdatedEvent.java` | 프로필 업데이트 이벤트 |
| `UserProfileUpdateListener.java` | 이벤트 수신 → 배치 큐 추가 |

### 6. Client
| 파일 | 설명 |
|-----|------|
| `AiAnalysisClient.java` | AI 서버 동기 API 호출 (WebClient) |

### 7. Service
| 파일 | 설명 |
|-----|------|
| `AiAnalysisService.java` | 배치 수집, AI 요청, 결과 처리 |

---

## 수정 파일 (4개)

| 파일 | 수정 내용 |
|-----|---------|
| `UserService.java` | company, department, position 변경 시 이벤트 발행 |
| `ProjectService.java` | 생성/수정/삭제 시 이벤트 발행 |
| `ActivityService.java` | 생성/수정/삭제 시 이벤트 발행 |
| `application.yml` | AI 서버 설정 추가 |

---

## 처리 흐름

```
[User company/department/position 변경]
[Project 생성/수정/삭제]
[Activity 생성/수정/삭제]
        ↓
[이벤트 발행: UserProfileUpdatedEvent]
        ↓
[Listener → ConcurrentHashMap에 userId 저장]
        ↓
[5분 대기 (Batching)]
        ↓
[@Scheduled(fixedDelay=300000) processBatch()]
  - ConcurrentHashMap에서 userId 목록 추출 및 클리어
  - 각 userId에 대해 순차 처리:
    1. User, Project, Activity 정보 조회
    2. AiAnalysisTask 생성 (PENDING)
    3. POST /ai/job/analyze/sync → 즉시 결과 반환
    4. User.description 업데이트
    5. Task.status = COMPLETED
```

---

## 주요 클래스 상세

### AiAnalysisService.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final ConcurrentHashMap<Integer, LocalDateTime> pendingUsers = new ConcurrentHashMap<>();
    private final AiAnalysisClient aiClient;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ActivityRepository activityRepository;
    private final AiAnalysisTaskRepository taskRepository;

    // 배치 큐에 추가
    public void addToBatch(Integer userId) {
        pendingUsers.put(userId, LocalDateTime.now());
        log.info("Adding user to batch: {}", userId);
    }

    // 5분마다 배치 처리
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void processBatch() {
        Set<Integer> userIds = new HashSet<>(pendingUsers.keySet());
        pendingUsers.clear();

        if (userIds.isEmpty()) return;

        log.info("Processing batch: {} users", userIds.size());

        for (Integer userId : userIds) {
            processUser(userId);
        }
    }

    private void processUser(Integer userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            List<Project> projects = projectRepository.findByUserId(userId);
            List<Activity> activities = activityRepository.findByUserId(userId);

            // Task 생성
            AiAnalysisTask task = AiAnalysisTask.create(user);
            taskRepository.save(task);

            // AI 서버 호출 (동기)
            AiJobAnalyzeRequest request = buildRequest(user, projects, activities);
            AiJobAnalyzeResponse response = aiClient.analyzeSync(request);

            // 결과 저장
            user.updateDescription(response.data().introduction());
            task.complete();

            log.info("AI analysis completed for user: {}", userId);
        } catch (Exception e) {
            log.error("AI analysis failed for user: {}", userId, e);
        }
    }
}
```

### AiAnalysisClient.java
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisClient {

    private final WebClient.Builder webClientBuilder;
    private final AiClientProperties properties;

    public AiJobAnalyzeResponse analyzeSync(AiJobAnalyzeRequest request) {
        return webClientBuilder.build()
            .post()
            .uri(properties.getBaseUrl() + "/ai/job/analyze/sync")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AiJobAnalyzeResponse.class)
            .block(Duration.ofSeconds(properties.getTimeoutSeconds()));
    }
}
```

### AiAnalysisTask.java (Entity)
```java
@Entity
@Table(name = "ai_analysis_task")
public class AiAnalysisTask extends BaseTimeEntity {

    // BaseTimeEntity 상속: createdAt(생성일), updatedAt(수정일=완료일)

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisStatus status;  // PENDING, COMPLETED, FAILED

    public static AiAnalysisTask create(User user) {
        AiAnalysisTask task = new AiAnalysisTask();
        task.user = user;
        task.status = AiAnalysisStatus.PENDING;
        return task;
    }

    public void complete() {
        this.status = AiAnalysisStatus.COMPLETED;
        // updatedAt 자동 갱신됨
    }

    public void fail() {
        this.status = AiAnalysisStatus.FAILED;
    }
}
```

### UserService.java 수정 부분
```java
@Transactional
public UserResponse updateMyStatus(UserPrincipal principal, Map<String, Object> request) {
    User user = findByPrincipal(principal);

    // 변경 전 값 저장
    String oldCompany = user.getCompany();
    String oldDepartment = user.getDepartment();
    String oldPosition = user.getPosition();

    // 기존 업데이트 로직
    applyUpdates(user, request);

    // company, department, position 중 하나라도 변경되면 이벤트 발행
    if (!Objects.equals(oldCompany, user.getCompany()) ||
        !Objects.equals(oldDepartment, user.getDepartment()) ||
        !Objects.equals(oldPosition, user.getPosition())) {

        eventPublisher.publishEvent(new UserProfileUpdatedEvent(
            user.getId(), "USER", LocalDateTime.now()
        ));
    }

    return toResponse(user);
}
```

---

## 데이터 매핑

| 소스 | AI 요청 필드 |
|-----|-------------|
| User.id | user_id |
| User.name | name |
| User.company | company |
| User.department | department |
| User.position | position |
| Project.name | projects[].name |
| Project.description | projects[].content |
| (계산) | projects[].period_months |
| Activity.name | awards[].name |
| Activity.startDate.year | awards[].year |

---

## application.yml 추가

```yaml
ai:
  client:
    base-url: ${AI_SERVER_URL:http://localhost:8000}
    timeout-seconds: 60
```

---

## 에러 처리

| 시나리오 | 처리 |
|---------|------|
| AI 서버 연결 실패 | 로그 기록, Task.status = FAILED |
| AI 서버 타임아웃 | 로그 기록, Task.status = FAILED |
| 결과 파싱 실패 | 로그 기록, Task.status = FAILED |

---

## 성능 고려사항

### 배치 주기 5분 선택 근거

| 요소 | 짧은 주기 (1-2분) | 5분 | 긴 주기 (10분+) |
|-----|-----------------|-----|----------------|
| 사용자 경험 | 빠른 피드백 | 적절한 대기 | 오래 기다림 |
| 중복 호출 | 많음 | 적절히 줄임 | 적음 |
| AI 서버 부하 | 높음 | 적절 | 낮음 |

**5분 선택 이유:**
1. **사용자 수정 패턴**: 프로필 수정은 보통 한 세션에서 여러 필드를 연속으로 수정하며, 대부분 2~5분 내에 완료
2. **적절한 피드백 시간**: 사용자가 수정 후 결과를 너무 오래 기다리지 않음
3. **효과적인 Batching**: 연속 수정을 충분히 모아서 중복 호출 방지
4. **AI 서버 부하 감소**: 호출 횟수를 적절히 줄여 서버 부하 관리

> 실제 운영 후 사용자 행동 데이터를 기반으로 조정 권장

### 기타 고려사항

- **fixedDelay 사용**: 이전 작업 완료 후 5분 대기, 배치 겹침 방지
- **동기 API 응답 시간**: 약 15초 예상
- **200명 기준**:
  - 일반적: 5분당 5~15명 → 75~225초 (1~4분)
  - 최악: 200명 동시 → 약 50분 소요 가능

---

## 향후 확장 (Kafka 도입 시)

```
현재: [이벤트] → [Listener] → [Map] → [@Scheduled 동기]
확장: [이벤트] → [Kafka Producer] → [Topic] → [Consumer 비동기]
```

- 프로젝트에 이미 Kafka 의존성 있음 (`spring-kafka`)
- 규모 확장 시 Consumer 병렬 처리로 전환 가능

---

## 구현 순서

### 1단계: 기반 설정
| 파일 | 설명 |
|-----|------|
| `AiClientProperties.java` | AI 서버 URL, timeout 설정 |
| `application.yml` | AI 설정 추가 |

### 2단계: DTO 생성
| 파일 | 설명 |
|-----|------|
| `AiJobAnalyzeRequest.java` | 요청 DTO |
| `AiJobAnalyzeResponse.java` | 응답 DTO |

### 3단계: Entity & Repository
| 파일 | 설명 |
|-----|------|
| `AiAnalysisStatus.java` | 상태 Enum |
| `AiAnalysisTask.java` | Entity |
| `AiAnalysisTaskRepository.java` | Repository |

### 4단계: AI 서버 통신
| 파일 | 설명 |
|-----|------|
| `AiAnalysisClient.java` | WebClient로 AI 서버 호출 |

### 5단계: 이벤트 시스템
| 파일 | 설명 |
|-----|------|
| `UserProfileUpdatedEvent.java` | 이벤트 객체 |
| `UserProfileUpdateListener.java` | 이벤트 리스너 |

### 6단계: 핵심 서비스
| 파일 | 설명 |
|-----|------|
| `AiAnalysisService.java` | 배치 수집, AI 요청, 결과 처리 |

### 7단계: 기존 서비스 수정
| 파일 | 설명 |
|-----|------|
| `UserService.java` | company/department/position 변경 시 이벤트 발행 |
| `ProjectService.java` | 생성/수정/삭제 시 이벤트 발행 |
| `ActivityService.java` | 생성/수정/삭제 시 이벤트 발행 |

### 8단계: 테스트
- 로그에서 "Adding user to batch" 확인
- 5분 후 "Processing batch" 로그 확인
- `ai_analysis_task` 테이블에 COMPLETED 레코드 확인
- `User.description` 업데이트 확인

---

## 검증 방법

1. User의 company/department/position 변경 또는 Project/Activity 생성/수정/삭제
2. 로그에서 "Adding user to batch: {userId}" 확인
3. 5분 후 로그에서 "Processing batch: N users" 확인
4. `ai_analysis_task` 테이블에 COMPLETED 레코드 확인
5. `User.description` 업데이트 확인
