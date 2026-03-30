const ESTACIONES = [
  { nombre: 'Radio Melodía Celestial',  frecuencia: '105.1 FM', url: 'https://stream-178.zeno.fm/1g5aal5p0hfvv',      img: 'assets/img/uno.jpg'   },
  { nombre: 'Minuto de Dios CTG',        frecuencia: '89.5 FM',  url: 'https://sp4.colombiatelecom.com.co/8042/stream', img: 'assets/img/dos.jpg'   },
  { nombre: 'Verdad y Vida Radio',       frecuencia: '100.1 AM', url: 'https://stream-204.zeno.fm/vvon5xkmvhwvv',      img: 'assets/img/tres.jpg'  },
  { nombre: 'Radio Estrella',            frecuencia: '89.3 FM',  url: 'https://stream-204.zeno.fm/fhgeiizn0xguv',      img: 'assets/img/cuatro.jpg'},
  { nombre: 'Radio Cristiana Colombia',  frecuencia: '',         url: 'https://stream-204.zeno.fm/a3y646rxcfhvv',      img: 'assets/img/cinco.jpg' },
  { nombre: 'Cielo Cartagena',           frecuencia: '103.0 FM', url: 'https://stream-204.zeno.fm/v2pspfgm54zuv',      img: 'assets/img/seis.jpg'  },
  { nombre: 'Alfa y Omega',              frecuencia: '',         url: 'https://stream-204.zeno.fm/ebsnme8phdovv',      img: 'assets/img/siete.jpg' },
];

let radioAudio = null;
let radioPlaying = false;
let radioIndex = 0;

function initRadio() {
  const lista = document.getElementById('radioLista');
  lista.innerHTML = '';
  ESTACIONES.forEach((est, i) => {
    const card = document.createElement('div');
    card.className = 'radio-card';
    card.id = `radioCard${i}`;
    card.innerHTML = `
      <img src="${est.img}" alt="${est.nombre}" onerror="this.src='assets/img/ic_radio.png'"/>
      <div class="radio-card-info">
        <h4>${est.nombre}</h4>
        <span>${est.frecuencia}</span>
      </div>
    `;
    card.onclick = () => seleccionarEstacion(i);
    lista.appendChild(card);
  });
}

function seleccionarEstacion(i) {
  // Update active card
  document.querySelectorAll('.radio-card').forEach(c => c.classList.remove('active-card'));
  document.getElementById(`radioCard${i}`)?.classList.add('active-card');

  radioIndex = i;
  const est = ESTACIONES[i];

  // Update anuncio
  document.getElementById('radioNombre').textContent = est.nombre;
  document.getElementById('radioFrecuencia').textContent = est.frecuencia;
  document.getElementById('radioImg').src = est.img;
  document.getElementById('radioImg').onerror = () => { document.getElementById('radioImg').src = 'assets/img/ic_radio.png'; };

  // Auto play
  reproducirEstacion(est);
}

function reproducirEstacion(est) {
  if (radioAudio) {
    radioAudio.pause();
    radioAudio = null;
  }
  radioPlaying = false;
  setRadioEstado('Conectando…', false);

  radioAudio = new Audio(est.url);
  radioAudio.crossOrigin = 'anonymous';

  radioAudio.oncanplay = () => {
    radioAudio.play().then(() => {
      radioPlaying = true;
      setRadioEstado('En vivo', true);
      updatePlayPauseBtn(true);
    }).catch(() => {
      setRadioEstado('Error al reproducir', false);
      updatePlayPauseBtn(false);
    });
  };

  radioAudio.onerror = () => {
    setRadioEstado('Error de conexión', false);
    radioPlaying = false;
    updatePlayPauseBtn(false);
  };

  radioAudio.onended = () => {
    radioPlaying = false;
    setRadioEstado('Detenido', false);
    updatePlayPauseBtn(false);
  };

  radioAudio.load();
}

function toggleRadio() {
  if (!radioAudio) {
    // Play first station
    seleccionarEstacion(radioIndex);
    return;
  }
  if (radioPlaying) {
    radioAudio.pause();
    radioPlaying = false;
    setRadioEstado('Detenido', false);
    updatePlayPauseBtn(false);
  } else {
    radioAudio.play().then(() => {
      radioPlaying = true;
      setRadioEstado('En vivo', true);
      updatePlayPauseBtn(true);
    }).catch(() => reproducirEstacion(ESTACIONES[radioIndex]));
  }
}

function setRadioEstado(texto, live) {
  const el = document.getElementById('radioEstado');
  el.textContent = texto;
  el.className = 'radio-estado' + (live ? ' live' : '');
}

function updatePlayPauseBtn(playing) {
  document.getElementById('iconPlay').classList.toggle('hidden', playing);
  document.getElementById('iconPause').classList.toggle('hidden', !playing);
}
