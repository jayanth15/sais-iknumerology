/** Offline, dependency-free birth date picker. Uses ISO yyyy-MM-dd for the existing Spring form. */
(() => {
  'use strict';
  const root = document.querySelector('[data-dob-picker]');
  if (!root) return;

  const native = root.querySelector('.dob-native');
  const enhanced = root.querySelector('.dob-enhanced');
  const textInput = root.querySelector('.dob-text');
  const trigger = root.querySelector('.dob-trigger');
  const popover = root.querySelector('.dob-popover');
  const closeButton = root.querySelector('.dob-close');
  const monthSelect = root.querySelector('.dob-month');
  const yearSelect = root.querySelector('.dob-year');
  const monthTitle = root.querySelector('.dob-month-title');
  const days = root.querySelector('.dob-days');
  const previous = root.querySelector('.dob-prev');
  const next = root.querySelector('.dob-next');
  const clear = root.querySelector('.dob-clear');
  const todayButton = root.querySelector('.dob-today');
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const minYear = 1900;
  const months = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'];
  const pad = n => String(n).padStart(2, '0');
  const iso = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  const display = date => `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
  const validDate = (year, month, day) => {
    const result = new Date(year, month - 1, day);
    return year >= 1 && year <= today.getFullYear() && month >= 1 && month <= 12 &&
      day >= 1 && result.getFullYear() === year && result.getMonth() === month - 1 &&
      result.getDate() === day && result <= today ? result : null;
  };
  const fromISO = value => {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
    return match ? validDate(+match[1], +match[2], +match[3]) : null;
  };
  const fromDisplay = value => {
    const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec((value || '').trim());
    return match ? validDate(+match[3], +match[2], +match[1]) : null;
  };

  let selected = fromISO(native.value);
  let viewYear = selected ? selected.getFullYear() : Math.max(minYear, today.getFullYear() - 25);
  let viewMonth = selected ? selected.getMonth() : 0;

  // Keep the original native control as a fully usable fallback if JavaScript is disabled.
  // It becomes the ISO value submitted to Spring when the modern interface is enabled.
  native.type = 'hidden';
  native.required = false;
  enhanced.hidden = false;
  if (selected) textInput.value = display(selected);

  months.forEach((month, index) => {
    const option = document.createElement('option');
    option.value = String(index);
    option.textContent = month;
    monthSelect.append(option);
  });
  for (let year = today.getFullYear(); year >= minYear; year--) {
    const option = document.createElement('option');
    option.value = String(year);
    option.textContent = String(year);
    yearSelect.append(option);
  }

  function jump(year, month) {
    if (year < minYear) { year = minYear; month = 0; }
    if (year > today.getFullYear() || (year === today.getFullYear() && month > today.getMonth())) {
      year = today.getFullYear(); month = today.getMonth();
    }
    viewYear = year;
    viewMonth = month;
    render();
  }

  function render() {
    yearSelect.value = String(viewYear);
    monthSelect.value = String(viewMonth);
    Array.from(monthSelect.options).forEach(option => {
      option.disabled = viewYear === today.getFullYear() && Number(option.value) > today.getMonth();
    });
    monthTitle.textContent = `${months[viewMonth]} ${viewYear}`;
    previous.disabled = viewYear === minYear && viewMonth === 0;
    next.disabled = viewYear === today.getFullYear() && viewMonth === today.getMonth();
    days.replaceChildren();
    const firstWeekday = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
    for (let cell = 0; cell < Math.ceil((firstWeekday + daysInMonth) / 7) * 7; cell++) {
      const dayNumber = cell - firstWeekday + 1;
      if (dayNumber < 1 || dayNumber > daysInMonth) {
        const placeholder = document.createElement('span');
        placeholder.className = 'dob-day-placeholder';
        days.append(placeholder);
        continue;
      }
      const day = new Date(viewYear, viewMonth, dayNumber);
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'dob-day';
      button.textContent = String(dayNumber);
      button.setAttribute('aria-label', `${dayNumber} ${months[viewMonth]} ${viewYear}`);
      button.disabled = day > today;
      if (iso(day) === iso(today)) button.classList.add('is-today');
      if (selected && iso(day) === iso(selected)) {
        button.classList.add('is-selected');
        button.setAttribute('aria-selected', 'true');
      }
      button.addEventListener('click', () => choose(day));
      days.append(button);
    }
  }

  function choose(date) {
    selected = date;
    native.value = iso(date);
    textInput.value = display(date);
    textInput.setCustomValidity('');
    textInput.classList.remove('dob-invalid');
    jump(date.getFullYear(), date.getMonth());
    hideCalendar();
    textInput.focus();
  }

  function hideCalendar(restoreFocus = false) {
    popover.hidden = true;
    trigger.setAttribute('aria-expanded', 'false');
    if (restoreFocus) trigger.focus();
  }
  function showCalendar() {
    const typed = fromDisplay(textInput.value);
    if (typed) { viewYear = typed.getFullYear(); viewMonth = typed.getMonth(); }
    render();
    popover.hidden = false;
    trigger.setAttribute('aria-expanded', 'true');
    yearSelect.focus(); // Older dates can be selected immediately without month-by-month clicking.
  }

  trigger.addEventListener('click', () => {
    if (popover.hidden) showCalendar(); else hideCalendar(true);
  });
  closeButton.addEventListener('click', () => hideCalendar(true));
  document.addEventListener('pointerdown', event => {
    if (!popover.hidden && !root.contains(event.target)) hideCalendar();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !popover.hidden) { event.preventDefault(); hideCalendar(true); }
  });
  yearSelect.addEventListener('change', () => jump(+yearSelect.value, viewMonth));
  monthSelect.addEventListener('change', () => jump(viewYear, +monthSelect.value));
  previous.addEventListener('click', () => {
    const firstOfPrevious = new Date(viewYear, viewMonth - 1, 1);
    jump(firstOfPrevious.getFullYear(), firstOfPrevious.getMonth());
  });
  next.addEventListener('click', () => {
    const firstOfNext = new Date(viewYear, viewMonth + 1, 1);
    jump(firstOfNext.getFullYear(), firstOfNext.getMonth());
  });
  todayButton.addEventListener('click', () => choose(today));
  clear.addEventListener('click', () => {
    selected = null;
    native.value = '';
    textInput.value = '';
    textInput.setCustomValidity('');
    textInput.classList.remove('dob-invalid');
    render();
    hideCalendar();
    textInput.focus();
  });

  function syncTypedDate(showError = false) {
    const value = textInput.value.trim();
    const parsed = fromDisplay(value);
    selected = parsed;
    native.value = parsed ? iso(parsed) : '';
    const invalid = value !== '' && !parsed;
    if (showError) {
      textInput.setCustomValidity(invalid ? 'Enter a valid date in DD/MM/YYYY format (not in the future).' : '');
      textInput.classList.toggle('dob-invalid', invalid);
    } else {
      textInput.setCustomValidity('');
      textInput.classList.remove('dob-invalid');
    }
    return parsed;
  }
  textInput.addEventListener('input', () => {
    const digits = textInput.value.replace(/\D/g, '').slice(0, 8);
    // Readable automatic separators also make pasted 8-digit dates easy to enter.
    const formatted = [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4)].filter(Boolean).join('/');
    if (textInput.value !== formatted) textInput.value = formatted;
    syncTypedDate(false);
  });
  textInput.addEventListener('blur', () => syncTypedDate(true));
  textInput.addEventListener('keydown', event => {
    if (event.key === 'ArrowDown' && popover.hidden) { event.preventDefault(); showCalendar(); }
  });
  const form = native.closest('form');
  if (form) form.addEventListener('submit', event => {
    const parsed = syncTypedDate(true);
    if (!parsed) {
      if (!textInput.value) textInput.setCustomValidity('Please enter your date of birth.');
      event.preventDefault();
      textInput.reportValidity();
      textInput.focus();
    }
  });
  render();
})();
