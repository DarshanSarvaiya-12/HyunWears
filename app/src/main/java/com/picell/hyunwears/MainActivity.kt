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

// Why we use this: Blueprint for each T-shirt holding only primary data (ID, Name, Price, and Image URL).
data class TShirt(
    val id: String,
    val name: String,
    val price: Int,
    val imageUrl: String
)

// Why we use this: Main entry point that boots up our Jetpack Compose frontend on the user's phone screen.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Why we use this: Forces a clean, modern Light Mode color scheme across the entire app.
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
    // Why we use this: FontFamily.SansSerif maps to Android's native Roboto font; Serif maps to Georgia-style serif font.
    val robotoFont = FontFamily.SansSerif
    val georgiaFont = FontFamily.Serif

    // Why we use this: Reactive state variables to store the user's search text, selected T-shirts, and cart count.
    var searchQuery by remember { mutableStateOf("") }
    var cartCount by remember { mutableIntStateOf(0) }
    val selectedIds = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    // Why we use this: Sample T-shirt catalog for Phase-1 frontend testing (will be replaced by Firebase in Step 2).
    val catalog = remember {
        listOf(
            TShirt("1", "Oversized Noir Tee", 500, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800"),
            TShirt("2", "Classic Pure White Tee", 500, "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800"),
            TShirt("3", "Urban Olive Heavy Tee", 650, "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=800"),
            TShirt("4", "Minimalist Cobalt Tee", 550, "https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800")
        )
    }

    // Why we use this: Filters the T-shirt list in real time when the user types in the RGB search bar.
    val filteredList = catalog.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    // Why we use this: Scaffold structures the screen cleanly with a fixed Top Header and scrollable catalog.
    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            // Why we use this: Top Header showing "HyunWears" at upper-left in Georgia font and Cart button at upper-right.
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
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

                    // Why we use this: Upper-right Cart button with curved corners showing live item count.
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Cart items: $cartCount", Toast.LENGTH_SHORT).show()
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
        // Why we use this: Smooth vertical scrolling list that holds the Search Bar, T-shirt Cards, and Footer.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Why we use this: Renders the Search Bar with a thin RGB gradient border.
                RgbSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    robotoFont = robotoFont
                )
            }

            // Why we use this: Loops through each T-shirt and displays its modern card.
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
                        cartCount++
                        Toast.makeText(context, "Added ${tshirt.name} to Cart", Toast.LENGTH_SHORT).show()
                    },
                    onSelectClick = {
                        if (isSelected) selectedIds.remove(tshirt.id) else selectedIds.add(tshirt.id)
                    }
                )
            }

            // Why we use this: Displays the required footer at the bottom of the catalog.
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
    // Why we use this: Creates the Red-Green-Blue (RGB) color gradient for the thin border.
    val rgbGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
    )

    // Why we use this: Box container styled with the thin 1.5dp RGB border and curved corners.
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
    // Why we use this: Clean white card container with soft elevation and rounded corners for each T-shirt.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Why we use this: Displays the T-shirt photograph at the top of the card.
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

            // Why we use this: Shows T-shirt name on the left and primary price on the right (no secondary discount texts).
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

            // Why we use this: Bottom row containing View, Buy, and Select buttons in curved-corner rectangles.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Button
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

                // Buy Button
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

                // Select Button
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
    // Why we use this: Displays "HyunWears by PiCell Company." cleanly centered at the bottom of the app.
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
