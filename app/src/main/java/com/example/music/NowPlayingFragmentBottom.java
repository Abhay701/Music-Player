package com.example.music;

import static android.content.Context.MODE_PRIVATE;
import static com.example.music.MainActivity.ARTIST_NAME;
import static com.example.music.MainActivity.ARTIST_TO_FRAG;
import static com.example.music.MainActivity.PATH_TO_FRAG;
import static com.example.music.MainActivity.SHOW_MINI_PLAYER;
import static com.example.music.MainActivity.SONG_TO_FRAG;
import static com.example.music.MainActivity.musicFiles;
import static com.example.music.PlayerActivity.uri;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class NowPlayingFragmentBottom extends Fragment implements ServiceConnection {

    ImageView nextBtn, albumArt;
    TextView artist, songName;
    FloatingActionButton playPauseBtn;
    View view;
    CardView cardView, cardView2;

    MusicService musicService;


    boolean isServiceBound = false; // Flag to track service binding state

    public static final String MUSIC_LAST_PLAYED="LAST_PLAYED";
    public static final String MUSIC_FILE="STORED_MUSIC";
    public static final String ARTIST_NAME="ARTIST NAME";
    public static final String SONG_NAME="SONG NAME";

    public NowPlayingFragmentBottom() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view=inflater.inflate(R.layout.fragment_now_playing_bottom, container, false);
        artist=view.findViewById(R.id.song_artist_miniPlayer);
        songName=view.findViewById(R.id.song_name_miniPlayer);
        albumArt=view.findViewById(R.id.bottom_album_art);
        nextBtn=view.findViewById(R.id.skip_next_button);
        playPauseBtn=view.findViewById(R.id.play_pause_miniPlayer);
        cardView = view.findViewById(R.id.cardM);
        cardView2=view.findViewById(R.id.cardR);
        cardView.setCardBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
        cardView.getBackground().setAlpha(0);
        cardView2.setCardBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
        cardView2.getBackground().setAlpha(0);



//         Click listener for mini player (cardView2)
        cardView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService != null) {
                    Toast.makeText(getContext(), "Not Working Now..\nPlease wait till the Update!", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getContext(), "Music service is not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });




        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService != null) {
                    if (musicService.musicFiles != null && musicService.position != -1) {
                        musicService.nextBtnnClicked();

                        // Update play/pause button based on current playback state
                        updatePlayPauseButton();

                        if (getActivity() != null) {
                            SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
                            editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
                            editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
                            editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
                            editor.apply();

                            SharedPreferences preferences = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
                            String path = preferences.getString(MUSIC_FILE, null);
                            String artistName = preferences.getString(ARTIST_NAME, null);
                            String song_name = preferences.getString(SONG_NAME, null);
                            if (path != null) {
                                SHOW_MINI_PLAYER = true;
                                PATH_TO_FRAG = path;
                                ARTIST_TO_FRAG = artistName;
                                SONG_TO_FRAG = song_name;
                            } else {
                                SHOW_MINI_PLAYER = false;
                                PATH_TO_FRAG = null;
                                ARTIST_TO_FRAG = null;
                                SONG_TO_FRAG = null;
                            }

                            if (SHOW_MINI_PLAYER) {
                                if (PATH_TO_FRAG != null) {
                                    byte[] art = new byte[0];
                                    try {
                                        art = getAlbumArt(PATH_TO_FRAG);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (art != null) {
                                        Glide.with(getContext()).load(art).into(albumArt);
                                    } else {
                                        Glide.with(getContext()).load(R.drawable.m7).into(albumArt);
                                    }
                                    songName.setText(SONG_TO_FRAG);
                                    artist.setText(ARTIST_TO_FRAG);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Select the Song first", Toast.LENGTH_SHORT).show();
                    }
                } else {
//                    Toast.makeText(getContext(), "Music service is not available", Toast.LENGTH_SHORT).show();
                }
            }
        });






        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(getContext(), "PlayPause", Toast.LENGTH_SHORT).show();
                if (musicService!=null){
                    musicService.playPauseBtnnClicked();
                    if (musicService.isPlaying()){
                        playPauseBtn.setImageResource(R.drawable.ic_pause);
                    }
                    else {
                        playPauseBtn.setImageResource(R.drawable.ic_play);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SHOW_MINI_PLAYER) {
            if (PATH_TO_FRAG != null) {
                byte[] art = new byte[0];
                try {
                    art = getAlbumArt(PATH_TO_FRAG);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (art!=null){
                    Glide.with(getContext()).load(art).into(albumArt);
                }
                else {
                    Glide.with(getContext()).load(R.drawable.m7).into(albumArt);
                }
                songName.setText(SONG_TO_FRAG);
                artist.setText(ARTIST_TO_FRAG);
                Intent intent=new Intent(getContext(), MusicService.class);
                if (getContext()!=null){
                    getContext().bindService(intent,this, Context.BIND_AUTO_CREATE);
                    // Update play/pause button based on current playback state
                    updatePlayPauseButton();
                }
            }
        }
    }

    private void updatePlayPauseButton() {
        if (musicService != null) {
            if (musicService.isPlaying()) {
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_play);
            }
        }
    }




    @Override
    public void onPause() {
        super.onPause();
        if (isServiceBound) {
            // Unbind from MusicService only if it was bound
            if (getContext() != null) {
                getContext().unbindService(this);
                isServiceBound = false; // Reset the flag
            }
        }

    }

    private byte[] getAlbumArt(String uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder= (MusicService.MyBinder) service;
        musicService=binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService=null;
    }
}