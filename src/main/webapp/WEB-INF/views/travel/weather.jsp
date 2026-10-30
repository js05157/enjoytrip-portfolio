<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragments/header.jspf" %>
<p class="eyebrow">BEFORE YOU GO</p><h1 class="page-title">여행 날씨와 해 뜨는 시간</h1>
<p class="muted">오늘부터 15일 뒤까지 여행지의 예보를 확인하세요. 모든 날짜와 시간은 한국 시간입니다.</p>
<div class="toolbar"><a class="button secondary small" href="<c:url value='/travel-photos'/>">여행 사진</a><a class="button secondary small" href="<c:url value='/chargers'/>">전기차 충전소</a></div>
<form class="search" method="get" action="<c:url value='/travel-info'/>">
  <div class="field"><label for="weatherName">장소 이름</label><input id="weatherName" name="name" maxlength="100" value="<c:out value='${name}'/>"></div>
  <div class="field"><label for="weatherLat">위도</label><input id="weatherLat" name="latitude" type="number" step="any" min="32" max="40" value="${latitude}" required></div>
  <div class="field"><label for="weatherLon">경도</label><input id="weatherLon" name="longitude" type="number" step="any" min="124" max="132" value="${longitude}" required></div>
  <div class="field"><label for="weatherStart">시작일</label><input id="weatherStart" name="start" type="date" min="${minDate}" max="${maxDate}" value="${start}" required></div>
  <div class="field"><label for="weatherEnd">종료일</label><input id="weatherEnd" name="end" type="date" min="${minDate}" max="${maxDate}" value="${end}" required></div>
  <button class="primary">기간 조회</button>
</form>
<c:if test="${not empty forecast}"><h2><c:out value="${name}"/>의 예보</h2>
<div class="table-wrap"><table><thead><tr><th>날짜</th><th>날씨</th><th>최저 / 최고 (°C)</th><th>강수확률 (%)</th><th>일출</th><th>일몰</th></tr></thead><tbody>
<c:forEach var="day" items="${forecast.days}"><tr><td><c:out value="${day.date}"/></td><td><c:out value="${day.condition}"/></td><td><c:out value="${day.minimum}"/> / <c:out value="${day.maximum}"/></td><td><c:out value="${day.rain}"/></td><td><c:out value="${day.sunrise}"/></td><td><c:out value="${day.sunset}"/></td></tr></c:forEach>
</tbody></table></div>
<p class="muted">제공: <a href="https://open-meteo.com/" target="_blank" rel="noopener noreferrer">Open-Meteo</a> · 데이터 CC BY 4.0 · 조회 시각 <c:out value="${forecast.retrievedAt}"/> KST<br>예보는 변경될 수 있으며 최대 30분간 저장된 결과가 표시됩니다. 일출·일몰은 지형과 실제 관측 환경에 따라 차이가 있을 수 있습니다.</p></c:if>
<p><a class="button secondary" href="<c:url value='/attractions'/>">여행지에서 위치 선택하기</a></p>
<%@ include file="../fragments/footer.jspf" %>
