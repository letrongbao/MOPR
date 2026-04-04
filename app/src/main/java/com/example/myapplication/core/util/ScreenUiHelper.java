package com.example.myapplication.core.util;

import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class ScreenUiHelper {

    private ScreenUiHelper() {
    }

    public static void enableEdgeToEdge(@NonNull AppCompatActivity activity, boolean lightStatusBarIcons) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(lightStatusBarIcons);
        }
    }

    public static void applyTopInset(@NonNull View target) {
        ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    public static void setupBackToolbar(@NonNull AppCompatActivity activity,
            @NonNull Toolbar toolbar,
            @Nullable String title) {
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (title != null) {
                activity.getSupportActionBar().setTitle(title);
            }
        }
        toolbar.setNavigationOnClickListener(v -> activity.finish());
    }
}
