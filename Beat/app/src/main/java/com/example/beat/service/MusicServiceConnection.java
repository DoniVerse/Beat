package com.example.beat.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MusicServiceConnection {
    private static final String TAG = "MusicServiceConnection";

    private Context context;
    private MusicService musicService;
    private boolean isServiceBound = false;
    private ServiceConnectionListener listener;

    public interface ServiceConnectionListener {
        void onServiceConnected(MusicService service);
        void onServiceDisconnected();
    }
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;

            if (listener != null) {
                listener.onServiceConnected(musicService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            musicService = null;
            isServiceBound = false;
            
            if (listener != null) {
                listener.onServiceDisconnected();
            }
        }
    };
    
    public MusicServiceConnection(Context context) {
        this.context = context;
    }
    
    public void setServiceConnectionListener(ServiceConnectionListener listener) {
        this.listener = listener;
    }
    
    public void bindService() {
        if (!isServiceBound) {
            Intent intent = new Intent(context, MusicService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Binding to service");
        }
    }
    
    public void unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
            musicService = null;
            Log.d(TAG, "Unbound from service");
        }
    }
    
    public MusicService getService() {
        return musicService;
    }
    
    public boolean isServiceBound() {
        return isServiceBound;
    }
    
    public void playMusic(String streamUrl, String title, String artist, String albumArt) {
        if (musicService != null) {
            musicService.playMusic(streamUrl, title, artist, albumArt);
        } else {
            Log.w(TAG, "Service not bound, cannot play music");
        }
    }
    
    public void pauseMusic() {
        if (musicService != null) {
            musicService.pauseMusic();
        }
    }
    
    public void resumeMusic() {
        if (musicService != null) {
            musicService.resumeMusic();
        }
    }
    
    public void stopMusic() {
        if (musicService != null) {
            musicService.stopMusic();
        }
    }
    
    public boolean isPlaying() {
        return musicService != null && musicService.isPlaying();
    }
    
    public int getCurrentPosition() {
        return musicService != null ? musicService.getCurrentPosition() : 0;
    }
    
    public int getDuration() {
        return musicService != null ? musicService.getDuration() : 0;
    }
    
    public void seekTo(int position) {
        if (musicService != null) {
            musicService.seekTo(position);
        }
    }

    public String getCurrentTrackTitle() {
        return musicService != null ? musicService.getCurrentTrackTitle() : null;
    }

    public String getCurrentArtist() {
        return musicService != null ? musicService.getCurrentArtist() : null;
    }
}
