document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.querySelector('.menu-toggle');
  const nav = document.querySelector('.global-nav');
  if (toggle && nav) {
    toggle.addEventListener('click', () => {
      const expanded = toggle.getAttribute('aria-expanded') === 'true';
      toggle.setAttribute('aria-expanded', String(!expanded));
      nav.classList.toggle('open');
    });
  }

  document.querySelectorAll('a[href^="#"]').forEach((a) => {
    a.addEventListener('click', (e) => {
      const id = a.getAttribute('href');
      const target = id && document.querySelector(id);
      if (!target) return;
      e.preventDefault();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  document.querySelectorAll('.faq-toggle').forEach((btn) => {
    btn.addEventListener('click', () => {
      const panel = btn.closest('.faq-item')?.querySelector('.faq-panel');
      if (!panel) return;
      const expanded = btn.getAttribute('aria-expanded') === 'true';
      btn.setAttribute('aria-expanded', String(!expanded));
      panel.hidden = expanded;
    });
  });

  const form = document.querySelector('#contact-form');
  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = form.querySelector('#name');
      const email = form.querySelector('#email');
      const message = form.querySelector('#message');
      const result = form.querySelector('#form-result');
      const errs = {
        name: form.querySelector('#err-name'),
        email: form.querySelector('#err-email'),
        message: form.querySelector('#err-message')
      };
      Object.values(errs).forEach((n) => (n.textContent = ''));
      let ok = true;
      if (!name.value.trim()) { errs.name.textContent = 'お名前は必須です。'; ok = false; }
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email.value.trim()) { errs.email.textContent = 'メールアドレスは必須です。'; ok = false; }
      else if (!emailPattern.test(email.value.trim())) { errs.email.textContent = 'メール形式が正しくありません。'; ok = false; }
      if (!message.value.trim()) { errs.message.textContent = 'お問い合わせ内容は必須です。'; ok = false; }
      result.textContent = ok ? '送信内容を受け付けました（デモ）。' : '入力内容を確認してください。';
    });
  }
});
