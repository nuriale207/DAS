package com.example.das;

import android.content.Context;
import android.media.MediaPlayer;

public class ReproductorSonido {

    private static ReproductorSonido reproductorSonido;
    private Boolean reproducirCancioncilla;

    private ReproductorSonido(){
    }

    public static ReproductorSonido getReproductorSonido() {
        if (reproductorSonido == null) {
            reproductorSonido = new ReproductorSonido();
        }
        return reproductorSonido;
    }


    //https://stackoverflow.com/questions/42326228/playing-short-sound-on-touch
    public void reproducirSonido(Context context, int sonido){
        MediaPlayer mediaPlayer = MediaPlayer.create(context,sonido);
        mediaPlayer.setVolume(1,1);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setVolume(1,1);
                mp.start();
            }

        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    public void reproducirFinPartida(Context context, int sonido){
        if(reproducirCancioncilla){
            reproducirCancioncilla = false;
            reproducirSonido(context,sonido);
        }
    }

    public void habilitarCancioncilla(){
        reproducirCancioncilla = true;
    }
}
