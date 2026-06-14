package com.baxter.schedulaizer.data.repository

import com.baxter.schedulaizer.data.db.dao.BillDao
import com.baxter.schedulaizer.data.db.dao.AlertDao
import com.baxter.schedulaizer.data.db.entity.BillEntity
import com.baxter.schedulaizer.util.DateUtils
import kotlinx.coroutines.flow.Flow

class BillRepository(
    private val billDao: BillDao,
    private val alertDao: AlertDao
) {
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val unpaidBills: Flow<List<BillEntity>> = billDao.getUnpaidBills()

    fun getBillsDueInRange(fromMs: Long, toMs: Long): Flow<List<BillEntity>> = billDao.getBillsDueInRange(fromMs, toMs)

    suspend fun save(bill: BillEntity): Long {
        if (bill.name.isBlank()) throw IllegalArgumentException("Bill name cannot be blank")
        if (bill.dueDayOfMonth !in 1..31) throw IllegalArgumentException("dueDayOfMonth must be 1..31")
        if (bill.intervalMonths < 1) throw IllegalArgumentException("intervalMonths must be >= 1")
        if (bill.amountCents < 0) throw IllegalArgumentException("amountCents must be >= 0")

        val nextDue = DateUtils.nextDueDateMs(bill.dueDayOfMonth, bill.intervalMonths)
        val now = DateUtils.nowMs()
        return if (bill.id == 0L) {
            val toInsert = bill.copy(nextDueMs = nextDue, createdMs = now, updatedMs = now)
            billDao.insertBill(toInsert)
        } else {
            val toUpdate = bill.copy(nextDueMs = nextDue, updatedMs = now)
            billDao.updateBill(toUpdate)
            toUpdate.id
        }
    }

    suspend fun markPaid(billId: Long) {
        billDao.markPaid(billId, DateUtils.nowMs())
        val bill = billDao.getBillById(billId) ?: return
        val advancedDue = DateUtils.nextDueDateMs(bill.dueDayOfMonth, bill.intervalMonths)
        billDao.updateBill(bill.copy(isPaid = false, nextDueMs = advancedDue, updatedMs = DateUtils.nowMs()))
    }

    suspend fun delete(bill: BillEntity) {
        billDao.deleteBill(bill)
        alertDao.deleteAlertsForParent(bill.id, "bill")
    }

    suspend fun getById(id: Long): BillEntity? = billDao.getBillById(id)

    suspend fun totalUnpaidCentsUpTo(endMs: Long): Long = billDao.sumUnpaidAmountCentsUpTo(endMs) ?: 0L
}
