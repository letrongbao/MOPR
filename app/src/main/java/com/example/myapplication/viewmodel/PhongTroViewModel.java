package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.core.repository.domain.PhongTroRepository;
import java.util.List;

public class PhongTroViewModel extends ViewModel {

    private final PhongTroRepository repository = new PhongTroRepository();
    private LiveData<List<PhongTro>> danhSachPhong;

    public LiveData<List<PhongTro>> getDanhSachPhong() {
        if (danhSachPhong == null) {
            danhSachPhong = repository.layDanhSachPhong();
        }
        return danhSachPhong;
    }

    public void themPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        repository.themPhong(phong, onSuccess, onFail);
    }

    public void capNhatPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        repository.capNhatPhong(phong, onSuccess, onFail);
    }

    public void xoaPhong(String id, Runnable onSuccess, Runnable onFail) {
        repository.xoaPhong(id, onSuccess, onFail);
    }
}
