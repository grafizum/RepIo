#!/usr/bin/env node
/* Render design/progression-options.html headlessly against the REAL branch
 * data and assert that no option emits undefined / NaN / "null levels" for ANY
 * of the 334 exercises - including the four review-flagged rows that have no
 * ladder at all. Also exercises the missing-data path.
 *
 * Usage: node design/check_options.js
 */
const fs = require('fs');
const path = require('path');

const DIR = __dirname;
const html = fs.readFileSync(path.join(DIR, 'progression-options.html'), 'utf8');
const js = html.split('<script>').slice(-1)[0].split('<' + '/script>')[0];

function stubDom() {
  const mk = () => ({
    value: '', textContent: '', opts: [],
    appendChild(o) { this.opts.push(o); if (!this.value) this.value = o.value; },
    addEventListener() {},
    innerHTML: '',
  });
  const els = { ex: mk(), meta: mk(), grid: mk() };
  global.document = { getElementById: id => els[id], createElement: mk };
  return els;
}

let failures = 0;
function check(label, cond, detail) {
  if (cond) { console.log('  ok   ' + label); }
  else { failures++; console.log('  FAIL ' + label + (detail ? ' -> ' + detail : '')); }
}

// --- with real data ---
global.window = {};
eval(fs.readFileSync(path.join(DIR, '_branches.js'), 'utf8'));
const els = stubDom();
// `new Function` scopes its body, so the option builders have to be handed back
// out or they are invisible here - which is the only reason this file can call
// them directly instead of clicking through the DOM.
const api = new Function(js + '\n; return { optionA, optionB, optionC };')();
const { optionA, optionB, optionC } = api;

const all = Object.keys(window.__BRANCHES__);
console.log('progression-options.html, ' + all.length + ' exercises');
check('first paint did not throw', els.grid.innerHTML.length > 0);
check('picker populated from the data', els.ex.opts.length === all.length,
  els.ex.opts.length + ' options');
check('meta line rendered', /branch/.test(els.meta.textContent), els.meta.textContent);

const BAD = /undefined|NaN|null levels|\[object Object\]/;
const bad = [];
for (const name of all) {
  for (const fn of [optionA, optionB, optionC]) {
    const out = fn(name);
    const m = out.match(BAD);
    if (m) bad.push(name + ' [' + fn.name + ']: ' + m[0]);
  }
}
check('all 3 options render cleanly for every exercise', bad.length === 0,
  bad.slice(0, 5).join(' | '));

// The four rows with no ladder must say so, not print a number.
const noLadder = all.filter(n => !window.__BRANCHES__[n].maxLevel);
check('the no-ladder rows are the 4 review-flagged ones', noLadder.length === 4,
  noLadder.join(', '));
for (const n of noLadder) {
  if (!/no level ladder|no ladder/.test(optionA(n))) {
    failures++;
    console.log('  FAIL optionA hides the missing ladder for ' + n);
  }
}

// --- without the data file ---
// The page THROWS here on purpose, right after writing the explanation. That is
// the design: stop before any code can read `B`, but leave the reason on screen
// first. A silent fallback would hide the real problem behind a default view.
console.log('missing-data path');
const saved = global.window.__BRANCHES__;
let threw = null;
const els2 = (function () {
  const w = global.window;
  global.window = {};
  const e = stubDom();
  try { new Function(js)(); } catch (err) { threw = err; }
  global.window = w;
  return e;
})();
check('stops instead of rendering a broken view', threw !== null);
check('the message is written BEFORE it stops',
  /Could not load/.test(els2.grid.innerHTML), els2.grid.innerHTML.slice(0, 70));
void saved;

console.log(failures ? '\n' + failures + ' FAILURE(S)' : '\nall checks passed');
process.exit(failures ? 1 : 0);