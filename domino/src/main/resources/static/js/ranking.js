/* Pede confirmação antes de remover alguém do ranking ou limpar o ranking inteiro. */
(function () {
  'use strict';
  document.querySelectorAll('form[data-confirma]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.dataset.confirma)) {
        e.preventDefault();
      }
    });
  });
})();
