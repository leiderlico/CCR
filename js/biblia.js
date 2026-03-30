/* ── Bible state ── */
let bibleData = null;
let currentVersion = 'RV1960';
let currentLibro = null;
let currentCapitulo = null;
window.currentLibroNombre = '';

const VERSIONES = [
  { id: 'RV1960',   nombre: 'Reina Valera 1960',            local: true  },
  { id: 'NVI',      nombre: 'Nueva Versión Internacional',   local: false },
  { id: 'NTV',      nombre: 'Nueva Traducción Viviente',     local: false },
  { id: 'RVA2015',  nombre: 'Reina Valera Actualizada 2015', local: false },
  { id: 'LBLA',     nombre: 'La Biblia de las Américas',     local: false },
  { id: 'PESHITTA', nombre: 'Peshitta',                      local: false },
  { id: 'BDO',      nombre: 'Biblia Dios Habla Hoy',         local: false },
  { id: 'TLA',      nombre: 'Traducción en Lenguaje Actual', local: false },
];

const BASE_URL = (() => {
  const loc = window.location.href;
  if (loc.includes('github.io')) return loc.split('/').slice(0, 4).join('/');
  return '.';
})();

async function initBiblia() {
  await loadVersion('RV1960');
}

async function loadVersion(versionId) {
  currentVersion = versionId;
  bibleData = null;
  document.getElementById('bibleLoading').classList.remove('hidden');
  document.getElementById('listaAntiguo').innerHTML = '';
  document.getElementById('listaNuevo').innerHTML = '';

  const cacheKey = 'bible_' + versionId;
  const cached = localStorage.getItem(cacheKey);
  try {
    let data;
    if (cached) {
      data = JSON.parse(cached);
    } else {
      const url = BASE_URL + '/bibles/biblia_' + versionId.toLowerCase() + '.json';
      const res = await fetch(url);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      data = await res.json();
      try { localStorage.setItem(cacheKey, JSON.stringify(data)); } catch(e) {}
    }
    bibleData = data;
    renderLibros();
  } catch(e) {
    document.getElementById('bibleLoading').classList.add('hidden');
    document.getElementById('listaAntiguo').innerHTML =
      '<p style="padding:20px;color:var(--text2);text-align:center">No se pudo cargar la Biblia ' + versionId + '.</p>';
  }
}

function renderLibros() {
  if (!bibleData) return;
  const libros = bibleData.libros || [];
  const antiguoEl = document.getElementById('listaAntiguo');
  const nuevoEl   = document.getElementById('listaNuevo');
  antiguoEl.innerHTML = '';
  nuevoEl.innerHTML   = '';
  libros.forEach(libro => {
    const el = document.createElement('div');
    el.className = 'libro-item';
    el.innerHTML = '<span>' + libro.nombre + '</span><span class="libro-capitulos">' + libro.capitulos + ' cap.</span><svg class="libro-chevron" viewBox="0 0 24 24"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>';
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
    v => v.libro_id === currentLibro.id && v.capitulo === num
  );
  const lista = document.getElementById('listaVersiculos');
  lista.innerHTML = '';
  versiculos.forEach(v => {
    const el = document.createElement('div');
    el.className = 'versiculo-item';
    el.innerHTML = '<span class="ver-num">' + v.versiculo + '</span><span class="ver-texto">' + v.texto + '</span>';
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
  VERSIONES.forEach(ver => {
    const isCached = !!localStorage.getItem('bible_' + ver.id);
    const el = document.createElement('div');
    el.className = 'version-item' + (ver.id === currentVersion ? ' active-version' : '');
    el.innerHTML = '<span>' + ver.nombre + '</span><span class="version-badge ' + (ver.local || isCached ? '' : 'download') + '">' + (ver.id === currentVersion ? '✓ ' : '') + ver.id + (!ver.local && !isCached ? ' ⬇' : '') + '</span>';
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
