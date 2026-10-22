package com.picell.hyunwears

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.sin

// Why we use this: Blueprint for each T-shirt matching our capitalized Firebase fields including Material, Colour, and Fabric.
data class TShirt(
    val id: String = "",
    val Name: String = "",
    val Price: Int = 0,
    val ImageUrl: String = "",
    val Material: String = "Cotton",
    val Colour: String = "Green",
    val Fabric: String = "220 GSM"
)

// Why we use this: FontFamily.Serif maps directly to Android's built-in NotoSerif font engine.
val NotoSerifFontFamily = FontFamily.Serif

// Why we use this: Applies Noto Serif globally across the entire app.
val HyunWearsTypography = Typography(
    displayLarge = TextStyle(fontFamily = NotoSerifFontFamily),
    displayMedium = TextStyle(fontFamily = NotoSerifFontFamily),
    displaySmall = TextStyle(fontFamily = NotoSerifFontFamily),
    headlineLarge = TextStyle(fontFamily = NotoSerifFontFamily),
    headlineMedium = TextStyle(fontFamily = NotoSerifFontFamily),
    headlineSmall = TextStyle(fontFamily = NotoSerifFontFamily),
    titleLarge = TextStyle(fontFamily = NotoSerifFontFamily),
    titleMedium = TextStyle(fontFamily = NotoSerifFontFamily),
    titleSmall = TextStyle(fontFamily = NotoSerifFontFamily),
    bodyLarge = TextStyle(fontFamily = NotoSerifFontFamily),
    bodyMedium = TextStyle(fontFamily = NotoSerifFontFamily),
    bodySmall = TextStyle(fontFamily = NotoSerifFontFamily),
    labelLarge = TextStyle(fontFamily = NotoSerifFontFamily),
    labelMedium = TextStyle(fontFamily = NotoSerifFontFamily),
    labelSmall = TextStyle(fontFamily = NotoSerifFontFamily)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Why we use this: Draws the light-mode theme and instant opening animation on Frame 1.
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFFAFAFA),
                    surface = Color.White,
                    onBackground = Color(0xFF111111),
                    onSurface = Color(0xFF111111),
                    primary = Color(0xFF111111)
                ),
                typography = HyunWearsTypography
            ) {
                HyunWearsMainEntry()
            }
        }
    }
}

// Why we use this: Reusable white button with a thin black border that animates to a thicker dark border when clicked.
@Composable
fun AnimatedWhiteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isSelected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var clickPulse by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isActive = isPressed || clickPulse || isSelected

    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.4.dp else 1.dp,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "buttonBorderWidth"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF000000) else Color(0xFF333333),
        animationSpec = tween(durationMillis = 150),
        label = "buttonBorderColor"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                scope.launch {
                    clickPulse = true
                    onClick()
                    delay(220)
                    clickPulse = false
                }
            },
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// Why we use this: Plays the 2-note harmonic opening chime on a background thread the instant the app opens.
fun playOpeningSound() {
    Thread {
        try {
            val sampleRate = 44100
            val durationSeconds = 0.65
            val numSamples = (durationSeconds * sampleRate).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val freq = if (t < 0.18) 523.25 else 783.99
                val localT = if (t < 0.18) t else (t - 0.18)
                val envelope = exp(-6.0 * localT)
                val wave = sin(2.0 * Math.PI * freq * t) * envelope
                samples[i] = (wave * Short.MAX_VALUE * 0.35).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(700)
            audioTrack.release()
        } catch (_: Exception) {
        }
    }.start()
}

// Why we use this: Separates the Opening Screen from the Store UI so cold startup is instant.
@Composable
fun HyunWearsMainEntry() {
    var isOpeningFinished by remember { mutableStateOf(false) }

    if (!isOpeningFinished) {
        HyunWearsOpeningScreen(
            onOpeningComplete = { isOpeningFinished = true }
        )
    } else {
        HyunWearsApp()
    }
}

@Composable
fun HyunWearsOpeningScreen(onOpeningComplete: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        playOpeningSound()
        delay(850)
        onOpeningComplete()
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1.0f else 0.84f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500),
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HyunWears",
            fontFamily = NotoSerifFontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            fontSize = 34.sp,
            color = Color(0xFF111111),
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        )
    }
}

