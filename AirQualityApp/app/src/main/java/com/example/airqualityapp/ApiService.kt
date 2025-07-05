package com.example.airqualityapp

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("available_locations")
    suspend fun getAvailableLocations(): List<Location>

    @GET("/latest_air_quality_data")
    suspend fun getLatestAirQualityData(@Query("location") location: String? = null): List<List<Any>>

    @GET("/all_air_quality_predict_data")
    suspend fun getAllAirQualityPredictData(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_air_quality_predict")
    suspend fun getLatest12AirQualityPredict(@Query("location") location: String? = null): List<List<Any>>

    // API cho từng tham số với location parameter
    @GET("/latest_12_humidity")
    suspend fun getLatest12Humidity(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_pm25")
    suspend fun getLatest12PM25(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_pm10")
    suspend fun getLatest12PM10(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_no2")
    suspend fun getLatest12NO2(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_so2")
    suspend fun getLatest12SO2(@Query("location") location: String? = null): List<List<Any>>

    @GET("/latest_12_co")
    suspend fun getLatest12CO(@Query("location") location: String? = null): List<List<Any>>

    // API tổng hợp tất cả tham số
    @GET("/latest_12_all_parameters")
    suspend fun getLatest12AllParameters(@Query("location") location: String? = null): ParametersResponse

}

// Cập nhật data class để phản ánh đúng cấu trúc dữ liệu từ backend
data class AirQualityData(
    val timestamp: String,
    val location: String,
    val temperature: Double,
    val humidity: Double,
    val pm25: Double,
    val pm10: Double,
    val no2: Double,
    val so2: Double,
    val co: Double,
    val air_quality: Double
)

data class AirQualityPredictData(
    val timestamp: String,
    val location: String,
    val temperature: Double
)

data class AirQualityPredict(
    val timestamp: String,
    val location: String,
    val temperature: Double
)

// Data class cho từng tham số riêng lẻ, cập nhật để bao gồm location
data class ParameterData(
    val timestamp: String,
    val location: String,
    val value: Double
)

// Data class cho API tổng hợp
data class ParametersResponse(
    val humidity: List<List<Any>>,
    val pm25: List<List<Any>>,
    val pm10: List<List<Any>>,
    val no2: List<List<Any>>,
    val so2: List<List<Any>>,
    val co: List<List<Any>>
)

data class Location(
    val id: String,      // Unique identifier for API calls (e.g., "thu_duc")
    val name: String,    // Short name (e.g., "Thủ Đức")
    val displayName: String  // Full display name (e.g., "Thủ Đức, Hồ Chí Minh")
)

// Tiện ích mở rộng để chuyển đổi từ List<List<Any>> sang danh sách tham số
fun List<List<Any>>.toParameterDataList(): List<ParameterData> {
    return this.map {
        ParameterData(
            timestamp = it[0] as String,
            location = it[1] as String,
            value = if (it[2] is Double) it[2] as Double else (it[2] as Number).toDouble()
        )
    }
}