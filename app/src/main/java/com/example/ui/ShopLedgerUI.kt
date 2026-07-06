package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R

// Helper list mapping color indices to actual vibrant colors
val NotebookColors = listOf(
    LedgerBlue,   // 0: Ink Blue
    LedgerRed,    // 1: Ledger Red
    LedgerGreen,  // 2: Ledger Green
    LedgerAmber,  // 3: Warm Amber/Warning
    LedgerSlate,  // 4: Slate Grey
    LedgerGold    // 5: Rich Gold
)

val NotebookColorNames = listOf(
    "Ink Blue",
    "Ledger Red",
    "Credit Green",
    "Warm Amber",
    "Slate Grey",
    "Rich Gold"
)

// Custom notebook paper modifier drawing horizontal blue lines and a vertical red margin
fun Modifier.notebookPaper(
    paperColor: Color = PaperSurface,
    lineSpacing: Dp = 26.dp,
    lineColor: Color = NotebookLinedBlue,
    marginColor: Color = NotebookMarginRed,
    marginWidth: Dp = 42.dp
) = this.drawBehind {
    val spacingPx = lineSpacing.toPx()
    val marginPx = marginWidth.toPx()

    // 1. Fill base paper background
    drawRect(color = paperColor)

    // 2. Draw horizontal blue paper lines
    var y = spacingPx + 15f
    while (y < size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.2.dp.toPx()
        )
        y += spacingPx
    }

    // 3. Draw vertical red double-lines for standard accounting/ledger margin
    drawLine(
        color = marginColor,
        start = Offset(marginPx, 0f),
        end = Offset(marginPx, size.height),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = marginColor.copy(alpha = 0.5f),
        start = Offset(marginPx - 4.dp.toPx(), 0f),
        end = Offset(marginPx - 4.dp.toPx(), size.height),
        strokeWidth = 1.dp.toPx()
    )
}

@Composable
fun ShopLedgerApp(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Ledger, 1: Schedule, 2: Stock
    var isYellowPaper by remember { mutableStateOf(true) }
    val paperColor = if (isYellowPaper) Color(0xFFFFFDD0) else Color(0xFFFFFDF9)
    
    // Bottom Safe-Area check and clean container
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp),
                color = BrownPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF8E1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 16.sp)
                        }
                        Text(
                            text = "Dukaan Ledger",
                            color = Color(0xFFFFF8E1), // amber-50
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Paper style toggle button
                        IconButton(
                            onClick = { isYellowPaper = !isYellowPaper },
                            modifier = Modifier.testTag("paper_style_toggle")
                        ) {
                            Icon(
                                imageVector = if (isYellowPaper) Icons.Default.Palette else Icons.Default.Style,
                                contentDescription = "Toggle Paper Color",
                                tint = Color(0xFFFFF8E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { /* Aesthetic / Search */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFFFF8E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant Material You bottom nav with top border and precise 80.dp height
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabFolderItem(
                        title = "Hisab",
                        icon = Icons.Default.MenuBook,
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        tag = "tab_ledger"
                    )
                    TabFolderItem(
                        title = "Schedule",
                        icon = Icons.Default.EventNote,
                        isSelected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        tag = "tab_schedule"
                    )
                    TabFolderItem(
                        title = "Stock",
                        icon = Icons.Default.Inventory,
                        isSelected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        tag = "tab_stock"
                    )
                }
            }
        },
        containerColor = CreamBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> LedgerTabScreen(viewModel = viewModel, paperColor = paperColor)
                1 -> ScheduleTabScreen(viewModel = viewModel, paperColor = paperColor)
                2 -> StockTabScreen(viewModel = viewModel, paperColor = paperColor)
            }
        }
    }
}

