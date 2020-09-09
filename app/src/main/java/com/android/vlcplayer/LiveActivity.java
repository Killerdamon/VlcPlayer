package com.android.vlcplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;

public class LiveActivity extends AppCompatActivity {

    private String TAG = "LiveActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private Context context = this;
    private View toplayout, frame_layout;
    RelativeLayout.LayoutParams saveLayout;
    private LinearLayout menu;
    private ProgressBar progressBar;
    private ImageView back, full_screen, info, audio, subtitle, snapshot, record;
    private TextView error_text;
    private boolean isFullScreen, isRecord = false;
    private int full_screen_width, full_screen_height;
    private int screen_width, screen_height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_live);

        //全屏大小
        full_screen_width = getResources().getDisplayMetrics().widthPixels;
        full_screen_height = getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "full_screen_width:" + full_screen_width + " full_screen_height:" + full_screen_height);

        final ArrayList<String> args = new ArrayList<>();//VLC参数
        args.add("--rtsp-tcp");//强制rtsp-tcp，加快加载视频速度
        args.add("--live-caching=0");
        args.add("--file-caching=0");
        args.add("--network-caching=0");//增加实时性，延时大概2-3秒
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mVideoLayout = findViewById(R.id.video_layout);

        toplayout = findViewById(R.id.toplayout);
        frame_layout = findViewById(R.id.frame_layout);

        full_screen = findViewById(R.id.full_screen);
        full_screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFullScreen) {
                    isFullScreen = false;
                    mHandler.sendEmptyMessageDelayed(UPDATE_SCREEN, 100);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //非全屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
                    toplayout.setVisibility(View.VISIBLE);
                    full_screen.setBackgroundResource(R.drawable.full_screen);
                    frame_layout.setLayoutParams(saveLayout);
                } else {
                    isFullScreen = true;
                    mHandler.sendEmptyMessageDelayed(UPDATE_FULL_SCREEN, 100);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //清除非全屏的flag
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //设置全屏的flag
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//横屏
                    full_screen.setBackgroundResource(R.drawable.full_screen2);
                    saveLayout = (RelativeLayout.LayoutParams) frame_layout.getLayoutParams();
                    frame_layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            }
        });
        back = findViewById(R.id.icon_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        progressBar = findViewById(R.id.progressBar);
        error_text = findViewById(R.id.error_text);

        menu = findViewById(R.id.menu);
        info = findViewById(R.id.info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.getVideoInfo(context, mMediaPlayer);
            }
        });
        audio = findViewById(R.id.audio);
        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.dialogAudio(context,  mMediaPlayer);
            }
        });
        subtitle = findViewById(R.id.subtitle);
        subtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.dialogSubtitle(context, mMediaPlayer);
            }
        });

        snapshot = findViewById(R.id.snapshot);
        snapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer.takeSnapShot(0, Utils.getSDPath(), 0, 0)) {
                    Toast.makeText(LiveActivity.this, "截图成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LiveActivity.this, "截图失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        record = findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecord) {
                    if (mMediaPlayer.record(Utils.getSDPath())) {
                        Toast.makeText(LiveActivity.this, "录制开始", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(LiveActivity.this, "录制失败", Toast.LENGTH_SHORT).show();
                    }
                    isRecord = true;
                }
                else {
                    Toast.makeText(LiveActivity.this, "录制结束", Toast.LENGTH_SHORT).show();
                    isRecord = false;
                    finish();
                }
            }
        });

        //监听播放状态
        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                if (event.type == MediaPlayer.Event.Opening) {
                    Log.d(TAG, "VLC Opening");
                    progressBar.setVisibility(View.VISIBLE);
                }
                else if (event.type == MediaPlayer.Event.Buffering){
                    Log.d(TAG, "VLC Buffering：" + event.getBuffering());
                    if (event.getBuffering() >= 100){
                        progressBar.setVisibility(View.GONE);
                    }
                    else
                        progressBar.setVisibility(View.VISIBLE);
                }
                else if (event.type == MediaPlayer.Event.Playing){
                    Log.d(TAG, "VLC Playing");
                    menu.setVisibility(View.VISIBLE);
                }
                else if (event.type == MediaPlayer.Event.Stopped){
                    Log.d(TAG, "VLC Stopped");
                    progressBar.setVisibility(View.GONE);
                }
                else if (event.type == MediaPlayer.Event.EncounteredError){
                    Log.d(TAG, "VLC EncounteredError");
                    progressBar.setVisibility(View.GONE);
                    error_text.setVisibility(View.VISIBLE);
                    error_text.setText("播放错误");
                }
                else if (event.type == MediaPlayer.Event.Vout){
                    Log.d(TAG, "VLC Vout"+ event.getVoutCount());
                    mHandler.sendEmptyMessageDelayed(UPDATE_SCREEN, 100);

                    //检查音轨
                    if (mMediaPlayer.getAudioTracks() != null){
                        audio.setVisibility(View.VISIBLE);
                    }
                    //检查字幕
                    if (mMediaPlayer.getSpuTracks() != null){
                        subtitle.setVisibility(View.VISIBLE);
                    }
                    snapshot.setVisibility(View.VISIBLE);
                    record.setVisibility(View.VISIBLE);
                }
                else if (event.type == MediaPlayer.Event.RecordChanged){
                    Log.d(TAG, "VLC RecordChanged");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
        mMediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
        Uri uri = Uri.parse("https://nclive.grtn.cn/gdws/sd/live.m3u8?_upt=f1ae9de51599655200");//rtsp流地址或其他流地址
        //final Media media = new Media(mLibVLC, getAssets().openFd(ASSET_FILENAME));
        final Media media = new Media(mLibVLC, uri);
        media.setHWDecoderEnabled(false, false);//设置后才可以录制和截屏
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }

    private final int UPDATE_SCREEN = 0;
    private final int UPDATE_FULL_SCREEN = 1;
    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_SCREEN:
                    //frame的屏幕大小
                    screen_width = frame_layout.getWidth();
                    screen_height = frame_layout.getHeight();
                    Log.d(TAG, "screen_width:" + screen_width + " screen_height:" + screen_height);
                    mMediaPlayer.getVLCVout().setWindowSize(screen_width, screen_height);
                    mMediaPlayer.setAspectRatio(screen_width + ":" + screen_height);//设置屏幕比例
                    mMediaPlayer.setScale(0);
                    break;
                case UPDATE_FULL_SCREEN:
                    mMediaPlayer.getVLCVout().setWindowSize(full_screen_height, full_screen_width);
                    mMediaPlayer.setAspectRatio(full_screen_height + ":" + full_screen_width);//设置屏幕比例
                    mMediaPlayer.setScale(0);
                    break;
            }
            return false;
        }
    });

}