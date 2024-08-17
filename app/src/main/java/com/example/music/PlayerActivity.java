package com.example.music;

import static com.example.music.AlbumDetailsAdapter.albumFiles;
import static com.example.music.ApplicationClass.ACTION_NEXT;
import static com.example.music.ApplicationClass.ACTION_PLAY;
import static com.example.music.ApplicationClass.ACTION_PREVIOUS;
import static com.example.music.ApplicationClass.CHANNEL_ID_2;
import static com.example.music.MainActivity.musicFiles;
import static com.example.music.MainActivity.repeatBoolean;
import static com.example.music.MainActivity.shuffleBoolean;
import static com.example.music.MusicAdapter.mFiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BitmapCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {


    private TextView song_name,artist_name,duration_played,duration_total;
    private ImageView cover_art,nextBtn,prevBtn,backBtn,shuffleBtn,repeatBtn;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    private int position =-1;
    public static ArrayList<MusicFiles> listSongs=new ArrayList<>();
    public static Uri uri;
    // public static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread,prevThread,nextThread;
    MusicService musicService;

    MediaSessionCompat mediaSessionCompat;

    private boolean serviceBound = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFulScreen();
        setContentView(R.layout.activity_player);
        mediaSessionCompat=new MediaSessionCompat(getBaseContext(),"My Audio");
        initViews();
        getIntenMethod();


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService!=null && fromUser){
                    musicService.seekTo(progress*1000);
                    duration_played.setText(formattedTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService!=null){
                    int mCurrentPosition = musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this,1000);
            }
        });

        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffleBoolean) {
                    shuffleBoolean = false;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
                    // If shuffle is turned off and repeat is on, turn off repeat
                    if (repeatBoolean) {
                        repeatBoolean = false;
                        repeatBtn.setImageResource(R.drawable.ic_repeat_off);
                    }
                } else {
                    shuffleBoolean = true;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                    // If shuffle is turned on, turn off repeat
                    if (repeatBoolean) {
                        repeatBoolean = false;
                        repeatBtn.setImageResource(R.drawable.ic_repeat_off);
                    }
                }
            }
        });

        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatBoolean) {
                    repeatBoolean = false;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_off);
                    // If repeat is turned off and shuffle is on, turn off shuffle
                    if (shuffleBoolean) {
                        shuffleBoolean = false;
                        shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
                    }
                } else {
                    repeatBoolean = true;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_on);
                    // If repeat is turned on, turn off shuffle
                    if (shuffleBoolean) {
                        shuffleBoolean = false;
                        shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
                    }
                }
            }
        });

        // Set the OnCompletionListener to trigger next song playback
//        musicService.OnCompleted();

        // Update UI elements based on the current song
        updateUI();

    }

    private void setFulScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }



    private void updateUI() {
            // Update UI elements like song name, artist, cover art, etc.
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            // Load cover art and update background colors using metaData method
            metaData(Uri.parse(listSongs.get(position).getPath()));
            // Update play/pause button based on playback state
            updateShuffleRepeatButtons();
    }



    @Override
    protected void onResume() {
        super.onResume();
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) {
            unbindService(this);
            serviceBound = false;
        }
    }

    private void prevThreadBtn() {

        prevThread=new Thread(){
            public void run(){
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        musicService.stop();
        musicService.release();
        if(shuffleBoolean && !repeatBoolean){
            position = getRandom(listSongs.size()-1);
        }
        else if (!shuffleBoolean && !repeatBoolean){
            position = (position - 1) < 0 ? (listSongs.size() - 1) : (position - 1);
        }
        uri = Uri.parse(listSongs.get(position).getPath());

//        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        musicService.createMediaPlayer(position);

        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());

        seekBar.setMax(musicService.getDuration() / 1000);

        musicService.showNotification(R.drawable.ic_pause);

        musicService.start();
        playPauseBtn.setImageResource(R.drawable.ic_pause);

        // Update the seek bar
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService!=null){
                    int mCurrentPosition = musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                }
                handler.postDelayed(this,1000);
            }
        });


        // Set OnCompletionListener for MediaPlayer
        musicService.OnCompleted();

    }

