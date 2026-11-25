(() => {
  const form = document.getElementById('email-composer-form');
  if (!form) return;
  const subject = form.elements.subjectTemplate;
  const body = form.elements.bodyTemplate;
  const status = document.getElementById('composer-sync-status');
  // Named action buttons shadow HTMLFormElement.action with a RadioNodeList.
  const actionUrl = new URL(form.getAttribute('action'), document.baseURI).href;
  let loadedType = form.elements.templateType.value;
  let saved = JSON.stringify([subject.value, body.value]);
  let timer, version = 0, queue = Promise.resolve(), submitting = false;
  const notice = (message, error = false) => {
    status.textContent = message;
    status.classList.toggle('is-error', error);
  };
  const request = async (url, data) => {
    const headers = {};
    if (form.dataset.csrfHeader && form.dataset.csrfToken) {
      headers[form.dataset.csrfHeader] = form.dataset.csrfToken;
    }
    const response = await fetch(url, {
      method: 'POST', body: new URLSearchParams(data), headers, credentials: 'same-origin'
    });
    if (response.redirected || response.status === 401) {
      throw new Error('Your session has expired. Sign in again to continue.');
    }
    if (response.status === 403) {
      throw new Error('Could not authorize this update. Reload the page and try again.');
    }
    if (!response.ok && !response.headers.get('content-type')?.includes('application/json')) {
      throw new Error('Could not update the preview. Please try again.');
    }
    return response;
  };
  async function synchronize() {
    const currentVersion = version;
    const data = new FormData(form);
    let desiredType = data.get('templateType');
    const wording = JSON.stringify([subject.value, body.value]);
    let saveError;
    notice('Updating...');
    // Save the editor under the template it belongs to before loading another one.
    if (wording !== saved) {
      const draft = new FormData(form);
      draft.set('templateType', loadedType);
      const response = await request(actionUrl + '/template', draft);
      const result = await response.json();
      if (!response.ok) {
        saveError = result.message || 'Template could not be saved.';
        // Keep an invalid draft editable instead of silently discarding it on a switch.
        desiredType = loadedType;
        data.set('templateType', loadedType);
        form.elements.templateType.value = loadedType;
      } else saved = wording;
    }
    const switching = desiredType !== loadedType;
    data.set('action', switching ? 'switch-template' : 'filter');
    data.set('reviewStep', 'false');
    const response = await request(actionUrl, data);
    const page = new DOMParser().parseFromString(await response.text(), 'text/html');
    const incoming = page.getElementById('email-composer-form');
    if (!incoming) throw new Error('Could not refresh the preview. Please reload the page.');
    if (currentVersion !== version) return;
    const focus = document.activeElement;
    const focusId = focus?.id;
    const selection = focus?.tagName === 'INPUT' && focus.type === 'text'
      ? [focus.selectionStart, focus.selectionEnd] : null;
    for (const selector of ['[data-composer-recipients]', '[data-composer-review]',
      '[data-composer-validation]', '.emc-preview-card']) {
      form.querySelector(selector).replaceWith(incoming.querySelector(selector));
    }
    if (switching) {
      subject.value = incoming.elements.subjectTemplate.value;
      body.value = incoming.elements.bodyTemplate.value;
      loadedType = desiredType;
      saved = JSON.stringify([subject.value, body.value]);
    }
    form.querySelectorAll('.emc-tpl-option').forEach(label =>
      label.classList.toggle('is-selected', label.querySelector('input').checked));
    form.elements.reviewStep.value = 'false';
    if (focusId && !focus?.isConnected) {
      const restored = document.getElementById(focusId);
      restored?.focus({preventScroll: true});
      if (selection) restored?.setSelectionRange(...selection);
    }
    if (saveError) throw new Error(saveError);
    notice('All changes saved');
  }
  function enqueue() {
    queue = queue.catch(() => {}).then(synchronize).catch(error => {
      form.elements.templateType.value = loadedType;
      subject.readOnly = body.readOnly = false;
      notice(error.message || 'Could not update. Please try again.', true);
      throw error;
    });
    // Display errors without leaving an unhandled rejection during typing.
    queue.catch(() => {});
    return queue;
  }
  function changed(event) {
    if (!event.target.matches('[name=templateType], [name=recipientMode], [name=selectedMemberId], [name=searchKeyword], [name=subjectTemplate], [name=bodyTemplate]')) return;
    version++;
    clearTimeout(timer);
    form.elements.reviewStep.value = 'false';
    form.querySelector('#confirm-dispatch-box')?.remove();
    const switching = form.elements.templateType.value !== loadedType;
    subject.readOnly = body.readOnly = switching;
    notice('Changes pending...');
    timer = setTimeout(() => enqueue().finally(() => {
      subject.readOnly = body.readOnly = form.elements.templateType.value !== loadedType;
    }).catch(() => {}), event.type === 'input' ? 650 : 0);
  }
  form.addEventListener('input', changed);
  form.addEventListener('change', event => {
    if (!event.target.matches('[name=searchKeyword], [name=subjectTemplate], [name=bodyTemplate]')) changed(event);
  });
  form.addEventListener('submit', async event => {
    if (submitting) return;
    event.preventDefault();
    clearTimeout(timer);
    const button = event.submitter;
    // Reset deliberately discards any pending local edits after in-flight writes finish.
    if (button?.value === 'reset-template') {
      try { await queue; } catch (_) { /* Reset can recover an invalid draft. */ }
    } else {
      try {
        // A reviewed, unchanged form can dispatch without invalidating its confirmation.
        if (form.elements.reviewStep.value !== 'true') await enqueue();
        else await queue;
      } catch (_) { return; }
    }
    submitting = true;
    // Preview refresh replaces the review buttons; use the equivalent current button.
    const submitter = button?.name
      ? [...form.querySelectorAll('button')].find(b => b.name === button.name && b.value === button.value)
      : form.querySelector('#confirm-dispatch-box button[type=submit]');
    if (submitter) form.requestSubmit(submitter);
    else submitting = false;
  });
})();
