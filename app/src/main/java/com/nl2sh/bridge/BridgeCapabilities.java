package com.nl2sh.bridge;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Versioned, read-only discovery independent of enabled services. */
final class BridgeCapabilities {
    static final int PROTOCOL = BuildConfig.BRIDGE_PROTOCOL_VERSION;

    private BridgeCapabilities() { }

    static JSONObject snapshot(Context context) throws JSONException {
        boolean accessibility = BridgeService.isRunning();
        boolean ime = BridgeImeService.isRunning();
        JSONArray capabilities = new JSONArray();
        if (accessibility) {
            for (String feature : new String[] {"ui.tree.v2", "ui.tap.semantic.v2",
                    "ui.gesture.v1", "ui.input.v1", "clipboard.paste.v1"}) {
                capabilities.put(feature);
            }
        }
        if (ime) { capabilities.put("ime.commit.v1"); }
        long build = 0;
        if (context != null) {
            try {
                PackageInfo info = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0);
                build = info.lastUpdateTime;
            } catch (PackageManager.NameNotFoundException ignored) {
                // Version remains available if package metadata cannot be read.
            }
        }
        return new JSONObject().put("ok", true).put("protocol_version", PROTOCOL)
                .put("app_version", BuildConfig.VERSION_NAME)
                .put("build", BuildConfig.VERSION_CODE).put("installed_at", build)
                .put("services", new JSONObject().put("accessibility", accessibility).put("ime", ime))
                .put("capabilities", capabilities)
                .put("limits", new JSONObject().put("tree_nodes", 250)
                        .put("reply_bytes", BridgeProvider.MAX_REPLY)
                        .put("input_text_bytes", 4096).put("payload_b64_bytes", 32 * 1024));
    }
}
