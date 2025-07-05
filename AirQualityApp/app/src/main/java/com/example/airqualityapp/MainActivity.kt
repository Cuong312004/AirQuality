package com.example.airqualityapp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.DateRange
import androidx.compose.material.icons.twotone.Thermostat
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.airqualityapp.ui.theme.AirQualityAppTheme
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.airqualityapp.AlertManager.alerts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale
import java.time.format.TextStyle as JavaTimeTextStyle

object LocationManager {
    // Vị trí mặc định (Thủ Đức)
    private val defaultLocation = Location(
        id = "thu_duc",
        name = "Thủ Đức",
        displayName = "Thủ Đức, Hồ Chí Minh"
    )

    // Danh sách vị trí có thể chọn (có thể quan sát được)
    private var _availableLocations = mutableStateListOf<Location>(defaultLocation)
    val availableLocations: List<Location> get() = _availableLocations

    // Vị trí hiện tại được chọn (mutableStateOf để có thể theo dõi thay đổi)
    private var _currentLocation = mutableStateOf(defaultLocation)
    val currentLocation: State<Location> get() = _currentLocation

    // Hàm để đặt vị trí hiện tại
    fun setCurrentLocation(location: Location) {
        _currentLocation.value = location
        Log.d("LocationManager", "Location changed to: ${location.displayName}")
    }
    fun Location.withDefaultValues(): Location {
        return if (this.id == "thu_duc") {
            this.copy(
                name = "Thủ Đức",
                displayName = "Thủ Đức, Hồ Chí Minh"
            )
        } else {
            this  // Giữ nguyên nếu không phải "thu_duc"
        }
    }
    // Hàm để lấy danh sách vị trí từ cloud
    suspend fun fetchLocationsFromCloud() {
        try {
            Log.d("LocationManager", "Fetching available locations from cloud")
            val locations = RetrofitClient.apiService.getAvailableLocations()

            _availableLocations.clear()
            _availableLocations.addAll(locations.map { it.withDefaultValues() })

            // Nếu vị trí hiện tại không có trong danh sách, đặt về vị trí mặc định
            if (_availableLocations.none { it.id == _currentLocation.value.id }) {
                _currentLocation.value = defaultLocation
            }

            Log.d("LocationManager", "Fetched ${locations.size} locations")
        } catch (e: Exception) {
            Log.e("LocationManager", "Error fetching locations", e)
            // Nếu lấy thất bại, đảm bảo có ít nhất vị trí mặc định
            if (_availableLocations.isEmpty()) {
                _availableLocations.add(defaultLocation)
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color(0xFF6BCBFF).toArgb()

        // Khởi tạo dữ liệu vị trí khi app khởi động
        lifecycleScope.launch {
            LocationManager.fetchLocationsFromCloud()
        }
        setContent {
            AirQualityAppTheme {
                AppNavigation()
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        //0xFFFF7043
        bottomBar = { EnhancedBottomNavigationBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { AnimatedScreen { MainScreen(navController) } }
            composable("alerts") { AnimatedScreen { AlertListScreen(alerts = AlertManager.alerts) } }
            composable("daterange") { AnimatedScreen { DateRangeScreen() } }
        }
    }
}

@Composable
fun EnhancedBottomNavigationBar(navController: NavController) {
    val screens = listOf("alerts", "home", "daterange")
    val icons = listOf(Icons.TwoTone.Notifications, Icons.TwoTone.Home, Icons.TwoTone.DateRange)
    val labels = listOf("Alerts", "Home", "Calendar")
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Create gradient background
    val gradientColors = listOf(Color(0xFF66E0FF), Color(0xFF004E92))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Main navigation bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .align(Alignment.Center),
            color = Color.White.copy(alpha = 0.15f),
            shadowElevation = 0.dp
        ) {
            // Add blur effect with Box and background filter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEachIndexed { index, route ->
                        val isSelected = currentRoute == route

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    onClick = { navController.navigate(route) }
                                    // Using the default Material 3 ripple effect instead of custom rememberRipple
                                )
                        ) {
                            // If selected, show indicator on top
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color.White)
                                        .padding(bottom = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Icon with animation
                            Icon(
                                imageVector = icons[index],
                                contentDescription = labels[index],
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(if (isSelected) 28.dp else 24.dp)
                                    .scale(animateFloatAsState(if (isSelected) 1.2f else 1f).value)
                            )

                            // Label text
                            Text(
                                text = labels[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Center floating action button for home (optional enhancement)
        if (currentRoute == "home") {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-0).dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF66E0FF), Color(0xFF004E92)),
                            radius = 60f
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .shadow(8.dp, CircleShape)
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Home,
                    contentDescription = "Home",
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedScreen(content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = content,
        transitionSpec = {
            (scaleIn(animationSpec = tween(300)) + fadeIn()) togetherWith
                    (scaleOut(animationSpec = tween(300)) + fadeOut())
        },
        label = "ScreenTransition"
    ) { targetContent ->
        targetContent()
    }
}


@Composable
fun MainScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var latestData by remember { mutableStateOf<AirQualityData?>(null) }
    var rawData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var humidityData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var pm25Data by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var pm10Data by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var no2Data by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var so2Data by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var coData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }

    // Theo dõi vị trí hiện tại
    val currentLocation by LocationManager.currentLocation

    // Hàm để lấy dữ liệu cho vị trí hiện tại
    val fetchData = {
        scope.launch {
            try {
                Log.d("API_DEBUG", "Đang gọi API cho vị trí: ${currentLocation.id}")

                // Lấy dữ liệu dự đoán với tham số vị trí
                val predictions = RetrofitClient.apiService.getLatest12AirQualityPredict(currentLocation.id)
                rawData = predictions.map {
                    val timestamp = it[0] as String
                    val temperature = (it[2] as Double).toFloat()
                    timestamp to temperature
                }

                // Lấy tất cả thông số với tham số vị trí
                val allParametersResponse = RetrofitClient.apiService.getLatest12AllParameters(currentLocation.id)

                humidityData = allParametersResponse.humidity.map {
                    val timestamp = it[0] as String
                    val humidity = (it[2] as Double).toFloat()
                    timestamp to humidity
                }

                pm25Data = allParametersResponse.pm25.map {
                    val timestamp = it[0] as String
                    val pm25 = (it[2] as Double).toFloat()
                    timestamp to pm25
                }

                pm10Data = allParametersResponse.pm10.map {
                    val timestamp = it[0] as String
                    val pm10 = (it[2] as Double).toFloat()
                    timestamp to pm10
                }

                no2Data = allParametersResponse.no2.map {
                    val timestamp = it[0] as String
                    val no2 = (it[2] as Double).toFloat()
                    timestamp to no2
                }

                so2Data = allParametersResponse.so2.map {
                    val timestamp = it[0] as String
                    val so2 = (it[2] as Double).toFloat()
                    timestamp to so2
                }

                coData = allParametersResponse.co.map {
                    val timestamp = it[0] as String
                    val co = (it[2] as Double).toFloat()
                    timestamp to co
                }

                // Lấy dữ liệu mới nhất với tham số vị trí
                val latest = RetrofitClient.apiService.getLatestAirQualityData(currentLocation.id)
                latestData = if (latest.isNotEmpty()) {
                    val data = latest[0]
                    AirQualityData(
                        timestamp = (data[0] as? String) ?: "",
                        location = currentLocation.id,
                        temperature = (data[2] as Double),
                        humidity = (data[3] as Double),
                        pm25 = (data[4] as Double),
                        pm10 = (data[5] as Double),
                        no2 = (data[6] as Double),
                        so2 = (data[7] as Double),
                        co = (data[8] as Double),
                        air_quality = (data[9] as Double)
                    )
                } else {
                    null
                }

                AlertManager.addAlert("Thông báo: Đã cập nhật dữ liệu mới", currentLocation.displayName)
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Lỗi khi lấy dữ liệu", e)
                e.printStackTrace()
            }
        }
    }

    // Lấy dữ liệu khi vị trí thay đổi
    LaunchedEffect(currentLocation) {
        fetchData()

        // Thiết lập cập nhật định kỳ
        while (true) {
            delay(300000) // Cập nhật mỗi 5 phút
            fetchData()
        }
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // Chuyển đổi dữ liệu sang định dạng LocalDateTime cho tất cả các tham số
    val temperatureData = rawData.map { (timeString, temp) ->
        LocalDateTime.parse(timeString, formatter) to temp
    }

    val humidityChartData = humidityData.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    val pm25ChartData = pm25Data.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    val pm10ChartData = pm10Data.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    val no2ChartData = no2Data.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    val so2ChartData = so2Data.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    val coChartData = coData.map { (timeString, value) ->
        LocalDateTime.parse(timeString, formatter) to value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6BCBFF))
                .padding(16.dp)
        ) {
            HeaderSection()
        }