// Why we use this: Custom lightweight vector symbols for Upload, Cart, and Search.
@Composable
fun rememberUploadIcon(): ImageVector {
    return remember {
        ImageVector.Builder("Upload", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color(0xFF111111))) {
                moveTo(9f, 16f); horizontalLineToRelative(6f); verticalLineToRelative(-6f)
                horizontalLineToRelative(4f); lineToRelative(-7f, -7f); lineToRelative(-7f, 7f)
                horizontalLineToRelative(4f); verticalLineToRelative(6f); close()
                moveTo(5f, 18f); horizontalLineToRelative(14f); verticalLineToRelative(2f)
                horizontalLineTo(5f); verticalLineToRelative(-2f); close()
            }
        }.build()
    }
}

@Composable
fun rememberCartIcon(): ImageVector {
    return remember {
        ImageVector.Builder("Cart", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color(0xFF111111))) {
                moveTo(7f, 18f); curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                reflectiveCurveTo(5.9f, 22f, 7f, 22f); reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); close()
                moveTo(1f, 2f); verticalLineToRelative(2f); horizontalLineToRelative(2f)
                lineToRelative(3.6f, 7.59f); lineToRelative(-1.35f, 2.45f)
                curveToRelative(-0.16f, 0.28f, -0.25f, 0.61f, -0.25f, 0.96f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(12f)
                verticalLineToRelative(-2f); horizontalLineTo(7.42f)
                curveToRelative(-0.14f, 0f, -0.25f, -0.11f, -0.25f, -0.25f)
                lineToRelative(0.03f, -0.12f); lineToRelative(0.9f, -1.63f)
                horizontalLineToRelative(7.45f); curveToRelative(0.75f, 0f, 1.41f, -0.41f, 1.75f, -1.03f)
                lineToRelative(3.58f, -6.49f); curveToRelative(0.08f, -0.14f, 0.12f, -0.31f, 0.12f, -0.48f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f); horizontalLineTo(5.21f)
                lineToRelative(-0.94f, -2f); horizontalLineTo(1f); close()
                moveTo(17f, 18f); curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                reflectiveCurveToRelative(0.89f, 2f, 1.99f, 2f); reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); close()
            }
        }.build()
    }
}

@Composable
fun rememberSearchIcon(): ImageVector {
    return remember {
        ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color(0xFF555555))) {
                moveTo(15.5f, 14f); horizontalLineToRelative(-0.79f); lineToRelative(-0.28f, -0.27f)
                curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f); curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
                reflectiveCurveTo(3f, 5.91f, 3f, 9.5f); reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
                curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f); lineToRelative(0.27f, 0.28f)
                verticalLineToRelative(0.79f); lineToRelative(5f, 4.99f); lineTo(20.49f, 19f)
                lineToRelative(-4.99f, -5f); close()
                moveTo(9.5f, 14f); curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
                reflectiveCurveTo(7.01f, 5f, 9.5f, 5f); reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
                reflectiveCurveTo(11.99f, 14f, 9.5f, 14f); close()
            }
        }.build()
    }
}

