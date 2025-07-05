# 🌱 Hệ thống Giám sát & Dự báo Chất lượng Không khí bằng IoT và Học sâu

> 📊 Hệ thống ứng dụng cảm biến IoT để giám sát thời gian thực các chỉ số chất lượng không khí và sử dụng mô hình học sâu (DNN, LSTM) để phân loại và dự đoán ô nhiễm, đồng thời hiển thị dữ liệu trên ứng dụng Android hiện đại.

---

## 🧭 Mục tiêu dự án

- Phát triển hệ thống IoT thu thập dữ liệu môi trường: PM2.5, PM10, CO, NO2, SO2, nhiệt độ, độ ẩm...
- Áp dụng các mô hình học sâu (DNN, LSTM) để phân loại mức độ ô nhiễm và dự đoán nhiệt độ
- Triển khai cloud backend sử dụng MQTT – FastAPI – PostgreSQL
- Hiển thị dữ liệu trực quan trên ứng dụng Android (Jetpack Compose)

---

## 🧱 Kiến trúc hệ thống

![System Architecture](https://github.com/Cuong312004/AirQuality/blob/main/images/mo-hinh-tong-the%20(1).png)

---

## 🛠 Công nghệ sử dụng

| Thành phần             | Công nghệ / Thiết bị                      |
|------------------------|-------------------------------------------|
| Cảm biến               | DHT11, PMS7003, MQ7                      |
| Vi điều khiển          | ESP8266 (Arduino IDE)                    |
| Giao tiếp              | MQTT (Mosquitto - Cedalo)               |
| Backend API            | Python, FastAPI                         |
| Cơ sở dữ liệu          | PostgreSQL                              |
| Mô hình AI             | DNN (phân loại), LSTM (dự đoán nhiệt độ)|
| Giao diện người dùng   | Android Studio, Jetpack Compose         |
| API giao tiếp          | Retrofit (Android)                      |

---

## 📦 Mô hình AI triển khai

### 🔹 Deep Neural Network (DNN)
- Dữ liệu đầu vào: Các chỉ số môi trường (PM2.5, PM10, NO2, CO, SO2, TEMP, HUM)
- Mục tiêu: Phân loại mức độ ô nhiễm (Tốt, Trung bình, Cao, Nguy hiểm)
  ![DNN_flow](https://github.com/Cuong312004/AirQuality/blob/main/images/huanluyendnn.png)
- Kết quả: Accuracy đạt **97.66%**
- Đánh giá: MAE, RMSE, R² trên tập kiểm định
  ![DNN_result_1](https://github.com/Cuong312004/AirQuality/blob/main/images/matranhonloandnn.png)
### 🔹 Long Short-Term Memory (LSTM)
- Mục tiêu: Dự đoán **nhiệt độ liên tục 7 ngày tới**
- Kiến trúc: LSTM layers + Dropout + BatchNorm + Dense
  ![LSTM_flow](https://github.com/Cuong312004/AirQuality/blob/main/images/huanluyenlstm.png)
- Loss Function: MSE – Optimizer: Adam
- Hiệu suất: 207s/predict với mức RAM < 22% và CPU < 3%
  ![LSTM_result_1](https://github.com/Cuong312004/AirQuality/blob/main/images/EvaluationMetrics.png)
  ![LSTM_result_2](https://github.com/Cuong312004/AirQuality/blob/main/images/uitiot_predict.png)
---

## 📌 Kiến trúc Cloud
![Cloud_result](https://github.com/Cuong312004/AirQuality/blob/main/images/trienkhaicloud%20(1).png)
## 📱 Ứng dụng Android

- Ngôn ngữ: Kotlin  
- UI: Jetpack Compose  
- API: Retrofit  
- Dữ liệu cập nhật mỗi 5 phút
![MOBILE_result_1](https://github.com/Cuong312004/AirQuality/blob/main/images/homescreen1.png)
### 🔸 Chức năng chính:
- **Trang chính (Home)**: Hiển thị AQI hiện tại + biểu đồ cảm biến  
- **Cảnh báo (Alerts)**: Gửi thông báo thời gian thực khi vượt ngưỡng  
- **Lịch dự báo (Forecast)**: Biểu đồ nhiệt độ theo giờ/ngày (LSTM)  
- **Dropdown chọn khu vực**: Lấy dữ liệu dựa trên vị trí từ API `/locations`
![MOBILE_result_2](https://github.com/Cuong312004/AirQuality/blob/main/images/predicthourScreen.png)
---

## 🌐 API Endpoint (FastAPI)

| Method | Endpoint | Mô tả |
|--------|----------|------|
| `GET`  | `/locations` | Lấy danh sách vị trí |
| `GET`  | `/latest_air_quality_data` | Dữ liệu cảm biến mới nhất |
| `GET`  | `/latest_12_air_quality_predict` | Dự đoán AQI |
| `GET`  | `/latest_12_all_parameters` | Dữ liệu toàn bộ chỉ số |

---

## 🧪 Dataset

- **Cảm biến thật** từ ESP8266 gửi mỗi 5s  
- **Dữ liệu huấn luyện** từ bộ `jena_climate_2009_2016.csv` và tập dữ liệu từ UIT Sensor Network

---

## 📁 Cấu trúc thư mục

```
📦 air-quality-monitoring/
├── sensors/                # Mã nguồn ESP8266
├── server/                 # FastAPI + Mô hình AI
│   ├── API_v2.py
│   ├── modelDNN.keras
│   ├── modelLSTM.keras
│   ├── mqtt_subscriber_v3.py
│   ├── scalerDNN.pkl
│   └── scalerLSTM.pkl
├── AirQualityApp/                 # Android App – Jetpack Compose
├── Dataset/               # PostgreSQL schema & init
├── README.md
```

---

## 👨‍💻 Thành viên thực hiện

- **Lưu Quốc Cường** – 20520173  
- **Bùi Minh Quân** – 20521173  
- 📧 Email: quocuongluu03@gmail.com

---
