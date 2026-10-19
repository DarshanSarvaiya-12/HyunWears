package com.picell.hyunwears

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

// Why we use this: Blueprint for each T-shirt holding only primary data (ID, Name, Price, and Image URL).
data class TShirt(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val imageUrl: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Why we use this: Enforces a clean Light Mode color scheme across HyunWears.
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFFAFAFA),
                    surface = Color.White,
                    onBackground = Color(0xFF111111),
                    onSurface = Color(0xFF111111),
                    primary = Color(0xFF111111)
                )
            ) {
                HyunWearsApp()
            }
        }
    }
}

@Composable
fun HyunWearsApp() {
    val robotoFont = FontFamily.SansSerif
    val georgiaFont = FontFamily.Serif
    val context = LocalContext.current

    // Why we use this: Initializes our connection to Firebase Cloud Firestore.
    val db = remember { FirebaseFirestore.getInstance() }

    // Why we use this: Reactive state lists for live Firebase T-shirts, search text, cart count, and loading state.
    val catalog = remember { mutableStateListOf<TShirt>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var cartCount by remember { mutableIntStateOf(0) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Why we use this: Listens to the "tshirts" collection in Firebase in real time.
    // If empty on first launch, it automatically populates 4 starter T-shirts into your Firebase database.
    DisposableEffect(Unit) {
        val listener = db.collection("tshirts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    Toast.makeText(context, "Firebase error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        // Why we use this: Auto-seeds starter T-shirts with compressed images (w=400&q=70) so scrolling doesn't lag.
                        val starterTees = listOf(
                            mapOf("name" to "Oversized Noir Tee", "price" to 500, "imageUrl" to "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=400&q=70"),
                            mapOf("name" to "Classic Pure White Tee", "price" to 500, "imageUrl" to "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=400&q=70"),
                            mapOf("name" to "Urban Olive Heavy Tee", "price" to 650, "imageUrl" to "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=400&q=70"),
                            mapOf("name" to "Minimalist Cobalt Tee", "price" to 550, "imageUrl" to "https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=400&q=70")
                        )
                        starterTees.forEach { tee ->
                            db.collection("tshirts").add(tee)
                        }
                    } else {
                        catalog.clear()
                        for (doc in snapshot.documents) {
                            val name = doc.getString("name") ?: "HyunWears Tee"
                            val price = doc.getLong("price")?.toInt() ?: 500
                            val imageUrl = doc.getString("imageUrl") ?: ""
                            catalog.add(TShirt(id = doc.id, name = name, price = price, imageUrl = imageUrl))
                        }
                        isLoading = false
                    }
                }
            }

        // Why we use this: Listens to the "cart_orders" collection in Firebase so the top-right Cart count stays synced.
        val cartListener = db.collection("cart_orders")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    cartCount = snapshot.size()
                }
            }

        onDispose {
            listener.remove()
            cartListener.remove()
        }
    }

    // Why we use this: Filters the live Firebase T-shirts based on the user's search query.
    val filteredList = remember(catalog.size, searchQuery) {
        catalog.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HyunWears",
                        fontFamily = georgiaFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color(0xFF111111)
                    )

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Live Firebase Cart items: $cartCount", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cart ($cartCount)",
                            fontFamily = robotoFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                RgbSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    robotoFont = robotoFont
                )
            }

            if (isLoading) {
                item {
                    // Why we use this: Shows a clean loading spinner while fetching live T-shirts from Firebase.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF111111))
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { tshirt ->
                    val isSelected = selectedIds.contains(tshirt.id)
                    TShirtCard(
                        tshirt = tshirt,
                        isSelected = isSelected,
                        robotoFont = robotoFont,
                        onViewClick = {
                            Toast.makeText(context, "Viewing ${tshirt.name}", Toast.LENGTH_SHORT).show()
                        },
                        onBuyClick = {
                            // Why we use this: Saves the purchased T-shirt directly into Firebase Cloud Firestore!
                            val orderData = mapOf(
                                "tshirtId" to tshirt.id,
                                "name" to tshirt.name,
                                "price" to tshirt.price,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("cart_orders")
                                .add(orderData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Saved ${tshirt.name} to Firebase Cart!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onSelectClick = {
                            if (isSelected) selectedIds.remove(tshirt.id) else selectedIds.add(tshirt.id)
                        }
                    )
                }
            }

            item {
                FooterSection(robotoFont = robotoFont)
            }
        }
    }
}

@Composable
fun RgbSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    robotoFont: FontFamily
) {
    val rgbGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(width = 1.5.dp, brush = rgbGradient, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = Color(0xFF555555),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = robotoFont,
                    fontSize = 15.sp,
                    color = Color(0xFF111111)
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search T-shirts...",
                            fontFamily = robotoFont,
                            fontSize = 15.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun TShirtCard(
    tshirt: TShirt,
    isSelected: Boolean,
    robotoFont: FontFamily,
    onViewClick: () -> Unit,
    onBuyClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = tshirt.imageUrl,
                contentDescription = tshirt.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F2F2))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tshirt.name,
                    fontFamily = robotoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF111111)
                )
                Text(
                    text = "${tshirt.price}",
                    fontFamily = robotoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF111111)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                ) {
                    Text(
                        text = "View",
                        fontFamily = robotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF111111)
                    )
                }

                Button(
                    onClick = onBuyClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))
                ) {
                    Text(
                        text = "Buy",
                        fontFamily = robotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onSelectClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF111111) else Color(0xFFCCCCCC)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color(0xFFEEEEEE) else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (isSelected) "Selected" else "Select",
                        fontFamily = robotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF111111)
                    )
                }
            }
        }
    }
}

@Composable
fun FooterSection(robotoFont: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HyunWears by PiCell Company.",
            fontFamily = robotoFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF111111)
        )
    }
}
