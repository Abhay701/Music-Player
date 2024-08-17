//package com.example.music;
//
//import android.annotation.SuppressLint;
//import android.content.ContentUris;
//import android.content.Context;
//import android.content.Intent;
//import android.media.MediaMetadataRetriever;
//import android.net.Uri;
//import android.provider.MediaStore;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.PopupMenu;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.google.android.material.snackbar.Snackbar;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//
//public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
//
//    private Context mContext;
//    private ArrayList<MusicFiles> mFiles;
//
//    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
//        this.mFiles = mFiles;
//        this.mContext = mContext;
//    }
//
//    @NonNull
//    @Override
//    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
//        return new MyViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
//        String title = mFiles.get(position).getTitle();
//        holder.file_name.setText(truncateTitle(title));
//        byte[] image = new byte[0];
//        try {
//            image = getAlbumArt(mFiles.get(position).getPath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (image != null) {
//            Glide.with(mContext).asBitmap().load(image).into(holder.album_art);
//        } else {
//            Glide.with(mContext).load(R.drawable.m8).into(holder.album_art);
//        }
//
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent=new Intent(mContext,PlayerActivity.class);
//                intent.putExtra("position",position);
//
//                mContext.startActivity(intent);
//            }
//        });
//
//        holder.menuMore.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PopupMenu popupMenu = new PopupMenu(mContext,v);
//                popupMenu.getMenuInflater().inflate(R.menu.popup,popupMenu.getMenu());
//                popupMenu.show();
//
//                popupMenu.setOnMenuItemClickListener((item) -> {
//                    if (item.getItemId() == R.id.delete) {
//                        Toast.makeText(mContext, "Delete Clicked!", Toast.LENGTH_SHORT).show();
//                        deleteFile(position, v);
//                        return true;
//                    }
//                    return false;
//
//                });
//            }
//        });
//
//    }
//
//    private void deleteFile(int position, View v){
//        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,Long.parseLong(mFiles.get(position).getId()));
//        File file=new File(mFiles.get(position).getPath());
//        boolean deleted = false; // Doesn't Delete the Music
////        boolean deleted = file.delete(); //Delete the Music in older Android Versions
//        if (deleted) {
//            mContext.getContentResolver().delete(contentUri,null,null);
//            mFiles.remove(position);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, mFiles.size());
//            Snackbar.make(v, "File Deleted! ", Snackbar.LENGTH_LONG).show();
//        }
//        else {
//            Snackbar.make(v, "File can't be Deleted! ", Snackbar.LENGTH_LONG).show();
//        }
//
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return mFiles.size();
//    }
//
//    public class MyViewHolder extends RecyclerView.ViewHolder {
//
//        TextView file_name;
//        ImageView album_art,menuMore;
//
//
//        public MyViewHolder(@NonNull View itemView) {
//            super(itemView);
//            file_name = itemView.findViewById(R.id.music_file_name);
//            album_art = itemView.findViewById(R.id.music_img);
//            menuMore = itemView.findViewById(R.id.menuMore);
//        }
//    }
//
//    private byte[] getAlbumArt(String uri) throws IOException {
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        retriever.setDataSource(uri);
//        byte[] art = retriever.getEmbeddedPicture();
//        retriever.release();
//        return art;
//    }
//
//    private String truncateTitle(String title) {
//        if (title.length() > 34) {
//            return title.substring(0, 34) + "...";
//        } else {
//            return title;
//        }
//    }
//}


package com.example.music;

import static java.security.AccessController.getContext;

import android.annotation.SuppressLint;
import android.app.MediaRouteButton;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> mFiles;
    private int currentPosition = -1; // Variable to hold the currently playing position

    private View lastSelectedView; // Keep track of the last selected view
    private int selectedPosition = -1;
    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
        this.mFiles = mFiles;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);

    }



    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {


        MusicFiles musicFile = mFiles.get(position);
        // Bind data to views
//        holder.file_name.setText(truncateTitle(musicFile.getTitle()));

        String title = mFiles.get(position).getTitle();
        holder.file_name.setText(truncateTitle(title));


        // Set text color based on whether it's the currently playing song
        if (position == currentPosition) {
            holder.file_name.setTextColor(ContextCompat.getColor(mContext, R.color.selectedTextColor));
        } else {
            holder.file_name.setTextColor(Color.WHITE); // Set default color
        }

        byte[] image = new byte[0];
        try {
            image = getAlbumArt(mFiles.get(position).getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (image != null) {
            Glide.with(mContext).asBitmap().load(image).into(holder.album_art);
        } else {
            Glide.with(mContext).load(R.drawable.m8).into(holder.album_art);
        }



//         Set red dot visibility based on selected position
        holder.red_dot.setVisibility(selectedPosition == position ? View.VISIBLE : View.INVISIBLE);
        // Update the red dot color based on selection
        if (lastSelectedView != null) {
            View redDot = lastSelectedView.findViewById(R.id.red_dot);
            if (redDot != null) {
                redDot.setBackgroundResource(R.drawable.green_dot);
            }
        }

        // Remove shadow from CardView
        holder.card.setCardBackgroundColor(mContext.getResources().getColor(android.R.color.transparent));
        holder.card.getBackground().setAlpha(0);


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Update selected position
                int previousSelectedPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // Notify adapter of item changes
                notifyItemChanged(previousSelectedPosition);
                notifyItemChanged(selectedPosition);

                // Update the currently playing position and notify data change
                int previousPosition = currentPosition;
                currentPosition = position;
                notifyItemChanged(previousPosition);
                notifyItemChanged(currentPosition);

                // Start PlayerActivity with the selected position
                startPlayerActivity(position);


            }
        });

        holder.menuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, v);
                popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                popupMenu.show();

                popupMenu.setOnMenuItemClickListener((item) -> {
                    if (item.getItemId() == R.id.delete) {
                        Toast.makeText(mContext, "Delete Clicked!", Toast.LENGTH_SHORT).show();
                        deleteFile(position, v);
                        return true;
                    }
                    return false;
                });
            }
        });

    }

    private void deleteFile(int position, View v) {
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(mFiles.get(position).getId()));
        File file = new File(mFiles.get(position).getPath());
        boolean deleted = false; // Doesn't Delete the Music
//        boolean deleted = file.delete(); //Delete the Music in older Android Versions
        if (deleted) {
            mContext.getContentResolver().delete(contentUri, null, null);
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
            Snackbar.make(v, "File Deleted! ", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(v, "File can't be Deleted! ", Snackbar.LENGTH_LONG).show();
        }

    }


    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        View red_dot;
        TextView file_name;
        ImageView album_art, menuMore;
        CardView card;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
            red_dot = itemView.findViewById(R.id.red_dot);
            card=itemView.findViewById(R.id.card);
        }
    }

    private byte[] getAlbumArt(String uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    private String truncateTitle(String title) {
        if (title.length() > 34) {
            return title.substring(0, 34) + "...";
        } else {
            return title;
        }
    }

    private void startPlayerActivity(int position) {
        Intent intent = new Intent(mContext, PlayerActivity.class);
        intent.putExtra("position", position);
        mContext.startActivity(intent);
    }



    void updateList(ArrayList<MusicFiles> musicFilesArrayList){
        mFiles=new ArrayList<>();
        mFiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
    }

}

