package com.khalil.musicplayer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.splashscreen.SplashScreen;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.transition.Visibility;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.khalil.musicplayer.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

SongAdapter songAdapter;
List<Song>allSongs=new ArrayList<>();
ActivityResultLauncher<String>storagePermissionLauncher;
final String storagePermission= Manifest.permission.READ_EXTERNAL_STORAGE;
ActivityResultLauncher<String>recordAudioPermissionLauncher;
final String recordAudioPermission= Manifest.permission.READ_EXTERNAL_STORAGE;
ExoPlayer exoPlayer;
int defaultStatusColor;
int repeatMode=1;  //repeat all=1,repeat one=2,shuffle=3
    boolean isBound=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        defaultStatusColor=getWindow().getStatusBarColor();
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);
       // exoPlayer=new ExoPlayer.Builder(this).build();
        //request permission
        storagePermissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
            if(granted){
                fetchSongs();

            }else {
                userResponseRecordAudioPermission();
            }
        });

        recordAudioPermissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
            if(granted&&exoPlayer.isPlaying()){
                activateAudioVisualizer();

            }else {
                userResponse();
            }
        });
        

       // recordAudioPermissionLauncher.launch(recordAudioPermission);
        
       // playerControls();
       //bind to the player service
        startBindService();
    }

    private void startBindService() {
        Intent playerServiceIntent=new Intent(this,PlayerService.class);
        bindService(playerServiceIntent,playerServiceConnection, Context.BIND_AUTO_CREATE);

    }

    ServiceConnection playerServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
           PlayerService.ServiceBinder binder=(PlayerService.ServiceBinder) iBinder;
           exoPlayer=binder.getPlayerService().player;
           isBound=true;
            storagePermissionLauncher.launch(storagePermission);
            playerControls();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void playerControls() {
        //song name marquee
        binding.homeSongNameView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        binding.homeSongNameView.setSelected(true);
        binding.homeSongNameView.setSingleLine(true);
        binding.playerViewLayout.songNameView.setSelected(true);
        //exit player view
        binding.playerViewLayout.playerCloseBtn.setOnClickListener(view->exitPlayerView());
        binding.playerViewLayout.playListBtn.setOnClickListener(view->exitPlayerView());
        //open player view in home activity
        binding.homeControllerWrapper.setOnClickListener(v -> openPlayerView());
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                //show title
                assert mediaItem != null;
                binding.playerViewLayout.songNameView.setText("         "+mediaItem.mediaMetadata.title+"             ");
                binding.homeSongNameView.setText(mediaItem.mediaMetadata.title);
         //       Toast.makeText(getBaseContext(),exoPlayer.getDuration()+"",Toast.LENGTH_LONG).show();
               binding.playerViewLayout.progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                binding.playerViewLayout.seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                binding.playerViewLayout.seekBar.setMax((int) exoPlayer.getDuration());
                binding.playerViewLayout.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline,0,0,0);
                binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
                showCurrentArtWork();

                updatePlayerPositionProgress();

                binding.playerViewLayout.artworkViewImg.setAnimation(loadRotation());

                activateAudioVisualizer();

                updatePlayerColors();

                if(!exoPlayer.isPlaying()){
                    exoPlayer.isPlaying();
                }
            }
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState==exoPlayer.STATE_READY){
                    binding.playerViewLayout.songNameView.setText("         "+exoPlayer.getCurrentMediaItem().mediaMetadata.title+"             ");
                    binding.homeSongNameView.setText(""+exoPlayer.getCurrentMediaItem().mediaMetadata.title+"");
                    binding.playerViewLayout.progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    binding.playerViewLayout.seekBar.setMax((int) exoPlayer.getDuration());
                    binding.playerViewLayout.seekBar.setProgress((int) exoPlayer.getCurrentPosition());

                    binding.playerViewLayout.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline,0,0,0);
                    binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
                    showCurrentArtWork();

                    updatePlayerPositionProgress();

                    binding.playerViewLayout.artworkViewImg.setAnimation(loadRotation());

                    activateAudioVisualizer();

                    updatePlayerColors();
                }else {
                    binding.playerViewLayout.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle_outline,0,0,0);

                    binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
                }
            }
        }

        );
        
        binding.homeSkipNextBtn.setOnClickListener(v -> skipToNextSong());
        binding.playerViewLayout.skipNextBtn.setOnClickListener(v -> skipToNextSong());
        binding.homeSkipPreviosBtn.setOnClickListener(v -> skipToPreviousSong());
        binding.playerViewLayout.skipPreviousBtn.setOnClickListener(v -> skipToPreviousSong());

        binding.homePlayPauseBtn.setOnClickListener(v ->  playOrpausPlayer());
        binding.playerViewLayout.playPauseBtn.setOnClickListener(v ->  playOrpausPlayer());

        binding.playerViewLayout.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue=seekBar.getProgress();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(exoPlayer.getPlaybackState()==Player.STATE_READY){
                    seekBar.setProgress(progressValue);
                    binding.playerViewLayout.progressView.setText(getReadableTime(progressValue));
                    exoPlayer.seekTo(progressValue);
                }

            }
        });

        binding.playerViewLayout.repeatMoodBtn.setOnClickListener(v -> {
            if (repeatMode==1){
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                repeatMode=2;
                binding.playerViewLayout.repeatMoodBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);

            }else if (repeatMode==2){
                exoPlayer.setShuffleModeEnabled(true);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                repeatMode=3;
                binding.playerViewLayout.repeatMoodBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);
            }else if (repeatMode==3){
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                exoPlayer.setShuffleModeEnabled(false);
                repeatMode=1;
                binding.playerViewLayout.repeatMoodBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat,0,0,0);

            }
            updatePlayerColors();
        });

    }

    private void playOrpausPlayer() {
        if(exoPlayer.isPlaying()){
            exoPlayer.pause();
            binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
            binding.playerViewLayout.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
            binding.playerViewLayout.artworkViewImg.clearAnimation();
        }else {
            exoPlayer.play();
            binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline,0,0,0);
            binding.playerViewLayout.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
            binding.playerViewLayout.artworkViewImg.setAnimation(loadRotation());
        }
        updatePlayerColors();
    }


    private void skipToNextSong() {
        if(exoPlayer.hasNextMediaItem()){
            exoPlayer.seekToNext();
        }
    }
    private void skipToPreviousSong() {
        if(exoPlayer.hasPreviousMediaItem()){
            exoPlayer.seekToPrevious();
        }
    }

    private void updatePlayerColors() {
        if(binding.playerViewLayout.playerView.getVisibility()==View.GONE)
            return;
        BitmapDrawable bitmapDrawable=(BitmapDrawable) binding.playerViewLayout.artworkViewImg.getDrawable();
        if(bitmapDrawable==null){
            bitmapDrawable=(BitmapDrawable) ContextCompat.getDrawable(this,R.drawable.artwork);
        }
        assert bitmapDrawable != null;
        Bitmap bmp=bitmapDrawable.getBitmap();
        binding.playerViewLayout.backgroundBlurImageView.setImageBitmap(bmp);
        binding.playerViewLayout.backgroundBlurImageView.setBlur(4);

        Palette.from(bmp).generate(palette -> {
            if(palette!=null){
                Palette.Swatch swatch=palette.getDarkMutedSwatch();
                if(swatch==null){
                    swatch=palette.getMutedSwatch();
                    if(swatch==null){
                        swatch=palette.getDominantSwatch();
                    }
                }
                assert swatch!=null;
                int titleTextColor=swatch.getTitleTextColor();
                int bodyTextColor=swatch.getBodyTextColor();
                int rgbColor=swatch.getRgb();
                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);
                binding.playerViewLayout.songNameView.setTextColor(titleTextColor);
                binding.playerViewLayout.playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                binding.playerViewLayout.progressView.setTextColor(bodyTextColor);
                binding.playerViewLayout.durationView.setTextColor(bodyTextColor);


                binding.playerViewLayout.repeatMoodBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                binding.playerViewLayout.skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                binding.playerViewLayout.skipNextBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                binding.playerViewLayout.playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                binding.playerViewLayout.playListBtn.getCompoundDrawables()[0].setTint(bodyTextColor);







            }
        });
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation=new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(exoPlayer.isPlaying()){
                    binding.playerViewLayout.progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    binding.playerViewLayout.seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                }
                updatePlayerPositionProgress();
            }
        }, 1000);
    }

    private void showCurrentArtWork() {
        binding.playerViewLayout.artworkViewImg.setImageURI(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.artworkUri);
        if(binding.playerViewLayout.artworkViewImg.getDrawable()==null){
            binding.playerViewLayout.artworkViewImg.setImageResource(R.drawable.artwork);
        }
    }

    private String getReadableTime(int duration) {
        String time;
        int hrs=  duration/(1000*60*60);
        int min=  (duration%(1000*60*60))/(1000*60);
        int sec=  (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;
        if (hrs<1){
            time=min+":"+sec;
        }else{
            time=hrs+":"+min+":"+sec;
        }
        return time;
    }

    private void openPlayerView() {
        binding.playerViewLayout.playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }


    private void exitPlayerView() {
        binding.playerViewLayout.playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));
    }

    private void activateAudioVisualizer() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
