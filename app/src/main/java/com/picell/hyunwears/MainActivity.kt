package com.picell.hyunwears

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Why we use this: Draws the Frontend UI immediately on launch before Firebase even starts.
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFFAFAFA),
                    surface = Color.White,
                    onBackground = Color(0xFF111111),
                    onSurface = Color(0xFF111111),
                    primary = Color(0xFF111111)
                )
            ) {
                HyunWearsMainEntry()
            }
        }
    }
}

// Why we use this: Custom lightweight Upload Icon so we don't need the 15MB material-icons-extended library.
@Composable
fun rememberUploadIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "UploadIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF111111))) {
                moveTo(9f, 16f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(4f)
                lineToRelative(-7f, -7f)
                lineToRelative(-7f, 7f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(6f)
                close()
                moveTo(5f, 18f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                verticalLineToRelative(-2f)
                close()
            }
        }.build()
    }
}

// Why we use this: Plays the 2-note harmonic opening chime on a background thread as soon as the app opens.
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

// Why we use this: Displays the Frontend and Opening Animation immediately (< 1 second), then signals Firebase to start.
@Composable
fun HyunWearsMainEntry() {
    var showSplashAnimation by remember { mutableStateOf(true) }
    var startBackendNow by remember { mutableStateOf(false) }
    var isAppFullyOpened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Step 1: Play opening sound and show opening animation immediately.
        playOpeningSound()
        delay(950)
        // Step 2: Fade out opening screen to reveal the Frontend & YouTube Shimmer Cards.
        showSplashAnimation = false
        delay(200)
        // Step 3: Now that Frontend is visible on screen, start Firebase Backend and RGB Search Bar!
        startBackendNow = true
        isAppFullyOpened = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HyunWearsApp(
            startBackendNow = startBackendNow,
            isAppFullyOpened = isAppFullyOpened
        )

        AnimatedVisibility(
            visible = showSplashAnimation,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            HyunWearsOpeningScreen()
        }
    }
}

// Why we use this: Light-mode opening animation scaling up "HyunWears" in Georgia font.
@Composable
fun HyunWearsOpeningScreen() {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1.0f else 0.82f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 550),
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
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = Color(0xFF111111),
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        )
    }
}

@Composable
fun HyunWearsApp(
    startBackendNow: Boolean,
    isAppFullyOpened: Boolean
) {
    val robotoFont = FontFamily.SansSerif
    val georgiaFont = FontFamily.Serif
    val context = LocalContext.current

    val catalog = remember { mutableStateListOf<TShirt>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var cartCount by remember { mutableIntStateOf(0) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Why we use this: Controls loading the first 2 images first, then the remaining images afterward.
    var loadRemainingImages by remember { mutableStateOf(false) }

    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadPage by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    var dbInstance by remember { mutableStateOf<FirebaseFirestore?>(null) }
    var storageInstance by remember { mutableStateOf<FirebaseStorage?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedGalleryUri = uri
            showUploadPage = true
        }
    }

    // Why we use this: Initializes Firebase on a background thread ONLY AFTER the Frontend UI is already open!
    LaunchedEffect(startBackendNow) {
        if (!startBackendNow) return@LaunchedEffect

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

    // Why we use this: Loads the remaining T-shirt images 800ms after the first 2 images start loading.
    LaunchedEffect(isLoading, isAppFullyOpened) {
        if (!isLoading && isAppFullyOpened) {
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Why we use this: Upload Icon button next to Cart that opens your phone's gallery.
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
                                imageVector = rememberUploadIcon(),
                                contentDescription = "Upload T-Shirt",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(20.dp)
                            )
                        }

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
        if (showUploadPage && selectedGalleryUri != null && storageInstance != null && dbInstance != null) {
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
                        enableRgbBorder = isAppFullyOpened,
                        robotoFont = robotoFont
                    )
                }

                if (isLoading || storageInstance == null) {
                    // Why we use this: Shows YouTube-style Shimmer Loading Cards immediately while Firebase connects in background.
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
                            robotoFont = robotoFont,
                            onViewClick = {
                                Toast.makeText(context, "Viewing ${tshirt.Name}", Toast.LENGTH_SHORT).show()
                            },
                            onBuyClick = {
                                val orderData = mapOf(
                                    "TshirtId" to tshirt.id,
                                    "Name" to tshirt.Name,
                                    "Price" to tshirt.Price,
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
                    FooterSection(robotoFont = robotoFont)
                }
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
    enableRgbBorder: Boolean,
    robotoFont: FontFamily
) {
    val borderModifier = if (enableRgbBorder) {
        val rgbGradient = remember {
            Brush.linearGradient(
                colors = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
            )
        }
        Modifier.border(width = 1.5.dp, brush = rgbGradient, shape = RoundedCornerShape(12.dp))
    } else {
        Modifier.border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .then(borderModifier)
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
    shouldLoadImage: Boolean,
    isSelected: Boolean,
    robotoFont: FontFamily,
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
            label = { Text("Name", fontFamily = robotoFont) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

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
