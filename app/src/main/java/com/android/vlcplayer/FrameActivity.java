package com.android.vlcplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayCallback;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

public class FrameActivity extends AppCompatActivity {

    private String TAG = "VodActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private SeekBar mProgress;
    private boolean mDragging;
    private Context context = this;
    private LinearLayout menu;
    private View toplayout, frame_layout;
    RelativeLayout.LayoutParams saveLayout;
    private ProgressBar progressBar;
    private ImageView back, full_screen, info;
    private ImageView play_pause, img;
    private TextView error_text, time_current, time_total;
    private boolean isFullScreen;
    private int full_screen_width, full_screen_height;
    private int screen_width, screen_height;
    private static final String ASSET_FILENAME = "bbb.m4v";
    private final int FRM_WIDTH = 500;
    private final int FRM_HEIGHT = 400;
    private final int PIXEL_SIZE = 4;

    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_frame);

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
        time_current = findViewById(R.id.time_current);
        time_total = findViewById(R.id.time_total);
        menu = findViewById(R.id.menu);
        info = findViewById(R.id.info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.getVideoInfo(context, mMediaPlayer);
            }
        });

        img = findViewById(R.id.img);
        mMediaPlayer.setVideoFormat("RGBA",FRM_WIDTH,FRM_HEIGHT,FRM_WIDTH*PIXEL_SIZE);
        ByteBuffer frameBuffer = ByteBuffer.allocateDirect(FRM_WIDTH*FRM_HEIGHT*PIXEL_SIZE);
        mMediaPlayer.setVideoCallback(frameBuffer, new MediaPlayCallback() {
            @Override
            public void onDisplay(ByteBuffer buffer) {
                //Log.d(TAG, "buffer:" + buffer);
                final Bitmap bitmap = Bitmap.createBitmap(FRM_WIDTH,FRM_HEIGHT, Bitmap.Config.ARGB_8888);
                buffer.rewind();
                bitmap.copyPixelsFromBuffer(buffer);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        img.setImageBitmap(bitmap);
                    }
                });
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
                    if (event.getBuffering() >= 100)
                        progressBar.setVisibility(View.GONE);
                    else
                        progressBar.setVisibility(View.VISIBLE);
                }
                else if (event.type == MediaPlayer.Event.Playing){
                    Log.d(TAG, "VLC Playing");
                    menu.setVisibility(View.VISIBLE);
                    //设置总时间
                    Log.d(TAG, "mMediaPlayer.getTitle()："+ mMediaPlayer.getTitle());
                    time_total.setText(stringForTime((int)mMediaPlayer.getLength()));
                }
                else if (event.type == MediaPlayer.Event.EndReached){
                    Log.d(TAG, "VLC EndReached");
                    finish();
                }
                else if (event.type == MediaPlayer.Event.MediaChanged){
                    Log.d(TAG, "VLC MediaChanged");
                }
                else if (event.type == MediaPlayer.Event.TimeChanged){
                    Log.d(TAG, "VLC TimeChanged："+ event.getTimeChanged());
                    time_current.setText(stringForTime((int) event.getTimeChanged()));
                }
                else if (event.type == MediaPlayer.Event.PositionChanged){
                    Log.d(TAG, "VLC PositionChanged：" + event.getPositionChanged());
                    mProgress.setProgress((int)(event.getPositionChanged()*100));
                }
                else if (event.type == MediaPlayer.Event.Stopped){
                    Log.d(TAG, "VLC Stopped");
                    progressBar.setVisibility(View.GONE);
                }
                else if (event.type == MediaPlayer.Event.SeekableChanged){
                    Log.d(TAG, "VLC SeekableChanged");
                }
                else if (event.type == MediaPlayer.Event.PausableChanged){
                    Log.d(TAG, "VLC PausableChanged");
                }
                else if (event.type == MediaPlayer.Event.Vout){
                    Log.d(TAG, "VLC Vout"+ event.getVoutCount());
                    mHandler.sendEmptyMessageDelayed(UPDATE_SCREEN, 100);
                }
                else if (event.type == MediaPlayer.Event.ESAdded){
                    Log.d(TAG, "VLC ESAdded");
                }
                else if (event.type == MediaPlayer.Event.ESDeleted){
                    Log.d(TAG, "VLC ESDeleted");
                }
                else if (event.type == MediaPlayer.Event.ESSelected){
                    Log.d(TAG, "VLC ESSelected");
                }
                else if (event.type == MediaPlayer.Event.LengthChanged){
                    Log.d(TAG, "VLC LengthChanged"+ event.getLengthChanged());
                }
                else if (event.type == MediaPlayer.Event.EncounteredError){
                    Log.d(TAG, "VLC EncounteredError");
                    progressBar.setVisibility(View.GONE);
                    error_text.setVisibility(View.VISIBLE);
                    error_text.setText("播放错误");
                }
            }
        });
        play_pause = findViewById(R.id.play_pause);
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMediaPlayer.isPlaying()){
                    mMediaPlayer.pause();
                    play_pause.setImageResource(R.drawable.media_pause2);
                }
                else {
                    mMediaPlayer.play();
                    play_pause.setImageResource(R.drawable.media_play);
                }
            }
        });
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mProgress = (SeekBar) findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(100);
        }
        mProgress.requestFocus();
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
        try {
            final Media media = new Media(mLibVLC, getAssets().openFd(ASSET_FILENAME));
            mMediaPlayer.setMedia(media);
            media.release();
            mMediaPlayer.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
                    .toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
			Log.d("TAG", "onProgressChanged");
            mDragging = true;
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mDragging) {
                Log.d("TAG", "onProgressChanged:" + progress);
                mMediaPlayer.setPosition((float) progress / 100);
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
			Log.d("TAG", "onStopTrackingTouch");
            mDragging = false;
        }
    };

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