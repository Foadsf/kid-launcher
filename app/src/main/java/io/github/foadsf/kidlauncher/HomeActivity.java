package io.github.foadsf.kidlauncher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class HomeActivity extends TileActivity implements ParentGateView.Listener {
    private ParentMenu parentMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        TextView greeting = (TextView) findViewById(R.id.greeting);
        greeting.setTypeface(face());
        String name = getString(R.string.kid_name);
        greeting.setText(name.isEmpty() ? getString(R.string.greeting_anon)
                : getString(R.string.greeting, name));
        ((ParentGateView) findViewById(R.id.gate)).setListener(this);
        parentMenu = new ParentMenu(this);
        TileGrid grid = (TileGrid) findViewById(R.id.grid);
        grid.setSpanLonelyLast(true);
        addTiles(grid, KidApps.HOME);
    }

    /** Home pressed while already home: a cheerful bounce instead of nothing. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (hasWindowFocus()) popIn();
    }

    /** A home screen has nowhere to go back to. */
    @Override
    public void onBackPressed() {
    }

    @Override
    public void onParentGate() {
        parentMenu.showGate();
    }
}