        // Tabs + Chỉ số AQI
        Box {
            Image(
                painter = painterResource(id = R.drawable.back1),
                contentDescription = "Box Background",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6BCBFF).copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                AQISectionWithTabs(latestData)
            }
        }

        // Các biểu đồ tham số
        ChartSection(
            title = "Temperature (°C)",
            color = Color(0xFFFF9F6B),
            data = temperatureData,
            unit = "°C"
        )

        ChartSection(
            title = "Humidity (%)",
            color = Color(0xFF4CAF50),
            data = humidityChartData,
            unit = "%"
        )

        ChartSection(
            title = "PM2.5 (µg/m³)",
            color = Color(0xFF9C27B0),
            data = pm25ChartData,
            unit = "µg/m³"
        )

        ChartSection(
            title = "PM10 (µg/m³)",
            color = Color(0xFF2196F3),
            data = pm10ChartData,
            unit = "µg/m³"
        )

        ChartSection(
            title = "NO2 (ppm)",
            color = Color(0xFFE91E63),
            data = no2ChartData,
            unit = "µg/m³"
        )

        ChartSection(
            title = "SO2 (ppm)",
            color = Color(0xFFFF5722),
            data = so2ChartData,
            unit = "µg/m³"
        )

        ChartSection(
            title = "CO (ppm)",
            color = Color(0xFF607D8B),
            data = coChartData,
            unit = "µg/m³"
        )

        // Thêm khoảng trống ở cuối để các phần tử không bị cắt
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChartSection(
    title: String,
    color: Color,
    data: List<Pair<LocalDateTime, Float>>,
    unit: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(16.dp))
            ParameterChart(
                chartData = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                color = color,
                unit = unit
            )
        }
    }
}

