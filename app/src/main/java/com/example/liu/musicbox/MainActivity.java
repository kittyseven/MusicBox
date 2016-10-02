package com.example.liu.musicbox;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView title, author;
    ImageButton play, stop, next, previous;
    ListView musicsList;
    //    用来存储本地歌曲信息的List
    ArrayList<Music> Musics;
    ContentResolver cr;
    Cursor musics;
    //歌曲总数目，进度条总长度
    int count;
    //    当前进度
    int progressStatus;
    //    对话框形式的进度条
    ProgressDialog progressDialog;

    ActivityReceiver activityReceiver;

    public static final String CTL_ACTION =
            "liu.action.CTL_ACTION";
    public static final String UPDATE_ACTION =
            "liu.action.UPDATE_ACTION";

    //定义音乐的播放状态，0x11代表没有播放；0x12代表正在播放；0x13代表暂停；
    int status = 0x11;

    //    实时更新进度条进度的handler
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x123) {
                progressDialog.setProgress(progressStatus);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        showProgress();
        musicsList.setAdapter(adapter);
        musicsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CTL_ACTION);
                intent.putExtra("DATA",Musics.get(position).DATA);
                sendBroadcast(intent);
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        registerReceiver(activityReceiver, filter);
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("COUNT",count);
        intent.putExtra("DATA",Musics.get(0).DATA);
        bindService(intent,);
        Log.d("Activity", "启动Service后");
    }

    public BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout linearLayout = new LinearLayout(MainActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(MainActivity.this);
            title.setTextSize(20);
            title.setTextColor(Color.BLACK);
            title.setText(Musics.get(position).TITLE);
            TextView artist = new TextView(MainActivity.this);
            artist.setTextSize(18);
            artist.setTextColor(Color.GRAY);
            StringBuilder artistalbum = new StringBuilder("");
            artistalbum.append(Musics.get(position).ARTIST);
            artistalbum.append(" - ");
            artistalbum.append(Musics.get(position).ALBUM);
            artist.setText(artistalbum);
            linearLayout.addView(title);
            linearLayout.addView(artist);
            return linearLayout;
        }
    };

    private void showProgress() {
        progressStatus = 0;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMax(count);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.progress_title);
        progressDialog.setMessage(getString(R.string.progress_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();

        new Thread() {
            @Override
            public void run() {
                Musics = initMusics();
                progressDialog.dismiss();
            }
        }.run();
    }

    private void init() {
        musicsList = (ListView) findViewById(R.id.musics_list);
        play = (ImageButton) findViewById(R.id.play);
        stop = (ImageButton) findViewById(R.id.stop);
        previous = (ImageButton) findViewById(R.id.previous);
        next = (ImageButton) findViewById(R.id.next);
        title = (TextView) findViewById(R.id.title);
        author = (TextView) findViewById(R.id.author);
        count = queryMusics();
        activityReceiver = new ActivityReceiver();
    }

    public class ActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra("update", -1);
            int current = intent.getIntExtra("current", -1);
            if (current >= 0) {
                title.setText(Musics.get(current).TITLE);
                author.setText(Musics.get(current).ARTIST);
            }
            switch (update) {
                case 0x11:
                    play.setImageResource(R.drawable.selector_play);
                    status = 0x11;
                    break;
                case 0x12:
                    play.setImageResource(R.drawable.selector_pause);
                    status = 0x12;
                    break;
                case 0x13:
                    play.setImageResource(R.drawable.selector_play);
                    status = 0x13;
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        Log.d("onclick", "进入了onclick");
        Intent intent = new Intent(CTL_ACTION);
        switch (v.getId()) {
            case R.id.play:
                intent.putExtra("control", 1);
                break;
            case R.id.stop:
                intent.putExtra("control", 2);
                break;
        }
        sendBroadcast(intent);
    }


    private int queryMusics() {
        cr = this.getContentResolver();
        musics = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID, // int
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA, // String
                        MediaStore.Audio.Media.DISPLAY_NAME, // String
                        MediaStore.Audio.Media.MIME_TYPE // String
                },
                MediaStore.Audio.Media.IS_MUSIC + " = 1 AND "
                        + MediaStore.Audio.Media.DURATION + " > 10000",
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        return musics.getCount();
    }


    private ArrayList<Music> initMusics() {
        ArrayList<Music> musiclistResult = new ArrayList<Music>();
        musics.moveToFirst();
        while (!musics.isAfterLast())

        {
            Music temp = new Music();
            temp._ID = musics.getInt(musics.getColumnIndex(MediaStore.Audio.Media._ID));
            temp.ALBUM = musics.getString(musics.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            temp.ARTIST = musics.getString(musics.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            temp.DATA = musics.getString(musics.getColumnIndex(MediaStore.Audio.Media.DATA));
            temp.DURATION = musics.getLong(musics.getColumnIndex(MediaStore.Audio.Media.DURATION));
            temp.TITLE = musics.getString(musics.getColumnIndex(MediaStore.Audio.Media.TITLE));
            temp.isExist = true;

            musiclistResult.add(temp);
            progressStatus = musiclistResult.size();
            handler.sendEmptyMessage(0x123);
            musics.moveToNext();
        }

        musics.close();
        return musiclistResult;
    }

    /**
     * 音乐的信息
     *
     * @author BabyBeaR
     */
    @SuppressWarnings("unused")
    private class Music {
        int _ID;
        long DURATION;
        boolean isExist;
        String TITLE, ARTIST, ALBUM, DATA;
    }
}
