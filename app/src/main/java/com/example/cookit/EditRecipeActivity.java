package com.example.cookit;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.ImageButton;
import android.view.Gravity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class EditRecipeActivity extends AppCompatActivity {

    EditText editNombre, editTiempo;
    Button btnGuardarCambios, btnAñadirIngrediente, btnAñadirPaso;
    LinearLayout containerIngredientes, containerPasos;
    Spinner spinnerCategorias;
    ImageView imgReceta;
    TextView txtAddImage;

    DataBaseHelper dbHelper;
    ArrayList<String> categoriasList = new ArrayList<>();
    ArrayAdapter<String> adapterCategorias;

    int idReceta;
    boolean ignorarPrimera = true;

    Uri imagenUri = null;

    ActivityResultLauncher<String> seleccionarImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if(uri != null){
                    imagenUri = uri;
                    imgReceta.setImageURI(uri);
                    txtAddImage.setVisibility(View.GONE);
                }
            }
    );

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
        imgReceta = findViewById(R.id.imgReceta);
        txtAddImage = findViewById(R.id.txtAddImage);

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

        idReceta = getIntent().getIntExtra("id", -1);
        String nombre = getIntent().getStringExtra("nombre");
        String ingredientes = getIntent().getStringExtra("ingredientes");
        String pasos = getIntent().getStringExtra("pasos");
        int tiempo = getIntent().getIntExtra("tiempo", 0);
        String categoria = getIntent().getStringExtra("categoria");

        editNombre.setText(nombre);
        editTiempo.setText(String.valueOf(tiempo));

        for(String ing : ingredientes.split("\n")){
            if(!ing.trim().isEmpty()){
                addIngrediente(ing);
            }
        }

        for(String paso : pasos.split("\n")){
            if(!paso.trim().isEmpty()){
                addPaso(paso.replaceAll("^\\d+\\.\\s",""));
            }
        }

        int catIndex = categoriasList.indexOf(categoria);
        spinnerCategorias.setSelection(catIndex >= 0 ? catIndex : 0);

        spinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(ignorarPrimera){
                    ignorarPrimera = false;
                    return;
                }
                if(position == categoriasList.size()-1){
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditRecipeActivity.this);
                    builder.setTitle("Nueva categoría");
                    final EditText input = new EditText(EditRecipeActivity.this);
                    input.setHint("Nombre de la categoría");
                    builder.setView(input);
                    builder.setPositiveButton("Añadir", (dialog, which) -> {
                        String nuevaCat = input.getText().toString().trim();
                        if(!nuevaCat.isEmpty()){
                            dbHelper.addCategoria(nuevaCat);
                            categoriasList.add(categoriasList.size()-1,nuevaCat);
                            adapterCategorias.notifyDataSetChanged();
                            spinnerCategorias.setSelection(categoriasList.indexOf(nuevaCat));
                        }else spinnerCategorias.setSelection(0);
                    });
                    builder.setNegativeButton("Cancelar", (dialog, which) -> spinnerCategorias.setSelection(0));
                    builder.setOnCancelListener(dialog -> spinnerCategorias.setSelection(0));
                    builder.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAñadirIngrediente.setOnClickListener(v -> addIngrediente(""));
        btnAñadirPaso.setOnClickListener(v -> addPaso(""));

        imgReceta.setOnClickListener(v -> seleccionarImagen.launch("image/*"));
        txtAddImage.setOnClickListener(v -> seleccionarImagen.launch("image/*"));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT imagen FROM "+DataBaseHelper.TABLE_RECETAS+" WHERE id=?",
                new String[]{String.valueOf(idReceta)});
        if(cursor.moveToFirst()){
            String path = cursor.getString(0);
            if(path != null && !path.isEmpty()){
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if(bmp != null) imgReceta.setImageBitmap(bmp);
            }
        }
        cursor.close();

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
    }

    private void addIngrediente(String texto){
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fila.setGravity(Gravity.CENTER_VERTICAL);

        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        et.setHint("Ingrediente");
        et.setText(texto);
        fila.addView(et);

        ImageButton btnEliminar = new ImageButton(this);
        btnEliminar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnEliminar.setBackgroundColor(Color.TRANSPARENT);
        btnEliminar.setOnClickListener(v -> containerIngredientes.removeView(fila));
        fila.addView(btnEliminar);

        containerIngredientes.addView(fila);
    }

    private void addPaso(String texto){
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fila.setGravity(Gravity.CENTER_VERTICAL);

        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        et.setHint("Paso " + (containerPasos.getChildCount()+1));
        et.setText(texto);
        fila.addView(et);

        ImageButton btnEliminar = new ImageButton(this);
        btnEliminar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnEliminar.setBackgroundColor(Color.TRANSPARENT);
        btnEliminar.setOnClickListener(v -> containerPasos.removeView(fila));
        fila.addView(btnEliminar);

        containerPasos.addView(fila);
    }

    private void guardarCambios(){
        String nombre = editNombre.getText().toString().trim();
        String tiempoStr = editTiempo.getText().toString().trim();
        String categoriaSeleccionada = spinnerCategorias.getSelectedItem().toString();

        if(nombre.isEmpty() || tiempoStr.isEmpty() || containerIngredientes.getChildCount()==0 || containerPasos.getChildCount()==0){
            Toast.makeText(this,"Rellena todos los campos",Toast.LENGTH_SHORT).show();
            return;
        }

        int tiempo = Integer.parseInt(tiempoStr);

        StringBuilder ingredientesBuilder = new StringBuilder();
        for(int i=0;i<containerIngredientes.getChildCount();i++){
            EditText et = (EditText)((LinearLayout)containerIngredientes.getChildAt(i)).getChildAt(0);
            String ing = et.getText().toString().trim();
            if(!ing.isEmpty()){
                if(ingredientesBuilder.length()>0) ingredientesBuilder.append("\n");
                ingredientesBuilder.append(ing);
            }
        }

        StringBuilder pasosBuilder = new StringBuilder();
        for(int i=0;i<containerPasos.getChildCount();i++){
            EditText et = (EditText)((LinearLayout)containerPasos.getChildAt(i)).getChildAt(0);
            String paso = et.getText().toString().trim();
            if(!paso.isEmpty()){
                if(pasosBuilder.length()>0) pasosBuilder.append("\n");
                pasosBuilder.append((i+1)+". "+paso);
            }
        }

        String imagenPath = null;
        if(imagenUri != null){
            imagenPath = guardarImagenInterna(imagenUri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.COLUMN_NOMBRE,nombre);
        values.put(DataBaseHelper.COLUMN_INGREDIENTES,ingredientesBuilder.toString());
        values.put(DataBaseHelper.COLUMN_PASOS,pasosBuilder.toString());
        values.put(DataBaseHelper.COLUMN_TIEMPO,tiempo);
        values.put(DataBaseHelper.COLUMN_CATEGORIA,categoriaSeleccionada);
        if(imagenPath != null) values.put(DataBaseHelper.COLUMN_IMAGEN,imagenPath);

        db.update(DataBaseHelper.TABLE_RECETAS,values,"id=?",new String[]{String.valueOf(idReceta)});

        Toast.makeText(this,"Receta actualizada",Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private String guardarImagenInterna(Uri uri){
        try{
            InputStream input = getContentResolver().openInputStream(uri);
            String nombreArchivo = "receta_"+System.currentTimeMillis()+".jpg";
            File file = new File(getFilesDir(),nombreArchivo);
            FileOutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while((length = input.read(buffer))>0){
                output.write(buffer,0,length);
            }
            output.close();
            input.close();
            return file.getAbsolutePath();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
