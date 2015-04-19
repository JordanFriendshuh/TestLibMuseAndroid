package com.interaxon.test.libmuse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

import com.example.christian.museic.MuseActivity;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import android.widget.EditText;
import android.view.View;
import android.content.Intent;

/**
 * Created by Christian, Shannon, and Jordan
 * Using Spotify SDK,
 */
public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback, OnClickListener {


    Intent muse;
    private static final String CLIENT_ID = "51968b9f394540049602ba37ae4c7db5";

    private static final String REDIRECT_URI = "museic-login://callback";//FUCK YOU TODO FIX

    // Request code that will be passed together with authentication result to the onAuthenticationResult callback
// Can be any integer
    private static final int REQUEST_CODE = 1337;

    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        ImageButton pauseButton = (ImageButton) findViewById(R.id.spotPause);
        pauseButton.setBackgroundResource(R.drawable.pause);
        pauseButton.setOnClickListener(this);
        ImageButton nextButton = (ImageButton) findViewById(R.id.next);
        nextButton.setBackgroundResource(R.drawable.next);
        nextButton.setOnClickListener(this);
        muse = new Intent(this, MuseActivity.class);
        startActivity(muse);
        setContentView(R.layout.activity_main);

    }
    boolean pauseBool = false;
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pause) {
            ImageButton pauseButton = (ImageButton) findViewById(R.id.spotPause);
            if(!pauseBool) {
                mPlayer.pause();
                pauseButton.setBackgroundResource(R.drawable.play);
                pauseBool = true;
            }else{
                mPlayer.resume();
                pauseButton.setBackgroundResource(R.drawable.pause);
                pauseBool = false;
            }
        }
        else if (v.getId() == R.id.next) {
            //Compute head stuff and display
            mPlayer.skipToNext();
            mPlayer.getPlayerState(new PlayerStateCallback() {
                @Override
                public void onPlayerState(PlayerState playerState) {
                    if(!playerState.playing){
                        mPlayer.getPlayerState(this);
                    }else {
                        System.out.println("Track Uri " + playerState.trackUri);
                        new HttpAsyncTask().execute("https://api.spotify.com/v1/tracks/" + playerState.trackUri.split(":")[2] );
                    }
                }
            });
        }
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            TextView elem1 = (TextView) findViewById(R.id.Songinfo);
            elem1.setText(result);
            //Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            //etResponse.setText(result);
        }
    }
    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpGet getRequest = new HttpGet(url);
            getRequest.addHeader("Accept:", "application/json");
            //getRequest.addHeader("Host:", "api.spotify.com");
            //getRequest.addHeader("Accept-Encoding:", "gzip, deflate, compress");
            getRequest.addHeader("User-Agent:", CLIENT_ID);
            HttpResponse httpResponse = httpclient.execute(getRequest);

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            if(line.toLowerCase().contains("name".toLowerCase())){
                String temp = line.split(":")[1];
                result = result + temp.replace("\"", "");
            }
        }
        inputStream.close();
        return result;

    }

    EditText etResponse;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                        mPlayer.play("spotify:user:1223090214:playlist:5A4TFRZVYxK1H1Bc9X9Bxl");
                        System.out.println("Getting State");
                        mPlayer.getPlayerState(new PlayerStateCallback() {
                            @Override
                            public void onPlayerState(PlayerState playerState) {
                                if(!playerState.playing){
                                    mPlayer.getPlayerState(this);
                                }else {
                                    System.out.println("Track Uri " + playerState.trackUri);
                                    new HttpAsyncTask().execute("https://api.spotify.com/v1/tracks/" + playerState.trackUri.split(":")[2] );
                                }
                            }
                        });
                        //playlist from Christian's spotify
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        switch (eventType) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }


   //public void moveBitch(View view)



}