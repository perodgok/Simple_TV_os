package com.simpletvos.home;

import android.app.Activity;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.os.SystemClock;
import android.graphics.drawable.Drawable;
import android.view.ViewConfiguration;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView clock;
    private TextView message;
    private LinearLayout root;
    private View homeContent;
    private View appsContent;
    private Button allAppsButton;
    private LinearLayout favoriteRow1, favoriteRow2;
    private final ArrayList<String> favoriteOrder = new ArrayList<>();
    private final java.util.Map<String, Button> favoriteButtons = new java.util.LinkedHashMap<>();
    private String movingTile = null;
    private boolean suppressNextOkUp = false;
    private Runnable pendingLongPress;
    private String pressedTileId = null;
    private static final String[] DEFAULT_FAVORITES = {"youtube", "zona", "browser", "vlc", "apps", "happ"};
    private final int WHITE = Color.rgb(242,246,253);
    private final int MUTED = Color.rgb(165,184,207);

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextColor(color);
        v.setTextSize(size); return v;
    }
    private void launch(String... pkgs) {
        for (String pkg : pkgs) {
            try {
                Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) { startActivity(i); return; }
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "Нет доступного экрана запуска для приложения", Toast.LENGTH_LONG).show();
    }
    private Button tile(String name, int color, String... packages) {
        Button b = new Button(this);
        b.setText(name); b.setTextSize(17); b.setTextColor(WHITE);
        b.setAllCaps(false); b.setBackground(bg(color, 15));
        b.setFocusable(true); b.setOnClickListener(v -> launch(packages));
        b.setOnFocusChangeListener((v,focused) -> {
            v.animate().scaleX(focused ? 1.055f : 1f).scaleY(focused ? 1.055f : 1f).setDuration(130).start();
            v.setElevation(dp(focused ? 10 : 0));
        });
        return b;
    }
    private void loadFavorites() {
        favoriteOrder.clear();
        String saved = getPreferences(MODE_PRIVATE).getString("favorite_order_v05", "");
        if (!saved.isEmpty()) {
            for (String id : saved.split(",")) {
                for (String valid : DEFAULT_FAVORITES)
                    if (valid.equals(id) && !favoriteOrder.contains(id)) favoriteOrder.add(id);
            }
        }
        for (String id : DEFAULT_FAVORITES) if (!favoriteOrder.contains(id)) favoriteOrder.add(id);
    }
    private void saveFavorites() {
        getPreferences(MODE_PRIVATE).edit().putString("favorite_order_v05",
                android.text.TextUtils.join(",", favoriteOrder)).apply();
    }
    private void renderFavorites() {
        if (favoriteRow1 == null || favoriteRow2 == null) return;
        favoriteRow1.removeAllViews(); favoriteRow2.removeAllViews();
        for (int n=0; n<favoriteOrder.size(); n++) {
            String id = favoriteOrder.get(n);
            Button b = favoriteButtons.get(id);
            if (b == null) continue;
            b.setText((id.equals(movingTile) ? "⇄  " : "") + favoriteLabel(id));
            applyFavoriteIcon(b, id);
            b.setBackground(bg(id.equals(movingTile) ? Color.rgb(100, 120, 158) : favoriteColor(id), 15));
            addTile(n<3 ? favoriteRow1 : favoriteRow2,b);
        }
    }
    private String favoriteLabel(String id) {
        switch(id) {
            case "youtube": return "YouTube";
            case "zona": return "Zona";
            case "browser": return "BrowseHere";
            case "vlc": return "VLC";
            case "apps": return "Все приложения";
            default: return "HAPP";
        }
    }
    private int favoriteColor(String id) {
        switch(id) {
            case "youtube": return Color.rgb(158,35,44);
            case "zona": return Color.rgb(53,74,99);
            case "browser": return Color.rgb(26,100,94);
            case "vlc": return Color.rgb(155,91,34);
            case "apps": return Color.rgb(72,65,125);
            default: return Color.rgb(62,75,120);
        }
    }
    private String[] favoritePackages(String id) {
        switch(id) {
            case "youtube": return new String[]{"com.google.android.youtube.tv"};
            case "zona": return new String[]{"mobi.zona"};
            case "browser": return new String[]{"com.tcl.browser", "com.internet.tvbrowser"};
            case "vlc": return new String[]{"org.videolan.vlc"};
            case "happ": return new String[]{"su.happ.proxyutility"};
            default: return new String[0];
        }
    }
    private void applyFavoriteIcon(Button button, String id) {
        Drawable icon = null;
        PackageManager pm = getPackageManager();
        for (String pkg : favoritePackages(id)) {
            try { icon = pm.getApplicationIcon(pkg); break; } catch (Exception ignored) {}
        }
        if (icon == null) icon = getDrawable(android.R.drawable.ic_menu_sort_by_size);
        if (icon != null) {
            icon = icon.mutate();
            icon.setBounds(0, 0, dp(37), dp(37));
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(dp(12));
            button.setGravity(Gravity.CENTER);
        }
    }
    private void clearPendingLongPress() {
        if (pendingLongPress != null) handler.removeCallbacks(pendingLongPress);
        pendingLongPress = null;
        pressedTileId = null;
    }
    private void registerFavorite(String id, Button button) {
        favoriteButtons.put(id,button);
        button.setOnLongClickListener(v -> {
            clearPendingLongPress();
            if (movingTile == null) beginMove(id);
            suppressNextOkUp = true;
            return true;
        });
        button.setOnKeyListener((v,key,event) -> {
            boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
            if (movingTile != null) {
                if (ok) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (suppressNextOkUp) suppressNextOkUp = false;
                        else finishMove();
                    }
                    return true;
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int direction = 0;
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT) direction = -1;
                    if (key == KeyEvent.KEYCODE_DPAD_RIGHT) direction = 1;
                    if (key == KeyEvent.KEYCODE_DPAD_UP) direction = -3;
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN) direction = 3;
                    if (direction != 0) {
                        if (event.getRepeatCount() == 0) moveFavorite(direction);
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_BACK) { cancelMove(); return true; }
                }
                return false;
            }
            if (ok) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) {
                        clearPendingLongPress();
                        pressedTileId = id;
                        suppressNextOkUp = false;
                        pendingLongPress = () -> {
                            if (id.equals(pressedTileId) && movingTile == null) {
                                beginMove(id);
                                suppressNextOkUp = true;
                            }
                        };
                        handler.postDelayed(pendingLongPress, ViewConfiguration.getLongPressTimeout());
                    } else if (event.isLongPress() && movingTile == null) {
                        clearPendingLongPress();
                        beginMove(id);
                        suppressNextOkUp = true;
                    }
                    return true;
                }
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    clearPendingLongPress();
                    if (suppressNextOkUp) { suppressNextOkUp = false; return true; }
                    button.performClick();
                    return true;
                }
            }
            return false;
        });
    }
    private void beginMove(String id) {
        movingTile=id;
        renderFavorites();
        favoriteButtons.get(id).requestFocus();
        Toast.makeText(this,"Режим переноса: стрелки — переместить, OK — сохранить", Toast.LENGTH_LONG).show();
    }
    private void moveFavorite(int delta) {
        int at=favoriteOrder.indexOf(movingTile);
        int next=at+delta;
        if(at<0 || next<0 || next>=favoriteOrder.size()) return;
        Collections.swap(favoriteOrder,at,next);
        renderFavorites();
        favoriteButtons.get(movingTile).requestFocus();
    }
    private void cancelMove() {
        movingTile = null;
        renderFavorites();
        Toast.makeText(this,"Режим перемещения отменён",Toast.LENGTH_SHORT).show();
    }
    private void finishMove() {
        movingTile=null;
        saveFavorites();
        renderFavorites();
        Toast.makeText(this,"Порядок приложений сохранён",Toast.LENGTH_SHORT).show();
    }
    private void launchHapp() {
        String pkg="su.happ.proxyutility";
        PackageManager pm=getPackageManager();
        try {
            Intent i=pm.getLeanbackLaunchIntentForPackage(pkg);
            if(i==null) i=pm.getLaunchIntentForPackage(pkg);
            if(i!=null) { startActivity(i); return; }
        } catch(Exception ignored){}
        // Some APK variants have no launcher/Leanback entry; show installed app details.
        try {
            Intent settings=new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
            settings.setData(android.net.Uri.parse("package:"+pkg));
            startActivity(settings);
            Toast.makeText(this,"HAPP установлен, но не объявляет экран запуска для ТВ",Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"Не удалось открыть HAPP: проверь установленную версию",Toast.LENGTH_LONG).show();
        }
    }
    private void editMessage() {
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setText(getPreferences(MODE_PRIVATE).getString("message", "Твой телевизор. Твои правила."));
        new AlertDialog.Builder(this).setTitle("Текст на главном экране")
            .setView(input).setPositiveButton("Сохранить", (d,w) -> {
                String updated = input.getText().toString();
                getPreferences(MODE_PRIVATE).edit().putString("message",updated).apply();
                message.setText(updated);
            }).setNegativeButton("Отмена",null).show();
    }
    // TV input switching is vendor-dependent. Keep the manufacturer TV Center enabled.
    private void openInputPanel() {
        Intent panel = new Intent();
        panel.setClassName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.inputpanel.InputsPanelActivity");
        try { startActivity(panel); return; } catch (Exception ignored) {}
        // Some MiTV builds use a different activity namespace.
        panel.setClassName("com.mediatek.wwtv.tvcenter", "com.mitv.livetv.nav.inputpanel.InputsPanelActivity");
        try { startActivity(panel); return; } catch (Exception ignored) {}
        launchTvCenter();
    }
    private void launchTvCenter() {
        try {
            Intent tv = getPackageManager().getLaunchIntentForPackage("com.mediatek.wwtv.tvcenter");
            if (tv != null) { startActivity(tv); return; }
        } catch (Exception ignored) {}
        try {
            Intent tv = new Intent();
            tv.setClassName("com.mediatek.wwtv.tvcenter", "com.mitv.livetv.nav.TurnkeyUiMainActivity");
            startActivity(tv); return;
        } catch (Exception ignored) {}
        Toast.makeText(this, "Меню входов не найдено в этой прошивке", Toast.LENGTH_LONG).show();
    }
    private void chooseInput(String source) {
        // Xiaomi/MediaTek firmware-dependent intent; may be ignored on some builds.
        try {
            Intent select = new Intent("com.mediatek.select.source");
            select.putExtra("com.mediatek.select.sourcename", source);
            sendBroadcast(select);
        } catch (Exception ignored) {}
        launchTvCenter();
    }
    // Input states are reported by Android's TV Input Framework; device names depend on HDMI-CEC.
    private void showInputs() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> inputIds = new ArrayList<>();
        names.add("Открыть системное меню входов"); inputIds.add("");
        TvInputManager manager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        if (manager != null) {
            try {
                for (TvInputInfo item : manager.getTvInputList()) {
                    if (item.getType() != TvInputInfo.TYPE_HDMI && item.getType() != TvInputInfo.TYPE_TUNER) continue;
                    String name = item.loadLabel(MainActivity.this).toString();
                    int state = manager.getInputState(item.getId());
                    String status = state == TvInputManager.INPUT_STATE_CONNECTED ? "Сигнал/устройство обнаружено" :
                                    state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY ? "Режим ожидания" :
                                    "Нет соединения / неизвестно";
                    names.add(name + "  —  " + status);
                    inputIds.add(item.getId());
                }
            } catch (Exception e) { names.add("Данные HDMI недоступны на этой прошивке"); inputIds.add(""); }
        }
        if (names.size() == 1) {
            names.add("ТВ / Антенна — открыть через системное меню"); inputIds.add("");
            names.add("HDMI — открыть через системное меню"); inputIds.add("");
        }
        new AlertDialog.Builder(this)
           .setTitle("Источники · ТВ / HDMI")
           .setItems(names.toArray(new String[0]), (dialog, which) -> {
               // Vendor's source chooser is responsible for the actual switch.
               openInputPanel();
           })
           .setNegativeButton("Закрыть", null).show();
    }
    private void showHome() {
        if (appsContent != null) appsContent.setVisibility(View.GONE);
        if (homeContent != null) homeContent.setVisibility(View.VISIBLE);
        if (allAppsButton != null) allAppsButton.requestFocus();
    }
    private void showAllApps() {
        if (homeContent != null) homeContent.setVisibility(View.GONE);
        if (appsContent != null) root.removeView(appsContent);
        ScrollView scroll = new ScrollView(this);
        LinearLayout pane = new LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pane);
        appsContent = scroll;
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button back = tile("←  Назад на главный", Color.rgb(51, 76, 102));
        back.setOnClickListener(v -> showHome());
        pane.addView(back, new LinearLayout.LayoutParams(dp(290),dp(62)));
        TextView heading = text("ВСЕ ПРИЛОЖЕНИЯ", 24, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0,dp(18),0,dp(15));
        pane.addView(heading);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<ApplicationInfo> launchable = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ApplicationInfo app : apps) {
            if (app.packageName.equals(getPackageName())) continue;
            Intent intent = pm.getLeanbackLaunchIntentForPackage(app.packageName);
            if (intent == null) intent = pm.getLaunchIntentForPackage(app.packageName);
            if (intent != null && seen.add(app.packageName)) launchable.add(app);
        }
        Collections.sort(launchable, (a,b) ->
            String.CASE_INSENSITIVE_ORDER.compare(
                pm.getApplicationLabel(a).toString(),pm.getApplicationLabel(b).toString()));
        TextView count = text("Найдено приложений с возможностью запуска: " + launchable.size(), 15, MUTED);
        pane.addView(count);
        LinearLayout row = null;
        for (int n=0; n<launchable.size(); n++) {
            if (n % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(106));
                rp.topMargin=dp(13); pane.addView(row,rp);
            }
            ApplicationInfo app = launchable.get(n);
            LinearLayout tileBox = new LinearLayout(this);
            tileBox.setOrientation(LinearLayout.HORIZONTAL);
            tileBox.setGravity(Gravity.CENTER_VERTICAL);
            tileBox.setPadding(dp(12),dp(5),dp(12),dp(5));
            tileBox.setBackground(bg(Color.rgb(43,61,85),14));
            tileBox.setFocusable(true);
            ImageView img = new ImageView(this);
            try { img.setImageDrawable(pm.getApplicationIcon(app)); } catch(Exception ignored){}
            tileBox.addView(img,new LinearLayout.LayoutParams(dp(42),dp(42)));
            TextView name=text(pm.getApplicationLabel(app).toString(),15,WHITE);
            name.setMaxLines(2); name.setPadding(dp(10),0,0,0);
            tileBox.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            tileBox.setOnClickListener(v->launch(app.packageName));
            tileBox.setOnFocusChangeListener((v,focused)->{
                v.setBackground(bg(focused ? Color.rgb(72,109,149) : Color.rgb(43,61,85),14));
                v.animate().scaleX(focused?1.025f:1f).scaleY(focused?1.025f:1f).setDuration(100).start();
            });
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-1,1);
            tp.rightMargin=dp(10);
            row.addView(tileBox,tp);
        }
        back.requestFocus();
    }
    @Override public void onBackPressed() {
        if (movingTile!=null) cancelMove();
        else if (appsContent != null && appsContent.getVisibility() == View.VISIBLE) showHome();
        else super.onBackPressed();
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(54),dp(32),dp(54),dp(32));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
              new int[]{Color.rgb(17,25,39),Color.rgb(25,43,64),Color.rgb(12,19,30)}));
        setContentView(root);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        homeContent = scroll;
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header,new LinearLayout.LayoutParams(-1,dp(104)));
        LinearLayout headings = new LinearLayout(this); headings.setOrientation(LinearLayout.VERTICAL);
        header.addView(headings,new LinearLayout.LayoutParams(0,-2,1));
        TextView eyebrow=text("S I M P L E   T V   O S",13,MUTED); headings.addView(eyebrow);
        TextView title=text("Добрый вечер.",36,WHITE); title.setTypeface(null,Typeface.BOLD); headings.addView(title);
        clock=text("",25,WHITE); clock.setGravity(Gravity.RIGHT);
        header.addView(clock,new LinearLayout.LayoutParams(dp(185),-2));
        TextView settingsIcon = text("⚙", 29, WHITE);
        settingsIcon.setGravity(Gravity.CENTER);
        GradientDrawable settingsBackground = new GradientDrawable();
        settingsBackground.setShape(GradientDrawable.OVAL);
        settingsBackground.setColor(Color.rgb(47, 68, 94));
        settingsIcon.setBackground(settingsBackground);
        settingsIcon.setFocusable(true);
        settingsIcon.setContentDescription("Настройки телевизора");
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(55),dp(55));
        settingsParams.leftMargin = dp(22);
        header.addView(settingsIcon, settingsParams);
        settingsIcon.setOnFocusChangeListener((v,focused) ->
            v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(130).start());
        settingsIcon.setOnClickListener(v -> {
            v.animate().rotationBy(90f).setDuration(220).start();
            try {
                Intent settings = new Intent("android.settings.SETTINGS");
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settings);
            } catch (Exception e) {
                launch("com.android.tv.settings", "com.android.settings");
            }
        });
        message = text(getPreferences(MODE_PRIVATE).getString("message", "Твой телевизор. Твои правила."),20,WHITE);
        message.setGravity(Gravity.CENTER_VERTICAL); message.setPadding(dp(24),0,dp(24),0);
        message.setBackground(bg(Color.rgb(34,54,77),16));
        content.addView(message,new LinearLayout.LayoutParams(-1,dp(92)));
        TextView label=text("ПРИЛОЖЕНИЯ",15,MUTED); label.setPadding(0,dp(24),0,dp(12)); content.addView(label);
        loadFavorites();
        favoriteRow1=new LinearLayout(this); favoriteRow1.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(favoriteRow1,new LinearLayout.LayoutParams(-1,dp(100)));
        favoriteRow2=new LinearLayout(this); favoriteRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(-1,dp(100));
        second.topMargin=dp(12); content.addView(favoriteRow2,second);
        registerFavorite("youtube",tile(favoriteLabel("youtube"),favoriteColor("youtube"),"com.google.android.youtube.tv"));
        registerFavorite("zona",tile(favoriteLabel("zona"),favoriteColor("zona"),"mobi.zona"));
        registerFavorite("browser",tile(favoriteLabel("browser"),favoriteColor("browser"),"com.tcl.browser","com.internet.tvbrowser"));
        registerFavorite("vlc",tile(favoriteLabel("vlc"),favoriteColor("vlc"),"org.videolan.vlc"));
        allAppsButton=tile(favoriteLabel("apps"),favoriteColor("apps"));
        allAppsButton.setOnClickListener(v -> showAllApps());
        registerFavorite("apps",allAppsButton);
        Button happ=tile(favoriteLabel("happ"),favoriteColor("happ"));
        happ.setOnClickListener(v -> launchHapp());
        registerFavorite("happ",happ);
        renderFavorites();
        Button edit=tile("✎  Изменить текст",Color.rgb(46,61,84));
        edit.setOnClickListener(v -> editMessage());
        LinearLayout.LayoutParams editParams=new LinearLayout.LayoutParams(dp(240),dp(55)); editParams.topMargin=dp(21);
        content.addView(edit,editParams);
        // Small source selector is placed at the bottom, not among favorite apps.
        LinearLayout footerActions = new LinearLayout(this);
        footerActions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, dp(50));
        footerParams.topMargin = dp(14);
        content.addView(footerActions, footerParams);
        Button sourcesButton = tile("▣  Входы  ·  ТВ / HDMI", Color.rgb(44, 61, 82));
        sourcesButton.setTextSize(13);
        sourcesButton.setOnClickListener(v -> showInputs());
        footerActions.addView(sourcesButton, new LinearLayout.LayoutParams(dp(245), dp(44)));
        TextView foot=text("HAPP: подписку добавим позже  •  Стрелки пульта + OK",13,MUTED);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2); fp.topMargin=dp(16);content.addView(foot,fp);
        handler.post(new Runnable(){ public void run(){
          if(clock!=null) clock.setText(new SimpleDateFormat("HH:mm  ·  dd MMM",new Locale("ru","RU")).format(new Date()));
          handler.postDelayed(this,30000);
        }});
        favoriteRow1.getChildAt(0).requestFocus();
    }
    private void addTile(LinearLayout row, Button b) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.rightMargin=dp(12); row.addView(b,p);
    }
    @Override protected void onDestroy(){ super.onDestroy(); handler.removeCallbacksAndMessages(null); }
}
