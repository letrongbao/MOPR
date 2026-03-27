package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.ChiPhi;
import com.example.myapplication.core.repository.domain.ChiPhiRepository;

import java.util.List;

public class ChiPhiViewModel extends ViewModel {

    private final ChiPhiRepository repository = new ChiPhiRepository();
    private LiveData<List<ChiPhi>> danhSach;

    public LiveData<List<ChiPhi>> getDanhSach() {
        if (danhSach == null) {
            danhSach = repository.listAll();
        }
        return danhSach;
    }

    public void them(ChiPhi cp, Runnable onSuccess, Runnable onFail) {
        repository.add(cp, onSuccess, onFail);
    }

    public void capNhat(ChiPhi cp, Runnable onSuccess, Runnable onFail) {
        repository.update(cp, onSuccess, onFail);
    }

    public void xoa(String id, Runnable onSuccess, Runnable onFail) {
        repository.delete(id, onSuccess, onFail);
    }
}
