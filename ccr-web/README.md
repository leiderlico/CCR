# CCR Biblia Web 🌐

App web completa de tu app Android CCR. Funciona en **iPhone, Android, PC** — sin instalar nada.

---

## 📁 Estructura del proyecto

```
ccr-web/
│
├── index.html                  ← toda la app (una sola página)
│
└── public/                     ← archivos de datos (JSON)
    ├── devocionales.json       ← ✅ ya incluido (de tu app)
    ├── preguntas_biblicas.json ← ✅ ya incluido (de tu app)
    └── biblia_rv1960.json      ← ⚠️ necesitas copiarlo de tu APK (ver abajo)
```

---

## ⚠️ Paso 1 — Copiar la Biblia RV1960

Tu archivo `biblia_rv1960.json` está en los **assets** de tu app Android (no viene en el ZIP de recursos).

Para obtenerlo, tienes 2 opciones:

### Opción A — Extraer del APK (recomendado)
1. Genera el APK de debug desde Android Studio: **Build → Build APK(s)**
2. El APK es un ZIP, renómbralo a `.zip` y ábrelo
3. Encuentra `assets/biblia_rv1960.json`
4. Cópialo a la carpeta `public/` de este proyecto

### Opción B — Desde el proyecto Android
Si tienes acceso al proyecto Android, ve a:
```
app/src/main/assets/biblia_rv1960.json
```
Cópialo a `ccr-web/public/biblia_rv1960.json`

---

## 🚀 Paso 2 — Publicar GRATIS en GitHub Pages

### 2.1 Crear repositorio en GitHub
1. Ve a **github.com** → crea una cuenta gratis si no tienes
2. Nuevo repositorio → llámalo `ccr-biblia`
3. Hazlo **público** (para GitHub Pages gratis)

### 2.2 Subir los archivos
```bash
# En tu computador, en la carpeta ccr-web/:
git init
git add .
git commit -m "CCR Biblia Web v1"
git remote add origin https://github.com/TUUSUARIO/ccr-biblia.git
git push -u origin main
```

### 2.3 Activar GitHub Pages
1. En tu repositorio → **Settings** → **Pages**
2. Source: **Deploy from a branch**
3. Branch: `main` → carpeta: `/ (root)`
4. Guardar

✅ En ~2 minutos tu app estará en:
```
https://TUUSUARIO.github.io/ccr-biblia
```

---

## 📱 Agregar a la pantalla de inicio (iPhone)

1. Abre `https://TUUSUARIO.github.io/ccr-biblia` en **Safari**
2. Toca el botón de compartir (cuadrado con flecha)
3. **"Agregar a pantalla de inicio"**
4. ¡Aparece como app nativa! 🎉

---

## 🎯 Funciones incluidas

| Función | Estado |
|---------|--------|
| 📖 Lector de Biblia RV1960 | ✅ |
| 🔍 Búsqueda de versículos | ✅ |
| 📅 Devocional del día | ✅ |
| 📋 Lista de todos los devocionales | ✅ |
| 🏆 Trivia bíblica (fácil/medio/difícil) | ✅ |
| ❤️ Versículos famosos | ✅ |
| 🙏 Peticiones de oración (guardadas localmente) | ✅ |
| 🌙 Tema oscuro | ✅ |
| 📱 PWA — funciona en iPhone | ✅ |

---

## 🔧 Para agregar más versiones de Biblia (NVI, NTV, etc.)

En `index.html`, busca el objeto `VERSIONES`:

```javascript
const VERSIONES = {
  'RV1960': { label: 'RV1960', file: '/biblia_rv1960.json' },
  'NVI':    { label: 'NVI',    file: '/biblia_nvi.json'    },
  'NTV':    { label: 'NTV',    file: '/biblia_ntv.json'    },
};
```

Agrega el JSON correspondiente en `public/` y aparecerá automáticamente como chip seleccionable.

---

## 💡 Preguntas frecuentes

**¿Funciona sin internet?**
Solo si conviertes a PWA completa con Service Worker. Por ahora necesita conexión para cargar la primera vez.

**¿Mis peticiones se guardan?**
Sí, en el `localStorage` del navegador del dispositivo.

**¿Puedo subir más versiones de Biblia?**
Sí, agrega los JSON en `public/` y actualiza el objeto `VERSIONES` en `index.html`.

**¿Puedo cambiar los colores?**
Sí, edita las variables CSS en la sección `:root { }` al inicio de `index.html`.
