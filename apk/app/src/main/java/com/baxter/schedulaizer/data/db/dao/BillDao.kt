package com.baxter.schedulaizer.data.db.dao

import androidx.room.*
import com.baxter.schedulaizer.data.db.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY nextDueMs ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY nextDueMs ASC")
    fun getUnpaidBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE nextDueMs >= :fromMs AND nextDueMs <= :toMs ORDER BY nextDueMs ASC")
    fun getBillsDueInRange(fromMs: Long, toMs: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("UPDATE bills SET isPaid = 1, updatedMs = :nowMs WHERE id = :id")
    suspend fun markPaid(id: Long, nowMs: Long)

    @Query("SELECT SUM(amountCents) FROM bills WHERE isPaid = 0 AND nextDueMs <= :endMs")
    suspend fun sumUnpaidAmountCentsUpTo(endMs: Long): Long?
}
