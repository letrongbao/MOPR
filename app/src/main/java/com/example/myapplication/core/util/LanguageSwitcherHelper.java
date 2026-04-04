package com.example.myapplication.core.util;

import android.app.Activity;
import android.view.View;
import android.animation.ObjectAnimator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class LanguageSwitcherHelper {

    private final AppCompatActivity activity;
    private View btnLangEn;
    private View btnLangVi;

    public LanguageSwitcherHelper(AppCompatActivity activity) {
        this.activity = activity;
    }

    /**
     * Setup language switcher with buttons from the include component.
     * Call this after setContentView() and after finding the container.
     */
    public void setupLanguageSwitcher() {
        // Find buttons from the include component
        btnLangEn = activity.findViewById(R.id.btnLangEn);
        btnLangVi = activity.findViewById(R.id.btnLangVi);

        refreshLanguageButtons();

        if (btnLangEn != null) {
            btnLangEn.setOnClickListener(v -> switchLanguageWithAnimation("en", v));
        }
        if (btnLangVi != null) {
            btnLangVi.setOnClickListener(v -> switchLanguageWithAnimation("vi", v));
        }
    }

    /**
     * Switch language with a smooth scale animation
     */
    private void switchLanguageWithAnimation(String languageTag, View view) {
        if (LanguageManager.isCurrentLanguage(activity, languageTag)) {
            return;
        }

        // Scale animation on click
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.start();
        scaleY.start();

        // Switch language
        LanguageManager.setLanguage(activity, languageTag);
        activity.recreate();
    }

    /**
     * Refresh language button styling (opacity) based on current selection
     */
    private void refreshLanguageButtons() {
        boolean isEnglish = LanguageManager.isCurrentLanguage(activity, "en");

        if (btnLangEn != null) {
            // Selected button: full opacity, unselected: semi-transparent
            btnLangEn.setAlpha(isEnglish ? 1f : 0.5f);
        }
        if (btnLangVi != null) {
            btnLangVi.setAlpha(isEnglish ? 0.5f : 1f);
        }
    }

    /**
     * Call this if you need to refresh button state (e.g., after orientation
     * change)
     */
    public void refresh() {
        setupLanguageSwitcher();
    }
}