@Composable
fun HyunWearsApp() {
    val context = LocalContext.current

    // Why we use this: Live lists for Firebase catalog, selected T-shirt IDs, and purchased items from Firebase Cart_Orders.
    val catalog = remember { mutableStateListOf<TShirt>() }
    val purchasedList = remember { mutableStateListOf<TShirt>() }
    val selectedIds = remember { mutableStateListOf<String>() }

    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Why we use this: Navigation states for View Page, Cart Page, and Upload Page.
    var viewingTShirt by remember { mutableStateOf<TShirt?>(null) }
    var showCartPage by remember { mutableStateOf(false) }
    var showUploadPage by remember { mutableStateOf(false) }
    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var enableRgbBorder by remember { mutableStateOf(false) }
    var loadRemainingImages by remember { mutableStateOf(false) }

    var dbInstance by remember { mutableStateOf<FirebaseFirestore?>(null) }
    var storageInstance by remember { mutableStateOf<FirebaseStorage?>(null) }

    // Why we use this: Makes the phone's Back button navigate properly from Cart, View, Upload, or Search back to Home.
    BackHandler(enabled = showCartPage || viewingTShirt != null || showUploadPage || searchQuery.isNotEmpty()) {
        when {
            showCartPage -> showCartPage = false
            viewingTShirt != null -> viewingTShirt = null
            showUploadPage && !isUploading -> {
                showUploadPage = false
                selectedGalleryUri = null
            }
            searchQuery.isNotEmpty() -> searchQuery = ""
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedGalleryUri = uri
            viewingTShirt = null
            showCartPage = false
            showUploadPage = true
        }
    }

    // Why we use this: Connects to Firebase Firestore and Storage in the background after the UI opens.
    LaunchedEffect(Unit) {
        delay(100)
        enableRgbBorder = true

        val (db, storage) = withContext(Dispatchers.IO) {
            FirebaseApp.initializeApp(context)
            Pair(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
        }
        dbInstance = db
        storageInstance = storage

        db.collection("T-shirts")
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
                        val material = doc.getString("Material") ?: "Cotton"
                        val colour = doc.getString("Colour") ?: "Green"
                        val fabric = doc.getString("Fabric") ?: "220 GSM"
                        catalog.add(
                            TShirt(
                                id = doc.id,
                                Name = name,
                                Price = price,
                                ImageUrl = imageUrl,
                                Material = material,
                                Colour = colour,
                                Fabric = fabric
                            )
                        )
                    }
                    isLoading = false
                }
            }

        // Why we use this: Listens to "Cart_Orders" in Firebase so the Purchased tab inside Cart Page shows live purchased items.
        db.collection("Cart_Orders")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    purchasedList.clear()
                    for (doc in snapshot.documents) {
                        val name = doc.getString("Name") ?: ""
                        val price = doc.getLong("Price")?.toInt() ?: 0
                        val imageUrl = doc.getString("ImageUrl") ?: ""
                        purchasedList.add(
                            TShirt(
                                id = doc.id,
                                Name = name,
                                Price = price,
                                ImageUrl = imageUrl
                            )
                        )
                    }
                }
            }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(800)
            loadRemainingImages = true
        }
    }

    val filteredList = remember(catalog.size, searchQuery) {
        catalog.filter { it.Name.contains(searchQuery, ignoreCase = true) }
    }

    // Why we use this: Computes the list of currently Selected T-shirts to show in the Cart Page.
    val selectedProducts = catalog.filter { selectedIds.contains(it.id) }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HyunWears",
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        fontSize = 21.sp,
                        color = Color(0xFF111111),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewingTShirt = null
                            showCartPage = false
                            if (!isUploading) showUploadPage = false
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Upload symbol button
                        AnimatedWhiteButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.size(36.dp),
                            cornerRadius = 9.dp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = rememberUploadIcon(),
                                contentDescription = "Upload T-Shirt",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Why we use this: Cart symbol button with a Blue Dot indicator whenever any T-shirt is Selected.
                        Box {
                            AnimatedWhiteButton(
                                onClick = {
                                    viewingTShirt = null
                                    if (!isUploading) showUploadPage = false
                                    showCartPage = !showCartPage
                                },
                                modifier = Modifier.size(36.dp),
                                cornerRadius = 9.dp,
                                isSelected = showCartPage,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = rememberCartIcon(),
                                    contentDescription = "Cart",
                                    tint = Color(0xFF111111),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Why we use this: Shows a blue dot on the Cart button when at least one T-shirt is selected.
                            if (selectedIds.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0066FF))
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            // 1. Upload Page
            showUploadPage && selectedGalleryUri != null && storageInstance != null && dbInstance != null -> {
                UploadTShirtScreen(
                    imageUri = selectedGalleryUri!!,
                    isUploading = isUploading,
                    modifier = Modifier.padding(innerPadding),
                    onCancel = {
                        if (!isUploading) {
                            showUploadPage = false
                            selectedGalleryUri = null
                        }
                    },
                    onUploadSubmit = { enteredName, enteredPrice, enteredMaterial, enteredColour, enteredFabric ->
                        isUploading = true
                        val fileName = "T-shirts/tee_${System.currentTimeMillis()}.jpg"
                        val storageRef = storageInstance!!.reference.child(fileName)

                        storageRef.putFile(selectedGalleryUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val newTeeData = mapOf(
                                        "Name" to enteredName,
                                        "Price" to enteredPrice,
                                        "ImageUrl" to downloadUri.toString(),
                                        "Material" to enteredMaterial,
                                        "Colour" to enteredColour,
                                        "Fabric" to enteredFabric
                                    )
                                    dbInstance!!.collection("T-shirts")
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
            }

            // 2. Cart Page (Opens when clicking the Cart button)
            showCartPage && storageInstance != null -> {
                CartScreen(
                    selectedProducts = selectedProducts,
                    purchasedProducts = purchasedList,
                    storage = storageInstance!!,
                    modifier = Modifier.padding(innerPadding),
                    onRemoveSelected = { idToRemove ->
                        selectedIds.remove(idToRemove)
                    },
                    onPurchaseClick = {
                        if (selectedProducts.isEmpty()) {
                            Toast.makeText(context, "No selected T-shirts in Cart", Toast.LENGTH_SHORT).show()
                        } else {
                            // Why we use this: Saves all Selected T-shirts in Cart into Firebase "Cart_Orders".
                            selectedProducts.forEach { tee ->
                                val orderData = mapOf(
                                    "TshirtId" to tee.id,
                                    "Name" to tee.Name,
                                    "Price" to tee.Price,
                                    "ImageUrl" to tee.ImageUrl,
                                    "Timestamp" to System.currentTimeMillis()
                                )
                                dbInstance?.collection("Cart_Orders")?.add(orderData)
                            }
                            selectedIds.clear()
                            Toast.makeText(context, "Purchase saved to Firebase Cart_Orders!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 3. Product Detail Page (Opens when clicking "View")
            viewingTShirt != null && storageInstance != null -> {
                val activeTee = viewingTShirt!!
                val isSelected = selectedIds.contains(activeTee.id)

                ProductDetailScreen(
                    tshirt = activeTee,
                    storage = storageInstance!!,
                    isSelected = isSelected,
                    modifier = Modifier.padding(innerPadding),
                    onSelectClick = {
                        if (isSelected) selectedIds.remove(activeTee.id) else selectedIds.add(activeTee.id)
                    },
                    onBuyClick = { chosenSize, chosenQty ->
                        val orderData = mapOf(
                            "TshirtId" to activeTee.id,
                            "Name" to activeTee.Name,
                            "Price" to activeTee.Price,
                            "Size" to chosenSize,
                            "Quantity" to chosenQty,
                            "ImageUrl" to activeTee.ImageUrl,
                            "Timestamp" to System.currentTimeMillis()
                        )
                        dbInstance?.collection("Cart_Orders")
                            ?.add(orderData)
                            ?.addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Purchased ${activeTee.Name} ($chosenSize x$chosenQty)!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                )
            }

            // 4. Home Store Catalog
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        RgbSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            enableRgbBorder = enableRgbBorder
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isLoading || storageInstance == null) {
                            items(2) {
                                YouTubeSkeletonCard()
                            }
                        } else {
                            itemsIndexed(filteredList, key = { _, item -> item.id }) { index, tshirt ->
                                val isSelected = selectedIds.contains(tshirt.id)
                                val shouldLoadImageNow = (index < 2) || loadRemainingImages

                                TShirtCard(
                                    tshirt = tshirt,
                                    storage = storageInstance!!,
                                    shouldLoadImage = shouldLoadImageNow,
                                    isSelected = isSelected,
                                    onViewClick = {
                                        viewingTShirt = tshirt
                                    },
                                    onBuyClick = {
                                        val orderData = mapOf(
                                            "TshirtId" to tshirt.id,
                                            "Name" to tshirt.Name,
                                            "Price" to tshirt.Price,
                                            "Size" to "M",
                                            "Quantity" to 1,
                                            "ImageUrl" to tshirt.ImageUrl,
                                            "Timestamp" to System.currentTimeMillis()
                                        )
                                        dbInstance?.collection("Cart_Orders")
                                            ?.add(orderData)
                                            ?.addOnSuccessListener {
                                                Toast.makeText(context, "Saved ${tshirt.Name} to Cart_Orders!", Toast.LENGTH_SHORT).show()
                                            }
                                            ?.addOnFailureListener { e ->
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
                            FooterSection()
                        }
                    }
                }
            }
        }
    }
}

// Why we use this: Cart Page showing Selected T-shirts properly with "Purchased" and "Purchase" buttons at the bottom.
@Composable
fun CartScreen(
    selectedProducts: List<TShirt>,
    purchasedProducts: List<TShirt>,
    storage: FirebaseStorage,
    modifier: Modifier = Modifier,
    onRemoveSelected: (String) -> Unit,
    onPurchaseClick: () -> Unit
) {
    // Why we use this: Allows toggling between Selected Cart items and already Purchased items when clicking "Purchased".
    var showPurchasedTab by remember { mutableStateOf(false) }

    // Why we use this: If viewing the Purchased list inside Cart, pressing Back returns to the Selected Cart view first.
    BackHandler(enabled = showPurchasedTab) {
        showPurchasedTab = false
    }

    val displayedItems = if (showPurchasedTab) purchasedProducts else selectedProducts

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (showPurchasedTab) "Purchased T-Shirts" else "Selected T-Shirts",
                fontFamily = NotoSerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color(0xFF111111),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showPurchasedTab) "No Purchased T-shirts yet" else "No Selected T-shirts in Cart",
                        fontFamily = NotoSerifFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayedItems, key = { it.id + (if (showPurchasedTab) "_p" else "_s") }) { tshirt ->
                        CartProductCard(
                            tshirt = tshirt,
                            storage = storage,
                            showRemoveButton = !showPurchasedTab,
                            onRemoveClick = { onRemoveSelected(tshirt.id) }
                        )
                    }
                }
            }
        }

        // Why we use this: Bottom row inside Cart Page containing the "Purchased" and "Purchase" buttons.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedWhiteButton(
                onClick = { showPurchasedTab = !showPurchasedTab },
                modifier = Modifier.weight(1f),
                cornerRadius = 10.dp,
                isSelected = showPurchasedTab,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Purchased",
                    fontFamily = NotoSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111)
                )
            }

            AnimatedWhiteButton(
                onClick = {
                    showPurchasedTab = false
                    onPurchaseClick()
                },
                modifier = Modifier.weight(1f),
                cornerRadius = 10.dp,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Purchase",
                    fontFamily = NotoSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111)
                )
            }
        }
    }
}

