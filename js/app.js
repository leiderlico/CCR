/* ── App State ── */
const state = {
  currentTab: 'biblia',    // biblia | grupos | devocional | juego | radio
  screenStack: [],          // navigation stack
  darkMode: true
};

/* ── Nav Tab to screen map ── */
const TAB_SCREENS = {
  biblia:    'screenLibros',
  grupos:    'screenGrupos',
  devocional: 'screenDevocional',
  juego:     'screenJuego',
  radio:     'screenRadio'
};

const TAB_TITLES = {
  biblia:    'CCR',
  grupos:    'CCR',
  devocional: 'CCR',
  juego:     'CCR',
  radio:     'CCR'
};

/* ── Init ── */
window.addEventListener('DOMContentLoaded', () => {
  // Restore dark mode
  const saved = localStorage.getItem('ccr_dark');
  if (saved === 'light') {
    state.darkMode = false;
    document.body.classList.remove('dark');
    document.body.classList.add('light');
    updateDarkIcon();
  }

  // Show splash then app
  setTimeout(() => {
    document.getElementById('splash').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    document.getElementById('app').classList.add('active');
    showTab('biblia');
    initBiblia();
    initRadio();
    initJuego();
  }, 1800);
});

/* ── Dark mode ── */
function toggleDarkMode() {
  state.darkMode = !state.darkMode;
  if (state.darkMode) {
    document.body.classList.add('dark');
    document.body.classList.remove('light');
  } else {
    document.body.classList.remove('dark');
    document.body.classList.add('light');
  }
  localStorage.setItem('ccr_dark', state.darkMode ? 'dark' : 'light');
  updateDarkIcon();
  // Update devocional pergamino
  updatePergaminoImage();
}

function updateDarkIcon() {
  const img = document.getElementById('iconDark');
  img.src = state.darkMode ? 'assets/img/noche.png' : 'assets/img/dia.png';
}

/* ── Navigation ── */
function navTo(tab, btn) {
  if (tab === state.currentTab && state.screenStack.length === 0) return;

  // Clear stack
  state.screenStack = [];
  showTab(tab);

  // Update nav buttons
  document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  else document.querySelector(`[data-tab="${tab}"]`)?.classList.add('active');

  // Update toolbar
  setToolbarForRoot(tab);

  // Load data if needed
  if (tab === 'devocional') loadDevocional();
  if (tab === 'juego') onJuegoTabOpened();
}

function showTab(tab) {
  state.currentTab = tab;
  // Hide all pages
  document.querySelectorAll('.page').forEach(p => { p.classList.add('hidden'); p.classList.remove('active'); });
  const screen = document.getElementById(TAB_SCREENS[tab]);
  if (screen) { screen.classList.remove('hidden'); screen.classList.add('active'); }

  // Bottom nav visibility
  document.getElementById('bottomNav').style.display = 'flex';
  document.getElementById('fabPeticiones').classList.toggle('hidden', tab !== 'biblia');
}

function setToolbarForRoot(tab) {
  document.getElementById('toolbarTitle').textContent = 'CCR';
  document.getElementById('btnBack').classList.add('hidden');
  document.getElementById('btnVersion').classList.add('hidden');
}

function pushScreen(screenId, title, showVersion = false) {
  // Save current visible page to stack
  const current = document.querySelector('.page.active');
  if (current) state.screenStack.push(current.id);

  // Hide current
  current?.classList.remove('active');
  current?.classList.add('hidden');

  // Show new
  const next = document.getElementById(screenId);
  if (next) {
    next.classList.remove('hidden');
    next.classList.add('active');
    next.scrollTop = 0;
  }

  // Toolbar
  document.getElementById('toolbarTitle').textContent = title;
  document.getElementById('btnBack').classList.remove('hidden');
  document.getElementById('btnVersion').classList.toggle('hidden', !showVersion);

  // Hide bottom nav for deep screens
  const deepScreens = ['screenVersiculos', 'screenPlayer'];
  document.getElementById('bottomNav').style.display = deepScreens.includes(screenId) ? 'none' : 'flex';
  document.getElementById('fabPeticiones').classList.add('hidden');
}

function goBack() {
  if (state.screenStack.length === 0) {
    navTo(state.currentTab, null);
    return;
  }

  // Hide current
  const current = document.querySelector('.page.active');
  current?.classList.remove('active');
  current?.classList.add('hidden');

  // Show previous
  const prevId = state.screenStack.pop();
  const prev = document.getElementById(prevId);
  if (prev) {
    prev.classList.remove('hidden');
    prev.classList.add('active');
  }

  // Update toolbar
  if (state.screenStack.length === 0) {
    setToolbarForRoot(state.currentTab);
    document.getElementById('bottomNav').style.display = 'flex';
    if (state.currentTab === 'biblia') document.getElementById('fabPeticiones').classList.remove('hidden');
  } else {
    // Determine title for prev screen
    document.getElementById('btnBack').classList.remove('hidden');
    if (prevId === 'screenCapitulos') {
      document.getElementById('toolbarTitle').textContent = window.currentLibroNombre || '';
      document.getElementById('btnVersion').classList.add('hidden');
      document.getElementById('bottomNav').style.display = 'flex';
    } else if (prevId === 'screenVideos') {
      document.getElementById('toolbarTitle').textContent = window.currentGrupoNombre || '';
      document.getElementById('bottomNav').style.display = 'flex';
    }
  }

  // Stop radio player if navigating away
  if (current?.id === 'screenPlayer') stopYTPlayer();
}

/* ── Peticiones ── */
function abrirPeticiones() {
  pushScreen('screenPeticiones', 'Peticiones de Oración');
}
