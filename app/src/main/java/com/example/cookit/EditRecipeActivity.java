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

public class EditRecipeActivity extends AppCompatActivity {

    EditText editNombre, editTiempo;
    Button btnGuardarCambios, btnAñadirIngrediente, btnAñadirPaso;
    LinearLayout containerIngredientes, containerPasos;
    Spinner spinnerCategorias;

    DataBaseHelper dbHelper;
    ArrayList<String> categoriasList = new ArrayList<>();
    ArrayAdapter<String> adapterCategorias;

    int idReceta;
    boolean ignorarPrimera = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        editNombre = findViewById(R.id.editNombre);
        editTiempo = findViewById(R.id.editTiempo);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        containerIngredientes = findViewById(R.id.containerIngredientes);
        containerPasos = findViewById(R.id.containerPasos);
        btnAñadirIngrediente = findViewById(R.id.btnAñadirIngrediente);
        btnAñadirPaso = findViewById(R.id.btnAñadirPaso);
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
                if (position == 0) tv.setTextColor(Color.GRAY);
                else tv.setTextColor(Color.BLACK);
                return view;
            }
        };
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapterCategorias);

        idReceta = getIntent().getIntExtra("id", -1);
        String nombre = getIntent().getStringExtra("nombre");
        String ingredientes = getIntent().getStringExtra("ingredientes");
        String pasos = getIntent().getStringExtra("pasos");
        int tiempo = getIntent().getIntExtra("tiempo", 0);
        String categoria = getIntent().getStringExtra("categoria");

        editNombre.setText(nombre);
        editTiempo.setText(String.valueOf(tiempo));

        String[] listaIng = ingredientes.split("\n");
        for (String ing : listaIng) {
            if (!ing.trim().isEmpty()) {
                EditText et = new EditText(this);
                et.setText(ing);
                et.setHint("Ingrediente");
                containerIngredientes.addView(et);
            }
        }

        String[] listaPasos = pasos.split("\n");
        for (String paso : listaPasos) {
            if (!paso.trim().isEmpty()) {
                EditText et = new EditText(this);
                et.setText(paso.replaceAll("^\\d+\\.\\s", ""));
                et.setHint("Paso");
                containerPasos.addView(et);
            }
        }

        int catIndex = categoriasList.indexOf(categoria);
        spinnerCategorias.setSelection(catIndex >= 0 ? catIndex : 0);

        spinnerCategorias.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {

                if (ignorarPrimera) {
                    ignorarPrimera = false;
                    return;
                }

                if (position == categoriasList.size() - 1) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(EditRecipeActivity.this);
                    builder.setTitle("Nueva categoría");

                    final EditText input = new EditText(EditRecipeActivity.this);
                    input.setHint("Nombre de la categoría");
                    builder.setView(input);

                    builder.setPositiveButton("Añadir", (dialog, which) -> {
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

                    builder.setNegativeButton("Cancelar", (dialog, which) -> {
                        spinnerCategorias.setSelection(0);
                    });

                    builder.setOnCancelListener(dialog ->
                            spinnerCategorias.setSelection(0)
                    );

                    builder.show();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnAñadirIngrediente.setOnClickListener(v -> {
            EditText et = new EditText(this);
            et.setHint("Ingrediente");
            containerIngredientes.addView(et);
        });

        btnAñadirPaso.setOnClickListener(v -> {
            EditText et = new EditText(this);
            et.setHint("Paso " + (containerPasos.getChildCount() + 1));
            containerPasos.addView(et);
        });

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nombre = editNombre.getText().toString().trim();
        String tiempoStr = editTiempo.getText().toString().trim();
        String categoriaSeleccionada = spinnerCategorias.getSelectedItem().toString();

        if (nombre.isEmpty() || tiempoStr.isEmpty()
                || containerIngredientes.getChildCount() == 0
                || containerPasos.getChildCount() == 0) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int tiempo = Integer.parseInt(tiempoStr);

        StringBuilder ingredientesBuilder = new StringBuilder();
        for (int i = 0; i < containerIngredientes.getChildCount(); i++) {
            EditText et = (EditText) containerIngredientes.getChildAt(i);
            String ing = et.getText().toString().trim();
            if (!ing.isEmpty()) {
                if (ingredientesBuilder.length() > 0) ingredientesBuilder.append("\n");
                ingredientesBuilder.append(ing);
            }
        }

        StringBuilder pasosBuilder = new StringBuilder();
        for (int i = 0; i < containerPasos.getChildCount(); i++) {
            EditText et = (EditText) containerPasos.getChildAt(i);
            String paso = et.getText().toString().trim();
            if (!paso.isEmpty()) {
                if (pasosBuilder.length() > 0) pasosBuilder.append("\n");
                pasosBuilder.append((i + 1) + ". " + paso);
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.COLUMN_NOMBRE, nombre);
        values.put(DataBaseHelper.COLUMN_INGREDIENTES, ingredientesBuilder.toString());
        values.put(DataBaseHelper.COLUMN_PASOS, pasosBuilder.toString());
        values.put(DataBaseHelper.COLUMN_TIEMPO, tiempo);
        values.put(DataBaseHelper.COLUMN_CATEGORIA, categoriaSeleccionada);

        db.update(DataBaseHelper.TABLE_RECETAS, values, "id=?", new String[]{String.valueOf(idReceta)});

        Toast.makeText(this, "Receta actualizada correctamente ✅", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
