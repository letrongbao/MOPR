package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.core.repository.domain.TenantRepository;
import java.util.List;

public class TenantViewModel extends ViewModel {

    private final TenantRepository repository = new TenantRepository();
    private LiveData<List<Tenant>> danhSachTenant;

    public LiveData<List<Tenant>> getTenantList() {
        if (danhSachTenant == null) {
            danhSachTenant = repository.getTenantList();
        }
        return danhSachTenant;
    }

    public LiveData<List<Tenant>> getTenantTheoPhong(String idPhong) {
        return repository.layTenantTheoPhong(idPhong);
    }

    public void addTenant(Tenant nguoiThue, Runnable onSuccess, Runnable onFail) {
        repository.addTenant(nguoiThue, onSuccess, onFail);
    }

    public void updateTenant(Tenant nguoiThue, Runnable onSuccess, Runnable onFail) {
        repository.updateTenant(nguoiThue, onSuccess, onFail);
    }

    public void deleteTenant(String id, Runnable onSuccess, Runnable onFail) {
        repository.deleteTenant(id, onSuccess, onFail);
    }
}


