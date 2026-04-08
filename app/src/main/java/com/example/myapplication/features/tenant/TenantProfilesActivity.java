package com.example.myapplication.features.tenant;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractMember;
import com.example.myapplication.features.contract.ContractActivity;
import com.example.myapplication.features.property.room.TenantProfileQuickAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class TenantProfilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TenantProfileQuickAdapter adapter;
    private LinearProgressIndicator progressBar;
    private TextView tvEmpty;
    private TextView tvProfileCount;
    private MaterialButton btnSort;
    private MaterialButton btnSelectHouse;
    private MaterialButton btnSelectRoom;

    private final List<ContractMember> profiles = new ArrayList<>();
    private ListenerRegistration memberListener;
    private ListenerRegistration roomListener;
    private ListenerRegistration houseListener;

    private final Map<String, String> houseNameById = new HashMap<>();
    private final Map<String, String> roomLabelById = new HashMap<>();
    private final Map<String, String> roomHouseById = new HashMap<>();
    private String selectedHouseId;
    private String selectedRoomId;

    private enum SortOption {
        REPRESENTATIVE_FIRST,
        NAME_AZ,
        ROOM_ASC
    }

    private SortOption currentSort = SortOption.REPRESENTATIVE_FIRST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_tenant_profiles);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_profiles_title));

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvProfileCount = findViewById(R.id.tvProfileCount);
        btnSort = findViewById(R.id.btnSort);
        btnSelectHouse = findViewById(R.id.btnSelectHouse);
        btnSelectRoom = findViewById(R.id.btnSelectRoom);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TenantProfileQuickAdapter(new TenantProfileQuickAdapter.OnProfileActionListener() {
            @Override
            public void onCall(@androidx.annotation.NonNull ContractMember member) {
                openDial(member.getPhoneNumber());
            }

            @Override
            public void onUpdate(@androidx.annotation.NonNull ContractMember member) {
                openContractUpdate(member);
            }
        });
        recyclerView.setAdapter(adapter);

        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortDialog());
        }
        if (btnSelectHouse != null) {
            btnSelectHouse.setOnClickListener(v -> showHouseFilterDialog());
        }
        if (btnSelectRoom != null) {
            btnSelectRoom.setOnClickListener(v -> showRoomFilterDialog());
        }

        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        subscribeProfiles(scopeDoc);
        subscribeFilterSources(scopeDoc);
        updateFilterButtons();
    }

    private void subscribeProfiles(@androidx.annotation.NonNull DocumentReference scopeDoc) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        memberListener = scopeDoc.collection("contractMembers")
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || snapshot == null) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(getString(R.string.error_load_data));
                        return;
                    }

                    profiles.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        ContractMember member = doc.toObject(ContractMember.class);
                        if (member != null) {
                            member.setId(doc.getId());
                            profiles.add(member);
                        }
                    });

                    if (profiles.isEmpty()) {
                        adapter.submitList(new ArrayList<>());
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvProfileCount.setText(getString(R.string.tenant_profiles_count_default));
                        return;
                    }

                    applyFiltersAndSort();
                });
    }

    private void subscribeFilterSources(@androidx.annotation.NonNull DocumentReference scopeDoc) {
        roomListener = scopeDoc.collection("rooms")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }
                    roomLabelById.clear();
                    roomHouseById.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String roomId = doc.getId();
                        String roomNumber = doc.getString("roomNumber");
                        String roomLabel = roomNumber != null && !roomNumber.trim().isEmpty()
                                ? roomNumber.trim()
                                : roomId;
                        roomLabelById.put(roomId, roomLabel);
                        String houseId = doc.getString("houseId");
                        if (houseId != null) {
                            roomHouseById.put(roomId, houseId);
                        }
                    });
                    if (selectedRoomId != null && !roomLabelById.containsKey(selectedRoomId)) {
                        selectedRoomId = null;
                    }
                    ensureRoomSelectionBelongsToHouse();
                    updateFilterButtons();
                    applyFiltersAndSort();
                });

        houseListener = scopeDoc.collection("houses")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }
                    houseNameById.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String houseId = doc.getId();
                        String houseName = doc.getString("houseName");
                        String address = doc.getString("address");
                        String label;
                        if (houseName != null && !houseName.trim().isEmpty()) {
                            label = houseName.trim();
                        } else if (address != null && !address.trim().isEmpty()) {
                            label = address.trim();
                        } else {
                            label = houseId;
                        }
                        houseNameById.put(houseId, label);
                    });
                    if (selectedHouseId != null && !houseNameById.containsKey(selectedHouseId)) {
                        selectedHouseId = null;
                        ensureRoomSelectionBelongsToHouse();
                    }
                    updateFilterButtons();
                    applyFiltersAndSort();
                });
    }

    private DocumentReference resolveScopeDoc() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return FirebaseFirestore.getInstance().collection("tenants").document(tenantId);
        }
        return FirebaseFirestore.getInstance().collection("users").document(user.getUid());
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.tenant_profiles_sort_representative_first),
                getString(R.string.sort_tenant_name_az),
                getString(R.string.tenant_profiles_sort_room)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sort_by))
                .setSingleChoiceItems(options, currentSort.ordinal(), (dialog, which) -> {
                    currentSort = SortOption.values()[which];
                    applySorting(currentSort);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showHouseFilterDialog() {
        ArrayList<String> houseIds = new ArrayList<>(houseNameById.keySet());
        Collections.sort(houseIds, (a, b) -> houseNameById.get(a).compareToIgnoreCase(houseNameById.get(b)));

        ArrayList<String> labels = new ArrayList<>();
        labels.add(getString(R.string.tenant_profiles_filter_house_all));
        for (String houseId : houseIds) {
            labels.add(houseNameById.get(houseId));
        }

        int checked = 0;
        if (selectedHouseId != null) {
            int idx = houseIds.indexOf(selectedHouseId);
            if (idx >= 0) {
                checked = idx + 1;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tenant_profiles_select_house))
                .setSingleChoiceItems(labels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedHouseId = which == 0 ? null : houseIds.get(which - 1);
                    ensureRoomSelectionBelongsToHouse();
                    updateFilterButtons();
                    applyFiltersAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showRoomFilterDialog() {
        ArrayList<String> roomIds = new ArrayList<>();
        for (String roomId : roomLabelById.keySet()) {
            if (selectedHouseId == null || selectedHouseId.equals(roomHouseById.get(roomId))) {
                roomIds.add(roomId);
            }
        }
        Collections.sort(roomIds, (a, b) -> roomLabelById.get(a).compareToIgnoreCase(roomLabelById.get(b)));

        ArrayList<String> labels = new ArrayList<>();
        labels.add(getString(R.string.tenant_profiles_filter_room_all));
        for (String roomId : roomIds) {
            labels.add(roomLabelById.get(roomId));
        }

        int checked = 0;
        if (selectedRoomId != null) {
            int idx = roomIds.indexOf(selectedRoomId);
            if (idx >= 0) {
                checked = idx + 1;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tenant_profiles_select_room))
                .setSingleChoiceItems(labels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedRoomId = which == 0 ? null : roomIds.get(which - 1);
                    updateFilterButtons();
                    applyFiltersAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void ensureRoomSelectionBelongsToHouse() {
        if (selectedHouseId == null || selectedRoomId == null) {
            return;
        }
        String roomHouseId = roomHouseById.get(selectedRoomId);
        if (roomHouseId == null || !selectedHouseId.equals(roomHouseId)) {
            selectedRoomId = null;
        }
    }

    private void updateFilterButtons() {
        if (btnSelectHouse != null) {
            String houseLabel = selectedHouseId != null && houseNameById.containsKey(selectedHouseId)
                    ? houseNameById.get(selectedHouseId)
                    : getString(R.string.tenant_profiles_filter_house_all);
            btnSelectHouse.setText(houseLabel);
        }
        if (btnSelectRoom != null) {
            String roomLabel = selectedRoomId != null && roomLabelById.containsKey(selectedRoomId)
                    ? roomLabelById.get(selectedRoomId)
                    : getString(R.string.tenant_profiles_filter_room_all);
            btnSelectRoom.setText(roomLabel);
        }
    }

    private void applySorting(SortOption option) {
        ArrayList<ContractMember> out = new ArrayList<>();
        for (ContractMember member : profiles) {
            if (member == null) {
                continue;
            }
            String roomId = member.getRoomId();
            if (selectedHouseId != null) {
                String houseId = roomId != null ? roomHouseById.get(roomId) : null;
                if (houseId == null || !selectedHouseId.equals(houseId)) {
                    continue;
                }
            }
            if (selectedRoomId != null) {
                if (roomId == null || !selectedRoomId.equals(roomId)) {
                    continue;
                }
            }
            out.add(member);
        }

        if (option == SortOption.REPRESENTATIVE_FIRST) {
            Collections.sort(out, (a, b) -> {
                if (a.isContractRepresentative() != b.isContractRepresentative()) {
                    return a.isContractRepresentative() ? -1 : 1;
                }
                if (a.isPrimaryContact() != b.isPrimaryContact()) {
                    return a.isPrimaryContact() ? -1 : 1;
                }
                String roomA = resolveRoomLabel(a);
                String roomB = resolveRoomLabel(b);
                int roomCmp = roomA.compareToIgnoreCase(roomB);
                if (roomCmp != 0) {
                    return roomCmp;
                }
                String nameA = a.getFullName() != null ? a.getFullName() : "";
                String nameB = b.getFullName() != null ? b.getFullName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });
        } else if (option == SortOption.NAME_AZ) {
            Collections.sort(out, (a, b) -> {
                String nameA = a.getFullName() != null ? a.getFullName() : "";
                String nameB = b.getFullName() != null ? b.getFullName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });
        } else {
            Collections.sort(out, (a, b) -> {
                String roomA = resolveRoomLabel(a);
                String roomB = resolveRoomLabel(b);
                return roomA.compareToIgnoreCase(roomB);
            });
        }

        tvProfileCount.setText(out.isEmpty()
                ? getString(R.string.tenant_profiles_count_default)
                : getString(R.string.tenant_profiles_count, out.size()));
        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitList(out);
    }

    private void applyFiltersAndSort() {
        applySorting(currentSort);
    }

    private String resolveRoomLabel(@androidx.annotation.NonNull ContractMember member) {
        String roomNumber = member.getRoomNumber();
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            return roomNumber.trim();
        }
        String roomId = member.getRoomId();
        if (roomId != null && roomLabelById.containsKey(roomId)) {
            return roomLabelById.get(roomId);
        }
        return "";
    }

    private void openDial(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.common_not_available), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone.trim()));
        startActivity(intent);
    }

    private void openContractUpdate(@androidx.annotation.NonNull ContractMember member) {
        String roomId = member.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.room_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ContractActivity.class);
        intent.putExtra(ContractActivity.EXTRA_ROOM_ID, roomId);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (memberListener != null) {
            memberListener.remove();
            memberListener = null;
        }
        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }
        if (houseListener != null) {
            houseListener.remove();
            houseListener = null;
        }
    }
}
