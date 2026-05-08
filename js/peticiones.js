/* PETICIONES */
const PET_SCRIPT = 'https://script.google.com/macros/s/AKfycbwi2SBX5TjxPSPiK2oFR6pnQv7-T-8ByXNZDOwh1OTm8VPg4IAPffLPCSO-N5tIPaCzRg/exec';

async function enviarPeticion() {
  const texto = document.getElementById('etPeticion').value.trim();
  if (!texto) {
    document.getElementById('etPeticion').style.borderColor = 'red';
    return;
  }

  try {
    const body = JSON.stringify({ action: 'peticion', texto });
    await fetch(PET_SCRIPT, { method: 'POST', body, headers: { 'Content-Type': 'text/plain;charset=utf-8' } });
  } catch(e) {
    try {
      await fetch(PET_SCRIPT, { method: 'POST', body: JSON.stringify({ action: 'peticion', texto }), mode: 'no-cors' });
    } catch(_) {}
  }

  document.getElementById('petFormHeader').classList.add('hidden');
  document.getElementById('petFormulario').classList.add('hidden');
  document.getElementById('petConfirmacion').classList.remove('hidden');
}

function nuevaPeticion() {
  document.getElementById('etPeticion').value = '';
  document.getElementById('etPeticion').style.borderColor = '';
  document.getElementById('petFormHeader').classList.remove('hidden');
  document.getElementById('petFormulario').classList.remove('hidden');
  document.getElementById('petConfirmacion').classList.add('hidden');
}

function mostrarPinPeticiones() {
  const pin = prompt('Ingresa el PIN para ver las peticiones');
  if (pin === null) return;
  if (pin.trim() !== '00000000') {
    alert('PIN incorrecto');
    return;
  }
  abrirPeticionesRecibidas();
}

async function abrirPeticionesRecibidas() {
  pushScreen('screenVerPeticiones', 'Peticiones recibidas', false);
  const loading = document.getElementById('peticionesLoading');
  const list = document.getElementById('peticionesList');
  loading?.classList.remove('hidden');
  if (list) list.innerHTML = '';

  try {
    const peticiones = await obtenerPeticiones();
    renderPeticionesRecibidas(peticiones);
  } catch(e) {
    if (list) list.innerHTML = '<div class="pet-empty">No se pudieron cargar las peticiones.</div>';
  } finally {
    loading?.classList.add('hidden');
  }
}

async function obtenerPeticiones() {
  const body = JSON.stringify({ action: 'obtener_peticiones' });
  const res = await fetch(PET_SCRIPT, {
    method: 'POST',
    body,
    headers: { 'Content-Type': 'text/plain;charset=utf-8' }
  });
  if (!res.ok) throw new Error('HTTP ' + res.status);
  const data = await res.json();
  const arr = data.peticiones || data.data || [];
  return arr.map(item => ({
    texto: item.texto || item.peticion || '',
    fecha: item.fecha || item.createdAt || ''
  })).filter(item => item.texto);
}

function renderPeticionesRecibidas(peticiones) {
  const list = document.getElementById('peticionesList');
  if (!list) return;
  if (!peticiones.length) {
    list.innerHTML = '<div class="pet-empty">No hay peticiones aún</div>';
    return;
  }
  list.innerHTML = peticiones.map(item => `
    <article class="pet-bubble">
      <p>${escapeHtml(item.texto)}</p>
      ${item.fecha ? `<span>${escapeHtml(item.fecha)}</span>` : ''}
    </article>
  `).join('');
}

/* VIDEOS */
const SHEET_CSV = 'https://docs.google.com/spreadsheets/d/e/2PACX-1vTdak_SUYm9hTc1h4GWLDzgi-VVmcbPRV2mKr5xMAr8hNlb1jsvUpAiBIepMqiDAYX04NOmoGK18Vft/pub?gid=0&single=true&output=csv';

window.currentGrupoNombre = '';
let videosCache = null;
let currentVideoBase = [];
let currentVideoQuery = '';
let currentVideoPredicador = null;

function abrirVideos(grupoNombre) {
  const grupo = normalizarGrupo(grupoNombre);
  window.currentGrupoNombre = grupo;
  pushScreen('screenVideos', grupo);
  cargarVideos(grupo);
}

async function cargarVideos(grupo) {
  document.getElementById('videosLoading').classList.remove('hidden');
  document.getElementById('listaVideos').innerHTML = '';

  try {
    const videos = await ensureVideosCache();

    const filtrados = videos
      .filter(v => normalizar(v.grupo) === normalizar(grupo))
      .sort((a, b) => (b.fechaOrden || 0) - (a.fechaOrden || 0));

    setVideoContext(filtrados);
    renderListaVideosFiltrada();
  } catch(e) {
    document.getElementById('videosLoading').classList.add('hidden');
    document.getElementById('listaVideos').innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">Error cargando videos.</p>';
  }
}

