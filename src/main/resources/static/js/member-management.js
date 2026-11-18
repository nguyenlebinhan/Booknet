document.querySelectorAll('[data-card]').forEach(button => {
  button.addEventListener('click', () => {
    document.getElementById('card-filter').value = button.dataset.card;
  });
});