@Composable
fun ParameterChart(
    chartData: List<Pair<LocalDateTime, Float>>,
    modifier: Modifier = Modifier,
    color: Color,
    unit: String
) {
    val animatedProgress = remember { Animatable(0f) }

    // Animation khi load
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    if (chartData.isEmpty()) return

    // Tính giá trị min và max từ dữ liệu
    var minValue = chartData.minOf { it.second }
    var maxValue = chartData.maxOf { it.second }

    // Nếu max - min < 3, điều chỉnh giá trị trục Y sao cho có ít nhất 3 khoảng
    if (maxValue - minValue < 3) {
        val diff = 3 - (maxValue - minValue)
        minValue -= diff / 2
        maxValue += diff / 2
    }

    // Đảm bảo min và max là các số nguyên
    minValue = (minValue.toInt() / 1).toFloat()
    maxValue = (maxValue.toInt() / 1).toFloat()

    // Tính toán thời gian min và max để xác định phạm vi trục X
    val minTime = chartData.minOf { it.first }
    val maxTime = chartData.maxOf { it.first }
    val totalDuration = ChronoUnit.MILLIS.between(minTime, maxTime).toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height
        val paddingLeft = 40f  // Chừa không gian cho trục Y

        // Vẽ trục Y
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, chartHeight),
            strokeWidth = 2.dp.toPx()
        )

        // Vẽ trục X
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, chartHeight - 10f),
            end = Offset(chartWidth, chartHeight - 10f),
            strokeWidth = 2.dp.toPx(),
        )

        // Vẽ nhãn trên trục Y
        val yLabels = generateYLabels(minValue, maxValue)
        yLabels.forEach { value ->
            val yPos = chartHeight - ((value - minValue) / (maxValue - minValue) * chartHeight) - 5f
            drawContext.canvas.nativeCanvas.drawText(
                "%.1f".format(value),
                paddingLeft - 35f,
                yPos,
                android.graphics.Paint().apply {
                    textSize = 24f
                }
            )
        }

        // Thêm đơn vị đo trên đầu trục Y
        drawContext.canvas.nativeCanvas.drawText(
            unit,
            paddingLeft - 35f,
            20f,
            android.graphics.Paint().apply {

                textSize = 24f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        )

        // Tạo nhãn động cho trục X
        val timeLabels = chartData.map { it.first }.distinct().sorted()
        val timeStep = (timeLabels.size / 3).coerceAtLeast(1)

        timeLabels.forEachIndexed { index, time ->
            if (index % timeStep == 0) {
                val xPos = paddingLeft + (ChronoUnit.MILLIS.between(minTime, time) / totalDuration * (chartWidth - paddingLeft))
                val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                drawContext.canvas.nativeCanvas.drawText(
                    formattedTime,
                    xPos - 15f,
                    chartHeight + 15f,
                    android.graphics.Paint().apply {
                        textSize = 24f
                    }
                )
            }
        }

        // Chuyển dữ liệu thành danh sách điểm cho Line Chart
        val points = chartData.map { (time, value) ->
            Offset(
                x = paddingLeft + (ChronoUnit.MILLIS.between(minTime, time) / totalDuration * (chartWidth - paddingLeft)),
                y = chartHeight - ((value - minValue) / (maxValue - minValue) * chartHeight) - 10f
            )
        }

        // Vẽ đường biểu đồ
        drawSmoothLineChart(canvas = this, points = points, color = color)

        // Vẽ điểm dữ liệu
        points.forEach { point ->
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = point
            )
        }

        // Phần điền màu dưới đường
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                val first = points.first()
                val last = points.last()

                moveTo(first.x, chartHeight)
                lineTo(first.x, first.y)

                // Thêm tất cả điểm vào đường dẫn
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val current = points[i]

                    quadraticBezierTo(
                        prev.x, prev.y,
                        (prev.x + current.x) / 2, (prev.y + current.y) / 2
                    )
                }

                lineTo(last.x, last.y)
                lineTo(last.x, chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                color = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun HeaderSection() {
    val currentDate = LocalDate.now()
    val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH))
    val currentLocation by LocationManager.currentLocation
    var showDropdown by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Location with dropdown indicator
                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) { showDropdown = true }
                            .padding(vertical = 4.dp)
                    ) {
                        // Rest of your Row content remains the same
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            currentLocation.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Location",
                            tint = Color.White
                        )
                    }

                    // Enhanced dropdown menu
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier
                            .width(240.dp)
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        LocationManager.availableLocations.forEach { location ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        location.displayName,
                                        fontWeight = if (location.id == currentLocation.id)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    LocationManager.setCurrentLocation(location)
                                    showDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationCity,
                                        contentDescription = null,
                                        tint = Color(0xFF6BCBFF)
                                    )
                                },
                                trailingIcon = {
                                    if (location.id == currentLocation.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF6BCBFF)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .background(
                                        if (location.id == currentLocation.id)
                                            Color(0xFFEAF7FF) else Color.Transparent
                                    )
                            )
                        }
                    }
                }
                Text(formattedDate, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun AQISectionWithTabs(latestData: AirQualityData?) {
    val tabTitles = listOf("°C", "Hum", "PM2.5", "PM10", "NO2", "SO2", "CO")
    val tabTitles2 = listOf("°C", "%", "µg/m³", "µg/m³", "ppm", "ppm", "ppm")
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Lấy giá trị tương ứng với tab được chọn
    val selectedValue = latestData?.let {
        when (selectedTabIndex) {
            0 -> it.temperature
            1 -> it.humidity
            2 -> it.pm25
            3 -> it.pm10
            4 -> it.no2
            5 -> it.so2
            6 -> it.co
            else -> 0.0
        }
    } ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AQISection(latestData, selectedValue, tabTitles2[selectedTabIndex])
    }
}

