package io.github.foadsf.kidlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The grown-up side: a sum a five-year-old cannot do yet (two-digit addition), then a short
 * menu. No PIN to forget. Either dialog closes itself after {@link #IDLE_MS} if left open.
 */
final class ParentMenu {
    private static final long IDLE_MS = 30_000;
    private final Activity activity;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    ParentMenu(Activity activity) {
        this.activity = activity;
    }

    void showGate() {
        final int a = 11 + random.nextInt(9);   // 11..19
        final int b = 5 + random.nextInt(5);    // 5..9, so there is always a carry
        float dp = activity.getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * dp);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView q = new TextView(activity);
        q.setText(activity.getString(R.string.gate_question, a, b));
        q.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        q.setGravity(Gravity.CENTER);
        box.addView(q);

        final EditText answer = new EditText(activity);
        answer.setInputType(InputType.TYPE_CLASS_NUMBER);
        answer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        answer.setGravity(Gravity.CENTER);
        answer.setSingleLine(true);
        answer.setImeOptions(EditorInfo.IME_ACTION_DONE);
        box.addView(answer);

        final AlertDialog d = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(R.string.gate_title)
                .setView(box)
                .setPositiveButton(R.string.gate_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(answer.getWindowToken(), 0);
                        if (String.valueOf(a + b).equals(answer.getText().toString().trim())) {
                            showMenu();
                        } else {
                            Toast.makeText(activity, R.string.gate_wrong, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.gate_cancel, null)
                .create();
        // The keyboard's Done (or a hardware Enter) answers, like pressing OK.
        answer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterUp = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_UP;
                if (actionId == EditorInfo.IME_ACTION_DONE || enterUp) {
                    d.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                return actionId == EditorInfo.IME_ACTION_DONE || event != null;
            }
        });
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        autoClose(d);
        d.show();
    }

    private void showMenu() {
        CharSequence[] items = {
                activity.getString(R.string.parent_settings),
                activity.getString(R.string.parent_other_home),
                activity.getString(R.string.parent_choose_home)};
        AlertDialog d = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(R.string.parent_title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) openSettings();
                        else if (which == 1) openOtherHome();
                        else openHomeChooser();
                    }
                })
                .setNegativeButton(R.string.gate_cancel, null)
                .create();
        autoClose(d);
        d.show();
    }

    private void autoClose(final AlertDialog d) {
        final Runnable close = new Runnable() {
            @Override
            public void run() {
                if (d.isShowing()) d.dismiss();
            }
        };
        handler.postDelayed(close, IDLE_MS);
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(close);
            }
        });
    }

    private void openSettings() {
        start(new Intent(Settings.ACTION_SETTINGS));
    }

    /** Every other installed home screen (on this phone: TouchWiz and Samsung's Easy mode). */
    private void openOtherHome() {
        PackageManager pm = activity.getPackageManager();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        final List<ResolveInfo> others = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(home, 0)) {
            if (!ri.activityInfo.packageName.equals(activity.getPackageName())) others.add(ri);
        }
        if (others.isEmpty()) {
            Toast.makeText(activity, R.string.no_other_home, Toast.LENGTH_LONG).show();
            return;
        }
        if (others.size() == 1) {
            startHome(others.get(0));
            return;
        }
        CharSequence[] names = new CharSequence[others.size()];
        for (int i = 0; i < names.length; i++) names[i] = others.get(i).loadLabel(pm);
        new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(R.string.parent_other_home)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startHome(others.get(which));
                    }
                })
                .show();
    }

    private void startHome(ResolveInfo ri) {
        start(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)));
    }

    /**
     * Samsung's 4.4 Settings has a Home screen picker; stock 4.4 hides it. Fall back to this
     * app's info page, whose "Clear defaults" button makes Android ask again on the next Home.
     */
    private void openHomeChooser() {
        Intent picker = new Intent("android.settings.HOME_SETTINGS");
        if (picker.resolveActivity(activity.getPackageManager()) != null) {
            start(picker);
        } else {
            start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.getPackageName(), null)));
        }
    }

    private void start(Intent i) {
        try {
            activity.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, R.string.app_missing, Toast.LENGTH_SHORT).show();
        }
    }
}
