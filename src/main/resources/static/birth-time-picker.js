/** Dependency-free, keyboard/touch-accessible 12-hour birth time selector.
 * Preserves the existing HH:mm value submitted to the Spring MVC controller.
 * The native time input remains available when JavaScript is disabled.
 */
(() => {
  'use strict';
  document.querySelectorAll('[data-time-picker]').forEach(root => {
    const native = root.querySelector('.birth-time-native');
    const enhanced = root.querySelector('.birth-time-enhanced');
    const hourSelect = root.querySelector('.birth-hour');
    const minuteSelect = root.querySelector('.birth-minute');
    const periods = Array.from(root.querySelectorAll('input[name="timeDisplayPeriod"]'));
    const output = root.querySelector('.birth-time-live');
    if (!native || !enhanced || !hourSelect || !minuteSelect || periods.length !== 2) return;
    const pad = number => String(number).padStart(2, '0');
    const option = (label, value) => {
      const element = document.createElement('option');
      element.textContent = label;
      element.value = value;
      return element;
    };
    hourSelect.append(option('HH', ''));
    for (let h = 1; h <= 12; h++) hourSelect.append(option(pad(h), String(h)));
    minuteSelect.append(option('MM', ''));
    for (let m = 0; m <= 59; m++) minuteSelect.append(option(pad(m), String(m)));

    const initial = /^(\d{2}):(\d{2})$/.exec(native.value);
    if (initial) {
      const h = Number(initial[1]);
      const m = Number(initial[2]);
      if (h <= 23 && m <= 59) {
        hourSelect.value = String(h % 12 || 12);
        minuteSelect.value = String(m);
        periods.forEach(input => { input.checked = input.value === (h >= 12 ? 'PM' : 'AM'); });
      }
    }

    function sync() {
      const hour = Number(hourSelect.value);
      const minute = Number(minuteSelect.value);
      const period = periods.find(item => item.checked)?.value || 'AM';
      if (hourSelect.value === '' || minuteSelect.value === '') {
        native.value = '';
        output.textContent = 'Choose an hour and minute';
        output.classList.remove('is-set');
        return;
      }
      const hour24 = (hour % 12) + (period === 'PM' ? 12 : 0);
      native.value = `${pad(hour24)}:${pad(minute)}`;
      output.textContent = `${pad(hour)}:${pad(minute)} ${period} · IST`;
      output.classList.add('is-set');
    }
    hourSelect.addEventListener('change', sync);
    minuteSelect.addEventListener('change', sync);
    periods.forEach(item => item.addEventListener('change', sync));
    // Progressive enhancement: keep a usable browser-native input without JS.
    native.type = 'hidden';
    native.required = false;
    enhanced.hidden = false;
    sync();
  });
})();
