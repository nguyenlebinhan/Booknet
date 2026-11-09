(function () {
  // 1. Account type tabs (sign in page)
  const tabs = document.querySelectorAll('.auth-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(other => {
        other.classList.toggle('active', other === tab);
        other.setAttribute('aria-pressed', other === tab);
      });
    });
  });

  // 2. Show / hide password
  document.querySelectorAll('.auth-password-toggle').forEach(toggle => {
    const input = toggle.parentElement.querySelector('input');
    toggle.addEventListener('click', () => {
      const isVisible = input.type === 'password';
      input.type = isVisible ? 'text' : 'password';
      toggle.setAttribute('aria-pressed', isVisible);
      toggle.setAttribute('aria-label', isVisible ? 'Hide password' : 'Show password');
      toggle.querySelector('.auth-eye').hidden = !isVisible;
      toggle.querySelector('.auth-eye-off').hidden = isVisible;
    });
  });

  // 3. Password strength meter (registration page)
  const strength = document.getElementById('password-strength');
  const password = document.getElementById('password');
  if (strength && password) {
    const levels = strength.querySelectorAll('span');
    const update = () => {
      const value = password.value;
      const score = !value ? 0 : [
        value.length >= 8,
        /[a-z]/.test(value) && /[A-Z]/.test(value),
        /\d/.test(value),
        value.length >= 12 || /[^A-Za-z0-9]/.test(value)
      ].filter(Boolean).length;
      levels.forEach((level, index) => level.classList.toggle('active', index < score));
    };
    password.addEventListener('input', update);
    update();
  }

  // 4. Resend countdown (forgot password page)
  const countdown = document.getElementById('resend-countdown');
  if (countdown) {
    let seconds = 60;
    const timer = setInterval(() => {
      seconds -= 1;
      if (seconds <= 0) {
        clearInterval(timer);
        countdown.textContent = 'No email yet? You can send the link again now.';
        return;
      }
      countdown.textContent = `No email yet? You can resend in 00:${String(seconds).padStart(2, '0')}.`;
    }, 1000);
  }
})();
