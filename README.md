# 🎬 Mis Películas — App Android (Java)

Trabajo Final — Diseño y Desarrollo de Aplicaciones Móviles (Android)

Esta app permite **registrar, listar, editar y eliminar películas favoritas**,
guardando los datos en una base de datos **SQLite local**, mostrándolos en un
**RecyclerView** con imágenes locales según el género, y consultando una
**API REST pública con Retrofit** para mostrar una "reseña de ejemplo".

---

## 1. ¿Qué hace cada parte de la app? (explicación general)

| Requisito del curso | Dónde está implementado |
|---|---|
| Layouts, controles básicos, RecyclerView | `res/layout/*.xml`, `MovieAdapter.java` |
| Activity + Fragment con comunicación | `MainActivity.java` ↔ `MovieFormFragment.java` |
| SQLite con CRUD completo | `db/MovieDbHelper.java` |
| Consumo de API REST con Retrofit | `api/ApiService.java`, `api/RetrofitClient.java`, `api/PostResponse.java` |
| Imágenes locales por género | `res/drawable/poster_*.xml` |
| AlertDialog de confirmación | `MainActivity.confirmarEliminacion()` |

### Flujo de la app
1. **MainActivity** se abre y muestra el `RecyclerView` con las películas
   guardadas en SQLite (`dbHelper.obtenerTodas()`).
2. Al presionar el botón flotante **"+"**, `MainActivity` reemplaza el
   contenedor de la pantalla con **`MovieFormFragment`** (Activity → Fragment).
3. El usuario llena el formulario (título, año, género, calificación) y
   presiona **Guardar**. El Fragment valida los datos y llama al método
   `alGuardarPelicula()` de la interfaz `OnMovieSavedListener`
   (**Fragment → Activity**, comunicación de vuelta).
4. `MainActivity` recibe esos datos, los guarda en SQLite (INSERT o UPDATE
   según corresponda) y vuelve a mostrar la lista actualizada.
5. Al tocar los "tres puntos" de una película se abre un menú con:
   - **Editar** → vuelve a abrir el Fragment, mismo flujo que el punto 2/3,
     pero precargado con los datos existentes.
   - **Eliminar** → se muestra un `AlertDialog` de confirmación antes de
     borrar el registro de SQLite.
   - **Ver reseña de ejemplo (API)** → se hace una petición HTTP con
     **Retrofit** a la API pública JSONPlaceholder, y el resultado (título +
     cuerpo del "post") se muestra dentro de un `AlertDialog`, simulando una
     reseña de la película.

### Cada película guardada, ¿qué imagen usa?
`MovieAdapter.obtenerPosterSegunGenero()` elige un recurso local
(`res/drawable/poster_*.xml`) según el texto del género: Acción, Comedia,
Drama, Terror, Animación, o un póster genérico para cualquier otro caso.
**Ninguna imagen se descarga de internet**: todas viven dentro del proyecto.

---

## 2. Paso a paso para abrir y ejecutar el proyecto en Android Studio

1. **Descomprime** el archivo `MisPeliculas.zip` en cualquier carpeta de tu
   computadora (por ejemplo, en `Documentos/MisPeliculas`).
2. Abre **Android Studio** (versión Hedgehog / 2023.1.1 o más reciente,
   recomendado por usar Android Gradle Plugin 8.2).
3. En la pantalla de bienvenida, haz clic en **"Open"** (Abrir) y selecciona
   la carpeta `MisPeliculas` que acabas de descomprimir (la que contiene el
   archivo `settings.gradle`).
4. Espera a que Android Studio haga el **"Gradle Sync"** (sincronización).
   * La primera vez puede tardar varios minutos porque descarga las
     dependencias (Retrofit, Material Components, etc.).
   * Si Android Studio pregunta si deseas **actualizar/crear el Gradle
     Wrapper**, acepta ("OK" / "Use Gradle wrapper"): esto es normal, ya que
     el proyecto no incluye el binario `gradle-wrapper.jar` (pesa varios MB
     y Android Studio lo genera automáticamente).
5. Una vez terminado el sync, conecta un **dispositivo Android físico** (con
   depuración USB activada) o crea un **emulador (AVD)** desde
   `Tools > Device Manager`.
6. Presiona el botón verde **▶ Run 'app'** (o `Shift + F10`).
7. Android Studio compilará la app, instalará el APK en el dispositivo/emulador
   y la abrirá automáticamente. ¡Ya puedes registrar tu primera película!

### ¿Cómo genero el archivo APK para entregar?
1. En el menú superior ve a **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
2. Espera a que termine la compilación (aparece una notificación abajo a la
   derecha: "APK(s) generated successfully").
