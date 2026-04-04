package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.Expense;
import com.example.myapplication.core.repository.domain.ExpenseRepository;

import java.util.List;

public class ExpenseViewModel extends ViewModel {

    private final ExpenseRepository repository = new ExpenseRepository();
    private LiveData<List<Expense>> dataList;

    public LiveData<List<Expense>> getExpenseList() {
        if (dataList == null) {
            dataList = repository.listAll();
        }
        return dataList;
    }

    public void addExpense(Expense cp, Runnable onSuccess, Runnable onFail) {
        repository.add(cp, onSuccess, onFail);
    }

    public void updateExpense(Expense cp, Runnable onSuccess, Runnable onFail) {
        repository.update(cp, onSuccess, onFail);
    }

    public void deleteExpense(String id, Runnable onSuccess, Runnable onFail) {
        repository.delete(id, onSuccess, onFail);
    }
}


