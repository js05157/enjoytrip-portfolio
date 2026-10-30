<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragments/header.jspf" %>
<p class="eyebrow">YOUR NEXT SCENE</p><h1 class="page-title">사진으로 고르는 다음 여행</h1>
<p class="muted">한국관광공사의 사진이 등록된 여행지를 지역별 이름순으로 둘러보세요.</p>
<div class="toolbar"><a class="button secondary small" href="<c:url value='/travel-info'/>">날씨·일출·일몰</a><a class="button secondary small" href="<c:url value='/chargers'/>">전기차 충전소</a></div>
<c:if test="${not empty item}"><section class="section"><h2><c:out value="${item.title}"/>의 사진</h2>
<c:if test="${not empty photoError}"><p class="notice error"><c:out value="${photoError}"/></p></c:if>
<div class="grid photo-gallery"><c:forEach var="photo" items="${photos}"><figure class="card"><a href="<c:out value='${photo.url}'/>" target="_blank" rel="noopener noreferrer"><img src="<c:out value='${photo.url}'/>" alt="<c:out value='${photo.name}'/>" loading="lazy"></a><figcaption class="card-body"><c:out value="${photo.name}"/><p class="muted"><c:out value="${photo.copyright}"/> · 한국관광공사</p></figcaption></figure></c:forEach></div>
<c:if test="${empty photos and empty photoError}"><p class="notice">등록된 추가 사진이 없습니다.</p></c:if></section></c:if>
<form class="search" action="<c:url value='/travel-photos'/>"><div class="field"><label for="photoArea">지역</label><select name="area" id="photoArea"><option value="">전국</option><c:forEach var="region" items="${regions}"><option value="${region.key}" ${area eq region.key ? 'selected' : ''}><c:out value="${region.value}"/></option></c:forEach></select></div><button class="primary">사진 찾기</button></form>
<div class="grid photo-gallery"><c:forEach var="place" items="${result.items}"><article class="card"><c:url var="detailUrl" value="/attractions/detail"><c:param name="id" value="${place.contentId}"/></c:url><a href="${detailUrl}"><img src="<c:out value='${place.image}'/>" alt="<c:out value='${place.title}'/>" loading="lazy"></a><div class="card-body"><h2><a href="${detailUrl}"><c:out value="${place.title}"/></a></h2><p><c:out value="${place.address}"/></p><c:url var="photoUrl" value="/travel-photos"><c:param name="area" value="${area}"/><c:param name="page" value="${page}"/><c:param name="contentId" value="${place.contentId}"/></c:url><a class="button secondary small" href="${photoUrl}">추가 사진 보기</a></div></article></c:forEach></div>
<c:if test="${empty result.items}"><p class="empty">표시할 사진이 없습니다. 개발 모드에서는 외부 사진을 불러오지 않습니다.</p></c:if>
<div class="pagination"><c:if test="${page gt 1}"><a href="?area=${area}&amp;page=${page-1}">이전</a></c:if><span>${page} 페이지</span><c:if test="${hasNext}"><a href="?area=${area}&amp;page=${page+1}">다음</a></c:if></div>
<p class="muted">사진 제공: 한국관광공사 TourAPI · 원본 사진은 클릭해서 확인할 수 있습니다. 개별 사진의 공공누리 유형과 이용 조건을 확인하세요.</p>
<%@ include file="../fragments/footer.jspf" %>
