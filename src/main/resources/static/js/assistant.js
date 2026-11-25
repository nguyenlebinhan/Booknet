// AI assistant bubble. Loads the conversation panel from /assistant when opened and sends its forms in
// the background. It stays open while the reader moves between pages.
(function () {
  const widget = document.getElementById('ai-widget');
  if (!widget) return;
  const OPEN = 'booknet-assistant-open';
  const url = widget.dataset.url;
  const panelWindow = document.getElementById('ai-window');
  const launch = document.getElementById('ai-launch');
  const status = document.getElementById('ai-loading');
  let busy = false;
  let loading = false;

  const find = (selector) => panelWindow.querySelector(selector);

  function remember(open) {
    try {
      sessionStorage.setItem(OPEN, String(open));
    } catch (error) { /* storage is blocked: the bubble just starts closed */ }
  }

  function wasOpen() {
    try {
      return sessionStorage.getItem(OPEN) === 'true';
    } catch (error) {
      return false;
    }
  }

  // Scrolls to the newest question, so its answer starts in view.
  function showLatest() {
    const list = find('#ai-messages');
    const questions = list.querySelectorAll('.from-you');
    list.scrollTop = questions.length ? questions[questions.length - 1].offsetTop - 12 : 0;
  }

  function showError(text) {
    const error = find('#ai-error');
    error.textContent = text;
    error.hidden = !text;
    find('.ai-input').classList.toggle('has-error', Boolean(text));
  }

  async function readPage(options) {
    const response = await fetch(url, options);
    // 403: the session ended, so the form's security token is out of date.
    if (!response.ok) throw new Error(response.status === 403 ? 'expired' : 'failed');
    const page = new DOMParser().parseFromString(await response.text(), 'text/html');
    // No panel: another page came back, for example when the sign-in ended.
    if (!page.getElementById('ai-panel')) throw new Error('expired');
    return page;
  }

  // Returns false when the panel could not be loaded; a panel already shown stays.
  async function load() {
    if (loading) return false;
    loading = true;
    const old = find('#ai-panel');
    if (!old) {
      status.hidden = false;
      status.textContent = 'Opening the assistant…';
    }
    try {
      const panel = (await readPage({})).getElementById('ai-panel');
      panel.querySelector('#ai-close').hidden = false;
      if (old) old.remove();
      panelWindow.append(panel);
      status.hidden = true;
      showLatest();
      return true;
    } catch (error) {
      if (!old) status.textContent = 'The assistant could not be opened. Close the bubble and try again.';
      return false;
    } finally {
      loading = false;
    }
  }

  async function open(focus) {
    panelWindow.hidden = false;
    launch.setAttribute('aria-expanded', 'true');
    remember(true);
    if (!find('#ai-panel')) await load();
    const box = find('#ai-message');
    if (focus && box && !panelWindow.hidden) box.focus({ preventScroll: true });
  }

  function close() {
    panelWindow.hidden = true;
    launch.setAttribute('aria-expanded', 'false');
    remember(false);
    launch.focus({ preventScroll: true });
  }

  function showWaiting(message) {
    const you = document.createElement('div');
    you.className = 'ai-message from-you';
    const bubble = document.createElement('p');
    bubble.className = 'ai-bubble';
    bubble.textContent = message;
    you.append(bubble);
    const wait = document.createElement('p');
    wait.className = 'ai-waiting';
    wait.textContent = 'booknet assistant is thinking…';
    find('#ai-messages').append(you, wait);
    showLatest();
  }

  // Takes the conversation, error and guest allowance from the page the server returned.
  function showPage(page, message) {
    find('#ai-messages').replaceChildren(...page.getElementById('ai-messages').childNodes);
    const error = page.getElementById('ai-error');
    showError(error.hidden ? '' : error.textContent);
    if (error.hidden && message !== null) find('#ai-message').value = '';
    const allowance = find('#ai-allowance');
    const updated = page.getElementById('ai-allowance');
    if (allowance && updated) allowance.textContent = updated.textContent;
    showLatest();
  }

  // message is null for "New conversation".
  async function send(form, message) {
    if (busy) return;
    const body = new URLSearchParams(new FormData(form));
    if (message === null) body.set('reset', 'true');
    busy = true;
    find('.ai-send').disabled = true;
    showError('');
    const before = [...find('#ai-messages').childNodes];
    if (message !== null) showWaiting(message);
    try {
      showPage(await readPage({ method: 'POST', body }), message);
    } catch (error) {
      find('#ai-messages').replaceChildren(...before);
      const draft = find('#ai-message').value;
      // When the session ended, a fresh panel carries a new security token.
      if (error.message === 'expired' && await load()) {
        find('#ai-message').value = draft;
        showError('Your session was renewed. Please try again.');
      } else {
        showError('Your message could not be sent. Check your connection and try again.');
      }
    } finally {
      busy = false;
      find('.ai-send').disabled = false;
      if (!panelWindow.hidden) find('#ai-message').focus({ preventScroll: true });
    }
  }

  function submitMessage() {
    const box = find('#ai-message');
    const message = box.value.trim();
    if (message) {
      send(find('#ai-form'), message);
    } else {
      box.focus();
    }
  }

  launch.addEventListener('click', () => (panelWindow.hidden ? open(true) : close()));

  panelWindow.addEventListener('submit', (event) => {
    event.preventDefault();
    if (event.target.id === 'ai-form') {
      submitMessage();
    } else {
      send(event.target, null);
    }
  });

  panelWindow.addEventListener('click', (event) => {
    if (event.target.closest('#ai-close')) close();
    // Starter questions fill the box so the reader can edit before sending.
    const chip = event.target.closest('.ai-chip');
    if (chip) {
      event.preventDefault();
      find('#ai-message').value = chip.value;
      find('#ai-message').focus();
    }
  });

  panelWindow.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      close();
    } else if (event.target.id === 'ai-message' && event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      submitMessage();
    }
  });

  launch.hidden = false;
  // On a phone the window covers the page, so it does not reopen over the page the reader went to.
  if (wasOpen() && !window.matchMedia('(max-width: 600px)').matches) open(false);
})();
