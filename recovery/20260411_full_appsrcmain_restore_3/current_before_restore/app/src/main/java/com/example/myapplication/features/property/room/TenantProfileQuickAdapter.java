package com.example.myapplication.features.property.room;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.ContractMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TenantProfileQuickAdapter extends RecyclerView.Adapter<TenantProfileQuickAdapter.ViewHolder> {

    public interface OnProfileActionListener {
        void onCall(@NonNull ContractMember member);

        void onUpdate(@NonNull ContractMember member);
    }

    private final List<ContractMember> items = new ArrayList<>();
    private final OnProfileActionListener actionListener;

    public TenantProfileQuickAdapter() {
        this(null);
    }

    public TenantProfileQuickAdapter(OnProfileActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<ContractMember> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tenant_profile_quick, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContractMember member = items.get(position);

        String name = member.getFullName() != null && !member.getFullName().trim().isEmpty()
                ? member.getFullName().trim()
                : holder.itemView.getContext().getString(R.string.tenant_profile_unknown_name);
        String first = name.substring(0, 1).toUpperCase(Locale.ROOT);

        holder.tvProfileInitial.setText(first);
        holder.tvProfileName.setText(name);
        String roleText = member.isContractRepresentative()
            ? holder.itemView.getContext().getString(R.string.tenant_profile_role_representative)
            : holder.itemView.getContext().getString(R.string.tenant_profile_role_member);
        String roomNumber = member.getRoomNumber() != null ? member.getRoomNumber().trim() : "";
        if (!roomNumber.isEmpty()) {
            roleText = holder.itemView.getContext().getString(R.string.tenant_profile_role_with_room, roleText,
                roomNumber);
        }
        holder.tvProfileRole.setText(roleText);

        holder.tvProfileStatus.setText(member.isFullyDocumented()
                ? holder.itemView.getContext().getString(R.string.tenant_profile_status_ready)
                : holder.itemView.getContext().getString(R.string.tenant_profile_status_missing));
        holder.tvProfileStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            member.isFullyDocumented() ? Color.parseColor("#40FFFFFF") : Color.parseColor("#80FFCDD2")));

        String phone = member.getPhoneNumber();
        holder.tvProfilePhone.setText(!TextUtils.isEmpty(phone)
                ? phone
                : holder.itemView.getContext().getString(R.string.common_not_available));

        String personalId = member.getPersonalId();
        holder.tvProfilePersonalId.setText(!TextUtils.isEmpty(personalId)
                ? personalId
                : holder.itemView.getContext().getString(R.string.common_not_available));

        if (member.isPrimaryContact()) {
            holder.tvBadgePrimaryContact.setVisibility(View.VISIBLE);
            holder.tvBadgePrimaryContact.setText(R.string.item_tenant_primary_contact);
            holder.tvBadgePrimaryContact.setTextColor(Color.parseColor("#1E88E5"));
            holder.tvBadgePrimaryContact.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
        } else {
            holder.tvBadgePrimaryContact.setVisibility(View.GONE);
        }

        if (member.isTemporaryResident()) {
            holder.tvBadgeTemporaryResidence.setText(R.string.temporary_residence_registered);
            holder.tvBadgeTemporaryResidence.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvBadgeTemporaryResidence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
        } else {
            holder.tvBadgeTemporaryResidence.setText(R.string.temporary_residence_not_registered);
            holder.tvBadgeTemporaryResidence.setTextColor(Color.parseColor("#E65100"));
            holder.tvBadgeTemporaryResidence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
        }

        if (member.isFullyDocumented()) {
            holder.tvBadgeDocuments.setText(R.string.documents_complete);
            holder.tvBadgeDocuments.setTextColor(Color.parseColor("#1565C0"));
            holder.tvBadgeDocuments.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
        } else {
            holder.tvBadgeDocuments.setText(R.string.documents_incomplete);
            holder.tvBadgeDocuments.setTextColor(Color.parseColor("#6A1B9A"));
            holder.tvBadgeDocuments.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F3E5F5")));
        }

        if (actionListener != null) {
            holder.profileActionsRow.setVisibility(View.VISIBLE);
            holder.btnProfileCall.setOnClickListener(v -> actionListener.onCall(member));
            holder.btnProfileUpdate.setOnClickListener(v -> actionListener.onUpdate(member));
        } else {
            holder.profileActionsRow.setVisibility(View.GONE);
            holder.btnProfileCall.setOnClickListener(null);
            holder.btnProfileUpdate.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProfileInitial;
        TextView tvProfileName;
        TextView tvProfileRole;
        TextView tvProfileStatus;
        TextView tvProfilePhone;
        TextView tvProfilePersonalId;
        TextView tvBadgePrimaryContact;
        TextView tvBadgeTemporaryResidence;
        TextView tvBadgeDocuments;
        View profileActionsRow;
        TextView btnProfileCall;
        TextView btnProfileUpdate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            tvProfileRole = itemView.findViewById(R.id.tvProfileRole);
            tvProfileStatus = itemView.findViewById(R.id.tvProfileStatus);
            tvProfilePhone = itemView.findViewById(R.id.tvProfilePhone);
            tvProfilePersonalId = itemView.findViewById(R.id.tvProfilePersonalId);
            tvBadgePrimaryContact = itemView.findViewById(R.id.tvBadgePrimaryContact);
            tvBadgeTemporaryResidence = itemView.findViewById(R.id.tvBadgeTemporaryResidence);
            tvBadgeDocuments = itemView.findViewById(R.id.tvBadgeDocuments);
            profileActionsRow = itemView.findViewById(R.id.profileActionsRow);
            btnProfileCall = itemView.findViewById(R.id.btnProfileCall);
            btnProfileUpdate = itemView.findViewById(R.id.btnProfileUpdate);
        }
    }
}
