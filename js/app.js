/* App State */
const state = {
  currentTab: 'biblia',
  screenStack: [],
  darkMode: true,
  historyDepth: 0,
  restoringHistory: false
};

const TAB_SCREENS = {
  biblia: 'screenLibros',
  grupos: 'screenGrupos',
  devocional: 'screenDevocional',
  juego: 'screenJuego',
  radio: 'screenRadio'
};

window.addEventListener('DOMContentLoaded', () => {
  document.addEventListener('click', event => {
    const menu = document.getElementById('toolbarMenu');
    const trigger = document.getElementById('btnToolbarMenu');
    if (!menu || menu.classList.contains('hidden')) return;
    if (menu.contains(event.target) || trigger?.contains(event.target)) return;
    closeToolbarMenu();
  });

  const saved = localStorage.getItem('ccr_dark');
  if (saved === 'light') {
    state.darkMode = false;
    document.body.classList.remove('dark');
    document.body.classList.add('light');
    updateDarkIcon();
  }

  setTimeout(() => {
    document.getElementById('splash').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    document.getElementById('app').classList.add('active');
    initBrowserHistory();
    ensureGrupoActions();
    showTab('biblia');
    setToolbarForRoot('biblia');
    replaceHistoryState();
    initBiblia();
    initRadio();
    initJuego();
  }, 1800);
});

function toggleDarkMode() {
  state.darkMode = !state.darkMode;
  document.body.classList.toggle('dark', state.darkMode);
  document.body.classList.toggle('light', !state.darkMode);
  localStorage.setItem('ccr_dark', state.darkMode ? 'dark' : 'light');
  updateDarkIcon();
  updatePergaminoImage();
}

function ensureGrupoActions() {
  document.querySelectorAll('.grupo-card').forEach(card => {
    if (card.querySelector('.grupo-action')) return;
    const action = document.createElement('span');
    action.className = 'grupo-action';
    action.innerHTML = '<img src="assets/img/ic_video.png" alt=""/>';
    card.appendChild(action);
  });
}

function updateDarkIcon() {
  const img = document.getElementById('iconDark');
  img.src = state.darkMode ? 'assets/img/noche.png' : 'assets/img/dia.png';
}

function navTo(tab, btn, options = {}) {
  closeToolbarMenu();
  if (tab === state.currentTab && state.screenStack.length === 0) return;

  state.screenStack = [];
  showTab(tab);

  document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  else document.querySelector(`[data-tab="${tab}"]`)?.classList.add('active');

  setToolbarForRoot(tab);

  if (tab === 'devocional') loadDevocional();
  if (tab === 'juego') onJuegoTabOpened();
  if (!options.skipHistory) pushHistoryState();
}

function showTab(tab) {
  state.currentTab = tab;
  document.querySelectorAll('.page').forEach(p => {
    p.classList.add('hidden');
    p.classList.remove('active');
  });

  const screen = document.getElementById(TAB_SCREENS[tab]);
  if (screen) {
    screen.classList.remove('hidden');
    screen.classList.add('active');
  }

  document.getElementById('bottomNav').style.display = 'flex';
  document.getElementById('fabPeticiones').classList.add('hidden');
}

function setToolbarForRoot() {
  document.getElementById('toolbarTitle').textContent = 'CCR';
  document.getElementById('btnBack').classList.add('hidden');
  document.getElementById('btnVersion').classList.add('hidden');
  document.body.classList.add('root-screen');
}

function pushScreen(screenId, title, showVersion = false, options = {}) {
  closeToolbarMenu();
  const current = document.querySelector('.page.active');
  if (current) state.screenStack.push(current.id);

  current?.classList.remove('active');
  current?.classList.add('hidden');

  const next = document.getElementById(screenId);
  if (next) {
    next.classList.remove('hidden');
    next.classList.add('active');
    next.scrollTop = 0;
  }

  document.getElementById('toolbarTitle').textContent = title;
  document.getElementById('btnBack').classList.remove('hidden');
  document.getElementById('btnVersion').classList.toggle('hidden', !showVersion);
  document.body.classList.remove('root-screen');

  const deepScreens = ['screenVersiculos', 'screenPlayer'];
  document.getElementById('bottomNav').style.display = deepScreens.includes(screenId) ? 'none' : 'flex';
  document.getElementById('fabPeticiones').classList.add('hidden');

  if (!options.skipHistory) pushHistoryState();
}

function goBack(options = {}) {
  if (!options.fromHistory && state.historyDepth > 0) {
    window.history.back();
    return;
  }
  goBackInternal();
}

