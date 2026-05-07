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

/* VIDEOS */
const SHEET_CSV = 'https://docs.google.com/spreadsheets/d/e/2PACX-1vTdak_SUYm9hTc1h4GWLDzgi-VVmcbPRV2mKr5xMAr8hNlb1jsvUpAiBIepMqiDAYX04NOmoGK18Vft/pub?gid=0&single=true&output=csv';

window.currentGrupoNombre = '';
let videosCache = null;

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
    if (!videosCache) {
      const res = await fetch(SHEET_CSV);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const csv = await res.text();
      videosCache = parseCSV(csv);
    }

    const filtrados = videosCache
      .filter(v => normalizar(v.grupo) === normalizar(grupo))
      .sort((a, b) => (b.fechaOrden || 0) - (a.fechaOrden || 0));

    document.getElementById('videosLoading').classList.add('hidden');
    const lista = document.getElementById('listaVideos');

    if (filtrados.length === 0) {
      lista.innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">No hay videos disponibles aun.</p>';
      return;
    }

    filtrados.forEach(v => {
      const videoId = v.videoId || extraerYoutubeId(v.urlYoutube);
      const titulo = v.titulo || 'Sin titulo';
      const desc = v.predicador || '';
      const thumb = videoId ? `https://img.youtube.com/vi/${videoId}/mqdefault.jpg` : 'assets/img/ic_video.png';

      const card = document.createElement('div');
      card.className = 'video-card';
      card.innerHTML = `
        <img class="video-thumb" src="${thumb}" alt="${escapeHtml(titulo)}" onerror="this.src='assets/img/ic_video.png'"/>
        <div class="video-info">
          <div class="video-title">${escapeHtml(titulo)}</div>
          <div class="video-desc">${escapeHtml(desc)}</div>
          ${v.cita ? `<div class="video-chip">${escapeHtml(v.cita)}</div>` : ''}
        </div>
      `;
      card.onclick = () => abrirPlayer(v, titulo);
      lista.appendChild(card);
    });
  } catch(e) {
    document.getElementById('videosLoading').classList.add('hidden');
    document.getElementById('listaVideos').innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">Error cargando videos.</p>';
  }
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

  renderPlayerInfo(typeof video === 'string' ? { titulo, urlYoutube } : video);
  pushScreen('screenPlayer', titulo);

  const playerEl = document.getElementById('ytPlayer');
  playerEl.innerHTML = '';

  if (ytApiReady && window.YT?.Player) {
    if (ytPlayer) ytPlayer.destroy();
    ytPlayer = new YT.Player('ytPlayer', {
      videoId,
      playerVars: { autoplay: 1, playsinline: 1, rel: 0, modestbranding: 1 },
      events: { onReady: e => e.target.playVideo() }
    });
  } else {
    playerEl.innerHTML = `<iframe width="100%" height="100%" src="https://www.youtube.com/embed/${videoId}?autoplay=1&rel=0&playsinline=1" frameborder="0" allowfullscreen allow="autoplay; encrypted-media; fullscreen; picture-in-picture"></iframe>`;
  }
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
  info.innerHTML = `
    <h3>${escapeHtml(titulo)}</h3>
    <div class="player-meta">
      ${video.cita ? `<div class="player-meta-item full"><span class="player-meta-label">Cita</span><span class="player-meta-value cita">${escapeHtml(video.cita)}</span></div>` : ''}
      ${video.predicador ? `<div class="player-meta-item"><span class="player-meta-label">Predicador</span><span class="player-meta-value">${escapeHtml(video.predicador)}</span></div>` : ''}
      ${video.fecha ? `<div class="player-meta-item"><span class="player-meta-label">Fecha</span><span class="player-meta-value">${escapeHtml(video.fecha)}</span></div>` : ''}
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
