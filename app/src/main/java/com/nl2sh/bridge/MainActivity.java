package com.nl2sh.bridge;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private TextView keyboardStatus;
    private TextView diagnostics;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(Gravity.TOP);
        diagnostics = new TextView(this);
        diagnostics.setTextIsSelectable(true);
        layout.addView(diagnostics);
        Button refresh = new Button(this);
        refresh.setText(R.string.refresh_diagnostics);
        refresh.setOnClickListener(ignored -> refreshDiagnostics());
        layout.addView(refresh);
        TextView explanation = new TextView(this);
        explanation.setText(R.string.bridge_explanation);
        layout.addView(explanation);
        Button settings = new Button(this);
        settings.setText(R.string.open_accessibility_settings);
        settings.setOnClickListener((View ignored) -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(settings);
        TextView keyboardExplanation = new TextView(this);
        keyboardExplanation.setText(R.string.ime_explanation);
        layout.addView(keyboardExplanation);
        keyboardStatus = new TextView(this);
        layout.addView(keyboardStatus);
        Button keyboardSettings = new Button(this);
        keyboardSettings.setText(R.string.open_keyboard_settings);
        keyboardSettings.setOnClickListener((View ignored) -> startActivity(
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        layout.addView(keyboardSettings);
        EditText testInput = new EditText(this);
        testInput.setHint(R.string.test_input_hint);
        testInput.setSingleLine(false);
        layout.addView(testInput);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (keyboardStatus != null) {
            keyboardStatus.setText(isBridgeKeyboardSelected()
                    ? R.string.ime_status_active : R.string.ime_status_inactive);
        }
        refreshDiagnostics();
    }

    /** Read the selected input method so the screen states which keyboard actually types. */
    private boolean isBridgeKeyboardSelected() {
        String selected = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return new ComponentName(this, BridgeImeService.class).equals(
                selected == null ? null : ComponentName.unflattenFromString(selected));
    }

    private void refreshDiagnostics() {
        if (diagnostics == null) { return; }
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean accessibilityEnabled = false;
        ComponentName service = new ComponentName(this, BridgeService.class);
        if (enabled != null) {
            for (String entry : enabled.split(":")) {
                accessibilityEnabled |= service.equals(ComponentName.unflattenFromString(entry));
            }
        }
        accessibilityEnabled &= Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        boolean keyboardEnabled = false;
        if (manager != null) {
            ComponentName keyboard = new ComponentName(this, BridgeImeService.class);
            for (InputMethodInfo info : manager.getEnabledInputMethodList()) {
                keyboardEnabled |= keyboard.equals(ComponentName.unflattenFromString(info.getId()));
            }
        }
        try {
            JSONObject capabilities = BridgeCapabilities.snapshot(this);
            diagnostics.setText(getString(R.string.diagnostic_summary,
                    BuildConfig.VERSION_NAME, BridgeCapabilities.PROTOCOL,
                    state(accessibilityEnabled), state(BridgeService.isRunning()),
                    state(keyboardEnabled), state(isBridgeKeyboardSelected()),
                    state(BridgeImeService.isRunning()),
                    state(getPackageManager().resolveContentProvider("com.nl2sh.bridge.ops", 0) != null),
                    capabilities.getJSONArray("capabilities").toString()));
        } catch (JSONException failure) {
            diagnostics.setText(R.string.diagnostics_unavailable);
        }
    }

    private String state(boolean available) {
        return getString(available ? R.string.diagnostic_yes : R.string.diagnostic_no);
    }
}
