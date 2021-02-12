package de.hsos.ma.ma_project;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.io.InputStream;

public class MovieListAdapter extends ArrayAdapter {
    //to reference the Activity
    private final Activity context;

    private final MainActivity.MovieData[] movieArray;


    public MovieListAdapter(Activity context, MainActivity.MovieData[] movieParam){

        super(context,R.layout.movie_layout , movieParam);

        this.context=context;
        this.movieArray = movieParam;

    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.movie_layout, null,true);

        //this code gets references to objects in the movie_layout.xml file
        TextView titelTextField = (TextView) rowView.findViewById(R.id.movie_titel);
        TextView genreTextField = (TextView) rowView.findViewById(R.id.movie_genre);
        TextView actorTextField = (TextView) rowView.findViewById(R.id.movie_actor);
        TextView plotTextField = (TextView) rowView.findViewById(R.id.movie_plot);
        TextView releaseDateTextField = (TextView) rowView.findViewById(R.id.movie_release_date);
        TextView idTextField = (TextView) rowView.findViewById(R.id.movie_id);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.movie_image);
        Button btnSubmit = (Button) rowView.findViewById(R.id.btnSubmit);
        RatingBar ratingBar = (RatingBar) rowView.findViewById(R.id.ratingBar);

        //this code sets the values of the objects to values from the arrays
        titelTextField.setText(movieArray[position].getTitle());
        genreTextField.setText(movieArray[position].getGenre());
        actorTextField.setText(movieArray[position].getActor());
        plotTextField.setText(movieArray[position].getPlot());
        releaseDateTextField.setText(String.valueOf(movieArray[position].getReleaseDate()));
        //TODO: Id einfügen
        idTextField.setText(String.valueOf(movieArray[position]));
        new DownloadImageTask(imageView).execute("https:" + movieArray[position].getImage());

        btnSubmit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //TODO:DB updaten + Post senden
                Log.i("Listview", "Hier im Button " + movieArray[position].getTitle());
                Log.i("Listview", "Rating: " + String.valueOf((ratingBar.getRating() - 1) / 4));
            }
        });

        return rowView;

    };

    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
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

}
