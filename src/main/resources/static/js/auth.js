(function () {
  // Keep native validation feedback in English, regardless of the browser language.
  document.querySelectorAll('form input, form textarea, form select').forEach(input => {
    const clearMessage = () => input.setCustomValidity('');
    input.addEventListener('input', clearMessage);
    input.addEventListener('change', clearMessage);
    input.addEventListener('invalid', () => {
      clearMessage();
      const validity = input.validity;
      let message = 'Please enter a valid value.';
      if (validity.valueMissing) {
        message = input.type === 'checkbox' ? 'Please select this checkbox to continue.' : 'Please fill out this field.';
      } else if (validity.typeMismatch) {
        message = input.type === 'email' ? 'Please enter a valid email address.' : 'Please enter a valid URL.';
      } else if (validity.tooShort) {
        message = `Please enter at least ${input.minLength} characters.`;
      } else if (validity.tooLong) {
        message = `Please enter no more than ${input.maxLength} characters.`;
      } else if (validity.patternMismatch) {
        if (input.name === 'password' || input.name === 'newPassword') {
          message = 'Password must be 8–32 characters and include uppercase and lowercase letters and a number; maximum 72 UTF-8 bytes.';
        } else if (input.name === 'phone') {
          message = 'Please enter a valid phone number.';
        } else {
          message = input.title || 'Please use the requested format.';
        }
      }
      input.setCustomValidity(message);
    });
  });

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
