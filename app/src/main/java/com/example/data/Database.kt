package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// ==========================================
// 1. ROOM ENTITIES (Models)
// ==========================================

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: Long, // timestamp
    val particulars: String,
    val type: String, // "DEBIT" or "CREDIT"
    val amount: Double,
    val color: Int // Selected color choice index or ARGB value
)

@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String, // "yyyy-MM-dd"
    val timeStr: String, // e.g., "10:30 AM"
    val description: String,
    val color: Int, // Color picker index
    val isDone: Boolean = false
)

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String // e.g., "kg", "pcs", "ltr"
)

@Entity(
    tableName = "stock_notes",
    foreignKeys = [
        ForeignKey(
            entity = StockItem::class,
            parentColumns = ["id"],
            childColumns = ["stockItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stockItemId"])]
)
data class StockNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockItemId: Long,
    val date: Long, // timestamp
    val text: String,
    val color: Int // Color picker index
)

// ==========================================
// 2. DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE customerId = :customerId ORDER BY date ASC, id ASC")
    fun getEntriesForCustomer(customerId: Long): Flow<List<LedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM daily_tasks WHERE dateStr = :dateStr ORDER BY timeStr ASC")
    fun getTasksForDate(dateStr: String): Flow<List<DailyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Delete
    suspend fun deleteTask(task: DailyTask)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun getAllStockItems(): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItem): Long

    @Update
    suspend fun updateStockItem(item: StockItem)

    @Delete
    suspend fun deleteStockItem(item: StockItem)

    @Query("SELECT * FROM stock_notes WHERE stockItemId = :itemId ORDER BY date DESC")
    fun getNotesForStockItem(itemId: Long): Flow<List<StockNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockNote(note: StockNote)

    @Delete
    suspend fun deleteStockNote(note: StockNote)
}

// ==========================================
// 3. DATABASE HOLDER
// ==========================================