async function ensureVideosCache() {
  if (videosCache) return videosCache;
  const res = await fetch(SHEET_CSV);
  if (!res.ok) throw new Error('HTTP ' + res.status);
  const csv = await res.text();
  videosCache = parseCSV(csv);
  return videosCache;
}

async function getVideosPorCapitulo(libroId, capitulo) {
  const videos = await ensureVideosCache();
  return videos
    .filter(v => Number(v.libroId) === Number(libroId) && Number(v.capitulo) === Number(capitulo))
    .sort((a, b) => (b.fechaOrden || 0) - (a.fechaOrden || 0));
}

async function abrirVideosCapitulo(libroId, capitulo, titulo) {
  const nombre = titulo || `${LIBROS_BIBLIA[Number(libroId)] || 'Capitulo'} ${capitulo}`;
  window.currentGrupoNombre = nombre;
  pushScreen('screenVideos', nombre);
  document.getElementById('videosLoading').classList.remove('hidden');
  document.getElementById('listaVideos').innerHTML = '';
  try {
    const videos = await getVideosPorCapitulo(libroId, capitulo);
    setVideoContext(videos, `Predicas de ${nombre}`);
    renderListaVideosFiltrada('No hay predicas registradas para este capitulo.');
  } catch(e) {
    document.getElementById('videosLoading').classList.add('hidden');
    document.getElementById('listaVideos').innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">Error cargando videos.</p>';
  }
}

function renderListaVideos(videos, emptyText) {
  document.getElementById('videosLoading').classList.add('hidden');
  const lista = document.getElementById('listaVideos');
  lista.innerHTML = '';

  if (!videos.length) {
    lista.innerHTML = `<p style="padding:24px;color:var(--text2);text-align:center">${escapeHtml(emptyText)}</p>`;
    return;
  }

  videos.forEach(v => {
    const videoId = v.videoId || extraerYoutubeId(v.urlYoutube);
    const titulo = v.titulo || 'Sin titulo';
    const desc = v.predicador || '';
    const thumb = videoId ? `https://img.youtube.com/vi/${videoId}/mqdefault.jpg` : 'assets/img/ic_video.png';

    const card = document.createElement('div');
    card.className = 'video-card';
    card.innerHTML = `
      <div class="video-thumb-wrap">
        <img class="video-thumb" src="${thumb}" alt="${escapeHtml(titulo)}" onerror="this.src='assets/img/ic_video.png'"/>
        ${v.fecha ? `<span class="video-date-badge">${escapeHtml(v.fecha)}</span>` : ''}
      </div>
      <div class="video-info">
        <div class="video-title">${escapeHtml(titulo)}</div>
        <div class="video-desc">${escapeHtml(desc)}</div>
        ${v.cita ? `<div class="video-chip">${escapeHtml(v.cita)}</div>` : ''}
      </div>
    `;
    card.onclick = () => abrirPlayer(v, titulo);
    lista.appendChild(card);
  });
}

function setVideoContext(videos, statusText = '') {
  currentVideoBase = [...videos];
  currentVideoQuery = '';
  currentVideoPredicador = null;
  const search = document.getElementById('videoSearch');
  if (search) search.value = '';
  const status = document.getElementById('videoStatus');
  if (status) {
    status.textContent = statusText;
    status.classList.toggle('hidden', !statusText);
  }
  renderVideoChips();
}

function renderVideoChips() {
  const chips = document.getElementById('videoChips');
  if (!chips) return;
  const predicadores = [...new Set(currentVideoBase.map(v => v.predicador).filter(Boolean))].sort();
  chips.innerHTML = `
    <button class="video-chip-filter active" data-predicador="" onclick="filtrarVideosPorPredicador(null, this)">Mas recientes</button>
    ${predicadores.map(p => `<button class="video-chip-filter" data-predicador="${escapeHtml(p)}" onclick='filtrarVideosPorPredicador(${JSON.stringify(p)}, this)'>${escapeHtml(p)}</button>`).join('')}
  `;
}

function filtrarVideosPorPredicador(predicador, btn) {
  currentVideoPredicador = predicador || null;
  document.querySelectorAll('.video-chip-filter').forEach(chip => chip.classList.remove('active'));
  btn?.classList.add('active');
  renderListaVideosFiltrada();
}

function buscarVideosActuales(query) {
  currentVideoQuery = query || '';
  renderListaVideosFiltrada();
}

