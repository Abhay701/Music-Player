package com.example.music;

import static com.example.music.ApplicationClass.ACTION_NEXT;
import static com.example.music.ApplicationClass.ACTION_PLAY;
import static com.example.music.ApplicationClass.ACTION_PREVIOUS;
import static com.example.music.ApplicationClass.CHANNEL_ID_2;
import static com.example.music.PlayerActivity.listSongs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.security.Provider;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener  {


    AudioManager audioManager;
    IBinder mBinder=new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicFiles> musicFiles=new ArrayList<>();
    Uri uri;
    int position=-1;
    MediaSessionCompat mediaSessionCompat;
    ActionPlaying actionPlaying;

    public static final String MUSIC_LAST_PLAYED="LAST_PLAYED";
    public static final String MUSIC_FILE="STORED_MUSIC";
    public static final String ARTIST_NAME="ARTIST NAME";
    public static final String SONG_NAME="SONG NAME";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSessionCompat=new MediaSessionCompat(getBaseContext(),"My Audio");


        // Retrieve last played song information
        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
        String storedUri = preferences.getString(MUSIC_FILE, null);
        String storedArtist = preferences.getString(ARTIST_NAME, null);
        String storedSong = preferences.getString(SONG_NAME, null);

        if (storedUri != null) {
            mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(storedUri));
            position = getPositionFromPath(storedUri);
            if (actionPlaying != null) {
                actionPlaying.playPauseBtnClicked();
            }
            // Start playback or update notification based on the playback state
            if (mediaPlayer.isPlaying()) {
                showNotification(R.drawable.ic_pause);
            } else {
                showNotification(R.drawable.ic_play);
            }
        }


    }

    private int getPositionFromPath(String path) {
        for (int i = 0; i < musicFiles.size(); i++) {
            if (musicFiles.get(i).getPath().equals(path)) {
                return i;
            }
        }
        return -1; // Return -1 if path is not found
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        Log.e("Bind","Method");
        return mBinder;
    }


    public class MyBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int myPosition=intent.getIntExtra("servicePosition",-1);
        String actionName= intent.getStringExtra("ActionName");
        if (myPosition!=-1){
            playMedia(myPosition);
        }
        if (actionName!=null){
            switch (actionName){
                case "playPause":
//                    Toast.makeText(this, "PlayPause", Toast.LENGTH_SHORT).show();
                    playPauseBtnnClicked();
                    break;
                case "next":
//                    Toast.makeText(this, "Next", Toast.LENGTH_SHORT).show();
                    nextBtnnClicked();
                    break;
                case "previous":
//                    Toast.makeText(this, "Previous", Toast.LENGTH_SHORT).show();
                    previousBtnClicked();
                    break;
            }
        }

        // Manage foreground service and notification based on app visibility
        if (isAppVisible()) {
            // App is visible, start or update foreground notification if needed
            if (isPlaying()) {
                showNotification(R.drawable.ic_pause); // Update notification if playing
            } else {
                stopForeground(true); // Remove foreground if not playing
            }
        } else {
            stopSelf(); // Stop the service
        }

        return START_STICKY;
    }

    private boolean isAppVisible() {
        // Check if the app is visible or in foreground
        // You can implement your logic here to check if any activities are visible
        // For simplicity, you can return true or false based on a static flag or any other logic
        // For example:
        // return MyApplication.isAppVisible();
        return false; // Replace with your logic
    }

    void playMedia(int StartPosition) {
        musicFiles=listSongs;
        position=StartPosition;
        if (mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            if (musicFiles!=null){
                createMediaPlayer(position);
                mediaPlayer.start();
            }
        }
        else{
            createMediaPlayer(position);
            mediaPlayer.start();
        }

        // Request audio focus when starting playback
        requestAudioFocus();

    }

    void start(){
        if (!isPlaying()) {
            // Start playback
            mediaPlayer.start();
            // Request audio focus when starting playback
            requestAudioFocus();
        }
    }
    boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }
    void stop(){
        mediaPlayer.stop();
        // Abandon audio focus when stopping playback
        abandonAudioFocus();
    }
    void release(){
        mediaPlayer.release();
        // Abandon audio focus when releasing resources
        abandonAudioFocus();
    }
    int getDuration(){
        return mediaPlayer.getDuration();
    }

    void seekTo(int position){
        mediaPlayer.seekTo(position);
    }

    int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                // Handle or log the exception
                e.printStackTrace();
            }
        }
        return 0; // Return default value if mediaPlayer is null or in an invalid state
    }

    void createMediaPlayer(int positionInner){
        position=positionInner;
        uri=Uri.parse(musicFiles.get(position).getPath());
        SharedPreferences.Editor editor=getSharedPreferences(MUSIC_LAST_PLAYED,MODE_PRIVATE).edit();
        editor.putString(MUSIC_FILE,uri.toString());
        editor.putString(ARTIST_NAME,musicFiles.get(position).getArtist());
        editor.putString(SONG_NAME,musicFiles.get(position).getTitle());
        editor.apply();
        mediaPlayer=MediaPlayer.create(getBaseContext(), uri);
    }
    void pause(){
        if (isPlaying()) {
            // Pause playback
            mediaPlayer.pause();
            // Abandon audio focus when pausing playback
            abandonAudioFocus();
        }
    }
    void OnCompleted(){
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (actionPlaying != null) {
            actionPlaying.nextBtnClicked();
        }
    }


    void setCallBack(ActionPlaying actionPlaying){
        this.actionPlaying=actionPlaying;
    }


    @SuppressLint("ForegroundServiceType")
    void showNotification(int playPauseBtn){

        if (musicFiles.isEmpty()) {
            // Handle case where musicFiles is empty
            return;
        }


        Intent intent=new Intent(this,PlayerActivity.class);
        PendingIntent contentIntent=PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent=new Intent(this,NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending=PendingIntent.getBroadcast(this,0,prevIntent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent=new Intent(this,NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending=PendingIntent.getBroadcast(this,0,pauseIntent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent=new Intent(this,NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending=PendingIntent.getBroadcast(this,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        byte[] picture=null;
        try {
            picture=getAlbumArt(musicFiles.get(position).getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Bitmap thumb=null;
        if (picture!=null){
            thumb= BitmapFactory.decodeByteArray(picture,0,picture.length);
        }
        else {
            thumb=BitmapFactory.decodeResource(getResources(),R.drawable.m8);
        }
        Notification notification=new NotificationCompat.Builder(this,CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(musicFiles.get(position).getTitle())
                .setContentText(musicFiles.get(position).getArtist())
                .addAction(R.drawable.ic_skip_previous,"Previous",prevPending)
                .addAction(playPauseBtn,"Pause",pausePending)
                .addAction(R.drawable.ic_skip_next,"Next",nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        startForeground(2,notification);

    }


    private byte[] getAlbumArt(String uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }


    void playPauseBtnnClicked(){
        if(actionPlaying != null){
            actionPlaying.playPauseBtnClicked();
        } else {
            // If no song is selected, attempt to play the last played song
            SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
            String storedUri = preferences.getString(MUSIC_FILE, null);

            if(storedUri != null){
                if(mediaPlayer != null){
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        // Update notification to show play button
                        showNotification(R.drawable.ic_play);
                    } else {
                        mediaPlayer.start();
                        // Update notification to show pause button
                        showNotification(R.drawable.ic_pause);
                    }
                    // Inform the UI or any other components about the playback state
                    if(actionPlaying != null){
                        actionPlaying.playPauseBtnClicked();
                    }
                } else {
                    // Create a new MediaPlayer instance for the last played song
                    mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(storedUri));
                    mediaPlayer.setOnCompletionListener(this); // Set onCompletionListener
                    position = getPositionFromPath(storedUri); // Get position from path

                    // Start the playback
                    mediaPlayer.start();

                    // Update the notification
                    showNotification(R.drawable.ic_pause); // Assuming you want to show pause initially

                    // Inform the UI or any other components about the playback state
                    if(actionPlaying != null){
                        actionPlaying.playPauseBtnClicked();
                    }
                }
            } else {
                Toast.makeText(this, "Select a Song!", Toast.LENGTH_SHORT).show();
            }
        }
    }



    void previousBtnClicked(){
        if(actionPlaying!=null){
            actionPlaying.prevBtnClicked();
        }
    }

    void nextBtnnClicked(){
        if(actionPlaying!=null){
            actionPlaying.nextBtnClicked();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Permanent loss of audio focus
                // Stop playback and release resources
                pause(); // Pause playback in your app
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Temporary loss of audio focus
                // Pause playback
                if (isPlaying()) {
                    pause(); // Pause playback in your app
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // Audio focus gained back
                // Resume playback if paused
                if (!isPlaying()) {
                    start(); // Resume playback in your app
                }
                break;
        }
    }





    private void requestAudioFocus() {
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Audio focus gained, start or resume playback
            if (!isPlaying()) {
                start(); // Start playback if not already playing
            }
        }
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }



}
