package de.hsos.ma.ma_project;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class MovieListAdapter extends ArrayAdapter {
    //to reference the Activity
    private final Activity context;

    //to store the animal images
    private final Integer[] imageIDarray;

    //to store the list of countries
    private final String[] titelArray;

    //to store the list of countries
    private final String[] genreArray;

    //to store the list of countries
    private final String[] actorArray;

    //to store the list of countries
    private final String[] releaseDateArray;

    //to store the list of countries
    private final String[] plotArray;

    public MovieListAdapter(Activity context, String[] titelArrayParam, String[] genreArrayParam,
                             Integer[] imageIDArrayParam, String[] actorArrayParam,
                             String[] plotArrayParam, String[] releaseDateArrayParam){

        super(context,R.layout.movie_layout , titelArrayParam);

        this.context=context;
        this.imageIDarray = imageIDArrayParam;
        this.titelArray = titelArrayParam;
        this.genreArray = genreArrayParam;
        this.actorArray = actorArrayParam;
        this.plotArray = plotArrayParam;
        this.releaseDateArray = releaseDateArrayParam;

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
        ImageView imageView = (ImageView) rowView.findViewById(R.id.movie_image);
        Button btnSubmit = (Button) rowView.findViewById(R.id.btnSubmit);
        RatingBar ratingBar = (RatingBar) rowView.findViewById(R.id.ratingBar);

        //this code sets the values of the objects to values from the arrays
        titelTextField.setText(titelArray[position]);
        genreTextField.setText(genreArray[position]);
        actorTextField.setText(actorArray[position]);
        plotTextField.setText(plotArray[position]);
        releaseDateTextField.setText(releaseDateArray[position]);
        imageView.setImageResource(imageIDarray[position]);

        btnSubmit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //DB updaten
                //Post senden
                Log.i("Listview", "Hier im Button " + titelArray[position]);
                Log.i("Listview", "Rating: " + String.valueOf((ratingBar.getRating() - 1) / 4));
            }
        });

        return rowView;

    };
}
