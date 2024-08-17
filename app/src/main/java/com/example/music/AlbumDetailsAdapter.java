package com.example.music;

import android.annotation.SuppressLint;
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

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> albumFiles;


    private int currentPosition = -1; // Variable to hold the currently playing position

//    private View lastSelectedView; // Keep track of the last selected view
    private int selectedPosition = -1;

    View view;

    public AlbumDetailsAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view= LayoutInflater.from(mContext).inflate(R.layout.music_items,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {

//        holder.album_name.setText(albumFiles.get(position).getTitle());


        String title = albumFiles.get(position).getTitle();
        holder.album_name.setText(truncateTitle(title));


        // Set text color based on whether it's the currently playing song
        if (position == currentPosition) {
            holder.album_name.setTextColor(ContextCompat.getColor(mContext, R.color.selectedTextColor));
        } else {
            holder.album_name.setTextColor(Color.WHITE); // Set default color
        }


        byte[] image = new byte[0];
        try {
            image = getAlbumArt(albumFiles.get(position).getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (image != null) {
            Glide.with(mContext).asBitmap().load(image).into(holder.album_image);
        } else {
            Glide.with(mContext).load(R.drawable.m8).into(holder.album_image);
        }



//         Set red dot visibility based on selected position
        holder.red_dot.setVisibility(selectedPosition == position ? View.VISIBLE : View.INVISIBLE);


        // Remove shadow from CardView
        holder.cardV.setCardBackgroundColor(mContext.getResources().getColor(android.R.color.transparent));
        holder.cardV.getBackground().setAlpha(0);


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
                currentPosition = position;
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
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(albumFiles.get(position).getId()));
        File file = new File(albumFiles.get(position).getPath());
        boolean deleted = false; // Doesn't Delete the Music
//        boolean deleted = file.delete(); //Delete the Music in older Android Versions
        if (deleted) {
            mContext.getContentResolver().delete(contentUri, null, null);
            albumFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, albumFiles.size());
            Snackbar.make(v, "File Deleted! ", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(v, "File can't be Deleted! ", Snackbar.LENGTH_LONG).show();
        }

    }


    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{


        View red_dot;
        ImageView album_image;
        TextView album_name;
        ImageView menuMore;
        CardView cardV;
        public MyHolder(View itemView){
            super(itemView);
            album_image=itemView.findViewById(R.id.music_img);
            album_name=itemView.findViewById(R.id.music_file_name);
            menuMore = itemView.findViewById(R.id.menuMore);
            red_dot = itemView.findViewById(R.id.red_dot);
            cardV=itemView.findViewById(R.id.card);
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
        Intent intent=new Intent(mContext,PlayerActivity.class);
        intent.putExtra("sender","albumDetails");
        intent.putExtra("position",position);
        mContext.startActivity(intent);
    }


}