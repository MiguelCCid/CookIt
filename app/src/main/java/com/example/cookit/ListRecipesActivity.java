package com.example.cookit;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListRecipesActivity extends AppCompatActivity {

    ListView listViewRecetas;
    SearchView searchViewRecetas;
    Spinner spinnerOrden;
    DataBaseHelper dbHelper;

    ArrayList<Receta> listaRecetasOriginal = new ArrayList<>();
    ArrayList<Receta> listaRecetasFiltrada = new ArrayList<>();
    ArrayList<String> recetasVisual = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_recipes);

        listViewRecetas = findViewById(R.id.listViewRecetas);
        searchViewRecetas = findViewById(R.id.searchViewRecetas);
        spinnerOrden = findViewById(R.id.spinnerOrden);
        Button btnAddReceta = findViewById(R.id.btnAddReceta);
        dbHelper = new DataBaseHelper(this);


        btnAddReceta.setOnClickListener(v -> {
            Intent i = new Intent(ListRecipesActivity.this, AddRecipeActivity.class);
            startActivity(i);
        });
        cargarRecetas();
        configurarSpinner();
        configurarBuscador();
        configurarClick();
    }

    private void configurarClick() {
        listViewRecetas.setOnItemClickListener((parent, view, position, id) -> {
            Receta r = listaRecetasFiltrada.get(position);

            Intent intent = new Intent(ListRecipesActivity.this, RecipeDetailActivity.class);
            intent.putExtra("id", r.id);
            intent.putExtra("nombre", r.nombre);
            intent.putExtra("ingredientes", r.ingredientes);
            intent.putExtra("pasos", r.pasos);
            intent.putExtra("tiempo", r.tiempo);
            intent.putExtra("categoria", r.categoria);

            startActivity(intent);
        });
    }

    private void configurarBuscador() {
        searchViewRecetas.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String texto) {
                filtrarLista(texto);
                return true;
            }
        });
    }

    private void configurarSpinner() {
        String[] opciones = {
                "Tiempo (mayor a menor)",
                "Tiempo (menor a mayor)",
                "Nombre (A-Z)",
                "Nombre (Z-A)",
                "Categoría (A-Z)",
                "Categoría (Z-A)"
        };

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrden.setAdapter(adapterSpinner);

        spinnerOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                ordenarLista(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarRecetas();
    }

    private void cargarRecetas() {
        new Thread(() -> {
            ArrayList<Receta> tempOriginal = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT id, nombre, ingredientes, pasos, tiempo, categoria FROM " +
                            DataBaseHelper.TABLE_RECETAS,
                    null
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String nombre = cursor.getString(1);
                    String ingredientes = cursor.getString(2);
                    String pasos = cursor.getString(3);
                    int tiempo = cursor.getInt(4);
                    String categoria = cursor.getString(5);

                    Receta r = new Receta(id, nombre, ingredientes, pasos, tiempo, categoria);
                    tempOriginal.add(r);

                } while (cursor.moveToNext());
            }
            cursor.close();

            runOnUiThread(() -> {
                listaRecetasOriginal.clear();
                listaRecetasOriginal.addAll(tempOriginal);

                listaRecetasFiltrada.clear();
                listaRecetasFiltrada.addAll(listaRecetasOriginal);

                actualizarListaVisual();
            });
        }).start();
    }

    private void actualizarListaVisual() {
        recetasVisual.clear();
        for (Receta r : listaRecetasFiltrada) {
            recetasVisual.add(r.nombre + " - " + r.tiempo + " min - " + r.categoria);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recetasVisual);
        listViewRecetas.setAdapter(adapter);
    }

    private void filtrarLista(String texto) {
        texto = texto.toLowerCase().trim();
        listaRecetasFiltrada.clear();

        if (texto.isEmpty()) {
            listaRecetasFiltrada.addAll(listaRecetasOriginal);
        } else {
            for (Receta r : listaRecetasOriginal) {
                if (r.nombre.toLowerCase().contains(texto) ||
                        r.categoria.toLowerCase().contains(texto)) {
                    listaRecetasFiltrada.add(r);
                }
            }
        }

        ordenarLista(spinnerOrden.getSelectedItemPosition());
    }

    private void ordenarLista(int criterio) {
        ArrayList<Receta> listaTemporal = new ArrayList<>(listaRecetasFiltrada);

        switch (criterio) {
            case 0:
                Collections.sort(listaTemporal, (a, b) -> b.tiempo - a.tiempo);
                break;
            case 1:
                Collections.sort(listaTemporal, (a, b) -> a.tiempo - b.tiempo);
                break;
            case 2:
                Collections.sort(listaTemporal, Comparator.comparing(a -> a.nombre.toLowerCase()));
                break;
            case 3:
                Collections.sort(listaTemporal, (a, b) -> b.nombre.toLowerCase().compareTo(a.nombre.toLowerCase()));
                break;
            case 4:
                listaTemporal.removeIf(r -> r.categoria.equals("Sin categoría"));
                Collections.sort(listaTemporal, Comparator.comparing(a -> a.categoria.toLowerCase()));
                break;
            case 5:
                listaTemporal.removeIf(r -> r.categoria.equals("Sin categoría"));
                Collections.sort(listaTemporal, (a, b) -> b.categoria.toLowerCase().compareTo(a.categoria.toLowerCase()));
                break;
        }

        recetasVisual.clear();
        for (Receta r : listaTemporal) {
            recetasVisual.add(r.nombre + " - " + r.tiempo + " min - " + r.categoria);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recetasVisual);
        listViewRecetas.setAdapter(adapter);
    }

    class Receta {
        int id;
        String nombre;
        String ingredientes;
        String pasos;
        int tiempo;
        String categoria;

        Receta(int id, String nombre, String ingredientes, String pasos, int tiempo, String categoria) {
            this.id = id;
            this.nombre = nombre;
            this.ingredientes = ingredientes;
            this.pasos = pasos;
            this.tiempo = tiempo;
            this.categoria = (categoria != null && !categoria.isEmpty()) ? categoria : "Sin categoría";
        }
    }
}
