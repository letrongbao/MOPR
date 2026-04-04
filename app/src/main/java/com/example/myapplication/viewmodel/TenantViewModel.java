package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.core.repository.domain.TenantRepository;
import java.util.List;

public class TenantViewModel extends ViewModel {

    private final TenantRepository repository = new TenantRepository();
    private LiveData<List<Tenant>> tenantList;

    public LiveData<List<Tenant>> getTenantList() {
        if (tenantList == null) {
            tenantList = repository.getTenantList();
        }
        return tenantList;
    }

    public LiveData<List<Tenant>> getTenantsByRoom(String roomId) {
        return repository.getTenantsByRoom(roomId);
    }

    public void addTenant(Tenant tenant, Runnable onSuccess, Runnable onFail) {
        repository.addTenant(tenant, onSuccess, onFail);
    }

    public void updateTenant(Tenant tenant, Runnable onSuccess, Runnable onFail) {
        repository.updateTenant(tenant, onSuccess, onFail);
    }

    public void deleteTenant(String id, Runnable onSuccess, Runnable onFail) {
        repository.deleteTenant(id, onSuccess, onFail);
    }
}