@Database(
    entities = [
        Customer::class,
        LedgerEntry::class,
        DailyTask::class,
        StockItem::class,
        StockNote::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun taskDao(): TaskDao
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_ledger_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. REPOSITORY LAYER
// ==========================================

class ShopRepository(private val db: AppDatabase) {
    val customers: Flow<List<Customer>> = db.customerDao().getAllCustomers()
    val stockItems: Flow<List<StockItem>> = db.stockDao().getAllStockItems()

    fun getLedgerEntries(customerId: Long): Flow<List<LedgerEntry>> =
        db.ledgerDao().getEntriesForCustomer(customerId)

    suspend fun addCustomer(name: String, phone: String = ""): Long {
        return db.customerDao().insertCustomer(Customer(name = name, phone = phone))
    }

    suspend fun deleteCustomer(customer: Customer) {
        db.customerDao().deleteCustomer(customer)
    }

    suspend fun addLedgerEntry(
        customerId: Long,
        particulars: String,
        type: String,
        amount: Double,
        color: Int,
        date: Long
    ) {
        db.ledgerDao().insertEntry(
            LedgerEntry(
                customerId = customerId,
                particulars = particulars,
                type = type,
                amount = amount,
                color = color,
                date = date
            )
        )
    }

    suspend fun deleteLedgerEntry(entry: LedgerEntry) {
        db.ledgerDao().deleteEntry(entry)
    }

    fun getTasksForDate(dateStr: String): Flow<List<DailyTask>> =
        db.taskDao().getTasksForDate(dateStr)

    suspend fun addTask(dateStr: String, timeStr: String, description: String, color: Int) {
        db.taskDao().insertTask(
            DailyTask(
                dateStr = dateStr,
                timeStr = timeStr,
                description = description,
                color = color
            )
        )
    }

    suspend fun updateTask(task: DailyTask) {
        db.taskDao().updateTask(task)
    }

    suspend fun deleteTask(task: DailyTask) {
        db.taskDao().deleteTask(task)
    }

    suspend fun addStockItem(name: String, quantity: Double, unit: String): Long {
        return db.stockDao().insertStockItem(StockItem(name = name, quantity = quantity, unit = unit))
    }

    suspend fun updateStockItem(item: StockItem) {
        db.stockDao().updateStockItem(item)
    }

    suspend fun deleteStockItem(item: StockItem) {
        db.stockDao().deleteStockItem(item)
    }

    fun getNotesForStockItem(itemId: Long): Flow<List<StockNote>> =
        db.stockDao().getNotesForStockItem(itemId)

    suspend fun addStockNote(stockItemId: Long, text: String, color: Int, date: Long = System.currentTimeMillis()) {
        db.stockDao().insertStockNote(
            StockNote(
                stockItemId = stockItemId,
                text = text,
                color = color,
                date = date
            )
        )
    }

    suspend fun deleteStockNote(note: StockNote) {
        db.stockDao().deleteStockNote(note)
    }

    // Populate database with mock/sample data if it's currently empty
    suspend fun populateSampleDataIfEmpty() {
        val currentCustomers = db.customerDao().getAllCustomers().first()
        if (currentCustomers.isEmpty()) {
            // Add sample customer "Rajesh Kumar"
            val customerId = db.customerDao().insertCustomer(
                Customer(name = "Rajesh Kumar", phone = "+91 98765 43210")
            )
            
            val today = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            
            // Add sample entries for Rajesh Kumar
            db.ledgerDao().insertEntry(
                LedgerEntry(
                    customerId = customerId,
                    date = today - 3 * dayMillis,
                    particulars = "Opening Balance",
                    type = "DEBIT",
                    amount = 1500.0,
                    color = 0 // Amber/Golden index
                )
            )
            db.ledgerDao().insertEntry(
                LedgerEntry(
                    customerId = customerId,
                    date = today - 2 * dayMillis,
                    particulars = "Received Cash Payment",
                    type = "CREDIT",
                    amount = 1000.0,
                    color = 1 // Green index
                )
            )
            db.ledgerDao().insertEntry(
                LedgerEntry(
                    customerId = customerId,
                    date = today - dayMillis,
                    particulars = "Sacks of Basmati Rice (2 bags)",
                    type = "DEBIT",
                    amount = 850.0,
                    color = 2 // Blue index
                )
            )

            // Add sample tasks for today
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            db.taskDao().insertTask(
                DailyTask(
                    dateStr = todayDateStr,
                    timeStr = "08:00 AM",
                    description = "Open shop & check inventory",
                    color = 0,
                    isDone = true
                )
            )
            db.taskDao().insertTask(
                DailyTask(
                    dateStr = todayDateStr,
                    timeStr = "11:30 AM",
                    description = "Call Rajesh Kumar for pending credit payment",
                    color = 3, // Red/Accent index
                    isDone = false
                )
            )
            db.taskDao().insertTask(
                DailyTask(
                    dateStr = todayDateStr,
                    timeStr = "04:00 PM",
                    description = "Receive delivery of basmati rice from distributor",
                    color = 2,
                    isDone = false
                )
            )

            // Add sample stock items
            val riceId = db.stockDao().insertStockItem(
                StockItem(name = "Basmati Rice (Premium)", quantity = 45.0, unit = "kg")
            )
            val oilId = db.stockDao().insertStockItem(
                StockItem(name = "Mustard Oil (Kachi Ghani)", quantity = 0.0, unit = "ltr")
            )
            val sugarId = db.stockDao().insertStockItem(
                StockItem(name = "Refined Sugar", quantity = 110.0, unit = "kg")
            )
            val flourId = db.stockDao().insertStockItem(
                StockItem(name = "Wheat Flour (Atta)", quantity = 4.0, unit = "kg")
            )

            // Add notes for Out of Stock items
            db.stockDao().insertStockNote(
                StockNote(
                    stockItemId = oilId,
                    date = today - dayMillis,
                    text = "Completely out of stock! Placed reorder of 15 tins with distributor",
                    color = 3 // red/amber warning
                )
            )
            db.stockDao().insertStockNote(
                StockNote(
                    stockItemId = flourId,
                    date = today,
                    text = "Stock is very low. Remind wholesale manager to send 10 bags on Monday",
                    color = 0 // warning color
                )
            )
        }
    }
}