// Why we use this: Displays each selected T-shirt in the Cart Page with a 1024x1024 full-bleed image above Name and Price.
@Composable
fun CartProductCard(
    tshirt: TShirt,
    storage: FirebaseStorage,
    showRemoveButton: Boolean,
    onRemoveClick: () -> Unit
) {
    val context = LocalContext.current
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Why we use this: 1024x1024 (1:1 ratio) image spread edge-to-edge touching the card borders above Name and Price.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF2F2F2))
            ) {
                if (resolvedDownloadUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedDownloadUrl)
                            .size(1024, 1024)
                            .build(),
                        contentDescription = tshirt.Name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tshirt.Name,
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )
                    Text(
                        text = "${tshirt.Price}",
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )
                }

                if (showRemoveButton) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedWhiteButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(vertical = 9.dp)
                    ) {
                        Text(
                            text = "Selected",
                            fontFamily = NotoSerifFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                    }
                }
            }
        }
    }
}

// Why we use this: Product Detail Page with 1024x1024 full-bleed 5-image carousel, Name & Price, Size, Quantity, thin separator line, and T-shirt specs.
@Composable
fun ProductDetailScreen(
    tshirt: TShirt,
    storage: FirebaseStorage,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelectClick: () -> Unit,
    onBuyClick: (String, Int) -> Unit
) {
    val context = LocalContext.current
    var resolvedDownloadUrl by remember(tshirt.ImageUrl) { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf("M") }
    var quantity by remember { mutableIntStateOf(1) }
    val sizes = remember { listOf("S", "M", "L", "XL", "XXL") }

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

    // Why we use this: Auto-moves horizontally across 5 images every 2.5 seconds.
    val pagerState = rememberPagerState(pageCount = { 5 })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(2500)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % 5
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Why we use this: Card where the 1024x1024 (1:1) 5-image carousel spreads all the way to the card borders above Name & Price.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color(0xFFF2F2F2))
                    ) { page ->
                        val zoomScale = when (page) {
                            0 -> 1.0f
                            1 -> 1.15f
                            2 -> 1.25f
                            3 -> 1.18f
                            else -> 1.08f
                        }
                        val pivotOrigin = when (page) {
                            0 -> TransformOrigin(0.5f, 0.5f)
                            1 -> TransformOrigin(0.5f, 0.25f)
                            2 -> TransformOrigin(0.5f, 0.5f)
                            3 -> TransformOrigin(0.5f, 0.8f)
                            else -> TransformOrigin(0.35f, 0.45f)
                        }

                        if (resolvedDownloadUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(resolvedDownloadUrl)
                                    .size(1024, 1024)
                                    .build(),
                                contentDescription = "${tshirt.Name} view ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        transformOrigin = pivotOrigin
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(rememberYouTubeShimmerBrush())
                            )
                        }
                    }

                    // Why we use this: Details section starting immediately below the full-bleed 1024x1024 image.
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Pager indicator dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { index ->
                                val isCurrent = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(if (isCurrent) 18.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) Color(0xFF111111) else Color(0xFFD0D0D0))
                                )
                            }
                        }

                        // Left side T-shirt Name and Right side Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tshirt.Name,
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF111111)
                            )
                            Text(
                                text = "${tshirt.Price}",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF111111)
                            )
                        }

                        // Size buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Size",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sizes.forEach { sizeOption ->
                                    AnimatedWhiteButton(
                                        onClick = { selectedSize = sizeOption },
                                        modifier = Modifier.weight(1f),
                                        cornerRadius = 10.dp,
                                        isSelected = (selectedSize == sizeOption),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = sizeOption,
                                            fontFamily = NotoSerifFontFamily,
                                            fontWeight = if (selectedSize == sizeOption) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111111)
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity plus minus buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quantity",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedWhiteButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(36.dp),
                                    cornerRadius = 9.dp,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "-",
                                        fontFamily = NotoSerifFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF111111)
                                    )
                                }

                                Text(
                                    text = "$quantity",
                                    fontFamily = NotoSerifFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF111111)
                                )

                                AnimatedWhiteButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(36.dp),
                                    cornerRadius = 9.dp,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "+",
                                        fontFamily = NotoSerifFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF111111)
                                    )
                                }
                            }
                        }

                        // Why we use this: Thin horizontal separator line below Quantity before T-shirt specifications.
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.8.dp,
                            color = Color(0xFFDDDDDD)
                        )

                        // Why we use this: Displays Material, Colour, and Fabric specifications below the thin separator line.
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Material : ${tshirt.Material}",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                            Text(
                                text = "Colour : ${tshirt.Colour}",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                            Text(
                                text = "Fabric : ${tshirt.Fabric}",
                                fontFamily = NotoSerifFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                        }
                    }
                }
            }
        }

        // Bottom side Select and Buy buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedWhiteButton(
                onClick = onSelectClick,
                modifier = Modifier.weight(1f),
                cornerRadius = 10.dp,
                isSelected = isSelected,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = if (isSelected) "Selected" else "Select",
                    fontFamily = NotoSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111)
                )
            }

            AnimatedWhiteButton(
                onClick = { onBuyClick(selectedSize, quantity) },
                modifier = Modifier.weight(1f),
                cornerRadius = 10.dp,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Buy",
                    fontFamily = NotoSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111)
                )
            }
        }
    }
}

