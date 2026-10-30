"use strict";
document.querySelectorAll("form[data-confirm]").forEach((form) =>
  form.addEventListener("submit", (event) => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  }),
);

const area = document.querySelector("#area[data-areas-url]");
if (area)
  area.addEventListener("change", async () => {
    const district = document.getElementById("district");
    district.replaceChildren(new Option("전체", ""));
    if (!area.value) return;
    try {
      const response = await fetch(
        area.dataset.areasUrl + "?area=" + encodeURIComponent(area.value),
      );
      if (!response.ok) throw new Error("지역 정보를 불러오지 못했습니다.");
      const values = await response.json();
      values.forEach((item) => district.add(new Option(item.name, item.code)));
    } catch (error) {
      district.replaceChildren(
        new Option("지역 정보를 불러오지 못했습니다", ""),
      );
    }
  });

const stops = document.getElementById("stops");
if (stops) {
  let dragging = null;
  stops.addEventListener("dragstart", (event) => {
    dragging = event.target.closest("[data-stop-id]");
  });
  stops.addEventListener("dragover", (event) => {
    event.preventDefault();
    const target = event.target.closest("[data-stop-id]");
    if (dragging && target && dragging !== target) {
      const rect = target.getBoundingClientRect();
      stops.insertBefore(
        dragging,
        event.clientY > rect.top + rect.height / 2
          ? target.nextSibling
          : target,
      );
    }
  });
  stops.addEventListener("dragend", () => {
    dragging = null;
  });
  stops.addEventListener("click", (event) => {
    const button = event.target.closest("[data-move]");
    if (!button) return;
    const row = button.closest("[data-stop-id]");
    if (button.dataset.move === "up" && row.previousElementSibling)
      stops.insertBefore(row, row.previousElementSibling);
    if (button.dataset.move === "down" && row.nextElementSibling)
      stops.insertBefore(row.nextElementSibling, row);
  });
  document.getElementById("reorder-form").addEventListener("submit", () => {
    document.getElementById("stop-order").value = Array.from(
      stops.querySelectorAll("[data-stop-id]"),
      (row) => row.dataset.stopId,
    ).join(",");
  });
}

document.querySelectorAll("[data-map]").forEach((element) => {
  if (!window.L) return;
  const map = L.map(element).setView([36.4, 127.8], 7);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 18,
  }).addTo(map);
  const layers = L.layerGroup().addTo(map);
  let route = null;
  const refresh = () => {
    layers.clearLayers();
    if (route) map.removeLayer(route);
    const rows = element.dataset.route
      ? document
          .getElementById(element.dataset.route)
          .querySelectorAll("[data-stop-id]")
      : element.querySelectorAll("[data-lat]");
    const points = [];
    rows.forEach((row, index) => {
      const lat = Number(row.dataset.lat),
        lon = Number(row.dataset.lon);
      if (
        !Number.isFinite(lat) ||
        !Number.isFinite(lon) ||
        (lat === 0 && lon === 0)
      )
        return;
      const point = [lat, lon];
      points.push(point);
      const popup = document.createElement("div");
      popup.textContent =
        (element.dataset.route ? index + 1 + ". " : "") + row.dataset.title;
      L.marker(point).addTo(layers).bindPopup(popup);
    });
    if (element.dataset.route && points.length > 1)
      route = L.polyline(points, { color: "#23674d" }).addTo(map);
    if (points.length)
      map.fitBounds(L.latLngBounds(points), { padding: [30, 30], maxZoom: 14 });
  };
  refresh();
  if (element.dataset.route) {
    const rows = document.getElementById(element.dataset.route);
    rows.addEventListener("dragend", refresh);
    rows.addEventListener("click", (event) => {
      if (event.target.closest("[data-move]")) refresh();
    });
  }
  if (element.dataset.picker) {
    let chosen;
    map.on("click", (event) => {
      const lat = document.getElementById("latitude"),
        lon = document.getElementById("longitude");
      if (!lat || !lon) return;
      lat.value = event.latlng.lat.toFixed(6);
      lon.value = event.latlng.lng.toFixed(6);
      if (chosen) map.removeLayer(chosen);
      chosen = L.marker(event.latlng).addTo(map);
    });
  }
});
