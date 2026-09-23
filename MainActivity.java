package com.example.mispeliculas;

// MainActivity.java
// -------------------------------------------------------------------
// Activity ÚNICA de la app (patrón "single activity"), responsable de:
//   1) Mostrar el RecyclerView con la lista de películas guardadas.
//   2) Abrir el MovieFormFragment para registrar/editar una película.
//   3) Recibir de vuelta los datos del Fragment (comunicación
//      Fragment -> Activity) e insertarlos/actualizarlos en SQLite.
//   4) Eliminar películas, mostrando antes un AlertDialog de confirmación.
//   5) Consumir la API REST (Retrofit) para mostrar una "reseña de
//      ejemplo" en un diálogo.
// -------------------------------------------------------------------

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mispeliculas.adapter.MovieAdapter;
import com.example.mispeliculas.api.PostResponse;
import com.example.mispeliculas.api.RetrofitClient;
import com.example.mispeliculas.db.MovieDbHelper;
import com.example.mispeliculas.model.Movie;
import com.example.mispeliculas.ui.MovieFormFragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// MainActivity IMPLEMENTA la interfaz que definimos en MovieFormFragment.
// Esto es lo que permite la comunicación Fragment -> Activity: el
// Fragment llama a estos métodos cuando el usuario guarda o cancela.
public class MainActivity extends AppCompatActivity implements MovieFormFragment.OnMovieSavedListener {

    // Ayudante de acceso a la base de datos SQLite (ver MovieDbHelper).
    private MovieDbHelper dbHelper;

    // Adaptador que conecta la lista de películas con el RecyclerView.
    private MovieAdapter adapter;

    // Referencias a las vistas definidas en activity_main.xml
    private RecyclerView recyclerPeliculas;
    private TextView txtListaVacia;
    private TextView txtSubtitulo;
    private android.widget.FrameLayout contenedorFormulario;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAgregar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Conecta esta Activity con su layout XML (activity_main.xml).
        setContentView(R.layout.activity_main);

        // Inicializamos el helper de la base de datos SQLite.
        dbHelper = new MovieDbHelper(this);

        // Enlazamos las vistas del layout con sus variables Java.
        recyclerPeliculas = findViewById(R.id.recyclerPeliculas);
        txtListaVacia = findViewById(R.id.txtListaVacia);
        txtSubtitulo = findViewById(R.id.txtSubtitulo);
        contenedorFormulario = findViewById(R.id.contenedorFormulario);
        fabAgregar = findViewById(R.id.fabAgregar);

        // Configuramos el RecyclerView con un LinearLayoutManager
        // (lista vertical simple, una película debajo de otra).
        recyclerPeliculas.setLayoutManager(new LinearLayoutManager(this));

        // Creamos el Adapter, pasándole la lista actual de la BD y un
        // listener anónimo que reacciona a Editar / Eliminar / Ver reseña.
        adapter = new MovieAdapter(dbHelper.obtenerTodas(), new MovieAdapter.OnMovieActionListener() {
            @Override
            public void alEditar(Movie pelicula) {
                mostrarFormulario(pelicula); // abre el Fragment en modo edición
            }

            @Override
            public void alEliminar(Movie pelicula) {
                confirmarEliminacion(pelicula); // muestra el AlertDialog
            }

            @Override
            public void alVerResena(Movie pelicula) {
                consultarResenaDeEjemplo(pelicula); // llamada Retrofit
            }
        });
        recyclerPeliculas.setAdapter(adapter);

        // El botón flotante (+) abre el formulario en modo "nueva película".
        fabAgregar.setOnClickListener(v -> mostrarFormulario(null));

