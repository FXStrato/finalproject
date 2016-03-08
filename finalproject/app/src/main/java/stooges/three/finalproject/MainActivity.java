package stooges.three.finalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.dd.CircularProgressButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    CircularProgressButton maincircle;
    CircularProgressButton favcircle;
//    ArrayList<String> names = new ArrayList<String>();
//    ArrayList<String> rating = new ArrayList<String>();
//    ArrayList<String> image = new ArrayList<String>();
//    ArrayList<String> address = new ArrayList<String>();
//    ArrayList<String> url = new ArrayList<String>();
    ArrayList<Restaurant> restaurants;

    final double lat = 47.655149;
    final double lon = -122.307947;
    final int dist = 8046;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // declare initialize favorite button
        Button favBtn = (Button) findViewById(R.id.favs);
        favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // starts Favorite Activity via intent
                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                startActivity(intent);
                // specifies an explicit transition animation to perform next.
                overridePendingTransition(0, 0);
            }
        });

        // declare and initialize settings button
        ImageButton button = (ImageButton) findViewById(R.id.settings_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // starts Settings Activity via intent
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
            }
        });

        // initialize Circular Progress Button
        maincircle = (CircularProgressButton) findViewById(R.id.search_button);

        // Within this method, call the async task that connects to Yelp and pulls restaurant data
        maincircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (maincircle.isIndeterminateProgressMode()) {
                    maincircle.setIndeterminateProgressMode(false);
                    maincircle.setProgress(0);
                } else {
                    maincircle.setIndeterminateProgressMode(true);
                    maincircle.setProgress(50); // set progress > 0 & < 100 to display indeterminate progress
                    //Get the necessary information first from preferences, to make sure we are searching correctly.
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String distance = sharedPref.getString("pref_distance", "");
                    if(distance != "") new YelpApi().execute("restaurant", lat + "", lon + "", distance + "");
                    else new YelpApi().execute("restaurant", lat + "", lon + "", dist + "");
                }
            }
        });

        // Within this method, call the async task that will pull restaurant from favorites list
        favcircle = (CircularProgressButton) findViewById(R.id.favorites_search);
        // TODO: 3/8/2016 Create intent and start Favorites List Activity
        favcircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(favcircle.isIndeterminateProgressMode()) {
                    favcircle.setIndeterminateProgressMode(false);
                    favcircle.setProgress(0);
                } else {
                    favcircle.setIndeterminateProgressMode(true);
                    favcircle.setProgress(50); // set progress > 0 & < 100 to display indeterminate progress
                }
            }
        });

        // todo: Sean's question - what is this (below) doing here?
        // todo: to me, this just checks if there's anything in favorites list (regardless of user
        // todo: input, and shows the fav circle?? Wouldn't we want it to show when the favorites
        // todo: button is pressed? Or is that what's happening and I don't see it?
        // Parse through the favorites in the preference screen to see if they have any favorites
        // if they don't have more than 2 favorites, don't display the hit me with favorites button
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // todo: Sean's question - where and when is pref_favorites stored in shared pref with data?
        // todo: in what form? arraylist?
        String favorites = sharedPref.getString("pref_favorites", "Error");
        if(!favorites.equals("Error")) {
            // Means there is favorites. Decide how to separate each restaurant first. We can use | for now
            String[] restlist = favorites.split("|");
            if (restlist.length > 1) {
                // shows loading
                favcircle.setVisibility(View.VISIBLE);
            }
        } else {
            favcircle.setVisibility(View.GONE);
            // notification for user to let them know that there are no favorites
            Toast.makeText(MainActivity.this, "No Favorites stored!", Toast.LENGTH_SHORT).show();
        }
    }

    public class YelpApi extends AsyncTask<String, Void, String> {
        private static final String API_HOST = "api.yelp.com";
        private static final String SEARCH_PATH = "/v2/search";

        private static final String consumer_key = "7EPO_0MB-5XsfzeE5PpqRw";
        private static final String consumer_secret = "5BYGwh_WC9AdJL6_dKFYr1JKCVc";
        private static final String token = "pXGPbhiaGJ7YiXADvT5uxSOSAFqGzKVy";
        private static final String token_secret = "NEIInw9KhCUBcu6s5qzYl6A5oUo";

        private OAuthService service;
        private Token accessToken;

        @Override
        protected String doInBackground(String... params) {
            String term = params[0];
            String latitude = params[1];
            String longitude = params[2];
            String radius = params[3];

            // set up service
            this.service = new ServiceBuilder().provider(YelpApi2.class).apiKey(consumer_key).apiSecret(consumer_secret).build();
            this.accessToken = new Token(token, token_secret);

            // make query and sign
            OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
            request.addQuerystringParameter("term", term);
            request.addQuerystringParameter("radius", radius);
            request.addQuerystringParameter("ll", latitude + "," + longitude);
            this.service.signRequest(this.accessToken, request);

            // send query
            Response response = request.send();
            return response.getBody();
        }

        protected void onPostExecute(String response) {
            try{
                // todo: Sean cmt - insert what information? be specific
                // insert the information into the arraylist. Should be passed to restaurant activity
                // through an intent, along with all the necessary information. The characters can be parsed there.
                // todo: what characters? ^

                restaurants = new ArrayList<Restaurant>();

                JSONObject json = new JSONObject(response);
                JSONArray businesses;
                businesses = json.getJSONArray("businesses");
                for(int i = 0; i < businesses.length(); i++) {
                    JSONObject rest = businesses.getJSONObject(i);
                    String name = rest.getString("name");
                    String rating = rest.getString("rating_img_url");
                    String img = rest.getString("image_url");
                    String address = rest.getJSONObject("location").getString("display_address");
                    String yelpUrl = rest.getString("url");
                    restaurants.add(new Restaurant(name, rating, img, address, yelpUrl));
                    // testing: prints out restaurants from array list that we stored from the
                    // response from Yelp API
                    // Log.v(TAG, restaurants.get(restaurants.size() - 1) + "");
                }

                // testing: prints out
                // printResults("SEARCHTEST", response);

                Intent intent = new Intent(getApplicationContext(), RestaurantDetailActivity.class);
                // todo: Sean - is there a difference in below?
                intent.putParcelableArrayListExtra("restaurants", restaurants);
                intent.putExtra("restaurants", restaurants);

                startActivity(intent);

            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        public void printResults(String TAG, String message) {
            int maxLogSize = 2000;
            for(int i = 0; i <= message.length() / maxLogSize; i++) {
                int start = i * maxLogSize;
                int end = (i+1) * maxLogSize;
                end = end > message.length() ? message.length() : end;
                Log.v(TAG, message.substring(start, end));
            }
        }

    }



}

//Instructions for messing with the circular button: https://github.com/dmytrodanylyk/circular-progress-button/wiki/User-Guide

//Spinner spinner = (Spinner) findViewById(R.id.filter_distance);
//// Create an ArrayAdapter using the string array and a default spinner layout
//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//        R.array.distance, android.R.layout.simple_spinner_item);
//// Specify the layout to use when the list of choices appears
//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        // Apply the adapter to the spinner
//        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(this);