package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.model.HoaDon;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HoaDonRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "hoa_don";

    public MutableLiveData<List<HoaDon>> layDanhSachHoaDon() {
        MutableLiveData<List<HoaDon>> data = new MutableLiveData<>();
        db.collection(COLLECTION).addSnapshotListener((snapshot, e) -> {
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
        db.collection(COLLECTION).whereEqualTo("idPhong", idPhong)
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
        db.collection(COLLECTION).add(hoaDon)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        hoaDon.tinhTongTien();
        db.collection(COLLECTION).document(hoaDon.getId()).set(hoaDon)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatTrangThai(String id, String trangThai, Runnable onSuccess, Runnable onFail) {
        db.collection(COLLECTION).document(id)
                .update("trangThai", trangThai)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaHoaDon(String id, Runnable onSuccess, Runnable onFail) {
        db.collection(COLLECTION).document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
