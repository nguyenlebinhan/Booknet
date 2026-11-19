document.querySelectorAll('[data-category-picker]').forEach((picker) => {
  const search = picker.querySelector('[data-category-search]');
  const results = picker.querySelector('[data-category-results]');
  const selectedList = picker.querySelector('[data-selected-category-list]');
  const selectionCount = picker.querySelector('[data-category-count]');
  const pageStatus = picker.querySelector('[data-category-page]');
  const previousButton = picker.querySelector('[data-category-previous]');
  const nextButton = picker.querySelector('[data-category-next]');
  const feedback = picker.querySelector('[data-category-status]');
  const form = picker.closest('form');
  const selected = new Map();
  const descendantCache = new Map();
  let visibleNodes = new Map();
  let page = 0;
  let totalPages = 0;
  let debounceTimer;
  let activeRequest;

  selectedList.querySelectorAll('[data-selected-id]').forEach((chip) => {
    const label = chip.querySelector('[data-selected-label]');
    selected.set(chip.dataset.selectedId, label.textContent);
  });

  function updateSelectionCount() {
    selectionCount.textContent = `${selected.size} selected`;
  }

  function createChip(id, label) {
    const chip = document.createElement('span');
    chip.className = 'category-picker-chip';
    chip.dataset.selectedId = id;

    const text = document.createElement('span');
    text.dataset.selectedLabel = '';
    text.textContent = label;

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.dataset.removeCategory = '';
    removeButton.setAttribute('aria-label', `Remove ${label}`);
    removeButton.textContent = '×';

    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'categoryIds';
    input.value = id;
    input.dataset.selectedInput = '';

    chip.append(text, removeButton, input);
    selectedList.append(chip);
  }

  async function getDescendants(id) {
    if (descendantCache.has(id)) return descendantCache.get(id);
    const url = new URL(`${picker.dataset.descendantsUrl}/${encodeURIComponent(id)}/descendants`, window.location.href);
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
      throw new Error(`Category descendants could not be loaded with status ${response.status}`);
    }
    const descendants = await response.json();
    descendantCache.set(id, descendants);
    return descendants;
  }

  function removeChip(id) {
    const chip = Array.from(selectedList.querySelectorAll('[data-selected-id]'))
      .find((item) => item.dataset.selectedId === id);
    if (chip) chip.remove();
  }

  async function setNodeSelection(node, isSelected) {
    const descendants = node.hasChildren ? await getDescendants(node.id) : [];
    const affectedCategories = [node, ...descendants];
    affectedCategories.forEach((category) => {
      const id = String(category.id);
      if (isSelected) {
        if (!selected.has(id)) {
          selected.set(id, category.label);
          createChip(id, category.label);
        }
      } else {
        selected.delete(id);
        removeChip(id);
      }
    });
    updateSelectionCount();
    syncVisibleOptions();
  }

  function syncVisibleOptions() {
    results.querySelectorAll('[data-category-option-id]').forEach((checkbox) => {
      const id = checkbox.dataset.categoryOptionId;
      const node = visibleNodes.get(id);
      const descendants = descendantCache.get(id) || flattenChildren(node);
      const selectedDescendants = descendants.filter((category) => selected.has(String(category.id))).length;
      checkbox.checked = selected.has(id)
        && (descendants.length === 0 || selectedDescendants === descendants.length);
      checkbox.indeterminate = descendants.length > 0
        && ((selected.has(id) && selectedDescendants < descendants.length)
          || (!selected.has(id) && selectedDescendants > 0));
    });
  }

  function flattenChildren(node) {
    const descendants = [];
    function collect(children) {
      children.forEach((child) => {
        descendants.push(child);
        collect(child.children);
      });
    }
    collect(node.children);
    return descendants;
  }

  function renderOptions(categories) {
    results.replaceChildren();
    if (categories.length === 0) {
      const emptyMessage = document.createElement('p');
      emptyMessage.className = 'category-picker-empty';
      emptyMessage.textContent = 'No categories found.';
      results.append(emptyMessage);
      return;
    }

    const nodes = new Map();
    categories.forEach((category) => {
      const id = String(category.id);
      nodes.set(id, { ...category, id, children: [] });
    });

    categories.forEach((category) => {
      if (category.parentId === null || category.parentId === undefined) return;
      const parentId = String(category.parentId);
      if (!nodes.has(parentId)) {
        nodes.set(parentId, {
          id: parentId,
          name: category.parentName,
          label: category.parentLabel,
          parentId: null,
          hasChildren: true,
          children: []
        });
      }
    });

    const roots = [];
    nodes.forEach((node) => {
      const parent = node.parentId === null || node.parentId === undefined
        ? null
        : nodes.get(String(node.parentId));
      if (parent) parent.children.push(node);
      else roots.push(node);
    });

    function renderNode(node, container) {
      const branch = document.createElement('div');
      branch.className = 'category-picker-node';
      visibleNodes.set(node.id, node);

      const row = document.createElement('label');
      row.className = 'category-picker-option';

      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.dataset.categoryOptionId = node.id;
      checkbox.checked = selected.has(node.id);
      checkbox.addEventListener('change', async () => {
        const isSelected = checkbox.checked;
        checkbox.disabled = true;
        feedback.textContent = '';
        try {
          await setNodeSelection(node, isSelected);
        } catch (error) {
          checkbox.checked = !isSelected;
          feedback.textContent = 'Subcategories could not be updated. Please try again.';
        } finally {
          checkbox.disabled = false;
          syncVisibleOptions();
        }
      });

      const text = document.createElement('span');
      text.textContent = node.name;
      row.append(checkbox, text);
      branch.append(row);

      if (node.children.length > 0) {
        const children = document.createElement('div');
        children.className = 'category-picker-children';
        node.children.sort((first, second) => first.name.localeCompare(second.name));
        node.children.forEach((child) => renderNode(child, children));
        branch.append(children);
      }
      container.append(branch);
    }

    visibleNodes = new Map();
    roots.sort((first, second) => first.name.localeCompare(second.name));
    roots.forEach((root) => renderNode(root, results));
    syncVisibleOptions();
  }

  async function loadPage() {
    if (activeRequest) activeRequest.abort();
    activeRequest = new AbortController();
    const request = activeRequest;
    const url = new URL(picker.dataset.optionsUrl, window.location.href);
    url.searchParams.set('query', search.value.trim());
    url.searchParams.set('page', String(page));

    feedback.textContent = '';
    results.textContent = 'Loading categories...';
    previousButton.disabled = true;
    nextButton.disabled = true;
    pageStatus.textContent = 'Loading...';

    try {
      const response = await fetch(url, {
        headers: { Accept: 'application/json' },
        signal: request.signal
      });
      if (!response.ok) {
        throw new Error(`Category search failed with status ${response.status}`);
      }

      const data = await response.json();
      page = data.page;
      totalPages = data.totalPages;
      renderOptions(data.categories);
      pageStatus.textContent = `Page ${page + 1} of ${Math.max(totalPages, 1)} · ${data.totalElements} categories`;
      previousButton.disabled = page <= 0;
      nextButton.disabled = totalPages === 0 || page + 1 >= totalPages;
    } catch (error) {
      if (error.name === 'AbortError') return;
      results.replaceChildren();
      pageStatus.textContent = '';
      feedback.textContent = 'Categories could not be loaded. Please try again.';
      previousButton.disabled = true;
      nextButton.disabled = true;
    }
  }

  selectedList.addEventListener('click', (event) => {
    const button = event.target.closest('[data-remove-category]');
    if (!button) return;
    const chip = button.closest('[data-selected-id]');
    if (!chip) return;
    const id = chip.dataset.selectedId;
    const node = visibleNodes.get(id) || { id, hasChildren: true };
    setNodeSelection(node, false).catch(() => {
      feedback.textContent = 'Subcategories could not be updated. Please try again.';
    });
  });

  search.addEventListener('input', () => {
    page = 0;
    window.clearTimeout(debounceTimer);
    debounceTimer = window.setTimeout(loadPage, 250);
  });

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
      feedback.textContent = 'Select at least one category.';
      search.focus();
    }
  });

  updateSelectionCount();
  loadPage();
});
