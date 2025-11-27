package com.example.cookit;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import java.util.ArrayList;

public class AddRecipeActivity extends AppCompatActivity {

    EditText editNombre, editTiempo;
    Button btnGuardar, btnAñadirIngrediente, btnAñadirPaso;
    LinearLayout containerIngredientes, containerPasos;
    Spinner spinnerCategorias;

    DataBaseHelper dbHelper;

    ArrayList<String> categoriasList = new ArrayList<>();
    ArrayAdapter<String> adapterCategorias;
    boolean primeraSeleccion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        editNombre = findViewById(R.id.editNombre);
        editTiempo = findViewById(R.id.editTiempo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnAñadirIngrediente = findViewById(R.id.btnAñadirIngrediente);
        btnAñadirPaso = findViewById(R.id.btnAñadirPaso);
        containerIngredientes = findViewById(R.id.containerIngredientes);
        containerPasos = findViewById(R.id.containerPasos);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        dbHelper = new DataBaseHelper(this);
        categoriasList.clear();
        categoriasList.add("Sin categoría");
        categoriasList.addAll(dbHelper.getAllCategorias());
        categoriasList.add("Añadir categoría");

        adapterCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriasList) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapterCategorias);
        spinnerCategorias.setSelection(0);
        spinnerCategorias.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (primeraSeleccion) {
                    primeraSeleccion = false;
                    return;
                }

                if (position == categoriasList.size() - 1) {

                    AlertDialog dialog;
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddRecipeActivity.this);
                    builder.setTitle("Nueva categoría");

                    final EditText input = new EditText(AddRecipeActivity.this);
                    input.setHint("Nombre de la categoría");
                    builder.setView(input);

                    builder.setPositiveButton("Añadir", (d, w) -> {
                        String nuevaCat = input.getText().toString().trim();
                        if (!nuevaCat.isEmpty()) {
                            dbHelper.addCategoria(nuevaCat);
                            categoriasList.add(categoriasList.size() - 1, nuevaCat);
                            adapterCategorias.notifyDataSetChanged();
                            spinnerCategorias.setSelection(categoriasList.indexOf(nuevaCat));
                        } else {
                            spinnerCategorias.setSelection(0);
                        }
                    });

                    builder.setNegativeButton("Cancelar", (d, w) -> {
                        spinnerCategorias.setSelection(0);
                    });

                    dialog = builder.create();
                    dialog.setOnCancelListener(dialogInterface -> spinnerCategorias.setSelection(0));
                    dialog.show();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                spinnerCategorias.setSelection(0);
            }
        });

        btnAñadirIngrediente.setOnClickListener(v -> {
            EditText nuevo = new EditText(this);
            nuevo.setHint("Ingrediente");
            nuevo.setTextSize(16);
            containerIngredientes.addView(nuevo);
        });

        btnAñadirPaso.setOnClickListener(v -> {
            EditText nuevo = new EditText(this);
            nuevo.setHint("Paso " + (containerPasos.getChildCount() + 1));
            nuevo.setTextSize(16);
            containerPasos.addView(nuevo);
        });

        btnGuardar.setOnClickListener(v -> {
            String nombre = editNombre.getText().toString().trim();
            String tiempoStr = editTiempo.getText().toString().trim();

            if (nombre.isEmpty() || tiempoStr.isEmpty()
                    || containerIngredientes.getChildCount() == 0
                    || containerPasos.getChildCount() == 0) {

                Toast.makeText(this,
                        "Rellena todos los campos (puedes dejar 'Sin categoría')",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int tiempo;
            try {
                tiempo = Integer.parseInt(tiempoStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Introduce un tiempo válido", Toast.LENGTH_SHORT).show();
                return;
            }

            String categoriaSeleccionada = categoriasList.get(
                    spinnerCategorias.getSelectedItemPosition()
            );

            StringBuilder ingredientesBuilder = new StringBuilder();
            for (int i = 0; i < containerIngredientes.getChildCount(); i++) {
                String ing = ((EditText) containerIngredientes.getChildAt(i))
                        .getText().toString().trim();
                if (!ing.isEmpty()) {
                    if (ingredientesBuilder.length() > 0) ingredientesBuilder.append("\n");
                    ingredientesBuilder.append(ing);
                }
            }

            StringBuilder pasosBuilder = new StringBuilder();
            for (int i = 0; i < containerPasos.getChildCount(); i++) {
                String paso = ((EditText) containerPasos.getChildAt(i))
                        .getText().toString().trim();
                if (!paso.isEmpty()) {
                    if (pasosBuilder.length() > 0) pasosBuilder.append("\n");
                    pasosBuilder.append((i + 1) + ". " + paso);
                }
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.COLUMN_NOMBRE, nombre);
            values.put(DataBaseHelper.COLUMN_INGREDIENTES, ingredientesBuilder.toString());
            values.put(DataBaseHelper.COLUMN_TIEMPO, tiempo);
            values.put(DataBaseHelper.COLUMN_PASOS, pasosBuilder.toString());
            values.put(DataBaseHelper.COLUMN_CATEGORIA, categoriaSeleccionada);

            long res = db.insert(DataBaseHelper.TABLE_RECETAS, null, values);

            if (res != -1) {
                Toast.makeText(this, "Receta guardada ✅", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al guardar ❌", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
