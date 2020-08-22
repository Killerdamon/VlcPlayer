package com.android.vlcplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    private View toplayout, frame_layout;
    RelativeLayout.LayoutParams saveLayout;
    private ProgressBar progressBar;
    private ImageView back, full_screen;
    private TextView error_text;
    private boolean isFullScreen;
    private int full_screen_width, full_screen_height;
    private int screen_width, screen_height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //全屏大小
        full_screen_width = getResources().getDisplayMetrics().widthPixels;
        full_screen_height = getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "full_screen_width:" + full_screen_width + " full_screen_height:" + full_screen_height);

        final ArrayList<String> args = new ArrayList<>();//VLC参数
        args.add("--rtsp-tcp");//强制rtsp-tcp，加快加载视频速度

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
                    mMediaPlayer.setAspectRatio(screen_width + ":" + screen_height);//设置屏幕比例
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //非全屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
                    toplayout.setVisibility(View.VISIBLE);
                    full_screen.setBackgroundResource(R.drawable.full_screen);
                    frame_layout.setLayoutParams(saveLayout);
                } else {
                    isFullScreen = true;
                    mMediaPlayer.setAspectRatio(full_screen_height+ ":" + full_screen_width);//设置屏幕比例
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
                }
                else if (event.type == MediaPlayer.Event.Playing){
                    Log.d(TAG, "VLC Playing");
                    //frame的屏幕大小
                    screen_width = frame_layout.getWidth();
                    screen_height = frame_layout.getHeight();
                    Log.d(TAG, "screen_width:" + screen_width + " screen_height:" + screen_height);
                    mMediaPlayer.setAspectRatio(screen_width + ":" + screen_height);//设置屏幕比例
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
        Uri uri = Uri.parse("http://nclive.grtn.cn/gdws/sd/live.m3u8?_upt=0704702d1598088562");//rtsp流地址或其他流地址
        //final Media media = new Media(mLibVLC, getAssets().openFd(ASSET_FILENAME));
        final Media media = new Media(mLibVLC, uri);
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
}