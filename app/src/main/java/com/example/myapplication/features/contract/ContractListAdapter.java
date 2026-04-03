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
import com.example.myapplication.domain.Tenant;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContractListAdapter extends RecyclerView.Adapter<ContractListAdapter.ViewHolder> {

    private List<Tenant> fullList = new ArrayList<>();
    private List<Tenant> displayList = new ArrayList<>();
    private String filterQuery = "";

    // ── Chip colors ─────────────────────────────────────────────────────────
    private static final int COLOR_DANG_THUE   = Color.parseColor("#4CAF50"); // Xanh lá
    private static final int COLOR_SAP_HET     = Color.parseColor("#FF6D00"); // Cam đỏ
    private static final int COLOR_KET_THUC    = Color.parseColor("#9E9E9E"); // Xám

    // ── Card background tints ────────────────────────────────────────────────
    private static final int CARD_SAP_HET_BG   = Color.parseColor("#FFF3E0"); // Cam nhạt
    private static final int CARD_DEFAULT_BG    = Color.WHITE;

    public interface OnNhacTaiKyListener {
        void onNhacTaiKy(Tenant contract);
    }

    public interface OnDepositUpdateListener {
        void onDepositUpdated(Tenant contract);
    }

    public interface OnContractDeleteListener {
        void onContractDeleted(String contractId);
    }

    private OnNhacTaiKyListener nhacListener;
    private OnDepositUpdateListener depositListener;
    private OnContractDeleteListener deleteListener;

    public void setOnNhacTaiKyListener(OnNhacTaiKyListener l) {
        this.nhacListener = l;
    }

    public void setOnDepositUpdateListener(OnDepositUpdateListener l) {
        this.depositListener = l;
    }

    public void setOnContractDeleteListener(OnContractDeleteListener l) {
        this.deleteListener = l;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** Nhận toàn bộ danh sách, sắp xếp: SắpHết → ĐangThuê → ĐãKếtThúc */
    public void setData(List<Tenant> list) {
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
        for (Tenant c : fullList) {
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
        List<Tenant> filtered = new ArrayList<>();
        for (Tenant c : fullList) {
            if (matches(c)) filtered.add(c);
        }
        this.displayList = filtered;
        notifyDataSetChanged();
    }

    private boolean matches(Tenant c) {
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
                .inflate(R.layout.item_contract, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Tenant c = displayList.get(position);
        ContractStatus status = ContractStatusHelper.resolve(c);
        long daysLeft = ContractStatusHelper.daysRemaining(c);

        // Tên phòng
        h.tvTenPhong.setText(c.getSoPhong() != null ? "Phòng " + c.getSoPhong() : "—");

        // Giá thuê - Format với dấu phân cách
        long giaThue = c.getGiaThue();
        String giaThueFormatted = String.format("%,dđ", giaThue).replace(',', '.');
        h.tvGiaThue.setText(giaThueFormatted);

        // Tiền cọc - Format với dấu phân cách
        long tienCoc = c.getTienCoc();
        String tienCocFormatted = String.format("%,dđ", tienCoc).replace(',', '.');
        h.tvTienCoc.setText(tienCocFormatted);

        // Thông tin người đại diện (nếu có), fallback về hoTen
        String tenNguoiDaiDien = c.getTenNguoiDaiDien();
        if (tenNguoiDaiDien == null || tenNguoiDaiDien.trim().isEmpty()) {
            tenNguoiDaiDien = c.getHoTen(); // Fallback to hoTen if no representative
        }
        h.tvTenantName.setText(tenNguoiDaiDien != null ? tenNguoiDaiDien : "—");
        h.tvTenantPhone.setText(c.getSoDienThoai() != null ? c.getSoDienThoai() : "—");

        // Chip trạng thái hợp đồng - Logic tự động theo ngày kết thúc
        updateContractStatusChip(h.chipTrangThai, status, daysLeft, c);

        // Hiển thị trạng thái thu cọc (nếu chưa thu và hợp đồng còn hiệu lực)
        updateDepositStatusDisplay(h, c, status);

        // Menu 3 chấm
        h.btnMenu.setOnClickListener(v -> showPopupMenu(v, c, position, status));
    }

    /**
     * Cập nhật chip trạng thái hợp đồng theo logic:
     * - Hết hạn: Màu Xám
     * - Sắp hết hạn (<30 ngày): Màu Đỏ rực
     * - Đang hiệu lực: Màu Xanh lá
     * 
     * Kiểm tra thêm: Nếu có ngayKetThuc (long timestamp), dùng nó để tính chính xác
     */
    private void updateContractStatusChip(Chip chip, ContractStatus status, long daysLeft, Tenant contract) {
        // Kiểm tra logic mới với ngayKetThuc (long timestamp)
        long ngayKetThuc = contract.getNgayKetThuc();
        if (ngayKetThuc > 0) {
            long currentTime = System.currentTimeMillis();
            long timeRemaining = ngayKetThuc - currentTime;
            
            // 30 ngày = 2,592,000,000 ms
            final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000; // 2592000000
            
            if (timeRemaining < 0) {
                // Hết hạn
                chip.setText("Hết hạn");
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Xám
                chip.setTextColor(Color.WHITE);
                return;
            } else if (timeRemaining < THIRTY_DAYS_MS) {
                // Sắp hết hạn (< 30 ngày)
                long daysLeftNew = timeRemaining / (24 * 60 * 60 * 1000);
                String text = "⚠ Còn " + daysLeftNew + " ngày";
                chip.setText(text);
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Đỏ rực
                chip.setTextColor(Color.WHITE);
                return;
            } else {
                // Đang hiệu lực
                chip.setText("✓ Đang hiệu lực");
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Xanh lá
                chip.setTextColor(Color.WHITE);
                return;
            }
        }
        
        // Fallback: dùng logic cũ nếu không có ngayKetThuc
        switch (status) {
            case DA_KET_THUC:
                chip.setText("Hết hạn");
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Xám
                chip.setTextColor(Color.WHITE);
                break;
            
            case SAP_HET_HAN:
                String text = "⚠ Sắp hết hạn";
                if (daysLeft >= 0) {
                    text = "⚠ Còn " + daysLeft + " ngày";
                }
                chip.setText(text);
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Đỏ rực
                chip.setTextColor(Color.WHITE);
                break;
            
            case DANG_THUE:
            default:
                chip.setText("✓ Đang hiệu lực");
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Xanh lá
                chip.setTextColor(Color.WHITE);
                break;
        }
    }

    /**
     * Hiển thị trạng thái thu cọc bằng TextView phụ
     */
    private void updateDepositStatusDisplay(ViewHolder h, Tenant c, ContractStatus status) {
        // Chỉ hiển thị trạng thái thu cọc nếu hợp đồng còn hiệu lực
        if (status == ContractStatus.DA_KET_THUC) {
            h.tvDepositStatus.setVisibility(View.GONE);
            return;
        }

        h.tvDepositStatus.setVisibility(View.VISIBLE);
        if (c.isTrangThaiThuCoc()) {
            h.tvDepositStatus.setText("✓ Đã thu cọc");
            h.tvDepositStatus.setTextColor(Color.parseColor("#4CAF50")); // Xanh
        } else {
            h.tvDepositStatus.setText("⏳ Chờ thu cọc");
            h.tvDepositStatus.setTextColor(Color.parseColor("#FF9800")); // Cam
        }
    }

    /** Mở Zalo với tin nhắn soạn sẵn, fallback sang Share chooser */
    private void sendZaloReminder(Context ctx, Tenant c) {
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

    private void showPopupMenu(View anchor, Tenant contract, int position, ContractStatus status) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_contract, popup.getMenu());
        
        // Hiển thị "Gửi nhắc tái ký" chỉ khi hợp đồng sắp hết hạn
        android.view.MenuItem menuNhacTaiKy = popup.getMenu().findItem(R.id.menu_nhac_tai_ky);
        if (menuNhacTaiKy != null) {
            menuNhacTaiKy.setVisible(status == ContractStatus.SAP_HET_HAN);
        }
        
        // Hiển thị "Xác nhận thu cọc" chỉ khi chưa thu cọc
        android.view.MenuItem menuThuCoc = popup.getMenu().findItem(R.id.menu_thu_coc);
        if (menuThuCoc != null) {
            menuThuCoc.setVisible(!contract.isTrangThaiThuCoc() && status != ContractStatus.DA_KET_THUC);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_thu_coc) {
                showBottomSheetThuCoc(anchor.getContext(), contract, position);
                return true;
            } else if (itemId == R.id.menu_xac_nhan_thu_coc) {
                confirmDepositReceived(anchor.getContext(), contract, position);
                return true;
            } else if (itemId == R.id.menu_nhac_tai_ky) {
                sendRenewalReminder(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_xem_chi_tiet) {
                openContractDetail(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_chinh_sua) {
                openEditContract(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_xoa_hop_dong) {
                showDeleteConfirmDialog(anchor.getContext(), contract, position);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    /**
     * Xác nhận đã thu cọc - cập nhật trực tiếp lên Firestore
     */
    private void confirmDepositReceived(Context context, Tenant contract, int position) {
        if (depositListener != null) {
            contract.setTrangThaiThuCoc(true);
            depositListener.onDepositUpdated(contract);
            notifyItemChanged(position);
            Toast.makeText(context, "✓ Đã xác nhận thu cọc", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gửi nhắc nhở tái ký hợp đồng qua Zalo
     */
    private void sendRenewalReminder(Context context, Tenant contract) {
        String soPhong = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        String ngayKetThuc = contract.getNgayKetThucHopDong() != null ? contract.getNgayKetThucHopDong() : "?";
        long daysLeft = ContractStatusHelper.daysRemaining(contract);
        
        String msg = "🏠 Thông báo tái ký hợp đồng\n\n" +
                "Phòng: " + soPhong + "\n" +
                "Ngày kết thúc: " + ngayKetThuc + "\n" +
                "Còn lại: " + daysLeft + " ngày\n\n" +
                "Quý khách vui lòng liên hệ chủ trọ để thực hiện thủ tục tái ký hợp đồng. Xin cảm ơn!";

        String sdt = contract.getSoDienThoai();
        if (sdt != null && !sdt.trim().isEmpty()) {
            // Chuẩn hóa: 0xxx → 84xxx
            String normalized = sdt.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0")) {
                normalized = "84" + normalized.substring(1);
            }
            try {
                Intent zaloIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://zalo.me/" + normalized + "?text=" + Uri.encode(msg)));
                zaloIntent.setPackage("com.zing.zalo");
                context.startActivity(zaloIntent);
                return;
            } catch (Exception ignored) { /* Zalo chưa cài */ }
        }

        // Fallback: Share chooser
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
        context.startActivity(Intent.createChooser(shareIntent, "Gửi nhắc tái ký qua..."));
    }

    /**
     * Mở màn hình chi tiết hợp đồng
     */
    private void openContractDetail(Context context, Tenant contract) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(context, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ContractDetailsActivity.class);
        intent.putExtra(ContractDetailsActivity.EXTRA_CONTRACT_ID, contract.getId());
        context.startActivity(intent);
    }

    /**
     * Mở màn hình chỉnh sửa hợp đồng
     */
    private void openEditContract(Context context, Tenant contract) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(context, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ContractActivity.class);
        
        // Gửi flag để báo đây là mode EDIT
        intent.putExtra("MODE", "EDIT");
        
        // Gửi toàn bộ thông tin hợp đồng
        intent.putExtra("CONTRACT_ID", contract.getId());
        intent.putExtra("PHONG_ID", contract.getIdPhong());
        intent.putExtra("SO_HOP_DONG", contract.getSoHopDong());
        intent.putExtra("HO_TEN", contract.getHoTen());
        intent.putExtra("SO_DIEN_THOAI", contract.getSoDienThoai());
        intent.putExtra("CCCD", contract.getCccd());
        intent.putExtra("SO_THANH_VIEN", contract.getSoThanhVien());
        intent.putExtra("NGAY_BAT_DAU", contract.getNgayBatDauThue());
        intent.putExtra("SO_THANG", contract.getSoThangHopDong());
        intent.putExtra("GIA_THUE", contract.getGiaThue());
        intent.putExtra("TIEN_COC", contract.getTienCoc());
        intent.putExtra("CHI_SO_DIEN", contract.getChiSoDienDau());
        intent.putExtra("DICH_VU_GUI_XE", contract.isDichVuGuiXe());
        intent.putExtra("SO_LUONG_XE", contract.getSoLuongXe());
        intent.putExtra("DICH_VU_INTERNET", contract.isDichVuInternet());
        intent.putExtra("DICH_VU_GIAT_SAY", contract.isDichVuGiatSay());
        intent.putExtra("GHI_CHU", contract.getGhiChu());
        intent.putExtra("HIEN_THI_COC", contract.isHienThiTienCocTrenInvoice());
        intent.putExtra("HIEN_THI_GHI_CHU", contract.isHienThiGhiChuTrenInvoice());
        intent.putExtra("NHAC_TRUOC_1_THANG", contract.isNhacTruoc1Thang());
        intent.putExtra("CCCD_FRONT_URL", contract.getCccdFrontUrl());
        intent.putExtra("CCCD_BACK_URL", contract.getCccdBackUrl());
        
        context.startActivity(intent);
    }

    private void showBottomSheetThuCoc(Context context, Tenant contract, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_deposit, null);
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

        // Format tiền cọc với dấu phân cách
        long tienCoc = contract.getTienCoc();
        String tienCocFormatted = String.format("%,dđ", tienCoc).replace(',', '.');
        tvSoTienCoc.setText(tienCocFormatted);

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

    private void shareQRAndMessage(Context context, Tenant contract) {
        String phong = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        long tienCoc = contract.getTienCoc();
        String tienCocFormatted = String.format("%,dđ", tienCoc).replace(',', '.');
        
        String message = "Yêu cầu thanh toán cọc phòng " + phong + "\n" +
                "Số tiền: " + tienCocFormatted + "\n" +
                "Vui lòng thanh toán qua mã QR bên dưới.";

        // Share text (QR image would need FileProvider setup)
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Yêu cầu thanh toán cọc phòng " + phong);
        
        context.startActivity(Intent.createChooser(shareIntent, "Gửi yêu cầu thanh toán"));
    }

    /**
     * Hiển thị dialog xác nhận xóa hợp đồng
     */
    private void showDeleteConfirmDialog(Context context, Tenant contract, int position) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(context, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
            return;
        }

        String roomName = contract.getSoPhong() != null ? contract.getSoPhong() : "?";
        String tenantName = contract.getHoTen() != null ? contract.getHoTen() : "?";
        
        new android.app.AlertDialog.Builder(context)
                .setTitle("⚠️ Xác nhận xóa hợp đồng")
                .setMessage("Bạn có chắc chắn muốn xóa hợp đồng của phòng " + roomName + 
                           " (Khách: " + tenantName + ")?\n\n" +
                           "⚠️ Thao tác này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteContract(context, contract, position);
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Xóa hợp đồng khỏi Firestore và cập nhật UI
     */
    private void deleteContract(Context context, Tenant contract, int position) {
        String contractId = contract.getId();
        
        // Hiển thị loading state
        Toast.makeText(context, "Đang xóa hợp đồng...", Toast.LENGTH_SHORT).show();
        
        // Gọi listener để Activity xử lý xóa trên Firestore
        if (deleteListener != null) {
            deleteListener.onContractDeleted(contractId);
        } else {
            // Fallback: xóa trực tiếp từ adapter (không khuyến khích)
            Toast.makeText(context, "Lỗi: Không thể xóa hợp đồng", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Xóa item khỏi danh sách hiển thị (được gọi từ Activity sau khi Firestore xóa thành công)
     */
    public void removeItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            Tenant removed = displayList.remove(position);
            fullList.remove(removed);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, displayList.size());
        }
    }

    /**
     * Xóa item theo ID (được gọi từ Activity sau khi Firestore xóa thành công)
     */
    public void removeItemById(String contractId) {
        if (contractId == null) return;
        
        for (int i = 0; i < displayList.size(); i++) {
            if (contractId.equals(displayList.get(i).getId())) {
                removeItem(i);
                return;
            }
        }
    }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenPhong, tvGiaThue, tvTienCoc, tvTenantName, tvTenantPhone, tvDepositStatus;
        Chip chipTrangThai;
        ImageButton btnMenu;

        ViewHolder(View v) {
            super(v);
            tvTenPhong = v.findViewById(R.id.tvTenPhong);
            tvGiaThue = v.findViewById(R.id.tvGiaThue);
            tvTienCoc = v.findViewById(R.id.tvTienCoc);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvTenantPhone = v.findViewById(R.id.tvTenantPhone);
            tvDepositStatus = v.findViewById(R.id.tvDepositStatus);
            chipTrangThai = v.findViewById(R.id.chipTrangThai);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}

