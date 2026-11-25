document.querySelectorAll('[data-summary-name]').forEach(input => {
  const update = () => document.querySelectorAll('[data-name-preview]').forEach(element => {
    element.textContent = input.value.trim() || element.dataset.emptyLabel || 'New account';
  });
  input.addEventListener('input', update);
  update();
});

document.querySelectorAll('[data-summary-email]').forEach(input => {
  const update = () => document.querySelectorAll('[data-email-preview]').forEach(element => {
    element.textContent = input.value.trim() || 'No email entered';
  });
  input.addEventListener('input', update);
  update();
});

document.querySelectorAll('[data-summary-role]').forEach(input => {
  const update = () => document.querySelectorAll('[data-role-preview]').forEach(element => {
    element.textContent = input.selectedOptions[0]?.textContent || 'Not selected';
  });
  input.addEventListener('change', update);
  update();
});

document.querySelectorAll('[data-review-form]').forEach(form => {
  form.querySelector('[data-review]')?.addEventListener('click', () => {
    const reason = form.querySelector('[name=reason]');
    if (!reason.reportValidity()) return;
    form.querySelector('[data-confirm]').hidden = false;
  });
});

// Open the inline profile editor when its heading action is followed.
function openProfileEditor() {
  const editor = document.getElementById('profile-edit');
  if (editor) editor.open = true;
}
document.querySelectorAll('a[href="#profile-edit"]').forEach(link => {
  link.addEventListener('click', openProfileEditor);
});
if (location.hash === '#profile-edit') openProfileEditor();