        // Pintamos la lista inicial (con el mensaje de "vacío" si corresponde).
        actualizarVistaLista();
    }

    // ---------------------------------------------------------------
    // onResume: se ejecuta cada vez que la Activity vuelve a primer
    // plano (por ejemplo, al cerrar el formulario). Refrescamos la
    // lista por seguridad, aunque normalmente ya se actualiza al
    // guardar/eliminar.
    // ---------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        if (contenedorFormulario.getVisibility() != View.VISIBLE) {
            actualizarVistaLista();
        }
    }

    // =================================================================
    // MOSTRAR / OCULTAR EL FORMULARIO (Activity <-> Fragment)
    // =================================================================

    // Muestra el MovieFormFragment. Si "pelicula" es null, se abre en
    // modo "nueva película"; si no es null, se abre en modo "editar".
    private void mostrarFormulario(@Nullable Movie pelicula) {
        MovieFormFragment fragment = MovieFormFragment.nuevaInstancia(pelicula);

        // FragmentTransaction es la forma estándar de agregar, reemplazar
        // o quitar Fragments dentro de un contenedor (contenedorFormulario).
        FragmentTransaction transaccion = getSupportFragmentManager().beginTransaction();
        transaccion.replace(R.id.contenedorFormulario, fragment);
        transaccion.commit();

        // Ocultamos la lista y mostramos el contenedor del formulario.
        recyclerPeliculas.setVisibility(View.GONE);
        txtListaVacia.setVisibility(View.GONE);
        fabAgregar.setVisibility(View.GONE);
        contenedorFormulario.setVisibility(View.VISIBLE);
    }

    // Oculta el formulario y vuelve a mostrar la lista de películas.
    private void ocultarFormulario() {
        contenedorFormulario.setVisibility(View.GONE);
        fabAgregar.setVisibility(View.VISIBLE);
        actualizarVistaLista();
    }

    // =================================================================
    // IMPLEMENTACIÓN de MovieFormFragment.OnMovieSavedListener
    // Aquí es donde la Activity "recibe" lo que el Fragment le envía.
    // =================================================================

    @Override
    public void alGuardarPelicula(Movie pelicula, boolean esNueva) {
        // Según corresponda, insertamos o actualizamos en SQLite.
        if (esNueva) {
            dbHelper.insertarPelicula(pelicula);
        } else {
            dbHelper.actualizarPelicula(pelicula);
        }

        Toast.makeText(this, R.string.mensaje_guardado_ok, Toast.LENGTH_SHORT).show();

        // Cerramos el formulario y refrescamos la lista con los datos
        // más recientes de la base de datos.
        ocultarFormulario();
    }

    @Override
    public void alCancelarFormulario() {
        // El usuario se arrepintió: simplemente cerramos el formulario
        // sin tocar la base de datos.
        ocultarFormulario();
    }

    // =================================================================
    // ELIMINAR PELÍCULA (con AlertDialog de confirmación)
    // =================================================================
    private void confirmarEliminacion(Movie pelicula) {
        // Armamos el mensaje personalizado usando el string con
        // marcador "%1$s" definido en strings.xml.
        String mensaje = getString(R.string.dialogo_borrar_mensaje, pelicula.getTitulo());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialogo_borrar_titulo)
                .setMessage(mensaje)
                .setIcon(android.R.drawable.ic_dialog_alert)
                // Botón positivo: el usuario CONFIRMA que quiere eliminar.
                .setPositiveButton(R.string.dialogo_borrar_confirmar, (dialog, which) -> {
                    dbHelper.eliminarPelicula(pelicula.getId());
                    Toast.makeText(this, R.string.mensaje_eliminado_ok, Toast.LENGTH_SHORT).show();
                    actualizarVistaLista();
                })
                // Botón negativo: el usuario CANCELA la eliminación.
                .setNegativeButton(R.string.dialogo_borrar_cancelar, (dialog, which) -> dialog.dismiss())
                .show();
    }

    // =================================================================
    // REFRESCAR LA LISTA (leer de SQLite y avisar al Adapter)
    // =================================================================
    private void actualizarVistaLista() {
        List<Movie> peliculas = dbHelper.obtenerTodas();
        adapter.actualizarLista(peliculas);

        // Mostramos el mensaje de "lista vacía" solo si no hay películas.
        boolean listaVacia = peliculas.isEmpty();
        recyclerPeliculas.setVisibility(listaVacia ? View.GONE : View.VISIBLE);
        txtListaVacia.setVisibility(listaVacia ? View.VISIBLE : View.GONE);

        // Actualizamos el subtítulo con el conteo total de películas.
        String textoConteo = peliculas.size() == 1
                ? "1 película guardada"
                : peliculas.size() + " películas guardadas";
        txtSubtitulo.setText(textoConteo);
    }

    // =================================================================
    // CONSUMO DE API REST CON RETROFIT
    // Se simula obtener una "reseña de ejemplo" desde una API pública
    // (JSONPlaceholder) y se muestra en un AlertDialog personalizado.
    // =================================================================
    private void consultarResenaDeEjemplo(Movie pelicula) {
        // Para mantener el ejemplo simple y robusto (sin crear un XML
        // adicional), construimos el diálogo con un TextView creado
        // por código, mostrando primero el estado "cargando".
        TextView textoDialogo = new TextView(this);
        textoDialogo.setPadding(48, 32, 48, 32);
        textoDialogo.setText(R.string.resena_cargando);
        textoDialogo.setTextSize(15);

        AlertDialog dialogoResena = new AlertDialog.Builder(this)
                .setTitle(pelicula.getTitulo())
                .setView(textoDialogo)
                .setPositiveButton(R.string.cerrar, (dialog, which) -> dialog.dismiss())
                .create();
        dialogoResena.show();

        // ---- Llamada real a la API usando Retrofit ----
        // Usamos el "id" de la película (o 1 si es nueva/-1) para pedir
        // un "post" distinto de JSONPlaceholder y así simular reseñas
        // variadas según la película consultada.
        int idParaConsultar = (int) (pelicula.getId() > 0 ? (pelicula.getId() % 100) + 1 : 1);

        Call<PostResponse> llamada = RetrofitClient.getApiService().obtenerResenaEjemplo(idParaConsultar);

        // enqueue() ejecuta la petición HTTP en un hilo secundario
        // (Retrofit lo hace automáticamente) y entrega el resultado en
        // el hilo principal a través de estos callbacks, por lo que es
        // seguro actualizar la interfaz gráfica directamente aquí dentro.
        llamada.enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostResponse resena = response.body();
                    // Armamos un texto de "reseña de ejemplo" combinando
                    // el título y cuerpo devueltos por la API pública.
                    String textoFinal = "\"" + capitalizar(resena.getTitle()) + "\"\n\n"
                            + capitalizar(resena.getBody())
                            + "\n\n(Reseña de ejemplo obtenida vía Retrofit desde una API REST pública)";
                    textoDialogo.setText(textoFinal);
                } else {
                    textoDialogo.setText(R.string.resena_error);
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                // Se ejecuta si no hay internet o la API no respondió.
                textoDialogo.setText(R.string.resena_error);
            }
        });
    }

    // Pequeño método auxiliar para que el texto que llega de la API
    // (en minúsculas) se vea más prolijo, con la primera letra en mayúscula.
    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
