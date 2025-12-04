package com.example.cookit;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.Comparator;

public class ListRecipesActivity extends AppCompatActivity {

    ListView listViewRecetas;
    SearchView searchViewRecetas;
    Spinner spinnerOrden;
    DataBaseHelper dbHelper;

    ArrayList<Receta> listaRecetasOriginal = new ArrayList<>();
    ArrayList<Receta> listaRecetasFiltrada = new ArrayList<>();
    RecetaAdapter adapter;

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
            startActivity(new Intent(ListRecipesActivity.this, AddRecipeActivity.class));
        });

        adapter = new RecetaAdapter();
        listViewRecetas.setAdapter(adapter);

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
            intent.putExtra("imagen", r.imagenPath);
            startActivity(intent);
        });
    }

    private void configurarBuscador() {
        searchViewRecetas.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String texto) {
                filtrarLista(texto);
                return true;
            }
        });
    }

    private void configurarSpinner() {
        String[] opciones = {"Tiempo (mayor a menor)","Tiempo (menor a mayor)","Nombre (A-Z)","Nombre (Z-A)","Categoría (A-Z)","Categoría (Z-A)"};
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrden.setAdapter(adapterSpinner);

        spinnerOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { ordenarLista(position); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
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
            Cursor cursor = db.rawQuery("SELECT id, nombre, ingredientes, pasos, tiempo, categoria, imagen FROM " +
                    DataBaseHelper.TABLE_RECETAS, null);

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String nombre = cursor.getString(1);
                    String ingredientes = cursor.getString(2);
                    String pasos = cursor.getString(3);
                    int tiempo = cursor.getInt(4);
                    String categoria = cursor.getString(5);
                    String imagen = cursor.getString(6);

                    tempOriginal.add(new Receta(id, nombre, ingredientes, pasos, tiempo, categoria, imagen));
                } while (cursor.moveToNext());
            }
            cursor.close();

            runOnUiThread(() -> {
                listaRecetasOriginal.clear();
                listaRecetasOriginal.addAll(tempOriginal);
                listaRecetasFiltrada.clear();
                listaRecetasFiltrada.addAll(listaRecetasOriginal);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void filtrarLista(String texto) {
        texto = texto.toLowerCase().trim();
        listaRecetasFiltrada.clear();
        if (texto.isEmpty()) listaRecetasFiltrada.addAll(listaRecetasOriginal);
        else {
            for (Receta r : listaRecetasOriginal) {
                if (r.nombre.toLowerCase().contains(texto) || r.categoria.toLowerCase().contains(texto))
                    listaRecetasFiltrada.add(r);
            }
        }
        ordenarLista(spinnerOrden.getSelectedItemPosition());
    }

    private void ordenarLista(int criterio) {
        switch (criterio) {
            case 0:
                listaRecetasFiltrada.sort((a, b) -> b.tiempo - a.tiempo);
                break;
            case 1:
                listaRecetasFiltrada.sort((a, b) -> a.tiempo - b.tiempo);
                break;
            case 2:
                listaRecetasFiltrada.sort(Comparator.comparing(a -> a.nombre.toLowerCase()));
                break;
            case 3:
                listaRecetasFiltrada.sort((a, b) -> b.nombre.toLowerCase().compareTo(a.nombre.toLowerCase()));
                break;
            case 4:
                listaRecetasFiltrada.sort((a, b) -> {
                    if (a.categoria.equalsIgnoreCase("Sin categoría") && !b.categoria.equalsIgnoreCase("Sin categoría"))
                        return 1;
                    if (!a.categoria.equalsIgnoreCase("Sin categoría") && b.categoria.equalsIgnoreCase("Sin categoría"))
                        return -1;
                    return a.categoria.toLowerCase().compareTo(b.categoria.toLowerCase());
                });
                break;

            case 5:
                listaRecetasFiltrada.sort((a, b) -> {
                    if (a.categoria.equalsIgnoreCase("Sin categoría") && !b.categoria.equalsIgnoreCase("Sin categoría"))
                        return 1;
                    if (!a.categoria.equalsIgnoreCase("Sin categoría") && b.categoria.equalsIgnoreCase("Sin categoría"))
                        return -1;
                    return b.categoria.toLowerCase().compareTo(a.categoria.toLowerCase());
                });
                break;

        }
        adapter.notifyDataSetChanged();
    }



    class RecetaAdapter extends BaseAdapter {
        @Override public int getCount() { return listaRecetasFiltrada.size(); }
        @Override public Object getItem(int position) { return listaRecetasFiltrada.get(position); }
        @Override public long getItemId(int position) { return listaRecetasFiltrada.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(ListRecipesActivity.this)
                    .inflate(R.layout.item_receta, parent, false);

            ImageView imgRecipe = convertView.findViewById(R.id.imgRecipe);
            TextView tvNombre = convertView.findViewById(R.id.tvItemNombre);
            TextView tvCategoria = convertView.findViewById(R.id.tvItemCategoria);
            TextView tvTiempo = convertView.findViewById(R.id.tvItemTiempo);

            Receta r = listaRecetasFiltrada.get(position);
            tvNombre.setText(r.nombre);
            tvCategoria.setText(r.categoria);
            tvTiempo.setText(r.tiempo + " min");

            if (r.imagenPath != null && !r.imagenPath.isEmpty()) {
                Bitmap bmp = BitmapFactory.decodeFile(r.imagenPath);
                if (bmp != null) imgRecipe.setImageBitmap(bmp);
                else imgRecipe.setImageResource(R.drawable.placeholder_receta);
            } else imgRecipe.setImageResource(R.drawable.placeholder_receta);

            return convertView;
        }
    }

    class Receta {
        int id;
        String nombre, ingredientes, pasos, categoria, imagenPath;
        int tiempo;

        Receta(int id, String nombre, String ingredientes, String pasos, int tiempo, String categoria, String imagenPath) {
            this.id = id;
            this.nombre = nombre;
            this.ingredientes = ingredientes;
            this.pasos = pasos;
            this.tiempo = tiempo;
            this.categoria = (categoria != null && !categoria.isEmpty()) ? categoria : "Sin categoría";
            this.imagenPath = imagenPath;
        }
    }
}
