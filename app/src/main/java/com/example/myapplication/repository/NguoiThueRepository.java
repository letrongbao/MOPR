package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.model.NguoiThue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class NguoiThueRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "nguoi_thue";

    private CollectionReference getUserCollection() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return db.collection("users").document(uid).collection(COLLECTION);
    }

    public MutableLiveData<List<NguoiThue>> layDanhSachNguoiThue() {
        MutableLiveData<List<NguoiThue>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<NguoiThue> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                NguoiThue n = doc.toObject(NguoiThue.class);
                n.setId(doc.getId());
                list.add(n);
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<NguoiThue>> layNguoiThueTheoPhong(String idPhong) {
        MutableLiveData<List<NguoiThue>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("idPhong", idPhong)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<NguoiThue> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        NguoiThue n = doc.toObject(NguoiThue.class);
                        n.setId(doc.getId());
                        list.add(n);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void themNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        getUserCollection().add(nguoiThue)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(nguoiThue.getId()).set(nguoiThue)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaNguoiThue(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
