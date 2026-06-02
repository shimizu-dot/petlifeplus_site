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
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = form.querySelector('#name');
      const email = form.querySelector('#email');
      const message = form.querySelector('#message');
      const result = form.querySelector('#form-result');
      const submit = form.querySelector('button[type="submit"]');
      const errs = {
        name: form.querySelector('#err-name'),
        email: form.querySelector('#err-email'),
        message: form.querySelector('#err-message')
      };
      Object.values(errs).forEach((n) => (n.textContent = ''));
      result.textContent = '';
      let ok = true;
      if (!name.value.trim()) { errs.name.textContent = 'お名前は必須です。'; ok = false; }
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email.value.trim()) { errs.email.textContent = 'メールアドレスは必須です。'; ok = false; }
      else if (!emailPattern.test(email.value.trim())) { errs.email.textContent = 'メール形式が正しくありません。'; ok = false; }
      if (!message.value.trim()) { errs.message.textContent = 'お問い合わせ内容は必須です。'; ok = false; }
      if (!ok) {
        result.textContent = '入力内容を確認してください。';
        return;
      }

      if (submit) submit.disabled = true;
      result.textContent = '送信中です...';
      try {
        const res = await fetch('/api/contact', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: name.value.trim(),
            email: email.value.trim(),
            message: message.value.trim()
          })
        });
        if (!res.ok) {
          let errorMessage = '送信に失敗しました。時間をおいて再度お試しください。';
          try {
            const data = await res.json();
            if (data && data.message) errorMessage = data.message;
          } catch (_e) {}
          result.textContent = errorMessage;
          return;
        }
        form.reset();
        result.textContent = 'お問い合わせを送信しました。通常2営業日以内にご返信します。';
      } catch (_e) {
        result.textContent = '通信に失敗しました。ネットワーク接続をご確認ください。';
      } finally {
        if (submit) submit.disabled = false;
      }
    });
  }
});
