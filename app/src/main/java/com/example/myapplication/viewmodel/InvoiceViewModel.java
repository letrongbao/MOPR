package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.core.repository.domain.InvoiceRepository;
import java.util.List;

public class InvoiceViewModel extends ViewModel {

    private final InvoiceRepository repository = new InvoiceRepository();
    private LiveData<List<Invoice>> danhSachInvoice;

    public LiveData<List<Invoice>> getInvoiceList() {
        if (danhSachInvoice == null) {
            danhSachInvoice = repository.getInvoiceList();
        }
        return danhSachInvoice;
    }

    public LiveData<List<Invoice>> getInvoicesByRoom(String idPhong) {
        return repository.getInvoicesByRoom(idPhong);
    }

    public void addInvoice(Invoice hoaDon, Runnable onSuccess, Runnable onFail) {
        repository.addInvoice(hoaDon, onSuccess, onFail);
    }

    public void addInvoiceUnique(Invoice hoaDon, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        repository.addInvoiceUnique(hoaDon, onSuccess, onDuplicate, onFail);
    }

    public void updateInvoice(Invoice hoaDon, Runnable onSuccess, Runnable onFail) {
        repository.updateInvoice(hoaDon, onSuccess, onFail);
    }

    public void updateStatus(String id, String trangThai, Runnable onSuccess, Runnable onFail) {
        repository.updateStatus(id, trangThai, onSuccess, onFail);
    }

    public void deleteInvoice(String id, Runnable onSuccess, Runnable onFail) {
        repository.deleteInvoice(id, onSuccess, onFail);
    }
}


