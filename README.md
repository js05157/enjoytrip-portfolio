# EnjoyTrip

지역별 관광지를 찾고, 여행 일정을 만들고, 다녀온 장소와 여행 이야기를 나누는 **JSP·Servlet MVC 기반 2인 팀 프로젝트**입니다.

- Organization: [enjoytrip-duo](https://github.com/enjoytrip-duo)
- 저장소: [enjoytrip](https://github.com/enjoytrip-duo/enjoytrip)
- 개발팀: `developers` (저장소 Write 권한)
- 개발 이력: 기능별 한글 커밋

## 구현 기능

| 요구사항 | 구현 |
| --- | --- |
| F101~F103 필수 | TourAPI 지역·시군구·유형·키워드 검색, 정렬·페이지 처리, 사진·상세 정보·위치 조회 |
| F107 필수 | 회원 가입·내 정보 조회·수정·탈퇴 |
| F108 필수 | 세션 로그인·POST 로그아웃·이메일 기반 비밀번호 재설정 |
| Redis 확장 | 관광지 목록 응답 10분 캐싱, 조회 조건별 키 분리, Redis 장애 시 원본 조회 |
| F104 추가 | 개인 여행 생성, 기간·예산·일차별 여행지·경비·메모, 드래그 방문 순서 저장·지도 동선 |
| F105 추가 | 여행 사진 업로드, 방문일·장소 유형·위치·설명 저장 및 핫플레이스 지도 |
| F106 심화 | 관광 뉴스 RSS 수집, 중복 제거, MySQL 저장·조회·원문 링크 |
| F109~F110 심화 | 공지사항·공유게시판 등록·조회·수정·삭제 및 작성 권한 검사 |
| F111·F113 | 여행지 좌표·기간별 날씨(최저·최고기온·강수확률), 일출·일몰 한국 시간 표시 |
| F112 | 지역별 여행 사진 갤러리, 관광지 추가 사진·원본·공공누리 유형 표시 |
| F114 | 한국환경공단 충전기별 위치·유형·용량·운영상태·상태 갱신 시각·지도·페이지 처리 |

필수·추가·심화 기능과 선택 아이디어 F111~F114까지 구현했습니다. 충전소 API와 실제 재설정 메일은 각각 개인 인증키/SMTP 설정이 필요하며, 설정 전에는 제공 준비 안내를 표시합니다.

## 개발 환경

- **JDK 17 이상**, 컴파일 대상 Java 17
- **Tomcat 11.0.27 / Jakarta Servlet 6.1** (`jakarta.servlet` 사용)
- Maven **3.9.16** (Wrapper 포함), WAR 패키징
- MySQL **8.0/8.4**, JDBC·HikariCP
- Redis **8.2**, Jedis **6.2.0**
- 지도: Leaflet **1.9.4** + OpenStreetMap (도로 길찾기가 아닌 방문 순서 연결선)

라이브러리 버전은 `pom.xml`로 공유합니다. Eclipse/STS에서는 가져온 뒤 **Maven → Update Project**를 실행하고 JDK 17 이상·Tomcat 11을 연결하세요. Tomcat 9의 `javax.servlet` 예제와 섞지 않습니다.

## 빠른 실행: 개발 모드

개발 모드는 **샘플 관광지와 파일 H2 DB**를 사용합니다. 실제 관광지로 오인하지 않도록 모든 화면에 개발 모드를 표시합니다. 가입한 회원·일정·게시글은 파일 DB에 저장되며 기본 계정/비밀번호는 없습니다. 개발 모드의 비밀번호 재설정 링크는 화면에만 표시하고 메일을 발송하지 않습니다.

```powershell
git clone https://github.com/enjoytrip-duo/enjoytrip.git
cd enjoytrip
docker compose up -d
.\scripts\run-local.ps1
```

- 접속: `http://localhost:8080/enjoytrip/`
- 8080 포트를 이미 사용하면: `.\scripts\run-local.ps1 -Port 18080`
- 중지: 실행 창에서 `Ctrl+C`
- Redis가 없어도 관광지 조회는 동작합니다. Redis 적용 확인에는 실제 Redis 실행이 필요합니다.
- 실행 스크립트는 공식 Tomcat ZIP의 SHA-512를 검증하고 `.local/`에 설치합니다. JDK는 미리 설치해야 합니다.
- 개인 DB와 업로드 사진은 Tomcat의 `CATALINA_BASE/enjoytrip-data/`에 저장됩니다. 파일 DB와 사진은 Git에 올리지 않습니다.
- 일반 빌드: `.\mvnw.cmd clean verify` → `target/enjoytrip.war`. macOS/Linux는 `./mvnw clean verify` 후 별도 Tomcat 11에 배포합니다.

## 실제 TourAPI·MySQL·SMTP 연결

`config/application-local.properties.example`을 **`config/application-local.properties`**로 복사해서 개인 값을 입력합니다. 개인 파일은 Git에서 제외합니다. 인증키는 공공데이터포털의 **Decoding 키**를 사용하며, 프로그램이 한 번 URL 인코딩합니다.

```properties
app.mode=live
app.base-url=http://localhost:8080/enjoytrip
db.url=jdbc:mysql://localhost:33306/enjoytrip?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
db.user=enjoytrip
db.password=개인_비밀번호
tour.service-key=개인_Decoding_인증키
ev.service-key=충전소_API_Decoding_인증키
redis.enabled=true
redis.uri=redis://localhost:16379/0
redis.ttl-seconds=600
app.admin-emails=관리자로_사용할_가입_이메일
smtp.host=개인_SMTP_서버
smtp.port=587
smtp.user=개인_SMTP_계정
smtp.password=개인_SMTP_비밀번호
smtp.from=발신_이메일
smtp.starttls=true
```

- 관광지 지역은 `ldongCode2`의 최신 법정동 시도·시군구 목록을 사용합니다. `lDongRegnCd`, `lDongSignguCd`로 조회하며 폐기 예정 `areaCode2`에 의존하지 않습니다. 코드 목록은 24시간 저장합니다.
- 날씨·일출·일몰은 별도 키 없는 Open-Meteo를 사용합니다. 오늘부터 15일 뒤까지 조회하며 한국 시간으로 표시합니다. 제공 데이터는 CC BY 4.0이며 화면에 출처를 표시합니다.
- 충전소는 [한국환경공단 전기자동차 충전소 정보](https://www.data.go.kr/data/15076352/openapi.do)를 별도로 활용 신청하고 `ev.service-key`에 Decoding 키를 입력하세요. TourAPI 등록과 별개입니다. 제공기관의 마지막 상태와 갱신 시각을 표시하며 도착 시 사용을 보장하지 않습니다. 키가 없을 때 가짜 충전소를 표시하지 않습니다.
- 개인 설정 변경 후 톰캣을 재시작해야 적용됩니다. 인증키·비밀번호는 Git에 커밋하지 않습니다.
- MySQL에 `enjoytrip` DB와 해당 DB만 접근하는 사용자를 먼저 생성하세요. 앱은 `src/main/resources/db/schema.sql`의 테이블을 `CREATE TABLE IF NOT EXISTS`로 준비하며 기존 테이블을 삭제하지 않습니다.
- MySQL도 Docker로 실행하려면 `.env.example`을 `.env`로 복사해 개인 비밀번호를 정하고 `docker compose -f compose.mysql.yaml up -d`를 실행합니다. 호스트 포트는 `33306`입니다. 기존 로컬 MySQL과 충돌하지 않습니다.
- SMTP 설정이 있어야 **실제 메일로** 비밀번호 재설정을 제공할 수 있습니다. live 모드에서는 재설정 링크를 화면/애플리케이션 로그로 노출하지 않습니다. 별도 Tomcat의 access log에도 쿼리 문자열을 남기지 않도록 `%r` 대신 `%m %U %H`를 사용하세요. 로컬 실행 스크립트에는 이 설정을 적용했습니다.
- `app.admin-emails`는 공지 작성·뉴스 수집 관리자 이메일 목록입니다. 쉼표로 구분합니다. 해당 이메일로 회원가입한 사용자가 관리자 메뉴를 사용합니다. 기본값은 비어 있습니다.
- Eclipse/외부 Tomcat에서는 JVM 인수 `-Denjoytrip.config=C:/절대경로/enjoytrip/config/application-local.properties` 또는 환경 변수 `APP_CONFIG`로 파일 위치를 지정합니다. `scripts/run-local.ps1`은 자동 지정합니다.
- 모든 설정은 환경 변수로도 덮어쓸 수 있습니다. 예: `DB_URL`, `DB_PASSWORD`, `TOUR_SERVICE_KEY`, `REDIS_URI`, `APP_MODE`, `APP_ADMIN_EMAILS`.
- 배포 시 HTTPS와 `app.base-url`을 실제 서비스 주소로 설정합니다. HTTPS 주소면 세션 쿠키에 Secure 속성이 적용됩니다.
- 실제 사용자 TourAPI 키와 외부 SMTP 자격 증명은 저장소에 포함하지 않습니다. 기본 실행은 demo 모드입니다.

## Redis 동작 확인

```text
AttractionServlet → CachedAttractionService → Redis GET
  적중: JSON 역직렬화 후 반환
  미적중: TourAPI 조회 → Redis SET EX 600 → 반환
  연결 실패: 300ms 연결 제한, 5초간 캐시 우회 → 원본 조회
```

날씨 응답은 30분, 충전소 응답은 1분간 Redis에 저장하며 외부 API가 실패한 응답은 저장하지 않습니다. 인증키를 포함한 요청 URL은 캐시 키에 노출하지 않고 SHA-256으로 구분합니다.

관광지 목록 키는 모드(`demo/live`)와 지역·시군구·콘텐츠 유형·검색어·페이지·크기·정렬 전체를 포함한 SHA-256으로 구분합니다. 영구 데이터는 MySQL에 저장하며 Redis에는 관광지 조회 결과만 저장합니다.

`/health`의 `redis`, `cache.hits`, `cache.misses`를 확인하세요. 같은 조건으로 두 번 조회했을 때 Redis가 연결되어 있으면 hits가 증가합니다. Redis 장애 시에는 `UNAVAILABLE`로 표시되며 원본 조회를 계속합니다.

## 검증

```powershell
.\mvnw.cmd clean verify
# 실제 Redis의 저장·TTL 만료 테스트도 포함
$env:REDIS_TEST_URI='redis://localhost:16379/15'
.\mvnw.cmd clean verify
# 실행 중인 demo 서버에 임시 회원을 생성·삭제하는 HTTP 검증
python scripts/smoke_test.py http://localhost:8080/enjoytrip
# live/demo 공통 읽기 전용 여행 도구 검증
python scripts/travel_smoke_test.py http://localhost:8080/enjoytrip
```

- JUnit: 관광 API 응답 계약, 검색 조건, 캐시 적중/손상/키 분리, 비밀번호 해시, 회원 DAO, 재설정 토큰 1회성, SMTP 전송, 일정 소유권·정렬, RSS XXE 차단, 이미지 재인코딩·경로 검증
- 실제 Redis 통합 테스트는 `REDIS_TEST_URI`가 있을 때 실행합니다. 다른 키를 삭제하거나 `FLUSHDB`를 실행하지 않습니다.
- HTTP 검증: 한글 JSP, 회원 CRUD·CSRF·로그인, 다른 회원의 일정·게시글 접근 차단, 공지 작성 권한, 사진 업로드/탈퇴 후 정리, 비밀번호 재설정·토큰 재사용 차단
- HTTP 검증은 **demo 서버 전용**이며 `smoke@example.com`, `other-smoke@example.com`을 생성 후 삭제합니다. 관리자 기능까지 검증하려면 테스트 서버에서 `APP_ADMIN_EMAILS=smoke@example.com`을 설정하세요.
- GitHub Actions가 JDK 17·MySQL 8.4·Redis 8.2·Tomcat 11 조합으로 빌드와 HTTP 검증을 실행합니다. 날씨·충전소의 HTTP 시나리오는 로컬 `fixture_api.py` 응답 서버를 사용해 외부 키와 네트워크에 의존하지 않습니다.

## 협업

각자 조직 레포를 clone하고 기능 브랜치에서 작업한 뒤 PR로 `main`에 병합합니다. 포크는 필요하지 않습니다. 예: `feature/attraction-search`, `feature/member-profile`.

커밋 메시지는 `기능: 관광지 검색 추가`, `수정: 로그인 세션 오류 해결`처럼 한글로 작성합니다. 공통 `pom.xml`, DB 스키마를 바꿀 때는 PR에 변경 이유를 남깁니다.

## 제출 문서

- [요구사항 및 구현 범위](docs/requirements.md)
- [Usecase Diagram](docs/usecases.md)
- [Class Diagram 및 데이터 구조](docs/architecture.md)
- [검증 결과](docs/verification.md)
- [실행 화면](docs/screenshots/)

관광 정보: [한국관광공사 TourAPI](https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15101578). 지도: [Leaflet](https://leafletjs.com/), [OpenStreetMap](https://www.openstreetmap.org/copyright). Leaflet 라이선스는 `assets/vendor/leaflet/LICENSE`에 포함합니다. 뉴스는 Google 뉴스 RSS 제목·출처·원문 링크만 저장하며 기사 본문은 복제하지 않습니다.
