(() => {
  const dialog = document.querySelector("[data-author-photo-dialog]");
  const image = dialog?.querySelector("[data-author-photo-image]");
  const closeButton = dialog?.querySelector("[data-author-photo-close]");

  if (!(dialog instanceof HTMLDialogElement)
      || !(image instanceof HTMLImageElement)
      || !(closeButton instanceof HTMLButtonElement)) {
    return;
  }

  document.addEventListener("click", (event) => {
    const trigger = event.target instanceof Element
      ? event.target.closest("[data-author-photo-src]")
      : null;

    if (!(trigger instanceof HTMLButtonElement)) {
      return;
    }

    const imageUrl = trigger.dataset.authorPhotoSrc;
    if (!imageUrl) {
      return;
    }

    image.src = imageUrl;
    image.alt = trigger.dataset.authorPhotoAlt || "Author photo";
    dialog.showModal();
  });

  closeButton.addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
      dialog.close();
    }
  });
  dialog.addEventListener("close", () => {
    image.removeAttribute("src");
    image.alt = "";
  });
})();
