package com.example.myapplication.core.util;

import android.content.Context;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.List;

public final class AuthProviderUtil {

    private static final String PROVIDER_PASSWORD = "password";
    private static final String PROVIDER_GOOGLE = "google.com";

    private AuthProviderUtil() {
        // Utility class.
    }

    public static boolean canChangePassword(FirebaseUser user) {
        return hasProvider(user, PROVIDER_PASSWORD);
    }

    public static String resolveLoginMethodLabel(Context context, FirebaseUser user) {
        boolean hasGoogle = hasProvider(user, PROVIDER_GOOGLE);
        boolean hasPassword = hasProvider(user, PROVIDER_PASSWORD);

        if (hasGoogle && hasPassword) {
            return context.getString(R.string.auth_method_google_and_password);
        }
        if (hasGoogle) {
            return context.getString(R.string.auth_method_google);
        }
        if (hasPassword) {
            return context.getString(R.string.auth_method_password);
        }
        return context.getString(R.string.auth_method_unknown);
    }

    private static boolean hasProvider(FirebaseUser user, String providerId) {
        if (user == null) {
            return false;
        }

        List<? extends UserInfo> providers = user.getProviderData();
        if (providers == null || providers.isEmpty()) {
            return false;
        }

        for (UserInfo info : providers) {
            if (info != null && providerId.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }
}
