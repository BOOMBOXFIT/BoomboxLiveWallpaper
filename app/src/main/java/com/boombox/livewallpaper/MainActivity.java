package com.boombox.livewallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 64, 48, 64);
        root.setBackgroundColor(Color.rgb(5,5,10));

        TextView title = new TextView(this);
        title.setText("BOOMBOX\nLIVE WALLPAPER");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,24);

        TextView info = new TextView(this);
        info.setText("Toca la pantalla: ondas de energía\nMueve el teléfono: efecto profundidad\nPartículas y ecualizador animados");
        info.setTextColor(Color.rgb(210,210,225));
        info.setTextSize(17f);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0,0,0,42);

        Button activate = new Button(this);
        activate.setText("ACTIVAR FONDO INTERACTIVO");
        activate.setTextSize(16f);
        activate.setMinHeight(58);
        activate.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, BoomboxWallpaperService.class));
            startActivity(intent);
        });

        root.addView(title,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(info,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(activate,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }
}