@Composable
fun AQISection(latestData: AirQualityData?, value: Double, selectedCategory: String) {
    val airQualityText = when (latestData?.air_quality) {
        0.0 -> "GOOD"
        1.0 -> "HAZARDOUS"
        2.0 -> "MODERATE"
        3.0 -> "POOR"
        else -> "UNKNOWN"
    }

    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(airQualityText, color = Color.Unspecified, fontSize = 20.sp)
        Text(value.toDouble().toString(), color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
        Text(selectedCategory, color = Color.Unspecified, fontSize = 30.sp)
    }
}

data class AlertItemData(
    val message: String,
    val time: String,
    val location: String
)


object AlertManager {
    private val _alerts = mutableStateListOf<AlertItemData>()
    val alerts: List<AlertItemData> get() = _alerts

    fun addAlert(message: String, location: String) {
        if (_alerts.size >= 10) _alerts.removeAt(_alerts.lastIndex) // Xóa phần tử cuối cùng
        _alerts.add(0, AlertItemData(message, getCurrentTime(), location)) // Thêm vào đầu danh sách
    }

    private fun getCurrentTime(): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now())
    }
}

@Composable
fun AlertListScreen(alerts: List<AlertItemData>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.back2),
            contentDescription = "Box Background",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            alerts.forEach { alert ->
                AlertItem(alert.message, alert.time, alert.location)
            }
        }
    }
}

