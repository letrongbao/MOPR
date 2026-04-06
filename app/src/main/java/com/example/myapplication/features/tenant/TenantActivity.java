package com.example.myapplication.features.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Room;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.features.contract.ContractActivity;
import com.example.myapplication.features.contract.ContractIntentKeys;
import com.example.myapplication.features.property.room.RoomActivity;
import com.example.myapplication.viewmodel.TenantViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TenantActivity extends AppCompatActivity {

    private String preselectRoomId;
    private boolean didAutoOpenAddDialog;

    private TenantAdapter adapter;
    private TextView tvEmpty;
    private List<Room> danhSachPhong = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_tenant);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_management_title));

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);
        EditText etTimKiem = findViewById(R.id.etTimKiem);

        // Internal note.
        adapter = new TenantAdapter(new TenantAdapter.OnItemActionListener() {
            @Override
            public void onDelete(Tenant tenant) {
                openContractEditScreen(tenant);
                Toast.makeText(TenantActivity.this, getString(R.string.tenant_manage_moved_to_contract),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSua(Tenant tenant) {
                openContractEditScreen(tenant);
            }
        });

        // Internal note.
        adapter.setOnAddToRoomListener((roomId, roomName) -> openContractCreateScreen(roomId));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        preselectRoomId = getIntent().getStringExtra("PRESELECT_ROOM_ID");

        // Internal note.
        if (etTimKiem != null) {
            etTimKiem.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                    adapter.filter(s.toString());
                    // Internal note.
                    tvEmpty.setVisibility(
                            adapter.getTenantCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
        }

        TenantViewModel tenantViewModel = new ViewModelProvider(this).get(TenantViewModel.class);
        tenantViewModel.getTenantList().observe(this, list -> {
            // Internal note.
            adapter.setData(list);
            tvEmpty.setVisibility(
                    (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        RoomViewModel roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        roomViewModel
                .getRoomList().observe(this, list -> {
                    danhSachPhong = list;
                    maybeAutoOpenAddDialog();
                });

        fabThem.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.tenant_add_moved_to_room_details), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, RoomActivity.class));
        });
    }

    private void maybeAutoOpenAddDialog() {
        if (didAutoOpenAddDialog)
            return;
        if (preselectRoomId == null || preselectRoomId.trim().isEmpty())
            return;
        if (danhSachPhong == null || danhSachPhong.isEmpty())
            return;

        didAutoOpenAddDialog = true;
        String roomId = preselectRoomId;
        preselectRoomId = null;
        openContractCreateScreen(roomId);
    }

    private void openContractCreateScreen(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.tenant_add_moved_to_room_details), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, RoomActivity.class));
            return;
        }

        Intent intent = new Intent(this, ContractActivity.class);
        intent.putExtra(ContractActivity.EXTRA_ROOM_ID, roomId);
        startActivity(intent);
    }

    private void openContractEditScreen(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getId().trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.error_contract_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ContractActivity.class);
        String roomId = tenant.getRoomId();
        if (roomId != null && !roomId.trim().isEmpty()) {
            intent.putExtra(ContractActivity.EXTRA_ROOM_ID, roomId);
        } else {
            intent.putExtra(ContractIntentKeys.MODE, "EDIT");
            intent.putExtra(ContractIntentKeys.CONTRACT_ID, tenant.getId());
            intent.putExtra(ContractIntentKeys.ROOM_ID, roomId);
        }
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
