package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.model.PhongTro;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class PhongTroRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "phong_tro";

    public MutableLiveData<List<PhongTro>> layDanhSachPhong() {
        MutableLiveData<List<PhongTro>> data = new MutableLiveData<>();
        db.collection(COLLECTION).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<PhongTro> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                PhongTro p = doc.toObject(PhongTro.class);
                p.setId(doc.getId());
                list.add(p);
            });
            data.setValue(list);
        });
        return data;
    }

    public void themPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        db.collection(COLLECTION).add(phong)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        db.collection(COLLECTION).document(phong.getId()).set(phong)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaPhong(String id, Runnable onSuccess, Runnable onFail) {
        db.collection(COLLECTION).document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
