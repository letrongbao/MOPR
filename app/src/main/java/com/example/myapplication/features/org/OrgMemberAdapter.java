package com.example.myapplication.features.org;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;

import java.util.ArrayList;
import java.util.List;

public class OrgMemberAdapter extends RecyclerView.Adapter<OrgMemberAdapter.VH> {

    public static class MemberItem {
        public final String uid;
        public final String role;
        public final String roomId;

        public MemberItem(@NonNull String uid, String role, String roomId) {
            this.uid = uid;
            this.role = role;
            this.roomId = roomId;
        }
    }

    private final List<MemberItem> items = new ArrayList<>();

    public void setItems(@NonNull List<MemberItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_member, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MemberItem it = items.get(position);
        h.tvUid.setText(it.uid);
        String role = it.role != null ? it.role : "";
        if (TenantRoles.TENANT.equals(role) && it.roomId != null && !it.roomId.trim().isEmpty()) {
            role = role + " (roomId=" + it.roomId + ")";
        }
        h.tvRole.setText(role);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUid, tvRole;

        VH(@NonNull View itemView) {
            super(itemView);
            tvUid = itemView.findViewById(R.id.tvUid);
            tvRole = itemView.findViewById(R.id.tvRole);
        }
    }
}
