/* Bible state */
let bibleData = null;
let currentVersion = 'RVA1960';
let currentLibro = null;
let currentCapitulo = null;
let selectedVerses = new Set();
let bibleSearchTimer = null;
let bibleSuggestions = [];
let pendingVerseHighlight = null;
window.currentLibroNombre = '';
window.currentLibroAbbrev = '';
window.currentCapitulo = null;
window.bibleData = null;

const HIGHLIGHT_STORAGE_KEY = 'ccr_verse_highlights_v1';
const HIGHLIGHT_COLORS = [
  '#f48fb166',
  '#ffcc8066',
  '#a5d6a766',
  '#bdbdbd66',
  '#cf94da66',
  '#ef9a9a66'
];

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
  window.bibleData = null;
  const inVersiculos = document.getElementById('screenVersiculos')?.classList.contains('active');
  if (!inVersiculos) {
    document.getElementById('bibleLoading').classList.remove('hidden');
    document.getElementById('listaAntiguo').innerHTML = '';
    document.getElementById('listaNuevo').innerHTML = '';
  }

  try {
    const data = await cargarBiblia(version);
    bibleData = normalizarBiblia(data);
    window.bibleData = bibleData;
    if (inVersiculos && currentLibro && currentCapitulo) {
      renderVersiculos(currentCapitulo);
      document.getElementById('btnVersion').textContent = currentVersion;
    } else {
      renderLibros();
    }
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
    el.innerHTML = `<span>${escapeHtml(libro.nombre)}</span>`;
    el.onclick = () => seleccionarLibro(libro);
    (libro.testamento === 'Antiguo' ? antiguoEl : nuevoEl).appendChild(el);
  });

  document.getElementById('bibleLoading').classList.add('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

function seleccionarLibro(libro) {
  currentLibro = libro;
  window.currentLibroNombre = libro.nombre;
  window.currentLibroAbbrev = libro.abreviacion || libro.nombre;
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
  window.currentCapitulo = num;
  renderVersiculos(num);
  pushScreen('screenVersiculos', getLibroAbreviado(currentLibro) + ' ' + num, true);
  document.getElementById('btnVersion').classList.remove('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

function renderVersiculos(num) {
  const versiculos = (bibleData.versiculos || []).filter(
    v => Number(v.libro_id) === Number(currentLibro.id) && Number(v.capitulo) === Number(num)
  );

  const lista = document.getElementById('listaVersiculos');
  lista.innerHTML = '';
  selectedVerses.clear();
  updateVerseSelectionBar();

  const chapterTitle = document.createElement('div');
  chapterTitle.className = 'versiculo-chapter-title';
  chapterTitle.textContent = `${currentLibro.nombre} ${num}`;
  lista.appendChild(chapterTitle);

  const tools = document.createElement('div');
  tools.id = 'verseSelectionBar';
  tools.className = 'verse-selection-bar hidden';
  tools.innerHTML = `
    <div class="verse-actions-row">
      <button onclick="listenSelectedVerses()">Escuchar</button>
      <button onclick="shareSelectedVerses()">Compartir</button>
      <button onclick="removeSelectedHighlights()">Borrar</button>
      <button onclick="clearVerseSelection()">Cancelar</button>
    </div>
    <div class="verse-colors-row">
      ${HIGHLIGHT_COLORS.map(color => `<button class="verse-color-chip" style="--chip:${color}" onclick="applyHighlightColor('${color}')" title="Resaltar"></button>`).join('')}
    </div>
  `;
  lista.appendChild(tools);

  const videosSlot = document.createElement('div');
  videosSlot.id = 'chapterVideosSlot';
  lista.appendChild(videosSlot);

  versiculos.forEach(v => {
    const el = document.createElement('div');
    el.className = 'versiculo-item';
    el.dataset.verse = v.versiculo;
    const savedColor = getVerseHighlight(v.versiculo);
    if (savedColor) {
      el.classList.add('highlighted');
      el.style.setProperty('--highlight-color', savedColor);
    }
    if (Number(v.versiculo) === Number(pendingVerseHighlight)) {
      el.classList.add('search-highlight');
      setTimeout(() => el.scrollIntoView({ behavior: 'smooth', block: 'center' }), 180);
      setTimeout(() => el.classList.remove('search-highlight'), 2600);
    }
    el.innerHTML = `<span class="ver-num">${v.versiculo}</span><span class="ver-texto">${escapeHtml(v.texto)}</span>`;
    bindVerseSelection(el, v.versiculo);
    lista.appendChild(el);
  });

  pendingVerseHighlight = null;
  renderChapterVideosHint(videosSlot, currentLibro.id, num, currentLibro.nombre);
}

function getLibroAbreviado(libro) {
  return libro?.abreviacion || libro?.nombre || '';
}

function bindVerseSelection(el, verseNumber) {
  let pressTimer = null;
  let longPressed = false;
  const start = () => {
    longPressed = false;
    pressTimer = setTimeout(() => {
      longPressed = true;
      toggleVerseSelection(verseNumber, el);
    }, 420);
  };
  const clear = () => {
    if (pressTimer) clearTimeout(pressTimer);
    pressTimer = null;
  };
  el.addEventListener('touchstart', start, { passive: true });
  el.addEventListener('mousedown', start);
  el.addEventListener('touchend', clear);
  el.addEventListener('mouseup', clear);
  el.addEventListener('mouseleave', clear);
  el.addEventListener('click', event => {
    if (longPressed) {
      event.preventDefault();
      return;
    }
    if (selectedVerses.size > 0) toggleVerseSelection(verseNumber, el);
  });
}

function toggleVerseSelection(verseNumber, el) {
  const key = Number(verseNumber);
  if (selectedVerses.has(key)) {
    selectedVerses.delete(key);
    el.classList.remove('selected');
  } else {
    selectedVerses.add(key);
    el.classList.add('selected');
  }
  updateVerseSelectionBar();
}

function updateVerseSelectionBar() {
  const bar = document.getElementById('verseSelectionBar');
  if (!bar) return;
  bar.classList.toggle('hidden', selectedVerses.size === 0);
}

function getSelectedVerseText() {
  const selected = [...selectedVerses].sort((a, b) => a - b);
  if (!selected.length) return '';
  const verses = (bibleData.versiculos || [])
    .filter(v =>
      Number(v.libro_id) === Number(currentLibro.id) &&
      Number(v.capitulo) === Number(currentCapitulo) &&
      selected.includes(Number(v.versiculo))
    )
    .sort((a, b) => Number(a.versiculo) - Number(b.versiculo));
  const range = selected.length === 1 ? selected[0] : `${selected[0]}-${selected[selected.length - 1]}`;
  return `${currentLibro.nombre} ${currentCapitulo}:${range}\n\n${verses.map(v => `${v.versiculo}. ${v.texto}`).join('\n')}`;
}

async function shareSelectedVerses() {
  const text = getSelectedVerseText();
  if (!text) return;
  try {
    if (navigator.share) await navigator.share({ title: `${currentLibro.nombre} ${currentCapitulo}`, text });
    else if (navigator.clipboard) {
      await navigator.clipboard.writeText(text);
      alert('Versiculo copiado');
    }
  } catch(e) {}
}

function listenSelectedVerses() {
  const text = getSelectedVerseText();
  if (!text || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text.replace(/\n+/g, '. '));
  utterance.lang = 'es-ES';
  window.speechSynthesis.speak(utterance);
}

function clearVerseSelection() {
  selectedVerses.clear();
  document.querySelectorAll('.versiculo-item.selected').forEach(el => el.classList.remove('selected'));
  updateVerseSelectionBar();
}

function getHighlightMap() {
  try { return JSON.parse(localStorage.getItem(HIGHLIGHT_STORAGE_KEY) || '{}'); }
  catch(e) { return {}; }
}

function saveHighlightMap(map) {
  try { localStorage.setItem(HIGHLIGHT_STORAGE_KEY, JSON.stringify(map)); } catch(e) {}
}

function getHighlightKey(verseNumber) {
  return `${Number(currentLibro?.id || 0)}_${Number(currentCapitulo || 0)}_${Number(verseNumber || 0)}`;
}

function getVerseHighlight(verseNumber) {
  return getHighlightMap()[getHighlightKey(verseNumber)] || '';
}

function applyHighlightColor(color) {
  if (!selectedVerses.size || !color) return;
  const map = getHighlightMap();
  selectedVerses.forEach(verse => {
    map[getHighlightKey(verse)] = color;
    const el = document.querySelector(`.versiculo-item[data-verse="${verse}"]`);
    if (el) {
      el.classList.add('highlighted');
      el.style.setProperty('--highlight-color', color);
    }
  });
  saveHighlightMap(map);
  clearVerseSelection();
}

function removeSelectedHighlights() {
  if (!selectedVerses.size) return;
  const map = getHighlightMap();
  selectedVerses.forEach(verse => {
    delete map[getHighlightKey(verse)];
    const el = document.querySelector(`.versiculo-item[data-verse="${verse}"]`);
    if (el) {
      el.classList.remove('highlighted');
      el.style.removeProperty('--highlight-color');
    }
  });
  saveHighlightMap(map);
  clearVerseSelection();
}

async function renderChapterVideosHint(slot, libroId, capitulo, libroNombre) {
  if (!slot || typeof getVideosPorCapitulo !== 'function') return;
  slot.innerHTML = '';
  try {
    const videos = await getVideosPorCapitulo(libroId, capitulo);
    if (!videos.length) return;
    const label = videos.length === 1 ? '1 predica de este capitulo' : `${videos.length} predicas de este capitulo`;
    const btn = document.createElement('button');
    btn.className = 'chapter-video-fab extended';
    btn.innerHTML = `
      <img src="assets/img/ic_video.png" alt=""/>
      <span>${escapeHtml(label)}</span>
      <b>${videos.length > 99 ? '99+' : videos.length}</b>
    `;
    btn.onclick = () => abrirVideosCapitulo(libroId, capitulo, `${libroNombre} ${capitulo}`);
    slot.appendChild(btn);
    setTimeout(() => btn.classList.remove('extended'), 1600);
  } catch(e) {
    // Videos are optional; the Bible chapter should keep working offline.
  }
}

function handleBibleSearchKey(event) {
  if (event.key !== 'Enter') return;
  event.preventDefault();
  const first = bibleSuggestions[0];
  if (first) abrirSugerenciaBiblica(first.libroId, first.capitulo, first.versiculo);
}

function handleBibleSearchInput(value) {
  clearTimeout(bibleSearchTimer);
  bibleSearchTimer = setTimeout(() => runBibleSearch(value), 180);
}

function runBibleSearch(value) {
  const query = (value || '').trim();
  if (!query || !bibleData) {
    renderBibleSuggestions([]);
    return;
  }

  const results = [];
  const ref = parseBibleReference(query);
  if (ref) results.push(ref);

  const normalized = normalizeBibleText(query);
  if (normalized.length >= 3) {
    const found = searchBibleContent(normalized, 10);
    found.forEach(item => {
      if (!results.some(r => r.libroId === item.libroId && r.capitulo === item.capitulo && r.versiculo === item.versiculo)) {
        results.push(item);
      }
    });
  }

  renderBibleSuggestions(results.slice(0, 10));
}

function parseBibleReference(query) {
  const normalized = normalizeBibleText(query).replace(/[.,;]/g, ' ');
  const aliases = getBookAliases();
  for (const item of aliases) {
    if (normalized !== item.alias && !normalized.startsWith(item.alias + ' ')) continue;
    const rest = normalized.slice(item.alias.length).trim();
    const match = /^(\d+)(?:\s*[: ]\s*(\d+))?/.exec(rest);
    if (!match) continue;
    const capitulo = Number(match[1]);
    const versiculo = Number(match[2] || 1);
    if (capitulo < 1 || capitulo > Number(item.libro.capitulos || 0)) continue;
    const verse = findVerse(item.libro.id, capitulo, versiculo);
    if (!verse) continue;
    return makeSuggestion(item.libro, capitulo, versiculo, verse.texto, 1);
  }
  return null;
}

function getBookAliases() {
  if (!bibleData) return [];
  const aliases = [];
  (bibleData.libros || []).forEach(libro => {
    [libro.nombre, libro.abreviacion, (libro.abreviacion || '').replace(/\./g, '')]
      .filter(Boolean)
      .forEach(alias => aliases.push({ alias: normalizeBibleText(alias), libro }));
  });
  return aliases
    .filter((item, index, arr) => arr.findIndex(x => x.alias === item.alias && x.libro.id === item.libro.id) === index)
    .sort((a, b) => b.alias.length - a.alias.length);
}

function searchBibleContent(normalizedQuery, limit) {
  const results = [];
  for (const verse of (bibleData.versiculos || [])) {
    if (!normalizeBibleText(verse.texto).includes(normalizedQuery)) continue;
    const libro = (bibleData.libros || []).find(l => Number(l.id) === Number(verse.libro_id));
    if (!libro) continue;
    results.push(makeSuggestion(libro, Number(verse.capitulo), Number(verse.versiculo), verse.texto, 3));
    if (results.length >= limit) break;
  }
  return results;
}

function makeSuggestion(libro, capitulo, versiculo, texto, priority) {
  return {
    referencia: `${libro.nombre} ${capitulo}:${versiculo}`,
    texto: texto || '',
    libroId: Number(libro.id),
    capitulo: Number(capitulo),
    versiculo: Number(versiculo),
    priority
  };
}

function findVerse(libroId, capitulo, versiculo) {
  return (bibleData.versiculos || []).find(v =>
    Number(v.libro_id) === Number(libroId) &&
    Number(v.capitulo) === Number(capitulo) &&
    Number(v.versiculo) === Number(versiculo)
  );
}

function renderBibleSuggestions(results) {
  const panel = document.getElementById('suggestionPanel');
  const container = document.getElementById('suggestionsContainer');
  if (!panel || !container) return;
  bibleSuggestions = results || [];
  panel.classList.toggle('hidden', bibleSuggestions.length === 0);
  container.innerHTML = bibleSuggestions.map(item => `
    <button class="suggestion-card" onclick="abrirSugerenciaBiblica(${item.libroId}, ${item.capitulo}, ${item.versiculo})">
      <span class="suggestion-ref">${escapeHtml(item.referencia)}</span>
      <span class="suggestion-text">${escapeHtml(item.texto)}</span>
    </button>
  `).join('');
}

function clearBibleSearch() {
  const input = document.getElementById('bibleSearchInput');
  if (input) input.value = '';
  clearBibleSuggestions();
}

function clearBibleSuggestions() {
  bibleSuggestions = [];
  renderBibleSuggestions([]);
}

function abrirSugerenciaBiblica(libroId, capitulo, versiculo) {
  if (!bibleData) return;
  const libro = (bibleData.libros || []).find(l => Number(l.id) === Number(libroId));
  if (!libro) return;
  clearBibleSuggestions();
  const input = document.getElementById('bibleSearchInput');
  if (input) input.value = '';
  currentLibro = libro;
  window.currentLibroNombre = libro.nombre;
  window.currentLibroAbbrev = libro.abreviacion || libro.nombre;
  pendingVerseHighlight = Number(versiculo);
  currentCapitulo = Number(capitulo);
  window.currentCapitulo = Number(capitulo);
  renderVersiculos(Number(capitulo));
  pushScreen('screenVersiculos', getLibroAbreviado(libro) + ' ' + capitulo, true);
  document.getElementById('btnVersion').classList.remove('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

function normalizeBibleText(text) {
  return String(text || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9\s]/gi, ' ')
    .replace(/\s+/g, ' ')
    .toLowerCase()
    .trim();
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
    const el = document.createElement('div');
    el.className = 'version-item' + (ver.id === currentVersion ? ' active-version' : '');
    el.innerHTML = `<span>${escapeHtml(ver.nombre)}</span>`;
    el.onclick = () => {
      cerrarModalVersion();
      loadVersion(ver.id);
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
