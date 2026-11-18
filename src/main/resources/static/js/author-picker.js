document.querySelectorAll('[data-author-picker]').forEach((picker) => {
  const search = picker.querySelector('[data-author-search]');
  const results = picker.querySelector('[data-author-results]');
  const selectedList = picker.querySelector('[data-selected-author-list]');
  const selectionCount = picker.querySelector('[data-author-count]');
  const pageStatus = picker.querySelector('[data-author-page]');
  const previousButton = picker.querySelector('[data-author-previous]');
  const nextButton = picker.querySelector('[data-author-next]');
  const feedback = picker.querySelector('[data-author-status]');
  const form = picker.closest('form');
  const selected = new Map();
  let page = 0;
  let totalPages = 0;
  let debounceTimer;
  let activeRequest;

  selectedList.querySelectorAll('[data-selected-id]').forEach((chip) => {
    selected.set(chip.dataset.selectedId, chip.querySelector('[data-selected-label]').textContent);
  });

  function updateSelectionCount() {
    selectionCount.textContent = `${selected.size} selected`;
  }

  function removeAuthor(id) {
    const chip = Array.from(selectedList.querySelectorAll('[data-selected-id]'))
      .find((item) => item.dataset.selectedId === id);
    if (chip) chip.remove();
    selected.delete(id);
    const checkbox = Array.from(results.querySelectorAll('[data-author-option-id]'))
      .find((item) => item.dataset.authorOptionId === id);
    if (checkbox) checkbox.checked = false;
    updateSelectionCount();
  }

  function addAuthor(author) {
    const id = String(author.id);
    if (selected.has(id)) return;
    selected.set(id, author.name);

    const chip = document.createElement('span');
    chip.className = 'author-picker-chip';
    chip.dataset.selectedId = id;

    const name = document.createElement('span');
    name.dataset.selectedLabel = '';
    name.textContent = author.name;

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.dataset.removeAuthor = '';
    removeButton.setAttribute('aria-label', `Remove ${author.name}`);
    removeButton.textContent = '×';

    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'authorIds';
    input.value = id;

    chip.append(name, removeButton, input);
    selectedList.append(chip);
    updateSelectionCount();
  }

  function renderAuthors(authors) {
    results.replaceChildren();
    if (authors.length === 0) {
      const emptyMessage = document.createElement('p');
      emptyMessage.className = 'author-picker-empty';
      emptyMessage.textContent = 'No authors found. Create an author first.';
      results.append(emptyMessage);
      return;
    }

    authors.forEach((author) => {
      const id = String(author.id);
      const option = document.createElement('label');
      option.className = 'author-picker-option';

      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.dataset.authorOptionId = id;
      checkbox.checked = selected.has(id);
      checkbox.addEventListener('change', () => {
        if (checkbox.checked) addAuthor(author);
        else removeAuthor(id);
      });

      const name = document.createElement('span');
      name.textContent = author.name;
      option.append(checkbox, name);
      results.append(option);
    });
  }

  async function loadPage() {
    if (activeRequest) activeRequest.abort();
    activeRequest = new AbortController();
    const request = activeRequest;
    const url = new URL(picker.dataset.optionsUrl, window.location.href);
    url.searchParams.set('query', search.value.trim());
    url.searchParams.set('page', String(page));

    feedback.textContent = '';
    results.textContent = 'Loading authors...';
    previousButton.disabled = true;
    nextButton.disabled = true;
    pageStatus.textContent = 'Loading...';

    try {
      const response = await fetch(url, {
        headers: { Accept: 'application/json' },
        signal: request.signal
      });
      if (!response.ok) {
        throw new Error(`Author search failed with status ${response.status}`);
      }

      const data = await response.json();
      page = data.page;
      totalPages = data.totalPages;
      renderAuthors(data.authors);
      pageStatus.textContent = `Page ${page + 1} of ${Math.max(totalPages, 1)} · ${data.totalElements} authors`;
      previousButton.disabled = page <= 0;
      nextButton.disabled = totalPages === 0 || page + 1 >= totalPages;
    } catch (error) {
      if (error.name === 'AbortError') return;
      results.replaceChildren();
      pageStatus.textContent = '';
      feedback.textContent = 'Authors could not be loaded. Please try again.';
      previousButton.disabled = true;
      nextButton.disabled = true;
    }
  }

  selectedList.addEventListener('click', (event) => {
    const button = event.target.closest('[data-remove-author]');
    if (!button) return;
    const chip = button.closest('[data-selected-id]');
    if (chip) removeAuthor(chip.dataset.selectedId);
  });

  search.addEventListener('input', () => {
    page = 0;
    window.clearTimeout(debounceTimer);
    debounceTimer = window.setTimeout(loadPage, 250);
  });

  window.addEventListener('focus', loadPage);

  previousButton.addEventListener('click', () => {
    if (page > 0) {
      page -= 1;
      loadPage();
    }
  });

  nextButton.addEventListener('click', () => {
    if (page + 1 < totalPages) {
      page += 1;
      loadPage();
    }
  });

  form.addEventListener('submit', (event) => {
    if (selected.size === 0) {
      event.preventDefault();
      feedback.textContent = 'Select at least one author.';
      search.focus();
    }
  });

  updateSelectionCount();
  loadPage();
});
