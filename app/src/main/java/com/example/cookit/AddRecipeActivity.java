package com.example.cookit;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class AddRecipeActivity extends AppCompatActivity {

    EditText editNombre, editTiempo;
    Button btnGuardar, btnAñadirIngrediente, btnAñadirPaso;
    LinearLayout containerIngredientes, containerPasos;
    Spinner spinnerCategorias;

    ImageView imgReceta;
    TextView txtAddImage;

    Uri imagenUri = null;
    DataBaseHelper dbHelper;

    ArrayList<String> categoriasList = new ArrayList<>();
    ArrayAdapter<String> adapterCategorias;
    boolean primeraSeleccion = true;

    ActivityResultLauncher<String> seleccionarImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imagenUri = uri;
                    imgReceta.setImageURI(uri);
                    txtAddImage.setVisibility(View.GONE);
                }
            }
    );

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
        imgReceta = findViewById(R.id.imgReceta);
        txtAddImage = findViewById(R.id.txtAddImage);

        dbHelper = new DataBaseHelper(this);

        imgReceta.setOnClickListener(v -> seleccionarImagen.launch("image/*"));
        txtAddImage.setOnClickListener(v -> seleccionarImagen.launch("image/*"));

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

        spinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (primeraSeleccion) {
                    primeraSeleccion = false;
                    return;
                }
                if (position == categoriasList.size() - 1) {
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
                        } else spinnerCategorias.setSelection(0);
                    });
                    builder.setNegativeButton("Cancelar", (d, w) -> spinnerCategorias.setSelection(0));
                    AlertDialog dialog = builder.create();
                    dialog.setOnCancelListener(dialogInterface -> spinnerCategorias.setSelection(0));
                    dialog.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { spinnerCategorias.setSelection(0); }
        });

        btnAñadirIngrediente.setOnClickListener(v -> añadirFila(containerIngredientes, "Ingrediente"));
        btnAñadirPaso.setOnClickListener(v -> añadirFila(containerPasos, "Paso " + (containerPasos.getChildCount() + 1)));
        btnGuardar.setOnClickListener(v -> guardarReceta());
    }

    private void añadirFila(LinearLayout contenedor, String hint) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        fila.setPadding(0, 6, 0, 6);

        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(16);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton btnEliminar = new ImageButton(this);
        btnEliminar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnEliminar.setBackgroundColor(Color.TRANSPARENT);
        btnEliminar.setOnClickListener(v -> contenedor.removeView(fila));

        fila.addView(et);
        fila.addView(btnEliminar);

        contenedor.addView(fila);
    }

    private void guardarReceta() {
        String nombre = editNombre.getText().toString().trim();
        String tiempoStr = editTiempo.getText().toString().trim();

        if (nombre.isEmpty() || tiempoStr.isEmpty() ||
                containerIngredientes.getChildCount() == 0 ||
                containerPasos.getChildCount() == 0) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int tiempo;
        try { tiempo = Integer.parseInt(tiempoStr); }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Introduce un tiempo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder ingredientesBuilder = new StringBuilder();
        for (int i = 0; i < containerIngredientes.getChildCount(); i++) {
            LinearLayout fila = (LinearLayout) containerIngredientes.getChildAt(i);
            EditText et = (EditText) fila.getChildAt(0);
            String ing = et.getText().toString().trim();
            if (!ing.isEmpty()) {
                if (ingredientesBuilder.length() > 0) ingredientesBuilder.append("\n");
                ingredientesBuilder.append(ing);
            }
        }

        StringBuilder pasosBuilder = new StringBuilder();
        for (int i = 0; i < containerPasos.getChildCount(); i++) {
            LinearLayout fila = (LinearLayout) containerPasos.getChildAt(i);
            EditText et = (EditText) fila.getChildAt(0);
            String paso = et.getText().toString().trim();
            if (!paso.isEmpty()) {
                if (pasosBuilder.length() > 0) pasosBuilder.append("\n");
                pasosBuilder.append((i + 1) + ". " + paso);
            }
        }

        String imagenPath = null;
        if (imagenUri != null) {
            imagenPath = guardarImagenInterna(imagenUri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.COLUMN_NOMBRE, nombre);
        values.put(DataBaseHelper.COLUMN_INGREDIENTES, ingredientesBuilder.toString());
        values.put(DataBaseHelper.COLUMN_TIEMPO, tiempo);
        values.put(DataBaseHelper.COLUMN_PASOS, pasosBuilder.toString());
        values.put(DataBaseHelper.COLUMN_CATEGORIA, categoriasList.get(spinnerCategorias.getSelectedItemPosition()));
        values.put(DataBaseHelper.COLUMN_IMAGEN, imagenPath);

        db.insert(DataBaseHelper.TABLE_RECETAS, null, values);
        finish();
    }

    private String guardarImagenInterna(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            String nombreArchivo = "receta_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), nombreArchivo);
            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.close();
            input.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
