package com.anbutravels.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF061B3A)
private val Blue = Color(0xFF0C3D78)
private val Yellow = Color(0xFFFFC928)
private val Pale = Color(0xFFF4F7FB)
private val Ink = Color(0xFF15243A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { AnbuApp() }
    }
}

data class RideBooking(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val phone: String = "",
    val pickup: String = "",
    val drop: String = "",
    val dateTime: String = "",
    val tripType: String = "One Way",
    val vehicle: String = "Sedan (4 Seater)",
    val distanceKm: String = "",
    val estimatedFare: Long = 0,
    val status: String = "Pending",
    val createdAt: Long = System.currentTimeMillis()
)

@Composable
fun AnbuApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var isAdmin by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Home") }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(user?.uid) {
        val current = user
        if (current == null) {
            isAdmin = false
            profileName = ""
        } else {
            db.collection("users").document(current.uid).get()
                .addOnSuccessListener { doc ->
                    profileName = doc.getString("name") ?: current.email?.substringBefore("@").orEmpty()
                    isAdmin = doc.getString("role") == "admin"
                }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            secondary = Yellow,
            background = Pale,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Navy
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
            if (user == null) {
                AuthScreen(
                    busy = busy,
                    message = message,
                    onLogin = { email, password ->
                        busy = true; message = ""
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                busy = false
                                if (!task.isSuccessful) message = task.exception?.localizedMessage ?: "Login failed"
                            }
                    },
                    onSignup = { name, phone, email, password ->
                        busy = true; message = ""
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = task.result.user?.uid
                                    if (uid == null) {
                                        busy = false; message = "Account created, but user ID was unavailable."
                                    } else {
                                        db.collection("users").document(uid).set(
                                            mapOf("name" to name.trim(), "phone" to phone.trim(),
                                                "email" to email.trim(), "role" to "customer",
                                                "createdAt" to System.currentTimeMillis())
                                        ).addOnCompleteListener { save ->
                                            busy = false
                                            if (!save.isSuccessful) message = "Account created; profile save failed. Check Firestore rules."
                                        }
                                    }
                                } else {
                                    busy = false
                                    message = task.exception?.localizedMessage ?: "Sign up failed"
                                }
                            }
                    }
                )
            } else {
                MainShell(
                    userId = user!!.uid,
                    name = profileName.ifBlank { user!!.email.orEmpty() },
                    email = user!!.email.orEmpty(),
                    phone = "",
                    isAdmin = isAdmin,
                    selectedTab = selectedTab,
                    onTab = { selectedTab = it },
                    onSignOut = { auth.signOut(); selectedTab = "Home" },
                    db = db
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(
    busy: Boolean,
    message: String,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String, String, String) -> Unit
) {
    var signup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).background(Color.White, RoundedCornerShape(28.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(74.dp).background(Yellow, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsCar, null, tint = Navy, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("ANBU", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Blue)
            Text("TOUR & TRAVELS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Safe • Comfortable • On Time", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(22.dp))
            Text(if (signup) "Create your account" else "Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(14.dp))
            if (signup) {
                AppField(name, { name = it }, "Full name", Icons.Default.Person)
                AppField(phone, { phone = it }, "Mobile number", Icons.Default.Phone, keyboard = KeyboardType.Phone)
            }
            AppField(email, { email = it }, "Email address", Icons.Default.Email, keyboard = KeyboardType.Email)
            AppField(password, { password = it }, "Password (minimum 6 characters)", Icons.Default.Lock, secret = true)
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (email.isBlank() || password.length < 6 || (signup && name.isBlank())) return@Button
                    if (signup) onSignup(name, phone, email, password) else onLogin(email, password)
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
                shape = RoundedCornerShape(14.dp)
            ) { if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Navy) else Text(if (signup) "SIGN UP" else "LOGIN", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { signup = !signup }) {
                Text(if (signup) "Already have an account? Login" else "New here? Create an account", color = Blue)
            }
        }
    }
}

@Composable
private fun AppField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    secret: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun MainShell(
    userId: String,
    name: String,
    email: String,
    phone: String,
    isAdmin: Boolean,
    selectedTab: String,
    onTab: (String) -> Unit,
    onSignOut: () -> Unit,
    db: FirebaseFirestore
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = if (isAdmin) listOf("Home", "Bookings", "Profile") else listOf("Home", "Bookings", "Packages", "Profile")
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTab(tab) },
                        icon = {
                            Icon(when (tab) {
                                "Home" -> Icons.Default.Home
                                "Bookings" -> Icons.Default.CalendarMonth
                                "Packages" -> Icons.Default.Luggage
                                else -> Icons.Default.Person
                            }, contentDescription = tab)
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            "Bookings" -> BookingsScreen(userId, isAdmin, db, Modifier.padding(padding))
            "Packages" -> PackagesScreen(Modifier.padding(padding))
            "Profile" -> ProfileScreen(name, email, isAdmin, onSignOut, Modifier.padding(padding))
            else -> HomeScreen(name, userId, db, Modifier.padding(padding), onOpenBookings = { onTab("Bookings") })
        }
    }
}