@Composable
fun DateRangeScreen() {
    var hourlyData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var dailyData by remember { mutableStateOf<List<Pair<String, Pair<Float, Float>>>>(emptyList()) }
    var currentLocation by remember { mutableStateOf("Hồ Chí Minh") }
    val currentDistrict by LocationManager.currentLocation
    var showDropdown by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var timeRangeData by remember { mutableStateOf<List<AirQualityData>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val dropdownAnchorPosition = remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    var data_curent_temp = 0.0
    fun fetchData() {
        coroutineScope.launch {
            try {

                // Lấy dữ liệu mới nhất với tham số vị trí
                val latest = RetrofitClient.apiService.getLatestAirQualityData(currentDistrict.id)
                val data = latest[0]
                data_curent_temp = data[2] as Double

                val predictions = RetrofitClient.apiService.getAllAirQualityPredictData(currentDistrict.id)
                    .map {
                        val timestamp = it[0] as String
                        val temperature = (it[2] as Double).toFloat()
                        timestamp to temperature
                    }

                hourlyData = predictions.take(12) // Lấy 12 giá trị đầu tiên

                dailyData = predictions
                    .groupBy { it.first.substring(0, 10) } // Gom nhóm theo ngày (YYYY-MM-DD)
                    .map { (date, values) ->
                        val minTemp = values.minOf { it.second }
                        val maxTemp = values.maxOf { it.second }
                        date to (minTemp to maxTemp)
                    }
            } catch (e: Exception) {
                Log.e("DateRangeScreen", "Error fetching data", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
        while (true) {
            delay(5 * 60 * 1000) // Cập nhật mỗi 5 phút
            fetchData()
        }
    }

    // Dynamic gradient based on time of day
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val gradientColors = when {
        currentHour < 5 -> listOf(Color(0xFF0F2027), Color(0xFF203A43)) // Night
        currentHour < 11 -> listOf(Color(0xFF2980B9), Color(0xFF6DD5FA)) // Morning
        currentHour < 18 -> listOf(Color(0xFF00B4DB), Color(0xFF0083B0)) // Day
        currentHour < 22 -> listOf(Color(0xFF141E30), Color(0xFF243B55)) // Night
        else -> listOf(Color(0xFF0F2027), Color(0xFF203A43)) // Deep Night
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Weather animation effect (optional)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Simple weather animation effect
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw some clouds or particles
            repeat(30) {
                val x = (Math.random() * canvasWidth).toFloat()
                val y = (Math.random() * canvasHeight / 3).toFloat()
                val radius = (3 + Math.random() * 8).toFloat()

                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Using LazyColumn as the main container instead of Column with vertical scroll
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Location header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentLocation,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Current temperature display
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        val currentTemp = hourlyData.firstOrNull()?.second?.toInt() ?: 0

                        // Animated temperature counting effect
                        val animatedTemperature by animateIntAsState(
                            targetValue = data_curent_temp.toInt(),
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = FastOutSlowInEasing
                            )
                        )

                        Text(
                            text = if (hourlyData.isEmpty()) "--" else "$animatedTemperature",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "°C",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            // Current district/area
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
                ) {
                    // Enhanced location picker with button styling
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) { showDropdown = true }
                            .padding(4.dp)
                    ) {
                        // Rest of your Surface content remains the same
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Location",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${currentDistrict.displayName}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Location",
                                tint = Color.White
                            )
                        }
                    }

                    // Enhanced dropdown positioned below the selector
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier
                            .width(280.dp)
                            .background(Color.White)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                    ) {
                        // Header for the dropdown
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF6BCBFF))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Chọn địa điểm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // List of locations
                        LocationManager.availableLocations.forEach { location ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        location.displayName,
                                        fontWeight = if (location.id == currentDistrict.id)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    LocationManager.setCurrentLocation(location)
                                    showDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationCity,
                                        contentDescription = null,
                                        tint = Color(0xFF6BCBFF)
                                    )
                                },
                                trailingIcon = {
                                    if (location.id == currentDistrict.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF6BCBFF)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .background(
                                        if (location.id == currentDistrict.id)
                                            Color(0xFFEAF7FF) else Color.Transparent
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            // Hourly forecast card
            item {
                WeatherCard(
                    title = "Dự báo hàng giờ",
                    icon = Icons.Outlined.Schedule,
                    content = {
                        EnhancedHourlyForecastSection(hourlyData)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Daily forecast card with height constraint for the inner LazyColumn
            item {
                WeatherCard(
                    title = "Dự báo hàng ngày",
                    icon = Icons.Outlined.DateRange,
                    content = {
                        FixedDailyForecastSection(dailyData)
                    }
                )
            }
        }
    }
}

@Composable
fun FixedDailyForecastSection(dailyData: List<Pair<String, Pair<Float, Float>>>) {
    // Use Column instead of LazyColumn to avoid nesting scrollable containers
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dailyData.forEach { (day, temps) ->
            EnhancedDailyForecastItem(day, temps)
        }
    }
}

// Rest of the composables remain the same
@Composable
fun WeatherCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Card content
            content()
        }
    }
}

