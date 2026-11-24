// AI assistant (UI only). Shows the message and the fixed reply straight away instead of
// reloading the page. Without this script the forms post and the server shows the same reply.
(function () {
  const form = document.getElementById('ai-form');
  const list = document.getElementById('ai-messages');
  const box = document.getElementById('ai-message');
  const script = document.getElementById('ai-script');
  if (!form || !list || !box || !script) return;
  const reply = script.dataset.reply;
  const searchUrl = script.dataset.search;

  function bubble(text, mine) {
    const wrap = document.createElement('div');
    wrap.className = 'ai-message ' + (mine ? 'from-you' : 'from-bot');
    const body = mine ? wrap : document.createElement('div');
    if (!mine) {
      const avatar = document.createElement('span');
      avatar.className = 'ai-avatar';
      avatar.setAttribute('aria-hidden', 'true');
      avatar.innerHTML = '<svg viewBox="0 0 24 24" width="16" height="16"><path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/></svg>';
      body.className = 'ai-bot-body';
      wrap.append(avatar, body);
    }
    const meta = document.createElement('p');
    meta.className = 'ai-meta';
    meta.textContent = mine ? 'You' : 'booknet assistant';
    body.append(meta);
    if (mine) {
      const p = document.createElement('p');
      p.className = 'ai-bubble';
      p.textContent = text;
      body.append(p);
    } else {
      const replyBox = document.createElement('div');
      replyBox.className = 'ai-reply';
      const p = document.createElement('p');
      p.textContent = reply;
      const link = document.createElement('a');
      link.href = searchUrl + '?keyword=' + encodeURIComponent(text.slice(0, 80));
      link.textContent = 'Search the catalog for your question ↗';
      const linkLine = document.createElement('p');
      linkLine.append(link);
      replyBox.append(p, linkLine);
      body.append(replyBox);
    }
    list.append(wrap);
    wrap.scrollIntoView({ block: 'nearest' });
  }

  function send(text) {
    const message = text.trim();
    if (!message) {
      box.focus();
      return;
    }
    bubble(message, true);
    bubble(message, false);
    box.value = '';
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    send(box.value);
  });

  box.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      send(box.value);
    }
  });

  // Starter questions and chips fill the box so the reader can edit before sending.
  document.querySelectorAll('.ai-starter, .ai-chip').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.preventDefault();
      box.value = button.value;
      box.focus();
    });
  });
})();
