package com.example.myapplication.features.home;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

/**
 * Màn hình Chốt đồng hồ - Khách thuê nhập số đồng hồ điện/nước.
 * TODO: Implement UI và logic nhập chỉ số đồng hồ.
 */
public class ConfirmMeterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: setContentView(R.layout.activity_confirm_meter);
        Toast.makeText(this, getString(R.string.confirm_meter_feature_in_development), Toast.LENGTH_SHORT).show();
        finish();
    }
}
