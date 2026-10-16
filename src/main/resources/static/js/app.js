'use strict';

// Misma origen: el frontend lo sirve el propio Spring Boot, por lo que no hace falta URL base.
const API = '';
const $ = (id) => document.getElementById(id);

let sesion = JSON.parse(sessionStorage.getItem('nuvox_sesion') || 'null');
let demos = [];
let editandoId = null;

const esComite = () => sesion && sesion.rol === 'ROLE_COMITE_AR';

// ---------- Utilidades ----------
function basic(user, pass) {
  const bytes = new TextEncoder().encode(`${user}:${pass}`);
  return 'Basic ' + btoa(String.fromCharCode(...bytes));
}

async function api(ruta, opciones = {}) {
  const headers = { 'Content-Type': 'application/json', ...(opciones.headers || {}) };
  if (sesion && !opciones.sinAuth) headers.Authorization = sesion.auth;
  const resp = await fetch(API + ruta, { ...opciones, headers });
  let cuerpo = null;
  try { cuerpo = await resp.json(); } catch (_) { /* sin cuerpo */ }
  if (resp.status === 401 && sesion && !opciones.sinAuth) { cerrarSesion(); throw new Error('Tu sesión expiró. Ingresa de nuevo.'); }
  if (!resp.ok) throw new Error((cuerpo && cuerpo.mensaje) || `Error ${resp.status}`);
  return cuerpo;
}

function mostrar(id, texto, ok = false) {
  const el = $(id);
  el.textContent = texto || '';
  el.classList.toggle('ok', ok);
}

const ETIQUETA = { EN_REVISION: 'EN REVISIÓN', APROBADO: 'APROBADO', RECHAZADO: 'RECHAZADO' };

// ---------- Login / registro ----------
$('tab-login').onclick = () => cambiarTab(true);
$('tab-registro').onclick = () => cambiarTab(false);
function cambiarTab(login) {
  $('tab-login').classList.toggle('activa', login);
  $('tab-registro').classList.toggle('activa', !login);
  $('form-login').classList.toggle('oculto', !login);
  $('form-registro').classList.toggle('oculto', login);
  mostrar('login-msg', '');
}

$('form-login').onsubmit = async (e) => {
  e.preventDefault();
  const btn = $('btn-login');
  btn.disabled = true;
  try {
    const user = $('login-user').value.trim();
    const pass = $('login-pass').value;
    const r = await api('/api/auth/login', { method: 'POST', sinAuth: true, body: JSON.stringify({ username: user, password: pass }) });
    sesion = { username: r.username, nombre: r.nombreCompleto, rol: r.rol, auth: basic(user, pass) };
    sessionStorage.setItem('nuvox_sesion', JSON.stringify(sesion));
    $('login-pass').value = '';
    mostrar('login-msg', '');
    iniciarPanel();
  } catch (err) {
    mostrar('login-msg', err.message);
  } finally { btn.disabled = false; }
};

$('form-registro').onsubmit = async (e) => {
  e.preventDefault();
  const btn = $('btn-registro');
  btn.disabled = true;
  try {
    await api('/api/auth/registro', {
      method: 'POST', sinAuth: true,
      body: JSON.stringify({ username: $('reg-user').value.trim(), password: $('reg-pass').value, nombreCompleto: $('reg-nombre').value.trim() })
    });
    $('login-user').value = $('reg-user').value.trim();
    $('form-registro').reset();
    cambiarTab(true);
    mostrar('login-msg', 'Cuenta creada. Ya puedes ingresar.', true);
  } catch (err) {
    mostrar('login-msg', err.message);
  } finally { btn.disabled = false; }
};

function cerrarSesion() {
  sesion = null;
  sessionStorage.removeItem('nuvox_sesion');
  $('vista-panel').classList.add('oculto');
  $('vista-login').classList.remove('oculto');
}
$('btn-logout').onclick = cerrarSesion;

// ---------- Panel ----------
function iniciarPanel() {
  $('vista-login').classList.add('oculto');
  $('vista-panel').classList.remove('oculto');
  $('usuario-info').textContent = `${sesion.nombre} · ${esComite() ? 'Comité A&R' : 'Artista'}`;
  $('titulo-panel').textContent = esComite() ? 'Expedientes de demos' : 'Mis demos';
  $('subtitulo-panel').textContent = esComite()
    ? 'Revisa, dictamina y archiva las maquetas recibidas.'
    : 'Envía tu demo con un enlace de SoundCloud o Dropbox y sigue su estado.';
  cargar();
}

async function cargar() {
  try {
    demos = await api('/api/demos');
    mostrar('panel-msg', '');
    pintar();
  } catch (err) { mostrar('panel-msg', err.message); }
}

function pintar() {
  const q = $('buscar').value.trim().toLowerCase();
  const est = $('filtro-estado').value;
  const visibles = demos.filter((d) =>
    (!est || d.estadoCuratorial === est) &&
    (!q || [d.tituloCancion, d.nombreArtistico, d.codigoExpediente].some((v) => (v || '').toLowerCase().includes(q))));

  $('stat-total').textContent = demos.length;
  $('stat-rev').textContent = demos.filter((d) => d.estadoCuratorial === 'EN_REVISION').length;
  $('stat-apr').textContent = demos.filter((d) => d.estadoCuratorial === 'APROBADO').length;
  $('stat-rec').textContent = demos.filter((d) => d.estadoCuratorial === 'RECHAZADO').length;

  const lista = $('lista');
  lista.replaceChildren();
  if (!visibles.length) {
    const v = document.createElement('div');
    v.className = 'vacio';
    v.textContent = 'No hay demos para mostrar.';
    lista.append(v);
    return;
  }
  visibles.forEach((d) => lista.append(tarjeta(d)));
}

