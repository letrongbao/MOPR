package com.example.myapplication.features.org;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;

import java.util.ArrayList;
import java.util.List;

public class OrgInviteAdapter extends RecyclerView.Adapter<OrgInviteAdapter.VH> {

    public interface RevokeCallback {
        void onRevoke(@NonNull String code);
    }

    public static class InviteItem {
        public final String code;
        public final String email;
        public final String role;
        public final String status;
        public final String roomId;

        public InviteItem(@NonNull String code, String email, String role, String status, String roomId) {
            this.code = code;
            this.email = email;
            this.role = role;
            this.status = status;
            this.roomId = roomId;
        }
    }

    private final List<InviteItem> items = new ArrayList<>();
    private final RevokeCallback cb;

    public OrgInviteAdapter(@NonNull RevokeCallback cb) {
        this.cb = cb;
    }

    public void setItems(@NonNull List<InviteItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_invite, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        InviteItem it = items.get(position);
        h.tvCode.setText(it.code);
        h.tvEmail.setText(it.email != null ? it.email : "");
        String role = it.role != null ? it.role : "";
        if (TenantRoles.TENANT.equals(role) && it.roomId != null && !it.roomId.trim().isEmpty()) {
            role = role + " (roomId=" + it.roomId + ")";
        }
        h.tvRole.setText(role);
        h.tvStatus.setText(it.status != null ? it.status : "");

        boolean revokable = it.status == null || "PENDING".equals(it.status);
        h.btnRevoke.setVisibility(revokable ? View.VISIBLE : View.GONE);
        h.btnRevoke.setOnClickListener(v -> cb.onRevoke(it.code));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvEmail, tvRole, tvStatus;
        Button btnRevoke;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnRevoke = itemView.findViewById(R.id.btnRevoke);
        }
    }
}
