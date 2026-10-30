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

const stops = document.getElementById('stops');
if (stops) {
  let dragging = null;
  stops.addEventListener('dragstart', event => { dragging = event.target.closest('[data-stop-id]'); });
  stops.addEventListener('dragover', event => {
    event.preventDefault(); const target = event.target.closest('[data-stop-id]');
    if (dragging && target && dragging !== target) {
      const rect = target.getBoundingClientRect();
      stops.insertBefore(dragging, event.clientY > rect.top + rect.height / 2 ? target.nextSibling : target);
    }
  });
  stops.addEventListener('dragend', () => { dragging = null; });
  stops.addEventListener('click', event => {
    const button = event.target.closest('[data-move]'); if (!button) return;
    const row = button.closest('[data-stop-id]');
    if (button.dataset.move === 'up' && row.previousElementSibling) stops.insertBefore(row, row.previousElementSibling);
    if (button.dataset.move === 'down' && row.nextElementSibling) stops.insertBefore(row.nextElementSibling, row);
  });
  document.getElementById('reorder-form').addEventListener('submit', () => {
    document.getElementById('stop-order').value = Array.from(stops.querySelectorAll('[data-stop-id]'), row => row.dataset.stopId).join(',');
  });
}