function limpiarBusquedaVideos() {
  const input = document.getElementById('videoSearch');
  if (input) input.value = '';
  currentVideoQuery = '';
  renderListaVideosFiltrada();
}

function renderListaVideosFiltrada(emptyText = 'No hay videos disponibles aun.') {
  let filtrados = [...currentVideoBase];
  const query = normalizar(currentVideoQuery);
  if (query) {
    filtrados = filtrados.filter(v =>
      normalizar(v.titulo).includes(query) ||
      normalizar(v.predicador).includes(query) ||
      normalizar(v.cita).includes(query)
    );
  }
  if (currentVideoPredicador) {
    filtrados = filtrados.filter(v => v.predicador === currentVideoPredicador);
  }
  renderListaVideos(filtrados, emptyText || 'Sin resultados.');
}

/* YOUTUBE PLAYER */
let ytPlayer = null;
let ytApiReady = false;

window.onYouTubeIframeAPIReady = () => { ytApiReady = true; };

function abrirPlayer(video, titulo) {
  const urlYoutube = typeof video === 'string' ? video : video.urlYoutube;
  const videoId = extraerYoutubeId(urlYoutube) || urlYoutube;
  if (!videoId) {
    alert('Este video no tiene una URL de YouTube valida.');
    return;
  }

  pushScreen('screenPlayer', titulo);
  renderPlayerInfo(typeof video === 'string' ? { titulo, urlYoutube } : video);

  const playerEl = document.getElementById('ytPlayer');
  if (ytPlayer && ytPlayer.destroy) ytPlayer.destroy();
  ytPlayer = null;
  playerEl.replaceChildren();
  const iframe = document.createElement('iframe');
  iframe.width = '100%';
  iframe.height = '100%';
  iframe.src = `https://www.youtube.com/embed/${videoId}?autoplay=1&rel=0&playsinline=1`;
  iframe.allow = 'autoplay; encrypted-media; fullscreen; picture-in-picture';
  iframe.allowFullscreen = true;
  iframe.frameBorder = '0';
  playerEl.appendChild(iframe);
}

function stopYTPlayer() {
  if (ytPlayer && ytPlayer.stopVideo) ytPlayer.stopVideo();
  const el = document.getElementById('ytPlayer');
  if (el) el.innerHTML = '';
  ytPlayer = null;
}

/* CSV PARSER */
function parseCSV(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim());
  if (lines.length < 2) return [];

  return lines.slice(1).map(line => {
    const cols = splitCSVLine(line);
    const titulo = cleanCSV(cols[0]);
    const predicador = cleanCSV(cols[1]);
    const grupo = cleanCSV(cols[2]);
    const fecha = cleanCSV(cols[3]);
    const urlYoutube = cleanCSV(cols[4]);
    const libroId = parseInt(cleanCSV(cols[5]) || '0', 10) || 0;
    const capitulo = parseInt(cleanCSV(cols[6]) || '0', 10) || 0;
    const versiculo = cleanCSV(cols[7]);
    const libroNombre = LIBROS_BIBLIA[libroId] || '';
    const cita = libroNombre && capitulo ? `${libroNombre} ${capitulo}${versiculo ? ':' + versiculo : ''}` : '';

    return {
      titulo,
      predicador,
      grupo,
      fecha,
      urlYoutube,
      videoId: extraerYoutubeId(urlYoutube),
      libroId,
      capitulo,
      versiculo,
      cita,
      fechaOrden: parseFechaOrden(fecha)
    };
  }).filter(v => v.titulo && v.urlYoutube);
}

function splitCSVLine(line) {
  const result = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    const next = line[i + 1];
    if (char === '"' && inQuotes && next === '"') {
      current += '"';
      i++;
    } else if (char === '"') {
      inQuotes = !inQuotes;
    } else if (char === ',' && !inQuotes) {
      result.push(current);
      current = '';
    } else {
      current += char;
    }
  }

  result.push(current);
  return result;
}

function cleanCSV(value) {
  return (value || '').trim().replace(/^"|"$/g, '').replace(/""/g, '"');
}

function parseFechaOrden(fecha) {
  const m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(fecha || '');
  if (!m) return 0;
  return new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1])).getTime();
}