//    private void prevBtnClicked() {
//        if (mediaPlayer.isPlaying()){
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            position=((position-1)<0?(listSongs.size() -1) : (position-1));
//            uri=Uri.parse(listSongs.get(position).getPath());
//            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
//            metaData(uri);
//            song_name.setText(listSongs.get(position).getTitle());
//            artist_name.setText(listSongs.get(position).getArtist());
//
//            seekBar.setMax(mediaPlayer.getDuration()/1000);
//
//
//            PlayerActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (mediaPlayer!=null){
//                        int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
//                        seekBar.setProgress(mCurrentPosition);
//                    }
//                    handler.postDelayed(this,1000);
//                }
//            });
//            playPauseBtn.setImageResource(R.drawable.ic_pause);
//            mediaPlayer.start();
//
//        }
//        else {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            position=((position-1)<0?(listSongs.size() -1) : (position-1));
//            uri=Uri.parse(listSongs.get(position).getPath());
//            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
//            metaData(uri);
//            song_name.setText(listSongs.get(position).getTitle());
//            artist_name.setText(listSongs.get(position).getArtist());
//
//            seekBar.setMax(mediaPlayer.getDuration()/1000);
//
//
//            PlayerActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (mediaPlayer!=null){
//                        int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
//                        seekBar.setProgress(mCurrentPosition);
//                    }
//                    handler.postDelayed(this,1000);
//                }
//            });
//            playPauseBtn.setImageResource(R.drawable.ic_play);
//        }
//
//    }

    private void nextThreadBtn() {
        nextThread=new Thread(){
            public void run(){
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();

    }


    public void nextBtnClicked() {
        musicService.stop();
        musicService.release();
        if(shuffleBoolean && !repeatBoolean){
            position = getRandom(listSongs.size()-1);
        }
        else if (!shuffleBoolean && !repeatBoolean){
            position = ((position + 1) % listSongs.size());
        }
        // else position will be position
        uri = Uri.parse(listSongs.get(position).getPath());
//        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        musicService.createMediaPlayer(position);

        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());

        seekBar.setMax(musicService.getDuration() / 1000);

        musicService.showNotification(R.drawable.ic_pause);

        musicService.start();
        playPauseBtn.setImageResource(R.drawable.ic_pause);

        // Update the seek bar
        PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService!=null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });

        // Set OnCompletionListener for MediaPlayer
        musicService.OnCompleted();

    }

    private int getRandom(int i) {
        Random random=new Random();

        return random.nextInt(i+1);

    }