@Composable
fun rememberYouTubeShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFEEEEEE),
            Color(0xFFF9F9F9),
            Color(0xFFEEEEEE)
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}

// Why we use this: YouTube-style loading skeleton card matching the 1024x1024 full-bleed card layout.
@Composable
fun YouTubeSkeletonCard() {
    val shimmerBrush = rememberYouTubeShimmerBrush()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(shimmerBrush)
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RgbSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    enableRgbBorder: Boolean
) {
    val borderModifier = if (enableRgbBorder) {
        val rgbGradient = remember {
            Brush.linearGradient(
                colors = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
            )
        }
        Modifier.border(width = 1.2.dp, brush = rgbGradient, shape = RoundedCornerShape(12.dp))
    } else {
        Modifier.border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = rememberSearchIcon(),
                contentDescription = "Search",
                tint = Color(0xFF555555),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = NotoSerifFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF111111)
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search T-shirts...",
                            fontFamily = NotoSerifFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

// Why we use this: T-Shirt Card where the 1024x1024 (1:1) image spreads edge-to-edge touching the card borders above Name & Price.
@Composable
fun TShirtCard(
    tshirt: TShirt,
    storage: FirebaseStorage,
    shouldLoadImage: Boolean,
    isSelected: Boolean,
    onViewClick: () -> Unit,
    onBuyClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    val context = LocalContext.current
    var resolvedDownloadUrl by remember(tshirt.ImageUrl) { mutableStateOf("") }
    var isImageRendered by remember(tshirt.ImageUrl) { mutableStateOf(false) }

    LaunchedEffect(tshirt.ImageUrl, shouldLoadImage) {
        if (!shouldLoadImage) return@LaunchedEffect
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Why we use this: 1024x1024 (1:1 square) image with zero outer padding so it touches the top, left, and right card borders.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (!isImageRendered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(rememberYouTubeShimmerBrush())
                    )
                }

                if (shouldLoadImage && resolvedDownloadUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedDownloadUrl)
                            .size(1024, 1024)
                            .build(),
                        contentDescription = tshirt.Name,
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageRendered = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Why we use this: Padding starts below the full-bleed image for Name, Price, and Buttons.
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tshirt.Name,
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )
                    Text(
                        text = "${tshirt.Price}",
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedWhiteButton(
                        onClick = onViewClick,
                        modifier = Modifier.weight(1f),
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = "View",
                            fontFamily = NotoSerifFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                    }

                    AnimatedWhiteButton(
                        onClick = onBuyClick,
                        modifier = Modifier.weight(1f),
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Buy",
                            fontFamily = NotoSerifFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                    }

                    AnimatedWhiteButton(
                        onClick = onSelectClick,
                        modifier = Modifier.weight(1f),
                        cornerRadius = 10.dp,
                        isSelected = isSelected,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isSelected) "Selected" else "Select",
                            fontFamily = NotoSerifFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadTShirtScreen(
    imageUri: Uri,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onUploadSubmit: (String, Int, String, String, String) -> Unit
) {
    var tshirtName by remember { mutableStateOf("") }
    var tshirtPriceText by remember { mutableStateOf("") }
    var tshirtMaterial by remember { mutableStateOf("Cotton") }
    var tshirtColour by remember { mutableStateOf("Green") }
    var tshirtFabric by remember { mutableStateOf("220 GSM") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Upload T-Shirt",
            fontFamily = NotoSerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF111111)
        )

        // Why we use this: 1024x1024 (1:1 square) preview of the selected gallery image.
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .size(1024, 1024)
                .build(),
            contentDescription = "Selected Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F2))
        )

        OutlinedTextField(
            value = tshirtName,
            onValueChange = { tshirtName = it },
            label = { Text("Name", fontFamily = NotoSerifFontFamily) },
            textStyle = TextStyle(fontFamily = NotoSerifFontFamily, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = tshirtPriceText,
            onValueChange = { tshirtPriceText = it.filter { char -> char.isDigit() } },
            label = { Text("Price", fontFamily = NotoSerifFontFamily) },
            textStyle = TextStyle(fontFamily = NotoSerifFontFamily, fontSize = 15.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = tshirtMaterial,
            onValueChange = { tshirtMaterial = it },
            label = { Text("Material", fontFamily = NotoSerifFontFamily) },
            textStyle = TextStyle(fontFamily = NotoSerifFontFamily, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = tshirtColour,
            onValueChange = { tshirtColour = it },
            label = { Text("Colour", fontFamily = NotoSerifFontFamily) },
            textStyle = TextStyle(fontFamily = NotoSerifFontFamily, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = tshirtFabric,
            onValueChange = { tshirtFabric = it },
            label = { Text("Fabric", fontFamily = NotoSerifFontFamily) },
            textStyle = TextStyle(fontFamily = NotoSerifFontFamily, fontSize = 15.sp),
            singleLine = true,
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
                AnimatedWhiteButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    cornerRadius = 10.dp,
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }

                AnimatedWhiteButton(
                    onClick = {
                        val priceVal = tshirtPriceText.toIntOrNull()
                        if (tshirtName.isBlank() || priceVal == null) {
                            Toast.makeText(context, "Enter Name and Price", Toast.LENGTH_SHORT).show()
                        } else {
                            onUploadSubmit(
                                tshirtName.trim(),
                                priceVal,
                                tshirtMaterial.trim().ifEmpty { "Cotton" },
                                tshirtColour.trim().ifEmpty { "Green" },
                                tshirtFabric.trim().ifEmpty { "220 GSM" }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    cornerRadius = 10.dp,
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = "Upload",
                        fontFamily = NotoSerifFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }
            }
        }
    }
}

@Composable
fun FooterSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HyunWears by PiCell Company.",
            fontFamily = NotoSerifFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF111111)
        )
    }
}
