package com.example.cookit;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RecipeDetailActivity extends AppCompatActivity {

    TextView tvNombre, tvIngredientes, tvPasos, tvTiempo, tvCategoria;
    Button btnEditar, btnEliminar;
    DataBaseHelper dbHelper;

    int idReceta;
    private static final int REQUEST_EDIT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        tvNombre = findViewById(R.id.tvNombre);
        tvIngredientes = findViewById(R.id.tvIngredientes);
        tvPasos = findViewById(R.id.tvPasos);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvCategoria = findViewById(R.id.tvCategoria);
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminar = findViewById(R.id.btnEliminar);

        dbHelper = new DataBaseHelper(this);

        idReceta = getIntent().getIntExtra("id", -1);
        mostrarReceta();

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(RecipeDetailActivity.this, EditRecipeActivity.class);
            intent.putExtra("id", idReceta);
            intent.putExtra("nombre", tvNombre.getText().toString());
            intent.putExtra("ingredientes", tvIngredientes.getText().toString().replace("Ingredientes:\n", ""));
            intent.putExtra("pasos", tvPasos.getText().toString().replace("Pasos:\n", ""));
            intent.putExtra("tiempo", Integer.parseInt(tvTiempo.getText().toString().replace("Tiempo: ", "").replace(" min", "")));
            intent.putExtra("categoria", tvCategoria.getText().toString().replace("Categoría: ", ""));
            startActivityForResult(intent, REQUEST_EDIT);
        });

        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }

    private void mostrarReceta() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT nombre, ingredientes, pasos, tiempo, categoria FROM " + DataBaseHelper.TABLE_RECETAS + " WHERE id=?",
                new String[]{String.valueOf(idReceta)}
        );

        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(0);
            String ingredientes = cursor.getString(1);
            String pasos = cursor.getString(2);
            int tiempo = cursor.getInt(3);
            String categoria = cursor.getString(4);

            tvNombre.setText(nombre);
            tvIngredientes.setText("Ingredientes:\n" + ingredientes);
            tvPasos.setText("Pasos:\n" + pasos);
            tvTiempo.setText("Tiempo: " + tiempo + " min");
            tvCategoria.setText("Categoría: " + categoria);
        }
        cursor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_EDIT && resultCode == RESULT_OK){
            mostrarReceta();
            Toast.makeText(this, "Receta actualizada con éxito", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar receta")
                .setMessage("¿Seguro que quieres eliminar esta receta?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarReceta())
                .setNegativeButton("No", null)
                .show();
    }

    private void eliminarReceta() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DataBaseHelper.TABLE_RECETAS, "id=?", new String[]{String.valueOf(idReceta)});
        Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show();
        finish();
    }
}