// Bottom tab design styled as physical notebook tabs
@Composable
fun RowScope.TabFolderItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val bg = if (isSelected) Color(0xFFFFECB3).copy(alpha = 0.5f) else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF3E2723) else Color(0xFF5D4037).copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// SCREEN 1: LEDGER (HISAB) TAB
// ==========================================
@Composable
fun LedgerTabScreen(
    viewModel: ShopViewModel,
    paperColor: Color
) {
    val customers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val ledgerEntries by viewModel.selectedCustomerEntries.collectAsStateWithLifecycle()
    val summary by viewModel.ledgerSummary.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    // Toggle showing individual page or list on mobile
    var mobileSelectedCustomerPageActive by remember { mutableStateOf(false) }

    // Synchronize selection
    LaunchedEffect(selectedCustomer) {
        if (selectedCustomer == null) {
            mobileSelectedCustomerPageActive = false
        }
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    selectedCustomer?.let { customer ->
                        val csvContent = generateCsvString(customer, ledgerEntries)
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                        Toast.makeText(context, "Ledger exported to CSV successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Adapt layout to wide screens
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // Wide layout: Split side-by-side
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Customer Selector
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .background(CreamBackground)
                        .padding(12.dp)
                ) {
                    CustomerListSection(
                        customers = customers,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.searchCustomers(it) },
                        selectedCustomer = selectedCustomer,
                        onCustomerSelect = { viewModel.selectCustomer(it.id) },
                        onAddCustomerClick = { showAddCustomerDialog = true },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) }
                    )
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = BrownSecondary.copy(alpha = 0.3f)
                )

                // Right Lined Ledger notebook page
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(12.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .notebookPaper(paperColor = paperColor)
                ) {
                    if (selectedCustomer != null) {
                        LedgerSheetSection(
                            customer = selectedCustomer!!,
                            entries = ledgerEntries,
                            summary = summary,
                            onAddEntryClick = { showAddEntryDialog = true },
                            onDeleteEntry = { viewModel.deleteLedgerEntry(it) },
                            showBack = false,
                            onBackClick = {},
                            onExportClick = {
                                val sanitizedName = selectedCustomer?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "customer"
                                exportLauncher.launch("Ledger_$sanitizedName.csv")
                            }
                        )
                    } else {
                        LedgerEmptyState()
                    }
                }
            }
        } else {
            // Mobile view: Toggle list vs notebook page
            if (mobileSelectedCustomerPageActive && selectedCustomer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookPaper(paperColor = paperColor)
                ) {
                    LedgerSheetSection(
                        customer = selectedCustomer!!,
                        entries = ledgerEntries,
                        summary = summary,
                        onAddEntryClick = { showAddEntryDialog = true },
                        onDeleteEntry = { viewModel.deleteLedgerEntry(it) },
                        showBack = true,
                        onBackClick = { mobileSelectedCustomerPageActive = false },
                        onExportClick = {
                            val sanitizedName = selectedCustomer?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "customer"
                            exportLauncher.launch("Ledger_$sanitizedName.csv")
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CreamBackground)
                        .padding(12.dp)
                ) {
                    CustomerListSection(
                        customers = customers,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.searchCustomers(it) },
                        selectedCustomer = selectedCustomer,
                        onCustomerSelect = {
                            viewModel.selectCustomer(it.id)
                            mobileSelectedCustomerPageActive = true
                        },
                        onAddCustomerClick = { showAddCustomerDialog = true },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) }
                    )
                }
            }
        }
    }

    // Modals
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone ->
                viewModel.addCustomer(name, phone)
                showAddCustomerDialog = false
                mobileSelectedCustomerPageActive = true
            }
        )
    }

    if (showAddEntryDialog) {
        AddLedgerEntryDialog(
            onDismiss = { showAddEntryDialog = false },
            onConfirm = { particulars, type, amount, colorIndex, date ->
                viewModel.addLedgerEntry(particulars, type, amount, colorIndex, date)
                showAddEntryDialog = false
            }
        )
    }
}

