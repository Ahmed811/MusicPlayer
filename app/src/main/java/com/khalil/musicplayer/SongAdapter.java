package com.khalil.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.metadata.Metadata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;
    public SongAdapter(Context context, List<Song> songs,ExoPlayer player,ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player=player;
        this.playerView=playerView;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item,parent,false);
        return new  SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Song song=songs.get(position);
        SongViewHolder viewHolder=(SongViewHolder) holder;
        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.durationHolder.setText(getDuration(song.getDuration()));
        viewHolder.sizeHolder.setText(getSize(song.getSize()));
        viewHolder.titleHolder.setText(song.title);

        Uri artwork=song.getArtworkUri();
        if (artwork!=null){
            viewHolder.artworkHolder.setImageURI(artwork);
            if (viewHolder.artworkHolder.getDrawable()==null){
                viewHolder.artworkHolder.setImageResource(R.drawable.iconl);
            }
        }
        viewHolder.itemView.setOnClickListener(view-> {

            context.startService(new Intent(context.getApplicationContext(),PlayerService.class));
            playerView.setVisibility(View.VISIBLE);
            if (!player.isPlaying()){
                player.setMediaItems(getMediaItems(),position,0);
            }else {
                player.pause();
                player.seekTo(position,0);
            }
            player.prepare();
            player.play();
         //   Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();


       if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
           ((MainActivity)context).recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
       }
        });
    }

    private List<MediaItem> getMediaItems() {
        List<MediaItem>mediaItems=new ArrayList<>();
        for (Song song:songs){
            MediaItem mediaItem=new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMediaMetadata(song))
                    .build();
            mediaItems.add(mediaItem);

        }
        return mediaItems;
    }

    private MediaMetadata getMediaMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder{
        ImageView artworkHolder;
        TextView titleHolder,durationHolder,sizeHolder;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            artworkHolder=itemView.findViewById(R.id.artworkView);
            titleHolder=itemView.findViewById(R.id.titleView);
            durationHolder=itemView.findViewById(R.id.durationView);
            sizeHolder=itemView.findViewById(R.id.sizeView);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song>filteredList){
        songs=filteredList;
        notifyDataSetChanged();
    }


    private String getDuration(int totalDuration){
        String totalDurationText;
        int hrs=totalDuration/(1000*60*60);
        int min= (totalDuration%(1000*60*60))/(1000*60);
        int sec= (((totalDuration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;
        if (hrs<1){
            totalDurationText=String.format("%02d:%02d",min,sec);
        }else{
            totalDurationText=String.format("%1d:%02d:%02d",hrs,min,sec);
        }
        return totalDurationText;
    }


    private String getSize(long bytes){
    String hrSize;
    double k=bytes/1024.0;
    double m=(k/1024.0);
    double g=(m/1024.0);
    double t=(g/1024.0);

    //the format
        DecimalFormat dec=new DecimalFormat("0.00");
        if (t>1){
          hrSize=dec.format(t).concat(" TB");
//            hrSize=t+" TB";
        }else if(g>1){
            hrSize=dec.format(g).concat(" GB");
//            hrSize=g+" GB";

        }else if(m>1){
            hrSize=dec.format(m).concat(" MB");
//            hrSize=m+" MB";
        }else if(k>1){
            hrSize=dec.format(k).concat(" KB");
//            hrSize=k+" KB";
        }else {
            hrSize=dec.format(g).concat(" Bytes");
//            hrSize=g+" Bytes";
        }
        return hrSize;
    }
}
