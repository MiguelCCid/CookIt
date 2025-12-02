    package com.example.cookit;

    import android.content.ContentValues;
    import android.content.Context;
    import android.database.Cursor;
    import android.database.sqlite.SQLiteDatabase;
    import android.database.sqlite.SQLiteOpenHelper;

    import java.util.ArrayList;

    public class DataBaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "cookit.db";
        private static final int DATABASE_VERSION = 2;

        public static final String TABLE_RECETAS = "recetas";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_INGREDIENTES = "ingredientes";
        public static final String COLUMN_TIEMPO = "tiempo";
        public static final String COLUMN_PASOS = "pasos";

        public static final String COLUMN_CATEGORIA = "categoria";
        public static final String COLUMN_IMAGEN = "imagen";


        public static final String TABLE_CATEGORIAS = "categorias";
        public static final String COLUMN_CAT_ID = "id";
        public static final String COLUMN_CAT_NOMBRE = "nombre";


        public DataBaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTableRecetas = "CREATE TABLE " + TABLE_RECETAS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NOMBRE + " TEXT, " +
                    COLUMN_INGREDIENTES + " TEXT, " +
                    COLUMN_TIEMPO + " INTEGER, " +
                    COLUMN_PASOS + " TEXT, " +
                    COLUMN_CATEGORIA + " TEXT, " +
                    COLUMN_IMAGEN + " TEXT)";

            String createTableCategorias = "CREATE TABLE " + TABLE_CATEGORIAS + " (" +
                    COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CAT_NOMBRE + " TEXT)";

            db.execSQL(createTableRecetas);
            db.execSQL(createTableCategorias);

        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECETAS);
            onCreate(db);
        }

        public void addCategoria(String nombre){
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAT_NOMBRE, nombre);
            db.insertWithOnConflict(TABLE_CATEGORIAS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public ArrayList<String> getAllCategorias(){
            ArrayList<String> list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_CAT_NOMBRE + " FROM " + TABLE_CATEGORIAS, null);
            if(cursor.moveToFirst()){
                do{
                    list.add(cursor.getString(0));
                }while(cursor.moveToNext());
            }
            cursor.close();
            return list;
        }
    }
