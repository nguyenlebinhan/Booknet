// Book details tabs. Without this script every panel stays visible, one under another.
(function () {
  const tabs = Array.from(document.querySelectorAll('.details-tab-list [role="tab"]'));
  if (!tabs.length) return;
  const panels = tabs.map((tab) => document.getElementById(tab.getAttribute('aria-controls')));

  function select(index, focus) {
    tabs.forEach((tab, i) => {
      const current = i === index;
      tab.classList.toggle('is-current', current);
      tab.setAttribute('aria-selected', String(current));
      tab.tabIndex = current ? 0 : -1;
      panels[i].hidden = !current;
    });
    if (focus) tabs[index].focus();
  }

  tabs.forEach((tab, i) => {
    tab.addEventListener('click', (event) => {
      event.preventDefault();
      select(i, false);
      history.replaceState(null, '', tab.getAttribute('href'));
    });
    tab.addEventListener('keydown', (event) => {
      if (event.key === 'ArrowRight') select((i + 1) % tabs.length, true);
      if (event.key === 'ArrowLeft') select((i - 1 + tabs.length) % tabs.length, true);
    });
  });

  const fromHash = tabs.findIndex((tab) => tab.getAttribute('href') === location.hash);
  select(fromHash >= 0 ? fromHash : 0, false);
})();
