package com.example.myapplication.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteRoom room);

    @Query("SELECT * FROM favorite_rooms ORDER BY savedAt DESC")
    LiveData<List<FavoriteRoom>> getAll();

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_rooms WHERE id = :roomId)")
    LiveData<Boolean> isFavorite(String roomId);

    @Query("DELETE FROM favorite_rooms WHERE id = :roomId")
    void deleteById(String roomId);
}