@Composable
fun CustomerListSection(
    customers: List<Customer>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCustomer: Customer?,
    onCustomerSelect: (Customer) -> Unit,
    onAddCustomerClick: () -> Unit,
    onDeleteCustomer: (Customer) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Shop Accounts (Hisab Khata)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrownPrimary,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_search"),
            placeholder = { Text("Search customer name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrownSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrownPrimary,
                unfocusedBorderColor = BrownSecondary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Add Customer Button
        Button(
            onClick = onAddCustomerClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_customer_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Customer Profile", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Accounts List
        if (customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No accounts found.\nTap 'Add Customer' to create one.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customers) { customer ->
                    val isSelected = selectedCustomer?.id == customer.id
                    val cardBg = if (isSelected) PaperSurface else Color.White
                    val borderStroke = if (isSelected) BorderStroke(2.dp, BrownPrimary) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCustomerSelect(customer) }
                            .testTag("customer_card_${customer.id}"),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = borderStroke,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BrownSecondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = customer.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BrownPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CharcoalText
                                    )
                                    if (customer.phone.isNotBlank()) {
                                        Text(
                                            text = customer.phone,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDeleteCustomer(customer) },
                                modifier = Modifier.testTag("delete_customer_${customer.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Account",
                                    tint = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerSheetSection(
    customer: Customer,
    entries: List<LedgerEntry>,
    summary: LedgerSummaryData,
    onAddEntryClick: () -> Unit,
    onDeleteEntry: (LedgerEntry) -> Unit,
    showBack: Boolean,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 54.dp, top = 16.dp, end = 16.dp, bottom = 16.dp) // Offset left edge to respect red notebook margin!
    ) {
        // Customer Header info with Geometric Balance
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFECB3).copy(alpha = 0.3f)) // bg-amber-100/50 look
                .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBack) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("back_to_accounts")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CharcoalText)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT CUSTOMER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D4C41),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF3E2723),
                            fontFamily = FontFamily.Serif
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export Button
                    OutlinedButton(
                        onClick = onExportClick,
                        border = BorderStroke(1.dp, Color(0xFFD84315)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD84315)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("export_csv_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Add Ledger Entry Button
                    Button(
                        onClick = onAddEntryClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)), // Amber-600 / rich terracotta rust
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("add_ledger_entry_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW HISAB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Balance Summary Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEBIT (OWED)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Text("₹${String.format("%.1f", summary.totalDebit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LedgerRed)
                }
                Column {
                    Text(
                        text = "CREDIT (PAID)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Text("₹${String.format("%.1f", summary.totalCredit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LedgerGreen)
                }

                val balanceColor = if (summary.netBalance >= 0) LedgerRed else LedgerGreen
                val balanceLabel = if (summary.netBalance >= 0) "TOTAL BALANCE" else "ADVANCE CREDIT"
                val displayBalance = Math.abs(summary.netBalance)

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = balanceLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        letterSpacing = 0.5.sp
                    )
                    Text("₹${String.format("%.0f", displayBalance)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = balanceColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ledger Notebook Table Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFECB3).copy(alpha = 0.2f),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DATE", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF5D4037), letterSpacing = 0.5.sp)
                    Text("PARTICULARS", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF5D4037), letterSpacing = 0.5.sp)
                    Text("DEBIT (₹)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.End, color = LedgerRed, letterSpacing = 0.5.sp)
                    Text("CREDIT (₹)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.End, color = LedgerGreen, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.width(36.dp))
                }
                Divider(color = Color(0xFFFFD54F), thickness = 1.dp)
            }
        }

        // Ledger rows list
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records on this ledger page.\nTap 'New Hisab' to record sales or payments.",
                    textAlign = TextAlign.Center,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        } else {
            // Running balance calculation in view
            var runningBal = 0.0
            val runningBalances = entries.map {
                if (it.type == "DEBIT") runningBal += it.amount else runningBal -= it.amount
                runningBal
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(entries.zip(runningBalances)) { (entry, bal) ->
                    val dateStr = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(entry.date))
                    val textAccent = NotebookColors.getOrNull(entry.color) ?: CharcoalText
                    val rowBg = if (entry.type == "CREDIT") Color(0xFFE8F5E9).copy(alpha = 0.5f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(vertical = 5.dp, horizontal = 4.dp)
                            .testTag("ledger_row_${entry.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date column
                        Text(
                            text = dateStr,
                            modifier = Modifier.weight(1.1f),
                            fontSize = 12.sp,
                            color = CharcoalText,
                            fontFamily = FontFamily.Monospace
                        )

                        // Particulars text column styled as handwriting
                        Text(
                            text = entry.particulars,
                            modifier = Modifier.weight(2f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textAccent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Serif
                        )

                        // Debit amount
                        Text(
                            text = if (entry.type == "DEBIT") String.format("%.0f", entry.amount) else "-",
                            modifier = Modifier.weight(1.2f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (entry.type == "DEBIT") LedgerRed else Color.LightGray
                        )

                        // Credit amount
                        Text(
                            text = if (entry.type == "CREDIT") String.format("%.0f", entry.amount) else "-",
                            modifier = Modifier.weight(1.2f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (entry.type == "CREDIT") LedgerGreen else Color.LightGray
                        )

                        // Action delete button
                        IconButton(
                            onClick = { onDeleteEntry(entry) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_entry_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete record",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Divider(color = NotebookLinedBlue.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun LedgerEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = BrownSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Customer Selected",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrownPrimary,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a customer account from the left list,\nor create a new account to view their lined notebook pages.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// ==========================================
// SCREEN 2: DAILY SCHEDULE TAB
// ==========================================
@Composable
fun ScheduleTabScreen(
    viewModel: ShopViewModel,
    paperColor: Color
) {
    val tasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val selectedDateStr by viewModel.selectedDateStr.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val parsedDate = remember(selectedDateStr) {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    // Trigger Android DatePickerDialog
    val showDatePicker = {
        val calendar = Calendar.getInstance().apply { time = parsedDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                viewModel.selectDate(newCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .notebookPaper(paperColor = paperColor)
            .padding(start = 54.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        // Date Header Controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val displayDate = remember(parsedDate) {
                    SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(parsedDate)
                }
                Text(
                    text = "Daily Schedule",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showDatePicker() }
                ) {
                    Text(
                        text = displayDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrownPrimary,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Date",
                        tint = BrownPrimary
                    )
                }
            }

            // Add Task FAB
            Button(
                onClick = { showAddTaskDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("add_task_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fast Date strip selector (Show yesterday, today, tomorrow for seamless tracking)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = Calendar.getInstance()
            val sdfDay = SimpleDateFormat("dd MMM", Locale.getDefault())
            val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Define dates: Today - 1, Today, Today + 1
            for (offset in -1..2) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val dateStr = sdfFull.format(cal.time)
                val label = when (offset) {
                    -1 -> "Yesterday"
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> sdfDay.format(cal.time)
                }
                val isSelected = dateStr == selectedDateStr

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BrownPrimary else Color.White)
                        .border(1.dp, if (isSelected) BrownPrimary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectDate(cal.time) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.Gray
                        )
                        Text(
                            text = SimpleDateFormat("dd", Locale.getDefault()).format(cal.time),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else CharcoalText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule List
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = NotebookLinedBlue,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Tasks for This Date",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "Everything looks clean! Tap 'Add Task' to schedule.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    val colorAccent = NotebookColors.getOrNull(task.color) ?: BrownSecondary

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_item_${task.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    // Custom color bar indicator on the left side of the card
                                    drawRect(
                                        color = colorAccent,
                                        topLeft = Offset.Zero,
                                        size = size.copy(width = 5.dp.toPx())
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Task Checkbox
                                Checkbox(
                                    checked = task.isDone,
                                    onCheckedChange = { viewModel.toggleTaskDone(task) },
                                    modifier = Modifier.testTag("checkbox_task_${task.id}"),
                                    colors = CheckboxDefaults.colors(checkedColor = colorAccent)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = task.description,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isDone) Color.Gray else CharcoalText,
                                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                        fontFamily = FontFamily.Serif
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = task.timeStr,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteTask(task) },
                                modifier = Modifier.testTag("delete_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete task",
                                    tint = Color.Red.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { time, desc, colorIndex ->
                viewModel.addTask(time, desc, colorIndex)
                showAddTaskDialog = false
            }
        )
    }
}

// ==========================================
// SCREEN 3: STOCK NOTES TAB
// ==========================================
@Composable
fun StockTabScreen(
    viewModel: ShopViewModel,
    paperColor: Color
) {
    val stockItems by viewModel.filteredStockItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.stockSearchQuery.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedStockItem.collectAsStateWithLifecycle()
    val itemNotes by viewModel.stockNotes.collectAsStateWithLifecycle()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .notebookPaper(paperColor = paperColor)
            .padding(start = 54.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Stock Inventory Notes",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrownPrimary,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchStock(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stock_search"),
            placeholder = { Text("Search stock items...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrownSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrownPrimary,
                unfocusedBorderColor = BrownSecondary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Add Stock Item Button
        Button(
            onClick = { showAddItemDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_stock_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.AddBox, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Stock Item", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stock List
        if (stockItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No stock items listed.\nTap 'Add Stock Item' to create inventory.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stockItems) { item ->
                    val isOutOfStock = item.quantity <= 0.0
                    val isLowStock = !isOutOfStock && item.quantity <= 5.0
                    val statusColor = when {
                        isOutOfStock -> LedgerRed
                        isLowStock -> LedgerAmber
                        else -> LedgerGreen
                    }
                    val statusLabel = when {
                        isOutOfStock -> "OUT OF STOCK"
                        isLowStock -> "LOW STOCK"
                        else -> "In Stock"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = getStockItemDrawableId(item.name)),
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CharcoalText,
                                        fontFamily = FontFamily.Serif
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$statusLabel: ${String.format("%.1f", item.quantity)} ${item.unit}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            // Quick Adjust Buttons & Action Note Trigger
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Decrease qty
                                IconButton(
                                    onClick = { viewModel.updateStockQuantity(item, -1.0) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(BrownSecondary.copy(alpha = 0.1f), CircleShape)
                                        .testTag("stock_dec_${item.id}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BrownPrimary, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = String.format("%.0f", item.quantity),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = CharcoalText
                                )

                                // Increase qty
                                IconButton(
                                    onClick = { viewModel.updateStockQuantity(item, 1.0) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(BrownSecondary.copy(alpha = 0.1f), CircleShape)
                                        .testTag("stock_inc_${item.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrownPrimary, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Notes button
                                Button(
                                    onClick = {
                                        viewModel.selectStockItem(item.id)
                                        showNotesSheet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrownSecondary),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("stock_notes_${item.id}")
                                ) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Notes", fontSize = 11.sp, color = Color.White)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Delete Button
                                IconButton(
                                    onClick = { viewModel.deleteStockItem(item) },
                                    modifier = Modifier.testTag("delete_stock_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Stock",
                                        tint = Color.Red.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddStockItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, qty, unit ->
                viewModel.addStockItem(name, qty, unit)
                showAddItemDialog = false
            }
        )
    }

    if (showNotesSheet && selectedItem != null) {
        StockNotesDialog(
            item = selectedItem!!,
            notes = itemNotes,
            onDismiss = {
                showNotesSheet = false
                viewModel.selectStockItem(null)
            },
            onAddNote = { text, color ->
                viewModel.addStockNote(text, color)
            },
            onDeleteNote = { note ->
                viewModel.deleteStockNote(note)
            }
        )
    }
}

// ==========================================
// MODAL FORMS & DIALOGS
// ==========================================

// 1. ADD CUSTOMER MODAL
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_customer"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Customer Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrownPrimary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_name"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_phone"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_add_customer")) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name, phone) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                        modifier = Modifier.testTag("btn_confirm_add_customer")
                    ) {
                        Text("Create Account")
                    }
                }
            }
        }
    }
}

// 2. ADD LEDGER ENTRY MODAL
@Composable
fun AddLedgerEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (particulars: String, type: String, amount: Double, colorIndex: Int, date: Long) -> Unit
) {
    var particulars by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DEBIT") } // DEBIT: customer owes, CREDIT: customer paid
    var amountStr by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("dialog_add_entry"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Record Ledger Entry (Hisab)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrownPrimary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Date selection row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrownSecondary.copy(alpha = 0.1f))
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDate = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sdf.format(Date(selectedDate)), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrownPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp), tint = BrownPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle: DEBIT (Give) / CREDIT (Take)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "DEBIT" },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "DEBIT") LedgerRed else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (type == "DEBIT") Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Debit (₹ Owed)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { type = "CREDIT" },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "CREDIT") LedgerGreen else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (type == "CREDIT") Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Credit (₹ Paid)", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_entry_amount"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = particulars,
                    onValueChange = { particulars = it },
                    label = { Text("Particulars (e.g. 2 sacks rice, paid cash)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_entry_particulars"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Color picker
                Text("Select Ink Color:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NotebookColors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) CharcoalText else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                                .testTag("color_picker_$index")
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_add_entry")) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (particulars.isNotBlank() && amt > 0) {
                                onConfirm(particulars, type, amt, selectedColorIndex, selectedDate)
                            }
                        },
                        enabled = particulars.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                        modifier = Modifier.testTag("btn_confirm_add_entry")
                    ) {
                        Text("Add Record")
                    }
                }
            }
        }
    }
}

// 3. ADD TASK MODAL
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (time: String, desc: String, colorIndex: Int) -> Unit
) {
    var timeStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_task"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Daily Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrownPrimary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("Task Time (e.g. 10:30 AM)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_time"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What needs to be done?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_desc"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Tag Color:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NotebookColors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) CharcoalText else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_add_task")) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (description.isNotBlank()) onConfirm(timeStr, description, selectedColorIndex) },
                        enabled = description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                        modifier = Modifier.testTag("btn_confirm_add_task")
                    ) {
                        Text("Add Task")
                    }
                }
            }
        }
    }
}

