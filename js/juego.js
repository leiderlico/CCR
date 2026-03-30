const SCRIPT_URL = 'https://script.google.com/macros/s/AKfycbwi2SBX5TjxPSPiK2oFR6pnQv7-T-8ByXNZDOwh1OTm8VPg4IAPffLPCSO-N5tIPaCzRg/exec';

let juegoState = {
  jugadorId: null,
  jugadorNombre: null,
  preguntas: [],
  preguntaActual: 0,
  correctas: 0,
  dificultad: 'facil',
  timer: null,
  tiempoRestante: 60,
  respondida: false
};

function onJuegoTabOpened() {
  verificarRegistro();
}

function showJuegoPanel(id) {
  ['panelVerificando','panelRegistro','panelDificultad','panelJugando','panelResultado'].forEach(p => {
    document.getElementById(p).classList.toggle('hidden', p !== id);
  });
}

async function verificarRegistro() {
  showJuegoPanel('panelVerificando');
  const guardado = localStorage.getItem('ccr_jugador');
  if (guardado) {
    const j = JSON.parse(guardado);
    juegoState.jugadorId = j.id;
    juegoState.jugadorNombre = j.nombre;
    document.getElementById('saludoJuego').textContent = `¡Hola, ${j.nombre.split(' ')[0]}!`;
    await cargarRanking();
    showJuegoPanel('panelDificultad');
  } else {
    showJuegoPanel('panelRegistro');
  }
}

async function registrarJugador() {
  const nombre = document.getElementById('regNombre').value.trim();
  const apellido = document.getElementById('regApellido').value.trim();
  if (!nombre || !apellido) { alert('Por favor completa los campos'); return; }

  const id = crypto.randomUUID ? crypto.randomUUID() : Date.now().toString();

  try {
    const body = JSON.stringify({ action: 'registrar', id, nombre, apellido });
    const res = await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'application/json' } });
    const data = await res.json();
    if (data.ok || data.id) {
      const jugador = { id: data.id || id, nombre: `${nombre} ${apellido}` };
      localStorage.setItem('ccr_jugador', JSON.stringify(jugador));
      juegoState.jugadorId = jugador.id;
      juegoState.jugadorNombre = jugador.nombre;
      document.getElementById('saludoJuego').textContent = `¡Hola, ${nombre}!`;
      await cargarRanking();
      showJuegoPanel('panelDificultad');
    } else {
      alert(data.mensaje || 'Error al registrarse');
    }
  } catch(e) {
    // Offline fallback: register locally
    const jugador = { id, nombre: `${nombre} ${apellido}` };
    localStorage.setItem('ccr_jugador', JSON.stringify(jugador));
    juegoState.jugadorId = id;
    juegoState.jugadorNombre = jugador.nombre;
    document.getElementById('saludoJuego').textContent = `¡Hola, ${nombre}!`;
    showJuegoPanel('panelDificultad');
  }
}

async function cargarRanking() {
  try {
    const body = JSON.stringify({ action: 'obtener_ranking' });
    const res = await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'application/json' } });
    const data = await res.json();
    mostrarRanking(data);
  } catch(e) { /* silent */ }
}

function mostrarRanking(data) {
  const box = document.getElementById('rankingBox');
  if (!data) return;
  const difs = ['facil','medio','dificil'];
  const labels = {'facil':'Fácil','medio':'Medio','dificil':'Difícil'};
  let html = '';
  difs.forEach(dif => {
    const top = data[`top_${dif}`] || [];
    if (top.length === 0) return;
    html += `<h4>🏆 ${labels[dif]}</h4>`;
    top.slice(0,5).forEach((j, i) => {
      html += `<div class="ranking-row"><span class="ranking-pos">${i+1}.</span><span class="ranking-nombre">${j.nombre}</span><span class="ranking-pts">${j.puntaje}</span></div>`;
    });
  });
  if (html) { box.innerHTML = html; box.classList.remove('hidden'); }
}

