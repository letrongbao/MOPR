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

    public LiveData<List<Invoice>> getInvoicesByRoom(String roomId) {
        return repository.getInvoicesByRoom(roomId);
    }

    public void addInvoice(Invoice invoice, Runnable onSuccess, Runnable onFail) {
        repository.addInvoice(invoice, onSuccess, onFail);
    }

    public void addInvoiceUnique(Invoice invoice, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        repository.addInvoiceUnique(invoice, onSuccess, onDuplicate, onFail);
    }

    public void updateInvoice(Invoice invoice, Runnable onSuccess, Runnable onFail) {
        repository.updateInvoice(invoice, onSuccess, onFail);
    }

    public void updateStatus(String id, String status, Runnable onSuccess, Runnable onFail) {
        repository.updateStatus(id, status, onSuccess, onFail);
    }

    public void deleteInvoice(String id, Runnable onSuccess, Runnable onFail) {
        repository.deleteInvoice(id, onSuccess, onFail);
    }
}


