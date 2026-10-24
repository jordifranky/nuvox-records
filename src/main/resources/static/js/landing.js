(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const dialogo = $('dlg-publico');
  const formulario = $('form-publico');
  const boton = $('public-submit');
  let enviando = false;
  let enviado = false;
  let modificado = false;

  function mensaje(texto, tipo = '') {
    const el = $('public-msg');
    el.textContent = texto;
    el.className = `mensaje ${tipo}`;
  }
  function restaurar() {
    formulario.reset();
    enviado = false;
    modificado = false;
    $('public-exito').classList.add('oculto');
    $('public-fields').classList.remove('oculto');
    $('public-form-actions').classList.remove('oculto');
    boton.disabled = false;
    boton.textContent = 'Enviar';
    mensaje('');
  }
  function abrir() { restaurar(); dialogo.showModal(); }
  function cerrar() {
    if (enviando) return; // No cerrar mientras el servidor procesa el envío.
    if (!enviado && modificado && !window.confirm('Hay información sin enviar. ¿Quieres descartarla?')) return;
    dialogo.close();
    restaurar();
  }
  ['home-enviar-nav', 'home-enviar', 'home-enviar-footer'].forEach(id => $(id)?.addEventListener('click', abrir));
  ['public-cerrar', 'public-cancelar', 'public-listo'].forEach(id => $(id)?.addEventListener('click', cerrar));
  $('public-nuevo').addEventListener('click', restaurar);
  formulario.addEventListener('input', () => { modificado = true; });
  dialogo.addEventListener('cancel', event => { event.preventDefault(); cerrar(); });
  $('home-acceso').addEventListener('click', () => {
    $('vista-inicio').classList.add('oculto');
    $('vista-login').classList.remove('oculto');
    $('login-user').focus();
  });
  formulario.addEventListener('submit', async event => {
    event.preventDefault();
    if (enviando || enviado || !formulario.reportValidity()) return;
    const datos = Object.fromEntries(new FormData(formulario).entries());
    enviando = true;
    boton.disabled = true;
    boton.textContent = 'Enviando…';
    $('public-cancelar').disabled = true;
    mensaje('Estamos enviando tu demo. Espera un momento…');
    try {
      const respuesta = await fetch('/api/public/demos', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(datos)
      });
      const cuerpo = await respuesta.json().catch(() => ({}));
      if (!respuesta.ok) throw new Error(cuerpo.mensaje || cuerpo.message || 'No pudimos enviar tu demo. Inténtalo de nuevo.');
      enviado = true;
      modificado = false;
      $('public-codigo').textContent = cuerpo.codigoExpediente || 'Envío registrado';
      $('public-fields').classList.add('oculto');
      $('public-form-actions').classList.add('oculto');
      $('public-exito').classList.remove('oculto');
      mensaje('');
    } catch (error) {
      mensaje(error.message || 'No se pudo completar el envío. Inténtalo de nuevo.', 'error');
    } finally {
      enviando = false;
      boton.disabled = false;
      $('public-cancelar').disabled = false;
      boton.textContent = 'Enviar';
    }
  });
})();
