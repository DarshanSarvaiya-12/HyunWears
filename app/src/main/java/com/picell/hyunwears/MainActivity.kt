package com.picell.hyunwears

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// Why we use this: Blueprint for each T-shirt matching our capitalized Firebase fields (Name, Price, ImageUrl).
data class TShirt(
    val id: String = "",
    val Name: String = "",
    val Price: Int = 0,
    val ImageUrl: String = ""
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

    // Why we use this: Initializes our connection to Firebase Cloud Firestore and Firebase Storage.
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    // Why we use this: Reactive state lists for live Firebase T-shirts, search text, cart count, and loading state.
    val catalog = remember { mutableStateListOf<TShirt>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var cartCount by remember { mutableIntStateOf(0) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Why we use this: State variables for the Gallery Picker and Upload Screen.
    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadPage by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // Why we use this: Opens the phone's native photo gallery when the user taps the Upload icon.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedGalleryUri = uri
            showUploadPage = true
        }
    }

    // Why we use this: Listens ONLY to the live "T-shirts" collection in Firebase Backend (no hardcoded frontend T-shirts).
    DisposableEffect(Unit) {
        val listener = db.collection("T-shirts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    Toast.makeText(context, "Firebase error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    catalog.clear()
                    for (doc in snapshot.documents) {
                        val name = doc.getString("Name") ?: ""
                        val price = doc.getLong("Price")?.toInt() ?: 0
                        val imageUrl = doc.getString("ImageUrl") ?: ""
                        catalog.add(
                            TShirt(
                                id = doc.id,
                                Name = name,
                                Price = price,
                                ImageUrl = imageUrl
                            )
                        )
                    }
                    isLoading = false
                }
            }

        // Why we use this: Listens to the "Cart_Orders" collection in Firebase so the top-right Cart count stays synced.
        val cartListener = db.collection("Cart_Orders")
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
        catalog.filter { it.Name.contains(searchQuery, ignoreCase = true) }
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
                    // Why we use this: Shows "HyunWears" at the upper-left side in Georgia font.
                    Text(
                        text = "HyunWears",
                        fontFamily = georgiaFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color(0xFF111111)
                    )

                    // Why we use this: Upper-right row holding the new Upload Icon button right next to the Cart button.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Why we use this: Upload Icon button that opens your phone's image gallery when tapped.
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = "Upload T-Shirt",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Why we use this: Cart button next to the Upload button.
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Cart_Orders items: $cartCount", Toast.LENGTH_SHORT).show()
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
        }
    ) { innerPadding ->
        // Why we use this: If the user picked an image from the gallery, show the Upload Page; otherwise show the T-shirt store catalog.
        if (showUploadPage && selectedGalleryUri != null) {
            UploadTShirtScreen(
                imageUri = selectedGalleryUri!!,
                isUploading = isUploading,
                robotoFont = robotoFont,
                modifier = Modifier.padding(innerPadding),
                onCancel = {
                    if (!isUploading) {
                        showUploadPage = false
                        selectedGalleryUri = null
                    }
                },
                onUploadSubmit = { enteredName, enteredPrice ->
                    isUploading = true
                    // Why we use this: Creates a unique file inside the "T-shirts/" folder in Firebase Storage.
                    val fileName = "T-shirts/tee_${System.currentTimeMillis()}.jpg"
                    val storageRef = storage.reference.child(fileName)

                    // Why we use this: Step 1 - Uploads the selected gallery image bytes to Firebase Storage.
                    storageRef.putFile(selectedGalleryUri!!)
                        .addOnSuccessListener {
                            // Why we use this: Step 2 - Retrieves the official Firebase Storage download URL for the uploaded image.
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                // Why we use this: Step 3 - Saves Name, Price, and ImageUrl into the "T-shirts" collection in Firebase Firestore.
                                val newTeeData = mapOf(
                                    "Name" to enteredName,
                                    "Price" to enteredPrice,
                                    "ImageUrl" to downloadUri.toString()
                                )
                                db.collection("T-shirts")
                                    .add(newTeeData)
                                    .addOnSuccessListener {
                                        isUploading = false
                                        showUploadPage = false
                                        selectedGalleryUri = null
                                        Toast.makeText(context, "Uploaded to Firebase!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isUploading = false
                                        Toast.makeText(context, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            isUploading = false
                            Toast.makeText(context, "Storage upload error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            )
        } else {
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
                            storage = storage,
                            isSelected = isSelected,
                            robotoFont = robotoFont,
                            onViewClick = {
                                Toast.makeText(context, "Viewing ${tshirt.Name}", Toast.LENGTH_SHORT).show()
                            },
                            onBuyClick = {
                                // Why we use this: Saves the purchased T-shirt into "Cart_Orders" with capitalized field names.
                                val orderData = mapOf(
                                    "TshirtId" to tshirt.id,
                                    "Name" to tshirt.Name,
                                    "Price" to tshirt.Price,
                                    "ImageUrl" to tshirt.ImageUrl,
                                    "Timestamp" to System.currentTimeMillis()
                                )
                                db.collection("Cart_Orders")
                                    .add(orderData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Saved ${tshirt.Name} to Cart_Orders!", Toast.LENGTH_SHORT).show()
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
}

// Why we use this: Upload Page where you preview your selected gallery photo, enter Name and Price, and upload to Firebase.
@Composable
fun UploadTShirtScreen(
    imageUri: Uri,
    isUploading: Boolean,
    robotoFont: FontFamily,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onUploadSubmit: (String, Int) -> Unit
) {
    var tshirtName by remember { mutableStateOf("") }
    var tshirtPriceText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Upload T-Shirt",
            fontFamily = robotoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF111111)
        )

        // Why we use this: Previews the image you picked from your phone's gallery.
        AsyncImage(
            model = imageUri,
            contentDescription = "Selected Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F2))
        )

        // Why we use this: Input box to enter the T-shirt Name.
        OutlinedTextField(
            value = tshirtName,
            onValueChange = { tshirtName = it },
            label = { Text("Name", fontFamily = robotoFont) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        // Why we use this: Numeric input box to enter the T-shirt Price (e.g., 500).
        OutlinedTextField(
            value = tshirtPriceText,
            onValueChange = { tshirtPriceText = it.filter { char -> char.isDigit() } },
            label = { Text("Price", fontFamily = robotoFont) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        if (isUploading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF111111))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = robotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }

                Button(
                    onClick = {
                        val priceVal = tshirtPriceText.toIntOrNull()
                        if (tshirtName.isBlank() || priceVal == null) {
                            Toast.makeText(context, "Enter Name and Price", Toast.LENGTH_SHORT).show()
                        } else {
                            onUploadSubmit(tshirtName.trim(), priceVal)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))
                ) {
                    Text(
                        text = "Upload",
                        fontFamily = robotoFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
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
    storage: FirebaseStorage,
    isSelected: Boolean,
    robotoFont: FontFamily,
    onViewClick: () -> Unit,
    onBuyClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    // Why we use this: Resolves Firebase Storage paths or download URLs from Firebase Backend.
    var resolvedDownloadUrl by remember(tshirt.ImageUrl) { mutableStateOf("") }

    LaunchedEffect(tshirt.ImageUrl) {
        val rawUrl = tshirt.ImageUrl.trim()
        if (rawUrl.isEmpty()) {
            resolvedDownloadUrl = ""
        } else if (rawUrl.startsWith("https://") || rawUrl.startsWith("http://")) {
            resolvedDownloadUrl = rawUrl
        } else if (rawUrl.startsWith("gs://")) {
            storage.getReferenceFromUrl(rawUrl).downloadUrl
                .addOnSuccessListener { uri -> resolvedDownloadUrl = uri.toString() }
        } else {
            storage.reference.child(rawUrl).downloadUrl
                .addOnSuccessListener { uri -> resolvedDownloadUrl = uri.toString() }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = resolvedDownloadUrl,
                contentDescription = tshirt.Name,
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
                    text = tshirt.Name,
                    fontFamily = robotoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF111111)
                )
                Text(
                    text = "${tshirt.Price}",
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
