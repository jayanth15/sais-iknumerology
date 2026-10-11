"use strict";
(() => {
  const form = document.getElementById('nameSearchForm');
  const submit = document.getElementById('generateSubmit');
  const busy = document.getElementById('generationBusy');
  if (!form || !submit || !busy) return;
  form.addEventListener('submit', () => {
    if (!form.checkValidity()) return;
    busy.hidden = false;
    submit.disabled = true;
    submit.textContent = 'Generating with Sarvam AI…';
  });
  window.addEventListener('pageshow', () => {
    submit.disabled = false;
    submit.textContent = 'Find names →';
    busy.hidden = true;
  });
})();
