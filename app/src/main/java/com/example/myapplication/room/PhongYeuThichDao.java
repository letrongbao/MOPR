package com.example.myapplication.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PhongYeuThichDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PhongYeuThich phong);

    @Query("SELECT * FROM phong_yeu_thich ORDER BY ngayLuu DESC")
    LiveData<List<PhongYeuThich>> getAll();

    @Query("SELECT EXISTS(SELECT 1 FROM phong_yeu_thich WHERE id = :phongId)")
    LiveData<Boolean> isFavorite(String phongId);

    @Query("DELETE FROM phong_yeu_thich WHERE id = :phongId")
    void deleteById(String phongId);
}
