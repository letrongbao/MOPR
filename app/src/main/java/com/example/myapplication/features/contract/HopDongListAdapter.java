package com.example.myapplication.features.contract;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.NguoiThue;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HopDongListAdapter extends RecyclerView.Adapter<HopDongListAdapter.ViewHolder> {

    private List<NguoiThue> fullList = new ArrayList<>();
    private List<NguoiThue> displayList = new ArrayList<>();
    private String filterQuery = "";

    // ── Chip colors ─────────────────────────────────────────────────────────
    private static final int COLOR_DANG_THUE   = Color.parseColor("#4CAF50"); // Xanh lá
    private static final int COLOR_SAP_HET     = Color.parseColor("#FF6D00"); // Cam đỏ
    private static final int COLOR_KET_THUC    = Color.parseColor("#9E9E9E"); // Xám

    // ── Card background tints ────────────────────────────────────────────────
    private static final int CARD_SAP_HET_BG   = Color.parseColor("#FFF3E0"); // Cam nhạt
    private static final int CARD_DEFAULT_BG    = Color.WHITE;

    public interface OnNhacTaiKyListener {
        void onNhacTaiKy(NguoiThue contract);
    }

    public interface OnDepositUpdateListener {
        void onDepositUpdated(NguoiThue contract);
    }

    private OnNhacTaiKyListener nhacListener;
    private OnDepositUpdateListener depositListener;

    public void setOnNhacTaiKyListener(OnNhacTaiKyListener l) {
        this.nhacListener = l;
    }

    public void setOnDepositUpdateListener(OnDepositUpdateListener l) {
        this.depositListener = l;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** Nhận toàn bộ danh sách, sắp xếp: SắpHết → ĐangThuê → ĐãKếtThúc */
    public void setData(List<NguoiThue> list) {
        this.fullList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        // Sắp xếp theo mức độ ưu tiên
        this.fullList.sort((a, b) -> {
            int pa = priorityOf(ContractStatusHelper.resolve(a));
            int pb = priorityOf(ContractStatusHelper.resolve(b));
            if (pa != pb) return Integer.compare(pa, pb);
            // Cùng priority → sắp theo ngày còn lại tăng dần
            long da = ContractStatusHelper.daysRemaining(a);
            long db = ContractStatusHelper.daysRemaining(b);
            return Long.compare(da, db);
        });
        rebuildDisplay();
    }

    /** Lọc theo từ khóa (tên hoặc số phòng) */
    public void filter(String query) {
        this.filterQuery = query == null ? "" : query.trim().toLowerCase();
        rebuildDisplay();
    }

    /** Đếm theo từng trạng thái (dùng cho summary) */
    public int countByStatus(ContractStatus status) {
        int count = 0;
        for (NguoiThue c : fullList) {
            if (ContractStatusHelper.resolve(c) == status) count++;
        }
        return count;
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private int priorityOf(ContractStatus s) {
        switch (s) {
            case SAP_HET_HAN:  return 0;
            case DANG_THUE:    return 1;
            case DA_KET_THUC:  return 2;
            default:           return 3;
        }
    }

    private void rebuildDisplay() {
        List<NguoiThue> filtered = new ArrayList<>();
        for (NguoiThue c : fullList) {
            if (matches(c)) filtered.add(c);
        }
        this.displayList = filtered;
        notifyDataSetChanged();
    }

    private boolean matches(NguoiThue c) {
        if (filterQuery.isEmpty()) return true;
        String name = c.getHoTen() != null ? c.getHoTen().toLowerCase() : "";
        String room = c.getSoPhong() != null ? c.getSoPhong().toLowerCase() : "";
        return name.contains(filterQuery) || room.contains(filterQuery);
    }

    // ── RecyclerView ────────────────────────────────────────────────────────

    @Override public int getItemCount() { return displayList.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hop_dong, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NguoiThue c = displayList.get(position);
        ContractStatus status = ContractStatusHelper.resolve(c);

        // Tên phòng
        h.tvTenPhong.setText(c.getSoPhong() != null ? "Phòng " + c.getSoPhong() : "—");

        // Giá thuê
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String giaThue = currencyFormat.format(c.getTienPhong()) + "đ";
        h.tvGiaThue.setText(giaThue);

        // Tiền cọc
        String tienCoc = currencyFormat.format(c.getTienCoc()) + "đ";
        h.tvTienCoc.setText(tienCoc);

        // Thông tin khách thuê
        h.tvTenantName.setText(c.getHoTen() != null ? c.getHoTen() : "—");
        h.tvTenantPhone.setText(c.getSoDienThoai() != null ? c.getSoDienThoai() : "—");

        // Chip trạng thái - Đổi màu theo trạng thái thu cọc
        if (c.isTrangThaiThuCoc()) {
            h.chipTrangThai.setText("✓ Đã thu cọc");
            h.chipTrangThai.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            h.chipTrangThai.setText("Chờ thu cọc");
            h.chipTrangThai.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
        }

        // Menu 3 chấm
        h.btnMenu.setOnClickListener(v -> showPopupMenu(v, c, position));
    }

    /** Mở Zalo với tin nhắn soạn sẵn, fallback sang Share chooser */
    private void sendZaloReminder(Context ctx, NguoiThue c) {
        String soPhong   = c.getSoPhong() != null ? c.getSoPhong() : "?";
        String ngayKetThuc = c.getNgayKetThucHopDong() != null ? c.getNgayKetThucHopDong() : "?";
        String msg = "Hợp đồng phòng " + soPhong + " của bạn sắp hết hạn vào ngày "
                + ngayKetThuc + ", vui lòng liên hệ chủ trọ để gia hạn.";

        String sdt = c.getSoDienThoai();
        if (sdt != null && !sdt.trim().isEmpty()) {
            // Chuẩn hóa: 0xxx → 84xxx
            String normalized = sdt.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0")) {
                normalized = "84" + normalized.substring(1);
            }
            try {
                Intent zaloIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://zalo.me/" + normalized));
                zaloIntent.setPackage("com.zing.zalo");
                ctx.startActivity(zaloIntent);
                return;
            } catch (Exception ignored) { /* Zalo chưa cài */ }
        }

        // Fallback: Share chooser
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
        ctx.startActivity(Intent.createChooser(shareIntent, "Gửi nhắc nhở qua..."));
    }

    private void showPopupMenu(View anchor, NguoiThue contract, int position) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_hop_dong, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_thu_coc) {
                showBottomSheetThuCoc(anchor.getContext(), contract, position);
                return true;
            } else if (itemId == R.id.menu_xem_chi_tiet) {
                Toast.makeText(anchor.getContext(), "Xem chi tiết hợp đồng", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void showBottomSheetThuCoc(Context context, NguoiThue contract, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_thu_coc, null);
        bottomSheet.setContentView(view);

        // Bind data
        TextView tvPhongInfo = view.findViewById(R.id.tvPhongInfo);
        TextView tvTenantInfo = view.findViewById(R.id.tvTenantInfo);
        TextView tvSoTienCoc = view.findViewById(R.id.tvSoTienCoc);
        MaterialButton btnGuiQR = view.findViewById(R.id.btnGuiQR);
        MaterialButton btnXacNhanDaThu = view.findViewById(R.id.btnXacNhanDaThu);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        String phongText = contract.getSoPhong() != null ? "Phòng " + contract.getSoPhong() : "—";
        tvPhongInfo.setText(phongText);

        String tenantText = "Khách: " + (contract.getHoTen() != null ? contract.getHoTen() : "—") 
                + " - " + (contract.getSoDienThoai() != null ? contract.getSoDienThoai() : "—");
        tvTenantInfo.setText(tenantText);

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String tienCoc = currencyFormat.format(contract.getTienCoc()) + "đ";
        tvSoTienCoc.setText(tienCoc);

        // Nút Gửi QR & Liên hệ
        btnGuiQR.setOnClickListener(v -> {
            shareQRAndMessage(context, contract);
        });

        // Nút Xác nhận đã thu
        btnXacNhanDaThu.setOnClickListener(v -> {
            if (depositListener != null) {
                contract.setTrangThaiThuCoc(true);
                depositListener.onDepositUpdated(contract);
                notifyItemChanged(position);
                bottomSheet.dismiss();
                Toast.makeText(context, "Đã xác nhận thu cọc", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void shareQRAndMessage(Context context, NguoiThue contract) {
        String phong = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String tienCoc = currencyFormat.format(contract.getTienCoc()) + "đ";
        
        String message = "Yêu cầu thanh toán cọc phòng " + phong + "\n" +
                "Số tiền: " + tienCoc + "\n" +
                "Vui lòng thanh toán qua mã QR bên dưới.";

        // Share text (QR image would need FileProvider setup)
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Yêu cầu thanh toán cọc phòng " + phong);
        
        context.startActivity(Intent.createChooser(shareIntent, "Gửi yêu cầu thanh toán"));
    }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenPhong, tvGiaThue, tvTienCoc, tvTenantName, tvTenantPhone;
        Chip chipTrangThai;
        ImageButton btnMenu;

        ViewHolder(View v) {
            super(v);
            tvTenPhong = v.findViewById(R.id.tvTenPhong);
            tvGiaThue = v.findViewById(R.id.tvGiaThue);
            tvTienCoc = v.findViewById(R.id.tvTienCoc);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvTenantPhone = v.findViewById(R.id.tvTenantPhone);
            chipTrangThai = v.findViewById(R.id.chipTrangThai);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
