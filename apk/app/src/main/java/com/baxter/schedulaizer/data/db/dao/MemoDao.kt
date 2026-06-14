package com.baxter.schedulaizer.data.db.dao

import androidx.room.*
import com.baxter.schedulaizer.data.db.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY createdMs DESC")
    fun getAllMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE parentId = :parentId AND parentType = :parentType ORDER BY createdMs DESC")
    fun getMemosForParent(parentId: Long, parentType: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE parentId = 0 ORDER BY createdMs DESC")
    fun getStandaloneMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemoById(id: Long): MemoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE parentId = :parentId AND parentType = :parentType")
    suspend fun deleteMemosForParent(parentId: Long, parentType: String)
}