function extraerYoutubeId(url) {
  if (!url) return '';
  const patterns = [
    /youtu\.be\/([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/watch\?.*?v=([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/embed\/([a-zA-Z0-9_-]{11})/
  ];
  for (const pattern of patterns) {
    const match = pattern.exec(url);
    if (match) return match[1];
  }
  return '';
}

function renderPlayerInfo(video) {
  const info = document.getElementById('playerInfo');
  const titulo = video.titulo || 'Sin titulo';
  const hasBible = Number(video.libroId) && Number(video.capitulo);
  info.innerHTML = `
    <h3>${escapeHtml(titulo)}</h3>
    <div class="player-meta compact">
      ${video.cita ? `<button class="player-cita" onclick="togglePlayerBible(${Number(video.libroId) || 0}, ${Number(video.capitulo) || 0})">${escapeHtml(video.cita)} <span>⌃</span></button>` : ''}
      ${video.predicador ? `<span class="player-predicador">${escapeHtml(video.predicador)}</span>` : ''}
      ${video.fecha ? `<span class="player-fecha">${escapeHtml(video.fecha)}</span>` : ''}
    </div>
    <div id="playerBiblePanel" class="player-bible-panel ${hasBible ? '' : 'hidden'}">${hasBible ? getPlayerBibleHtml(Number(video.libroId), Number(video.capitulo)) : ''}</div>
  `;
}

function togglePlayerBible(libroId, capitulo) {
  const panel = document.getElementById('playerBiblePanel');
  if (!panel || !libroId || !capitulo) return;
  const hidden = panel.classList.contains('hidden');
  if (!hidden) {
    panel.classList.add('hidden');
    return;
  }
  panel.innerHTML = getPlayerBibleHtml(libroId, capitulo);
  panel.classList.remove('hidden');
}

function getPlayerBibleHtml(libroId, capitulo) {
  const libroNombre = LIBROS_BIBLIA[libroId] || `Libro ${libroId}`;
  const versiculos = (window.bibleData || bibleData || { versiculos: [] }).versiculos
    .filter(v => Number(v.libro_id) === Number(libroId) && Number(v.capitulo) === Number(capitulo));
  return `
    <div class="player-bible-head">
      <strong>${escapeHtml(libroNombre)} ${capitulo}</strong>
      <span>${escapeHtml(currentVersion || '')}</span>
    </div>
    <div class="player-bible-verses">
      ${versiculos.map(v => `<p><b>${v.versiculo}</b> ${escapeHtml(v.texto)}</p>`).join('') || '<p>No se encontraron versiculos para esta cita.</p>'}
    </div>
  `;
}

const LIBROS_BIBLIA = {
  1: 'Genesis', 2: 'Exodo', 3: 'Levitico', 4: 'Numeros', 5: 'Deuteronomio',
  6: 'Josue', 7: 'Jueces', 8: 'Rut', 9: '1 Samuel', 10: '2 Samuel',
  11: '1 Reyes', 12: '2 Reyes', 13: '1 Cronicas', 14: '2 Cronicas',
  15: 'Esdras', 16: 'Nehemias', 17: 'Ester', 18: 'Job', 19: 'Salmos',
  20: 'Proverbios', 21: 'Eclesiastes', 22: 'Cantares', 23: 'Isaias',
  24: 'Jeremias', 25: 'Lamentaciones', 26: 'Ezequiel', 27: 'Daniel',
  28: 'Oseas', 29: 'Joel', 30: 'Amos', 31: 'Abdias', 32: 'Jonas',
  33: 'Miqueas', 34: 'Nahum', 35: 'Habacuc', 36: 'Sofonias',
  37: 'Hageo', 38: 'Zacarias', 39: 'Malaquias', 40: 'Mateo',
  41: 'Marcos', 42: 'Lucas', 43: 'Juan', 44: 'Hechos', 45: 'Romanos',
  46: '1 Corintios', 47: '2 Corintios', 48: 'Galatas', 49: 'Efesios',
  50: 'Filipenses', 51: 'Colosenses', 52: '1 Tesalonicenses',
  53: '2 Tesalonicenses', 54: '1 Timoteo', 55: '2 Timoteo', 56: 'Tito',
  57: 'Filemon', 58: 'Hebreos', 59: 'Santiago', 60: '1 Pedro',
  61: '2 Pedro', 62: '1 Juan', 63: '2 Juan', 64: '3 Juan',
  65: 'Judas', 66: 'Apocalipsis'
};

function normalizar(texto) {
  return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();
}

function normalizarGrupo(grupoNombre) {
  const grupo = normalizar(grupoNombre);
  if (grupo.includes('AYUNO')) return 'AYUNO Y ORACION';
  if (grupo.includes('ESCUELA')) return 'ESCUELA DOMINICAL';
  if (grupo.includes('SANIDAD')) return 'SERVICIOS DE SANIDAD Y MILAGRO';
  if (grupo.includes('PREDICAS')) return 'PREDICAS ESPECIALES';
  return grupoNombre;
}

function escapeHtml(value) {
  return String(value || '').replace(/[&<>"']/g, ch => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[ch]));
}
