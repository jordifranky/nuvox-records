'use strict';

/* =====================================================================
   NUVOX RECORDS — Frontend
   Consume la API REST del backend Spring Boot (mismo origen):
     POST   /api/auth/login      → iniciar sesión (BCrypt en el servidor)
     POST   /api/auth/registro   → crear cuenta de artista
     GET    /api/demos           → listar expedientes
     POST   /api/demos           → registrar expediente
     PUT    /api/demos/{id}      → actualizar / dictaminar (comité)
     DELETE /api/demos/{id}      → eliminar (comité)
   ===================================================================== */

const API = ''; // el frontend lo sirve el propio Spring Boot: no hace falta URL base
const $ = (id) => document.getElementById(id);

let sesion = leerSesion();
let demos = [];
let editandoId = null;
let porBorrar = null;

const esComite = () => !!sesion && sesion.rol === 'ROLE_COMITE_AR';
const ETIQUETA = { EN_REVISION: 'EN REVISIÓN', APROBADO: 'APROBADO', RECHAZADO: 'RECHAZADO' };

/* ---------------- Utilidades ---------------- */
function leerSesion() {
  try { return JSON.parse(sessionStorage.getItem('nuvox_sesion') || 'null'); } catch (_) { return null; }
}
function guardarSesion(s) {
  try { s ? sessionStorage.setItem('nuvox_sesion', JSON.stringify(s)) : sessionStorage.removeItem('nuvox_sesion'); } catch (_) { /* sin almacenamiento */ }
}

function basic(user, pass) {
  const bytes = new TextEncoder().encode(`${user}:${pass}`);
  return 'Basic ' + btoa(String.fromCharCode(...bytes));
}

async function api(ruta, opciones = {}) {
  const headers = { 'Content-Type': 'application/json', ...(opciones.headers || {}) };
  if (sesion && !opciones.sinAuth) headers.Authorization = sesion.auth;
  const resp = await fetch(API + ruta, { ...opciones, headers });
  let cuerpo = null;
  try { cuerpo = await resp.json(); } catch (_) { /* respuesta sin cuerpo */ }
  if (resp.status === 401 && sesion && !opciones.sinAuth) {
    cerrarSesion();
    throw new Error('Tu sesión expiró. Ingresa de nuevo.');
  }
  if (resp.status === 403) throw new Error('No tienes permisos para esta acción.');
  if (!resp.ok) throw new Error((cuerpo && cuerpo.mensaje) || `Error ${resp.status}`);
  return cuerpo;
}

function mostrar(id, texto, ok = false) {
  const n = $(id);
  n.textContent = texto || '';
  n.classList.toggle('ok', ok);
}

let toastTimer;
function toast(texto, error = false) {
  const t = $('toast');
  t.textContent = texto;
  t.classList.toggle('error', error);
  t.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('visible'), 3200);
}