@Composable
private fun HomeScreen(name: String, userId: String, db: FirebaseFirestore, modifier: Modifier, onOpenBookings: () -> Unit) {
    var showBooking by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(Pale)) {
        Box(
            Modifier.fillMaxWidth().background(Navy).padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Column {
                Text("ANBU TOUR & TRAVELS", color = Yellow, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Your journey, our responsibility.", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Text("Vanakkam, $name", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Where would you like to go today?", color = Color.White.copy(alpha = .8f))
                Spacer(Modifier.height(15.dp))
                Button(onClick = { showBooking = true }, colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Text("BOOK YOUR RIDE", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showBooking) BookingForm(userId, db, onClose = { showBooking = false })
        else {
            Column(Modifier.padding(16.dp)) {
                Text("Our Services", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceCard("Book Ride", Icons.Default.DirectionsCar, Modifier.weight(1f)) { showBooking = true }
                    ServiceCard("Tour Packages", Icons.Default.Landscape, Modifier.weight(1f)) { showBooking = true }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceCard("Outstation", Icons.Default.Route, Modifier.weight(1f)) { showBooking = true }
                    ServiceCard("My Bookings", Icons.Default.ReceiptLong, Modifier.weight(1f), onOpenBookings)
                }
                Spacer(Modifier.height(18.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Popular destinations", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Perambalur  •  Trichy  •  Chennai  •  Madurai", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                        Text("Call your travel partner to confirm special trips.", color = Blue, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

@Composable
private fun BookingForm(userId: String, db: FirebaseFirestore, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pickup by remember { mutableStateOf("") }
    var drop by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())) }
    var trip by remember { mutableStateOf("One Way") }
    var vehicle by remember { mutableStateOf("Sedan (4 Seater)") }
    var distance by remember { mutableStateOf("50") }
    var sending by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val rate = when (vehicle) {
        "SUV (6 Seater)" -> 16L
        "Tempo Traveller (12 Seater)" -> 22L
        else -> 12L
    }
    val tripMultiplier = if (trip == "Round Trip") 2L else 1L
    val fare = ((distance.toLongOrNull() ?: 0L).coerceAtLeast(0) * rate * tripMultiplier) + 200L

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
            Text("Book a Ride", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            item {
                Text("Trip type", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("One Way", "Round Trip", "Outstation").forEach {
                        FilterChip(selected = trip == it, onClick = { trip = it }, label = { Text(it) })
                    }
                }
            }
            item { AppField(name, { name = it }, "Customer name", Icons.Default.Person) }
            item { AppField(phone, { phone = it }, "Mobile number", Icons.Default.Phone, keyboard = KeyboardType.Phone) }
            item { AppField(pickup, { pickup = it }, "Pickup location", Icons.Default.Place) }
            item { AppField(drop, { drop = it }, "Drop location", Icons.Default.Place) }
            item { AppField(date, { date = it }, "Date and time", Icons.Default.DateRange) }
            item {
                Text("Select vehicle", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                listOf("Sedan (4 Seater)", "SUV (6 Seater)", "Tempo Traveller (12 Seater)").forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { vehicle = item }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Blue)
                        Text(item, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        RadioButton(selected = vehicle == item, onClick = { vehicle = item })
                    }
                }
            }
            item { AppField(distance, { distance = it.filter(Char::isDigit) }, "Estimated distance (km)", Icons.Default.Route, keyboard = KeyboardType.Number) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Estimated fare", color = Color.Gray)
                        Text("₹$fare", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Blue)
                        Text("Indicative only. Final fare is confirmed by Anbu Tour & Travels.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            item {
                if (status.isNotBlank()) Text(status, color = if (status.startsWith("Booking submitted")) Color(0xFF167647) else MaterialTheme.colorScheme.error)
                Button(
                    onClick = {
                        if (name.isBlank() || phone.filter(Char::isDigit).length < 10 ||
                            pickup.isBlank() || drop.isBlank() || (distance.toLongOrNull() ?: 0L) <= 0L) {
                            status = "Enter name, a valid 10-digit phone, pickup, drop and distance above 0 km."
                        } else {
                            sending = true; status = ""
                            val ref = db.collection("bookings").document()
                            val booking = hashMapOf(
                                "userId" to userId, "customerName" to name.trim(), "phone" to phone.trim(),
                                "pickup" to pickup.trim(), "drop" to drop.trim(), "dateTime" to date.trim(),
                                "tripType" to trip, "vehicle" to vehicle, "distanceKm" to distance,
                                "estimatedFare" to fare, "status" to "Pending", "createdAt" to System.currentTimeMillis()
                            )
                            ref.set(booking).addOnCompleteListener { task ->
                                sending = false
                                if (task.isSuccessful) status = "Booking submitted successfully!" else status = task.exception?.localizedMessage ?: "Could not submit booking. Check Firebase rules."
                            }
                        }
                    },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)
                ) { if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("SUBMIT BOOKING", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun BookingsScreen(userId: String, isAdmin: Boolean, db: FirebaseFirestore, modifier: Modifier = Modifier) {
    var bookings by remember { mutableStateOf<List<RideBooking>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    DisposableEffect(userId, isAdmin) {
        val query = if (isAdmin) db.collection("bookings").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    else db.collection("bookings").whereEqualTo("userId", userId)
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, e ->
            loading = false
            if (e != null) error = e.localizedMessage ?: "Unable to load bookings"
            else {
                error = ""
                bookings = snapshot?.documents?.map { doc ->
                    RideBooking(
                        id = doc.id, userId = doc.getString("userId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        phone = doc.getString("phone") ?: "", pickup = doc.getString("pickup") ?: "",
                        drop = doc.getString("drop") ?: "", dateTime = doc.getString("dateTime") ?: "",
                        tripType = doc.getString("tripType") ?: "One Way",
                        vehicle = doc.getString("vehicle") ?: "Sedan (4 Seater)",
                        distanceKm = doc.getString("distanceKm") ?: "",
                        estimatedFare = doc.getLong("estimatedFare") ?: 0L,
                        status = doc.getString("status") ?: "Pending",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }?.sortedByDescending { it.createdAt } ?: emptyList()
            }
        }
        onDispose { registration.remove() }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(if (isAdmin) "Owner Dashboard" else "My Bookings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(if (isAdmin) "Review requests and update trip status." else "Track your ride requests and status.", color = Color.Gray)
        if (isAdmin && !loading) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("Total", bookings.size.toString(), Modifier.weight(1f))
                SummaryTile("Pending", bookings.count { it.status == "Pending" }.toString(), Modifier.weight(1f))
                SummaryTile("Confirmed", bookings.count { it.status == "Confirmed" }.toString(), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = searchText, onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (isAdmin) "Search customer, phone or route" else "Search your routes or vehicle") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(14.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            listOf("All", "Pending", "Confirmed", "Completed", "Cancelled").forEach { filter ->
                FilterChip(selected = statusFilter == filter, onClick = { statusFilter = filter }, label = { Text(filter, fontSize = 11.sp) })
            }
        }
        if (loading) CircularProgressIndicator()
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        val visibleBookings = bookings.filter { booking ->
            val matchesStatus = statusFilter == "All" || booking.status.equals(statusFilter, ignoreCase = true)
            val query = searchText.trim().lowercase()
            val matchesText = query.isBlank() || listOf(
                booking.customerName, booking.phone, booking.pickup, booking.drop, booking.vehicle, booking.id
            ).any { it.lowercase().contains(query) }
            matchesStatus && matchesText
        }
        if (!loading && visibleBookings.isEmpty() && error.isBlank()) Text("No matching bookings found.", color = Color.Gray, modifier = Modifier.padding(top = 20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visibleBookings, key = { it.id }) { booking ->
                BookingCard(booking, isAdmin) { newStatus ->
                    db.collection("bookings").document(booking.id).update("status", newStatus)
                        .addOnFailureListener { error = it.localizedMessage ?: "Could not update status" }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue)
        }
    }
}

@Composable
private fun BookingCard(booking: RideBooking, isAdmin: Boolean, onStatus: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = Blue, modifier = Modifier.size(34.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(booking.vehicle, fontWeight = FontWeight.Bold, color = Ink)
                    Text("${booking.pickup} → ${booking.drop}", fontSize = 12.sp, color = Color.Gray)
                    if (isAdmin) Text("${booking.customerName} • ${booking.phone}", fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${booking.estimatedFare}", fontWeight = FontWeight.Bold, color = Blue)
                    StatusPill(booking.status)
                }
            }
            Text("${booking.tripType} • ${booking.dateTime}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            if (isAdmin || expanded) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                if (isAdmin) {
                    Text("Update status", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
                        listOf("Confirmed", "Completed", "Cancelled").forEach { status ->
                            TextButton(onClick = { onStatus(status) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(status, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Text("Booking ID: ${booking.id.take(8)}", fontSize = 11.sp, color = Color.Gray)
                    if (booking.status == "Pending") {
                        TextButton(onClick = { onStatus("Cancelled") }) {
                            Text("Cancel pending booking", color = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status.lowercase()) {
        "confirmed" -> Color(0xFF168653)
        "completed" -> Color(0xFF2368C4)
        "cancelled" -> Color(0xFFC62828)
        else -> Color(0xFFEA7A16)
    }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(50)) {
        Text(status, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
    }
}

@Composable
private fun PackagesScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Tour Packages", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Sample packages — confirm availability and final prices with the owner.", color = Color.Gray)
        Spacer(Modifier.height(14.dp))
        listOf(
            Triple("Kodaikanal Tour", "2 Days / 1 Night", "From ₹4,999"),
            Triple("Ooty Tour", "2 Days / 1 Night", "From ₹5,999"),
            Triple("Rameswaram Tour", "3 Days / 2 Nights", "From ₹7,999"),
            Triple("Kerala Tour", "4 Days / 3 Nights", "From ₹9,999")
        ).forEach { (title, duration, price) ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Landscape, null, tint = Blue, modifier = Modifier.size(42.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(duration, fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(price, fontWeight = FontWeight.Bold, color = Blue, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(name: String, email: String, isAdmin: Boolean, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(if (isAdmin) "Owner Profile" else "My Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Icon(Icons.Default.AccountCircle, null, tint = Yellow, modifier = Modifier.size(58.dp))
                Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(email, color = Color.White.copy(alpha = .85f))
                Text(if (isAdmin) "Owner / Administrator" else "Customer", color = Yellow, modifier = Modifier.padding(top = 6.dp))
                Text("ANBU TOUR & TRAVELS", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                Text("Perambalur, Tamil Nadu", color = Color.White.copy(alpha = .8f))
                Text("Contact: +91 76049 05989", color = Color.White.copy(alpha = .8f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("LOG OUT", fontWeight = FontWeight.Bold)
        }
    }
}
