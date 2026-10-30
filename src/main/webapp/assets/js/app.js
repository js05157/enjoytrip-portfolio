"use strict";
document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => {
  if (!window.confirm(form.dataset.confirm)) event.preventDefault();
}));

const area = document.querySelector('#area[data-areas-url]');
if (area) area.addEventListener('change', async () => {
  const district = document.getElementById('district');
  district.replaceChildren(new Option('전체', ''));
  if (!area.value) return;
  try {
    const response = await fetch(area.dataset.areasUrl + '?area=' + encodeURIComponent(area.value));
    if (!response.ok) throw new Error('지역 정보를 불러오지 못했습니다.');
    const values = await response.json();
    values.forEach(item => district.add(new Option(item.name, item.code)));
  } catch (error) { district.replaceChildren(new Option('지역 정보를 불러오지 못했습니다', '')); }
});
