package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ShopRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ShopRepository(db)
        viewModelScope.launch {
            repository.populateSampleDataIfEmpty()
            // Set initial selected customer to the first one available
            repository.customers.first().firstOrNull()?.let {
                _selectedCustomerId.value = it.id
            }
        }
    }

    // ==========================================
    // CUSTOMER / LEDGER STATE
    // ==========================================
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    // Filtered customer list
    val filteredCustomers: StateFlow<List<Customer>> = combine(
        repository.customers,
        _customerSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()

    // Current selected customer object
    val selectedCustomer: StateFlow<Customer?> = combine(
        repository.customers,
        _selectedCustomerId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Ledger entries of selected customer
    val selectedCustomerEntries: StateFlow<List<LedgerEntry>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getLedgerEntries(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Calculate prominent summary values for selected customer
    val ledgerSummary: StateFlow<LedgerSummaryData> = selectedCustomerEntries.map { entries ->
        var totalDebit = 0.0
        var totalCredit = 0.0
        entries.forEach {
            if (it.type == "DEBIT") {
                totalDebit += it.amount
            } else {
                totalCredit += it.amount
            }
        }
        val netBalance = totalDebit - totalCredit
        LedgerSummaryData(
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            netBalance = netBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerSummaryData()
    )

    fun selectCustomer(id: Long) {
        _selectedCustomerId.value = id
    }

    fun searchCustomers(query: String) {
        _customerSearchQuery.value = query
    }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val id = repository.addCustomer(name, phone)
            _selectedCustomerId.value = id
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomerId.value == customer.id) {
                // Select another customer if possible
                val remaining = repository.customers.first()
                val next = remaining.firstOrNull { it.id != customer.id }
                _selectedCustomerId.value = next?.id
            }
        }
    }

    fun addLedgerEntry(particulars: String, type: String, amount: Double, color: Int, date: Long) {
        val custId = _selectedCustomerId.value ?: return
        viewModelScope.launch {
            repository.addLedgerEntry(
                customerId = custId,
                particulars = particulars,
                type = type,
                amount = amount,
                color = color,
                date = date
            )
        }
    }

    fun deleteLedgerEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            repository.deleteLedgerEntry(entry)
        }
    }

    // ==========================================
    // DAILY SCHEDULE STATE
    // ==========================================
    private val _selectedDateStr = MutableStateFlow(getCurrentDateStr())
    val selectedDateStr = _selectedDateStr.asStateFlow()

    val dailyTasks: StateFlow<List<DailyTask>> = _selectedDateStr
        .flatMapLatest { date ->
            repository.getTasksForDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDate(date: Date) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDateStr.value = sdf.format(date)
    }

    fun addTask(timeStr: String, description: String, color: Int) {
        viewModelScope.launch {
            repository.addTask(
                dateStr = _selectedDateStr.value,
                timeStr = timeStr,
                description = description,
                color = color
            )
        }
    }

    fun toggleTaskDone(task: DailyTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isDone = !task.isDone))
        }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // ==========================================
    // STOCK NOTES STATE
    // ==========================================
    private val _stockSearchQuery = MutableStateFlow("")
    val stockSearchQuery = _stockSearchQuery.asStateFlow()

    val filteredStockItems: StateFlow<List<StockItem>> = combine(
        repository.stockItems,
        _stockSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedStockItemId = MutableStateFlow<Long?>(null)
    val selectedStockItemId = _selectedStockItemId.asStateFlow()

    val selectedStockItem: StateFlow<StockItem?> = combine(
        repository.stockItems,
        _selectedStockItemId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val stockNotes: StateFlow<List<StockNote>> = _selectedStockItemId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getNotesForStockItem(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addStockItem(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            val id = repository.addStockItem(name, quantity, unit)
            _selectedStockItemId.value = id
        }
    }

    fun updateStockQuantity(item: StockItem, delta: Double) {
        viewModelScope.launch {
            val newQty = (item.quantity + delta).coerceAtLeast(0.0)
            repository.updateStockItem(item.copy(quantity = newQty))
        }
    }

    fun deleteStockItem(item: StockItem) {
        viewModelScope.launch {
            repository.deleteStockItem(item)
            if (_selectedStockItemId.value == item.id) {
                _selectedStockItemId.value = null
            }
        }
    }

    fun selectStockItem(itemId: Long?) {
        _selectedStockItemId.value = itemId
    }

    fun searchStock(query: String) {
        _stockSearchQuery.value = query
    }

    fun addStockNote(text: String, color: Int) {
        val itemId = _selectedStockItemId.value ?: return
        viewModelScope.launch {
            repository.addStockNote(stockItemId = itemId, text = text, color = color)
        }
    }

    fun deleteStockNote(note: StockNote) {
        viewModelScope.launch {
            repository.deleteStockNote(note)
        }
    }

    // Helpers
    private fun getCurrentDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}

data class LedgerSummaryData(
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val netBalance: Double = 0.0
)
