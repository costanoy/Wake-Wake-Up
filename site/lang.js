/* Wake Wake Up · language. Portuguese lives at /, English at /en/.
   Loaded in <head>, before anything is drawn, so a redirect never flashes the wrong page.
   Rules, in order:
   1. A choice made on the PT / EN switch is remembered in this browser and always wins.
   2. Moving around inside the site never redirects: the visitor is already reading this version.
   3. Otherwise the browser language decides: "pt..." gets Portuguese, anything else gets English.
   Crawlers are never redirected, so each version is indexed at its own address (see hreflang).
   Each page says which version it is and where the other one is: <html data-lang data-alt>. */
(function () {
  'use strict';

  var KEY = 'wwu-lang';
  var root = document.documentElement;
  var here = root.getAttribute('data-lang');
  var alt = root.getAttribute('data-alt');

  // Remember a choice made on the switch (the click runs before the link navigates).
  document.addEventListener('click', function (event) {
    var link = event.target && event.target.closest ? event.target.closest('[data-lang-choice]') : null;
    if (!link) return;
    try { localStorage.setItem(KEY, link.getAttribute('data-lang-choice')); } catch (e) { /* storage off */ }
  });

  if (!here || !alt || location.protocol === 'file:') return;
  if (/bot|crawl|spider|slurp|inspectiontool|lighthouse|headless/i.test(navigator.userAgent || '')) return;

  var wanted = null;
  try { wanted = localStorage.getItem(KEY); } catch (e) { /* storage off */ }
  if (wanted !== 'pt' && wanted !== 'en') {
    var fromThisSite = false;
    try { fromThisSite = !!document.referrer && new URL(document.referrer).origin === location.origin; } catch (e) { /* no referrer */ }
    if (fromThisSite) return;
    var first = (navigator.languages && navigator.languages[0]) || navigator.language || '';
    if (!first) return;
    wanted = /^pt(-|$)/i.test(first) ? 'pt' : 'en';
  }
  if (wanted === here) return;

  // Keep the section the visitor was sent to.
  var PT_TO_EN = { '#baixar': '#download', '#como-funciona': '#how-it-works', '#teste': '#test' };
  var hash = location.hash;
  Object.keys(PT_TO_EN).forEach(function (pt) {
    if (here === 'pt' && hash === pt) hash = PT_TO_EN[pt];
    else if (here === 'en' && hash === PT_TO_EN[pt]) hash = pt;
  });
  location.replace(alt + hash);
})();
