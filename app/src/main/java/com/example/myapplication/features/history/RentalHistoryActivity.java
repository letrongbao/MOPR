package com.example.myapplication.features.history;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.RentalHistoryRepository;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.RentalHistory;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RentalHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RentalHistoryAdapter adapter;
    private LinearProgressIndicator progressBar;
    private TextView tvEmpty, tvContractCount;
    private MaterialButton btnSort;

    private RentalHistoryRepository repository;
    private List<RentalHistory> historyList = new ArrayList<>();

    private enum SortOption {
        DATE_NEWEST, DATE_OLDEST, NAME_AZ, ROOM_ASC
    }

    private SortOption currentSort = SortOption.DATE_NEWEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_rental_history);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Lịch sử cho thuê");

        repository = new RentalHistoryRepository();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvContractCount = findViewById(R.id.tvContractCount);
        btnSort = findViewById(R.id.btnSort);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RentalHistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        btnSort.setOnClickListener(v -> showSortDialog());

        loadHistory();
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        repository.getAllHistory()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    historyList.clear();
                    querySnapshot.getDocuments().forEach(doc -> {
                        RentalHistory history = doc.toObject(RentalHistory.class);
                        if (history != null) {
                            history.setId(doc.getId());
                            historyList.add(history);
                        }
                    });

                    if (historyList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvContractCount.setVisibility(View.GONE);
                        btnSort.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        tvContractCount.setVisibility(View.VISIBLE);
                        btnSort.setVisibility(View.VISIBLE);
                        tvContractCount.setText("Số hợp đồng: " + historyList.size());
                        applySorting(currentSort);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSortDialog() {
        String[] options = {
                "Ngày kết thúc (mới nhất)",
                "Ngày kết thúc (cũ nhất)",
                "Tên người thuê (A-Z)",
                "Phòng (tăng dần)"
        };

        int checkedItem = currentSort.ordinal();

        new AlertDialog.Builder(this)
                .setTitle("Sắp xếp theo")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    SortOption selected = SortOption.values()[which];
                    applySorting(selected);
                    currentSort = selected;
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applySorting(SortOption option) {
        switch (option) {
            case DATE_NEWEST:
                Collections.sort(historyList, (a, b) -> Long.compare(b.getEndTimestamp(), a.getEndTimestamp()));
                break;
            case DATE_OLDEST:
                Collections.sort(historyList, (a, b) -> Long.compare(a.getEndTimestamp(), b.getEndTimestamp()));
                break;
            case NAME_AZ:
                Collections.sort(historyList, (a, b) -> {
                    String nameA = a.getHoTen() != null ? a.getHoTen() : "";
                    String nameB = b.getHoTen() != null ? b.getHoTen() : "";
                    return nameA.compareToIgnoreCase(nameB);
                });
                break;
            case ROOM_ASC:
                Collections.sort(historyList, (a, b) -> {
                    String roomA = a.getSoPhong() != null ? a.getSoPhong() : "";
                    String roomB = b.getSoPhong() != null ? b.getSoPhong() : "";
                    return roomA.compareToIgnoreCase(roomB);
                });
                break;
        }
        adapter.updateData(historyList);
    }
}