return;
        }

        binding.playerViewLayout.visualizer.setColor(ContextCompat.getColor(this,R.color.secondary_color));
        binding.playerViewLayout.visualizer.setDensity(15);
        binding.playerViewLayout.visualizer.setPlayer(exoPlayer.getAudioSessionId());
    }
    private void userResponseRecordAudioPermission() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(recordAudioPermission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting To show audio visualizer")
                        .setMessage("allow the app To show audio visualizer when playing audio")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                               recordAudioPermissionLauncher.launch(recordAudioPermission);
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "You denied us for show audio visualizer", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }else{
                Toast.makeText(MainActivity.this, "You denied us for show audio visualizer", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void userResponse() {
        if(ContextCompat.checkSelfPermission(this,storagePermission)== PackageManager.PERMISSION_GRANTED){
            fetchSongs();
        }else {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(storagePermission)){
                    new AlertDialog.Builder(this)
                            .setTitle("Requesting Permission")
                            .setMessage("we need your permission to fetch songs on your device")
                            .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    storagePermissionLauncher.launch(storagePermission);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(MainActivity.this, "You denied us for show songs", Toast.LENGTH_SHORT).show();
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();
                }
            }else{
                Toast.makeText(MainActivity.this, "You canceled to show songs", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void fetchSongs() {
    List<Song>songs=new ArrayList<>();
        Uri mediaStoreUri;
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.Q){
            mediaStoreUri= MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else {
            mediaStoreUri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

    String[] projection=new String[]{
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.SIZE,
    MediaStore.Audio.Media.ALBUM_ID,

    };

 String sortOrder=MediaStore.Audio.Media.DATE_ADDED + " DESC";

 try (Cursor cursor=getContentResolver().query(mediaStoreUri,projection,null,null,sortOrder)){
    int idColumn=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
    int nameColumn=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
    int durationColumn=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
    int sizeColumn=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
    int albumIdColumn=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

    while (cursor.moveToNext()){
        long id=cursor.getLong(idColumn);
        String name=cursor.getString(nameColumn);
        int duration=cursor.getInt(durationColumn);
        int size=cursor.getInt(sizeColumn);
        long albumId=cursor.getLong(albumIdColumn);

        Uri uri=ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);
        Uri albumArtUri=ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId);
        name=name.substring(0,name.lastIndexOf("."));
        Song song=new Song(name,uri,albumArtUri,size,duration);
        songs.add(song);

    }

   showSongs(songs);

 }


    }

    private void showSongs(List<Song> songs) {
        if (songs.size()==0){
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }
        allSongs.clear();
        allSongs.addAll(songs);
        songAdapter=new SongAdapter(this,songs,exoPlayer,binding.playerViewLayout.playerView
        );
        String title=getResources().getString(R.string.app_name)+"-"+songs.size();
       Objects.requireNonNull(getSupportActionBar()).setTitle(title);
       // Toast.makeText(this, String.valueOf(allSongs.size()), Toast.LENGTH_SHORT).show();

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        binding.recyclerview.setLayoutManager(layoutManager);

      //  recyclerView.setAdapter(songAdapter);
        //for animation
        ScaleInAnimationAdapter scaleInAnimationAdapter=new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        binding.recyclerview.setAdapter(scaleInAnimationAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search,menu);
        MenuItem item=menu.findItem(R.id.search_btn);
        SearchView searchView=(SearchView) item.getActionView();
        searchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void searchSong(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }


        });
    }

    private void filterSongs(String query) {
        List<Song>filteredList=new ArrayList<>();
        if (allSongs.size()>0){
            for (Song song:allSongs){
                if(song.getTitle().toLowerCase().contains(query)){
                    filteredList.add(song);
                }
            }
            if (songAdapter!=null){
                songAdapter.filterSongs(filteredList);
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (binding.playerViewLayout.playerView.getVisibility()==View.VISIBLE){
            exitPlayerView();
        }else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (exoPlayer.isPlaying()){
//            exoPlayer.stop();
//        }
//        exoPlayer.release();
        stopbindService();
    }

    private void stopbindService() {
        if (isBound){
            unbindService(playerServiceConnection);
            isBound=false;
        }
    }
}