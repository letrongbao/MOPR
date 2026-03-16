package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.model.NguoiThue;
import com.example.myapplication.repository.NguoiThueRepository;
import java.util.List;

public class NguoiThueViewModel extends ViewModel {

    private final NguoiThueRepository repository = new NguoiThueRepository();
    private LiveData<List<NguoiThue>> danhSachNguoiThue;

    public LiveData<List<NguoiThue>> getDanhSachNguoiThue() {
        if (danhSachNguoiThue == null) {
            danhSachNguoiThue = repository.layDanhSachNguoiThue();
        }
        return danhSachNguoiThue;
    }

    public LiveData<List<NguoiThue>> getNguoiThueTheoPhong(String idPhong) {
        return repository.layNguoiThueTheoPhong(idPhong);
    }

    public void themNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        repository.themNguoiThue(nguoiThue, onSuccess, onFail);
    }

    public void capNhatNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        repository.capNhatNguoiThue(nguoiThue, onSuccess, onFail);
    }

    public void xoaNguoiThue(String id, Runnable onSuccess, Runnable onFail) {
        repository.xoaNguoiThue(id, onSuccess, onFail);
    }
}
