package de.hsos.ma.ma_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    int counterForInit = 0;
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
    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;
    private RequestQueue queue;
    private ArrayList<MovieData> Movies;

    //https://stackoverflow.com/questions/39058638/android-volley-noconnectionerror
    String url ="http://192.168.178.32:8000/";

    //Dummy Data
    String[] titelArray = {"Octopus","Pig","Sheep","Rabbit","Snake","Spider" };

    String[] genreArray = {
            "8 tentacled monster",
            "Delicious in rolls",
            "Great for jumpers",
            "Nice in a stew",
            "Great for shoes",
            "Scary."
    };

    Integer[] imageArray = {R.drawable.movie_image_placeholder,
            R.drawable.movie_image_placeholder,
            R.drawable.movie_image_placeholder,
            R.drawable.movie_image_placeholder,
            R.drawable.movie_image_placeholder,
            R.drawable.movie_image_placeholder};

    Integer[] idArray = {1,2,3,4,5,6};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movieRateLayout = findViewById(R.id.movieRateLayout);

        MovieListAdapter whatever = new MovieListAdapter(this, titelArray, genreArray, imageArray, titelArray, genreArray, titelArray, idArray);
        listView = (ListView) findViewById(R.id.recommendationList);
        listView.setAdapter(whatever);

        bar = (ProgressBar) findViewById(R.id.progressBar);
        ratingTitle = (TextView) findViewById(R.id.movie_titel);
        ratingActor = (TextView) findViewById(R.id.movie_actor);
        ratingGenre = (TextView) findViewById(R.id.movie_genre);
        ratingID = (TextView) findViewById(R.id.movie_id);
        ratingImage = (ImageView) findViewById(R.id.movie_image);
        ratingPlot = (TextView) findViewById(R.id.movie_plot);
        ratingReleaseDate = (TextView) findViewById(R.id.movie_release_date);

        queue = Volley.newRequestQueue(this);

        VolleyGetDB();

        //Log.i("Volley", "Bevor Get angefragt wird");
        //VolleyGetDB();

        //dbHelper = new FeedReaderContract.FeedReaderDbHelper(this);

        mDBHelper = new DatabaseHelper(this);

        Movies = getRandomDBEntries(mDBHelper);
        fillMovieLayout(Movies.get(0));
        Movies.remove(0);
        addListenerOnRatingButton();
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
                updateMovieRating(Integer.parseInt(ratingID.getText().toString()), rating, 3);
                //Daten per Post an Server
                VolleyPostUpdate(ratingTitle.getText().toString(), Integer.valueOf(ratingReleaseDate.getText().toString()), "Action", rating);

                if(!Movies.isEmpty()){
                    //Neuen Film laden
                    fillMovieLayout(Movies.get(0));
                    Movies.remove(0);
                    bar.setVisibility(View.GONE);
                }else{
                    movieRateLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
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

        if (id == R.id.online_offline) {
            if(online){
                online = false;
                //Asnyc Funktion losschicken
                AsyncTaskRunner runner = new AsyncTaskRunner();

                //pass the measurement as params to the AsyncTask
                //Hier müssen evtl. die Inputs übergeben werden
                runner.execute();

                bar.setVisibility(View.VISIBLE);
            }else{
                online = true;

                //Server-Request losschicken
                AsyncTaskRunnerPost postReq = new AsyncTaskRunnerPost();
                postReq.execute("start");
            }
        }
        if (id == R.id.retry_favorites) {
            //TODO:10 neue Filme aus der DB ziehen
            //ReadFromDB(dbHelper, null);
            //TODO:Die Imagebuttons füllen
            counterForInit = 0;
            movieRateLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }

        return super.onOptionsItemSelected(item);
    }

    public long WriteToDB(FeedReaderContract.FeedReaderDbHelper dbHelper, MovieData movie){
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_DOC2VEC, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GENRE, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_ACTOR, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_RELEASE_DATE, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_IMAGE, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_RATING, "");

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
        return newRowId;
    }

    public Cursor ReadFromDB(FeedReaderContract.FeedReaderDbHelper dbHelper, String title){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                BaseColumns._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_DOC2VEC,
                FeedReaderContract.FeedEntry.COLUMN_NAME_GENRE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_ACTOR,
                FeedReaderContract.FeedEntry.COLUMN_NAME_RELEASE_DATE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_IMAGE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_RATING
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE + " = ?";
        String[] selectionArgs = { title };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                FeedReaderContract.FeedEntry.COLUMN_NAME_RELEASE_DATE + " DESC";

        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        return cursor;
    }

    public int UpdateDB(FeedReaderContract.FeedReaderDbHelper dbHelper, String title, int rating){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_RATING, rating);

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

    private class AsyncTaskRunner extends AsyncTask<Double, Integer, INDArray> {

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            bar.setVisibility(View.INVISIBLE);
        }

        // This is our main background thread for the neural net
        @Override
        protected INDArray doInBackground(Double... params) {
            //TODO:Wir haben eigentlich keinen gesonderten User Input

            //Get the value from params, which is an array so they will be 0,1,2,3
            /*
            double pld = params[0];
            double pwd = params[1];
            double sld = params[2];
            double swd = params[3];

            //Create input INDArray for the user Input
            INDArray actualInput = Nd4j.zeros(1,4);
            actualInput.putScalar(new int[]{0,0}, pld);
            actualInput.putScalar(new int[]{0,1}, pwd);
            actualInput.putScalar(new int[]{0,2}, sld);
            actualInput.putScalar(new int[]{0,3}, swd);
             */

            //TODO:Alle Daten aus der DB in Matrix überführen

            //Convert the iris data into 150x4 matrix
            /*
            int row=150;
            int col=4;
            double[][] irisMatrix=new double[row][col];
            int i = 0;
            for(int r=0; r<row; r++){
                for( int c=0; c<col; c++){
                    irisMatrix[r][c]=com.example.jmerwin.irisclassifier.DataSet.irisData[i++];
                }
            }
            */

            //TODO:Alle Label Daten in Matrix überführen

            //Now do the same for the label data
            /*
            int rowLabel=150;
            int colLabel=3;
            double[][] twodimLabel=new double[rowLabel][colLabel];
            int ii = 0;
            for(int r=0; r<rowLabel; r++){
                for( int c=0; c<colLabel; c++){
                    twodimLabel[r][c]=com.example.jmerwin.irisclassifier.DataSet.labelData[ii++];
                }
            }
            */

            //TODO:Matrixen in INDArrays umformen
            //Converting the data matrices into training INDArrays is straight forward
            /*
            INDArray trainingIn = Nd4j.create(irisMatrix);
            INDArray trainingOut = Nd4j.create(twodimLabel);
             */

            //define the layers of the network
            DenseLayer inputLayer = new DenseLayer.Builder()
                    .nIn(4)
                    .nOut(3)
                    .name("Input")
                    .build();

            DenseLayer hiddenLayer = new DenseLayer.Builder()
                    .nIn(3)
                    .nOut(3)
                    .name("Hidden")
                    .build();

            OutputLayer outputLayer = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nIn(3)
                    .nOut(10)
                    .name("Output")
                    .activation(Activation.SOFTMAX)
                    .build();

            NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder();
            long seed = 6;
            nncBuilder.seed(seed);
            nncBuilder.activation(Activation.TANH);
            nncBuilder.weightInit(WeightInit.XAVIER);

            NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
            listBuilder.layer(0, inputLayer);
            listBuilder.layer(1, hiddenLayer);
            listBuilder.layer(2, outputLayer);

            MultiLayerNetwork myNetwork = new MultiLayerNetwork(listBuilder.build());
            myNetwork.init();

            //TODO:Netzwerk trainieren
            /*
            //Create a data set from the INDArrays and train the network
            DataSet myData = new DataSet(trainingIn, trainingOut);
            for(int l=0; l<=1000; l++) {
                myNetwork.fit(myData);
            }
             */

            /*
            //TODO:Weiß nicht ob wir gesondert evaluieren müssen
            //Evaluate the input data against the model
            INDArray actualOutput = myNetwork.output(actualInput);
            Log.d("myNetwork Output ", actualOutput.toString());

            //Here we return the INDArray to onPostExecute where it can be
            //used to update the UI
            return actualOutput;
             */
            //TODO: Richtigen output zurückgeben
            return null;
        }

        //This is where we update the UI with our classification results
        @Override
        protected void onPostExecute(INDArray result) {
            super.onPostExecute(result);

            //Hide the progress bar now that we are finished
            //ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
            //bar.setVisibility(View.INVISIBLE);

            //Retrieve the results

            //Update the UI with output


            //Aktuelle View verstecken und Vorschläge zeigen
            //TestView.setVisibility(View.INVISIBLE);
            //RecommendationView.setVisibility(View.VISIBLE);


        }
    }

    public class MovieData {
        private String title;
        private String doc2vec;
        private String genre;
        private String actor;
        private int releaseDate;
        private String image;
        private String plot;
        private int id;

        MovieData(String title, String plot, int releaseDate, String image, String actor, String genre, int id){
            this.title = title;
            this.plot = plot;
            this.releaseDate = releaseDate;
            this.image = image;
            this.actor = actor;
            this.genre = genre;
            this.id = id;
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

    private class AsyncTaskRunnerPost extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            try {
                String url="Unsere URL";
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestMethod("POST");

                JSONObject cred = new JSONObject();
                //TODO: Json befüllen

                DataOutputStream localDataOutputStream = new DataOutputStream(con.getOutputStream());
                localDataOutputStream.writeBytes(cred.toString());
                localDataOutputStream.flush();
                localDataOutputStream.close();


            }
            catch (Exception e){
                Log.v("ErrorAPP",e.toString());
            }
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
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

    public void VolleyGetDB(){
        url = url + "api/all_films/0";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("Volley", "Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.e("Volley", "Fehler in Get request");
                        Log.e("Volley", error.toString());
                    }
                });

        // Access the RequestQueue through your singleton class.
        queue.add(jsonArrayRequest);

    }

    public void VolleyPostUpdate(String title, int year, String username, float rating){
        try {
            //TODO: Irgendwie den Server erreichen
            String URL = url + "api/post_rating";
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

            // Add the realibility on the connection.
            req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));

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

    public void updateMovieRating(int movie_id, float rating, int user_id) {
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

        ContentValues contentValues = new ContentValues();
        contentValues.put("film_id", movie_id);
        contentValues.put("user_id", user_id);
        contentValues.put("rating", rating);
        if(this.mDb.update("api_rating", contentValues, "film_id = " + movie_id + " AND user_id = " + user_id, null) == 0){
            Log.i("Rating", "Noch kein Rating vorhanden");
            this.mDb.insert("api_rating", null, contentValues);
        }

        // close the database
        mDb.close();
    }

    public void parseVolleyError(VolleyError error) {
        try {
            String responseBody = new String(error.networkResponse.data, "utf-8");
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);
            String message = jsonMessage.getString("message");
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
        } catch (UnsupportedEncodingException errorr) {
        }
    }
}

