/**
 * BookNet - Library Services Platform
 * Interactive Frontend Logic (Week 1)
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const navbar = document.getElementById('navbar');
  const hamburgerBtn = document.getElementById('hamburger-btn');
  const navLinks = document.getElementById('nav-links');
  const themeToggleBtn = document.getElementById('theme-toggle-btn');
  const themeIcon = document.getElementById('theme-icon');

  // Auth Modal Elements
  const authModal = document.getElementById('auth-modal');
  const navAuthBtn = document.getElementById('nav-auth-btn');
  const heroCtaBtn = document.getElementById('hero-cta-btn');
  const heroExploreBtn = document.getElementById('hero-explore-btn');
  const modalCloseBtn = document.getElementById('modal-close-btn');
  const tabLogin = document.getElementById('tab-login');
  const tabSignup = document.getElementById('tab-signup');
  const formLogin = document.getElementById('form-login');
  const formSignup = document.getElementById('form-signup');
  const loginEmailInput = document.getElementById('login-email');
  const signupEmailInput = document.getElementById('signup-email');
  const userProfileBadge = document.getElementById('user-profile-badge');
  const userNameDisplay = document.getElementById('user-name-display');
  const logoutBtn = document.getElementById('logout-btn');

  // Scroll to top
  const scrollTopBtn = document.getElementById('scroll-to-top-btn');

  // Catalog search & filter
  const searchInput = document.getElementById('book-search-input');
  const filterBtns = document.querySelectorAll('.filter-btn');
  const bookCards = document.querySelectorAll('.book-card');
  const noResultsMsg = document.getElementById('no-results-msg');

  // ===================================================================
  // 1. THEME SWITCHER (Dark / Light)
  // ===================================================================
  const savedTheme = localStorage.getItem('booknet_theme') || 'dark';
  applyTheme(savedTheme);

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(newTheme);
      localStorage.setItem('booknet_theme', newTheme);
    });
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    if (themeIcon) {
      if (theme === 'light') {
        // Show Moon icon (to switch to dark)
        themeIcon.innerHTML = `
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
            d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
        `;
      } else {
        // Show Sun icon (to switch to light)
        themeIcon.innerHTML = `
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
            d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
        `;
      }
    }
  }

  // ===================================================================
  // 2. NAVBAR SCROLL EFFECT & MOBILE MENU
  // ===================================================================
  window.addEventListener('scroll', () => {
    if (window.scrollY > 40) {
      navbar.classList.add('scrolled');
    } else {
      navbar.classList.remove('scrolled');
    }

    // Scroll-to-top floating button visibility
    if (window.scrollY > 280) {
      scrollTopBtn.classList.add('visible');
    } else {
      scrollTopBtn.classList.remove('visible');
    }
  });

  if (hamburgerBtn && navLinks) {
    hamburgerBtn.addEventListener('click', () => {
      navLinks.classList.toggle('active');
    });

    // Close mobile menu on link click
    navLinks.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        navLinks.classList.remove('active');
      });
    });
  }

  // ===================================================================
  // 3. SCROLL-TO-TOP BUTTON
  // ===================================================================
  if (scrollTopBtn) {
    scrollTopBtn.addEventListener('click', () => {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    });
  }

  // ===================================================================
  // 4. LOGIN / SIGN UP MODAL & FORM SWITCHING
  // ===================================================================
  function openAuthModal(mode = 'login') {
    if (mode === 'signup') {
      switchTab('signup');
    } else {
      switchTab('login');
    }
    authModal.classList.add('active');
    document.body.style.overflow = 'hidden';
  }

  function closeAuthModal() {
    authModal.classList.remove('active');
    document.body.style.overflow = '';
  }

  function switchTab(mode) {
    if (mode === 'login') {
      tabLogin.classList.add('active');
      tabSignup.classList.remove('active');
      formLogin.classList.add('active');
      formSignup.classList.remove('active');
      setTimeout(() => loginEmailInput && loginEmailInput.focus(), 100);
    } else {
      tabSignup.classList.add('active');
      tabLogin.classList.remove('active');
      formSignup.classList.add('active');
      formLogin.classList.remove('active');
      setTimeout(() => signupEmailInput && signupEmailInput.focus(), 100);
    }
  }

  if (navAuthBtn) {
    navAuthBtn.addEventListener('click', () => openAuthModal('login'));
  }

  if (heroCtaBtn) {
    heroCtaBtn.addEventListener('click', () => openAuthModal('signup'));
  }

  if (modalCloseBtn) {
    modalCloseBtn.addEventListener('click', closeAuthModal);
  }

  // Close modal when clicking on dark backdrop
  if (authModal) {
    authModal.addEventListener('click', (e) => {
      if (e.target === authModal) {
        closeAuthModal();
      }
    });
  }

  // Escape key closes modal
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && authModal.classList.contains('active')) {
      closeAuthModal();
    }
  });

  // Tab buttons click
  if (tabLogin) {
    tabLogin.addEventListener('click', () => switchTab('login'));
  }
  if (tabSignup) {
    tabSignup.addEventListener('click', () => switchTab('signup'));
  }

  // Switch links inside forms
  document.querySelectorAll('[data-switch-to]').forEach(el => {
    el.addEventListener('click', (e) => {
      e.preventDefault();
      const target = el.getAttribute('data-switch-to');
      switchTab(target);
    });
  });

  // ===================================================================
  // 5. SIGN UP & LOGIN FORMS - SPRING MVC BACKEND SUBMISSION
  // Backend xử lý logic và trả về thông báo alert theo yêu cầu:
  // - Sign up -> "You registered successfully!"
  // - Login -> "Welcome to home page!"
  // ===================================================================
  if (formSignup) {
    formSignup.addEventListener('submit', (e) => {
      const password = document.getElementById('signup-password').value;
      const confirmPassword = document.getElementById('signup-confirm-password').value;

      if (password !== confirmPassword) {
        e.preventDefault();
        alert('Mật khẩu xác nhận không khớp! Vui lòng kiểm tra lại.');
        return false;
      }
      // Form submits normally via POST to /register on Spring MVC backend
    });
  }

  // Logout handler via backend link/action
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      // Allow navigation to /logout handled by Spring MVC HomeController
    });
  }

  // Logout handler
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      if (userProfileBadge) userProfileBadge.style.display = 'none';
      if (navAuthBtn) navAuthBtn.style.display = 'inline-flex';
      showToast('Bạn đã đăng xuất khỏi BookNet.', 'info');
    });
  }

  // ===================================================================
  // 7. TOAST NOTIFICATION SYSTEM
  // ===================================================================
  function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let iconSvg = '';
    if (type === 'success') {
      iconSvg = `
        <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
        </svg>
      `;
    } else if (type === 'warning') {
      iconSvg = `
        <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      `;
    } else {
      iconSvg = `
        <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      `;
    }

    toast.innerHTML = `
      <div class="toast-icon">${iconSvg}</div>
      <div class="toast-content">
        <div class="toast-message">${message}</div>
        <div class="toast-subtext">BookNet Library Services</div>
      </div>
    `;

    container.appendChild(toast);

    // Trigger enter animation
    setTimeout(() => toast.classList.add('show'), 10);

    // Auto dismiss
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  // ===================================================================
  // 8. INTERACTIVE CATALOG SEARCH & FILTER
  // ===================================================================
  let currentCategory = 'all';

  function filterBooks() {
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
    let visibleCount = 0;

    bookCards.forEach(card => {
      const title = card.getAttribute('data-title')?.toLowerCase() || '';
      const author = card.getAttribute('data-author')?.toLowerCase() || '';
      const category = card.getAttribute('data-category') || '';

      const matchesCategory = currentCategory === 'all' || category === currentCategory;
      const matchesQuery = query === '' || title.includes(query) || author.includes(query) || category.includes(query);

      if (matchesCategory && matchesQuery) {
        card.style.display = 'flex';
        visibleCount++;
      } else {
        card.style.display = 'none';
      }
    });

    if (noResultsMsg) {
      noResultsMsg.style.display = visibleCount === 0 ? 'block' : 'none';
    }
  }

  if (searchInput) {
    searchInput.addEventListener('input', filterBooks);
  }

  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      filterBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentCategory = btn.getAttribute('data-category') || 'all';
      filterBooks();
    });
  });

  // Borrow & Reserve Action Handlers
  document.querySelectorAll('.btn-borrow').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const bookCard = e.target.closest('.book-card');
      const bookTitle = bookCard ? bookCard.getAttribute('data-title') : 'Tựa sách';
      showToast(`Đã ghi nhận yêu cầu mượn "${bookTitle}". Hạn trả: 14 ngày.`, 'success');
    });
  });

  document.querySelectorAll('.btn-reserve').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const bookCard = e.target.closest('.book-card');
      const bookTitle = bookCard ? bookCard.getAttribute('data-title') : 'Tựa sách';
      showToast(`Đã đặt trước "${bookTitle}". Thông báo sẽ gửi khi sách có sẵn!`, 'info');
    });
  });

  // Contact form submission
  const contactForm = document.getElementById('contact-form');
  if (contactForm) {
    contactForm.addEventListener('submit', (e) => {
      e.preventDefault();
      showToast('Cảm ơn bạn! Tin nhắn đã được gửi đến ban quản lý thư viện BookNet.', 'success');
      contactForm.reset();
    });
  }
});
