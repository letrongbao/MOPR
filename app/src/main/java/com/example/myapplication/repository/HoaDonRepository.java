package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.model.HoaDon;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HoaDonRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "hoa_don";

    private CollectionReference getUserCollection() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return db.collection("users").document(uid).collection(COLLECTION);
    }

    public MutableLiveData<List<HoaDon>> layDanhSachHoaDon() {
        MutableLiveData<List<HoaDon>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<HoaDon> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                HoaDon h = doc.toObject(HoaDon.class);
                h.setId(doc.getId());
                list.add(h);
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<HoaDon>> layHoaDonTheoPhong(String idPhong) {
        MutableLiveData<List<HoaDon>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("idPhong", idPhong)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<HoaDon> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        HoaDon h = doc.toObject(HoaDon.class);
                        h.setId(doc.getId());
                        list.add(h);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void themHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        hoaDon.tinhTongTien();
        getUserCollection().add(hoaDon)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        hoaDon.tinhTongTien();
        getUserCollection().document(hoaDon.getId()).set(hoaDon)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatTrangThai(String id, String trangThai, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id)
                .update("trangThai", trangThai)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaHoaDon(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
