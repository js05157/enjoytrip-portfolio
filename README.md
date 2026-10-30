# EnjoyTrip

공공데이터를 활용한 관광지 조회 및 여행 서비스를 만드는 **JSP·Servlet 기반 2인 팀 프로젝트**입니다.
화면과 서버 코드를 하나의 Maven 웹 프로젝트에서 함께 관리합니다.

## 현재 상태

- Maven 웹 프로젝트 생성 완료 (`war` 패키징)
- 기본 JSP 첫 화면과 `web.xml` 포함
- Maven Wrapper로 Maven 실행 버전 고정
- 관광지 조회, 회원 관리, 로그인 등 서비스 기능은 구현 예정

## 개발 환경

| 항목 | 현재 프로젝트 설정 |
| --- | --- |
| 프로젝트 | `com.enjoytrip:enjoytrip:0.0.1-SNAPSHOT` |
| Java 컴파일 기준 | Java 8 (`pom.xml`의 source/target 값) |
| 웹 설정 | Servlet 4.0 (`web.xml`) |
| Maven | 3.9.16 (Wrapper 사용) |
| 빌드 결과 | `target/enjoytrip.war` |

현재 Java/Servlet 값은 프로젝트 생성 시 만들어진 설정을 유지한 것입니다.
팀원이 개발을 시작하기 전에 **사용할 JDK와 Tomcat 버전을 동일하게 정하고**, 필요한 경우 `pom.xml`과 `web.xml`을 함께 수정하세요.

- Servlet 4.0 기반 `javax.servlet` 코드는 Tomcat 9 계열에 해당합니다.
- Tomcat 10 이상에서는 `jakarta.servlet` 패키지를 사용합니다. Tomcat 11은 Java 17 이상과 Servlet 6.1 기준입니다.
- 현재 초기 템플릿에는 Servlet API 의존성이 없습니다. Servlet 클래스를 추가할 때 선택한 서버에 맞는 API를 `provided` 범위로 추가하세요.
- 개인 IDE 설정과 워크스페이스의 Tomcat 서버 설정은 저장소에 포함하지 않습니다.

참고: [Tomcat 버전별 지원 규격](https://tomcat.apache.org/whichversion.html), [Maven Wrapper 사용법](https://maven.apache.org/tools/wrapper/maven-wrapper-plugin/usage.html)

## 프로젝트 구조

```text
enjoytrip/
├─ .mvn/                  # Maven 공통 실행 설정
├─ mvnw / mvnw.cmd        # Maven 실행 파일
├─ pom.xml                # 라이브러리와 빌드 설정
└─ src/main/
   ├─ java/               # Servlet, Service, DAO, DTO 추가 위치
   ├─ resources/          # 공통 설정 파일 추가 위치
   └─ webapp/
      ├─ index.jsp        # 기본 첫 화면
      └─ WEB-INF/
         └─ web.xml       # 웹 애플리케이션 설정
```

`src/main/java`, `src/main/resources`는 코드를 추가할 때 생성하면 됩니다.
기능을 구현하면서 JSP는 `WEB-INF/views`, CSS·JavaScript·이미지는 `assets` 아래에 정리합니다.

## 시작하기

1. 저장소를 복제합니다.

   ```bash
   git clone https://github.com/js05157/enjoytrip.git
   cd enjoytrip
   ```

2. Eclipse/STS에서 `File → Import → Maven → Existing Maven Projects`를 선택하고 `pom.xml`이 있는 폴더를 불러옵니다.
3. 팀에서 정한 JDK를 프로젝트에 연결하고 Maven 설정을 동기화합니다.
4. 아래 명령으로 빌드합니다. 최초 실행 시 Maven과 의존성을 다운로드하므로 인터넷 연결이 필요합니다.

   Windows PowerShell:

   ```powershell
   .\mvnw.cmd clean verify
   ```

   macOS / Linux:

   ```bash
   ./mvnw clean verify
   ```

5. 선택한 Tomcat에 프로젝트를 추가하거나 `target/enjoytrip.war`를 배포합니다. 컨텍스트 경로가 `enjoytrip`, 포트가 `8080`이면 `http://localhost:8080/enjoytrip/`에 접속합니다.

## 구현할 기능

| 요구사항 | 기능 | 우선순위 |
| --- | --- | --- |
| F101 | 지역별 관광지 정보 수집 및 표시 | 필수 |
| F102 | 관광지·숙박·음식점 조회 | 필수 |
| F103 | 문화시설·공연·여행코스·쇼핑 조회 | 필수 |
| F107 | 회원 가입·조회·수정·탈퇴 | 필수 |
| F108 | 로그인·로그아웃·비밀번호 찾기 | 필수 |
| F104~F105 | 여행 계획·핫플레이스 등록 | 추가 |
| F106 | 관광지 관련 뉴스 수집·저장·조회 | 심화·선택 |
| F109~F110 | 공지사항·공유게시판 | 심화·선택 |

## 협업 규칙

- `main`: 함께 사용할 기준 코드
- `feature/관광지조회`, `feature/회원관리`처럼 기능별 브랜치에서 작업합니다.
- PR로 변경 내용을 공유하고, 서로 확인한 뒤 병합합니다.
- 커밋 메시지는 한글로 작성합니다. 예: `기능: 지역별 관광지 조회 추가`, `수정: 로그인 세션 처리 오류 해결`
- 역할 분담 예시: 한 명은 관광지 기능과 해당 JSP, 다른 한 명은 회원 기능과 해당 JSP를 담당합니다.
- `pom.xml`, `web.xml`처럼 같이 쓰는 파일을 수정할 때는 변경 내용을 팀원에게 공유합니다.

## 개인 설정 관리

- DB 비밀번호와 API 키는 `.env` 또는 `application-local.properties` 등 Git에서 제외한 개인 설정 파일로 관리합니다.
- 현재 초기 템플릿에는 이 설정 파일을 자동으로 읽는 기능이 없습니다. 기능 구현 시 로딩 코드와 값이 비어 있는 예제 파일을 함께 추가하세요.
- `target/`, IDE 개인 설정, 로그는 `.gitignore`로 제외합니다.
