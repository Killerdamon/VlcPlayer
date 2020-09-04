package com.android.vlcplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class Utils {
    public static String TAG = "Utils";
    public static void getVideoInfo(Context context, MediaPlayer mMediaPlayer)
    {
        String info = "";
        Media media = (Media)mMediaPlayer.getMedia();
        for (int i=0; i<media.getTrackCount();i++){
            if (media.getTrack(i).type == 0) {
                Media.AudioTrack audioTrack = (Media.AudioTrack) media.getTrack(i);
                Log.d(TAG, "audioTrack rate:" + audioTrack.rate + " channels:" + audioTrack.channels +
                        " codec:" +  audioTrack.codec);
                info = info + "音频"+i+"\n" + "编码：" + audioTrack.codec + "\n"+ "声道：" + audioTrack.channels + "\n"
                        + "采样率：" + audioTrack.rate + "Hz\n\n";
            }
            else if (media.getTrack(i).type == 1) {
                Media.VideoTrack videoTrack = (Media.VideoTrack) media.getTrack(i);
                Log.d(TAG, "videoTrack width:" + videoTrack.width + " height:" +  videoTrack.height
                        + " codec:" +  videoTrack.codec + " language:" +  videoTrack.language + " level:" +  videoTrack.level);
                info = info + "视频"+i+"\n" + "编码：" + videoTrack.codec + "\n"+
                        "分辨率：" + videoTrack.width + "*" + videoTrack.height  + "\n"
                        + "帧率：" + videoTrack.frameRateNum + "\n\n";
            }
        }
        dialogInfo(context,"Video Info", info);
    }

    public static AlertDialog.Builder dialogInfo;
    public static void dialogInfo(Context context, String title, String message){
        dialogInfo = new AlertDialog.Builder(context)
                .setTitle(title)//设置title
                .setMessage(message)
                .setCancelable(false)//表示点击dialog其它部分不能取消(除了“取消”，“确定”按钮)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        dialogInfo.show();
    }

    public static AlertDialog.Builder dialogAudio;
    public static void dialogAudio(Context context, final MediaPlayer mediaPlayer){
        final MediaPlayer.TrackDescription[] audioTracks = mediaPlayer.getAudioTracks();
        int curAudio = mediaPlayer.getAudioTrack();

        String[] items = new String[audioTracks.length];
        int select = 0;
        for (int i = 0; i < audioTracks.length; i++) {
            Log.d(TAG, "audioTracks name:" + audioTracks[i].name + " id:" + audioTracks[i].id);
            items[i] = audioTracks[i].name;
            if (audioTracks[i].id == curAudio){
                select = i;
            }
        }

        dialogAudio = new AlertDialog.Builder(context);
        dialogAudio.setTitle("音轨设置");
        dialogAudio.setSingleChoiceItems(items, select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mediaPlayer.setAudioTrack(audioTracks[i].id);
            }
        });
        dialogAudio.show();
    }

    public static AlertDialog.Builder dialogSubtitle;
    public static void dialogSubtitle(Context context, final MediaPlayer mediaPlayer){
        final MediaPlayer.TrackDescription[] spuTracks = mediaPlayer.getSpuTracks();
        int curSubtitle = mediaPlayer.getSpuTrack();

        String[] items = new String[spuTracks.length];
        int select = 0;
        for (int i = 0; i < spuTracks.length; i++) {
            Log.d(TAG, "spuTracks name:" + spuTracks[i].name + " id:" + spuTracks[i].id);
            items[i] = spuTracks[i].name;
            if (spuTracks[i].id == curSubtitle){
                select = i;
            }
        }

        dialogAudio = new AlertDialog.Builder(context);
        dialogAudio.setTitle("字幕设置");
        dialogAudio.setSingleChoiceItems(items, select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mediaPlayer.setSpuTrack(spuTracks[i].id);
            }
        });
        dialogAudio.show();
    }
}
