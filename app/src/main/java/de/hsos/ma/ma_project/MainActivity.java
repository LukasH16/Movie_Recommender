package de.hsos.ma.ma_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.bytedeco.tesseract.INT_FEATURE_STRUCT;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private TextView ratingTitle;
    private TextView ratingGenre;
    private TextView ratingActor;
    private TextView ratingReleaseDate;
    private TextView ratingPlot;
    private ImageView ratingImage;
    private Button btnSubmit;
    private TextView ratingID;
    private RatingBar ratingBar;
    private LinearLayout movieRateLayout;
    private ListView listView;
    private ProgressBar bar;
    boolean online = false;
    private FeedReaderContract.FeedReaderDbHelper dbHelper;
    private SQLiteDatabase mDb;
    private static RequestQueue queue;
    private ArrayList<MovieData> Movies;
    private MovieData[] RecommendedMovies = new MovieData[10];
    private ArrayList<String> Actors;
    private ArrayList<String> Genres;
    private int maxBatches = 5;
    public static Context context;

    //https://stackoverflow.com/questions/39058638/android-volley-noconnectionerror
    //192.168.188.35
    //192.168.178.32
    private static String url ="http://192.168.178.30:8000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movieRateLayout = findViewById(R.id.movieRateLayout);

        for(int i = 1; i < RecommendedMovies.length; i++){
            RecommendedMovies[i] = new MovieData("title", "plot",
                    2000, "//img.icons8.com/officel/80/000000/movie.png",
                    "actor", "genre", 0.0, 1);
        }

        MovieListAdapter whatever = new MovieListAdapter(this, RecommendedMovies);
        listView = (ListView) findViewById(R.id.recommendationList);
        listView.setAdapter(whatever);

        Log.i("Listview", "Hier zum ersten mal gesetzt");

        bar = (ProgressBar) findViewById(R.id.progressBar);
        ratingTitle = (TextView) findViewById(R.id.movie_titel);
        ratingActor = (TextView) findViewById(R.id.movie_actor);
        ratingGenre = (TextView) findViewById(R.id.movie_genre);
        ratingID = (TextView) findViewById(R.id.movie_id);
        ratingImage = (ImageView) findViewById(R.id.movie_image);
        ratingPlot = (TextView) findViewById(R.id.movie_plot);
        ratingReleaseDate = (TextView) findViewById(R.id.movie_release_date);

        queue = Volley.newRequestQueue(this);
        Movies = new ArrayList<>();
        Actors = new ArrayList<>();
        Genres = new ArrayList<>();

        dbHelper = new FeedReaderContract.FeedReaderDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 0, 1);
        VolleyGetDB();
        VolleyGetActors();
        VolleyGetGenres();

        addListenerOnRatingButton();

        MainActivity.context = getApplicationContext();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    public void fillMovieLayout(MovieData movie){
        ratingTitle.setText(movie.title);
        ratingID.setText(String.valueOf(movie.id));
        ratingPlot.setText(movie.plot);
        ratingActor.setText(movie.actor);
        ratingGenre.setText(movie.genre);
        ratingReleaseDate.setText(String.valueOf(movie.releaseDate));
        new DownloadImageTask(ratingImage).execute("https:" + movie.image);
    }

    public void addListenerOnRatingButton() {
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //if click on me, then display the current rating value.
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bar.setVisibility(View.VISIBLE);
                float rating = ((ratingBar.getRating() - 1) / 4);
                Log.i("Rating: ", String.valueOf((ratingBar.getRating() - 1) / 4));
                //Daten in DB Updaten
                int changedLines = dbHelper.UpdateDB(dbHelper, ratingTitle.getText().toString(), rating);
                Log.i("Update", String.valueOf(changedLines));
                //Daten per Post an Server
                VolleyPostUpdate(ratingTitle.getText().toString(), Integer.valueOf(ratingReleaseDate.getText().toString()), "Action", rating);

                if(!Movies.isEmpty()){
                    //Neuen Film laden
                    fillMovieLayout(Movies.get(0));
                    Movies.remove(0);
                    bar.setVisibility(View.GONE);
                }else{
                    movieRateLayout.setVisibility(View.GONE);
                    AsyncTaskRunner runner = new AsyncTaskRunner();

                    final double pl = 1;
                    final double pw = 1;
                    final double sl = 1;
                    final double sw = 1;

                    //pass the measurement as params to the AsyncTask
                    runner.execute(pl,pw,sl,sw);

                    //listView.setVisibility(View.VISIBLE);
                    bar.setVisibility(View.VISIBLE);
                }
            }

        });

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.retry_favorites) {
            Movies = dbHelper.ReadFromDB(dbHelper);
            Log.i("Layout", String.valueOf(Movies.size()));
            fillMovieLayout(Movies.get(0));
            Movies.remove(0);
            movieRateLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }

        return super.onOptionsItemSelected(item);
    }

    private class AsyncTaskRunner extends AsyncTask<Double, Integer, INDArray> {

        int[] movieIds;
        String[] titlesSorted;
        ArrayList<MovieData> moviesSorted;

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            bar.setVisibility(View.INVISIBLE);
        }

        public double[] getOneHot(String genre, int release_date, String doc_vec){
            double[] oneHot = new double[20 + Genres.size() + 1];
            for (int i = 0, len = oneHot.length; i < len; i++) oneHot[i] = 0;

            StringTokenizer genreToken  = new StringTokenizer(genre, ",");
            doc_vec = doc_vec.substring(1, doc_vec.length() - 1);
            StringTokenizer doc_vecToken  = new StringTokenizer(doc_vec, ",");
            int i = 0;
            while (doc_vecToken.hasMoreTokens()) {
                oneHot[i] = Double.parseDouble(doc_vecToken.nextToken());
                i++;
            }

            while (genreToken.hasMoreTokens()) {
                oneHot[20 + Genres.indexOf(genreToken.nextToken())] = 1;
            }
            oneHot[oneHot.length - 1] = ((double)release_date  - 1900) / 150;
            return oneHot;
        }

        // This is our main background thread for the neural net
        @Override
        protected INDArray doInBackground(Double... params) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long count = DatabaseUtils.queryNumEntries(db, FeedReaderContract.FeedEntry.TABLE_NAME);
            db.close();

            double[][] training_in = new double[(int) count][20 + Genres.size() + 1];
            double[] training_out = new double[(int) count];
            movieIds = new int[(int) count];
            titlesSorted = new String[(int) count];
            moviesSorted = new ArrayList<>();
            //parameter = dbHelper.GetNNPrams(dbHelper, parameter);
            db = dbHelper.getReadableDatabase();

            String[] projection = {
                    BaseColumns._ID,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_GENRE,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_ACTOR,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_RELEASE_DATE,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_RATING,
                    FeedReaderContract.FeedEntry.COLUMN_NAME_DOC2VEC,
            };

            Cursor cursor = db.rawQuery("Select * from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);

            int i = 0;
            while(cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE));
                String genre = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_GENRE));
                String actor = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_ACTOR));
                String doc_vec = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_DOC2VEC));
                int release_date = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_RELEASE_DATE));
                float rating = cursor.getFloat(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_RATING));

                double [] oneHot = getOneHot(genre, release_date, doc_vec);
                training_in[i] = oneHot;
                training_out[i] = rating;
                movieIds[i] = id;
                titlesSorted[i] = title;

                i++;
            }
            cursor.close();

            //Converting the data matrices into training INDArrays is straight forward
            INDArray trainingIn = Nd4j.create(training_in);
            INDArray trainingOut = Nd4j.create(training_out, new int[]{training_out.length,1});
            Log.i("INDArrayIn", trainingIn.toString());
            Log.i("INDArrayOut", trainingOut.toString());

            //define the layers of the network

            DenseLayer inputLayer = new DenseLayer.Builder()
                    .nIn(20 + Genres.size() + 1)
                    .nOut(32)
                    .name("Input")
                    .build();

            DenseLayer hiddenLayer = new DenseLayer.Builder()
                    .nIn(32)
                    .nOut(16)
                    .name("Hidden")
                    .build();

            OutputLayer outputLayer = new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_SQUARED_LOGARITHMIC_ERROR)
                    .nIn(16)
                    .nOut(1)
                    .name("Output")
                    .activation(Activation.SIGMOID)
                    .build();

            NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder();
            long seed = 6;
            nncBuilder.seed(seed);
            nncBuilder.activation(Activation.RELU);
            nncBuilder.weightInit(WeightInit.XAVIER);

            NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
            listBuilder.layer(0, inputLayer);
            listBuilder.layer(1, hiddenLayer);
            listBuilder.layer(2, outputLayer);

            MultiLayerNetwork myNetwork = new MultiLayerNetwork(listBuilder.build());
            myNetwork.init();

            //Create a data set from the INDArrays and train the network
            for(int l=0; l<=100; l++) {
                myNetwork.fit(trainingIn, trainingOut);
                Log.i("NN", "Epoch" + String.valueOf(l));
            }

            //Evaluate the input data against the model
            INDArray actualOutput = myNetwork.output(trainingIn);
            Log.d("myNetwork Output ", actualOutput.toString());

            //Here we return the INDArray to onPostExecute where it can be
            //used to update the UI
            return actualOutput;
        }

        //This is where we update the UI with our classification results
        @Override
        protected void onPostExecute(INDArray result) {
            super.onPostExecute(result);
            double [] resultArray = result.toDoubleVector();
            //for (int i = 0, len = resultArray.length; i < len; i++)Log.i("Output Array " + String.valueOf(i) , String.valueOf(resultArray[i]));
            Log.i("Output Array Result" , String.valueOf(resultArray.length));
            Log.i("Output Array Title" , String.valueOf(movieIds.length));
            for (int i = 0, len = resultArray.length; i < len; i++)moviesSorted.add(new MovieData(titlesSorted[i], resultArray[i]));

            Collections.sort(moviesSorted, new sortByRating());
            for (int i = 0, len = resultArray.length; i < len; i++)Log.i("Movie Array " + String.valueOf(i) , String.valueOf(moviesSorted.get(i).rating));

            String[] titlesTop = new String[10];
            for(int i = 0; i < 10; i++){
                titlesTop[i] = moviesSorted.get(i).title;
            }
            MovieData[] topMovies = new MovieData[10];
            for(int i = 0; i < titlesTop.length; i++){
                topMovies[i] = dbHelper.getMovieFromTitle(dbHelper, titlesTop[i]);
            }

            MovieListAdapter whatever = new MovieListAdapter(MainActivity.this, topMovies);
            listView = (ListView) findViewById(R.id.recommendationList);
            listView.setAdapter(whatever);

            //Hide the progress bar now that we are finished
            listView.setVisibility(View.VISIBLE);
            bar.setVisibility(View.INVISIBLE);

            //Retrieve the results

            //Update the UI with output


            //Aktuelle View verstecken und Vorschläge zeigen
            //TestView.setVisibility(View.INVISIBLE);
            //RecommendationView.setVisibility(View.VISIBLE);


        }
    }

    public static class MovieData {
        private String title;
        private String doc2vec;
        private String genre;
        private String actor;
        private int releaseDate;
        private String image;
        private String plot;
        private int id;
        private double rating;

        MovieData(String title, String plot, int releaseDate, String image, String actor, String genre, int id){
            this.title = title;
            this.plot = plot;
            this.releaseDate = releaseDate;
            this.image = image;
            this.actor = actor;
            this.genre = genre;
            this.id = id;
        }

        MovieData(String title, String plot, int releaseDate, String image, String actor, String genre, String doc_vec){
            this.title = title;
            this.plot = plot;
            this.releaseDate = releaseDate;
            this.image = image;
            this.actor = actor;
            this.genre = genre;
            this.rating = 0;
            this.doc2vec = doc_vec;
        }

        MovieData(String title, String plot, int releaseDate, String image, String actor, String genre, double rating){
            this.title = title;
            this.plot = plot;
            this.releaseDate = releaseDate;
            this.image = image;
            this.actor = actor;
            this.genre = genre;
            this.rating = rating;
        }

        MovieData(String title, String plot, int releaseDate, String image, String actor, String genre, double rating, int id){
            this.title = title;
            this.plot = plot;
            this.releaseDate = releaseDate;
            this.image = image;
            this.actor = actor;
            this.genre = genre;
            this.rating = rating;
        }

        MovieData(String title, double rating){
            this.title = title;
            this.rating = rating;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDoc2vec() {
            return doc2vec;
        }

        public void setDoc2vec(String doc2vec) {
            this.doc2vec = doc2vec;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public int getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(int releaseDate) {
            this.releaseDate = releaseDate;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getPlot() {
            return plot;
        }

        public void setPlot(String plot) {
            this.plot = plot;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public void VolleyGetActors(){
        String urlGet = url + "api/actors";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, urlGet, null, new Response.Listener<JSONArray>() {


                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("Actors Transmitted", response.toString());
                        for (int i = 0; i<response.length(); i++) {
                            try {
                                Actors.add(response.getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("Actors Transmitted", Actors.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Actor Error", error.toString());

                    }
                });

        queue.add(jsonArrayRequest);

    }

    public void VolleyGetGenres(){
        String urlGet = url + "api/genres";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, urlGet, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("Genres Transmitted", response.toString());
                        for (int i = 0; i<response.length(); i++) {
                            try {
                                Genres.add(response.getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("Genres Transmitted", Genres.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Genre Error", error.toString());

                    }
                });

        queue.add(jsonArrayRequest);

    }

    public void VolleyGetDB(){
        String urlGet = url + "api/all_films/0";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, urlGet, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        //Handle incoming data

                        try {
                            maxBatches = response.getJSONObject(0).getInt("max_batch_num");
                            Log.i("Volley", String.valueOf(maxBatches));
                            for(int i = 1; i <= maxBatches; i++){
                                VolleyGetDB(i);
                            }
                            ArrayList<MovieData> movies = new ArrayList<>();
                            for(int i = 1; i<response.length(); i++){
                                movies.add(jsonAdapterMovie(response.getJSONObject(i)));
                            }
                            long linesAdded = dbHelper.fillDB(dbHelper, movies);
                            Movies = dbHelper.ReadFromDB(dbHelper);
                            Log.i("Layout", String.valueOf(Movies.size()));
                            fillMovieLayout(Movies.get(0));
                            Movies.remove(0);
                            Log.i("Volley", response.toString());
                            Log.i("Volley", String.valueOf(linesAdded) + " lines added");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley", "Fehler in Get request");
                        Log.e("Volley", error.toString());
                    }
                });

        queue.add(jsonArrayRequest);

    }

    public void VolleyGetDB(int batchNum){
        String urlGet = url + "api/all_films/" + batchNum;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, urlGet, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        //Log.i("Volley", "Response: " + response.toString());
                        //Handle incoming data
                        ArrayList<MovieData> movies = new ArrayList<>();
                        for(int i = 1; i<response.length(); i++){
                            try {
                                maxBatches = response.getJSONObject(0).getInt("max_batch_num");
                                movies.add(jsonAdapterMovie(response.getJSONObject(i)));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        long linesAdded = dbHelper.fillDB(dbHelper, movies);
                        if((linesAdded / maxBatches) < 105 && (linesAdded / maxBatches) > 99){
                            int movielength = Movies.size();
                            Movies = dbHelper.ReadFromDB(dbHelper);
                            Log.i("Layout", String.valueOf(Movies.size()));
                            for(int i = Movies.size(); i > movielength; i--){
                                Movies.remove(0);
                            }
                            Log.i("Layout", "Final Layout update");
                        }
                        Log.i("Volley", String.valueOf(linesAdded) + " lines added");
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley", "Fehler in Get request");
                        Log.e("Volley", error.toString());
                    }
                });

        queue.add(jsonArrayRequest);

    }

    public static void VolleyPostUpdate(String title, int year, String username, float rating){
        try {
            String URL = MainActivity.url + "api/post_rating";
            Log.i("Post", URL);
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", title);
            jsonBody.put("year", year);
            jsonBody.put("username", username);
            jsonBody.put("rating", rating);
            //final String requestBody = jsonBody.toString();

            JsonObjectRequest req = new JsonObjectRequest(URL, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                VolleyLog.v("Response:%n %s", response.toString(4));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e("Volley", "Im onResponse");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse response = error.networkResponse;
                    String errorMsg = "";
                    if(response != null && response.data != null) {
                        String errorString = new String(response.data);
                        Log.i("log error", errorString);
                    }
                    Log.e("Volley", "Im onErrorResponse");
                }
            });

            queue.add(req);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<MovieData> getRandomDBEntries(DatabaseHelper mDBHelper){
        try {
            mDBHelper.updateDataBase();
        } catch (IOException mIOException) {
            throw new Error("UnableToUpdateDatabase");
        }

        try {
            mDb = mDBHelper.getWritableDatabase();
        } catch (SQLException mSQLException) {
            throw mSQLException;
        }

        Log.i("Movies", "Vor dem DB Zugriff");

        final String MY_QUERY = "Select api_film.id, title, release_year, doc_vec, link_to_picture, short, api_actor.name AS actor, api_genre.name AS genre \n" +
                "From api_film\n" +
                "join api_film_actors on api_film.id = api_film_actors.film_id\n" +
                "join api_actor on api_film_actors.actor_id = api_actor.id\n" +
                "join api_film_genre on api_film.id = api_film_genre.film_id\n" +
                "join api_genre on api_film_genre.genre_id = api_genre.id\n" +
                "group by title\n" +
                "order by RANDOM() LIMIT 10";

        Cursor cursor = this.mDb.rawQuery(MY_QUERY, null);

        // To increase performance first get the index of each column in the cursor
        final int idIndex = cursor.getColumnIndex("id");
        final int titleIndex = cursor.getColumnIndex("title");
        final int release_yearIndex = cursor.getColumnIndex("release_year");
        final int shortIndex = cursor.getColumnIndex("short");
        final int link_to_pictureIndex = cursor.getColumnIndex("link_to_picture");
        final int actorIndex = cursor.getColumnIndex("actor");
        final int genreIndex = cursor.getColumnIndex("genre");

        try {
            // If moveToFirst() returns false then cursor is empty
            if (!cursor.moveToFirst()) {
                return new ArrayList<MovieData>();
            }

            final ArrayList<MovieData> Movies = new ArrayList<>();

            do {
                // Read the values of a row in the table using the indexes acquired above
                final int id = cursor.getInt(idIndex);
                final String title = cursor.getString(titleIndex);
                final int release_year = cursor.getInt(release_yearIndex);
                final String mshort = cursor.getString(shortIndex);
                final String link_to_picture = cursor.getString(link_to_pictureIndex);
                final String actor = cursor.getString(actorIndex);
                final String genre = cursor.getString(genreIndex);

                Movies.add(new MovieData(title, mshort, release_year, link_to_picture, actor, genre, id));

            } while (cursor.moveToNext());

            return Movies;
        }finally {
            // Don't forget to close the Cursor once you are done to avoid memory leaks.
            // Using a try/finally like in this example is usually the best way to handle this
            cursor.close();

            // close the database
            mDb.close();
        }
    }

    public MovieData jsonAdapterMovie(JSONObject movie){
        String title = null;
        try {
            title = movie.getString("title");
            int release_year = movie.getInt("release_year");
            String actor = "";
            JSONArray actors = movie.getJSONArray("actors");
            for(int i = 0; i<actors.length(); i++){
                if(i == actors.length() - 1){
                    actor = actor + actors.getString(i);
                }else{
                    actor = actor + actors.getString(i) + ",";
                }

            }
            String genre = "";
            JSONArray genres = movie.getJSONArray("genres");
            for(int i = 0; i<genres.length(); i++){
                genre = genre + "," + genres.getString(i);
            }
            String doc_vec = movie.getString("doc_vec");
            /*
            JSONArray dec_vecs = movie.getJSONArray("doc_vec");
            for(int i = 0; i<dec_vecs.length(); i++){
                doc_vec = doc_vec + "," + dec_vecs.getDouble(i);
            }

             */
            String plot = movie.getString("plot");
            String image = movie.getString("image_link");

            return new MovieData(title, plot, release_year, image, actor, genre, doc_vec);
        } catch (JSONException e) {
            Log.i("Volley", "Fehler beim JSON parsen");
            e.printStackTrace();
            return null;
        }
    }

    public class sortByRating implements Comparator<MovieData> {
        // Used for sorting in ascending order of
        // roll number
        public int compare(MovieData a, MovieData b)
        {
            if (a.rating > b.rating) return -1;
            if (a.rating < b.rating) return 1;
            return 0;
        }
    }
}

