/* ── Bible state ── */
let bibleData = null;
let currentVersion = 'RV1960';
let currentLibroId = null;
let currentCapitulo = null;
window.currentLibroNombre = '';

const VERSIONES = [
  { id: 'RV1960',   nombre: 'Reina Valera 1960',  url: null, asset: true },
  { id: 'NVI',      nombre: 'Nueva Versión Internacional', url: null, asset: true },
  { id: 'NTV',      nombre: 'Nueva Traducción Viviente',   url: null, asset: true },
  { id: 'RVA2015',  nombre: 'Reina Valera Actualizada 2015', url: 'BIBLIA_RVA2015_URL' },
  { id: 'LBLA',     nombre: 'La Biblia de las Américas',    url: 'BIBLIA_LBLA_URL' },
  { id: 'PESHITTA', nombre: 'Peshitta',                      url: 'BIBLIA_PESHITTA_URL' },
  { id: 'BDO',      nombre: 'Biblia Dios Habla Hoy',         url: 'BIBLIA_BDO_URL' },
  { id: 'TLA',      nombre: 'Traducción en Lenguaje Actual',  url: 'BIBLIA_TLA_URL' },
];

// Cached downloaded versions
const downloadedVersions = new Set(['RV1960']); // RV1960 always available from GitHub

async function initBiblia() {
  await loadVersion('RV1960');
}

async function loadVersion(versionId) {
  currentVersion = versionId;
  bibleData = null;
  const ver = VERSIONES.find(v => v.id === versionId);
  if (!ver) return;

  document.getElementById('bibleLoading').classList.remove('hidden');
  document.getElementById('listaAntiguo').innerHTML = '';
  document.getElementById('listaNuevo').innerHTML = '';

  try {
    // URL for GitHub raw content — replace YOUR_USER/YOUR_REPO with real values
    const url = `https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/bibles/biblia_${versionId.toLowerCase()}.json`;
    const cached = localStorage.getItem(`bible_${versionId}`);

    let data;
    if (cached) {
      data = JSON.parse(cached);
    } else {
      const res = await fetch(url);
      if (!res.ok) throw new Error('No disponible');
      data = await res.json();
      // Cache it
      try { localStorage.setItem(`bible_${versionId}`, JSON.stringify(data)); } catch(e) {}
      downloadedVersions.add(versionId);
    }
    bibleData = data;
    renderLibros();
  } catch(e) {
    document.getElementById('bibleLoading').classList.add('hidden');
    document.getElementById('listaAntiguo').innerHTML = '<p style="padding:20px;color:var(--text2);text-align:center">Error cargando la Biblia. Verifica tu conexión.</p>';
  }
}

function renderLibros() {
  if (!bibleData) return;

  const libros = bibleData.libros || bibleData;
  const antiguoEl = document.getElementById('listaAntiguo');
  const nuevoEl = document.getElementById('listaNuevo');
  antiguoEl.innerHTML = '';
  nuevoEl.innerHTML = '';

  libros.forEach(libro => {
    const el = document.createElement('div');
    el.className = 'libro-item';
    const caps = libro.capitulos ? libro.capitulos.length : (libro.num_capitulos || '');
    el.innerHTML = `
      <span>${libro.nombre}</span>
      <span class="libro-capitulos">${caps} cap.</span>
      <svg class="libro-chevron" viewBox="0 0 24 24"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>
    `;
    el.onclick = () => seleccionarLibro(libro);
    const testamento = libro.testamento || (libros.indexOf(libro) < 39 ? 'Antiguo' : 'Nuevo');
    (testamento === 'Antiguo' ? antiguoEl : nuevoEl).appendChild(el);
  });

  document.getElementById('bibleLoading').classList.add('hidden');
  // Update version button
  const btnVer = document.getElementById('btnVersion');
  btnVer.textContent = currentVersion;
}

function seleccionarLibro(libro) {
  currentLibroId = libro.id || libro.nombre;
  window.currentLibroNombre = libro.nombre;
  const caps = libro.capitulos ? libro.capitulos.length : (libro.num_capitulos || 0);

  // Render capitulos
  const grid = document.getElementById('listaCapitulos');
  grid.innerHTML = '';
  for (let i = 1; i <= caps; i++) {
    const el = document.createElement('div');
    el.className = 'cap-item';
    el.textContent = i;
    el.onclick = () => seleccionarCapitulo(libro, i);
    grid.appendChild(el);
  }

  pushScreen('screenCapitulos', libro.nombre, false);
  // Show version btn in versiculos later
}

function seleccionarCapitulo(libro, num) {
  currentCapitulo = num;
  const caps = libro.capitulos || [];
  const cap = caps[num - 1];
  const versiculos = cap ? (cap.versiculos || cap) : [];

  const lista = document.getElementById('listaVersiculos');
  lista.innerHTML = '';
  versiculos.forEach((v, i) => {
    const el = document.createElement('div');
    el.className = 'versiculo-item';
    const num_ver = v.versiculo || v.num || (i + 1);
    const texto = v.texto || v.text || v;
    el.innerHTML = `<span class="ver-num">${num_ver}</span><span class="ver-texto">${texto}</span>`;
    lista.appendChild(el);
  });

  pushScreen('screenVersiculos', `${libro.nombre} ${num}`, true);
  // Show version button
  document.getElementById('btnVersion').classList.remove('hidden');
  document.getElementById('btnVersion').textContent = currentVersion;
}

/* ── Tabs ── */
function switchTab(btn, tab) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
  document.getElementById(tab === 'antiguo' ? 'tabAntiguo' : 'tabNuevo').classList.add('active');
}

/* ── Version selector ── */
function mostrarSelectorVersion() {
  const list = document.getElementById('versionList');
  list.innerHTML = '';

  VERSIONES.forEach(ver => {
    const el = document.createElement('div');
    el.className = 'version-item' + (ver.id === currentVersion ? ' active-version' : '');
    const isDownloaded = downloadedVersions.has(ver.id) || !!localStorage.getItem(`bible_${ver.id}`);
    el.innerHTML = `
      <span>${ver.nombre}</span>
      <span class="version-badge ${isDownloaded ? '' : 'download'}">${ver.id === currentVersion ? '✓' : (isDownloaded ? ver.id : '⬇ ' + ver.id)}</span>
    `;
    el.onclick = () => {
      cerrarModalVersion();
      if (ver.url === 'BIBLIA_RVA2015_URL' || ver.url?.includes('YOUR_USER')) {
        alert('Esta versión aún no está disponible. Configura las URLs en el código.');
        return;
      }
      loadVersion(ver.id).then(() => {
        // If in versiculos screen, reload
        if (document.getElementById('screenVersiculos').classList.contains('active')) {
          const libro = (bibleData?.libros || bibleData || []).find(l => (l.id || l.nombre) === currentLibroId);
          if (libro && currentCapitulo) seleccionarCapitulo(libro, currentCapitulo);
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
