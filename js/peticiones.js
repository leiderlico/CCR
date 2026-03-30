/* ── PETICIONES ── */
const PET_SCRIPT = 'https://script.google.com/macros/s/AKfycbwi2SBX5TjxPSPiK2oFR6pnQv7-T-8ByXNZDOwh1OTm8VPg4IAPffLPCSO-N5tIPaCzRg/exec';

async function enviarPeticion() {
  const texto = document.getElementById('etPeticion').value.trim();
  if (!texto) { document.getElementById('etPeticion').style.borderColor = 'red'; return; }

  try {
    const body = JSON.stringify({ action: 'peticion', texto });
    await fetch(PET_SCRIPT, { method: 'POST', body, headers: { 'Content-Type': 'application/json' } });
  } catch(e) { /* silent — show confirmation anyway */ }

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

/* ── VIDEOS ── */
const SHEET_CSV = 'https://docs.google.com/spreadsheets/d/e/2PACX-1vTdak_SUYm9hTc1h4GWLDzgi-VVmcbPRV2mKr5xMAr8hNlb1jsvUpAiBIepMqiDAYX04NOmoGK18Vft/pub?gid=0&single=true&output=csv';

window.currentGrupoNombre = '';
let videosCache = null;

function abrirVideos(grupoNombre) {
  window.currentGrupoNombre = grupoNombre;
  pushScreen('screenVideos', grupoNombre);
  cargarVideos(grupoNombre);
}

async function cargarVideos(grupo) {
  document.getElementById('videosLoading').classList.remove('hidden');
  document.getElementById('listaVideos').innerHTML = '';

  try {
    if (!videosCache) {
      const res = await fetch(SHEET_CSV);
      const csv = await res.text();
      videosCache = parseCSV(csv);
    }

    const filtrados = videosCache.filter(v => {
      const g = (v.grupo || v.Grupo || '').toUpperCase().trim();
      return g === grupo.toUpperCase().trim();
    });

    document.getElementById('videosLoading').classList.add('hidden');
    const lista = document.getElementById('listaVideos');

    if (filtrados.length === 0) {
      lista.innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">No hay videos disponibles aún.</p>';
      return;
    }

    filtrados.forEach(v => {
      const videoId = v.video_id || v.VideoId || v.youtubeId || '';
      const titulo = v.titulo || v.Titulo || v.title || 'Sin título';
      const desc = v.descripcion || v.Descripcion || v.description || '';
      const thumb = videoId ? `https://img.youtube.com/vi/${videoId}/mqdefault.jpg` : 'assets/img/ic_video.png';

      const card = document.createElement('div');
      card.className = 'video-card';
      card.innerHTML = `
        <img class="video-thumb" src="${thumb}" alt="${titulo}" onerror="this.src='assets/img/ic_video.png'"/>
        <div class="video-info">
          <div class="video-title">${titulo}</div>
          <div class="video-desc">${desc}</div>
        </div>
      `;
      card.onclick = () => abrirPlayer(videoId, titulo, desc);
      lista.appendChild(card);
    });
  } catch(e) {
    document.getElementById('videosLoading').classList.add('hidden');
    document.getElementById('listaVideos').innerHTML = '<p style="padding:24px;color:var(--text2);text-align:center">Error cargando videos.</p>';
  }
}

/* ── YOUTUBE PLAYER ── */
let ytPlayer = null;
let ytApiReady = false;

window.onYouTubeIframeAPIReady = () => { ytApiReady = true; };

function abrirPlayer(videoId, titulo, desc) {
  document.getElementById('playerTitulo').textContent = titulo;
  document.getElementById('playerDesc').textContent = desc;

  pushScreen('screenPlayer', titulo);

  if (ytApiReady) {
    if (ytPlayer) {
      ytPlayer.destroy();
    }
    ytPlayer = new YT.Player('ytPlayer', {
      videoId,
      playerVars: { autoplay: 1, playsinline: 1, rel: 0, modestbranding: 1 },
      events: { onReady: e => e.target.playVideo() }
    });
  } else {
    // Fallback embed
    document.getElementById('ytPlayer').innerHTML = `<iframe width="100%" height="100%" src="https://www.youtube.com/embed/${videoId}?autoplay=1&playsinline=1" frameborder="0" allowfullscreen allow="autoplay"></iframe>`;
  }
}

function stopYTPlayer() {
  if (ytPlayer && ytPlayer.stopVideo) ytPlayer.stopVideo();
  const el = document.getElementById('ytPlayer');
  if (el) el.innerHTML = '';
  ytPlayer = null;
}

/* ── CSV PARSER ── */
function parseCSV(text) {
  const lines = text.split('\n').filter(l => l.trim());
  if (lines.length < 2) return [];
  const headers = lines[0].split(',').map(h => h.trim().replace(/"/g,''));
  return lines.slice(1).map(line => {
    const cols = splitCSVLine(line);
    const obj = {};
    headers.forEach((h, i) => { obj[h] = (cols[i] || '').trim().replace(/^"|"$/g,''); });
    return obj;
  });
}

function splitCSVLine(line) {
  const result = [];
  let current = '';
  let inQuotes = false;
  for (const char of line) {
    if (char === '"') inQuotes = !inQuotes;
    else if (char === ',' && !inQuotes) { result.push(current); current = ''; }
    else current += char;
  }
  result.push(current);
  return result;
}
