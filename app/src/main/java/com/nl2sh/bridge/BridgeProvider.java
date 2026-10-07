package com.nl2sh.bridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import org.json.JSONException;
import org.json.JSONObject;

/** Binder entry point restricted to Android shell/root callers. */
public final class BridgeProvider extends ContentProvider {
    static final int MAX_REPLY = 64 * 1024;

    /** Reverse the caller's extra-value encoding: '%3A' -> ':' and '%25' -> '%'. */
    private static String decode(String value) {
        return value.replace("%3A", ":").replace("%25", "%");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        if (uid != 0 && uid != 2000) {
            throw new SecurityException("nl2sh bridge requires shell or root UID");
        }
        JSONObject request = new JSONObject();
        JSONObject reply;
        String requestId = "";
        boolean envelope = "invoke".equals(method);
        try {
            if (envelope) {
                String payload = extras == null ? "" : extras.getString("payload_b64", "");
                if (payload.isEmpty() || payload.length() > 32 * 1024
                        || !payload.matches("[A-Za-z0-9_-]+")) {
                    throw new JSONException("payload exceeds size limit");
                }
                JSONObject decoded = new JSONObject(StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(Base64.decode(payload,
                                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP))).toString());
                Object id = decoded.get("request_id");
                if (!(id instanceof String)) { throw new JSONException("invalid request ID"); }
                requestId = (String) id;
                if (requestId.isEmpty() || requestId.length() > 128
                        || !Integer.valueOf(BridgeCapabilities.PROTOCOL).equals(decoded.get("protocol"))) {
                    throw new JSONException("invalid protocol or request ID");
                }
                Object action = decoded.get("method");
                if (!(action instanceof String)) { throw new JSONException("invalid method"); }
                method = (String) action;
                if ("invoke".equals(method)) { throw new JSONException("nested invoke"); }
                request = decoded.getJSONObject("args");
            }
            request.put("action", method);
            if (!envelope && extras != null) {
                request.put("text", decode(extras.getString("text", "")));
                request.put("text_b64", decode(extras.getString("text_b64", "")));
                request.put("mode", decode(extras.getString("mode", "")));
                request.put("field_id", decode(extras.getString("field_id", "")));
                request.put("package", decode(extras.getString("package", "")));
                request.put("bounds", decode(extras.getString("bounds", "")));
                request.put("class", decode(extras.getString("class", "")));
                request.put("resource_id", decode(extras.getString("resource_id", "")));
                request.put("node_text", decode(extras.getString("node_text", "")));
                request.put("description", decode(extras.getString("description", "")));
                request.put("text_hash", decode(extras.getString("text_hash", "")));
                request.put("description_hash", decode(extras.getString("description_hash", "")));
                for (String coordinate : new String[] {"x", "y", "end_x", "end_y", "duration_ms"}) {
                    request.put(coordinate, decode(extras.getString(coordinate, "")));
                }
            }
            // IME actions stay available when only the keyboard is enabled: the input method is
            // an independent service with its own approval-free authority, so it is never
            // dispatched through the Accessibility service.
            reply = "capabilities".equals(method)
                    ? BridgeCapabilities.snapshot(getContext())
                    : method.startsWith("ime_")
                    ? BridgeImeService.dispatch(request, true)
                    : BridgeService.dispatch(request);
        } catch (JSONException | IllegalArgumentException | CharacterCodingException failure) {
            reply = BridgeService.error("invalid request");
        }
        if (envelope) {
            reply = envelope(requestId, reply);
        }
        byte[] encoded = reply.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_REPLY) {
            JSONObject failure = BridgeService.error("reply exceeds size limit");
            encoded = (envelope ? envelope(requestId, failure) : failure)
                    .toString().getBytes(StandardCharsets.UTF_8);
        }
        Bundle result = new Bundle();
        result.putString("data", Base64.encodeToString(encoded,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
        return result;
    }

    private static JSONObject envelope(String requestId, JSONObject result) {
        try {
            boolean ok = result.optBoolean("ok", false);
            JSONObject reply = new JSONObject().put("protocol", BridgeCapabilities.PROTOCOL)
                    .put("request_id", requestId).put("ok", ok);
            return ok ? reply.put("result", result)
                    : reply.put("error", result.optString("error", "action failed"));
        } catch (JSONException failure) {
            return BridgeService.error("cannot encode reply");
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
