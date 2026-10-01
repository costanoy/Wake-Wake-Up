/* Wake Wake Up · landing page behaviour. No framework, no build step. */
(function () {
  'use strict';

  var config = Object.assign(
    { storeUrl: '', apkUrl: '', contact: '', videoSrc: 'wake-wake-up-video.mp4', privacyUrl: 'privacidade.html' },
    window.WWU_CONFIG || {}
  );
  var reduceMotion = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  var $ = function (id) { return document.getElementById(id); };

  // ------------------------------------------------------------------ Flip digit
  // Split-flap card: on change the old top half falls, then the new bottom half drops.

  function half(position, digit, flap) {
    var el = document.createElement('div');
    el.className = 'flip__half flip__half--' + position + (flap ? ' flip__flap--' + position : '');
    var span = document.createElement('span');
    span.textContent = digit;
    el.appendChild(span);
    return el;
  }

  function Flip(el) {
    this.el = el;
    this.value = el.getAttribute('data-digit') || '0';
    this.top = half('top', this.value);
    this.bottom = half('bottom', this.value);
    var hinge = document.createElement('div');
    hinge.className = 'flip__hinge';
    el.append(this.top, this.bottom, hinge);
    this.flaps = [];
  }

  Flip.prototype.clearFlaps = function () {
    this.flaps.forEach(function (node) { node.remove(); });
    this.flaps = [];
    this.bottom.firstChild.textContent = this.value;
  };

  Flip.prototype.set = function (digit) {
    if (digit === this.value) return;
    var previous = this.value;
    this.clearFlaps();
    this.value = digit;
    this.top.firstChild.textContent = digit;
    if (reduceMotion) {
      this.bottom.firstChild.textContent = digit;
      return;
    }
    // Static layers during the flip: new top, old bottom.
    this.bottom.firstChild.textContent = previous;
    var falling = half('top', previous, true);
    var shade = document.createElement('div');
    shade.className = 'flip__shade';
    var dropping = half('bottom', digit, true);
    this.flaps = [falling, shade, dropping];
    this.el.append(falling, shade, dropping);
    var self = this;
    dropping.addEventListener('animationend', function () {
      if (self.flaps.indexOf(dropping) !== -1) self.clearFlaps();
    });
  };

  /** Change the digit with no animation. */
  Flip.prototype.snap = function (digit) {
    this.value = digit;
    this.top.firstChild.textContent = digit;
    this.clearFlaps();
  };

  function flipsIn(container) {
    return Array.prototype.map.call(container.querySelectorAll('.flip'), function (el) { return new Flip(el); });
  }

  // ------------------------------------------------------------------ Hero clock (Brasília time)

  function brasiliaTime() {
    try {
      var parts = new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date());
      var get = function (type) {
        var part = parts.find(function (p) { return p.type === type; });
        return (part ? part.value : '00').padStart(2, '0');
      };
      return get('hour') + get('minute');
    } catch (e) {
      var d = new Date(Date.now() - 3 * 3600e3);
      return String(d.getUTCHours()).padStart(2, '0') + String(d.getUTCMinutes()).padStart(2, '0');
    }
  }

  var clock = $('hero-clock');
  var clockFlips = flipsIn(clock);
  function tickClock() {
    var t = brasiliaTime();
    clockFlips.forEach(function (flip, i) { flip.set(t[i]); });
    clock.setAttribute('aria-label', 'Horário de Brasília: ' + t.slice(0, 2) + ':' + t.slice(2));
  }
  tickClock();
  setInterval(tickClock, 1000);

  // ------------------------------------------------------------------ Download buttons, footer links

  document.querySelectorAll('[data-download]').forEach(function (slot) {
    var node;
    if (config.storeUrl) {
      node = document.createElement('a');
      node.href = config.storeUrl;
      node.className = 'download';
      node.setAttribute('aria-label', 'Baixar o Wake Wake Up no Google Play');
      node.innerHTML = '<span class="download__button"></span><span>BAIXAR NO GOOGLE PLAY</span>';
    } else if (config.apkUrl) {
      // Not on the store yet: the button hands over the APK file itself.
      node = document.createElement('a');
      node.href = config.apkUrl;
      node.setAttribute('download', '');
      node.className = 'download';
      node.setAttribute('aria-label', 'Baixar o APK do Wake Wake Up para Android');
      node.innerHTML = '<span class="download__button"></span><span>BAIXAR O APK</span>';
    } else {
      node = document.createElement('div');
      node.className = 'download download--soon';
      node.innerHTML = '<span class="download__button"></span><span>EM BREVE NO GOOGLE PLAY</span>';
    }
    slot.appendChild(node);
  });
  // The install note only makes sense while the download is a loose APK file.
  document.querySelectorAll('[data-apk-note]').forEach(function (note) {
    note.hidden = !!config.storeUrl || !config.apkUrl;
  });

  var contactLink = $('contact-link');
  if (config.contact) {
    contactLink.href = 'mailto:' + config.contact;
    contactLink.textContent = config.contact;
    contactLink.hidden = false;
  }
  $('privacy-link').href = config.privacyUrl;

  // ------------------------------------------------------------------ Video (never autoplays)

  var video = $('demo-video');
  var playButton = $('demo-play');
  if (video.getAttribute('src') !== config.videoSrc) video.src = config.videoSrc;
  playButton.addEventListener('click', function () {
    var started = video.play();
    if (started && started.catch) started.catch(function () {});
  });
  video.addEventListener('play', function () { playButton.hidden = true; video.controls = true; });
  ['pause', 'ended'].forEach(function (name) {
    video.addEventListener(name, function () { playButton.hidden = false; video.controls = false; });
  });

  // ------------------------------------------------------------------ LED segments

  function buildSegs(container, count, hotFrom) {
    var segs = [];
    for (var i = 0; i < count; i++) {
      var seg = document.createElement('div');
      seg.className = 'seg' + (i >= hotFrom ? ' is-hot' : '');
      container.appendChild(seg);
      segs.push(seg);
    }
    return segs;
  }
  function light(segs, lit) {
    segs.forEach(function (seg, i) { seg.classList.toggle('is-on', i < lit); });
  }

  // Step 01: the volume meter ramps 3 → 16; the switch turns the ramp off (meter full).
  var volSegs = buildSegs($('vol-segs'), 16, 13);
  var volLabel = $('vol-label');
  var rampSwitch = $('ramp-switch');
  var ramp = true;
  var vol = 4;
  function paintVolume() {
    light(volSegs, ramp ? vol : 16);
    volLabel.textContent = ramp ? 'SUBINDO' : 'MÁXIMO';
  }
  paintVolume();
  setInterval(function () {
    if (!ramp || reduceMotion) return;
    vol = vol >= 16 ? 3 : vol + 1;
    paintVolume();
  }, 450);
  rampSwitch.addEventListener('click', function () {
    ramp = !ramp;
    rampSwitch.setAttribute('aria-checked', String(ramp));
    paintVolume();
  });

  // ------------------------------------------------------------------ Step 02: calculator mission (7 × 8)

  var calc = $('calc');
  var calcDisplay = calc.querySelector('.calc__display');
  var calcMsg = $('calc-msg');
  var calcAnswer = $('calc-answer');
  var answer = '';
  var state = 'idle'; // idle | wrong | ok
  var okTimer, shakeTimer;

  function paintCalc() {
    calc.classList.toggle('is-wrong', state === 'wrong');
    calc.classList.toggle('is-ok', state === 'ok');
    calcAnswer.textContent = answer ? answer.replace('-', '−') : '_';
    calcMsg.textContent = state === 'wrong' ? 'Errado. Tente de novo.' : state === 'ok' ? 'Certo. Alarme desligado.' : 'Digite o resultado';
  }
  function shake() {
    if (reduceMotion) return;
    var steps = [-8, 8, -6, 6, -3, 3, 0];
    var i = 0;
    clearTimeout(shakeTimer);
    (function step() {
      calcDisplay.style.transform = 'translateX(' + steps[i] + 'px)';
      i++;
      if (i < steps.length) shakeTimer = setTimeout(step, 45);
    })();
  }
  function press(key) {
    if (key === 'OK') {
      if (answer === '56') {
        state = 'ok';
        clearTimeout(okTimer);
        okTimer = setTimeout(function () { state = 'idle'; answer = ''; paintCalc(); }, 1800);
      } else if (answer && answer !== '-') {
        state = 'wrong';
        shake();
      }
      paintCalc();
      return;
    }
    // After a verdict, the next key starts from an empty display.
    var current = state === 'idle' ? answer : '';
    if (key === 'C') current = '';
    else if (key === 'B') current = current.slice(0, -1);
    else if (key === 'M') current = current.charAt(0) === '-' ? current.slice(1) : '-' + current;
    else if (current.replace('-', '').length < 4) current += key;
    answer = current;
    state = 'idle';
    clearTimeout(okTimer);
    paintCalc();
  }
  calc.querySelectorAll('[data-key]').forEach(function (button) {
    button.addEventListener('click', function () { press(button.getAttribute('data-key')); });
  });

  // ------------------------------------------------------------------ Step 03: "12 MIN" flips in from 00

  var bomdiaFlips = flipsIn($('bomdia-digits'));

  // ------------------------------------------------------------------ Tuner (radiogroup)

  var STATIONS = [
    ['Campainha mecânica', 'APP'], ['Telefone de disco', 'APP'], ['Bipe digital', 'APP'], ['Sirene', 'APP'], ['Pássaros', 'APP'],
    ['Toques do celular', 'CELULAR'], ['Música do aparelho', 'CELULAR'], ['Rádio da internet', 'RÁDIO']
  ];
  var ticks = $('dial-ticks');
  for (var t = 0; t < 29; t++) ticks.appendChild(document.createElement('i'));
  var stationsEl = $('stations');
  var stationButtons = STATIONS.map(function (station, index) {
    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'station';
    button.setAttribute('role', 'radio');
    button.innerHTML = '<i></i><span></span><small></small>';
    button.querySelector('span').textContent = station[0];
    button.querySelector('small').textContent = station[1];
    button.addEventListener('click', function () { pickStation(index); });
    stationsEl.appendChild(button);
    return button;
  });
  function pickStation(index, focus) {
    stationButtons.forEach(function (button, i) {
      button.setAttribute('aria-checked', String(i === index));
      button.tabIndex = i === index ? 0 : -1;
    });
    $('station-name').textContent = STATIONS[index][0].toUpperCase();
    $('station-group').textContent = STATIONS[index][1];
    $('dial-needle').style.left = (5 + index * (90 / (STATIONS.length - 1))) + '%';
    if (focus) stationButtons[index].focus();
  }
  // Arrow keys move the selection, as a radio group should.
  stationsEl.addEventListener('keydown', function (event) {
    var current = stationButtons.indexOf(document.activeElement);
    if (current < 0) return;
    var delta = event.key === 'ArrowDown' || event.key === 'ArrowRight' ? 1 : event.key === 'ArrowUp' || event.key === 'ArrowLeft' ? -1 : 0;
    if (!delta) return;
    event.preventDefault();
    pickStation((current + delta + STATIONS.length) % STATIONS.length, true);
  });
  pickStation(0);

  // ------------------------------------------------------------------ Stats chart (example data)

  var MINUTES = [6, 14, 9, 22, 4, 11, 12];
  var DAYS = ['QUI', 'SEX', 'SÁB', 'DOM', 'SEG', 'TER', 'HOJE'];
  var chartEl = $('chart');
  var columns = MINUTES.map(function (minutes, i) {
    var col = document.createElement('div');
    col.className = 'chart__col' + (i === MINUTES.length - 1 ? ' is-today' : '');
    var value = document.createElement('span');
    value.className = 'chart__value';
    value.textContent = minutes;
    var bars = document.createElement('div');
    bars.className = 'chart__bars';
    // Built bottom-up (segment 0 is the lowest), shown top-down.
    var segs = buildSegs(bars, 12, 8);
    bars.replaceChildren.apply(bars, segs.slice().reverse());
    var day = document.createElement('span');
    day.className = 'chart__day';
    day.textContent = DAYS[i];
    col.append(value, bars, day);
    chartEl.appendChild(col);
    return { segs: segs, full: Math.max(1, Math.round(minutes / 2)) };
  });
  function paintChart(step) {
    columns.forEach(function (column) { light(column.segs, Math.min(step, column.full)); });
  }
  paintChart(12);

  // ------------------------------------------------------------------ Scroll reveals and their triggers

  var triggers = {
    stats: function () {
      var step = 0;
      paintChart(0);
      var timer = setInterval(function () {
        step++;
        paintChart(step);
        if (step >= 12) clearInterval(timer);
      }, 80);
    },
    bomdia: function () {
      bomdiaFlips[0].snap('0');
      bomdiaFlips[1].snap('0');
      setTimeout(function () { bomdiaFlips[0].set('1'); bomdiaFlips[1].set('2'); }, 500);
    }
  };

  if (!reduceMotion && 'IntersectionObserver' in window) {
    var targets = Array.prototype.slice.call(document.querySelectorAll('[data-reveal]'));
    targets.forEach(function (el) {
      if (el.getBoundingClientRect().top > window.innerHeight * 0.92) el.classList.add('reveal');
    });
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var el = entry.target;
        el.classList.add('is-in');
        var trigger = el.getAttribute('data-trigger');
        if (trigger && triggers[trigger]) triggers[trigger]();
        observer.unobserve(el);
      });
    }, { threshold: 0.2 });
    targets.forEach(function (el) { observer.observe(el); });
  }
})();
