(function () {
  var s = window.STATS;
  if (!s || typeof Chart === 'undefined') { return; }
  function make(id, config) {
    var el = document.getElementById(id);
    if (el) { new Chart(el, config); }
  }
  make('salesChart', {
    type: 'bar',
    data: { labels: s.dayLabels, datasets: [{ label: 'Revenue (\u20B9)', data: s.dayRevenue, backgroundColor: '#16a05a' }] },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
  });
  make('statusChart', { type: 'doughnut', data: { labels: s.statusLabels, datasets: [{ data: s.statusCounts }] } });
  make('topItemsChart', {
    type: 'bar',
    data: { labels: s.topItemNames, datasets: [{ label: 'Quantity sold', data: s.topItemQuantities, backgroundColor: '#ff7a1a' }] },
    options: { indexAxis: 'y', plugins: { legend: { display: false } } }
  });
  make('warningChart', { type: 'pie', data: { labels: s.warningLabels, datasets: [{ data: s.warningCounts }] } });
})();
