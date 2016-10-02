package com.example.liu.musicbox;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;


public class MusicService extends Service {
    ServiceReceiver serviceReceiver;
    int status = 0x11;
    MediaPlayer player;
    int current = 0;
    int count;
    String path;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceReceiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.CTL_ACTION);
        registerReceiver(serviceReceiver, filter);
        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                current++;
                if (current >= 3) {
                    current = 0;
                }
                Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
                sendIntent.putExtra("current", current);
                sendBroadcast(sendIntent);
                prepareAndPlay(musics[current]);
            }
        });
    }

    public class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control", -1);
            String DATA = intent.getStringExtra("DATA");
            switch (control) {
                case 1:
                    if (status == 0x11) {
                        prepareAndPlay(musics[current]);
                        status = 0x12;
                    } else if (status == 0x12) {
                        player.pause();
                        status = 0x13;
                    } else if (status == 0x13) {
                        player.start();
                        status = 0x12;
                    }
                    break;
                case 2:
                    if (status == 0x12 || status == 0x13) {
                        player.stop();
                        status = 0x11;
                    }
                    break;
                default:
                    status = 0x11;
                    try {
                        player.setDataSource(DATA);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
            sendIntent.putExtra("update", status);
            sendIntent.putExtra("current", current);
            sendBroadcast(sendIntent);
        }
    }

    public void prepareAndPlay(String music) {
        try {
            AssetFileDescriptor afd = am.openFd(music);
            player.reset();
            player.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