@Composable
fun EnhancedHourlyForecastSection(hourlyData: List<Pair<String, Float>>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(hourlyData) { (time, temp) ->
            HourlyForecastItem(time, temp)
        }
    }
}

@Composable
fun HourlyForecastItem(time: String, temp: Float) {
    // Determine weather icon based on temperature
    val weatherIcon = when {
        temp >= 30 -> Icons.Outlined.WbSunny
        temp >= 20 -> Icons.Outlined.Cloud
        else -> Icons.Outlined.NightsStay
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(80.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Time
            Text(
                text = time.substring(11, 16),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // Weather icon
            Icon(
                imageVector = weatherIcon,
                contentDescription = "Weather",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .padding(vertical = 4.dp)
            )

            // Temperature
            Text(
                text = "${temp.toInt()}°",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EnhancedDailyForecastItem(day: String, temps: Pair<Float, Float>) {
    val (minTemp, maxTemp) = temps
    val dayOfWeek = try {
        val date = LocalDate.parse(day)
        date.dayOfWeek.getDisplayName(JavaTimeTextStyle.SHORT, Locale.getDefault())
    } catch (e: Exception) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(day)
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            dayFormat.format(date)
        } catch (e: Exception) {
            day
        }
    }

    val formattedDate = try {
        val date = LocalDate.parse(day)
        val formatter = DateTimeFormatter.ofPattern("dd/MM")
        formatter.format(date)
    } catch (e: Exception) {
        day
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and day
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dayOfWeek,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formattedDate,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            // Weather icon based on average temp
            val avgTemp = (minTemp + maxTemp) / 2
            val weatherIcon = when {
                avgTemp >= 30 -> Icons.Outlined.WbSunny
                avgTemp >= 20 -> Icons.Outlined.Cloud
                else -> Icons.Outlined.AcUnit
            }

            Icon(
                imageVector = weatherIcon,
                contentDescription = "Weather",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Temperature range
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Min temp
                Text(
                    text = "${minTemp.toInt()}°",
                    color = Color(0xFF90CAF9),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Temperature bar
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF90CAF9),
                                    Color(0xFFF48FB1)
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Max temp
                Text(
                    text = "${maxTemp.toInt()}°",
                    color = Color(0xFFF48FB1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AlertItem(title: String, time: String, location: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Time",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = time,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Location",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val screens = listOf("alerts", "home", "daterange")
    val icons = listOf(Icons.TwoTone.Notifications, Icons.TwoTone.Home, Icons.TwoTone.DateRange)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.dp)
            .background(Color(0xFF66E0FF))
            .padding(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF66E0FF)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEachIndexed { index, route ->
                    val isSelected = currentRoute == route
                    IconButton(onClick = { navController.navigate(route) }) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF004E92) else Color.White,
                            modifier = Modifier.size(if (isSelected) 34.dp else 28.dp)
                            //if (isSelected) Brush.verticalGradient(listOf(Color(0xFFFF9F6B), Color(0xFFFF6B6B))) else SolidColor(Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

fun drawSmoothLineChart(
    canvas: DrawScope,
    points: List<Offset>,
    color: Color
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
    }

    val strokeWidth = with(canvas) { 3.dp.toPx() }  // ✅ Làm dày nét vẽ

    for (i in points.indices) {
        when (i) {
            0 -> {
                // Giữ nguyên điểm đầu
                path.moveTo(points[i].x, points[i].y)
            }
            points.lastIndex -> {
                // 🔥 Dùng quadraticBezierTo() để nối đến điểm cuối cùng
                val prev = points[i - 1]
                val end = points[i]
                path.quadraticTo(prev.x, prev.y, end.x, end.y)
            }
            else -> {
                val prev = points[i - 1]
                val current = points[i]
                val next = points[i + 1]

                val midX1 = (prev.x + current.x) / 2
                val midY1 = (prev.y + current.y) / 2
                val midX2 = (current.x + next.x) / 2
                val midY2 = (current.y + next.y) / 2

                // Sử dụng Catmull-Rom Spline thay vì cubicTo
                path.quadraticTo(midX1, midY1, current.x, current.y)
            }
        }
    }

    canvas.drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round  // ✅ Làm mượt tại các điểm gấp khúc
        )
    )
}




@Composable
fun TemperatureChart(
    tempData: List<Pair<LocalDateTime, Float>>, // ⏳ Sử dụng LocalDateTime thay vì timestamp
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    // Animation khi load
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    if (tempData.isEmpty()) return

    // Tính giá trị min và max của nhiệt độ từ dữ liệu
    var minTemp = tempData.minOf { it.second }
    var maxTemp = tempData.maxOf { it.second }

    // Nếu max - min < 3, điều chỉnh giá trị trục Y sao cho có ít nhất 3 khoảng nhiệt độ
    if (maxTemp - minTemp < 3) {
        val diff = 3 - (maxTemp - minTemp)
        minTemp -= diff / 2
        maxTemp += diff / 2
    }

    // Đảm bảo minTemp và maxTemp là các số nguyên chẵn
    minTemp = (minTemp.toInt() / 1).toFloat() // Làm tròn minTemp về giá trị nguyên
    maxTemp = (maxTemp.toInt() / 1).toFloat() // Làm tròn maxTemp về giá trị nguyên

    // Tính toán thời gian min và max để xác định phạm vi trục X
    val minTime = tempData.minOf { it.first }
    val maxTime = tempData.maxOf { it.first }
    val totalDuration = ChronoUnit.MILLIS.between(minTime, maxTime).toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height
        val paddingLeft = 25f  // Chừa không gian cho trục Y

        // 🎯 Vẽ trục Y
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, chartHeight),
            strokeWidth = 2.dp.toPx()
        )

        // 🎯 Vẽ trục X
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, chartHeight - 10f),
            end = Offset(chartWidth, chartHeight - 10f),
            strokeWidth = 2.dp.toPx(),
        )

        // 📌 Vẽ Nhãn trên trục Y dựa vào giá trị min và max từ dữ liệu
        val yLabels = generateYLabels(minTemp, maxTemp)
        yLabels.forEach { value ->
            val yPos = chartHeight - ((value - minTemp) / (maxTemp - minTemp) * chartHeight) - 5f
            drawContext.canvas.nativeCanvas.drawText(

                "$value",
                -25f,
                yPos,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                }
            )
        }
        // 🎯 Thêm chữ "°C" trên đầu trục Y
        drawContext.canvas.nativeCanvas.drawText(
            "°C",
            paddingLeft - 50f,  // Dịch trái để gần trục Y hơn
            20f,  // Đặt ở trên đầu của trục Y
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 35f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        )

        // 📌 Tạo nhãn động cho trục X
        val timeLabels = tempData.map { it.first }.distinct().sorted()
        val timeStep = (timeLabels.size / 5).coerceAtLeast(1)

        timeLabels.forEachIndexed { index, time ->
            if (index % timeStep == 0) {
                val xPos = paddingLeft + (ChronoUnit.MILLIS.between(minTime, time) / totalDuration * (chartWidth - paddingLeft))
                val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                drawContext.canvas.nativeCanvas.drawText(
                    formattedTime,
                    xPos - 20f,
                    chartHeight + 20f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 30f
                    }
                )
            }
        }

        // 🎯 Thêm chữ "Time" trên đầu trục X
        drawContext.canvas.nativeCanvas.drawText(
            "Time",
            chartWidth / 2,  // Căn giữa theo chiều ngang
            chartHeight + 50f,  // Đặt phía trên trục X
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 35f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
        )

        // 📌 Vẽ cột nhiệt độ (Bar Chart)
        // 📌 Vẽ cột nhiệt độ (Bar Chart) với cột dày hơn và có hiển thị giá trị
        tempData.forEach { (time, temp) ->
            val normalizedTemp = (temp - minTemp) / (maxTemp - minTemp)
            val barHeight = chartHeight * normalizedTemp * animatedProgress.value
            val xPos = paddingLeft + (ChronoUnit.MILLIS.between(minTime, time) / totalDuration * (chartWidth - paddingLeft))

            // Vẽ cột dày hơn (tăng từ 20f lên 35f)
            drawRect(
                color = Color(0xFFFF7043).copy(alpha = 0.8f),
                topLeft = Offset(xPos - 17.5f, chartHeight - barHeight - 10f), // Dịch trái một chút để căn giữa
                size = Size(50f, barHeight) // Điều chỉnh độ rộng của cột
            )

            // Hiển thị giá trị nhiệt độ trên mỗi cột
            drawContext.canvas.nativeCanvas.drawText(
                "%.1f".format(temp), // Hiển thị nhiệt độ với 1 số lẻ
                xPos - 20f, // Căn chỉnh để chữ nằm giữa cột
                chartHeight - barHeight - 30f, // Đặt chữ trên cột
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }


        // 📌 Chuyển dữ liệu thành danh sách điểm cho Line Chart
        val points = tempData.map { (time, temp) ->
            Offset(
                x = paddingLeft + (ChronoUnit.MILLIS.between(minTime, time) / totalDuration * (chartWidth - paddingLeft)),
                y = chartHeight - ((temp - minTemp) / (maxTemp - minTemp) * chartHeight) - 10f
            )
        }

        // 📌 Vẽ đường nhiệt độ (Line Chart)
        drawSmoothLineChart(canvas = this, points = points, color = Color.Black)
    }
}

fun generateYLabels(minTemp: Float, maxTemp: Float): List<Float> {
    val range = maxTemp - minTemp
    val step = if (range < 3) 1f else (range / 3).coerceAtLeast(1f)  // Tạo bước nhảy ít nhất là 1 độ
    val labels = mutableListOf<Float>()

    var label = (minTemp / 1).toInt() * 1f  // Làm tròn xuống đến số nguyên chẵn gần nhất
    if (label % 2 != 0f) label += 1f // Nếu không phải số chẵn, làm tròn lên

    while (label <= maxTemp) {
        labels.add(label)
        label += step
        if (label % 2 != 0f) label += 1f // Đảm bảo rằng giá trị tiếp theo là số chẵn
    }
    return labels
}




@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    AirQualityAppTheme {
        AppNavigation()
    }
}
@Preview(showBackground = true)
@Composable
fun PreviewAlertListScreen() {
    AirQualityAppTheme {
        AlertListScreen(alerts = AlertManager.alerts)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDateRangeScreen() {
    AirQualityAppTheme {
        DateRangeScreen()
    }
}