//    private void nextBtnClicked() {
//
//        if (mediaPlayer.isPlaying()){
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            position=((position+1)%listSongs.size());
//            uri=Uri.parse(listSongs.get(position).getPath());
//            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
//            metaData(uri);
//            song_name.setText(listSongs.get(position).getTitle());
//            artist_name.setText(listSongs.get(position).getArtist());
//
//            seekBar.setMax(mediaPlayer.getDuration()/1000);
//
//
//            PlayerActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (mediaPlayer!=null){
//                        int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
//                        seekBar.setProgress(mCurrentPosition);
//                    }
//                    handler.postDelayed(this,1000);
//                }
//            });
//            playPauseBtn.setImageResource(R.drawable.ic_pause);
//            mediaPlayer.start();
//
//        }
//        else {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            position=((position+1)%listSongs.size());
//            uri=Uri.parse(listSongs.get(position).getPath());
//            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
//            metaData(uri);
//            song_name.setText(listSongs.get(position).getTitle());
//            artist_name.setText(listSongs.get(position).getArtist());
//
//            seekBar.setMax(mediaPlayer.getDuration()/1000);
//
//
//            PlayerActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (mediaPlayer!=null){
//                        int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
//                        seekBar.setProgress(mCurrentPosition);
//                    }
//                    handler.postDelayed(this,1000);
//                }
//            });
//            playPauseBtn.setImageResource(R.drawable.ic_play);
//        }
//
//    }

    private void playThreadBtn() {
        playThread=new Thread(){
            public void run(){
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if (musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration()/1000);


            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService!=null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });

        }
        else {
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.showNotification(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration()/1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService!=null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                        duration_played.setText(formattedTime(mCurrentPosition));
                    }
                    handler.postDelayed(this,1000);
                }
            });

        }


    }

    private String formattedTime(int mCurrentPosition) {

        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition%60);
        String minutes = String.valueOf(mCurrentPosition/60);
        totalout=minutes+":"+seconds;
        totalNew=minutes+":"+0+seconds;
        if (seconds.length()==1){
            return totalNew;
        }
        else {
            return totalout;
        }
    }

    private void getIntenMethod() {
        position=getIntent().getIntExtra("position",-1);
        String sender=getIntent().getStringExtra("sender");
        if (sender!=null && sender.equals("albumDetails")){
            listSongs=albumFiles;
        }
        else {
            listSongs=mFiles;
        }

        if (listSongs!=null){
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            uri=Uri.parse(listSongs.get(position).getPath());
        }

        Intent intent=new Intent(this,MusicService.class);
        intent.putExtra("servicePosition",position);
        startService(intent);
        // Set OnCompletionListener for MediaPlayer
//        musicService.OnCompleted();

    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name =findViewById(R.id.song_artist);
                duration_played=findViewById(R.id.durationPlayed);
        duration_total=findViewById(R.id.durationTotal);
                cover_art=findViewById(R.id.cover_art);
        nextBtn=findViewById(R.id.id_next);
                prevBtn=findViewById(R.id.id_prev);
        backBtn=findViewById(R.id.back_btn);
                shuffleBtn=findViewById(R.id.id_shuffle);
        repeatBtn=findViewById(R.id.id_repeat);
                playPauseBtn=findViewById(R.id.play_pause);
        seekBar=findViewById(R.id.seekBar);

    }

    private void metaData(Uri uri){
        if (!isDestroyed()) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(uri.toString());
            int durationTotal = Integer.parseInt(listSongs.get(position).getDuration())/1000;
            duration_total.setText(formattedTime(durationTotal));
            byte[] art = retriever.getEmbeddedPicture();
            Bitmap bitmap;
            if (art!=null){
                bitmap = BitmapFactory.decodeByteArray(art,0,art.length);
                ImageAnimation(this,cover_art,bitmap);
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(@Nullable Palette palette) {
                        Palette.Swatch swatch=palette.getDominantSwatch();
                        if (swatch!=null){
                            ImageView gredient = findViewById(R.id.imageViewGredient);
                            RelativeLayout mContainer = findViewById(R.id.mContainer);
                            gredient.setBackgroundResource(R.drawable.gredient_bg);
                            mContainer.setBackgroundResource(R.drawable.main_bg);
                            GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP
                            ,new int[]{swatch.getRgb(),0x0000000});
                            gredient.setBackground(gradientDrawable);

                            GradientDrawable gradientDrawableBg=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP
                                    ,new int[]{swatch.getRgb(),swatch.getRgb()});
                            mContainer.setBackground(gradientDrawableBg);
                            song_name.setTextColor(swatch.getTitleTextColor());
                            artist_name.setTextColor(swatch.getBodyTextColor());
                        }

                        else {

                            ImageView gredient = findViewById(R.id.imageViewGredient);
                            RelativeLayout mContainer = findViewById(R.id.mContainer);
                            gredient.setBackgroundResource(R.drawable.gredient_bg);
                            mContainer.setBackgroundResource(R.drawable.main_bg);
                            GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP
                                    ,new int[]{0xff000000,0x0000000});
                            gredient.setBackground(gradientDrawable);

                            GradientDrawable gradientDrawableBg=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP
                                    ,new int[]{0xff000000,0xff000000});
                            mContainer.setBackground(gradientDrawableBg);
                            song_name.setTextColor(Color.WHITE);
                            artist_name.setTextColor(Color.DKGRAY);
                        }

                    }

                });
            }
            else {
                Glide.with(this).asBitmap().load(R.drawable.m7).into(cover_art);
                ImageView gredient = findViewById(R.id.imageViewGredient);
                RelativeLayout mContainer = findViewById(R.id.mContainer);
                gredient.setBackgroundResource(R.drawable.gredient_bg);
                mContainer.setBackgroundResource(R.drawable.main_bg);
                song_name.setTextColor(Color.WHITE);
                artist_name.setTextColor(Color.DKGRAY);
            }
        }
    }

    public void ImageAnimation(Context context,ImageView imageView,Bitmap bitmap){
        Animation animOut = AnimationUtils.loadAnimation(context,android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context,android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        imageView.startAnimation(animOut);

    }


    // Update shuffle and repeat buttons based on shuffleBoolean and repeatBoolean
    private void updateShuffleRepeatButtons() {
        if (shuffleBoolean) {
            shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
        } else {
            shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
        }
        if (repeatBoolean) {
            repeatBtn.setImageResource(R.drawable.ic_repeat_on);
        } else {
            repeatBtn.setImageResource(R.drawable.ic_repeat_off);
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder= (MusicService.MyBinder) service;
        musicService=myBinder.getService();
        musicService.setCallBack(this);
//        Toast.makeText(this,"Connected"+musicService,Toast.LENGTH_SHORT).show();
        seekBar.setMax(musicService.getDuration()/1000);
        metaData(uri);

        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.OnCompleted();
        musicService.showNotification(R.drawable.ic_pause);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService=null;
    }

}
