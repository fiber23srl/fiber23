package it.fiber23.cloud;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int REQ_MEDIA = 23;
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(SyncWorker.PREFS, Context.MODE_PRIVATE);
        if (prefs.getString(SyncWorker.KEY_API_URL, "").isEmpty()) {
            prefs.edit().putString(SyncWorker.KEY_API_URL, ApiClient.DEFAULT_API_URL).apply();
        }
        show();
    }

    private void show() {
        if (prefs.getString(SyncWorker.KEY_TOKEN, "").isEmpty()) {
            showLogin();
        } else {
            showDashboard();
        }
    }

    private void showLogin() {
        root = baseRoot();
        addLogo(root);
        TextView title = title("Accedi al tuo cloud");
        root.addView(title);

        EditText apiUrl = input("API cloud", false);
        apiUrl.setText(prefs.getString(SyncWorker.KEY_API_URL, ApiClient.DEFAULT_API_URL));
        EditText email = input("Email", false);
        EditText password = input("Password", true);
        Button login = primaryButton("Accedi");

        root.addView(apiUrl);
        root.addView(email);
        root.addView(password);
        root.addView(login);

        login.setOnClickListener(v -> {
            login.setEnabled(false);
            new Thread(() -> {
                try {
                    ApiClient api = new ApiClient(apiUrl.getText().toString(), "");
                    JSONObject response = api.login(email.getText().toString(), password.getText().toString(), Build.MODEL);
                    prefs.edit()
                            .putString(SyncWorker.KEY_API_URL, apiUrl.getText().toString())
                            .putString(SyncWorker.KEY_TOKEN, response.getString("token"))
                            .apply();
                    runOnUiThread(this::showDashboard);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        login.setEnabled(true);
                        toast(e.getMessage());
                    });
                }
            }).start();
        });
        setContentView(wrap(root));
    }

    private void showDashboard() {
        root = baseRoot();
        addLogo(root);
        root.addView(title("Backup foto e video"));
        statusText = text("Carico il tuo spazio cloud...");
        root.addView(statusText);

        Button syncNow = primaryButton("Sincronizza ora");
        Button refresh = secondaryButton("Aggiorna stato");
        CheckBox autoSync = new CheckBox(this);
        autoSync.setText("Sincronizzazione automatica ogni 6 ore");
        autoSync.setTextColor(Color.rgb(17, 31, 46));
        autoSync.setChecked(prefs.getBoolean("auto_sync", false));

        root.addView(syncNow);
        root.addView(refresh);
        root.addView(autoSync);
        root.addView(text("L'app salva foto e video nella cartella Backup telefono del tuo Fiber23 Cloud."));

        Button logout = secondaryButton("Esci dall'app");
        root.addView(logout);

        syncNow.setOnClickListener(v -> {
            if (!hasMediaPermission()) {
                requestMediaPermission();
                return;
            }
            enqueueSyncNow();
            toast("Sincronizzazione avviata. Puoi lasciare l'app aperta o tornare piu tardi.");
        });
        refresh.setOnClickListener(v -> loadStatus());
        autoSync.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("auto_sync", checked).apply();
            if (checked) {
                if (!hasMediaPermission()) requestMediaPermission();
                scheduleAutoSync();
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("fiber23-auto-sync");
            }
        });
        logout.setOnClickListener(v -> {
            prefs.edit().remove(SyncWorker.KEY_TOKEN).remove(SyncWorker.KEY_LAST_SYNC).apply();
            WorkManager.getInstance(this).cancelUniqueWork("fiber23-auto-sync");
            showLogin();
        });
        setContentView(wrap(root));
        loadStatus();
    }

    private void loadStatus() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(prefs.getString(SyncWorker.KEY_API_URL, ApiClient.DEFAULT_API_URL), prefs.getString(SyncWorker.KEY_TOKEN, ""));
                JSONObject user = api.me().getJSONObject("user");
                long used = user.getLong("used_bytes");
                long remaining = user.getLong("remaining_bytes");
                int quota = user.getInt("quota_gb");
                String message = "Totale: " + quota + " GB\nUsato: " + ApiClient.humanBytes(used) + "\nDisponibile: " + ApiClient.humanBytes(remaining);
                String last = prefs.getString(SyncWorker.KEY_LAST_MESSAGE, "");
                if (!last.isEmpty()) message += "\nUltima sync: " + last;
                String finalMessage = message;
                runOnUiThread(() -> statusText.setText(finalMessage));
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Errore: " + e.getMessage()));
            }
        }).start();
    }

    private void enqueueSyncNow() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void scheduleAutoSync() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 6, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("fiber23-auto-sync", ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, REQ_MEDIA);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_MEDIA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MEDIA && hasMediaPermission()) {
            toast("Permesso galleria attivo.");
        }
    }

    private LinearLayout baseRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(28), dp(22), dp(28));
        layout.setBackgroundColor(Color.rgb(244, 248, 251));
        return layout;
    }

    private ScrollView wrap(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(child);
        return scroll;
    }

    private void addLogo(LinearLayout parent) {
        TextView logo = new TextView(this);
        logo.setText("F23  Fiber23 Cloud");
        logo.setTextColor(Color.rgb(17, 31, 46));
        logo.setTextSize(20);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        logo.setPadding(0, 0, 0, dp(28));
        parent.addView(logo);
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(17, 31, 46));
        view.setTextSize(30);
        view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        view.setPadding(0, 0, 0, dp(16));
        return view;
    }

    private TextView text(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(75, 92, 112));
        view.setTextSize(16);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setMinHeight(dp(52));
        if (password) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setBackgroundColor(Color.rgb(17, 31, 46));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(17, 31, 46));
        button.setTextSize(16);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "" : message, Toast.LENGTH_LONG).show();
    }
}
