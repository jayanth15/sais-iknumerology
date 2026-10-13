"use strict";
(() => {
  const form = document.getElementById('nameSearchForm');
  const submit = document.getElementById('generateSubmit');
  const busy = document.getElementById('generationBusy');
  const upload = document.getElementById('excelUploadPanel');
  const file = document.getElementById('namesFile');
  const startLetters = document.getElementById('startLetter');
  const sarvamNote = document.getElementById('sarvamNotice');
  const excelNote = document.getElementById('excelNotice');
  if (!form || !submit || !busy) return;
  const source = () => form.querySelector('input[name="nameSource"]:checked')?.value || 'SARVAM';
  const update = () => {
    const isExcel = source() === 'EXCEL';
    if (upload) upload.hidden = !isExcel;
    if (file) {file.required = isExcel; if (!isExcel) file.value = '';}
    if (startLetters) {
      startLetters.disabled = isExcel;
      startLetters.required = !isExcel;
      startLetters.setAttribute('aria-disabled', String(isExcel));
    }
    if (sarvamNote) sarvamNote.hidden = isExcel;
    if (excelNote) excelNote.hidden = !isExcel;
    busy.textContent = isExcel ? 'Reading Excel and calculating pyramid numbers…' : 'Generating with Sarvam AI… This may take around 30 seconds.';
  };
  form.querySelectorAll('input[name="nameSource"]').forEach(choice => choice.addEventListener('change', update));
  update();
  form.addEventListener('submit', () => {
    if (!form.checkValidity()) return;
    const isExcel = source() === 'EXCEL';
    busy.hidden = false;
    submit.disabled = true;
    submit.textContent = isExcel ? 'Reading Excel…' : 'Generating with Sarvam AI…';
  });
  window.addEventListener('pageshow', () => {
    submit.disabled = false;
    submit.textContent = 'Find names →';
    busy.hidden = true;
    update();
  });
})();
