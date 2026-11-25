document.addEventListener('DOMContentLoaded', function () {
  // ---------- navbar: shadow on scroll + highlight the current page ----------
  var nav = document.getElementById('spNav');
  if (nav) {
    var onScroll = function () { nav.classList.toggle('is-scrolled', window.scrollY > 8); };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    var path = window.location.pathname.replace(/\/+$/, '') || '/';
    var best = null;
    nav.querySelectorAll('.navbar-nav .nav-link').forEach(function (a) {
      var href = (a.getAttribute('href') || '').split('?')[0].replace(/\/+$/, '') || '/';
      if (href === '/' ) { return; }
      if (path === href || path.indexOf(href + '/') === 0) {
        if (!best || href.length > best.len) { best = { el: a, len: href.length }; }
      }
    });
    if (best) { best.el.classList.add('active'); best.el.setAttribute('aria-current', 'page'); }
  }

  // ---------- success messages fade away on their own ----------
  document.querySelectorAll('.js-flash').forEach(function (el) {
    setTimeout(function () {
      if (window.bootstrap && el.isConnected) { bootstrap.Alert.getOrCreateInstance(el).close(); }
    }, 6000);
  });

  // ---------- quantity steppers ----------
  document.querySelectorAll('.qty').forEach(function (box) {
    var input = box.querySelector('input');
    if (!input) { return; }
    box.querySelectorAll('button[data-step]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var min = parseInt(input.min || '1', 10), max = parseInt(input.max || '20', 10);
        var next = (parseInt(input.value, 10) || min) + parseInt(btn.dataset.step, 10);
        input.value = Math.min(max, Math.max(min, next));
      });
    });
  });

  // ---------- password show / hide ----------
  document.querySelectorAll('.pw-toggle').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = btn.parentElement.querySelector('input');
      if (!input) { return; }
      var show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      var icon = btn.querySelector('i');
      if (icon) { icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye'; }
      btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    });
  });

  // ---------- restaurant list: search + cuisine chips ----------
  var restItems = document.querySelectorAll('.js-rest');
  if (restItems.length) {
    var rSearch = document.getElementById('restaurantSearch');
    var rChips = document.getElementById('cuisineChips');
    var rCount = document.getElementById('restaurantCount');
    var rEmpty = document.getElementById('restaurantEmpty');
    var rActive = 'all';
    var cuisines = {};
    restItems.forEach(function (el) { if (el.dataset.cuisine) { cuisines[el.dataset.cuisine] = true; } });
    if (rChips) {
      var makeChip = function (label, value) {
        var b = document.createElement('button');
        b.type = 'button'; b.className = 'chip' + (value === 'all' ? ' active' : ''); b.textContent = label; b.dataset.value = value;
        b.addEventListener('click', function () {
          rChips.querySelectorAll('.chip').forEach(function (c) { c.classList.remove('active'); });
          b.classList.add('active'); rActive = value; applyRest();
        });
        rChips.appendChild(b);
      };
      makeChip('All', 'all');
      Object.keys(cuisines).sort().forEach(function (c) { makeChip(c, c); });
    }
    var applyRest = function () {
      var q = rSearch ? rSearch.value.trim().toLowerCase() : '';
      var shown = 0;
      restItems.forEach(function (el) {
        var okCuisine = rActive === 'all' || el.dataset.cuisine === rActive;
        var okText = !q || el.textContent.toLowerCase().indexOf(q) !== -1;
        var show = okCuisine && okText;
        el.classList.toggle('d-none', !show);
        if (show) { shown++; }
      });
      if (rCount) { rCount.textContent = shown + (shown === 1 ? ' restaurant' : ' restaurants'); }
      if (rEmpty) { rEmpty.classList.toggle('d-none', shown !== 0); }
    };
    if (rSearch) {
      rSearch.addEventListener('input', applyRest);
      var heroForm = rSearch.closest('form');
      if (heroForm) {
        heroForm.addEventListener('submit', function (e) {
          e.preventDefault(); applyRest();
          var target = document.getElementById('restaurants');
          if (target) { target.scrollIntoView({ behavior: 'smooth' }); }
        });
      }
    }
    applyRest();
  }

  // ---------- menu page: search, cuisine chips, hide dishes with warnings ----------
  var dishes = document.querySelectorAll('.js-dish');
  if (dishes.length) {
    var mSearch = document.getElementById('dishSearch');
    var mChips = document.getElementById('dishChips');
    var mSafe = document.getElementById('safeOnly');
    var mCount = document.getElementById('dishCount');
    var mEmpty = document.getElementById('dishEmpty');
    var mActive = 'all';
    var dc = {};
    dishes.forEach(function (el) { if (el.dataset.cuisine) { dc[el.dataset.cuisine] = true; } });
    var keys = Object.keys(dc).sort();
    if (mChips && keys.length > 1) {
      var mk = function (label, value) {
        var b = document.createElement('button');
        b.type = 'button'; b.className = 'chip' + (value === 'all' ? ' active' : ''); b.textContent = label;
        b.addEventListener('click', function () {
          mChips.querySelectorAll('.chip').forEach(function (c) { c.classList.remove('active'); });
          b.classList.add('active'); mActive = value; applyMenu();
        });
        mChips.appendChild(b);
      };
      mk('All', 'all');
      keys.forEach(function (c) { mk(c, c); });
    }
    var applyMenu = function () {
      var q = mSearch ? mSearch.value.trim().toLowerCase() : '';
      var onlySafe = mSafe && mSafe.checked;
      var shown = 0;
      dishes.forEach(function (el) {
        var show = (mActive === 'all' || el.dataset.cuisine === mActive)
          && (!q || el.textContent.toLowerCase().indexOf(q) !== -1)
          && (!onlySafe || el.dataset.safe === 'true');
        el.classList.toggle('d-none', !show);
        if (show) { shown++; }
      });
      if (mCount) { mCount.textContent = shown + (shown === 1 ? ' dish' : ' dishes'); }
      if (mEmpty) { mEmpty.classList.toggle('d-none', shown !== 0); }
    };
    if (mSearch) { mSearch.addEventListener('input', applyMenu); }
    if (mSafe) { mSafe.addEventListener('change', applyMenu); }
    applyMenu();
  }

  // ---------- warn before adding an item that carries safety warnings for this customer ----------
  var modalEl = document.getElementById('warningModal');
  if (modalEl && window.bootstrap) {
    var modal = new bootstrap.Modal(modalEl);
    var pending = null;
    document.querySelectorAll('form.js-add-form').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        var text = form.dataset.warning;
        if (!text || form.dataset.confirmed === 'true') { return; }
        e.preventDefault();
        pending = form;
        document.getElementById('warningModalBody').textContent = text;
        modal.show();
      });
    });
    var confirmBtn = document.getElementById('warningConfirm');
    if (confirmBtn) {
      confirmBtn.addEventListener('click', function () {
        if (pending) {
          pending.dataset.confirmed = 'true';
          modal.hide();
          pending.submit();
        }
      });
    }
  }

  // ---------- checkout: the order button stays disabled until warnings are acknowledged ----------
  var ack = document.getElementById('acknowledgeWarnings');
  var btn = document.getElementById('placeOrderBtn');
  if (ack && btn) {
    var sync = function () { btn.disabled = !ack.checked; };
    ack.addEventListener('change', sync);
    sync();
  }
  // Prevent double submits on the order form (re-enabled if the user comes back with the browser's back button).
  if (btn && btn.form) {
    btn.form.addEventListener('submit', function () {
      setTimeout(function () {
        btn.dataset.label = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Placing order...';
      }, 0);
    });
    window.addEventListener('pageshow', function (ev) {
      if (ev.persisted && btn.dataset.label) { btn.innerHTML = btn.dataset.label; if (ack) { ack.dispatchEvent(new Event('change')); } else { btn.disabled = false; } }
    });
  }
});