3. Haz clic en **"locate"** dentro de esa notificación, o ve manualmente a
   la carpeta: `app/build/outputs/apk/debug/app-debug.apk`.
4. Ese archivo `app-debug.apk` es el que puedes entregar / instalar en
   cualquier dispositivo Android (activando "Instalar apps de orígenes
   desconocidos" si lo instalas manualmente).

### Verificación rápida de que todo funciona
- [ ] Registrar una película nueva y verla aparecer en la lista.
- [ ] Editar esa película y confirmar que los cambios se reflejan.
- [ ] Eliminar una película y ver el `AlertDialog` de confirmación.
- [ ] Tocar "Ver reseña de ejemplo (API)" con el dispositivo conectado a
      internet, y comprobar que aparece un texto (requiere el permiso de
      Internet, ya declarado en el `AndroidManifest.xml`).
- [ ] Cerrar y volver a abrir la app: las películas deben seguir ahí
      (persistencia real en SQLite, no en memoria).

---

---

## 3. Mejoras agregadas (selección visual de póster y descripción)

Esta versión agrega, sobre el proyecto original que ya funcionaba:

| Mejora | Archivos involucrados |
|---|---|
| Selección visual de póster (cuadrícula 2 columnas) | `PosterAdapter.java`, `item_poster_seleccionable.xml`, `ic_check_circle.xml` |
| Conversión texto ↔ drawable en un solo lugar | `util/PosterUtils.java` |
| Campo de descripción | `fragment_movie_form.xml`, `MovieFormFragment.java` |
| Migración SQLite sin perder datos | `MovieDbHelper.java` (ver `onUpgrade`) |
| Póster real mostrado en la lista (ya no depende del género) | `MovieAdapter.java` |
| Esquinas redondeadas en los pósteres | `item_movie.xml`, `item_poster_seleccionable.xml`, `themes.xml` |

**Las 10 imágenes de póster** van en `app/src/main/res/drawable/` con estos nombres exactos
(el proyecto ya incluye una versión *placeholder* generada localmente para que compile;
puedes reemplazarlas por tus propias fotos usando el mismo nombre de archivo, sin tocar el código):

```
poster_accion.jpg           poster_ciencia_ficcion.jpg
poster_comedia.jpg          poster_aventura.jpg
poster_drama.jpg            poster_romance.jpg
poster_terror.jpg           poster_suspenso.jpg
poster_animacion.jpg        poster_generico.jpg
```

---

## 4. Estructura del proyecto

```
MisPeliculas/
├── build.gradle                  # Configuración raíz de Gradle
├── settings.gradle               # Declara el módulo "app"
├── gradle.properties
├── gradle/wrapper/               # Configuración del Gradle Wrapper
└── app/
    ├── build.gradle              # Dependencias: Retrofit, Material, RecyclerView…
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml   # Permiso de Internet + declaración de MainActivity
        ├── java/com/example/mispeliculas/
        │   ├── MainActivity.java         # Activity principal (RecyclerView + lógica)
        │   ├── model/Movie.java          # Modelo de datos de una película
        │   ├── db/MovieDbHelper.java     # SQLite: CRUD completo
        │   ├── adapter/MovieAdapter.java # Adapter del RecyclerView
        │   ├── ui/MovieFormFragment.java # Fragment del formulario
        │   └── api/                      # Retrofit: ApiService, RetrofitClient, PostResponse
        └── res/
            ├── layout/            # activity_main, item_movie, fragment_movie_form
            ├── drawable/          # Pósters locales por género + íconos
            ├── values/            # strings, colors, themes
            └── mipmap-*/          # Ícono de la app en distintas resoluciones
```

---

## 4. Notas para la sustentación / entrega

- El código está **comentado línea por línea en español**, explicando el
  "qué" y el "por qué" de cada instrucción, tanto en las clases Java como
  en los layouts XML.
- Se usó **SQLiteOpenHelper** manual (sin Room) para que se vea explícitamente
  cada sentencia SQL del CRUD, tal como se pide en el curso.
- Se usó **Retrofit + Gson** apuntando a la API pública y gratuita
  `https://jsonplaceholder.typicode.com/`, reutilizando el endpoint
  `posts/{id}` para simular una "reseña de ejemplo" de cada película.
- La comunicación **Fragment → Activity** se implementó con el patrón de
  interfaz de callback (`OnMovieSavedListener`), que es la forma recomendada
  oficialmente por Android (evita acoplar el Fragment a una Activity concreta).
- El documento de publicación en Google Play se entrega por separado como
  `Documento_Publicacion_MisPeliculas.pdf`.
