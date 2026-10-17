package io.github.foadsf.kidlauncher;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/** All the games on one scrolling page. Back, Home or the house button leave it. */
public class GamesActivity extends TileActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.games);
        ((TextView) findViewById(R.id.title)).setTypeface(face());
        ((SkyView) findViewById(R.id.sky)).setShowSun(false);
        findViewById(R.id.back_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        addTiles((TileGrid) findViewById(R.id.grid), KidApps.GAME_LIST);
    }
}
