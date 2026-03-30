let devocionalLoaded = false;

const DEV_BASE_URL = (() => {
  const loc = window.location.href;
  if (loc.includes('github.io')) return loc.split('/').slice(0, 4).join('/');
  return '.';
})();

function updatePergaminoImage() {
  const img = document.getElementById('imgPergamino');
  if (!img) return;
  img.src = state.darkMode
    ? 'assets/img/pergaminodevocionalnight.jpg'
    : 'assets/img/pergaminodevocional.jpg';
}

async function loadDevocional() {
  if (devocionalLoaded) return;
  document.getElementById('devLoading').classList.remove('hidden');
  updatePergaminoImage();

  try {
    const cached = sessionStorage.getItem('devocionales');
    let lista;
    if (cached) {
      lista = JSON.parse(cached);
    } else {
      const url = DEV_BASE_URL + '/data/devocionales.json';
      const res = await fetch(url);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      lista = await res.json();
      sessionStorage.setItem('devocionales', JSON.stringify(lista));
    }

    const hoy = new Date();
    const inicio = new Date(hoy.getFullYear(), 0, 0);
    const diaAnio = Math.floor((hoy - inicio) / (1000 * 60 * 60 * 24));

    const dev = lista.find(d => d.dia_del_año === diaAnio && d.año === hoy.getFullYear())
              || lista.find(d => d.dia_del_año === diaAnio)
              || lista[0];

    if (dev) {
      document.getElementById('devFecha').textContent = (dev.fecha || '').toUpperCase();
      document.getElementById('devTitulo').textContent = dev.titulo || '';
      document.getElementById('devVersiculo').textContent = dev.versiculo_texto || '';
      document.getElementById('devRef').textContent = dev.versiculo_referencia ? '— ' + dev.versiculo_referencia.toUpperCase() : '';
      document.getElementById('devCuerpo').textContent = dev.cuerpo || '';
    }
    devocionalLoaded = true;
  } catch(e) {
    document.getElementById('devFecha').textContent = 'No disponible';
    document.getElementById('devCuerpo').textContent = 'El devocional no pudo cargarse.';
    devocionalLoaded = false;
  } finally {
    document.getElementById('devLoading').classList.add('hidden');
  }
}
