let devocionalLoaded = false;

function updatePergaminoImage() {
  const img = document.getElementById('imgPergamino');
  if (!img) return;
  img.src = state.darkMode
    ? 'assets/img/pergaminodevocionalnight.jpg'
    : 'assets/img/pergaminodevocional.jpg';
}

async function loadDevocional() {
  if (devocionalLoaded) return;

  const loading = document.getElementById('devLoading');
  loading.classList.remove('hidden');
  updatePergaminoImage();

  try {
    // Load from GitHub raw
    const url = 'https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/data/devocionales.json';
    const cached = sessionStorage.getItem('devocionales');
    let lista;

    if (cached) {
      lista = JSON.parse(cached);
    } else {
      const res = await fetch(url);
      if (!res.ok) throw new Error('Error');
      lista = await res.json();
      sessionStorage.setItem('devocionales', JSON.stringify(lista));
    }

    const hoy = new Date();
    const inicio = new Date(hoy.getFullYear(), 0, 0);
    const diff = hoy - inicio;
    const diaAnio = Math.floor(diff / (1000 * 60 * 60 * 24));

    const dev = lista.find(d => d.dia_del_año === diaAnio && d.año === hoy.getFullYear())
              || lista.find(d => d.dia_del_año === diaAnio)
              || lista[0];

    if (dev) {
      document.getElementById('devFecha').textContent = dev.fecha?.toUpperCase() || '';
      document.getElementById('devTitulo').textContent = dev.titulo || '';
      document.getElementById('devVersiculo').textContent = dev.versiculo_texto || '';
      document.getElementById('devRef').textContent = dev.versiculo_referencia ? `— ${dev.versiculo_referencia.toUpperCase()}` : '';
      document.getElementById('devCuerpo').textContent = dev.cuerpo || '';
    }
    devocionalLoaded = true;
  } catch(e) {
    document.getElementById('devFecha').textContent = 'No disponible';
    document.getElementById('devCuerpo').textContent = 'El devocional no pudo cargarse. Verifica tu conexión a internet.';
    devocionalLoaded = false;
  } finally {
    loading.classList.add('hidden');
  }
}
