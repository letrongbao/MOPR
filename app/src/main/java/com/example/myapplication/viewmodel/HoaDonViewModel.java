package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.model.HoaDon;
import com.example.myapplication.repository.HoaDonRepository;
import java.util.List;

public class HoaDonViewModel extends ViewModel {

    private final HoaDonRepository repository = new HoaDonRepository();
    private LiveData<List<HoaDon>> danhSachHoaDon;

    public LiveData<List<HoaDon>> getDanhSachHoaDon() {
        if (danhSachHoaDon == null) {
            danhSachHoaDon = repository.layDanhSachHoaDon();
        }
        return danhSachHoaDon;
    }

    public LiveData<List<HoaDon>> getHoaDonTheoPhong(String idPhong) {
        return repository.layHoaDonTheoPhong(idPhong);
    }

    public void themHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        repository.themHoaDon(hoaDon, onSuccess, onFail);
    }

    public void themHoaDonUnique(HoaDon hoaDon, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        repository.themHoaDonUnique(hoaDon, onSuccess, onDuplicate, onFail);
    }

    public void capNhatHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        repository.capNhatHoaDon(hoaDon, onSuccess, onFail);
    }

    public void capNhatTrangThai(String id, String trangThai, Runnable onSuccess, Runnable onFail) {
        repository.capNhatTrangThai(id, trangThai, onSuccess, onFail);
    }

    public void xoaHoaDon(String id, Runnable onSuccess, Runnable onFail) {
        repository.xoaHoaDon(id, onSuccess, onFail);
    }
}
