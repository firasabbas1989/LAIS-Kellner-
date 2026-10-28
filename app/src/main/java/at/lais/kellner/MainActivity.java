package at.lais.kellner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LAIS Kellner v2 – Hülle (WebView) um die Kellner-App der LAIS-Kasse.
 *  - findet die Kasse automatisch im WLAN (kein IP-Tippen nötig)
 *  - merkt sich die Kasse, startet direkt in die Kellner-App
 *  - schöne Offline-Seite mit automatischem Wiederverbinden
 *  - Display bleibt an, Splash-Screen, adaptives Icon
 *  - Zurück-Taste: Menü (Neu laden / Kasse suchen / Beenden)
 */
public class MainActivity extends Activity {
    private static final int BG = 0xFF071A1F, CARD = 0xFF0F2A30, ACC = 0xFFFF6B57, MUTE = 0xFF9FB8BE;
    private static final int[] PORTS = {3000, 3001, 3002};

    private SharedPreferences sp;
    private FrameLayout root;
    private WebView web;
    private volatile boolean scanning = false;
    private volatile int scanToken = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("lais", MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        setContentView(root);
        getWindow().setBackgroundDrawable(new ColorDrawable(BG));
        String addr = sp.getString("addr", "");
        if (addr.isEmpty()) showSetup(true); else showWeb(addr);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private Button button(String text, int color, int textColor) {
        Button bt = new Button(this);
        bt.setText(text); bt.setTextSize(17); bt.setAllCaps(false); bt.setTextColor(textColor);
        bt.setTypeface(Typeface.DEFAULT_BOLD);
        bt.setBackground(round(color, 16));
        return bt;
    }

    private TextView text(String s, int sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sizeSp); t.setTextColor(color); t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    // ===================== Einrichtung / Suche =====================
    private void showSetup(boolean autoScan) {
        web = null;
        scanToken++;
        root.removeAllViews();
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(dp(24), dp(40), dp(24), dp(24));
        sv.addView(l, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        l.addView(logo, new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView t = text("LAIS Kellner", 30, Color.WHITE, true);
        t.setPadding(0, dp(12), 0, 0);
        l.addView(t);
        TextView sub = text("Verbinde dich mit deiner Kasse\nاتصل بكاشير المطعم", 15, MUTE, false);
        sub.setPadding(0, dp(6), 0, dp(18));
        l.addView(sub);

        final TextView status = text("", 15, Color.WHITE, false);
        status.setPadding(0, 0, 0, dp(8));
        l.addView(status);
        final LinearLayout found = new LinearLayout(this);
        found.setOrientation(LinearLayout.VERTICAL);
        l.addView(found, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Button search = button("🔍  Kasse automatisch suchen / بحث تلقائي", ACC, Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.topMargin = dp(8);
        l.addView(search, lp);
        search.setOnClickListener(v -> scan(status, found, search));

        TextView or = text("oder Adresse selbst eingeben / أو أدخل العنوان يدوياً", 13, MUTE, false);
        or.setPadding(0, dp(24), 0, dp(8));
        l.addView(or);
        final EditText e = new EditText(this);
        e.setHint("192.168.1.20");
        e.setHintTextColor(0xFF6F8A90);
        e.setText(sp.getString("addr", ""));
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        e.setTextColor(Color.WHITE); e.setTextSize(19);
        e.setBackground(round(CARD, 14));
        e.setPadding(dp(16), dp(12), dp(16), dp(12));
        l.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        Button go = button("Verbinden / اتصال", CARD, Color.WHITE);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp2.topMargin = dp(10);
        l.addView(go, lp2);
        go.setOnClickListener(v -> {
            String a = e.getText().toString().trim();
            if (a.isEmpty()) { Toast.makeText(this, "Bitte Adresse eingeben", Toast.LENGTH_SHORT).show(); return; }
            connect(a);
        });
        TextView tip = text("Handy und Kasse müssen im selben WLAN sein.\nلازم الموبايل والكاشير على نفس الواي فاي.", 12, MUTE, false);
        tip.setPadding(0, dp(22), 0, 0);
        l.addView(tip);

        if (autoScan) scan(status, found, search);
    }

    private void connect(String a) {
        scanToken++;
        sp.edit().putString("addr", a).apply();
        showWeb(a);
    }

    /** Alle IPv4-Netze (x.y.z.) des Geräts. */
    private List<String> subnets() {
        List<String> out = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(en)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
                    if (ia instanceof Inet4Address && ia.isSiteLocalAddress()) {
                        String h = ia.getHostAddress();
                        String base = h.substring(0, h.lastIndexOf('.') + 1);
                        if (!out.contains(base)) out.add(base);
                    }
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private String probe(String ip) {
        for (int port : PORTS) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(ip, port), 350);
            } catch (Exception e) { continue; }
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("http://" + ip + ":" + port + "/manifest.json").openConnection();
                c.setConnectTimeout(900); c.setReadTimeout(1500);
                InputStream in = c.getInputStream();
                byte[] buf = new byte[2048];
                int n = in.read(buf);
                in.close();
                if (n > 0 && new String(buf, 0, n).contains("LAIS")) return ip + ":" + port;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private void scan(final TextView status, final LinearLayout found, final Button search) {
        if (scanning) return;
        final List<String> bases = subnets();
        found.removeAllViews();
        if (bases.isEmpty()) { status.setText("Kein WLAN gefunden – bitte mit dem WLAN verbinden\nلا يوجد واي فاي"); return; }
        scanning = true;
        final int token = ++scanToken;
        status.setText("Suche Kasse im WLAN … / جاري البحث");
        search.setEnabled(false); search.setAlpha(0.5f);
        new Thread(() -> {
            ExecutorService ex = Executors.newFixedThreadPool(64);
            final List<String> hits = Collections.synchronizedList(new ArrayList<String>());
            for (String base : bases) {
                for (int i = 1; i < 255; i++) {
                    final String ip = base + i;
                    ex.submit(() -> {
                        if (token != scanToken) return;
                        String r = probe(ip);
                        if (r != null && !hits.contains(r)) {
                            hits.add(r);
                            runOnUiThread(() -> { if (token == scanToken) addFound(found, r); });
                        }
                    });
                }
            }
            ex.shutdown();
            try { ex.awaitTermination(40, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            scanning = false;
            runOnUiThread(() -> {
                search.setEnabled(true); search.setAlpha(1f);
                if (token != scanToken) return;
                if (hits.isEmpty()) status.setText("Keine Kasse gefunden. Läuft die Kasse? Kellner-App aktiv?\nما لقينا الكاشير – تأكد أنه شغال");
                else if (hits.size() == 1) { status.setText("✔ Kasse gefunden – verbinde …"); connect(hits.get(0)); }
                else status.setText("Mehrere Kassen gefunden – bitte wählen / اختر الكاشير");
            });
        }).start();
    }

    private void addFound(LinearLayout found, final String addr) {
        Button bt = button("🖥  Kasse " + addr, CARD, Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.bottomMargin = dp(8);
        found.addView(bt, lp);
        bt.setOnClickListener(v -> connect(addr));
    }

    // ===================== Kellner-App =====================
    private String url(String a) {
        a = a.trim();
        if (!a.startsWith("http")) a = "http://" + a;
        String rest = a.substring(a.indexOf("//") + 2);
        if (!rest.contains(":") && !rest.contains("/")) a += ":3000";
        return a;
    }

    private class Bridge {
        @JavascriptInterface public void retry() { runOnUiThread(() -> { String a = sp.getString("addr", ""); if (!a.isEmpty()) showWeb(a); }); }
        @JavascriptInterface public void search() { runOnUiThread(() -> showSetup(true)); }
        @JavascriptInterface public void exit() { runOnUiThread(() -> finish()); }
    }

    private void showWeb(String addr) {
        scanToken++;
        root.removeAllViews();
        final String target = url(addr);
        web = new WebView(this);
        web.setBackgroundColor(BG);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setBuiltInZoomControls(false);
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError er) {
                if (r.isForMainFrame()) offline(target, String.valueOf(er.getDescription()));
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.loadUrl(target);
    }

    /** Offline-Seite: verbindet automatisch neu, sobald die Kasse wieder erreichbar ist. */
    private void offline(String target, String err) {
        if (web == null) return;
        String esc = err.replace("<", "&lt;").replace(">", "&gt;");
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"
            + "body{margin:0;background:#071a1f;color:#fff;font-family:sans-serif;text-align:center;padding:60px 24px}"
            + ".dot{width:18px;height:18px;border-radius:50%;background:#ff6b57;margin:0 auto 22px;animation:p 1.2s infinite}"
            + "@keyframes p{0%,100%{opacity:.2;transform:scale(.7)}50%{opacity:1;transform:scale(1.2)}}"
            + "h1{font-size:26px;margin:0 0 8px}p{color:#9fb8be;font-size:16px;line-height:1.5}"
            + "button{display:block;width:100%;max-width:340px;margin:12px auto 0;padding:16px;border:0;border-radius:16px;font-size:18px;font-weight:700;color:#fff;background:#0f2a30}"
            + "button.a{background:#ff6b57}small{display:block;margin-top:26px;color:#5d777d;font-size:12px}</style></head><body>"
            + "<div class='dot'></div><h1>Keine Verbindung</h1><h1 dir='rtl'>لا يوجد اتصال</h1>"
            + "<p>Die Kasse ist nicht erreichbar.<br>Prüfe WLAN und ob die Kasse läuft.<br>Es wird automatisch neu verbunden …</p>"
            + "<button class='a' onclick='AndroidApp.retry()'>Erneut versuchen / إعادة المحاولة</button>"
            + "<button onclick='AndroidApp.search()'>Kasse suchen / بحث عن الكاشير</button>"
            + "<small>" + esc + "</small>"
            + "<script>setInterval(function(){fetch('/manifest.json',{cache:'no-store'}).then(function(r){if(r.ok)location.replace('/')}).catch(function(){})},3000)</script>"
            + "</body></html>";
        web.loadDataWithBaseURL(target.replaceAll("/+$", "") + "/", html, "text/html", "utf-8", null);
    }

    @Override public boolean onKeyDown(int code, KeyEvent ev) {
        if (code == KeyEvent.KEYCODE_BACK) {
            if (web == null) { finish(); return true; }
            new AlertDialog.Builder(this).setTitle("LAIS Kellner")
                .setItems(new String[]{"Neu laden / تحديث", "Kasse suchen / ändern · تغيير الكاشير", "Beenden / خروج"}, (d, i) -> {
                    if (i == 0 && web != null) web.reload();
                    else if (i == 1) showSetup(false);
                    else if (i == 2) finish();
                }).setNegativeButton("Zurück", null).show();
            return true;
        }
        return super.onKeyDown(code, ev);
    }

    @Override protected void onPause() { super.onPause(); if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() { scanToken++; super.onDestroy(); }
}
