package com.example.liu.musicbox;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
                if (current >= count) {
                    current = 0;
                }
                Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
                sendIntent.putExtra("current", current);
                sendIntent.putExtra("isCompletion",true);
                sendBroadcast(sendIntent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        path = intent.getStringExtra("DATA");
        count = intent.getIntExtra("COUNT", -1);
        prepareAndPlay(path);
        return super.onStartCommand(intent, flags, startId);
    }

    public class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control", -1);
            path = intent.getStringExtra("DATA");
            switch (control) {
                case 1:
                    if (status == 0x11) {
                        prepareAndPlay(path);
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
                case 3:
                    status = 0x11;
                    current--;
                    prepareAndPlay(path);
                    break;
                case 4:
                    status = 0x11;
                    current++;
                    prepareAndPlay(path);
                default:
                    status = 0x11;
                    prepareAndPlay(path);
                    break;
            }
            Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
            sendIntent.putExtra("update", status);
            sendIntent.putExtra("current", current);
            sendBroadcast(sendIntent);
        }
    }

    public void prepareAndPlay(String path) {
        try {
            player.reset();
            player.setDataSource(path);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
