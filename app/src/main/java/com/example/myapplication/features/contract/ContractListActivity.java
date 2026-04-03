package com.example.myapplication.features.contract;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.TenantRepository;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.appbar.AppBarLayout;

import java.util.List;

public class ContractListActivity extends AppCompatActivity {

    private static final String TAG = "ContractListActivity";

    private ContractListAdapter adapter;
    private TenantRepository repo;

    private TextView tvCountDangThue, tvCountSapHet, tvCountKetThuc;
    private LinearLayout layoutEmpty;
    private RecyclerView rvHopDong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_contract_list);

        // Xử lý padding cho AppBarLayout để không bị lẹm thanh thông báo
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        // Setup Toolbar và nút Back đồng nhất
        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Hợp đồng thông minh");

        // Ánh xạ các View còn lại
        tvCountDangThue = findViewById(R.id.tvCountDangThue);
        tvCountSapHet = findViewById(R.id.tvCountSapHet);
        tvCountKetThuc = findViewById(R.id.tvCountKetThuc);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvHopDong = findViewById(R.id.rvHopDong);

        // RecyclerView + Adapter
        adapter = new ContractListAdapter();
        rvHopDong.setLayoutManager(new LinearLayoutManager(this));
        rvHopDong.setAdapter(adapter);

        // Nhắc tái ký qua Zalo
        adapter.setOnNhacTaiKyListener(this::sendZaloReminder);

        // Xử lý cập nhật trạng thái thu cọc
        adapter.setOnDepositUpdateListener(this::updateDepositStatus);

        // Xử lý xóa hợp đồng
        adapter.setOnContractDeleteListener(this::deleteContract);

        // SearchView
        EditText etSearch = findViewById(R.id.etSearchHd);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    adapter.filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        // Load data
        repo = new TenantRepository();
        loadData();
    }

    private void loadData() {
        repo.getTenantList().observe(this, list -> {
            if (list == null)
                return;
            adapter.setData(list);
            updateSummary(list);
            updateEmptyState(list);
        });
    }

    private void updateSummary(List<Tenant> list) {
        int dangThue = 0, sapHet = 0, ketThuc = 0;
        for (Tenant c : list) {
            ContractStatus s = ContractStatusHelper.resolve(c);
            if (s == null)
                continue;
            switch (s) {
                case DANG_THUE:
                    dangThue++;
                    break;
                case SAP_HET_HAN:
                    sapHet++;
                    break;
                case DA_KET_THUC:
                    ketThuc++;
                    break;
            }
        }

        if (tvCountDangThue != null)
            tvCountDangThue.setText(String.valueOf(dangThue));
        if (tvCountSapHet != null)
            tvCountSapHet.setText(String.valueOf(sapHet));
        if (tvCountKetThuc != null)
            tvCountKetThuc.setText(String.valueOf(ketThuc));
    }

    private void updateEmptyState(List<Tenant> list) {
        boolean isEmpty = list == null || list.isEmpty();
        rvHopDong.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (layoutEmpty != null)
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void sendZaloReminder(Tenant contract) {
        String soPhong = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        String ngayKT = contract.getNgayKetThucHopDong() != null ? contract.getNgayKetThucHopDong() : "?";
        String msg = "Hợp đồng phòng " + soPhong + " của bạn sắp hết hạn vào ngày "
                + ngayKT + ", vui lòng liên hệ chủ trọ để gia hạn.";

        String sdt = contract.getSoDienThoai();
        if (sdt != null && !sdt.trim().isEmpty()) {
            String normalized = sdt.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0"))
                normalized = "84" + normalized.substring(1);
            try {
                Intent zalo = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + normalized));
                zalo.setPackage("com.zing.zalo");
                startActivity(zalo);
                return;
            } catch (Exception ignored) {
            }
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(Intent.createChooser(share, "Gửi nhắc nhở qua..."));
    }

    private void updateDepositStatus(Tenant contract) {
        if (contract == null || contract.getId() == null)
            return;

        repo.updateStatusThuCoc(contract.getId(), true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái thu cọc", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    contract.setTrangThaiThuCoc(false);
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteContract(String contractId) {
        if (contractId == null || contractId.trim().isEmpty())
            return;

        repo.deleteContract(contractId,
                () -> {
                    Toast.makeText(this, "✓ Đã xóa hợp đồng", Toast.LENGTH_SHORT).show();
                    adapter.removeItemById(contractId);
                },
                () -> {
                    Toast.makeText(this, "❌ Lỗi: Không thể xóa hợp đồng", Toast.LENGTH_LONG).show();
                });
    }
}
