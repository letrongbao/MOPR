package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class TenantSession {
    private TenantSession() {}

    private static final String PREFS = "NhaTroPrefs";
    private static final String KEY_ACTIVE_TENANT_ID = "activeTenantId";

    private static volatile String cachedTenantId;

    public static void init(Context context) {
        if (cachedTenantId != null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cachedTenantId = prefs.getString(KEY_ACTIVE_TENANT_ID, null);
    }

    public static @Nullable String getActiveTenantId() {
        return cachedTenantId;
    }

    public static void setActiveTenantId(Context context, String tenantId) {
        cachedTenantId = tenantId;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_TENANT_ID, tenantId).apply();
    }

    public static void clear(Context context) {
        cachedTenantId = null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ACTIVE_TENANT_ID).apply();
    }
}
