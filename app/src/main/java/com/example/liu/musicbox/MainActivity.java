package com.example.liu.musicbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView title, author;
    ImageButton play, stop;

    ActivityReceiver activityReceiver;

    public static final String CTL_ACTION =
            "liu.action.CTL_ACTION";
    public static final String UPDATE_ACTION =
            "liu.action.UPDATE_ACTION";

    //定义音乐的播放状态，0x11代表没有播放；0x12代表正在播放；0x13代表暂停；
    int status = 0x11;

    String[] titleStrs = new String[]{"牵丝戏", "提灯照河山", "锦鲤抄"};
    String[] authorStrs = new String[]{"小魂", "司夏", "银临"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        registerReceiver(activityReceiver, filter);
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        Log.d("Activity", "启动Service后");
    }

    private void init() {
        play = (ImageButton) findViewById(R.id.play);
        stop = (ImageButton) findViewById(R.id.stop);
        title = (TextView) findViewById(R.id.title);
        author = (TextView) findViewById(R.id.author);
        activityReceiver = new ActivityReceiver();
    }

    public class ActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra("update", -1);
            int current = intent.getIntExtra("current", -1);
            if (current >= 0) {
                title.setText(titleStrs[current]);
                author.setText(authorStrs[current]);
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
}
