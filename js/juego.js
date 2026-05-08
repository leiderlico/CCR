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
  tiempoTotal: 60,
  preguntasRespondidas: 0,
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
    const res = await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'text/plain;charset=utf-8' } });
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
  document.getElementById('rankingLoading')?.classList.remove('hidden');
  try {
    const body = JSON.stringify({ action: 'obtener_ranking' });
    const res = await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'text/plain;charset=utf-8' } });
    const data = await res.json();
    mostrarRanking(data);
    mostrarRankingCompleto(data);
  } catch(e) { /* silent */ }
  document.getElementById('rankingLoading')?.classList.add('hidden');
}

function mostrarRanking(data) {
  const box = document.getElementById('rankingBox');
  if (!data) return;
  const difs = ['facil','medio','dificil'];
  const labels = {'facil':'FACIL','medio':'MEDIO','dificil':'DIFICIL'};
  let html = '';
  difs.forEach(dif => {
    const top = data[`top_${dif}`] || [];
    html += `<h4>${labels[dif]}</h4><div class="ranking-medals">`;
    top.slice(0,3).forEach((j, i) => {
      html += `<div class="ranking-medal rank-${i + 1}"><span class="ranking-pos">${i+1}</span><span class="ranking-nombre">${escapeHtml(firstName(j.nombre))}</span><span class="ranking-pts">${getRankingPoints(j, dif)}pts</span></div>`;
    });
    for (let i = top.length; i < 3; i++) {
      html += `<div class="ranking-medal rank-${i + 1}"><span class="ranking-pos">${i+1}</span><span class="ranking-nombre">-</span><span class="ranking-pts">0pts</span></div>`;
    }
    html += '</div>';
  });
  if (html) { box.innerHTML = html; box.classList.remove('hidden'); }
}

async function iniciarJuego(dificultad) {
  juegoState.dificultad = dificultad;
  juegoState.correctas = 0;
  juegoState.preguntaActual = 0;
  juegoState.preguntasRespondidas = 0;

  // Las preguntas vienen locales, igual que en la app Android.
  try {
    const url = getAppBaseUrl() + '/data/preguntas_biblicas.json';
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
    if (juegoState.preguntas.length === 0) throw new Error('Sin preguntas para ' + dificultad);
  } catch(e) {
    alert('No se pudieron cargar las preguntas. Verifica tu conexión.');
    return;
  }

  showJuegoPanel('panelJugando');
  mostrarPregunta();
  iniciarTimerGlobal();
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

}

function actualizarTimer(t) {
  document.getElementById('timerText').textContent = Math.max(0, t).toFixed(1);
  const circum = 100.5;
  const offset = circum - (t / juegoState.tiempoTotal) * circum;
  document.getElementById('timerCircle').setAttribute('stroke-dashoffset', offset);
  document.getElementById('timerCircle').setAttribute('stroke', t > 20 ? '#f5c518' : '#e74c3c');
}

function responder(btn, respuesta, correcta) {
  if (juegoState.respondida) return;
  juegoState.respondida = true;

  const esCorrecta = respuesta === correcta;
  if (esCorrecta) juegoState.correctas++;
  juegoState.preguntasRespondidas++;
  avanzarPregunta();
}

function avanzarPregunta() {
  juegoState.preguntaActual++;
  mostrarPregunta();
}

function iniciarTimerGlobal() {
  clearInterval(juegoState.timer);
  juegoState.tiempoRestante = juegoState.tiempoTotal;
  actualizarTimer(juegoState.tiempoRestante);
  juegoState.timer = setInterval(() => {
    juegoState.tiempoRestante = Math.max(0, juegoState.tiempoRestante - 0.1);
    actualizarTimer(juegoState.tiempoRestante);
    if (juegoState.tiempoRestante <= 0) terminarJuego(juegoState.preguntasRespondidas);
  }, 100);
}

async function terminarJuego(preguntasRespondidas = juegoState.preguntas.length) {
  clearInterval(juegoState.timer);
  const puntaje = calcularPuntaje(juegoState.correctas, preguntasRespondidas, juegoState.tiempoTotal - juegoState.tiempoRestante);
  document.getElementById('resultadoTexto').textContent = `${puntaje} pts`;
  showJuegoPanel('panelResultado');

  // Save score
  try {
    const body = JSON.stringify({
      action: 'sumar_puntaje',
      id: juegoState.jugadorId,
      puntaje,
      dificultad: juegoState.dificultad
    });
    await fetch(SCRIPT_URL, { method: 'POST', body, headers: { 'Content-Type': 'text/plain;charset=utf-8' } });
  } catch(e) { /* silent */ }
}

function volverDificultad() {
  cargarRanking().then(() => showJuegoPanel('panelDificultad'));
}

function abrirRankingJuego() {
  pushScreen('screenRanking', 'Ranking');
  cargarRanking();
}

function calcularPuntaje(correctas, respondidas, tiempoUsadoSeg) {
  const r = Math.max(1, respondidas);
  const c = correctas;
  const fp = Math.pow(c / r, 1.5);
  const fc = Math.pow(r / 10, 1.2);
  const ft = Math.exp(-2 * tiempoUsadoSeg / juegoState.tiempoTotal);
  return Math.max(0, Math.min(1000, Math.trunc(1000 * fp * fc * ft)));
}

function mostrarRankingCompleto(data) {
  const box = document.getElementById('rankingCompleto');
  if (!box || !data) return;
  const difs = [
    ['facil', 'FACIL'],
    ['medio', 'MEDIO'],
    ['dificil', 'DIFICIL']
  ];
  box.innerHTML = difs.map(([dif, label]) => {
    const rows = (data[`top_${dif}`] || []).map((j, i) => `
      <div class="ranking-full-row">
        <span class="ranking-full-pos">${i < 3 ? ['1','2','3'][i] : i + 1}</span>
        <span class="ranking-full-name">${escapeHtml(j.nombre || '-')}</span>
        <strong>${getRankingPoints(j, dif)} pts</strong>
      </div>
    `).join('') || '<p class="empty-message">Sin datos aun.</p>';
    return `<section class="ranking-section ${dif}"><h3>${label}</h3>${rows}</section>`;
  }).join('');
}

function getRankingPoints(jugador, dif) {
  const field = { facil: 'p_facil', medio: 'p_medio', dificil: 'p_dificil' }[dif] || 'puntaje';
  return Number(jugador?.[field] ?? jugador?.puntaje ?? 0) || 0;
}

function shuffleArr(arr) {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function getAppBaseUrl() {
  const loc = window.location.href;
  if (loc.includes('github.io')) return loc.split('/').slice(0, 4).join('/');
  return '.';
}

function firstName(nombre) {
  return String(nombre || '').trim().split(/\s+/)[0] || '-';
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
