package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.Room;
import com.example.myapplication.core.repository.domain.RoomRepository;
import java.util.List;

public class RoomViewModel extends ViewModel {

    private final RoomRepository repository = new RoomRepository();
    private LiveData<List<Room>> danhSachPhong;

    public LiveData<List<Room>> getRoomList() {
        if (danhSachPhong == null) {
            danhSachPhong = repository.getRoomList();
        }
        return danhSachPhong;
    }

    public void addRoom(Room phong, Runnable onSuccess, Runnable onFail) {
        repository.addRoom(phong, onSuccess, onFail);
    }

    public void updateRoom(Room phong, Runnable onSuccess, Runnable onFail) {
        repository.updateRoom(phong, onSuccess, onFail);
    }

    public void deleteRoom(String id, Runnable onSuccess, Runnable onFail) {
        repository.deleteRoom(id, onSuccess, onFail);
    }
}


