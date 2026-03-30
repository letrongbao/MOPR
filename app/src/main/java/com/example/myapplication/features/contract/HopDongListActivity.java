package com.example.myapplication.features.contract;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.NguoiThueRepository;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.NguoiThue;

import java.util.List;

public class HopDongListActivity extends AppCompatActivity {

    private static final String TAG = "HopDongListActivity";

    private HopDongListAdapter adapter;
    private NguoiThueRepository repo;

    private TextView tvCountDangThue, tvCountSapHet, tvCountKetThuc;
    private LinearLayout layoutEmpty;
    private RecyclerView rvHopDong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat ctrl = WindowCompat.getInsetsController(window, window.getDecorView());
        if (ctrl != null) ctrl.setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_hop_dong_list);

        // Padding cho AppBar
        View appBar = findViewById(R.id.appBarLayoutHd);
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, sys.top, 0, 0);
                return insets;
            });
        }

        // Back button
        View btnBack = findViewById(R.id.btnBackHd);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Summary TextViews
        tvCountDangThue = findViewById(R.id.tvCountDangThue);
        tvCountSapHet   = findViewById(R.id.tvCountSapHet);
        tvCountKetThuc  = findViewById(R.id.tvCountKetThuc);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        rvHopDong       = findViewById(R.id.rvHopDong);

        // RecyclerView + Adapter
        adapter = new HopDongListAdapter();
        rvHopDong.setLayoutManager(new LinearLayoutManager(this));
        rvHopDong.setAdapter(adapter);

        // Nhắc tái ký qua Zalo
        adapter.setOnNhacTaiKyListener(contract -> sendZaloReminder(contract));

        // Xử lý cập nhật trạng thái thu cọc
        adapter.setOnDepositUpdateListener(contract -> updateDepositStatus(contract));

        // Xử lý xóa hợp đồng
        adapter.setOnContractDeleteListener(contractId -> deleteContract(contractId));

        // SearchView
        EditText etSearch = findViewById(R.id.etSearchHd);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    adapter.filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Load data
        repo = new NguoiThueRepository();
        loadData();
    }

    private void loadData() {
        // Dùng layDanhSachNguoiThue() có sẵn trong repository (lấy tất cả)
        repo.layDanhSachNguoiThue().observe(this, list -> {
            if (list == null) return;
            adapter.setData(list);
            updateSummary(list);
            updateEmptyState(list);
        });
    }

    private void updateSummary(List<NguoiThue> list) {
        int dangThue = 0, sapHet = 0, ketThuc = 0;
        for (NguoiThue c : list) {
            ContractStatus s = ContractStatusHelper.resolve(c);
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
        
        // Cập nhật UI với màu sắc tương ứng
        if (tvCountDangThue != null) {
            tvCountDangThue.setText(String.valueOf(dangThue));
            tvCountDangThue.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
        }
        if (tvCountSapHet != null) {
            tvCountSapHet.setText(String.valueOf(sapHet));
            tvCountSapHet.setTextColor(Color.parseColor("#F44336")); // Đỏ rực
        }
        if (tvCountKetThuc != null) {
            tvCountKetThuc.setText(String.valueOf(ketThuc));
            tvCountKetThuc.setTextColor(Color.parseColor("#9E9E9E")); // Xám
        }
        
        Log.d(TAG, String.format("Stats updated - Active: %d, Expiring: %d, Ended: %d", 
                dangThue, sapHet, ketThuc));
    }

    private void updateEmptyState(List<NguoiThue> list) {
        boolean isEmpty = list == null || list.isEmpty();
        rvHopDong.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void sendZaloReminder(NguoiThue contract) {
        String soPhong   = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        String ngayKT    = contract.getNgayKetThucHopDong() != null ? contract.getNgayKetThucHopDong() : "?";
        String msg = "Hợp đồng phòng " + soPhong + " của bạn sắp hết hạn vào ngày "
                + ngayKT + ", vui lòng liên hệ chủ trọ để gia hạn.";

        String sdt = contract.getSoDienThoai();
        if (sdt != null && !sdt.trim().isEmpty()) {
            String normalized = sdt.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0")) normalized = "84" + normalized.substring(1);
            try {
                Intent zalo = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + normalized));
                zalo.setPackage("com.zing.zalo");
                startActivity(zalo);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: share chooser
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(Intent.createChooser(share, "Gửi nhắc nhở qua..."));
    }

    private void updateDepositStatus(NguoiThue contract) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Updating deposit status for contract: " + contract.getId());

        repo.capNhatTrangThaiThuCoc(contract.getId(), true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deposit status updated successfully");
                    Toast.makeText(this, "Đã cập nhật trạng thái thu cọc", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update deposit status", e);
                    Toast.makeText(this, "Lỗi: Không thể cập nhật - " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    // Revert UI change
                    contract.setTrangThaiThuCoc(false);
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Xóa hợp đồng khỏi Firestore
     */
    private void deleteContract(String contractId) {
        if (contractId == null || contractId.trim().isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deleting contract: " + contractId);

        repo.xoaHopDong(contractId, 
            () -> {
                // Thành công
                Log.d(TAG, "Contract deleted successfully");
                Toast.makeText(this, "✓ Đã xóa hợp đồng thành công", Toast.LENGTH_SHORT).show();
                
                // Xóa item khỏi adapter
                adapter.removeItemById(contractId);
            },
            () -> {
                // Thất bại
                Log.e(TAG, "Failed to delete contract");
                Toast.makeText(this, "❌ Lỗi: Không thể xóa hợp đồng. Vui lòng kiểm tra kết nối mạng và thử lại.", 
                        Toast.LENGTH_LONG).show();
            }
        );
    }
}
