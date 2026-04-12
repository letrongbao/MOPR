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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantProfileQuickAdapter extends RecyclerView.Adapter<TenantProfileQuickAdapter.ViewHolder> {

    public interface OnProfileActionListener {
        void onCall(@NonNull ContractMember member);

        void onUpdate(@NonNull ContractMember member);

        void onViewPersonalIdImage(@NonNull ContractMember member);

        void onConfirmTemporaryResidence(@NonNull ContractMember member);
    }

    private final List<ContractMember> items = new ArrayList<>();
    private final OnProfileActionListener actionListener;
    private final Map<String, String> roomLabelById = new HashMap<>();
    private final Map<String, String> roomHouseById = new HashMap<>();
    private final Map<String, String> houseLabelById = new HashMap<>();

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

    public void setLocationContext(Map<String, String> roomLabels,
            Map<String, String> roomHouses,
            Map<String, String> houseLabels) {
        roomLabelById.clear();
        roomHouseById.clear();
        houseLabelById.clear();
        if (roomLabels != null) {
            roomLabelById.putAll(roomLabels);
        }
        if (roomHouses != null) {
            roomHouseById.putAll(roomHouses);
        }
        if (houseLabels != null) {
            houseLabelById.putAll(houseLabels);
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
        String roomNumber = resolveRoomLabel(member);
        String houseLabel = resolveHouseLabel(member, holder.itemView.getContext());
        String roomHouseLine = holder.itemView.getContext().getString(
                R.string.tenant_profile_room_house_line,
                roomNumber,
                houseLabel);
        roleText = roleText + "\n" + roomHouseLine;
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

        if (member.isTemporaryResident()) {
            holder.tvBadgeTemporaryResidence.setText(R.string.temporary_residence_registered);
            holder.tvBadgeTemporaryResidence.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvBadgeTemporaryResidence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
        } else {
            holder.tvBadgeTemporaryResidence.setText(R.string.temporary_residence_not_registered);
            holder.tvBadgeTemporaryResidence.setTextColor(Color.parseColor("#E65100"));
            holder.tvBadgeTemporaryResidence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
        }

        if (member.isTemporaryAbsent()) {
            holder.tvBadgeTemporaryAbsence.setText(R.string.temporary_absence_registered);
            holder.tvBadgeTemporaryAbsence.setTextColor(Color.parseColor("#6A1B9A"));
            holder.tvBadgeTemporaryAbsence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F3E5F5")));
        } else {
            holder.tvBadgeTemporaryAbsence.setText(R.string.temporary_absence_not_registered);
            holder.tvBadgeTemporaryAbsence.setTextColor(Color.parseColor("#E65100"));
            holder.tvBadgeTemporaryAbsence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
        }

        if (actionListener != null) {
            holder.profileActionsRow.setVisibility(View.VISIBLE);
            holder.btnProfileCall.setOnClickListener(v -> actionListener.onCall(member));

            if (!TextUtils.isEmpty(member.getPersonalIdImageUrl())) {
                holder.btnProfileViewIdImage.setVisibility(View.VISIBLE);
                holder.btnProfileViewIdImage.setOnClickListener(v -> actionListener.onViewPersonalIdImage(member));
            } else {
                holder.btnProfileViewIdImage.setVisibility(View.GONE);
                holder.btnProfileViewIdImage.setOnClickListener(null);
            }

            if (member.isFullyDocumented() && !(member.isTemporaryResident() && member.isTemporaryAbsent())) {
                holder.btnProfileConfirmResidenceAbsence.setVisibility(View.VISIBLE);
                holder.btnProfileConfirmResidenceAbsence
                        .setText(R.string.tenant_profile_action_confirm_temporary_residence_absence);
                holder.btnProfileConfirmResidenceAbsence
                        .setOnClickListener(v -> actionListener.onConfirmTemporaryResidence(member));
            } else {
                holder.btnProfileConfirmResidenceAbsence.setVisibility(View.GONE);
                holder.btnProfileConfirmResidenceAbsence.setOnClickListener(null);
            }
        } else {
            holder.profileActionsRow.setVisibility(View.GONE);
            holder.btnProfileCall.setOnClickListener(null);
            holder.btnProfileViewIdImage.setOnClickListener(null);
            holder.btnProfileConfirmResidenceAbsence.setOnClickListener(null);
        }
    }

    private String resolveRoomLabel(@NonNull ContractMember member) {
        String roomId = member.getRoomId();
        if (roomId != null && roomLabelById.containsKey(roomId)) {
            return roomLabelById.get(roomId);
        }
        String roomNumber = member.getRoomNumber() != null ? member.getRoomNumber().trim() : "";
        if (!roomNumber.isEmpty()) {
            return roomNumber;
        }
        return roomId != null && !roomId.trim().isEmpty() ? roomId : "-";
    }

    private String resolveHouseLabel(@NonNull ContractMember member, @NonNull android.content.Context context) {
        String roomId = member.getRoomId();
        if (roomId != null) {
            String houseId = roomHouseById.get(roomId);
            if (houseId != null) {
                String label = houseLabelById.get(houseId);
                if (!TextUtils.isEmpty(label)) {
                    return label;
                }
            }
        }
        return context.getString(R.string.tenant_profile_house_unknown);
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
        TextView tvBadgeTemporaryResidence;
        TextView tvBadgeTemporaryAbsence;
        View profileActionsRow;
        TextView btnProfileCall;
        TextView btnProfileViewIdImage;
        TextView btnProfileConfirmResidenceAbsence;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            tvProfileRole = itemView.findViewById(R.id.tvProfileRole);
            tvProfileStatus = itemView.findViewById(R.id.tvProfileStatus);
            tvProfilePhone = itemView.findViewById(R.id.tvProfilePhone);
            tvProfilePersonalId = itemView.findViewById(R.id.tvProfilePersonalId);
            tvBadgeTemporaryResidence = itemView.findViewById(R.id.tvBadgeTemporaryResidence);
            tvBadgeTemporaryAbsence = itemView.findViewById(R.id.tvBadgeTemporaryAbsence);
            profileActionsRow = itemView.findViewById(R.id.profileActionsRow);
            btnProfileCall = itemView.findViewById(R.id.btnProfileCall);
            btnProfileViewIdImage = itemView.findViewById(R.id.btnProfileViewIdImage);
            btnProfileConfirmResidenceAbsence = itemView.findViewById(R.id.btnProfileConfirmResidenceAbsence);
        }
    }
}
