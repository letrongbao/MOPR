package com.example.myapplication;

import android.os.Bundle;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    GridView gridView;

    String[] items = {
            "Phòng trọ",
            "Người thuê",
            "Hóa đơn",
            "Doanh thu"
    };

    int[] images = {
            R.drawable.baseline_home_24,
            R.drawable.baseline_person_24,
            R.drawable.baseline_receipt_24,
            R.drawable.baseline_bar_chart_24
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        gridView = findViewById(R.id.gridView);

        GridAdapter adapter = new GridAdapter(this, items, images);
        gridView.setAdapter(adapter);
    }
}