function el(tag, clase, texto) {
  const n = document.createElement(tag);
  if (clase) n.className = clase;
  if (texto != null) n.textContent = texto; // textContent evita inyección de HTML
  return n;
}

function tarjeta(d) {
  const c = el('article', 'card');
  const f1 = el('div', 'fila');
  f1.append(el('span', 'codigo', d.codigoExpediente), el('span', `badge ${d.estadoCuratorial}`, ETIQUETA[d.estadoCuratorial] || d.estadoCuratorial));
  c.append(f1, el('h3', null, d.tituloCancion), el('div', 'meta', `${d.nombreArtistico} · ${d.pais} · ${d.generoMusical}`));
  const tecnica = [d.formatoAudio, d.bpm ? `${d.bpm} BPM` : '', d.tonalidad].filter(Boolean).join(' · ');
  c.append(el('div', 'meta', tecnica));
  if (d.notasArtista) c.append(el('div', 'nota', d.notasArtista));
  if (d.dictamenComite) c.append(el('div', 'dictamen', d.dictamenComite));

  const ac = el('div', 'acciones');
  const a = el('a', 'escuchar', '▶ Escuchar demo');
  // Solo se enlazan URLs https (el backend ya valida SoundCloud/Dropbox)
  if (/^https:\/\//i.test(d.enlaceAudio)) { a.href = d.enlaceAudio; a.target = '_blank'; a.rel = 'noopener noreferrer'; }
  ac.append(a);
  if (esComite()) {
    const ed = el('button', 'btn btn-fantasma btn-sm', 'Editar');
    ed.type = 'button'; ed.onclick = () => abrirForm(d);
    const del = el('button', 'btn btn-peligro btn-sm', 'Eliminar');
    del.type = 'button'; del.onclick = () => eliminar(d);
    ac.append(ed, del);
  }
  c.append(ac);
  return c;
}

$('buscar').oninput = pintar;
$('filtro-estado').onchange = pintar;

// ---------- Formulario (POST / PUT) ----------
const dlg = $('dlg-demo');
$('btn-nueva').onclick = () => abrirForm(null);
$('btn-cancelar').onclick = () => dlg.close();

function abrirForm(d) {
  editandoId = d ? d.id : null;
  $('dlg-titulo').textContent = d ? `Editar ${d.codigoExpediente}` : 'Enviar demo';
  $('form-demo').reset();
  mostrar('demo-msg', '');
  $('bloque-comite').classList.toggle('oculto', !esComite());
  const v = d || {};
  $('f-titulo').value = v.tituloCancion || '';
  $('f-artista').value = v.nombreArtistico || (d ? '' : (sesion.nombre || ''));
  $('f-correo').value = v.correoArtista || sesion.username;
  $('f-correo').readOnly = !esComite();
  $('f-genero').value = v.generoMusical || '';
  $('f-pais').value = v.pais || '';
  $('f-formato').value = v.formatoAudio || '';
  $('f-bpm').value = v.bpm ?? '';
  $('f-tono').value = v.tonalidad || '';
  $('f-enlace').value = v.enlaceAudio || '';
  $('f-notas').value = v.notasArtista || '';
  $('f-codigo').value = v.codigoExpediente || '';
  $('f-estado').value = v.estadoCuratorial || 'EN_REVISION';
  $('f-dictamen').value = v.dictamenComite || '';
  dlg.showModal();
}

$('form-demo').onsubmit = async (e) => {
  e.preventDefault();
  const btn = $('btn-guardar');
  btn.disabled = true;
  const cuerpo = {
    tituloCancion: $('f-titulo').value.trim(),
    nombreArtistico: $('f-artista').value.trim(),
    correoArtista: $('f-correo').value.trim(),
    generoMusical: $('f-genero').value.trim(),
    pais: $('f-pais').value.trim(),
    formatoAudio: $('f-formato').value.trim(),
    bpm: $('f-bpm').value ? Number($('f-bpm').value) : null,
    tonalidad: $('f-tono').value.trim() || null,
    enlaceAudio: $('f-enlace').value.trim(),
    notasArtista: $('f-notas').value.trim() || null
  };
  if (esComite()) {
    cuerpo.codigoExpediente = $('f-codigo').value.trim() || null;
    cuerpo.estadoCuratorial = $('f-estado').value;
    cuerpo.dictamenComite = $('f-dictamen').value.trim() || null;
  }
  try {
    if (editandoId) await api(`/api/demos/${editandoId}`, { method: 'PUT', body: JSON.stringify(cuerpo) });
    else await api('/api/demos', { method: 'POST', body: JSON.stringify(cuerpo) });
    dlg.close();
    await cargar();
  } catch (err) { mostrar('demo-msg', err.message); }
  finally { btn.disabled = false; }
};

async function eliminar(d) {
  if (!confirm(`¿Eliminar definitivamente el expediente ${d.codigoExpediente}?`)) return;
  try {
    await api(`/api/demos/${d.id}`, { method: 'DELETE' });
    await cargar();
  } catch (err) { mostrar('panel-msg', err.message); }
}

// ---------- Arranque ----------
if (sesion) iniciarPanel();
