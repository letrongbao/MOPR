package com.example.myapplication.features.contract;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.TenantRepository;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class ContractListActivity extends AppCompatActivity {

    private ContractListAdapter adapter;
    private TenantRepository repo;

    private TextView tvCountDangThue, tvCountSapHet, tvCountKetThuc;
    private TextView tvDueIn7Days, tvDueIn30Days, tvExpiredNeedAction;
    private LinearLayout layoutEmpty;
    private RecyclerView rvHopDong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_contract_list);

        // Internal note.
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        // Internal note.
        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.contract_list_title));

        // Internal note.
        tvCountDangThue = findViewById(R.id.tvCountDangThue);
        tvCountSapHet = findViewById(R.id.tvCountSapHet);
        tvCountKetThuc = findViewById(R.id.tvCountKetThuc);
        tvDueIn7Days = findViewById(R.id.tvDueIn7Days);
        tvDueIn30Days = findViewById(R.id.tvDueIn30Days);
        tvExpiredNeedAction = findViewById(R.id.tvExpiredNeedAction);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvHopDong = findViewById(R.id.rvHopDong);

        // RecyclerView + Adapter
        adapter = new ContractListAdapter();
        rvHopDong.setLayoutManager(new LinearLayoutManager(this));
        rvHopDong.setAdapter(adapter);

        // SearchView
        EditText etSearch = findViewById(R.id.etSearchHd);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    adapter.filter(s.toString());
                    updateEmptyState();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        setupLegalFilterChips();

        // Load data
        repo = new TenantRepository();
        loadData();
    }

    private void loadData() {
        repo.getTenantList().observe(this, list -> {
            if (list == null)
                return;
            adapter.setData(list);
            updateSummary(list);
            updateEmptyState();
        });
    }

    private void setupLegalFilterChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupLegalFilter);
        if (chipGroup == null) {
            return;
        }
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chipFilterAll : checkedIds.get(0);
            ContractListAdapter.LegalFilter filter;
            if (checkedId == R.id.chipFilterActive) {
                filter = ContractListAdapter.LegalFilter.ACTIVE;
            } else if (checkedId == R.id.chipFilterExpiring) {
                filter = ContractListAdapter.LegalFilter.EXPIRING;
            } else if (checkedId == R.id.chipFilterEnded) {
                filter = ContractListAdapter.LegalFilter.ENDED;
            } else {
                filter = ContractListAdapter.LegalFilter.ALL;
            }
            adapter.setLegalFilter(filter);
            updateEmptyState();
        });
    }

    private void updateSummary(List<Tenant> list) {
        int dangThue = 0, sapHet = 0, ketThuc = 0;
        int dueIn7 = 0, dueIn30 = 0, expiredNeedAction = 0;
        for (Tenant c : list) {
            ContractStatus s = ContractStatusHelper.resolve(c);
            if (s == null)
                continue;
            long daysRemaining = ContractStatusHelper.daysRemaining(c);
            switch (s) {
                case ACTIVE_RENTAL:
                    dangThue++;
                    break;
                case EXPIRING_SOON:
                    sapHet++;
                    if (daysRemaining >= 0 && daysRemaining <= 7) {
                        dueIn7++;
                    }
                    if (daysRemaining >= 0 && daysRemaining <= 30) {
                        dueIn30++;
                    }
                    break;
                case ENDED:
                    ketThuc++;
                    expiredNeedAction++;
                    break;
            }
        }

        if (tvCountDangThue != null)
            tvCountDangThue.setText(String.valueOf(dangThue));
        if (tvCountSapHet != null)
            tvCountSapHet.setText(String.valueOf(sapHet));
        if (tvCountKetThuc != null)
            tvCountKetThuc.setText(String.valueOf(ketThuc));
        if (tvDueIn7Days != null)
            tvDueIn7Days.setText(getString(R.string.contract_due_7_days, dueIn7));
        if (tvDueIn30Days != null)
            tvDueIn30Days.setText(getString(R.string.contract_due_30_days, dueIn30));
        if (tvExpiredNeedAction != null)
            tvExpiredNeedAction.setText(getString(R.string.contract_expired_need_action, expiredNeedAction));
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter == null || adapter.getDisplayCount() == 0;
        rvHopDong.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (layoutEmpty != null)
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

}