async function iniciarJuego(dificultad) {
  juegoState.dificultad = dificultad;
  juegoState.correctas = 0;
  juegoState.preguntaActual = 0;

  // Load questions from GitHub
  try {
    const url = `' + (window.location.href.includes('github.io') ? window.location.href.split('/').slice(0,4).join('/') : '.') + '/data/preguntas_biblicas.json'`;
    const cached = sessionStorage.getItem('preguntas');
    let todas;
    if (cached) {
      todas = JSON.parse(cached);
    } else {
      const res = await fetch(url);
      todas = await res.json();
      sessionStorage.setItem('preguntas', JSON.stringify(todas));
    }
    const filtradas = todas.filter(p => p.dificultad === dificultad);
    juegoState.preguntas = shuffleArr(filtradas).slice(0, 10);
  } catch(e) {
    alert('No se pudieron cargar las preguntas. Verifica tu conexión.');
    return;
  }

  showJuegoPanel('panelJugando');
  mostrarPregunta();
}

function mostrarPregunta() {
  const { preguntas, preguntaActual } = juegoState;
  if (preguntaActual >= preguntas.length) { terminarJuego(); return; }

  const p = preguntas[preguntaActual];
  document.getElementById('jPregNum').textContent = `Pregunta ${preguntaActual + 1} / ${preguntas.length}`;
  document.getElementById('jPregunta').textContent = p.pregunta;
  juegoState.respondida = false;

  const opciones = shuffleArr([p.correcta, p.opcion_b, p.opcion_c, p.opcion_d]);
  const wrap = document.getElementById('jOpciones');
  wrap.innerHTML = '';
  opciones.forEach(op => {
    const btn = document.createElement('button');
    btn.className = 'opcion-btn';
    btn.textContent = op;
    btn.onclick = () => responder(btn, op, p.correcta);
    wrap.appendChild(btn);
  });

  // Timer
  clearInterval(juegoState.timer);
  juegoState.tiempoRestante = 60;
  actualizarTimer(60);
  juegoState.timer = setInterval(() => {
    juegoState.tiempoRestante--;
    actualizarTimer(juegoState.tiempoRestante);
    if (juegoState.tiempoRestante <= 0) {
      clearInterval(juegoState.timer);
      if (!juegoState.respondida) avanzarPregunta();
    }
  }, 1000);
}

function actualizarTimer(t) {
  document.getElementById('timerText').textContent = t;
  const circum = 100.5;
  const offset = circum - (t / 60) * circum;
  document.getElementById('timerCircle').setAttribute('stroke-dashoffset', offset);
  document.getElementById('timerCircle').setAttribute('stroke', t > 20 ? '#f5c518' : '#e74c3c');
}

function responder(btn, respuesta, correcta) {
  if (juegoState.respondida) return;
  juegoState.respondida = true;
  clearInterval(juegoState.timer);

  const esCorrecta = respuesta === correcta;
  if (esCorrecta) juegoState.correctas++;

  // Highlight all buttons
  document.querySelectorAll('.opcion-btn').forEach(b => {
    b.disabled = true;
    if (b.textContent === correcta) b.classList.add('correcta');
    else if (b === btn && !esCorrecta) b.classList.add('incorrecta');
  });

  setTimeout(avanzarPregunta, 1200);
}

function avanzarPregunta() {
  juegoState.preguntaActual++;
  mostrarPregunta();
}

async function terminarJuego() {
  clearInterval(juegoState.timer);
  const puntaje = juegoState.correctas * 100;
  document.getElementById('resultadoTexto').textContent = `${juegoState.correctas} / ${juegoState.preguntas.length} correctas — ${puntaje} pts`;
  showJuegoPanel('panelResultado');

  // Save score
  try {
    const body = JSON.stringify({
      action: 'sumar_puntaje',
      id: juegoState.jugadorId,
      puntaje,
      dificultad: juegoState.dificultad
    });
    await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'application/json' } });
  } catch(e) { /* silent */ }
}

function volverDificultad() {
  cargarRanking().then(() => showJuegoPanel('panelDificultad'));
}

function shuffleArr(arr) {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}
