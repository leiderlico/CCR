/* Bible state */
let bibleData = null;
let currentVersion = 'RVA1960';
let currentLibro = null;
let currentCapitulo = null;
window.currentLibroNombre = '';

const VERSIONES = [
  { id: 'RVA1960', nombre: 'Reina Valera 1960', archivo: 'biblia_rv1960.json', local: true },
  { id: 'NVI', nombre: 'Nueva Version Internacional', archivo: 'biblia_nvi.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_nvi.json' },
  { id: 'NTV', nombre: 'Nueva Traduccion Viviente', archivo: 'biblia_ntv.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_ntv.json' },
  { id: 'RVA2015', nombre: 'Reina Valera 2015', archivo: 'biblia_rva2015.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_rva2015.json' },
  { id: 'LBLA', nombre: 'La Biblia de las Americas', archivo: 'biblia_lbla.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_lbla.json' },
  { id: 'BDO', nombre: 'Biblia del Oso', archivo: 'bdo.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/bdo.json' },
  { id: 'BTX', nombre: 'La Biblia Textual', archivo: 'btx.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/btx.json' },
  { id: 'PDT', nombre: 'Palabra de Dios para Todos', archivo: 'pdt.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/pdt.json' },
  { id: 'PESHITTA', nombre: 'Biblia Peshitta', archivo: 'psh.json', url: 'https://github.com/leiderlico/bibliasccr/releases/download/v1/psh.json', hidden: true },
];

const BASE_URL = (() => {
  const loc = window.location.href;
  if (loc.includes('github.io')) return loc.split('/').slice(0, 4).join('/');
  return '.';
})();

async function initBiblia() {
  await loadVersion('RVA1960');
}

async function loadVersion(versionId) {
  const version = VERSIONES.find(v => v.id === versionId) || VERSIONES[0];
  currentVersion = version.id;
  bibleData = null;
  document.getElementById('bibleLoading').classList.remove('hidden');
  document.getElementById('listaAntiguo').innerHTML = '';
  document.getElementById('listaNuevo').innerHTML = '';

  try {
    const data = await cargarBiblia(version);
    bibleData = normalizarBiblia(data);
    renderLibros();
  } catch(e) {
    document.getElementById('bibleLoading').classList.add('hidden');
    document.getElementById('listaAntiguo').innerHTML =
      `<div class="empty-message">
        No se pudo cargar ${escapeHtml(version.nombre)}.<br>
        Agrega el archivo <strong>bibles/${escapeHtml(version.archivo)}</strong> o revisa la conexion.
      </div>`;
  }
}

async function cargarBiblia(version) {
  const cacheKey = 'bible_' + version.id;
  const cached = localStorage.getItem(cacheKey);
  if (cached) return JSON.parse(cached);

  const localUrl = BASE_URL + '/bibles/' + version.archivo;
  try {
    const local = await fetch(localUrl);
    if (local.ok) {
      const data = await local.json();
      guardarBibliaCache(cacheKey, data);
      return data;
    }
  } catch(e) {
    // Sigue con descarga remota si esta version la tiene.
  }

  if (version.url) {
    const remote = await fetch(version.url);
    if (remote.ok) {
      const data = await remote.json();
      guardarBibliaCache(cacheKey, data);
      return data;
    }
  }

  throw new Error('No disponible');
}

function guardarBibliaCache(cacheKey, data) {
  try { localStorage.setItem(cacheKey, JSON.stringify(data)); } catch(e) {}
}

function normalizarBiblia(data) {
  return {
    libros: data.libros || [],
    versiculos: data.versiculos || []
  };
}

function renderLibros() {
  if (!bibleData) return;
  const libros = bibleData.libros || [];
  const antiguoEl = document.getElementById('listaAntiguo');
  const nuevoEl = document.getElementById('listaNuevo');
  antiguoEl.innerHTML = '';
  nuevoEl.innerHTML = '';

  libros.forEach(libro => {
    const el = document.createElement('div');
    el.className = 'libro-item';
    el.innerHTML = `
      <span>${escapeHtml(libro.nombre)}</span>
      <span class="libro-capitulos">${libro.capitulos} cap.</span>
      <svg class="libro-chevron" viewBox="0 0 24 24"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>
    `;
    el.onclick = () => seleccionarLibro(libro);
    (libro.testamento === 'Antiguo' ? antiguoEl : nuevoEl).appendChild(el);
  });

  document.getElementById('bibleLoading').classList.add('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

function seleccionarLibro(libro) {
  currentLibro = libro;
  window.currentLibroNombre = libro.nombre;
  const grid = document.getElementById('listaCapitulos');
  grid.innerHTML = '';

  for (let i = 1; i <= libro.capitulos; i++) {
    const el = document.createElement('div');
    el.className = 'cap-item';
    el.textContent = i;
    el.onclick = () => seleccionarCapitulo(i);
    grid.appendChild(el);
  }

  pushScreen('screenCapitulos', libro.nombre, false);
}

function seleccionarCapitulo(num) {
  currentCapitulo = num;
  const versiculos = (bibleData.versiculos || []).filter(
    v => Number(v.libro_id) === Number(currentLibro.id) && Number(v.capitulo) === Number(num)
  );

  const lista = document.getElementById('listaVersiculos');
  lista.innerHTML = '';
  versiculos.forEach(v => {
    const el = document.createElement('div');
    el.className = 'versiculo-item';
    el.innerHTML = `<span class="ver-num">${v.versiculo}</span><span class="ver-texto">${escapeHtml(v.texto)}</span>`;
    lista.appendChild(el);
  });

  pushScreen('screenVersiculos', currentLibro.nombre + ' ' + num, true);
  document.getElementById('btnVersion').classList.remove('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

function switchTab(btn, tab) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
  document.getElementById(tab === 'antiguo' ? 'tabAntiguo' : 'tabNuevo').classList.add('active');
}

function mostrarSelectorVersion() {
  const list = document.getElementById('versionList');
  list.innerHTML = '';

  VERSIONES.filter(ver => !ver.hidden).forEach(ver => {
    const isCached = !!localStorage.getItem('bible_' + ver.id);
    const el = document.createElement('div');
    el.className = 'version-item' + (ver.id === currentVersion ? ' active-version' : '');
    el.innerHTML = `
      <span>${escapeHtml(ver.nombre)}</span>
      <span class="version-badge ${ver.local || isCached ? '' : 'download'}">${ver.id === currentVersion ? 'OK ' : ''}${ver.id}${!ver.local && !isCached ? ' bajar' : ''}</span>
    `;
    el.onclick = () => {
      cerrarModalVersion();
      loadVersion(ver.id).then(() => {
        if (currentLibro && currentCapitulo && document.getElementById('screenVersiculos').classList.contains('active')) {
          seleccionarCapitulo(currentCapitulo);
        }
      });
    };
    list.appendChild(el);
  });

  document.getElementById('modalVersion').classList.remove('hidden');
}

function cerrarModalVersion(e) {
  if (!e || e.target.id === 'modalVersion') {
    document.getElementById('modalVersion').classList.add('hidden');
  }
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
