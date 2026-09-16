# LOCAL FIT

공공데이터 기반 수도권 지역 분석·추천 플랫폼

📎 **상세 설계 결정, 트러블슈팅 전체, 데이터 상세는 노션에서 확인할 수 있습니다.**
👉 [노션 문서 링크](https://app.notion.com/p/localfit-3bf73803dafa808d9bace273983d5f6c?source=copy_link)

---

## 프로젝트 소개

"어디 살지"를 정할 때 사람마다 중요하게 보는 게 다릅니다. 누군가는 월세가 얼마인지가 가장 중요하고, 누군가는 지하철역이 가까운지, 또 누군가는 큰 병원이 있는지를 먼저 봅니다.

LOCAL FIT은 **사용자가 직접 지표별 중요도를 정하면, 그 기준으로 관심지역을 계층적으로 평가**해주는 서비스입니다. 국토교통부, 행정안전부, 국가철도공단, 건강보험심사평가원의 공공데이터를 수집·가공해 점수를 산출합니다.

**기간**: 2026.08 ~ (진행 중) · **인원**: 1명 (개인 프로젝트, 백엔드/프론트엔드 모두 구현)

---

## 기술 스택

| 구분         | 기술                             |
| ------------ | -------------------------------- |
| **Backend**  |                                  |
| Language     | Java 21                          |
| Framework    | Spring Boot 4.1                  |
| DB           | MySQL 8.4                        |
| Cache        | Redis                            |
| ORM          | Spring Data JPA / Hibernate      |
| Security     | Spring Security + JWT            |
| Test         | JUnit 5 / Mockito / AssertJ / H2 |
| API Docs     | springdoc-openapi (Swagger UI)   |
| Build        | Gradle                           |
| **Frontend** |                                  |
| Library      | React 19                         |
| Build Tool   | Vite                             |
| Styling      | Tailwind CSS 4                   |
| Routing      | React Router                     |
| HTTP         | Axios                            |
| **공통**     |                                  |
| Infra        | Docker                           |
| IDE          | IntelliJ IDEA / VS Code          |
| 형상관리     | Git / GitHub                     |

---

## 활용한 공공데이터

| 데이터                             | 제공기관           | 형식 | 용도                      |
| ---------------------------------- | ------------------ | ---- | ------------------------- |
| 행정표준코드\_법정동코드           | 행정안전부         | JSON | Region 마스터 데이터 구축 |
| 아파트 전월세 실거래가             | 국토교통부         | XML  | 지역별 주거비 통계/점수   |
| 전국도시철도역사정보표준데이터     | 국가철도공단       | XLSX | 교통 접근성 통계/점수     |
| 건강보험심사평가원\_병원정보서비스 | 건강보험심사평가원 | JSON | 의료 접근성 통계/점수     |

**수집 결과**: 법정동 약 2,900건 · 전월세 실거래 약 22만 건 · 지하철역 약 650건 · 병원 약 750건

> 각 데이터의 요청 주소, 수집 방식, 갱신 주기 등 상세 내용은 노션 참고

---

## 아키텍처

외부 API를 사용자 요청 시점에 직접 호출하지 않고, **수집 계층과 서비스 계층을 분리**했습니다. 공공데이터는 미리 수집·저장하고, 사용자 요청은 항상 내부 DB만 조회합니다.

```
[공공데이터 API]
       ↓  (개발용 수동 트리거 / 향후 배치)
   MySQL 저장
       ↓
   통계 집계 (JPQL 집계 쿼리)
       ↓
   계층별 순위 계산 → 가중합산
   (시군구 / 시도 / 수도권)
       ↓
   REST API  →  클라이언트
```

### 패키지 구조

```
localfit
├── backend
│   └── src/main/java/com/localfit
│       ├── domain
│       │   ├── region/          법정동 마스터 (행안부)
│       │   ├── rent/            전월세 실거래가 (국토부)
│       │   ├── subway/          지하철역 (국가철도공단)
│       │   ├── hospital/        병원 정보 (심평원)
│       │   ├── recommendation/  계층별 가중합산 점수 계산
│       │   └── user/            회원 / 인증 / 관심지역
│       └── global
│           ├── common/          ApiResponse, BaseEntity
│           ├── config/          Redis, RestClient, JPA Auditing, Swagger
│           ├── exception/       전역 예외 처리
│           └── security/        Spring Security, JWT
└── frontend
    └── src
        ├── api/                 백엔드 API 호출 함수
        ├── components/          공통 컴포넌트
        └── pages/               화면 단위 컴포넌트
```

---

## 핵심 기능

**계층별 가중합산 추천**
관심지역을 시군구 / 시도 / 수도권 3계층에서 각각 독립 평가합니다. 지역끼리 직접 경쟁시키지 않아, 관심지역 목록이 바뀌어도 개별 지역의 점수는 변하지 않습니다.

**정밀도가 다른 지표의 계층 통일**
주거비(동 단위)와 지하철·병원(시군구 단위)은 원본 데이터의 정밀도가 다릅니다. 시군구 계층에서는 시도 값을 상속하되 `inherited` 플래그로 그 사실을 명시해, 응답 구조는 통일하면서도 데이터의 한계를 숨기지 않습니다.

**등급 기반 가중치**
숫자 가중치 대신 4단계 등급(매우중요~상관없음)을 선택하면 서버가 비율로 환산합니다. 지표 추가 시에도 가중치 계산·점수 합산 로직은 수정하지 않도록 `enum` 기반으로 설계했습니다.

**JWT 인증 (Refresh Token Rotation)**
재발급마다 토큰을 교체하고, 이전 토큰 재사용이 감지되면 세션 전체를 폐기합니다.

> 설계 배경과 판단 근거는 노션에 정리했습니다.

---

## 트러블슈팅 (일부)

- **배치 처리 55분 → 6분**: 건별 조회/저장을 캐싱·배치 저장으로 전환
- **환승역 중복 저장**: 노선별로 나뉜 원본 데이터를 역명 정규화 + 시군구 기준으로 재설계
- **인천 행정구역 개편 미반영**: 기관마다 다른 데이터 갱신 시점 문제를 주소 키워드 매핑으로 해결
- **쿼리 파라미터 바인딩 실패**: Map 필드가 `@ModelAttribute`에 바인딩되지 않아 가중치가 항상 무시되던 문제

> 전체 트러블슈팅(10건)은 원인 분석과 해결 과정을 포함해 노션에 정리했습니다.

---

## 테스트

| 클래스                            | 레벨           | 검증 대상                                   |
| --------------------------------- | -------------- | ------------------------------------------- |
| `FavoriteRegionScoreServiceTest`  | 단위 (Mockito) | 가중치 환산, 순위 계산, 계층 상속, 가중합산 |
| `HiraRegionNormalizerTest`        | 단위           | 시도/시군구명 정규화 엣지케이스             |
| `SubwayStationNameNormalizerTest` | 단위           | 역명 정규화 엣지케이스                      |
| `RentTransactionRepositoryTest`   | 통합 (H2)      | 집계 쿼리 동작                              |
| `AuthControllerTest`              | API (MockMvc)  | 요청 검증, 예외 처리, 응답 형식             |

---

## 실행 방법

두 가지 방법으로 실행할 수 있습니다.

### 방법 A — Docker로 한 번에 실행 (권장)

**1. 환경변수 설정**

루트에 `.env` 파일을 만듭니다.

```
DB_PASSWORD=원하는_비밀번호
JWT_SECRET=256비트_이상의_임의_문자열
PUBLIC_DATA_SERVICE_KEY=공공데이터포털_발급_인증키
```

**2. 지하철역 데이터 파일 배치**

```
backend/src/main/resources/data/subway_stations.xlsx
```

**3. 실행**

```bash
docker-compose up -d --build
```

MySQL, Redis, 백엔드, 프론트엔드가 한 번에 뜹니다.

- `http://localhost` — 프론트엔드
- `http://localhost:8080/swagger-ui.html` — API 문서

**4. 데이터 수집**

Docker의 MySQL은 빈 상태로 시작하므로, Swagger나 curl로 순서대로 호출합니다.

```
POST /internal/regions/sync     # 1) 법정동 (선행 필수)
POST /internal/rent/sync        # 2) 전월세
POST /internal/subway/sync      # 3) 지하철역
POST /internal/hospital/sync    # 4) 병원
```

**컨테이너 종료/재시작**

```bash
docker-compose stop     # 데이터 유지한 채 정지
docker-compose start    # 재시작
docker-compose down     # 컨테이너 삭제 (볼륨은 유지, 데이터 안 날아감)
```

---

### 방법 B — 로컬에서 직접 실행 (개발용)

Docker 없이 각각 실행합니다. 코드를 수정하며 개발할 때는 이 방법이 더 빠릅니다 (핫 리로드 지원).

**1. MySQL, Redis 준비**

로컬에 설치하거나, DB/캐시만 Docker로 띄울 수 있습니다.

```bash
docker run -d --name localfit-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=비밀번호 -e MYSQL_DATABASE=localfit mysql:8.4
docker run -d --name localfit-redis -p 6379:6379 redis:7-alpine
```

**2. 백엔드 설정**

```bash
cd backend
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

| 항목                                      | 설명                                                 |
| ----------------------------------------- | ---------------------------------------------------- |
| `spring.datasource.username` / `password` | MySQL 계정                                           |
| `jwt.secret`                              | 256비트 이상의 임의 문자열                           |
| `public-data.service-key`                 | [공공데이터포털](https://www.data.go.kr) 발급 인증키 |

지하철역 데이터 파일을 아래 경로에 배치합니다.

```
backend/src/main/resources/data/subway_stations.xlsx
```

**3. 백엔드 실행 및 데이터 수집**

```bash
./gradlew bootRun
```

```
POST /internal/regions/sync
POST /internal/rent/sync
POST /internal/subway/sync
POST /internal/hospital/sync
```

API 문서: `http://localhost:8080/swagger-ui.html`

**4. 프론트엔드 실행**

```bash
cd frontend
npm install
npm run dev
```

`http://localhost:5173` 접속. Vite 프록시가 API 요청을 백엔드(8080)로 전달합니다.

---

## 향후 계획

- 지표 확장 (교육, 보육, 안전 등)
- 월세 비용 산정 개선 (전월세 전환율 적용)
- 배치 자동화 (스케줄러 도입)
- CI/CD 구축
