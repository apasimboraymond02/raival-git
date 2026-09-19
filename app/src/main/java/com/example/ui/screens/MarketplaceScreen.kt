package com.example.ui.screens

import com.example.BuildConfig

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.annotation.SuppressLint
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@SuppressLint("DefaultLocale")

enum class MarketplaceView {
    BROWSE,
    CREATE_LISTING,
    LISTING_DETAIL,
    CHAT,
    PAYMENT,
    TRANSACTION_ESCROW,
    DISPUTE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: RaivalViewModel,
    currentUser: User,
    onUserClick: (User) -> Unit = {}
) {
    val activeListings by viewModel.activeListings.collectAsState()
    val allListings by viewModel.allListings.collectAsState()
    val userTransactions by viewModel.userMarketplaceTransactions.collectAsState()
    val allTransactions by viewModel.allMarketplaceTransactions.collectAsState()

    var currentView by remember { mutableStateOf(MarketplaceView.BROWSE) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var activeTransaction by remember { mutableStateOf<MarketplaceTransaction?>(null) }
    
    // Chat parameters
    var activeChatRoomId by remember { mutableStateOf<String?>(null) }
    var activeChatPartnerName by remember { mutableStateOf("") }
    var activeChatListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var activeChatBuyerId by remember { mutableStateOf<Int?>(null) }

// Navigation and tabs inside the main Browse screen
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Browse, 1: My Listings, 2: Escrow, 3: Admin (if admin)

    val coroutineScope = rememberCoroutineScope()

    // Clean back handling
    val handleBack = {
        when (currentView) {
            MarketplaceView.BROWSE -> {}
            MarketplaceView.CREATE_LISTING -> currentView = MarketplaceView.BROWSE
            MarketplaceView.LISTING_DETAIL -> currentView = MarketplaceView.BROWSE
            MarketplaceView.CHAT -> {
                // If we came from details, go back to details
                if (selectedListing != null) {
                    currentView = MarketplaceView.LISTING_DETAIL
                } else {
                    currentView = MarketplaceView.BROWSE
                }
            }
            MarketplaceView.PAYMENT -> currentView = MarketplaceView.LISTING_DETAIL
            MarketplaceView.TRANSACTION_ESCROW -> currentView = MarketplaceView.BROWSE
            MarketplaceView.DISPUTE -> {
                if (activeTransaction != null) {
                    currentView = MarketplaceView.TRANSACTION_ESCROW
                } else {
                    currentView = MarketplaceView.BROWSE
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (BuildConfig.IS_DEMO_MODE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFA500).copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Demo", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "⚠️ DEMO MODE: Financial transactions (deposits, withdrawals, purchases) are disabled. All data is simulated.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "marketplace_navigation"
        ) { targetView ->
            when (targetView) {
                MarketplaceView.BROWSE -> {
                    BrowseListingsScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        activeListings = activeListings,
                        allListings = allListings,
                        userTransactions = userTransactions,
                        allTransactions = allTransactions,
                        activeSubTab = activeSubTab,
                        onSubTabChange = { activeSubTab = it },
                        onSelectListing = { listing ->
                            selectedListing = listing
                            currentView = MarketplaceView.LISTING_DETAIL
                        },
                        onContactSeller = { listing ->
                            val chatRoomId = "marketplace_${listing.id}_buyer_${currentUser.id}"
                            activeChatRoomId = chatRoomId
                            activeChatPartnerName = listing.sellerName
                            activeChatListing = listing
                            activeChatBuyerId = currentUser.id
                            selectedListing = listing
                            currentView = MarketplaceView.CHAT
                        },
                        onSelectTransaction = { tx ->
                            activeTransaction = tx
                            // fetch listing context for transaction
                            coroutineScope.launch {
                                val listing = viewModel.getListingById(tx.listingId).firstOrNull()
                                selectedListing = listing
                                currentView = MarketplaceView.TRANSACTION_ESCROW
                            }
                        },
                        onCreateListingClick = {
                            currentView = MarketplaceView.CREATE_LISTING
                        },
                        onUserClick = onUserClick
                    )
                }

                MarketplaceView.CREATE_LISTING -> {
                    CreateListingScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onBack = handleBack,
                        onListingCreated = {
                            currentView = MarketplaceView.BROWSE
                            activeSubTab = 1 // Open My Listings tab
                        }
                    )
                }

                MarketplaceView.LISTING_DETAIL -> {
                    ListingDetailScreen(
                        listing = selectedListing!!,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onBack = handleBack,
                        onUserClick = onUserClick,
                        onContactSeller = {
                            val chatRoomId = "marketplace_${selectedListing?.id ?: 0}_buyer_${currentUser.id}"
                            activeChatRoomId = chatRoomId
                            activeChatPartnerName = selectedListing?.sellerName ?: ""
                            activeChatListing = selectedListing
                            activeChatBuyerId = currentUser.id
                            currentView = MarketplaceView.CHAT
                        },
                        onMakeOffer = { offerAmount ->
                            viewModel.makeMarketplaceOffer(
                                MarketplaceOffer(
                                    listingId = selectedListing?.id ?: 0,
                                    buyerId = currentUser.id,
                                    buyerName = currentUser.username,
                                    offerAmount = offerAmount,
                                    status = "Pending"
                                )
                            ) {
                                coroutineScope.launch {
                                    val sysRoomId = "marketplace_${selectedListing?.id ?: 0}_buyer_${currentUser.id}"
                                    viewModel.repository.sendChatMessage(
                                        sysRoomId,
                                        "🤖 OFFER: Buyer made an offer of GHS ${String.format(Locale.getDefault(), "%.2f", offerAmount)} for this team."
                                    )
                                }
                            }
                        },
                        onOpenChat = { buyerId, buyerName ->
                            val chatRoomId = "marketplace_${selectedListing?.id ?: 0}_buyer_${buyerId}"
                            activeChatRoomId = chatRoomId
                            activeChatPartnerName = buyerName
                            activeChatListing = selectedListing
                            activeChatBuyerId = buyerId
                            currentView = MarketplaceView.CHAT
                        },
                        onAcceptOffer = { offer ->
                            viewModel.updateMarketplaceOffer(offer.copy(status = "Accepted")) {
                                coroutineScope.launch {
                                    val sysRoomId = "marketplace_${selectedListing?.id ?: 0}_buyer_${offer.buyerId}"
                                    viewModel.repository.sendChatMessage(
                                        sysRoomId,
                                        "🤖 OFFER ACCEPTED: Seller accepted the offer of GHS ${String.format(Locale.getDefault(), "%.2f", offer.offerAmount)}! Buyer can now proceed to payment."
                                    )
                                }
                            }
                        },
                        onDeclineOffer = { offer ->
                            viewModel.updateMarketplaceOffer(offer.copy(status = "Declined")) {
                                coroutineScope.launch {
                                    val sysRoomId = "marketplace_${selectedListing?.id ?: 0}_buyer_${offer.buyerId}"
                                    viewModel.repository.sendChatMessage(
                                        sysRoomId,
                                        "🤖 OFFER DECLINED: Seller declined the offer of GHS ${String.format(Locale.getDefault(), "%.2f", offer.offerAmount)}."
                                    )
                                }
                            }
                        },
                        onBuyNow = {
                            currentView = MarketplaceView.PAYMENT
                        },
onRemoveListing = {
                            selectedListing?.let {
                                viewModel.deleteMarketplaceListing(it)
                                currentView = MarketplaceView.BROWSE
                            }
                        }
                    )
                }

                MarketplaceView.CHAT -> {
                    ChatScreen(
                        chatRoomId = activeChatRoomId ?: "",
                        partnerName = activeChatPartnerName,
                        listing = activeChatListing!!,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        buyerId = activeChatBuyerId ?: currentUser.id,
                        onBack = handleBack,
                        onProceedToPayment = {
                            currentView = MarketplaceView.PAYMENT
                        },
                        onViewTransaction = { tx ->
                            activeTransaction = tx
                            currentView = MarketplaceView.TRANSACTION_ESCROW
                        }
                    )
                }

                MarketplaceView.PAYMENT -> {
                    PaymentScreen(
                        listing = selectedListing!!,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onBack = handleBack,
                        onPaymentSuccess = { txId ->
                            coroutineScope.launch {
                                val tx = viewModel.repository.getMarketplaceTransactionByIdOneShot(txId.toInt())
                                if (tx != null) {
                                    activeTransaction = tx
                                    currentView = MarketplaceView.TRANSACTION_ESCROW
                                } else {
                                    currentView = MarketplaceView.BROWSE
                                }
                            }
                        }
                    )
                }

                MarketplaceView.TRANSACTION_ESCROW -> {
                    TransactionEscrowScreen(
                        transaction = activeTransaction!!,
                        listing = selectedListing!!,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onBack = handleBack,
                        onDisputeReport = {
                            currentView = MarketplaceView.DISPUTE
                        },
                        onTransactionUpdated = { updatedTx ->
                            activeTransaction = updatedTx
                        }
                    )
                }

                MarketplaceView.DISPUTE -> {
                    DisputeScreen(
                        transaction = activeTransaction!!,
                        listing = selectedListing!!,
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onBack = handleBack,
                        onDisputeResolved = { resolvedTx ->
                            activeTransaction = resolvedTx
                            currentView = MarketplaceView.TRANSACTION_ESCROW
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BROWSE LISTINGS SCREEN (MAIN MARKETPLACE INTERFACE)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseListingsScreen(
    viewModel: RaivalViewModel,
    currentUser: User,
    activeListings: List<MarketplaceListing>,
    allListings: List<MarketplaceListing>,
    userTransactions: List<MarketplaceTransaction>,
    allTransactions: List<MarketplaceTransaction>,
    activeSubTab: Int,
    onSubTabChange: (Int) -> Unit,
    onSelectListing: (MarketplaceListing) -> Unit,
    onContactSeller: (MarketplaceListing) -> Unit,
    onSelectTransaction: (MarketplaceTransaction) -> Unit,
    onCreateListingClick: () -> Unit,
    onUserClick: (User) -> Unit = {}
) {
    val allUsers by viewModel.allUsers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedGameFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RaivalBackground,
        floatingActionButton = {
            if (activeSubTab == 0 || activeSubTab == 1) {
                FloatingActionButton(
                    onClick = onCreateListingClick,
                    containerColor = RaivalPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("sell_team_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = "Sell")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sell Team", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Marketplace Header Dashboard card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏪 TEAM MARKETPLACE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = RaivalSecondary
                        )
                        Box(
                            modifier = Modifier
                                .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "5% ESCROW FEE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaivalPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Buy & sell premium eFootball, FC Mobile, and DLS squads securely through our escrow system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Listings", fontSize = 10.sp, color = RaivalTextSecondary)
                            Text("${activeListings.size}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        }
                        HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = RaivalSurfaceLight)
                        Column {
                            Text("Pending Escrow", fontSize = 10.sp, color = RaivalTextSecondary)
                            val pendingCount = userTransactions.count { it.status == "Payment Held" || it.status == "Team Transferred" || it.status == "Disputed" }
                            Text("$pendingCount", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        }
                        HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = RaivalSurfaceLight)
                        Column {
                            Text("Your Wallet Balance", fontSize = 10.sp, color = RaivalTextSecondary)
                            Text("GHS ${String.format(Locale.getDefault(), "%.2f", currentUser.balance)}", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Tabs Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val subTabs = mutableListOf(
                    "Browse" to Icons.Default.Search,
                    "My Listings" to Icons.Default.Sell,
                    "Escrow Tracker" to Icons.Default.VerifiedUser
                )
                if (currentUser.role == "admin") {
                    subTabs.add("Admin Center" to Icons.Default.AdminPanelSettings)
                }

                subTabs.forEachIndexed { index, pair ->
                    val isSelected = activeSubTab == index
                    val containerColor = if (isSelected) RaivalPrimary else Color.Transparent
                    val contentColor = if (isSelected) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(containerColor)
                            .clickable { onSubTabChange(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(pair.second, contentDescription = pair.first, modifier = Modifier.size(14.dp), tint = contentColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pair.first,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (activeSubTab) {
                0 -> {
                    // Browse listings sub-screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filters Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search Barcelona, Real Madrid...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RaivalPrimary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("marketplace_search_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalSurfaceLight
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Game Quick-Filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val games = listOf("All", "eFootball", "FC Mobile", "DLS")
                            games.forEach { game ->
                                val isSelected = selectedGameFilter == game
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) RaivalPrimary else RaivalSurface)
                                        .border(BorderStroke(1.dp, if (isSelected) RaivalPrimary else RaivalSurfaceLight), RoundedCornerShape(20.dp))
                                        .clickable { selectedGameFilter = game }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = game,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        // Sorting and count row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Teams (${activeListings.size})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = RaivalTextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    sortBy = when (sortBy) {
                                        "Newest" -> "Lowest Price"
                                        "Lowest Price" -> "Highest OVR"
                                        else -> "Newest"
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(14.dp), tint = RaivalPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sort: $sortBy",
                                    fontSize = 11.sp,
                                    color = RaivalPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Filtered Listings List
                        val filteredListings = activeListings
                            .filter {
                                (selectedGameFilter == "All" || it.game.equals(selectedGameFilter, ignoreCase = true)) &&
                                        (it.teamName.contains(searchQuery, ignoreCase = true) ||
                                                it.sellerName.contains(searchQuery, ignoreCase = true))
                            }
                            .sortedWith(
                                when (sortBy) {
                                    "Lowest Price" -> compareBy { it.price }
                                    "Highest OVR" -> compareByDescending { it.ovrRating }
                                    else -> compareByDescending { it.listedAt }
                                }
                            )

                        if (filteredListings.isEmpty()) {
                            EmptyStateView(
                                message = "No active listings found matching your search. Be the first to sell a team!",
                                icon = Icons.Default.Storefront
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                filteredListings.forEach { listing ->
                                    ListingCard(
                                        listing = listing,
                                        isMyListing = listing.sellerId == currentUser.id,
                                        onSelect = { onSelectListing(listing) },
                                        onContact = { onContactSeller(listing) },
                                        onUserClick = {
                                            val sellerUser = allUsers.find { it.id == listing.sellerId }
                                            if (sellerUser != null) {
                                                onUserClick(sellerUser)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // My listings sub-screen
                    val myListings = allListings.filter { it.sellerId == currentUser.id }
                    if (myListings.isEmpty()) {
                        EmptyStateView(
                            message = "You haven't listed any teams for sale yet. Tap 'Sell Team' below to get started!",
                            icon = Icons.Default.Sell
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            myListings.forEach { listing ->
                                ListingCard(
                                    listing = listing,
                                    isMyListing = true,
                                    onSelect = { onSelectListing(listing) },
                                    onContact = {},
                                    onUserClick = {
                                        val sellerUser = allUsers.find { it.id == listing.sellerId }
                                        if (sellerUser != null) {
                                            onUserClick(sellerUser)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Escrow transaction tracker
                    if (userTransactions.isEmpty()) {
                        EmptyStateView(
                            message = "You don't have any active marketplace transactions. Secure trades appear here when you buy or sell a team.",
                            icon = Icons.Default.VerifiedUser
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            userTransactions.forEach { tx ->
                                TransactionTrackerCard(
                                    tx = tx,
                                    isSeller = tx.sellerId == currentUser.id,
                                    onClick = { onSelectTransaction(tx) }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // Admin Control Dashboard
                    AdminMarketplaceDashboard(
                        allTransactions = allTransactions,
                        allListings = allListings,
                        viewModel = viewModel,
                        onSelectTransaction = onSelectTransaction,
                        onSelectListing = onSelectListing
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LISTING CARD COMPONENT
// -------------------------------------------------------------
@Composable
fun ListingCard(
    listing: MarketplaceListing,
    isMyListing: Boolean,
    onSelect: () -> Unit,
    onContact: () -> Unit,
    onUserClick: () -> Unit = {}
) {
    val gameColor = when (listing.game.lowercase()) {
        "efootball" -> RaivalPrimary
        "fc mobile" -> RaivalSecondary
        else -> RaivalAccent // DLS
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RaivalSurfaceLight),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(gameColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = listing.game,
                            color = gameColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (listing.ratingType) {
                            "Collective Strength" -> "${listing.ovrRating} CS"
                            "Team Rating" -> "${listing.ovrRating} Rating"
                            else -> "${listing.ovrRating} OVR"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)}",
                    color = RaivalSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Team Name
            Text(
                text = listing.teamName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )

            // Players Preview
            Spacer(modifier = Modifier.height(4.dp))
            val previewPlayers = listing.players.split("\n").take(3).joinToString(" • ") { it.replace("⭐ ", "") }
            Text(
                text = previewPlayers,
                color = RaivalTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom seller information & Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seller profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onUserClick()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(RaivalSurfaceLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = listing.sellerName.take(1).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaivalPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isMyListing) "You (Seller)" else listing.sellerName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${listing.sellerRating} • ${listing.views} views",
                                fontSize = 9.sp,
                                color = RaivalTextSecondary
                            )
                        }
                    }
                }

                // Listing state / action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMyListing) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (listing.status == "Active") RaivalSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = listing.status.uppercase(),
                                color = if (listing.status == "Active") RaivalSuccess else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onContact,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Buy", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TRANSACTION TRACKER CARD COMPONENT
// -------------------------------------------------------------
@Composable
fun TransactionTrackerCard(
    tx: MarketplaceTransaction,
    isSeller: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (tx.status) {
        "Payment Held" -> RaivalSecondary
        "Team Transferred" -> RaivalPrimary
        "Completed" -> RaivalSuccess
        "Disputed" -> RaivalError
        else -> Color.LightGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction #${1000 + tx.id}",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tx.status.uppercase(),
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Title
            Text(
                text = if (isSeller) "Selling to ${tx.buyerName}" else "Buying from ${tx.sellerName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Escrow Total", fontSize = 10.sp, color = RaivalTextSecondary)
                    Text("GHS ${String.format(Locale.getDefault(), "%.2f", tx.totalAmount)}", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (tx.status == "Completed") "View Details" else "Track Escrow",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SELLER/BUYER TRANSACTION ESCROW DETAIL SCREEN
// -------------------------------------------------------------
@Composable
fun TransactionEscrowScreen(
    transaction: MarketplaceTransaction,
    listing: MarketplaceListing,
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit,
    onDisputeReport: () -> Unit,
    onTransactionUpdated: (MarketplaceTransaction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isSeller = transaction.sellerId == currentUser.id

    val escrowSteps = listOf(
        "Escrow Funded" to "Buyer deposited funds securely into Raival's holding bank.",
        "Team Transferred" to "Seller transfers game credentials to the buyer.",
        "Roster Verified" to "Buyer verifies the team content matches listing.",
        "Funds Released" to "Escrow is released directly into Seller's cash wallet."
    )

    val currentStepIndex = when (transaction.status) {
        "Payment Held" -> 1
        "Team Transferred" -> 2
        "Completed" -> 4
        "Disputed" -> 2 // freeze step view
        else -> 4
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Escrow Transaction", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = listing.teamName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        val ratingText = when (listing.ratingType) {
                            "Collective Strength" -> "${listing.ovrRating} CS"
                            "Team Rating" -> "${listing.ovrRating} Rating"
                            else -> "${listing.ovrRating} OVR"
                        }
                        Text(
                            text = "${listing.game} • $ratingText • listed by ${transaction.sellerName}",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = RaivalSurfaceLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Negotiated Price", fontSize = 11.sp, color = RaivalTextSecondary)
                                Text("GHS ${String.format(Locale.getDefault(), "%.2f", transaction.amount)}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Status", fontSize = 11.sp, color = RaivalTextSecondary)
                                val statusColor = if (transaction.status == "Completed") RaivalSuccess else if (transaction.status == "Disputed") RaivalError else RaivalPrimary
                                Text(transaction.status, fontWeight = FontWeight.Black, color = statusColor, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Timeline Steps View
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🛡️ SECURE ESCROW PIPELINE",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = RaivalPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        escrowSteps.forEachIndexed { index, (title, description) ->
                            val isCompleted = index < currentStepIndex
                            val isActive = index == currentStepIndex
                            val stepColor = if (isCompleted) RaivalSuccess else if (isActive) RaivalPrimary else RaivalTextSecondary

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (isCompleted) RaivalSuccess.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .border(
                                                BorderStroke(
                                                    2.dp,
                                                    if (isCompleted) RaivalSuccess else if (isActive) RaivalPrimary else RaivalSurfaceLight
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = RaivalSuccess)
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = stepColor
                                            )
                                        }
                                    }

                                    if (index < escrowSteps.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .height(30.dp)
                                                .width(2.dp),
                                            color = if (isCompleted) RaivalSuccess else RaivalSurfaceLight
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) RaivalPrimary else Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = description,
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real Interactive Actions based on buyer/seller status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "REQUIRED ACTIONS",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (transaction.status) {
                            "Payment Held" -> {
                                if (isSeller) {
                                    Text(
                                        text = "Buyer has successfully deposited GHS ${String.format(Locale.getDefault(), "%.2f", transaction.totalAmount)} into Escrow. Please transfer the team roster credentials to the buyer immediately.",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val updated = transaction.copy(status = "Team Transferred")
                                                viewModel.updateMarketplaceTransaction(updated) {
                                                    onTransactionUpdated(updated)
                                                    coroutineScope.launch {
                                                        val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                                        viewModel.repository.sendChatMessage(
                                                            chatRoomId,
                                                            "🤖 DELIVERY CONFIRMED: Seller has confirmed team transfer! Buyer, please check your eFootball/FC Mobile/DLS account and verify the roster to release payment."
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Confirm Credentials Transferred", fontWeight = FontWeight.Black, color = Color.Black)
                                    }
                                } else {
                                    Text(
                                        text = "Payment is securely held in Escrow. Waiting for seller to transfer the account details.",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CircularProgressIndicator(color = RaivalPrimary, modifier = Modifier.size(24.dp))
                                }
                            }

                            "Team Transferred" -> {
                                if (!isSeller) {
                                    Text(
                                        text = "Seller reported credentials transferred. Please verify that the OVR rating, special cards, coins, and players exactly match the listing details. If everything is correct, click 'Release Payment'.",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onDisputeReport,
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalError),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("File Dispute", fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val updatedTx = transaction.copy(status = "Completed")
                                                    viewModel.updateMarketplaceTransaction(updatedTx) {
                                                        onTransactionUpdated(updatedTx)
                                                        coroutineScope.launch {
                                                            // Deduct from buyer, credit seller
                                                            val seller = viewModel.repository.getUserByIdOneShot(transaction.sellerId)
                                                            if (seller != null) {
                                                                viewModel.repository.saveUserProgress(seller.copy(balance = seller.balance + transaction.amount))
                                                            }
                                                            // Mark listing as Sold
                                                            viewModel.updateMarketplaceListing(listing.copy(status = "Sold"))

                                                            val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                                            viewModel.repository.sendChatMessage(
                                                                chatRoomId,
                                                                "🤖 TRANSACTION COMPLETE: Buyer verified team credentials! GHS ${String.format(Locale.getDefault(), "%.2f", transaction.amount)} has been released from Escrow to Seller's cash balance. Congratulations to both!"
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                                            modifier = Modifier.weight(1.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Release Payment", fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Roster transfer confirmation submitted. Waiting for buyer to check, verify, and release payment. Buyer has 24 hours to verify or report a dispute.",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CircularProgressIndicator(color = RaivalSuccess, modifier = Modifier.size(24.dp))
                                }
                            }

                            "Completed" -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = RaivalSuccess)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This transaction has been successfully verified and finalized. Escrow funds were transferred safely into Seller's wallet.",
                                    fontSize = 11.sp,
                                    color = RaivalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            "Disputed" -> {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(40.dp), tint = RaivalError)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This transaction has been flagged for dispute. Buyer claims roster content does not match listing specifications. Raival administrators are evaluating evidence.",
                                    fontSize = 11.sp,
                                    color = RaivalError,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onDisputeReport,
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Dispute Center", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TRANSACTION DISPUTE SCREEN
// -------------------------------------------------------------
@Composable
fun DisputeScreen(
    transaction: MarketplaceTransaction,
    listing: MarketplaceListing,
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit,
    onDisputeResolved: (MarketplaceTransaction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var issueReportText by remember { mutableStateOf(transaction.disputeReason ?: "") }
    var sellerResponseText by remember { mutableStateOf(transaction.disputeSellerResponse ?: "") }

    val isBuyer = transaction.buyerId == currentUser.id
    val isSeller = transaction.sellerId == currentUser.id
    val isAdmin = currentUser.role == "admin"

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Resolution Center", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ DISPUTE CASE INFORMATION", fontWeight = FontWeight.Black, color = RaivalError)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Transaction ID: #${1000 + transaction.id}", fontSize = 11.sp, color = RaivalTextSecondary)
                    Text("Team Name: ${listing.teamName} (${listing.game})", fontSize = 11.sp, color = Color.White)
                    Text("Escrow Held: GHS ${String.format(Locale.getDefault(), "%.2f", transaction.totalAmount)}", fontSize = 11.sp, color = RaivalSecondary)
                }
            }

            // Buyer's claim section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 BUYER'S COMPLAINT & EVIDENCE", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isBuyer && transaction.status != "Disputed") {
                        OutlinedTextField(
                            value = issueReportText,
                            onValueChange = { issueReportText = it },
                            label = { Text("Describe the discrepancy in detail...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaivalPrimary,
                                focusedLabelColor = RaivalPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val updatedTx = transaction.copy(
                                        status = "Disputed",
                                        disputeReason = issueReportText
                                    )
                                    viewModel.updateMarketplaceTransaction(updatedTx) {
                                        onDisputeResolved(updatedTx)
                                        coroutineScope.launch {
                                            val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                            viewModel.repository.sendChatMessage(
                                                chatRoomId,
                                                "🚨 DISPUTE REPORTED: Buyer has opened a dispute! Claim: $issueReportText. Escrow is frozen pending investigation."
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalError),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Dispute & Freeze Escrow", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (transaction.disputeReason.isNullOrBlank()) "No claim details filed yet." else transaction.disputeReason,
                            fontSize = 12.sp,
                            color = RaivalTextSecondary
                        )
                    }
                }
            }

            // Seller's response section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💬 SELLER'S COUNTER-ARGUMENT", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isSeller && transaction.status == "Disputed" && transaction.disputeSellerResponse.isNullOrBlank()) {
                        OutlinedTextField(
                            value = sellerResponseText,
                            onValueChange = { sellerResponseText = it },
                            label = { Text("Submit your counter-statement...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaivalPrimary,
                                focusedLabelColor = RaivalPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val updatedTx = transaction.copy(
                                        disputeSellerResponse = sellerResponseText
                                    )
                                    viewModel.updateMarketplaceTransaction(updatedTx) {
                                        onDisputeResolved(updatedTx)
                                        coroutineScope.launch {
                                            val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                            viewModel.repository.sendChatMessage(
                                                chatRoomId,
                                                "💬 SELLER RESPONDED: Seller submitted counter-statement: $sellerResponseText"
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Counter Statement", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Text(
                            text = if (transaction.disputeSellerResponse.isNullOrBlank()) "Waiting for seller's counter statement." else transaction.disputeSellerResponse,
                            fontSize = 12.sp,
                            color = RaivalTextSecondary
                        )
                    }
                }
            }

            // Admin Decision panel
            if (isAdmin && transaction.status == "Disputed") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🛡️ ADMIN DETERMINATION HUB", fontWeight = FontWeight.Black, color = RaivalSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You are reviewing this case as a Raival Marketplace Moderator. Verify listing specifications and dispute claims carefully before deciding.",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    // Refund Buyer: release escrow back to buyer's balance, void listing
                                    coroutineScope.launch {
                                        val updatedTx = transaction.copy(status = "Refunded")
                                        viewModel.updateMarketplaceTransaction(updatedTx) {
                                            onDisputeResolved(updatedTx)
                                            coroutineScope.launch {
                                                // Return funds to buyer
                                                val buyer = viewModel.repository.getUserByIdOneShot(transaction.buyerId)
                                                if (buyer != null) {
                                                    viewModel.repository.saveUserProgress(buyer.copy(balance = buyer.balance + transaction.totalAmount))
                                                }

                                                val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                                viewModel.repository.sendChatMessage(
                                                    chatRoomId,
                                                    "🛡️ ADMIN DECISION: Case ruled in favor of BUYER. Escrow of GHS ${String.format(Locale.getDefault(), "%.2f", transaction.totalAmount)} has been fully refunded to Buyer's wallet."
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalError),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Approve Buyer - Refund Funds", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    // Payout Seller: release escrow to seller balance
                                    coroutineScope.launch {
                                        val updatedTx = transaction.copy(status = "Completed")
                                        viewModel.updateMarketplaceTransaction(updatedTx) {
                                            onDisputeResolved(updatedTx)
                                            coroutineScope.launch {
                                                // Give funds to seller
                                                val seller = viewModel.repository.getUserByIdOneShot(transaction.sellerId)
                                                if (seller != null) {
                                                    viewModel.repository.saveUserProgress(seller.copy(balance = seller.balance + transaction.amount))
                                                }
                                                // Mark listing as Sold
                                                viewModel.updateMarketplaceListing(listing.copy(status = "Sold"))

                                                val chatRoomId = "marketplace_${listing.id}_buyer_${transaction.buyerId}"
                                                viewModel.repository.sendChatMessage(
                                                    chatRoomId,
                                                    "🛡️ ADMIN DECISION: Case ruled in favor of SELLER. Escrow of GHS ${String.format(Locale.getDefault(), "%.2f", transaction.amount)} has been released to Seller's wallet."
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Approve Seller - Release Payout", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CREATE LISTING SCREEN (SELL YOUR TEAM)
// -------------------------------------------------------------
@Composable
fun CreateListingScreen(
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit,
    onListingCreated: () -> Unit
) {
    var selectedGame by remember { mutableStateOf("eFootball") }
    var teamName by remember { mutableStateOf("") }
    var ovrRating by remember { mutableStateOf("") }
    var ratingType by remember { mutableStateOf("OVR") }
    var playersList by remember { mutableStateOf("") }
    var specialCards by remember { mutableStateOf("5") }
    var coinsInput by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var priceGhs by remember { mutableStateOf("") }
    var listingType by remember { mutableStateOf("Negotiable") } // "Fixed Price", "Negotiable", "Auction"
    var durationDays by remember { mutableIntStateOf(7) } // 7, 14, 30
    var playerScreenshotsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddScreenshotDialog by remember { mutableStateOf(false) }

    val localImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            playerScreenshotsList = playerScreenshotsList + uri.toString()
        }
    }

    var isSubmitting by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Sell Your Team", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game select
            Text("Select Mobile Football Game", fontWeight = FontWeight.Bold, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val games = listOf("eFootball", "FC Mobile", "DLS")
                games.forEach { game ->
                    val isSelected = selectedGame == game
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RaivalPrimary else RaivalSurface)
                            .border(BorderStroke(1.dp, if (isSelected) RaivalPrimary else RaivalSurfaceLight), RoundedCornerShape(12.dp))
                            .clickable { selectedGame = game }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Metric Selector
            Text("Select Rating Metric Type", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val metrics = listOf("OVR", "Collective Strength", "Team Rating")
                metrics.forEach { metric ->
                    val isSelected = ratingType == metric
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RaivalSecondary.copy(alpha = 0.15f) else RaivalSurface)
                            .border(BorderStroke(1.dp, if (isSelected) RaivalSecondary else RaivalSurfaceLight), RoundedCornerShape(8.dp))
                            .clickable { ratingType = metric }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = metric,
                            fontSize = 10.sp,
                            color = if (isSelected) RaivalSecondary else Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Team Name & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.weight(1.5f).testTag("listing_team_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ovrRating,
                    onValueChange = { ovrRating = it },
                    label = { Text(ratingType) },
                    modifier = Modifier.weight(1f).testTag("listing_ovr_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                    singleLine = true
                )
            }

            // Players Screenshots section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, RaivalSurfaceLight), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📸 Player Screenshots", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Text("Provide screenshot verifications of all your major players", fontSize = 10.sp, color = RaivalTextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { localImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("📁 Local", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showAddScreenshotDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+ Add", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (playerScreenshotsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(RaivalBackground, shape = RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No player screenshots added yet.", color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(playerScreenshotsList.size) { index ->
                            val screenshotName = playerScreenshotsList[index]
                            Card(
                                modifier = Modifier.size(width = 110.dp, height = 150.dp),
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Simulated player card icon/headshot
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(RaivalBackground, shape = CircleShape)
                                                .border(BorderStroke(1.dp, RaivalPrimary), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = RaivalPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Text(
                                            text = screenshotName.substringBefore(".jpg").replace("_", " "),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(RaivalSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("VERIFIED SCREENSHOT", fontSize = 7.sp, color = RaivalSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Remove button
                                    IconButton(
                                        onClick = { playerScreenshotsList = playerScreenshotsList - screenshotName },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Players list description
            OutlinedTextField(
                value = playersList,
                onValueChange = { playersList = it },
                label = { Text("Core Players (one per line, e.g. CF - Messi)") },
                modifier = Modifier.fillMaxWidth().height(120.dp).testTag("listing_players_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary)
            )

            // Special cards & Coins values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = specialCards,
                    onValueChange = { specialCards = it },
                    label = { Text("Special Cards count") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                    singleLine = true
                )

                OutlinedTextField(
                    value = coinsInput,
                    onValueChange = { coinsInput = it },
                    label = { Text("In-Game Coins Balance") },
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                    singleLine = true
                )
            }

            // Description
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Other Details / Seller Notes") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary)
            )

            // Pricing
            OutlinedTextField(
                value = priceGhs,
                onValueChange = { priceGhs = it },
                label = { Text("Price (GHS)") },
                modifier = Modifier.fillMaxWidth().testTag("listing_price_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                singleLine = true,
                leadingIcon = { Text("GHS", modifier = Modifier.padding(start = 8.dp), color = RaivalSecondary, fontWeight = FontWeight.Bold) }
            )

            // Listing Type and duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("Fixed Price", "Negotiable", "Auction")
                types.forEach { type ->
                    val isSelected = listingType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RaivalSecondary.copy(alpha = 0.15f) else RaivalSurface)
                            .border(BorderStroke(1.dp, if (isSelected) RaivalSecondary else RaivalSurfaceLight), RoundedCornerShape(8.dp))
                            .clickable { listingType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(type, fontSize = 11.sp, color = if (isSelected) RaivalSecondary else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Post button
            Button(
                onClick = {
                    val priceVal = priceGhs.toDoubleOrNull() ?: 0.0
                    val ovrVal = ovrRating.toIntOrNull() ?: 80
                    val cardsVal = specialCards.toIntOrNull() ?: 0
                    val coinsVal = coinsInput.toIntOrNull() ?: 0

                    if (teamName.isNotBlank() && priceVal > 0.0) {
                        isSubmitting = true
                        viewModel.createMarketplaceListing(
                            MarketplaceListing(
                                sellerId = currentUser.id,
                                sellerName = currentUser.username,
                                game = selectedGame,
                                teamName = teamName,
                                ovrRating = ovrVal,
                                players = playersList,
                                specialCardsCount = cardsVal,
                                coins = coinsVal,
                                description = descriptionText,
                                price = priceVal,
                                listingType = listingType,
                                durationDays = durationDays,
                                status = "Active",
                                views = 1,
                                ratingType = ratingType,
                                playerScreenshots = playerScreenshotsList.joinToString(",")
                            )
                        ) {
                            isSubmitting = false
                            onListingCreated()
                        }
                    }
                },
                enabled = !isSubmitting && teamName.isNotBlank() && priceGhs.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary, disabledContainerColor = RaivalSurfaceLight),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("post_listing_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("Post Listing", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (showAddScreenshotDialog) {
        var customPlayerName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("CF") }
        var selectedPlayerOvr by remember { mutableStateOf("90") }

        AlertDialog(
            onDismissRequest = { showAddScreenshotDialog = false },
            title = { Text("Upload Player Screenshot", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the details of the player shown in the screenshot.", color = RaivalTextSecondary, fontSize = 11.sp)

                    OutlinedTextField(
                        value = customPlayerName,
                        onValueChange = { customPlayerName = it },
                        label = { Text("Player Name (e.g., Messi)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = { selectedRole = it },
                            label = { Text("Role (e.g., CF)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = selectedPlayerOvr,
                            onValueChange = { selectedPlayerOvr = it },
                            label = { Text("Rating (e.g., 90)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customPlayerName.trim().ifBlank { "Player" }
                        val formatted = "${name}_(${selectedRole}_${selectedPlayerOvr}).jpg"
                        playerScreenshotsList = playerScreenshotsList + formatted
                        showAddScreenshotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                ) {
                    Text("UPLOAD & VERIFY", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddScreenshotDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight)
                ) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = RaivalSurface
        )
    }
}

// -------------------------------------------------------------
// LISTING DETAIL SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing: MarketplaceListing,
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit,
    onContactSeller: () -> Unit,
    onMakeOffer: (Double) -> Unit,
    onOpenChat: (Int, String) -> Unit,
    onAcceptOffer: (MarketplaceOffer) -> Unit,
    onDeclineOffer: (MarketplaceOffer) -> Unit,
    onBuyNow: () -> Unit,
    onRemoveListing: () -> Unit,
    onUserClick: (User) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isSeller = listing.sellerId == currentUser.id

    val offersFlow = remember(listing.id) { viewModel.getOffersForListing(listing.id) }
    val offers by offersFlow.collectAsState(initial = emptyList())

    var offerInputText by remember { mutableStateOf("") }
    var showOfferDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Details", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // General Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RaivalSurfaceLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = listing.game.uppercase(),
                                    color = RaivalPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)}",
                                color = RaivalSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = listing.teamName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = listing.description.ifBlank { "No additional seller notes provided." },
                            fontSize = 12.sp,
                            color = RaivalTextSecondary
                        )
                    }
                }
            }

            // Seller Trust Card (for buyers to view and verify the seller)
            if (!isSeller) {
                item {
                    val userTxs by remember(listing.sellerId) {
                        viewModel.repository.getMarketplaceTransactionsForUser(listing.sellerId)
                    }.collectAsState(initial = emptyList())

                    val sales = userTxs.filter { it.sellerId == listing.sellerId }
                    val completedSales = sales.filter { it.status == "Completed" }
                    val disputedSales = sales.filter { it.status == "Disputed" }
                    val completedCount = completedSales.size
                    val disputedCount = disputedSales.size

                    var trustScore = 100
                    var trustText = "Provisional (New Seller)"
                    var trustColor = RaivalPrimary
                    var trustIcon = Icons.Default.VerifiedUser

                    val totalRated = completedCount + disputedCount
                    if (totalRated > 0) {
                        trustScore = (completedCount * 100) / totalRated
                        if (trustScore >= 95) {
                            trustText = "Elite Trader (Excellent)"
                            trustColor = RaivalSuccess
                            trustIcon = Icons.Default.Verified
                        } else if (trustScore >= 85) {
                            trustText = "Trusted Seller (Good)"
                            trustColor = RaivalSecondary
                            trustIcon = Icons.Default.CheckCircle
                        } else if (trustScore >= 70) {
                            trustText = "Fair Trader (Neutral)"
                            trustColor = Color.Yellow
                            trustIcon = Icons.Default.Info
                        } else {
                            trustText = "High Risk (Caution)"
                            trustColor = RaivalError
                            trustIcon = Icons.Default.Warning
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val sellerUser = viewModel.repository.getUserByIdOneShot(listing.sellerId)
                                    if (sellerUser != null) {
                                        onUserClick(sellerUser)
                                    }
                                }
                            }
                            .testTag("seller_trust_detail_card"),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                        border = BorderStroke(1.dp, trustColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "🛡️ ESCROW SELLER TRUST",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                        color = RaivalSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Seller: ${listing.sellerName}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = trustText,
                                        fontSize = 11.sp,
                                        color = trustColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = trustIcon,
                                        contentDescription = null,
                                        tint = trustColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "$trustScore%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = trustColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = RaivalSurfaceLight)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Success Rate", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text("$trustScore%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Completed Sales", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text("$completedCount sales", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalSuccess)
                                }
                                Column {
                                    Text("Disputes", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text("$disputedCount cases", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (disputedCount > 0) RaivalError else Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "👉 Click to view seller's full transaction profile, win-rate, badges, and levels.",
                                fontSize = 10.sp,
                                color = RaivalPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Team Spec Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 TEAM INFORMATION",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val ratingLabel = when (listing.ratingType) {
                            "Collective Strength" -> "Collective Strength"
                            "Team Rating" -> "Team Rating"
                            else -> "Overall Rating"
                        }
                        val ratingValue = when (listing.ratingType) {
                            "Collective Strength" -> "${listing.ovrRating} CS"
                            "Team Rating" -> "${listing.ovrRating} Rating"
                            else -> "${listing.ovrRating} OVR"
                        }
                        val specs = listOf(
                            Triple(ratingLabel, ratingValue, Icons.AutoMirrored.Filled.TrendingUp),
                            Triple("Special Cards", "${listing.specialCardsCount} Included", Icons.Default.Style),
                            Triple("Coins In-Game", "${listing.coins}", Icons.Default.Wallet)
                        )

                        specs.forEach { (label, value, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 12.sp, color = RaivalTextSecondary)
                                }
                                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = RaivalSurfaceLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Players Roster:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        listing.players.split("\n").forEach { player ->
                            if (player.isNotBlank()) {
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(12.dp), tint = RaivalSuccess)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(player, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Player Screenshots Card
            if (listing.playerScreenshots.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                        border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📸 VERIFIED PLAYER SCREENSHOTS",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Actual screenshots of players verified by the escrow system",
                                fontSize = 11.sp,
                                color = RaivalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val screenshots = listing.playerScreenshots.split(",")
                                items(screenshots.size) { index ->
                                    val screenshotName = screenshots[index].trim()
                                    if (screenshotName.isNotBlank()) {
                                        Card(
                                            modifier = Modifier.size(width = 120.dp, height = 160.dp),
                                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Simulated premium visual card frame
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .background(RaivalBackground, shape = CircleShape)
                                                        .border(BorderStroke(1.5.dp, RaivalSecondary), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = RaivalSecondary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }

                                                Text(
                                                    text = screenshotName.substringBefore(".jpg").replace("_", " "),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .background(RaivalSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("VERIFIED SCREENSHOT", fontSize = 7.sp, color = RaivalSuccess, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Escrow Pricing and Fee Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💰 PRICING & ESCROW FEES",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val platformFee = listing.price * 0.05
                        val sellerNet = listing.price - platformFee

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Team Selling Price", fontSize = 12.sp, color = RaivalTextSecondary)
                            Text("GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)}", fontSize = 12.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Platform Escrow Fee (5%)", fontSize = 12.sp, color = RaivalTextSecondary)
                            Text("GHS ${String.format(Locale.getDefault(), "%.2f", platformFee)}", fontSize = 12.sp, color = RaivalError)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Seller Net Income", fontSize = 12.sp, color = RaivalTextSecondary)
                            Text("GHS ${String.format(Locale.getDefault(), "%.2f", sellerNet)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalSuccess)
                        }
                    }
                }
            }

            // SELLER ACTIONS: Show Offers and Inquiries
            if (isSeller) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                        border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📩 BUYER INQUIRIES & OFFERS",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = RaivalSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (offers.isEmpty()) {
                                Text(
                                    "No active offers or inquiries from buyers yet. Your listing is being shown to other gamers.",
                                    fontSize = 11.sp,
                                    color = RaivalTextSecondary
                                )
                            } else {
                                offers.forEach { offer ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(RaivalSurfaceLight, shape = RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(offer.buyerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Text("Offer: GHS ${String.format(Locale.getDefault(), "%.2f", offer.offerAmount)}", color = RaivalPrimary, fontSize = 11.sp)
                                            Text("Status: ${offer.status}", color = RaivalTextSecondary, fontSize = 10.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = { onOpenChat(offer.buyerId, offer.buyerName) },
                                                modifier = Modifier.background(RaivalSurface, shape = CircleShape)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                                            }

                                            if (offer.status == "Pending") {
                                                IconButton(
                                                    onClick = { onAcceptOffer(offer) },
                                                    modifier = Modifier.background(RaivalSuccess.copy(alpha = 0.15f), shape = CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = RaivalSuccess, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = { onDeclineOffer(offer) },
                                                    modifier = Modifier.background(RaivalError.copy(alpha = 0.15f), shape = CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = RaivalError, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRemoveListing,
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalError),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Remove Listing", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // BUYER ACTIONS: Chat, Make Offer, Buy Now
            if (!isSeller) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onContactSeller,
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = RaivalPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Contact Seller", color = Color.White)
                            }

                            Button(
                                onClick = { showOfferDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, RaivalSecondary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Make Offer", color = RaivalSecondary)
                            }
                        }

                        Button(
                            onClick = onBuyNow,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Proceed to Buy (Escrow)", fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // Make Offer Dialog
    if (showOfferDialog) {
        AlertDialog(
            onDismissRequest = { showOfferDialog = false },
            title = { Text("Make Offer", fontWeight = FontWeight.Black, color = Color.White) },
            text = {
                Column {
                    Text("Propose a negotiated price to the seller. We'll alert the seller in your chat.", fontSize = 11.sp, color = RaivalTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = offerInputText,
                        onValueChange = { offerInputText = it },
                        label = { Text("Offer Amount (GHS)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = offerInputText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onMakeOffer(amount)
                            showOfferDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                ) {
                    Text("Submit Offer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfferDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = RaivalSurface
        )
    }
}

// -------------------------------------------------------------
// SECURE PAYMENT SCREEN
// -------------------------------------------------------------
@Composable
fun PaymentScreen(
    listing: MarketplaceListing,
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit,
    onPaymentSuccess: (Long) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("MTN MoMo") }
    var phoneInput by remember { mutableStateOf(currentUser.phone) }
    var pinInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val platformFee = listing.price * 0.05
    val totalCharged = listing.price + platformFee

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Secure Escrow Payment", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛒 PURCHASE CONTEXT", fontWeight = FontWeight.Bold, color = RaivalSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Team Name: ${listing.teamName}", color = Color.White)
                    Text("Game: ${listing.game}", color = RaivalTextSecondary, fontSize = 11.sp)
                    Text("Seller: ${listing.sellerName}", color = RaivalTextSecondary, fontSize = 11.sp)
                }
            }

            // Fee Invoice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧾 FEE CALCULATION INVOICE", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Team Roster Value", fontSize = 12.sp, color = RaivalTextSecondary)
                        Text("GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)}", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Secure Escrow Fee (5%)", fontSize = 12.sp, color = RaivalTextSecondary)
                        Text("GHS ${String.format(Locale.getDefault(), "%.2f", platformFee)}", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = RaivalSurfaceLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount Charged", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaivalPrimary)
                        Text("GHS ${String.format(Locale.getDefault(), "%.2f", totalCharged)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = RaivalPrimary)
                    }
                }
            }

            // Payment Methods Selection
            Text("Select Mobile Wallet Provider", fontWeight = FontWeight.Bold, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("MTN MoMo", "Vodafone Cash").forEach { method ->
                    val isSelected = selectedMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RaivalPrimary else RaivalSurface)
                            .border(BorderStroke(1.dp, if (isSelected) RaivalPrimary else RaivalSurfaceLight), RoundedCornerShape(12.dp))
                            .clickable { selectedMethod = method }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(method, fontWeight = FontWeight.Black, color = if (isSelected) Color.Black else Color.White)
                    }
                }
            }

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Mobile Money Wallet Number") },
                modifier = Modifier.fillMaxWidth().testTag("payment_phone_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                singleLine = true
            )

            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("4-Digit Wallet Pin (Dummy)") },
                modifier = Modifier.fillMaxWidth().testTag("payment_pin_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RaivalPrimary, focusedLabelColor = RaivalPrimary),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (phoneInput.isNotBlank()) {
                        isProcessing = true
                        viewModel.createMarketplaceTransaction(
                            MarketplaceTransaction(
                                listingId = listing.id,
                                buyerId = currentUser.id,
                                buyerName = currentUser.username,
                                sellerId = listing.sellerId,
                                sellerName = listing.sellerName,
                                amount = listing.price,
                                platformFee = platformFee,
                                totalAmount = totalCharged,
                                status = "Payment Held",
                                paymentMethod = selectedMethod,
                                phone = phoneInput
                            )
                        ) { txId ->
                            coroutineScope.launch {
                                // Deduct funds from buyer's account balance
                                val updatedUser = currentUser.copy(balance = currentUser.balance - totalCharged)
                                viewModel.repository.saveUserProgress(updatedUser)

                                // Insert auto chat announcement
                                val chatRoomId = "marketplace_${listing.id}_buyer_${currentUser.id}"
                                viewModel.repository.sendChatMessage(
                                    chatRoomId,
                                    "🤖 ESCROW ACTIVE: Buyer deposited GHS ${String.format(Locale.getDefault(), "%.2f", totalCharged)} (Value: GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)} + GHS ${String.format(Locale.getDefault(), "%.2f", platformFee)} fee) into held bank account. Seller: proceed with credentials delivery!"
                                )

                                isProcessing = false
                                onPaymentSuccess(txId)
                            }
                        }
                    }
                },
                enabled = !isProcessing && phoneInput.isNotBlank() && pinInput.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary, disabledContainerColor = RaivalSurfaceLight),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("payment_confirm_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Payment GHS ${String.format("%.2f", totalCharged)}", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }

            Text(
                text = "🔒 Payment held in bank-grade escrow. Released to seller only upon successful credential transfer and your explicit confirmation.",
                fontSize = 10.sp,
                color = RaivalTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// CHAT / MESSAGING VIEW
// -------------------------------------------------------------
@Composable
fun ChatScreen(
    chatRoomId: String,
    partnerName: String,
    listing: MarketplaceListing,
    viewModel: RaivalViewModel,
    currentUser: User,
    buyerId: Int,
    onBack: () -> Unit,
    onProceedToPayment: () -> Unit,
    onViewTransaction: (MarketplaceTransaction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var messageText by remember { mutableStateOf("") }

    // Active transaction status check
    val userTxs by viewModel.userMarketplaceTransactions.collectAsState()
    val activeTx = userTxs.find { it.listingId == listing.id && it.buyerId == buyerId }

    val offersFlow = remember(listing.id) { viewModel.getOffersForListing(listing.id) }
    val offers by offersFlow.collectAsState(initial = emptyList())
    val myOffer = offers.find { it.buyerId == buyerId }

    // Activate chatroom focus
    LaunchedEffect(chatRoomId) {
        viewModel.repository.setCurrentUser(currentUser) // safe refresh
        viewModel.selectChatRoom(chatRoomId)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text("Chat: $partnerName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "${listing.teamName} (${listing.game}) • GHS ${String.format(Locale.getDefault(), "%.2f", listing.price)}",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        },
        containerColor = RaivalBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Escrow Bar info or Active Payment prompt at top of chat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface)
                    .border(BorderStroke(1.dp, RaivalSurfaceLight))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeTx != null) "🛡️ ACTIVE ESCROW STATE" else "💡 PENDING DEAL STEPS",
                            fontWeight = FontWeight.Bold,
                            color = RaivalPrimary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = when {
                                activeTx != null -> "Status: ${activeTx.status}"
                                myOffer?.status == "Accepted" -> "Offer Accepted! Proceed to payment."
                                else -> "Negotiating price & terms."
                            },
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (activeTx != null) {
                        Button(
                            onClick = { onViewTransaction(activeTx) },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Track Escrow", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else if (myOffer?.status == "Accepted" && currentUser.id == buyerId) {
                        Button(
                            onClick = onProceedToPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Pay GHS ${String.format(Locale.getDefault(), "%.2f", myOffer.offerAmount)}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = false
            ) {
                items(chatMessages) { msg ->
                    val isMe = msg.senderId == currentUser.id
                    val isSystem = msg.senderUsername == "System" || msg.message.contains("🤖")

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isSystem) Alignment.Center else if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        if (isSystem) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RaivalSurfaceLight)
                                    .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    fontSize = 11.sp,
                                    color = RaivalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isMe) RaivalPrimary else RaivalSurface)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        fontSize = 12.sp,
                                        color = if (isMe) Color.Black else Color.White
                                    )
                                }
                                Text(
                                    text = msg.senderUsername,
                                    fontSize = 9.sp,
                                    color = RaivalTextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.sendChatMessage(chatRoomId, "🔍 Verification Requested: Can you verify that the CF Messi card is maxed out and show current coin levels?")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ask Verification", fontSize = 10.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.sendChatMessage(chatRoomId, "📸 Screenshot: (Image upload coming soon - currently text-only verification)")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Share Roster Screen", fontSize = 10.sp, color = Color.White)
                }
            }

            // Input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type message details...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RaivalPrimary,
                        unfocusedBorderColor = RaivalSurfaceLight
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.repository.sendChatMessage(chatRoomId, messageText)
                                messageText = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .background(RaivalPrimary, shape = CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN MARKETPLACE CONTROL DASHBOARD
// -------------------------------------------------------------
@Composable
fun AdminMarketplaceDashboard(
    allTransactions: List<MarketplaceTransaction>,
    allListings: List<MarketplaceListing>,
    viewModel: RaivalViewModel,
    onSelectTransaction: (MarketplaceTransaction) -> Unit,
    onSelectListing: (MarketplaceListing) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val activeDisputes = allTransactions.filter { it.status == "Disputed" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛡️ MODERATOR CONTROL CENTER", fontWeight = FontWeight.Black, color = RaivalSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Platform Volume", fontSize = 11.sp, color = RaivalTextSecondary)
                        val totalVolume = allTransactions.sumOf { it.totalAmount }
                        Text("GHS ${String.format(Locale.getDefault(), "%.2f", totalVolume)}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                    }
                    Column {
                        Text("Fees Earned", fontSize = 11.sp, color = RaivalTextSecondary)
                        val totalFees = allTransactions.sumOf { it.platformFee }
                        Text("GHS ${String.format(Locale.getDefault(), "%.2f", totalFees)}", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 15.sp)
                    }
                    Column {
                        Text("Active Disputes", fontSize = 11.sp, color = RaivalTextSecondary)
                        Text("${activeDisputes.size}", fontWeight = FontWeight.Black, color = RaivalError, fontSize = 15.sp)
                    }
                }
            }
        }

        // Active Disputes Section
        Text("🚨 ACTIVE DISPUTES (${activeDisputes.size})", fontWeight = FontWeight.Bold, color = Color.White)

        if (activeDisputes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No pending disputes reported. Escrows are running smoothly!", fontSize = 12.sp, color = RaivalTextSecondary)
                }
            }
        } else {
            activeDisputes.forEach { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTransaction(tx) },
                    border = BorderStroke(1.dp, RaivalError),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Disputed Tx #${1000 + tx.id}", fontWeight = FontWeight.Bold, color = RaivalError)
                            Text("GHS ${String.format(Locale.getDefault(), "%.2f", tx.totalAmount)}", color = RaivalSecondary, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Buyer: ${tx.buyerName} vs Seller: ${tx.sellerName}", fontSize = 11.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSelectTransaction(tx) },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Review Claims & Decide", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Listings Management Section
        Text("📋 ACTIVE PUBLIC LISTINGS (${allListings.size})", fontWeight = FontWeight.Bold, color = Color.White)

        allListings.forEach { listing ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectListing(listing) },
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(listing.teamName, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${listing.game} • OVR ${listing.ovrRating} • Seller: ${listing.sellerName}", fontSize = 10.sp, color = RaivalTextSecondary)
                        Text("Status: ${listing.status}", fontSize = 10.sp, color = if (listing.status == "Active") RaivalSuccess else Color.LightGray)
                    }

                    Row {
                        IconButton(
                            onClick = { onSelectListing(listing) }
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "View", tint = RaivalPrimary)
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteMarketplaceListing(listing)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RaivalError)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GENERAL UTILS
// -------------------------------------------------------------
@Composable
fun EmptyStateView(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(RaivalSurface, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = RaivalTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