// 4. ADD STOCK ITEM MODAL
@Composable
fun AddStockItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Double, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }

    val units = listOf("kg", "pcs", "ltr", "bags", "boxes", "tins")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_stock"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Stock Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrownPrimary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Wheat Flour)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_stock_name"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Initial Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_stock_qty"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                    )

                    // Simple select row for units
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Unit:", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var expandedUnitMenu by remember { mutableStateOf(false) }
                            Text(unit, fontWeight = FontWeight.Bold, color = BrownPrimary)
                            IconButton(onClick = { expandedUnitMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expandedUnitMenu,
                                onDismissRequest = { expandedUnitMenu = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            unit = u
                                            expandedUnitMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_add_stock")) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) onConfirm(name, qty, unit)
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                        modifier = Modifier.testTag("btn_confirm_add_stock")
                    ) {
                        Text("Save Item")
                    }
                }
            }
        }
    }
}

// 5. VIEW/ADD NOTES MODAL FOR A STOCK ITEM
@Composable
fun StockNotesDialog(
    item: StockItem,
    notes: List<StockNote>,
    onDismiss: () -> Unit,
    onAddNote: (text: String, color: Int) -> Unit,
    onDeleteNote: (StockNote) -> Unit
) {
    var newNoteText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_stock_notes"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = getStockItemDrawableId(item.name)),
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "${item.name} Notes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrownPrimary,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Current Qty: ${item.quantity} ${item.unit}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable notes list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (notes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No notes written for this stock item.\nWrite a reminder below (e.g. stock level warnings or order details).",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes) { note ->
                                val noteColorAccent = NotebookColors.getOrNull(note.color) ?: BrownSecondary
                                val noteDateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(note.date))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = noteColorAccent.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, noteColorAccent.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = noteDateStr,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = noteColorAccent
                                            )
                                            IconButton(
                                                onClick = { onDeleteNote(note) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Note",
                                                    tint = Color.Red.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.text,
                                            fontSize = 13.sp,
                                            color = CharcoalText,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // Write a new note section
                Text("Add Quick Note:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    placeholder = { Text("Type stock notes here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("input_stock_note_text"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrownPrimary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Note color and post button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small color picker row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotebookColors.take(4).forEachIndexed { idx, color ->
                            val isSel = selectedColorIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSel) 2.dp else 0.dp,
                                        color = if (isSel) CharcoalText else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = idx }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                onAddNote(newNoteText, selectedColorIndex)
                                newNoteText = ""
                            }
                        },
                        enabled = newNoteText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrownPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_save_stock_note")
                    ) {
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun generateCsvString(customer: Customer, entries: List<LedgerEntry>): String {
    val sb = java.lang.StringBuilder()
    sb.append("Customer Name,${escapeCsv(customer.name)}\n")
    if (customer.phone.isNotBlank()) {
        sb.append("Customer Phone,${escapeCsv(customer.phone)}\n")
    }
    sb.append("\n")
    sb.append("Date,Particulars,Type,Amount (INR),Balance (INR)\n")
    
    var runningBalance = 0.0
    entries.forEach { entry ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.date))
        if (entry.type == "DEBIT") {
            runningBalance += entry.amount
        } else {
            runningBalance -= entry.amount
        }
        sb.append("${escapeCsv(dateStr)},${escapeCsv(entry.particulars)},${escapeCsv(entry.type)},${entry.amount},$runningBalance\n")
    }
    return sb.toString()
}

private fun escapeCsv(value: String): String {
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    return value
}

fun getStockItemDrawableId(name: String): Int {
    val nameLower = name.lowercase()
    return when {
        nameLower.contains("rice") -> R.drawable.img_stock_rice_1783257893698
        nameLower.contains("oil") -> R.drawable.img_stock_oil_1783257912208
        nameLower.contains("sugar") -> R.drawable.img_stock_sugar_1783257925569
        nameLower.contains("flour") || nameLower.contains("atta") || nameLower.contains("wheat") -> R.drawable.img_stock_flour_1783257938824
        else -> R.drawable.img_stock_general_1783257956413
    }
}
