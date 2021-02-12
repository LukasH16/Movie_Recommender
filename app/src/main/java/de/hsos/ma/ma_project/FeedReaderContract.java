package de.hsos.ma.ma_project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class FeedReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FeedReaderContract() {
    }

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "MovieData";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DOC2VEC = "doc2vec";
        public static final String COLUMN_NAME_GENRE = "genre";
        public static final String COLUMN_NAME_ACTOR = "actor";
        public static final String COLUMN_NAME_RELEASE_DATE = "releaseDate";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_RATING = "rating";
        public static final String COLUMN_NAME_PLOT = "plot";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                        FeedEntry._ID + " INTEGER PRIMARY KEY," +
                        FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                        FeedEntry.COLUMN_NAME_DOC2VEC + " TEXT," +
                        FeedEntry.COLUMN_NAME_GENRE + " TEXT," +
                        FeedEntry.COLUMN_NAME_ACTOR + " TEXT," +
                        FeedEntry.COLUMN_NAME_RELEASE_DATE + " INTEGER," +
                        FeedEntry.COLUMN_NAME_IMAGE + " TEXT," +
                        FeedEntry.COLUMN_NAME_RATING + " RAW," +
                        FeedEntry.COLUMN_NAME_PLOT + " TEXT)";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;

    }

    public static class FeedReaderDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "Movie.db";

        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(FeedEntry.SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(FeedEntry.SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

        public long fillDB(FeedReaderDbHelper dbHelper, ArrayList<MainActivity.MovieData> movies){
            long newRowId = 0;
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Gets the data repository in write mode
            // Create a new map of values, where column names are the keys
            for(int i = 0; i<movies.size(); i++){
                MainActivity.MovieData movie = movies.get(i);
                ContentValues values = new ContentValues();
                values.put(FeedEntry.COLUMN_NAME_TITLE, movie.getTitle());
                values.put(FeedEntry.COLUMN_NAME_RATING, 0.0f);
                values.put(FeedEntry.COLUMN_NAME_IMAGE, movie.getImage());
                values.put(FeedEntry.COLUMN_NAME_RELEASE_DATE, movie.getReleaseDate());
                values.put(FeedEntry.COLUMN_NAME_ACTOR, movie.getActor());
                values.put(FeedEntry.COLUMN_NAME_GENRE, movie.getGenre());
                values.put(FeedEntry.COLUMN_NAME_PLOT, movie.getPlot());

                // Insert the new row, returning the primary key value of the new row
                newRowId = db.insert(FeedEntry.TABLE_NAME, null, values);
            }
            return newRowId;
        }

        public ArrayList<MainActivity.MovieData> ReadFromDB(FeedReaderContract.FeedReaderDbHelper dbHelper){
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    BaseColumns._ID,
                    FeedEntry.COLUMN_NAME_TITLE,
                    FeedEntry.COLUMN_NAME_GENRE,
                    FeedEntry.COLUMN_NAME_ACTOR,
                    FeedEntry.COLUMN_NAME_RELEASE_DATE,
                    FeedEntry.COLUMN_NAME_IMAGE,
                    FeedEntry.COLUMN_NAME_RATING,
                    FeedEntry.COLUMN_NAME_PLOT
            };

            Cursor cursor = db.rawQuery("select * from MovieData Order By RANDOM() LIMIT 1", null);

            ArrayList<MainActivity.MovieData> movies = new ArrayList<>();
            while(cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE));
                String genre = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_GENRE));
                String actor = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_ACTOR));
                int release_date = cursor.getInt(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_RELEASE_DATE));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_IMAGE));
                float rating = cursor.getFloat(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_RATING));
                String plot = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_PLOT));

                movies.add(new MainActivity.MovieData(title, plot, release_date, image, actor, genre, rating));
            }
            cursor.close();

            return movies;
        }

        public int UpdateDB(FeedReaderContract.FeedReaderDbHelper dbHelper, String title, float rating){
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // New value for one column
            ContentValues values = new ContentValues();
            values.put(FeedEntry.COLUMN_NAME_RATING, rating);

            // Which row to update, based on the title
            String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
            String[] selectionArgs = { title };

            int count = db.update(
                    FeedReaderContract.FeedEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            return count;
        }

        public MainActivity.MovieData getMovieFromTitle(FeedReaderContract.FeedReaderDbHelper dbHelper, String title){
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    BaseColumns._ID,
                    FeedEntry.COLUMN_NAME_TITLE,
                    FeedEntry.COLUMN_NAME_GENRE,
                    FeedEntry.COLUMN_NAME_ACTOR,
                    FeedEntry.COLUMN_NAME_RELEASE_DATE,
                    FeedEntry.COLUMN_NAME_IMAGE,
                    FeedEntry.COLUMN_NAME_RATING,
                    FeedEntry.COLUMN_NAME_PLOT
            };

            Cursor cursor = db.rawQuery("select * from MovieData Where title = '" + title + "'", null);

            MainActivity.MovieData movie = new MainActivity.MovieData(title, "plot", 0, "https://img.icons8.com/officel/80/000000/movie.png", "actor", "genre", 0);;

            while(cursor.moveToNext()) {
                String newTitle = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE));
                String genre = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_GENRE));
                String actor = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_ACTOR));
                int release_date = cursor.getInt(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_RELEASE_DATE));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_IMAGE));
                float rating = cursor.getFloat(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_RATING));
                String plot = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_PLOT));

                movie = new MainActivity.MovieData(title, plot, release_date, image, actor, genre, rating);
            }
            cursor.close();

            return movie;
        }

    }

}

