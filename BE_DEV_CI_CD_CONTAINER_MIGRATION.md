# BizKit-BE DEV CI/CD 컨테이너화 마이그레이션 정리

작성일: 2026-03-01
범위: `18-team-18TEAM-be/.github/workflows/be-dev-ci.yml`, `18-team-18TEAM-be/.github/workflows/be-dev-cd.yml`

## 1) 배경
기존 DEV 백엔드 배포는 **jar 빌드/EC2(단일 인스턴스) 베어메탈 방식**이었고,
`deploy.sh + codedeploy/be/appspec.yml` 기반으로 시스템 데몬(`bizkit-be.service`)에 배포했습니다.

이번 변경으로 `dev` 배포는 **FE DEV와 동일한 컨테이너 패턴**으로 변경했습니다.

- CI는 기존 구조 유지
- CD는 `ECR + Buildah + Podman + CodeDeploy(컨테이너 전용 hooks/appspec)`로 전환

---

## 2) 변경 전 (BE DEV) CD 파이프라인

### `be-dev-cd.yml` (변경 전)
1. `Set Up Java` (`temurin`, `21`)
2. `Set Up Node.js` (`semantic-release`용)
3. `semantic-release` 실행
4. `gradle.properties`에서 버전 읽기
5. `./gradlew clean bootJar -x test`
6. AWS Credential 설정
7. `build/libs/*.jar` 업로드 to S3 (`releases/be/<version>/bizkit-be-<version>.jar`)
8. CodeDeploy 번들 생성
   - `codedeploy/be/appspec.yml`
   - hooks: `codedeploy/be/hooks/{start.sh,stop.sh,validate.sh}`
   - `deploy.sh` 포함
   - `bundle/codedeploy/be/env.sh` 에 `RELEASE_ID`, `ARCHIVE_URL`
9. 번들 업로드 후 CodeDeploy 생성/대기

### 동작 방식 요약
- 인스턴스에서 `deploy.sh`가 JAR를 받아 시스템 서비스(`bizkit-be.service`)를 재시작
- 롤백/검증/정리는 기존 서비스 유닛 중심

---

## 3) 변경 후 (BE DEV) CD 파이프라인

### `be-dev-cd.yml` (현재)
1. `Node.js` 환경 구성 (Java setup 단계 제거)
2. `semantic-release` 실행
3. 버전 추출
4. AWS Credential 설정
5. `Buildah` 설치
6. ECR 로그인 + 레포트리 보장
   - `ECR_REPOSITORY_BE_DEV` 미지정 시 `bizkit-be-dev` fallback
7. 컨테이너 이미지 빌드/푸시
   - `buildah bud -f Containerfile -t <ECR_URI>:<tag>`
   - `tag` 예시: `dev-${VERSION}-${GITHUB_SHA::7}`
8. CodeDeploy 번들 생성 (컨테이너 전용)
   - `deploy/appspec-container.yml`
   - hooks: `deploy/{start-container.sh,stop-container.sh,validate-container.sh}`
   - `deploy/deploy-be-container.sh`
   - `bundle/deploy/env.sh`에 `RELEASE_ID`, `APP_STAGE`, `IMAGE_URI`, `HEALTH_URL`
9. 번들 업로드 후 CodeDeploy 생성/대기

### 동작 방식 요약
- CodeDeploy 훅에서 **이미지 기반 컨테이너 실행**
- `deploy-be-container.sh`가 `podman`로 컨테이너 생성/시작
- `podman generate systemd --user`로 `container-<name>.service` 관리
- 헬스체크(`/actuator/health`) 실패 시 이전 이미지 롤백 시도

---

## 4) 변경 전/후 한눈에 비교

| 항목 | 변경 전 (Before) | 변경 후 (After) |
|---|---|---|
| 배포 단위 | `jar` | 컨테이너 이미지 |
| 빌드 도구 | Gradle(`bootJar`) | `buildah bud` + `Containerfile` |
| 배포 아티팩트 | S3 업로드 `*.jar` | ECR 푸시 이미지 URI |
| 앱스펙 | `deploy/appspec.yml` + `codedeploy/be/appspec.yml` | `deploy/appspec-container.yml` |
| 시작 스크립트 | `codedeploy/be/hooks/start.sh` -> `deploy.sh` | `deploy/start-container.sh` -> `deploy/deploy-be-container.sh` |
| 정지/검증 | `systemd` 직접 제어 | `podman` + 사용자 systemd 유닛 제어 |
| 롤백 | 이전 릴리즈 JAR로 symlink/서비스 재시작 | 이전 이미지 pull 후 컨테이너 재기동 |
| 런타임 종속성 | Java runtime만으로 실행 | Podman runtime 필요 |
| 베어메탈 직접 실행 | O | N |

---

## 5) CI/CD 변경 여부

### CI (`be-dev-ci.yml`)
- 현재 구조는 **기존과 동일**
- 변경 범위는 **CD(dev)만** 컨테이너화 적용

---

## 6) 변경된 파일 목록

- `18-team-18TEAM-be/.github/workflows/be-dev-cd.yml`
- `18-team-18TEAM-be/deploy/appspec-container.yml` (신규)
- `18-team-18TEAM-be/deploy/start-container.sh` (신규)
- `18-team-18TEAM-be/deploy/deploy-be-container.sh` (신규)
- `18-team-18TEAM-be/deploy/stop-container.sh` (신규)
- `18-team-18TEAM-be/deploy/validate-container.sh` (신규)

기존 파일은 유지:
- `18-team-18TEAM-be/deploy.sh`
- `18-team-18TEAM-be/codedeploy/be/appspec.yml`
- `18-team-18TEAM-be/codedeploy/be/hooks/*`

---

## 7) 운영 포인트 (중요)

1. 대상 인스턴스에 Podman + systemd 사용자 유닛 환경(`runuser`, `XDG_RUNTIME_DIR`, `linger`) 준비가 필요
2. 기존 legacy 서비스 중복 정리를 위해 기본적으로 `bizkit-be.service`, `bizkit-backend.service`를 정리 대상에 포함
3. 시크릿 정합성 확인
   - `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_BUCKET`, `S3_PREFIX`
   - `ECR_REPOSITORY_BE_DEV` (없으면 `bizkit-be-dev` fallback)
   - `CODEDEPLOY_APP_BE_DEV`, `CODEDEPLOY_DG_BE_DEV`
4. 배포 실패 시 로그 확인 포인트
   - `deploy-be-container.sh`의 `podman logs`
   - 롤백 상태와 이전 이미지 pull 성공 여부

---

## 8) 실행상태 판단 요약

- **BE DEV CD**: 컨테이너화 완료
- **BE DEV CI**: 기존 유지
- **BE PROD CD**: 기존 요청에 따라 컨테이너화 진행됨(현재 파일 기준)
