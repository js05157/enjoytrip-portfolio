# 검증 결과

검증일: **2026-10-30 (Asia/Seoul)**

## 실행 결과

| 대상 | 결과 |
| --- | --- |
| Maven `clean verify` / WAR 빌드 | 성공 |
| JUnit 24개 | 실패 0, 오류 0, 스킵 0 (실제 Redis 포함) |
| MySQL 8.0.46 연결·테이블 초기화 | 연결 정상, 앱 테이블 7/7 생성 확인 |
| 실제 TourAPI 목록·키워드·상세·법정동·추가 사진 | 업데이트된 개인 키로 HTTP 200, 정상 코드 확인 |
| 지역·시군구 필터 | 서울 관광지 767건 → 종로구 147건, 브라우저 선택·검색 확인 |
| 실제 Open-Meteo | 7일 날씨·기온·강수확률·일출·일몰 응답과 JSP 표시 확인 |
| 실제 Redis 8.2.11 | 저장·조회·TTL 만료, 관광지 반복 조회 시 캐시 hits 증가 확인 |
| live 서버 `/health` | `database=UP`, `redis=UP`, `demo=false` |
| 회원·일정·게시판·사진·재설정 HTTP 시나리오 | 격리된 demo 서버에서 성공, 임시 회원/업로드 정리 |
| 여행 도구 HTTP 시나리오 | live 서버와 격리된 fixture 서버 모두 성공 |
| 충전소 응답·지도·상태·JSP | 로컬 HTTP 응답 서버로 성공, JSP record 속성 접근 오류 수정 후 재검증 |
| SMTP 전송 | 로컬 SMTP 수신 서버로 MIME 메일 전달 확인 |
| 실제 관광 뉴스 RSS 수집·DB 저장 | 이전 통합 검증에서 HTTP 200 및 새 기사 저장 확인 |
| 한글·사진 갤러리·날씨 화면 | 실제 데이터로 브라우저 시각 검증, 모바일 폭 390px에서 페이지 가로 넘침 없음 |

API 키와 DB 비밀번호는 테스트 출력·커밋에 포함하지 않았습니다. 실제 키를 사용한 외부 조회와 합성 응답을 사용한 계약 검증을 구분했습니다.

## 재현 방법

```powershell
.\mvnw.cmd clean verify
# 실제 Redis의 저장·TTL 만료 테스트도 포함
$env:REDIS_TEST_URI='redis://localhost:16379/15'
.\mvnw.cmd clean verify
# demo 전용: 임시 회원·일정·게시글·사진 생성 후 정리
python scripts/smoke_test.py http://localhost:8080/enjoytrip
# live/demo 공통: 읽기 전용 여행 도구 검증
python scripts/travel_smoke_test.py http://localhost:8080/enjoytrip
```

GitHub Actions는 JDK 17·MySQL 8.4·Redis 8.2·Tomcat 11에서 동일한 빌드와 HTTP 검증을 수행합니다. 날씨·충전소 HTTP 시나리오는 `scripts/fixture_api.py`의 localhost 응답 서버를 사용하며 실제 인증키가 필요하지 않습니다. `--fixtures` 옵션은 해당 응답 서버를 연결한 테스트 환경에서만 사용합니다.

## 개인 설정 후 확인할 항목

- `ev.service-key`가 비어 있어 **한국환경공단 실제 충전소 API 호출은 아직 확인하지 못했습니다.** 연동 코드·오류 처리·캐시·지도·상태 표시는 계약/HTTP 테스트로 검증했습니다. 해당 서비스 활용 신청 후 키를 설정하고 톰캣을 재시작해야 합니다.
- `smtp.host/user/password/from`이 비어 있어 **실제 메일 계정 인증과 수신함 도착은 아직 확인하지 못했습니다.** live 모드에서 재설정 링크를 화면에 노출하지 않습니다.
- 공지 작성·뉴스 수집을 사용할 본인의 가입 이메일을 `app.admin-emails`에 지정해야 합니다. 기본 관리자 계정/비밀번호는 만들지 않았습니다.
- 이 PC의 Docker Desktop 시작 오류로 Compose 기동은 확인하지 못했습니다. 현재 실제 Redis는 공식 이미지의 실행 파일로 별도 실행 중이며 `health`에서 정상 연결을 확인했습니다. 팀원의 일반적인 실행 방법은 README의 Docker Compose 방식입니다.

## 실행 화면

실제 관광지 지역 검색:

![서울 종로구 관광지](screenshots/attractions-live.png)

실제 날씨·일출·일몰:

![기간별 날씨](screenshots/weather.png)

실제 관광 사진:

![여행 사진 갤러리](screenshots/travel-photos.png)

기존 개발 모드의 홈·핫플레이스 화면:

![홈 화면](screenshots/home.png)

![핫플레이스 지도](screenshots/hotplace-map.png)