function goBackInternal() {
  closeToolbarMenu();
  if (state.screenStack.length === 0) {
    navTo(state.currentTab, null, { skipHistory: true });
    return;
  }

  const current = document.querySelector('.page.active');
  current?.classList.remove('active');
  current?.classList.add('hidden');

  const prevId = state.screenStack.pop();
  const prev = document.getElementById(prevId);
  if (prev) {
    prev.classList.remove('hidden');
    prev.classList.add('active');
  }

  if (state.screenStack.length === 0) {
    setToolbarForRoot(state.currentTab);
    document.getElementById('bottomNav').style.display = 'flex';
    document.getElementById('fabPeticiones').classList.add('hidden');
  } else {
    document.getElementById('btnBack').classList.remove('hidden');
    document.body.classList.remove('root-screen');
    if (prevId === 'screenCapitulos') {
      document.getElementById('toolbarTitle').textContent = window.currentLibroNombre || '';
      document.getElementById('btnVersion').classList.add('hidden');
      document.getElementById('bottomNav').style.display = 'flex';
    } else if (prevId === 'screenVideos') {
      document.getElementById('toolbarTitle').textContent = window.currentGrupoNombre || '';
      document.getElementById('bottomNav').style.display = 'flex';
    } else if (prevId === 'screenVersiculos') {
      document.getElementById('toolbarTitle').textContent = window.currentLibroAbbrev && window.currentCapitulo
        ? `${window.currentLibroAbbrev} ${window.currentCapitulo}`
        : (window.currentLibroAbbrev || window.currentLibroNombre || '');
      document.getElementById('btnVersion').classList.remove('hidden');
      document.getElementById('bottomNav').style.display = 'none';
    }
  }

  if (current?.id === 'screenPlayer') stopYTPlayer();
}

function abrirPeticiones() {
  pushScreen('screenPeticiones', 'Peticiones de Oracion');
}

function toggleToolbarMenu(event) {
  event?.stopPropagation();
  document.getElementById('toolbarMenu')?.classList.toggle('hidden');
}

function closeToolbarMenu() {
  document.getElementById('toolbarMenu')?.classList.add('hidden');
}

function abrirPeticionesDesdeMenu() {
  closeToolbarMenu();
  abrirPeticiones();
}

function mostrarAcercaCCR() {
  closeToolbarMenu();
  pushScreen('screenAcerca', 'Acerca de CCR');
}

async function compartirApp() {
  closeToolbarMenu();
  const shareData = { title: 'CCR', text: 'CCR', url: window.location.href };
  try {
    if (navigator.share) {
      await navigator.share(shareData);
    } else if (navigator.clipboard) {
      await navigator.clipboard.writeText(window.location.href);
      alert('Enlace copiado');
    }
  } catch(e) {}
}

function mostrarConfiguracion() {
  closeToolbarMenu();
  mostrarSelectorVersion();
}

function initBrowserHistory() {
  if (!window.history?.replaceState) return;
  window.history.replaceState(getHistorySnapshot(), '', window.location.href);
  window.addEventListener('popstate', event => {
    if (!event.state || !event.state.ccr) return;
    state.restoringHistory = true;
    state.historyDepth = Math.max(0, state.historyDepth - 1);
    restoreHistorySnapshot(event.state);
    state.restoringHistory = false;
  });
}

function getHistorySnapshot() {
  const activePage = document.querySelector('.page.active')?.id || TAB_SCREENS[state.currentTab];
  return {
    ccr: true,
    tab: state.currentTab,
    stack: [...state.screenStack],
    activePage,
    title: document.getElementById('toolbarTitle')?.textContent || 'CCR',
    showVersion: !document.getElementById('btnVersion')?.classList.contains('hidden'),
    bottomHidden: document.getElementById('bottomNav')?.style.display === 'none'
  };
}

function pushHistoryState() {
  if (state.restoringHistory || !window.history?.pushState) return;
  window.history.pushState(getHistorySnapshot(), '', window.location.href);
  state.historyDepth++;
}

function replaceHistoryState() {
  if (!window.history?.replaceState) return;
  window.history.replaceState(getHistorySnapshot(), '', window.location.href);
}

function restoreHistorySnapshot(snapshot) {
  state.currentTab = snapshot.tab || 'biblia';
  state.screenStack = Array.isArray(snapshot.stack) ? [...snapshot.stack] : [];

  document.querySelectorAll('.page').forEach(p => {
    p.classList.add('hidden');
    p.classList.remove('active');
  });

  const activePage = document.getElementById(snapshot.activePage || TAB_SCREENS[state.currentTab]);
  activePage?.classList.remove('hidden');
  activePage?.classList.add('active');

  document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
  document.querySelector(`[data-tab="${state.currentTab}"]`)?.classList.add('active');

  document.getElementById('toolbarTitle').textContent = snapshot.title || 'CCR';
  document.getElementById('btnBack').classList.toggle('hidden', state.screenStack.length === 0);
  document.getElementById('btnVersion').classList.toggle('hidden', !snapshot.showVersion);
  document.getElementById('bottomNav').style.display = snapshot.bottomHidden ? 'none' : 'flex';
  document.getElementById('fabPeticiones').classList.add('hidden');
  document.body.classList.toggle('root-screen', state.screenStack.length === 0);

  if (snapshot.activePage !== 'screenPlayer') stopYTPlayer();
}
