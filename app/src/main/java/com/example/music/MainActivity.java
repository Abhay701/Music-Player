package com.example.music;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    static boolean shuffleBoolean = false, repeatBoolean = false;
    static ArrayList<MusicFiles> albums=new ArrayList<>();
    private String MY_SORT_PREF = "SortOrder";
    public static final String MUSIC_LAST_PLAYED="LAST_PLAYED";
    public static final String MUSIC_FILE="STORED_MUSIC";
    public static boolean SHOW_MINI_PLAYER=false;
    public static String PATH_TO_FRAG=null;
    public static String ARTIST_TO_FRAG=null;
    public static String SONG_TO_FRAG=null;

    public static final String ARTIST_NAME="ARTIST NAME";
    public static final String SONG_NAME="SONG NAME";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
    }

    private void permission() {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        REQUEST_CODE);

            } else {
                // Permission has already been granted
//            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                musicFiles=getAllAudio(this);
                initViewPager();
//                loadSongs();
            }
        }
        else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE);

            } else {
                // Permission has already been granted
//            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                musicFiles=getAllAudio(this);
                initViewPager();
//                loadSongs();
            }
        }

    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CODE){
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//
//                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
//                initViewPager();
//            }
//            else {
//                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
//                        ,REQUEST_CODE);
//            }
//        }
//    }


    private boolean permissionRequested = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                musicFiles=getAllAudio(this);
                initViewPager();
            }
            else {
                if (!permissionRequested) {
                    permissionRequested = true;
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
                } else {
                    Toast.makeText(this, "Oops Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private void initViewPager() {
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragments(new SongsFragment(), "Songs");
        viewPagerAdapter.addFragments(new AlbumFragment(), "Albums");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

    }


    public static class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }


        void addFragments(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }


        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    public ArrayList<MusicFiles> getAllAudio(Context context){


        SharedPreferences preferences=getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE);
        String sortOrder=preferences.getString("sorting","sortByName");


        ArrayList<String> duplicate = new ArrayList<>();
        albums.clear();

        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();

        String order=null;

        Uri uri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        switch (sortOrder){
            case "sortByName":
                order=MediaStore.MediaColumns.DISPLAY_NAME+" ASC";
                break;
            case "sortByDate":
                order=MediaStore.MediaColumns.DATE_ADDED+" DESC";
                break;
            case "sortBySize":
                order=MediaStore.MediaColumns.SIZE+" DESC";
                break;
        }

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };
        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,order);
        if (cursor!=null){
            while (cursor.moveToNext()){
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);
                String id = cursor.getString(5);

                MusicFiles musicFiles=new MusicFiles(path,title,artist,album,duration,id);
                Log.e("Path: "+path,"Album: "+album);
                tempAudioList.add(musicFiles);

                if (!duplicate.contains(album)){
                    albums.add(musicFiles);
                    duplicate.add(album);
                }

            }
            cursor.close();
        }
        return tempAudioList;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search,menu);
        MenuItem menuItem=menu.findItem(R.id.search_option);
        SearchView searchView=(SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);


        MenuItem byNameItem = menu.findItem(R.id.by_name);
        MenuItem byDateItem = menu.findItem(R.id.by_date);
        MenuItem bySizeItem = menu.findItem(R.id.by_size);

        // Get the current sorting preference
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");

        // Check the appropriate menu item based on sorting preference
        switch (sortOrder) {
            case "sortByName":
                byNameItem.setChecked(true);
                break;
            case "sortByDate":
                byDateItem.setChecked(true);
                break;
            case "sortBySize":
                bySizeItem.setChecked(true);
                break;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

//    @Override
//    public boolean onQueryTextChange(String newText) {
//        String userInput=newText.toLowerCase();
//        ArrayList<MusicFiles> myFiles=new ArrayList<>();
//        for(MusicFiles song : musicFiles){
//            if (song.getTitle().toLowerCase().contains(userInput)){
//                myFiles.add(song);
//            }
//        }
//        SongsFragment.musicAdapter.updateList(myFiles);
//        return true;
//    }


    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<MusicFiles> myFiles = new ArrayList<>();

        // Check if musicFiles is null
        if (musicFiles != null) {
            for (MusicFiles song : musicFiles) {
                if (song.getTitle().toLowerCase().contains(userInput)) {
                    myFiles.add(song);
                }
            }
        } else {
            Log.e("MainActivity", "musicFiles is null");
            // Handle the case where musicFiles is null, maybe show a message to user
            Toast.makeText(this, "No songs available", Toast.LENGTH_SHORT).show();
        }

        // Ensure that your adapter and fragment are properly updated
        if (SongsFragment.musicAdapter != null) {
            SongsFragment.musicAdapter.updateList(myFiles);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor=getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE).edit();
        if (item.getItemId() == R.id.by_name) {
            editor.putString("sorting", "sortByName");
            editor.apply();
            this.recreate();
            return true;
        } else if (item.getItemId() == R.id.by_date) {
            editor.putString("sorting", "sortByDate");
            editor.apply();
            this.recreate();
            return true;
        } else if (item.getItemId() == R.id.by_size) {
            editor.putString("sorting", "sortBySize");
            editor.apply();
            this.recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences=getSharedPreferences(MUSIC_LAST_PLAYED,MODE_PRIVATE);
        String path=preferences.getString(MUSIC_FILE,null);
        String artist=preferences.getString(ARTIST_NAME,null);
        String song_name= preferences.getString(SONG_NAME,null);
        if (path!=null){
            SHOW_MINI_PLAYER=true;
            PATH_TO_FRAG=path;
            ARTIST_TO_FRAG=artist;
            SONG_TO_FRAG=song_name;
        }
        else {
            SHOW_MINI_PLAYER=false;
            PATH_TO_FRAG=null;
            ARTIST_TO_FRAG=null;
            SONG_TO_FRAG=null;
        }
    }
}
