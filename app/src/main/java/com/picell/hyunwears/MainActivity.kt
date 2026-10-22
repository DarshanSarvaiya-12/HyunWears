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
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.sin

// Why we use this: Blueprint for each T-shirt matching our capitalized Firebase fields (Name, Price, ImageUrl).
data class TShirt(
    val id: String = "",
    val Name: String = "",
    val Price: Int = 0,
    val ImageUrl: String = ""
)

// Why we use this: FontFamily.Serif maps directly to Android's built-in NotoSerif-Regular.ttf and NotoSerif-Bold.ttf files.
val NotoSerifFontFamily = FontFamily.Serif

// Why we use this: Applies Noto Serif as the global default font across the entire app so the phone's default sans-serif never appears.
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
        // Why we use this: Renders the light-mode theme with global Noto Serif typography immediately on launch.
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

// Why we use this: Reusable white button with a thin black border that animates into a thicker dark border when clicked or selected.
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

    // Why we use this: Keeps the thicker dark border visible for 220ms on tap or permanently while selected.
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

// Why we use this: Separates the Opening Screen from the Store UI so zero store code runs during cold launch.
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

// Why we use this: Instant opening animation scaling up the slightly bold "HyunWears" Georgia-style serif logo.
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

// Why we use this: Custom lightweight vector symbols for Upload, Cart, and Search so APK stays under 2 MB.
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

    val catalog = remember { mutableStateListOf<TShirt>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var cartCount by remember { mutableIntStateOf(0) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Why we use this: Holds the currently viewed T-shirt when the user clicks the "View" button.
    var viewingTShirt by remember { mutableStateOf<TShirt?>(null) }

    // Why we use this: Defers the RGB border and remaining images until after the store screen is visible.
    var enableRgbBorder by remember { mutableStateOf(false) }
    var loadRemainingImages by remember { mutableStateOf(false) }

    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadPage by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    var dbInstance by remember { mutableStateOf<FirebaseFirestore?>(null) }
    var storageInstance by remember { mutableStateOf<FirebaseStorage?>(null) }

    // Why we use this: Makes the phone's hardware/gesture Back button work properly across View page, Upload page, and Search!
    BackHandler(enabled = viewingTShirt != null || showUploadPage || searchQuery.isNotEmpty()) {
        when {
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
            showUploadPage = true
        }
    }

    // Why we use this: Draws the YouTube Shimmer Cards first, then starts Firebase Backend and RGB Search Bar in the background.
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

        db.collection("Cart_Orders")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    cartCount = snapshot.size()
                }
            }
    }

    // Why we use this: Loads the first 2 images immediately, then loads the rest 800ms later.
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(800)
            loadRemainingImages = true
        }
    }

    val filteredList = remember(catalog.size, searchQuery) {
        catalog.filter { it.Name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            // Why we use this: Compact Header with slightly bold Georgia-style HyunWears logo on left and equal symbol buttons on right.
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Why we use this: Clicking the HyunWears logo also returns you home from View or Upload screens.
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
                            if (!isUploading) showUploadPage = false
                        }
                    )

                    // Why we use this: Small, identical-sized (36dp x 36dp) symbol-only Upload and Cart buttons with animated borders.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                        AnimatedWhiteButton(
                            onClick = {
                                Toast.makeText(context, "Cart_Orders items: $cartCount", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp),
                            cornerRadius = 9.dp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = rememberCartIcon(),
                                contentDescription = "Cart",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            // Why we use this: Opens the Upload Page when the user selects a photo from their gallery.
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
                    onUploadSubmit = { enteredName, enteredPrice ->
                        isUploading = true
                        val fileName = "T-shirts/tee_${System.currentTimeMillis()}.jpg"
                        val storageRef = storageInstance!!.reference.child(fileName)

                        storageRef.putFile(selectedGalleryUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val newTeeData = mapOf(
                                        "Name" to enteredName,
                                        "Price" to enteredPrice,
                                        "ImageUrl" to downloadUri.toString()
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

            // Why we use this: Opens the Product Detail Page when the user clicks "View" on any T-shirt card.
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
                                    "Added ${activeTee.Name} ($chosenSize x$chosenQty) to Cart_Orders!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                )
            }

            // Why we use this: Default Home Catalog showing the RGB Search Bar and T-shirt Cards.
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

// Why we use this: Product Detail Page opened when clicking "View" — shows 5 auto-scrolling images, Name & Price, Sizes, Quantity +/-, and bottom Select & Buy buttons.
@Composable
fun ProductDetailScreen(
    tshirt: TShirt,
    storage: FirebaseStorage,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelectClick: () -> Unit,
    onBuyClick: (String, Int) -> Unit
) {
    var resolvedDownloadUrl by remember(tshirt.ImageUrl) { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf("M") }
    var quantity by remember { mutableIntStateOf(1) }
    val sizes = remember { listOf("S", "M", "L", "XL", "XXL") }

    // Why we use this: Resolves the Firebase Storage image URL for the 5-image horizontal pager.
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

    // Why we use this: Controls the 5-image horizontal carousel and auto-moves horizontally every 2.5 seconds.
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Why we use this: Card container for the 5 auto-scrolling horizontal product images.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F2F2))
                    ) { page ->
                        // Why we use this: Gives each of the 5 slides a distinct framing angle (full view, upper detail, center weave, lower hem, close-up) if using 1 source image.
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
                                model = resolvedDownloadUrl,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Why we use this: 5 indicator dots showing the active image in the auto-scrolling 5-image carousel.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val isCurrent = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (isCurrent) 18.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) Color(0xFF111111) else Color(0xFFD0D0D0))
                            )
                        }
                    }
                }
            }

            // Why we use this: Below the image — Left side T-shirt Name and Right side its primary Price.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
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

            // Why we use this: Size selector buttons (S, M, L, XL, XXL) in white with animated black borders.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Size",
                    fontFamily = NotoSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111),
                    modifier = Modifier.padding(horizontal = 4.dp)
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

            // Why we use this: Quantity plus (+) and minus (-) buttons with animated black borders.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
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
                        modifier = Modifier.size(38.dp),
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
                        modifier = Modifier.size(38.dp),
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
        }

        // Why we use this: Bottom side Select and Buy buttons in curved-corner white rectangles with animated black borders.
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
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(14.dp))

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
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        model = resolvedDownloadUrl,
                        contentDescription = tshirt.Name,
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageRendered = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Why we use this: Shows T-shirt Name on the left and primary Price on the right in Noto Serif font.
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

            // Why we use this: Bottom row with View, Buy, and Select buttons styled in white with animated black borders.
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

@Composable
fun UploadTShirtScreen(
    imageUri: Uri,
    isUploading: Boolean,
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
            fontFamily = NotoSerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF111111)
        )

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
                            onUploadSubmit(tshirtName.trim(), priceVal)
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
