# BizKit-BE PROD CI/CD 컨테이너화 마이그레이션 정리

작성일: 2026-03-01
범위:
- `18-team-18TEAM-be/.github/workflows/be-prod-cd.yml`
- `18-team-18TEAM-be/.github/workflows/be-prod-ci.yml`

## 1) 목적
BE PROD 배포(`main`)를 기존 **jar 아티팩트 배포**에서
**`buildah + ECR 이미지 + Podman 컨테이너 실행` 기반 배포로 전환**.

`be-prod-ci.yml`은 유지되고, `be-prod-cd.yml`만 배포 방식이 바뀜.

---

## 2) 변경 전 (PROD) 기준

기존 `be-prod-cd.yml` (jar 방식)은 아래 순서였음.

1. `Java 21` 설치
2. `semantic-release`
3. `gradle.properties`에서 버전 조회
4. `./gradlew clean bootJar -x test`(JAR 생성)
5. AWS Credential 설정
6. `build/libs/*.jar`를 S3 업로드
   - `releases/be/<version>/bizkit-be-<version>.jar`
7. CodeDeploy 번들 생성
   - `codedeploy/be/appspec.yml`
   - `codedeploy/be/hooks/{start,stop,validate}.sh`
   - `deploy.sh`
   - `env.sh`에 `RELEASE_ID`, `ARCHIVE_URL` 전달
8. 번들 S3 업로드 후 CodeDeploy 실행

실제 실행은 배포 서버에서 jar를 받아
`/home/ubuntu/artifact/be`로 배치하고
`bizkit-be.service`(systemd)로 재시작하는 전통 방식.

---

## 3) 변경 후 (PROD) 기준

현재 `be-prod-cd.yml`은 아래 구조로 변경됨.

1. `Node.js 22` 설치 (semantic-release용)
2. `semantic-release` 실행
3. 버전 읽기 (`gradle.properties`)
4. AWS Credential 설정
5. `Buildah` 설치
6. ECR 로그인 + 레포 확인/생성
   - 시크릿: `ECR_REPOSITORY_BE_PROD` (미지정 시 `bizkit-be-prod` 기본값)
7. 컨테이너 이미지 빌드/푸시
   - `buildah bud --layers -f Containerfile -t <ECR_URI>:<tag> .`
   - `buildah push <ECR_URI>:<tag>`
   - 태그 예시: `prod-${VERSION}-${GITHUB_SHA::7}`
8. CodeDeploy 번들 생성(컨테이너용)
   - `deploy/appspec-container.yml`
   - hooks: `deploy/start-container.sh`, `deploy/stop-container.sh`, `deploy/validate-container.sh`
   - 실행 스크립트: `deploy/deploy-be-container.sh`
   - `bundle/deploy/env.sh`에 `RELEASE_ID`, `APP_STAGE`, `IMAGE_URI`, `HEALTH_URL`
9. 번들 업로드 후 CodeDeploy 실행 & 대기
10. 동일한 Discord 알림 단계 유지

실행 서버에서는 컨테이너 기반 구동
- `podman pull/create`
- `podman generate systemd --user`로 사용자 단위 유닛 생성/기동
- 헬스체크(`http://127.0.0.1:8080/actuator/health`) 성공 시 완료
- 실패 시 이전 이미지로 롤백 시도

---

## 4) 파일 변경 비교

### CI
- `18-team-18TEAM-be/.github/workflows/be-prod-ci.yml`
  - 변경 없음(참고용 정리용)
  - 여전히 Gradle bootJar 빌드 기반으로 PR 빌드/검증 수행

### CD
- `18-team-18TEAM-be/.github/workflows/be-prod-cd.yml`
  - 기존 `Set Up Java`, `Gradle 빌드`, S3 JAR 업로드, `codedeploy/be` 앱스펙 번들링 단계 제거
  - `Buildah`, ECR 로그인, 이미지 빌드/푸시, 컨테이너 앱스펙 번들 단계 추가

### 신규 추가(기존 파일 유지)
- `18-team-18TEAM-be/deploy/appspec-container.yml` (신규)
- `18-team-18TEAM-be/deploy/start-container.sh` (신규)
- `18-team-18TEAM-be/deploy/deploy-be-container.sh` (신규)
- `18-team-18TEAM-be/deploy/stop-container.sh` (신규)
- `18-team-18TEAM-be/deploy/validate-container.sh` (신규)

유지된 기존 파일(레거시):
- `18-team-18TEAM-be/deploy.sh`
- `18-team-18TEAM-be/codedeploy/be/appspec.yml`
- `18-team-18TEAM-be/codedeploy/be/hooks/*`

---

## 5) 핵심 동작 차이(한눈 정리)

| 항목 | 기존(PROD jar) | 변경후(PROD container) |
|---|---|---|
| 배포 산출물 | JAR (`*.jar`) | 컨테이너 이미지 (`ECR URI`) |
| 빌드 도구 | Gradle bootJar | `buildah bud` + `Containerfile` |
| 저장소 | S3 업로드 jar | ECR 이미지 푸시 |
| 실행 경로 | `deploy.sh` + `bizkit-be.service` | `deploy-be-container.sh` + 사용자 systemd 유닛 |
| 앱스펙 | `codedeploy/be/appspec.yml` | `deploy/appspec-container.yml` |
| 헬스체크 | app이 시스템 서비스로 올라간 후 확인 | 컨테이너 + 유닛 상태 + HTTP 헬스체크 |
| 롤백 | 이전 JAR symlink/서비스 재기동 | 이전 이미지 pull 후 컨테이너 재기동 |
| 인프라 의존성 | Java 환경 + systemd | Podman + 사용자 systemd, runuser 환경 |
| legacy 정리 | `bizkit-be.service` 중심 | 기본적으로 `bizkit-be.service`, `bizkit-backend.service` 정지/검증 대상 |

---

## 6) 배포 아티팩트 환경변수/시크릿 정합성

필수:
- `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_BUCKET`, `S3_PREFIX`
- `ECR_REPOSITORY_BE_PROD`(선택, 기본값 `bizkit-be-prod`)
- `CODEDEPLOY_APP_BE_PROD`, `CODEDEPLOY_DG_BE_PROD`
- `RELEASE_TOKEN`

선택:
- `DISCORD_WEBHOOK_URL`

---

## 7) 운영 체크리스트

1. 배포 대상 서버에서 사용자 systemd+Podman 실행 환경이 준비되어야 함
   - `runuser`, `podman`, `aws` CLI, linger 사용자 런타임(`runuser` 환경) 존재
2. `/home/ubuntu/.env-be` 경로에 BE 런타임 환경변수 존재 여부 확인
3. 이전 방식(`bizkit-be.service`)이 떠 있을 경우 중단 타이밍 충돌 체크
4. 배포 실패 시 다음 로그 우선 확인
   - `deploy-be-container.sh` 로그
   - `podman logs` 결과
   - CodeDeploy validation 로그

---

## 8) 변경 후 요약

- `BE PROD CD`는 **완전 컨테이너화**되었고, `BE PROD CI`는 기존 빌드/검증 구조는 유지됨
- 기존 레거시 파일은 삭제하지 않고 보존했기 때문에 롤백/대비가 용이함