// Crea elementos con textContent (nunca innerHTML con datos del usuario → evita XSS)
function el(tag, clase, texto) {
  const n = document.createElement(tag);
  if (clase) n.className = clase;
  if (texto != null) n.textContent = texto;
  return n;
}
function icono(id) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('class', 'ic');
  svg.setAttribute('aria-hidden', 'true');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', `#${id}`);
  svg.append(use);
  return svg;
}
// Tono (0-359) estable a partir de un texto, para colorear cada portada
function tono(texto) {
  let h = 0;
  for (const c of String(texto)) h = (h * 31 + c.charCodeAt(0)) % 360;
  return h;
}
const dos = (n) => String(n).padStart(2, '0');
function fecha(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return isNaN(d) ? '' : d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }).replace('.', '');
}
function iniciales(nombre) {
  return (nombre || '?').split(/[\s-]+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

/* ---------------- Acceso: login y registro ---------------- */
// Tipo de acceso elegido: 'staff' (comité A&R) o 'artista'
let tipoAcceso = 'staff';
const PIE = {
  staff: 'Acceso exclusivo para el comité evaluador de Nuvox Records.',
  artista: 'Envía tus demos y sigue su estado. Solo ves tus propios expedientes.'
};

$('tab-staff').onclick = () => elegirTipo('staff');
$('tab-artista').onclick = () => elegirTipo('artista');
$('ir-registro').onclick = (e) => { e.preventDefault(); mostrarRegistro(true); };
$('ir-login').onclick = (e) => { e.preventDefault(); mostrarRegistro(false); };

function elegirTipo(tipo) {
  tipoAcceso = tipo;
  $('tab-staff').classList.toggle('activo', tipo === 'staff');
  $('tab-artista').classList.toggle('activo', tipo === 'artista');
  // Solo los artistas pueden registrarse
  $('ir-registro-wrap').classList.toggle('oculto', tipo !== 'artista');
  $('acceso-pie').textContent = PIE[tipo];
  mostrar('login-msg', '');
}

// El formulario de registro reemplaza al de login en la misma tarjeta
function mostrarRegistro(registro) {
  $('form-login').classList.toggle('oculto', registro);
  $('form-registro').classList.toggle('oculto', !registro);
  $('segmento-rol').classList.toggle('oculto', registro);
  $('acceso-eyebrow').textContent = registro ? 'NUEVO ARTISTA' : 'CREDENCIAL DE ENTRADA';
  $('acceso-titulo').textContent = registro ? 'Crear cuenta' : 'Acceso';
  $('acceso-pie').textContent = registro ? 'Tu cuenta se crea con rol de artista.' : PIE[tipoAcceso];
  mostrar('login-msg', '');
  (registro ? $('reg-nombre') : $('login-user')).focus();
}
elegirTipo('staff');

$('form-login').onsubmit = async (e) => {
  e.preventDefault();
  const user = $('login-user').value.trim();
  const pass = $('login-pass').value;
  if (!user || !pass) return mostrar('login-msg', 'Ingresa tu correo y tu clave de acceso.');
  const btn = $('btn-login');
  btn.disabled = true;
  try {
    const r = await api('/api/auth/login', { method: 'POST', sinAuth: true, body: JSON.stringify({ username: user, password: pass }) });
    // El backend devuelve el rol: verificamos que coincida con el botón elegido
    const esStaff = r.rol === 'ROLE_COMITE_AR';
    if (tipoAcceso === 'staff' && !esStaff) {
      return mostrar('login-msg', 'Esta cuenta no pertenece al personal administrativo. Ingresa desde "Soy artista".');
    }
    if (tipoAcceso === 'artista' && esStaff) {
      return mostrar('login-msg', 'Esta es una cuenta del personal administrativo. Ingresa desde "Personal administrativo".');
    }
    sesion = { username: r.username, nombre: r.nombreCompleto, rol: r.rol, auth: basic(user, pass) };
    guardarSesion(sesion);
    $('login-pass').value = '';
    mostrar('login-msg', '');
    iniciarPanel();
  } catch (err) {
    mostrar('login-msg', err.message);
  } finally { btn.disabled = false; }
};

$('form-registro').onsubmit = async (e) => {
  e.preventDefault();
  const nombre = $('reg-nombre').value.trim();
  const correo = $('reg-user').value.trim();
  const pass = $('reg-pass').value;
  if (!nombre || !correo || !pass) return mostrar('login-msg', 'Completa todos los campos.');
  if (pass.length < 8) return mostrar('login-msg', 'La contraseña debe tener al menos 8 caracteres.');
  const btn = $('btn-registro');
  btn.disabled = true;
  try {
    await api('/api/auth/registro', {
      method: 'POST', sinAuth: true,
      body: JSON.stringify({ username: correo, password: pass, nombreCompleto: nombre })
    });
    $('login-user').value = correo;
    $('form-registro').reset();
    elegirTipo('artista');
    mostrarRegistro(false);
    mostrar('login-msg', 'Cuenta creada. Ya puedes ingresar con tu correo y contraseña.', true);
  } catch (err) {
    mostrar('login-msg', err.message);
  } finally { btn.disabled = false; }
};

function cerrarSesion() {
  sesion = null;
  demos = [];
  guardarSesion(null);
  $('vista-panel').classList.add('oculto');
  $('vista-login').classList.remove('oculto');
  $('rail').classList.remove('abierto');
}
$('btn-logout').onclick = cerrarSesion;

/* ---------------- Panel ---------------- */
// Menú por rol: el artista NO ve las opciones de curaduría
const MENU = {
  comite: [
    { titulo: 'Curaduría' },
    { id: 'dashboard', texto: 'Dashboard', icono: 'i-grid' },
    { id: 'catalogo', texto: 'Catálogo de demos', icono: 'i-disc' },
    { titulo: 'Revisión A&R' },
    { id: 'evaluacion', texto: 'En evaluación', icono: 'i-clock', cuenta: 'EN_REVISION' },
    { id: 'aprobadas', texto: 'Aprobadas', icono: 'i-check' },
    { id: 'rechazadas', texto: 'Devoluciones', icono: 'i-return' },
    { titulo: 'Acciones' },
    { id: 'nueva', texto: 'Registrar nueva demo', icono: 'i-plus' }
  ],
  artista: [
    { titulo: 'Portal artista' },
    { id: 'mis-demos', texto: 'Mis demos', icono: 'i-disc' },
    { id: 'nueva', texto: 'Registrar nueva demo', icono: 'i-plus' },
    { id: 'criterios', texto: 'Criterios de envío', icono: 'i-book' }
  ]
};

// Cada vista define su encabezado y qué demos muestra
const VISTAS = {
  dashboard:  { eyebrow: 'Dashboard', titulo: 'Dashboard', sec: '// Comité A&R', sub: 'Resumen del archivo: pendientes, últimos envíos y de dónde llegan las demos.', panel: 'v-dashboard', metricas: true },
  catalogo:   { eyebrow: 'Curaduría', titulo: 'Catálogo de demos', sec: '// Comité A&R', sub: 'Todos los expedientes recibidos. Busca, filtra por estado, edita o elimina.', lista: 'Expedientes de audio', filtro: true, metricas: true, ver: () => true },
  evaluacion: { eyebrow: 'Revisión A&R', titulo: 'En evaluación', sec: '// Mesa del comité', sub: 'Demos pendientes de dictamen. Escúchalas y apruébalas o recházalas con un comentario.', lista: 'Pendientes de dictamen', rapido: true, ver: (d) => d.estadoCuratorial === 'EN_REVISION' },
  aprobadas:  { eyebrow: 'Revisión A&R', titulo: 'Aprobadas', sec: '// Listas para catálogo', sub: 'Demos aprobadas por el comité, con su dictamen editorial.', lista: 'Demos aprobadas', ver: (d) => d.estadoCuratorial === 'APROBADO' },
  rechazadas: { eyebrow: 'Revisión A&R', titulo: 'Devoluciones', sec: '// Rechazadas', sub: 'Demos rechazadas y la devolución que recibió cada artista.', lista: 'Demos rechazadas', ver: (d) => d.estadoCuratorial === 'RECHAZADO' },
  'mis-demos':{ eyebrow: 'Portal de artista', titulo: 'Mis demos', sec: '// Archivo personal', sub: 'Sigue el estado de cada demo que enviaste y lee el dictamen del comité.', lista: 'Mis expedientes', filtro: true, metricas: true, ver: () => true },
  criterios:  { eyebrow: 'Portal de artista', titulo: 'Criterios de envío', sec: '// Antes de enviar', sub: 'Lo que el comité necesita para escuchar y evaluar tu demo.', panel: 'v-criterios' }
};
let vista = 'catalogo';

function iniciarPanel() {
  const comite = esComite();
  $('vista-login').classList.add('oculto');
  $('vista-panel').classList.remove('oculto');
  $('usuario-nombre').textContent = sesion.nombre || sesion.username;
  $('usuario-rol').textContent = comite ? 'Comité A&R' : 'Artista';
  $('avatar').textContent = iniciales(sesion.nombre || sesion.username);
  $('rol-chip').textContent = comite ? 'COMITÉ A&R' : 'PORTAL ARTISTA';
  armarMenu();
  vista = comite ? 'dashboard' : 'mis-demos';
  cargar();
}

function armarMenu() {
  const nav = $('rail-nav');
  nav.replaceChildren();
  MENU[esComite() ? 'comite' : 'artista'].forEach((it) => {
    if (it.titulo) { nav.append(el('p', 'rail-titulo', it.titulo.toUpperCase())); return; }
    const a = el('a');
    a.href = '#'; a.dataset.vista = it.id;
    a.append(icono(it.icono), el('span', null, it.texto));
    if (it.cuenta) { const c = el('em', 'nav-cuenta mono'); c.dataset.cuenta = it.cuenta; a.append(c); }
    a.onclick = (e) => {
      e.preventDefault();
      $('rail').classList.remove('abierto');
      if (it.id === 'nueva') return abrirForm(null);
      irA(it.id);
    };
    nav.append(a);
  });
}

function irA(id) {
  vista = id;
  $('buscar').value = '';
  $('filtro-estado').value = '';
  window.scrollTo({ top: 0 });
  pintar();
}

async function cargar() {
  try {
    demos = await api('/api/demos');
    mostrar('panel-msg', '');
    pintar();
  } catch (err) { mostrar('panel-msg', err.message); }
}

const contar = (e) => demos.filter((d) => d.estadoCuratorial === e).length;
const recientes = () => [...demos].sort((a, b) => String(b.fechaEnvio || '').localeCompare(String(a.fechaEnvio || '')) || b.id - a.id);

function pintar() {
  const v = VISTAS[vista];
  // Menú activo y contadores
  document.querySelectorAll('#rail-nav a').forEach((a) => a.classList.toggle('activo', a.dataset.vista === vista));
  document.querySelectorAll('#rail-nav [data-cuenta]').forEach((c) => { const n = contar(c.dataset.cuenta); c.textContent = n || ''; });

  // Encabezado
  $('eyebrow-panel').textContent = v.eyebrow.toUpperCase();
  $('titulo-panel').textContent = v.titulo;
  $('titulo-sec').textContent = v.sec;
  $('subtitulo-panel').textContent = v.sub;

  // Qué bloques se ven
  $('metricas').classList.toggle('oculto', !v.metricas);
  $('v-dashboard').classList.toggle('oculto', v.panel !== 'v-dashboard');
  $('v-criterios').classList.toggle('oculto', v.panel !== 'v-criterios');
  const hayLista = !!v.lista;
  $('lista-cab').classList.toggle('oculto', !hayLista);
  $('lista').classList.toggle('oculto', !hayLista);
  $('filtro-estado').classList.toggle('oculto', !v.filtro);
  document.querySelector('.buscador').classList.toggle('oculto', !hayLista);

  $('stat-total').textContent = dos(demos.length);
  $('stat-rev').textContent = dos(contar('EN_REVISION'));
  $('stat-apr').textContent = dos(contar('APROBADO'));
  $('stat-rec').textContent = dos(contar('RECHAZADO'));

  if (v.panel === 'v-dashboard') return pintarDashboard();
  if (!hayLista) return;

  const q = $('buscar').value.trim().toLowerCase();
  const est = $('filtro-estado').value;
  const visibles = recientes().filter((d) => v.ver(d) &&
    (!est || d.estadoCuratorial === est) &&
    (!q || [d.tituloCancion, d.nombreArtistico, d.codigoExpediente, d.generoMusical]
      .some((x) => (x || '').toLowerCase().includes(q))));

  $('lista-titulo').textContent = v.lista;
  $('cuenta').textContent = `${visibles.length} ${visibles.length === 1 ? 'REGISTRO' : 'REGISTROS'}`;
  const lista = $('lista');
  lista.replaceChildren();
  if (!visibles.length) {
    const vacio = el('div', 'vacio');
    const textos = {
      evaluacion: ['Mesa al día', 'No hay demos pendientes de dictamen.'],
      aprobadas: ['Sin aprobaciones todavía', 'Las demos que apruebes aparecerán aquí.'],
      rechazadas: ['Sin devoluciones', 'Las demos rechazadas aparecerán aquí con su dictamen.'],
      'mis-demos': ['Aún no enviaste demos', 'Usa "Registrar nueva demo" en el menú para enviar la primera.']
    };
    const [t, d] = (q || est) ? ['Sin coincidencias', 'Prueba con otra búsqueda o estado.'] : (textos[vista] || ['Aún no hay expedientes', 'Registra la primera desde el menú.']);
    vacio.append(el('strong', null, t), el('span', null, d));
    lista.append(vacio);
    return;
  }
  visibles.forEach((d) => lista.append(expediente(d, v.rapido)));
}

function pintarDashboard() {
  const pend = contar('EN_REVISION');
  const aviso = $('dash-pendientes');
  aviso.textContent = pend
    ? `Tienes ${pend} ${pend === 1 ? 'demo pendiente' : 'demos pendientes'} de dictamen. Ir a "En evaluación"`
    : 'No hay demos pendientes de dictamen. La mesa está al día.';
  aviso.classList.toggle('al-dia', !pend);
  aviso.onclick = () => pend && irA('evaluacion');

  const ul = $('dash-recientes');
  ul.replaceChildren();
  recientes().slice(0, 5).forEach((d) => {
    const li = el('li');
    li.append(el('span', 'fila-codigo mono', d.codigoExpediente),
      el('span', 'fila-titulo', d.tituloCancion),
      el('span', 'fila-artista', d.nombreArtistico),
      el('span', `chip ${d.estadoCuratorial}`, ETIQUETA[d.estadoCuratorial]));
    ul.append(li);
  });
  if (!demos.length) ul.append(el('li', 'fila-vacia', 'Todavía no hay envíos.'));

  barras('dash-generos', (d) => d.generoMusical);
  barras('dash-paises', (d) => d.pais);
}

// Ranking simple (top 5) dibujado con barras proporcionales
function barras(id, campo) {
  const cuenta = {};
  demos.forEach((d) => { const k = (campo(d) || '—').trim(); cuenta[k] = (cuenta[k] || 0) + 1; });
  const top = Object.entries(cuenta).sort((a, b) => b[1] - a[1]).slice(0, 5);
  const max = top.length ? top[0][1] : 1;
  const ul = $(id);
  ul.replaceChildren();
  top.forEach(([k, n]) => {
    const li = el('li');
    const barra = el('span', 'barra');
    barra.style.setProperty('--p', `${(n / max) * 100}%`);
    li.append(el('span', 'barra-txt', k), el('b', 'mono', String(n)), barra);
    ul.append(li);
  });
  if (!top.length) ul.append(el('li', 'fila-vacia', 'Sin datos todavía.'));
}

function expediente(d, rapido = false) {
  const art = el('article', `expediente est-${d.estadoCuratorial}`);

  // Portada ilustrada (grabadora + vinilo); el botón ▶ va sobre la etiqueta del vinilo
  const portada = el('div', 'portada');
  const play = el('a', 'play');
  play.append(icono('i-play'));
  play.setAttribute('aria-label', `Escuchar ${d.tituloCancion}`);
  if (/^https:\/\//i.test(d.enlaceAudio || '')) {
    play.href = d.enlaceAudio; play.target = '_blank'; play.rel = 'noopener noreferrer';
    play.title = 'Escuchar demo';
  } else { play.classList.add('sin-enlace'); }
  portada.append(play);

  const cuerpo = el('div', 'exp-cuerpo');
  const meta = el('div', 'exp-meta mono');
  meta.append(el('span', 'codigo', `#${d.codigoExpediente}`));
  if (d.fechaEnvio) meta.append(el('span', null, fecha(d.fechaEnvio)));
  if (d.formatoAudio) meta.append(el('span', 'fmt', d.formatoAudio));
  const tecnica = [d.bpm ? `${d.bpm} BPM` : '', d.tonalidad].filter(Boolean).join(' · ');
  if (tecnica) meta.append(el('span', null, tecnica));

  const proyecto = el('div', 'exp-proyecto');
  proyecto.append(el('b', null, d.nombreArtistico), document.createTextNode(` — ${d.generoMusical} — ${d.pais}`));
  if (esComite() && d.correoArtista) proyecto.append(document.createTextNode(` — ${d.correoArtista}`));

  cuerpo.append(meta, el('h3', null, d.tituloCancion), proyecto);
  if (d.notasArtista) cuerpo.append(el('p', 'exp-notas', d.notasArtista));
  if (d.dictamenComite) {
    const dic = el('div', 'dictamen');
    dic.append(el('small', null, d.estadoCuratorial === 'RECHAZADO' ? 'DEVOLUCIÓN DE LA SALA' : 'DICTAMEN EDITORIAL'),
      el('p', null, d.dictamenComite));
    cuerpo.append(dic);
  } else if (!esComite() && d.estadoCuratorial === 'EN_REVISION') {
    cuerpo.append(el('p', 'exp-espera', 'El comité aún no emite su dictamen.'));
  }

  const lado = el('div', 'exp-lado');
  lado.append(el('span', `chip ${d.estadoCuratorial}`, ETIQUETA[d.estadoCuratorial] || d.estadoCuratorial));
  if (esComite()) {
    const acciones = el('div', 'exp-acciones');
    if (rapido) {
      const ok = el('button', 'btn btn-outline exito');
      ok.type = 'button'; ok.append(icono('i-check'), document.createTextNode('Aprobar'));
      ok.onclick = () => pedirVeredicto(d, 'APROBADO');
      const no = el('button', 'btn btn-outline peligro');
      no.type = 'button'; no.append(icono('i-return'), document.createTextNode('Rechazar'));
      no.onclick = () => pedirVeredicto(d, 'RECHAZADO');
      acciones.append(ok, no);
    } else {
      const ed = el('button', 'btn btn-outline');
      ed.type = 'button'; ed.append(icono('i-edit'), document.createTextNode('Editar'));
      ed.onclick = () => abrirForm(d);
      const del = el('button', 'btn btn-outline peligro');
      del.type = 'button'; del.append(icono('i-trash'), document.createTextNode('Eliminar'));
      del.onclick = () => pedirBorrado(d);
      acciones.append(ed, del);
    }
    lado.append(acciones);
  }

  art.append(portada, cuerpo, lado);
  return art;
}

$('buscar').oninput = pintar;
$('filtro-estado').onchange = pintar;

/* ---------------- Veredicto rápido (PUT con estado + dictamen) ---------------- */
const dlgVer = $('dlg-veredicto');
let veredicto = null;
function pedirVeredicto(d, estado) {
  veredicto = { d, estado };
  const aprobar = estado === 'APROBADO';
  $('v-titulo').textContent = aprobar ? 'Aprobar demo' : 'Rechazar demo';
  $('v-obra').textContent = `"${d.tituloCancion}" — ${d.nombreArtistico} (#${d.codigoExpediente})`;
  $('v-dictamen').value = d.dictamenComite || '';
  $('btn-confirmar-veredicto').textContent = aprobar ? 'Aprobar' : 'Rechazar';
  $('btn-confirmar-veredicto').className = `btn ${aprobar ? 'btn-primario' : 'btn-peligro'}`;
  mostrar('v-msg', '');
  dlgVer.showModal();
  $('v-dictamen').focus();
}
$('btn-cerrar-veredicto').onclick = () => dlgVer.close();
$('btn-cancelar-veredicto').onclick = () => dlgVer.close();
$('form-veredicto').onsubmit = async (e) => {
  e.preventDefault();
  const dictamen = $('v-dictamen').value.trim();
  if (!dictamen) return mostrar('v-msg', 'Escribe el dictamen para el artista.');
  const { d, estado } = veredicto;
  const cuerpo = {
    codigoExpediente: d.codigoExpediente, tituloCancion: d.tituloCancion, nombreArtistico: d.nombreArtistico,
    correoArtista: d.correoArtista, generoMusical: d.generoMusical, pais: d.pais, bpm: d.bpm, tonalidad: d.tonalidad,
    formatoAudio: d.formatoAudio, enlaceAudio: d.enlaceAudio, notasArtista: d.notasArtista,
    estadoCuratorial: estado, dictamenComite: dictamen
  };
  const btn = $('btn-confirmar-veredicto');
  btn.disabled = true;
  try {
    await api(`/api/demos/${d.id}`, { method: 'PUT', body: JSON.stringify(cuerpo) });
    dlgVer.close();
    toast(estado === 'APROBADO' ? `"${d.tituloCancion}" aprobada.` : `"${d.tituloCancion}" rechazada.`);
    await cargar();
  } catch (err) { mostrar('v-msg', err.message); }
  finally { btn.disabled = false; }
};

/* ---------------- Navegación móvil ---------------- */
$('btn-menu').onclick = () => $('rail').classList.toggle('abierto');
document.addEventListener('click', (e) => {
  const rail = $('rail');
  if (rail.classList.contains('abierto') && !rail.contains(e.target) && !$('btn-menu').contains(e.target)) rail.classList.remove('abierto');
});

/* ---------------- Formulario: registrar (POST) / editar (PUT) ---------------- */
const dlg = $('dlg-demo');
$('btn-cancelar').onclick = () => dlg.close();
$('btn-cerrar-demo').onclick = () => dlg.close();

function abrirForm(d) {
  editandoId = d ? d.id : null;
  const comite = esComite();
  $('dlg-eyebrow').textContent = d ? `EXPEDIENTE #${d.codigoExpediente}` : 'INGESTA A&R // NUEVO EXPEDIENTE';
  $('dlg-titulo').textContent = d ? 'Editar y dictaminar demo' : 'Registrar nueva demo';
  $('btn-guardar').textContent = d ? 'Guardar cambios' : (comite ? 'Registrar expediente' : 'Enviar al comité');
  $('form-demo').reset();
  mostrar('demo-msg', '');
  $('bloque-comite').classList.toggle('oculto', !comite);

  const v = d || {};
  $('f-titulo').value = v.tituloCancion || '';
  $('f-artista').value = v.nombreArtistico || (d ? '' : (comite ? '' : (sesion.nombre || '')));
  $('f-correo').value = v.correoArtista || (comite ? '' : sesion.username);
  $('f-correo').readOnly = !comite; // un artista siempre envía con su propio correo
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
  $('f-titulo').focus();
}

// Validación en el navegador (el backend vuelve a validar todo con @Valid)
function validarForm() {
  const obligatorios = [['f-titulo', 'el título'], ['f-artista', 'el nombre artístico'], ['f-correo', 'el correo'],
    ['f-genero', 'el género'], ['f-pais', 'el país'], ['f-formato', 'el formato de audio'], ['f-enlace', 'el enlace de la demo']];
  for (const [id, nombre] of obligatorios) {
    if (!$(id).value.trim()) { $(id).focus(); return `Falta completar ${nombre}.`; }
  }
  if (!/^\S+@\S+\.\S+$/.test($('f-correo').value.trim())) { $('f-correo').focus(); return 'El correo no tiene un formato válido.'; }
  if (!/^https:\/\/(www\.|on\.)?(soundcloud\.com|dropbox\.com)\/\S+$/i.test($('f-enlace').value.trim())) {
    $('f-enlace').focus(); return 'El enlace debe ser https de SoundCloud o Dropbox.';
  }
  return null;
}

$('form-demo').onsubmit = async (e) => {
  e.preventDefault();
  const error = validarForm();
  if (error) return errorForm(error);

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

  const btn = $('btn-guardar');
  btn.disabled = true;
  try {
    if (editandoId) {
      await api(`/api/demos/${editandoId}`, { method: 'PUT', body: JSON.stringify(cuerpo) });
      toast('Expediente actualizado.');
    } else {
      const nueva = await api('/api/demos', { method: 'POST', body: JSON.stringify(cuerpo) });
      toast(`Demo registrada con el código ${nueva.codigoExpediente}.`);
    }
    dlg.close();
    await cargar();
  } catch (err) { errorForm(err.message); }
  finally { btn.disabled = false; }
};

// El error se muestra en el pie del diálogo, que siempre está visible
function errorForm(texto) {
  mostrar('demo-msg', texto);
}

/* ---------------- Eliminar (DELETE) con confirmación ---------------- */
const dlgBorrar = $('dlg-borrar');
function pedirBorrado(d) {
  porBorrar = d;
  $('b-titulo').textContent = `"${d.tituloCancion}"`;
  $('b-artista').textContent = d.nombreArtistico;
  $('b-formato').textContent = d.formatoAudio || '—';
  $('b-codigo').textContent = d.codigoExpediente;
  mostrar('borrar-msg', '');
  dlgBorrar.showModal();
}
$('btn-no-borrar').onclick = () => dlgBorrar.close();
$('btn-cerrar-borrar').onclick = () => dlgBorrar.close();
$('btn-si-borrar').onclick = async () => {
  if (!porBorrar) return;
  const btn = $('btn-si-borrar');
  btn.disabled = true;
  try {
    await api(`/api/demos/${porBorrar.id}`, { method: 'DELETE' });
    dlgBorrar.close();
    toast(`Expediente ${porBorrar.codigoExpediente} eliminado.`);
    porBorrar = null;
    await cargar();
  } catch (err) { mostrar('borrar-msg', err.message); }
  finally { btn.disabled = false; }
};

/* ---------------- Arranque ---------------- */
if (sesion) iniciarPanel();
