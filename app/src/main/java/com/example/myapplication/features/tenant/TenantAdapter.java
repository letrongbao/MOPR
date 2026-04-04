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
import java.util.Locale;
import java.util.Map;

/**
 * Internal note.
 *
 * Internal note.
 * Internal note.
 */
public class TenantAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Internal note.
    // Internal note.
    // Internal note.
    private static final String GOOGLE_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSfeQn8xbJTbHw4FHbqSqZpC87uxfy34w0l211T5vHj66VdVUw/viewform?usp=pp_url&entry.166517188=999&entry.1163193725=ABC_XYZ";

    // Internal note.
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    // Internal note.

    private static class Row {
        final String header;
        final Tenant tenant;

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

    // Internal note.

    private List<Tenant> fullList = new ArrayList<>();

    private List<Row> displayList = new ArrayList<>();

    private String filterQuery = "";

    // Internal note.
    public interface OnItemActionListener {
        void onDelete(Tenant tenant);

        void onSua(Tenant tenant);
    }

    public interface OnAddToRoomListener {
        void onAdd(String roomId, String roomName);
    }

    public interface OnSelfEntryListener {

        void onSelfEntry(String roomNumber);
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

    // Internal note.

    /**
     * Internal note.
     * Internal note.
     */
    public void setData(List<Tenant> list) {
        this.fullList = list != null ? list : new ArrayList<>();
        rebuildDisplay();
    }

    public void filter(String query) {
        this.filterQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        rebuildDisplay();
    }

    public int getTenantCount() {
        int count = 0;
        for (Row r : displayList)
            if (!r.isHeader())
                count++;
        return count;
    }

    // Internal note.

    private void rebuildDisplay() {
        // Internal note.
        List<Tenant> filtered = new ArrayList<>();
        for (Tenant n : fullList) {
            if (matchesFilter(n))
                filtered.add(n);
        }

        // Internal note.
        // Internal note.
        Map<String, List<Tenant>> groups = new LinkedHashMap<>();
        Map<String, String> roomNames = new LinkedHashMap<>();

        for (Tenant n : filtered) {
            String roomId = n.getRoomId() != null ? n.getRoomId() : "unknown";
            String roomNumber = n.getRoomNumber() != null ? n.getRoomNumber() : roomId;

            if (!groups.containsKey(roomId)) {
                groups.put(roomId, new ArrayList<>());
                roomNames.put(roomId, roomNumber);
            }
            groups.get(roomId).add(n);
        }

        // Internal note.
        List<String> sortedRoomIds = new ArrayList<>(groups.keySet());
        Collections.sort(sortedRoomIds, (a, b) -> {
            String nameA = roomNames.getOrDefault(a, a);
            String nameB = roomNames.getOrDefault(b, b);
            // Internal note.
            try {
                return Integer.compare(Integer.parseInt(nameA), Integer.parseInt(nameB));
            } catch (NumberFormatException ignored) {
            }
            return nameA.compareToIgnoreCase(nameB);
        });

        // Internal note.
        List<Row> newList = new ArrayList<>();
        for (String roomId : sortedRoomIds) {
            String label = roomNames.getOrDefault(roomId, roomId);
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
        String name = n.getFullName() != null ? n.getFullName().toLowerCase(Locale.ROOT) : "";
        String phoneNumber = n.getPhoneNumber() != null ? n.getPhoneNumber().toLowerCase(Locale.ROOT) : "";
        return name.contains(filterQuery) || phoneNumber.contains(filterQuery);
    }

    // Internal note.

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

    // Internal note.

    private void bindHeader(HeaderViewHolder h, String title, int position) {
        h.tvRoomName.setText(h.itemView.getContext().getString(R.string.room_number, title));

        // Internal note.
        String roomId = null;
        String tenantId = null;
        String roomNumber = title;

        if (position + 1 < displayList.size()) {
            Row next = displayList.get(position + 1);
            if (!next.isHeader() && next.tenant != null) {
                Tenant firstTenant = next.tenant;
                roomId = firstTenant.getRoomId();
                tenantId = firstTenant.getId();
                if (firstTenant.getRoomNumber() != null)
                    roomNumber = firstTenant.getRoomNumber();
            }
        }

        final String finalRoomId = roomId;
        final String finalRoomName = h.itemView.getContext().getString(R.string.room_number, title);
        final String finalSoPhong = roomNumber;
        final String finalTenantId = tenantId;

        // Internal note.
        h.btnAdd.setOnClickListener(v -> {
            if (addListener != null)
                addListener.onAdd(finalRoomId, finalRoomName);
        });

        // Internal note.
        if (h.btnManualInput != null) {
            h.btnManualInput.setOnClickListener(v -> {

                // Internal note.
                if (finalSoPhong == null || finalSoPhong.isEmpty()) {
                    android.widget.Toast.makeText(v.getContext(),
                            v.getContext().getString(R.string.cannot_identify_room_number),
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (finalTenantId == null || finalTenantId.isEmpty()) {
                    android.widget.Toast.makeText(v.getContext(),
                            v.getContext().getString(R.string.room_no_tenant_add_first),
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Internal note.
                if (selfEntryListener != null) {
                    selfEntryListener.onSelfEntry(finalSoPhong);
                    return;
                }

                // Internal note.
                String finalUrl = GOOGLE_FORM_URL
                        .replace("999", finalSoPhong)
                        .replace("ABC_XYZ", finalTenantId);

                // Internal note.
                String message = v.getContext().getString(R.string.tenant_self_entry_message,
                        finalSoPhong, finalUrl);

                // Internal note.
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                v.getContext().startActivity(
                        Intent.createChooser(shareIntent, v.getContext().getString(R.string.send_link_via)));
            });
        }
    }

    // Internal note.

    private void bindItem(ItemViewHolder h, Tenant n) {
        // Internal note.
        h.tvFullName.setText(n.getFullName() != null ? n.getFullName() : "—");
        h.tvPhoneNumber.setText(n.getPhoneNumber() != null ? n.getPhoneNumber() : "—");

        // Internal note.
        // Internal note.
        if (n.isPrimaryContact()) {
            h.badgePrimaryContact.setText(h.itemView.getContext().getString(R.string.is_primary_contact));
            h.badgePrimaryContact.setTextColor(Color.parseColor("#388E3C"));
        } else {
            h.badgePrimaryContact.setText(h.itemView.getContext().getString(R.string.primary_contact_label));
            h.badgePrimaryContact.setTextColor(Color.parseColor("#888888"));
        }

        // Internal note.
        if (n.isContractRepresentative()) {
            h.badgeContractRepresentative.setText(h.itemView.getContext().getString(R.string.is_contract_representative));
            h.badgeContractRepresentative.setTextColor(Color.parseColor("#1565C0"));
        } else {
            h.badgeContractRepresentative.setText(h.itemView.getContext().getString(R.string.contract_representative_label));
            h.badgeContractRepresentative.setTextColor(Color.parseColor("#888888"));
        }

        // Internal note.
        // Internal note.
        if (n.isTemporaryResident()) {
            h.dotTemporaryResidence.setBackgroundResource(R.drawable.bg_icon_circle_green);
            h.badgeTemporaryResidence.setText(h.itemView.getContext().getString(R.string.temporary_residence_registered));
            h.badgeTemporaryResidence.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.dotTemporaryResidence.setBackgroundResource(R.drawable.bg_icon_circle_orange);
            h.badgeTemporaryResidence.setText(h.itemView.getContext().getString(R.string.temporary_residence_not_registered));
            h.badgeTemporaryResidence.setTextColor(Color.parseColor("#FF6D00"));
        }

        // Internal note.
        if (n.isFullyDocumented()) {
            h.dotDocuments.setBackgroundResource(R.drawable.bg_icon_circle_green);
            h.badgeDocuments.setText(h.itemView.getContext().getString(R.string.documents_complete));
            h.badgeDocuments.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.dotDocuments.setBackgroundResource(R.drawable.bg_icon_circle_orange);
            h.badgeDocuments.setText(h.itemView.getContext().getString(R.string.documents_incomplete));
            h.badgeDocuments.setTextColor(Color.parseColor("#FF6D00"));
        }

        // Internal note.
        h.btnCall.setOnClickListener(v -> {
            String phoneNumber = n.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.trim().isEmpty())
                return;
            // Internal note.
            String dialNumber = "tel:" + phoneNumber.replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(dialNumber));
            v.getContext().startActivity(intent);
        });

        // Internal note.
        h.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, v.getContext().getString(R.string.edit));
            popup.getMenu().add(0, 2, 1, v.getContext().getString(R.string.delete));
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    actionListener.onSua(n);
                    return true;
                } else if (item.getItemId() == 2) {
                    actionListener.onDelete(n);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // Internal note.
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName;
        View btnAdd;
        View btnManualInput;

        HeaderViewHolder(View v) {
            super(v);
            tvRoomName = v.findViewById(R.id.tvTenPhong);
            btnAdd = v.findViewById(R.id.btnThem);
            btnManualInput = v.findViewById(R.id.btnTuNhap);
        }
    }

    // Internal note.
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvPhoneNumber;
        TextView badgePrimaryContact, badgeContractRepresentative;
        TextView badgeTemporaryResidence, badgeDocuments;
        View dotTemporaryResidence, dotDocuments;
        ImageButton btnCall, btnMenu;

        ItemViewHolder(View v) {
            super(v);
            tvFullName = v.findViewById(R.id.tvHoTen);
            tvPhoneNumber = v.findViewById(R.id.tvSdt);
            badgePrimaryContact = v.findViewById(R.id.badgePrimaryContact);
            badgeContractRepresentative = v.findViewById(R.id.badgeDaiDienHD);
            badgeTemporaryResidence = v.findViewById(R.id.badgeTamTru);
            badgeDocuments = v.findViewById(R.id.badgeGiayTo);
            dotTemporaryResidence = v.findViewById(R.id.dotTamTru);
            dotDocuments = v.findViewById(R.id.dotGiayTo);
            btnCall = v.findViewById(R.id.btnGoi);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
