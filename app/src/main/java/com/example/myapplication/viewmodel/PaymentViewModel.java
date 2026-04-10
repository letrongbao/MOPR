package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.domain.Payment;

import java.util.List;

public class PaymentViewModel extends ViewModel {
    private final PaymentRepository repository;
    private LiveData<List<Payment>> paymentList;

    public PaymentViewModel() {
        repository = new PaymentRepository();
    }

    public LiveData<List<Payment>> getPaymentList() {
        if (paymentList == null) {
            paymentList = repository.getPaymentList();
        }
        return paymentList;
    }

    public LiveData<List<Payment>> listByInvoice(String invoiceId) {
        return repository.listByInvoice(invoiceId);
    }
    
    public LiveData<List<Payment>> listByRoom(String roomId) {
        return repository.listByRoom(roomId);
    }

    public void addPayment(Payment payment, Runnable onSuccess, Runnable onFail) {
        repository.add(payment, onSuccess, onFail);
    }

    public void deletePayment(String id, Runnable onSuccess, Runnable onFail) {
        repository.delete(id, onSuccess, onFail);
    }
}
