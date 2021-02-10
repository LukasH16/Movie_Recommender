package de.hsos.ma.ma_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;

import com.google.gson.Gson;

import org.bytedeco.tesseract.INT_FEATURE_STRUCT;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
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
    private Button btnSubmit;
    private RatingBar ratingBar;
    private LinearLayout movieRateLayout;
    private ListView listView;
    private ProgressBar bar;
    boolean online = false;
    private FeedReaderContract.FeedReaderDbHelper dbHelper;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        movieRateLayout = findViewById(R.id.movieRateLayout);

        MovieListAdapter whatever = new MovieListAdapter(this, titelArray, genreArray, imageArray, titelArray, genreArray, titelArray);
        listView = (ListView) findViewById(R.id.recommendationList);
        listView.setAdapter(whatever);

        bar = (ProgressBar) findViewById(R.id.progressBar);

        // Daten vom Server holen
        /*
        String data = getJSON("UnsereURL");
        MovieData msg = new Gson().fromJson(data, MovieData.class);
        System.out.println(msg);
         */

        dbHelper = new FeedReaderContract.FeedReaderDbHelper(this);

        //Daten von einem Film in DB speichern
        WriteToDB(dbHelper, null);

        // 20 Datensätze zur initialisierung bestimmen
        /*
        Cursor cursor = ReadFromDB(dbHelper, null);
        List MovieTitles = new ArrayList<>();
        while(cursor.moveToNext()) {
            String MovieTitle = cursor.getString(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE));
            MovieTitles.add(MovieTitle);
        }
        cursor.close();
        */

        addListenerOnRatingButton();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }


    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public void addListenerOnRatingButton() {
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //if click on me, then display the current rating value.
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterForInit++;
                bar.setVisibility(View.VISIBLE);
                Log.i("Rating: ", String.valueOf((ratingBar.getRating() - 1) / 4));
                //Daten in DB Updaten
                //Daten per Post an Server
                //Neuen Film laden
                bar.setVisibility(View.GONE);

                if(counterForInit>=10){
                    movieRateLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }
            }

        });

    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
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

    public String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    public long WriteToDB(FeedReaderContract.FeedReaderDbHelper dbHelper, MovieData movie){
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, "");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WORD2VEC, "");
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
                FeedReaderContract.FeedEntry.COLUMN_NAME_WORD2VEC,
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
        private List<String> doc2vec;
        private List<String> genre;
        private List<String> actor;
        private int releaseDate;
        private String image;
        private String plot;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getWord2vec() {
            return doc2vec;
        }

        public void setWord2vec(List<String> word2vec) {
            this.doc2vec = word2vec;
        }

        public List<String> getGenre() {
            return genre;
        }

        public void setGenre(List<String> genre) {
            this.genre = genre;
        }

        public List<String> getActor() {
            return actor;
        }

        public void setActor(List<String> actor) {
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
}

