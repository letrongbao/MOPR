package com.example.myapplication.features.tenant;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.Tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter Multi-ViewType hiển thị danh sách khách thuê phân nhóm theo phòng.
 *
 * ViewType 0 = HEADER (tên phòng + nút Thêm)
 * ViewType 1 = ITEM (thẻ bài thông tin khách thuê)
 */
public class TenantAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── Link Google Form pre-filled (số phòng + tenant ID) ─────────────────
    // '999'     → sẽ được thay bằng soPhong thực tế của phòng.
    // 'ABC_XYZ' → sẽ được thay bằng ID document Tenant trên Firestore.
    private static final String GOOGLE_FORM_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSfeQn8xbJTbHw4FHbqSqZpC87uxfy34w0l211T5vHj66VdVUw/viewform?usp=pp_url&entry.166517188=999&entry.1163193725=ABC_XYZ";

    // ── ViewType constants ──────────────────────────────────────────────────
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    // ── Wrapper để chứa lẫn lộn Header (String) và Item (Tenant) ────────
    /** Mỗi phần tử trong displayList là một trong hai loại này. */
    private static class Row {
        final String header; // != null → đây là dòng header tên phòng
        final Tenant tenant; // != null → đây là dòng thẻ bài khách

        Row(String header) {
            this.header = header;
            this.tenant = null;
        }

        Row(Tenant tenant) {
            this.tenant = tenant;
            this.header = null;
        }

        boolean isHeader() {
            return header != null;
        }
    }

    // ── State ───────────────────────────────────────────────────────────────
    /** Danh sách nguồn đầy đủ – KHÔNG lọc. */
    private List<Tenant> fullList = new ArrayList<>();
    /** Danh sách hiển thị sau khi group + filter. */
    private List<Row> displayList = new ArrayList<>();

    /** Từ khoá tìm kiếm hiện tại (rỗng = hiển thị tất cả). */
    private String filterQuery = "";

    // ── Callbacks ───────────────────────────────────────────────────────────
    public interface OnItemActionListener {
        void onXoa(Tenant nguoiThue);

        void onSua(Tenant nguoiThue);
    }

    /** Callback cho nút [+ Thêm] trên header mỗi phòng. */
    public interface OnAddToRoomListener {
        void onAdd(String roomId, String roomName);
    }

    /** Callback cho nút [Tự nhập] — chia sẻ link Google Form đến khách. */
    public interface OnSelfEntryListener {
        /** @param soPhong số phòng thực tế (ví dụ: "5") */
        void onSelfEntry(String soPhong);
    }

    private final OnItemActionListener actionListener;
    private OnAddToRoomListener addListener;
    private OnSelfEntryListener selfEntryListener;

    public TenantAdapter(OnItemActionListener listener) {
        this.actionListener = listener;
    }

    public void setOnAddToRoomListener(OnAddToRoomListener l) {
        this.addListener = l;
    }

    public void setOnSelfEntryListener(OnSelfEntryListener l) {
        this.selfEntryListener = l;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Nhận danh sách khách thuê mới, thực hiện Group-by phòng,
     * sắp xếp theo tên phòng rồi rebuild displayList.
     */
    public void setData(List<Tenant> list) {
        this.fullList = list != null ? list : new ArrayList<>();
        rebuildDisplay();
    }

    /** Lọc trực tiếp theo từ khoá (tên hoặc SĐT). */
    public void filter(String query) {
        this.filterQuery = query == null ? "" : query.trim().toLowerCase();
        rebuildDisplay();
    }

    /** Trả số khách đang hiển thị (bỏ qua row header). */
    public int getTenantCount() {
        int count = 0;
        for (Row r : displayList)
            if (!r.isHeader())
                count++;
        return count;
    }

    // ── Rebuild logic ────────────────────────────────────────────────────────

    private void rebuildDisplay() {
        // 1. Lọc theo từ khoá
        List<Tenant> filtered = new ArrayList<>();
        for (Tenant n : fullList) {
            if (matchesFilter(n))
                filtered.add(n);
        }

        // 2. Gom nhóm theo idPhong → giữ nguyên thứ tự phòng đầu tiên xuất hiện
        // nhưng sort key = soPhong để đồng nhất UI
        Map<String, List<Tenant>> groups = new LinkedHashMap<>();
        Map<String, String> roomNames = new LinkedHashMap<>(); // roomId → soPhong

        for (Tenant n : filtered) {
            String roomId = n.getIdPhong() != null ? n.getIdPhong() : "unknown";
            String soPhong = n.getSoPhong() != null ? n.getSoPhong() : roomId;

            if (!groups.containsKey(roomId)) {
                groups.put(roomId, new ArrayList<>());
                roomNames.put(roomId, soPhong);
            }
            groups.get(roomId).add(n);
        }

        // 3. Sắp xếp các nhóm theo tên phòng (alphabetically / numerically)
        List<String> sortedRoomIds = new ArrayList<>(groups.keySet());
        Collections.sort(sortedRoomIds, (a, b) -> {
            String nameA = roomNames.getOrDefault(a, a);
            String nameB = roomNames.getOrDefault(b, b);
            // Thử so kiểu số trước
            try {
                return Integer.compare(Integer.parseInt(nameA), Integer.parseInt(nameB));
            } catch (NumberFormatException ignored) {
            }
            return nameA.compareToIgnoreCase(nameB);
        });

        // 4. Build displayList: Header → [Items…] → Header → [Items…]
        List<Row> newList = new ArrayList<>();
        for (String roomId : sortedRoomIds) {
            String label = "Phòng " + roomNames.getOrDefault(roomId, roomId);
            newList.add(new Row(label)); // Header
            for (Tenant n : groups.get(roomId)) {
                newList.add(new Row(n)); // Item
            }
        }

        this.displayList = newList;
        notifyDataSetChanged();
    }

    private boolean matchesFilter(Tenant n) {
        if (filterQuery.isEmpty())
            return true;
        String name = n.getHoTen() != null ? n.getHoTen().toLowerCase() : "";
        String sdt = n.getSoDienThoai() != null ? n.getSoDienThoai().toLowerCase() : "";
        return name.contains(filterQuery) || sdt.contains(filterQuery);
    }

    // ── RecyclerView boilerplate ─────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View v = inf.inflate(R.layout.item_tenant_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_tenant_card, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = displayList.get(position);
        if (row.isHeader()) {
            bindHeader((HeaderViewHolder) holder, row.header, position);
        } else {
            bindItem((ItemViewHolder) holder, row.tenant);
        }
    }

    // ── Bind Header ──────────────────────────────────────────────────────────

    private void bindHeader(HeaderViewHolder h, String title, int position) {
        h.tvTenPhong.setText(title);

        // Trích xuất soPhong, roomId và tenantId từ tenant đầu tiên trong nhóm
        String roomId   = null;
        String tenantId = null;   // ID document Tenant trên Firestore
        String soPhong  = title.replace("Phòng ", "").trim(); // fallback từ tiêu đề

        if (position + 1 < displayList.size()) {
            Row next = displayList.get(position + 1);
            if (!next.isHeader() && next.tenant != null) {
                Tenant firstTenant = next.tenant;
                roomId   = firstTenant.getIdPhong();
                tenantId = firstTenant.getId();
                if (firstTenant.getSoPhong() != null)
                    soPhong = firstTenant.getSoPhong();
            }
        }

        final String finalRoomId   = roomId;
        final String finalRoomName = title;
        final String finalSoPhong  = soPhong;
        final String finalTenantId = tenantId;

        // Nút [+ Thêm]
        h.btnThem.setOnClickListener(v ->
                { if (addListener != null) addListener.onAdd(finalRoomId, finalRoomName); });

        // Nút [Tự nhập] — xây dựng link pre-filled và mở hộp thoại chia sẻ
        if (h.btnTuNhap != null) {
            h.btnTuNhap.setOnClickListener(v -> {

                // Kiểm tra an toàn: cần có cả số phòng và tenant ID
                if (finalSoPhong == null || finalSoPhong.isEmpty()) {
                    android.widget.Toast.makeText(v.getContext(),
                            "Không xác định được số phòng",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (finalTenantId == null || finalTenantId.isEmpty()) {
                    android.widget.Toast.makeText(v.getContext(),
                            "Phòng chưa có khách thuê. Hãy thêm khách trước.",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Delegate tới Activity nếu có listener riêng
                if (selfEntryListener != null) {
                    selfEntryListener.onSelfEntry(finalSoPhong);
                    return;
                }

                // Xây dựng URL pre-filled: thay cả số phòng và tenant ID
                String finalUrl = GOOGLE_FORM_URL
                        .replace("999", finalSoPhong)
                        .replace("ABC_XYZ", finalTenantId);

                // Nội dung tin nhắn
                String message = "Chào bạn, mời bạn nhập thông tin khách thuê cho Phòng "
                        + finalSoPhong
                        + " tại đây: "
                        + finalUrl;

                // Mở hộp thoại chọn ứng dụng để gửi (Zalo, SMS, Messenger...)
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                v.getContext().startActivity(
                        Intent.createChooser(shareIntent, "Gửi link tự nhập qua..."));
            });
        }
    }

    // ── Bind Item ────────────────────────────────────────────────────────────

    private void bindItem(ItemViewHolder h, Tenant n) {
        // Thông tin cơ bản
        h.tvHoTen.setText(n.getHoTen() != null ? n.getHoTen() : "—");
        h.tvSdt.setText(n.getSoDienThoai() != null ? n.getSoDienThoai() : "—");

        // ── Badge vai trò (Hàng 1) ──────────────────────────────────────────
        // Người liên hệ
        if (n.isNguoiLienHe()) {
            h.badgeNguoiLienHe.setText("✓ Là người liên hệ");
            h.badgeNguoiLienHe.setTextColor(Color.parseColor("#388E3C")); // xanh lá
        } else {
            h.badgeNguoiLienHe.setText("Là người liên hệ");
            h.badgeNguoiLienHe.setTextColor(Color.parseColor("#888888")); // xám
        }

        // Đại diện hợp đồng
        if (n.isDaiDienHopDong()) {
            h.badgeDaiDienHD.setText("✓ Đại diện hợp đồng");
            h.badgeDaiDienHD.setTextColor(Color.parseColor("#1565C0")); // xanh dương
        } else {
            h.badgeDaiDienHD.setText("Đại diện hợp đồng");
            h.badgeDaiDienHD.setTextColor(Color.parseColor("#888888"));
        }

        // ── Badge trạng thái (Hàng 2) ───────────────────────────────────────
        // Tạm trú
        if (n.isTamTru()) {
            h.dotTamTru.setBackgroundResource(R.drawable.bg_icon_circle_green);
            h.badgeTamTru.setText("Đã đăng ký tạm trú");
            h.badgeTamTru.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.dotTamTru.setBackgroundResource(R.drawable.bg_icon_circle_orange);
            h.badgeTamTru.setText("Chưa đăng ký tạm trú");
            h.badgeTamTru.setTextColor(Color.parseColor("#FF6D00"));
        }

        // Giấy tờ
        if (n.isDayDuGiayTo()) {
            h.dotGiayTo.setBackgroundResource(R.drawable.bg_icon_circle_green);
            h.badgeGiayTo.setText("Đầy đủ giấy tờ");
            h.badgeGiayTo.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.dotGiayTo.setBackgroundResource(R.drawable.bg_icon_circle_orange);
            h.badgeGiayTo.setText("Chưa đầy đủ giấy tờ");
            h.badgeGiayTo.setTextColor(Color.parseColor("#FF6D00"));
        }

        // ── Nút Gọi điện (Intent.ACTION_DIAL) ──────────────────────────────
        h.btnGoi.setOnClickListener(v -> {
            String sdt = n.getSoDienThoai();
            if (sdt == null || sdt.trim().isEmpty())
                return;
            // Chuẩn hoá số: bỏ dấu gạch ngang / khoảng trắng
            String dialNumber = "tel:" + sdt.replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(dialNumber));
            v.getContext().startActivity(intent);
        });

        // ── Menu 3 chấm → Sửa / Xóa ────────────────────────────────────────
        h.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "Chỉnh sửa");
            popup.getMenu().add(0, 2, 1, "Xóa");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    actionListener.onSua(n);
                    return true;
                } else if (item.getItemId() == 2) {
                    actionListener.onXoa(n);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // ── ViewHolder: Header ───────────────────────────────────────────────────
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenPhong;
        View btnThem;
        View btnTuNhap;

        HeaderViewHolder(View v) {
            super(v);
            tvTenPhong = v.findViewById(R.id.tvTenPhong);
            btnThem = v.findViewById(R.id.btnThem);
            btnTuNhap = v.findViewById(R.id.btnTuNhap);
        }
    }

    // ── ViewHolder: Item ─────────────────────────────────────────────────────
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvHoTen, tvSdt;
        TextView badgeNguoiLienHe, badgeDaiDienHD;
        TextView badgeTamTru, badgeGiayTo;
        View dotTamTru, dotGiayTo;
        ImageButton btnGoi, btnMenu;

        ItemViewHolder(View v) {
            super(v);
            tvHoTen = v.findViewById(R.id.tvHoTen);
            tvSdt = v.findViewById(R.id.tvSdt);
            badgeNguoiLienHe = v.findViewById(R.id.badgeNguoiLienHe);
            badgeDaiDienHD = v.findViewById(R.id.badgeDaiDienHD);
            badgeTamTru = v.findViewById(R.id.badgeTamTru);
            badgeGiayTo = v.findViewById(R.id.badgeGiayTo);
            dotTamTru = v.findViewById(R.id.dotTamTru);
            dotGiayTo = v.findViewById(R.id.dotGiayTo);
            btnGoi = v.findViewById(R.id.btnGoi);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}

