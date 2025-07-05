import pickle
import paho.mqtt.client as mqtt
import numpy as np
import pandas as pd
import tensorflow as tf
from sqlalchemy import create_engine, Column, Integer, Float, TIMESTAMP, String
from sqlalchemy.orm import sessionmaker, declarative_base
from sqlalchemy.sql import text
from datetime import datetime, timedelta
import json
import gc
import time
import threading
from collections import deque
import statistics
import signal
import sys

# Performance monitoring class
class PerformanceMonitor:
    def __init__(self, max_history=100):
        self.max_history = max_history
        self.processing_times = deque(maxlen=max_history)
        self.prediction_times = deque(maxlen=max_history)
        self.lstm_prediction_times = deque(maxlen=max_history)
        self.db_save_times = deque(maxlen=max_history)
        self.total_messages_processed = 0
        self.total_predictions_made = 0
        self.total_lstm_predictions = 0
        self.total_db_operations = 0
        self.error_count = 0
        self.start_time = time.time()
        self.lock = threading.Lock()
        
    def record_processing_time(self, duration):
        with self.lock:
            self.processing_times.append(duration)
            self.total_messages_processed += 1
    
    def record_prediction_time(self, duration):
        with self.lock:
            self.prediction_times.append(duration)
            self.total_predictions_made += 1
    
    def record_lstm_prediction_time(self, duration):
        with self.lock:
            self.lstm_prediction_times.append(duration)
            self.total_lstm_predictions += 1
    
    def record_db_save_time(self, duration):
        with self.lock:
            self.db_save_times.append(duration)
            self.total_db_operations += 1
    
    def record_error(self):
        with self.lock:
            self.error_count += 1
            
    def export_stats_to_json(self, filename=None):
        """Export thống kê ra file JSON"""
        if filename is None:
            filename = f"performance_stats_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        try:
            import json
            stats = self.get_stats()
            resources = self.get_system_resources()
            alerts = self.check_performance_alerts()
            
            export_data = {
                'timestamp': datetime.now().isoformat(),
                'performance_stats': stats,
                'system_resources': resources,
                'alerts': alerts
            }
            
            with open(filename, 'w') as f:
                json.dump(export_data, f, indent=2, default=str)
            
            print(f"[INFO] Performance stats exported to {filename}")
            return filename
            
        except Exception as e:
            print(f"[ERROR] Failed to export stats: {e}")
            return None
    
    def get_stats(self):
        with self.lock:
            uptime = time.time() - self.start_time
            
            stats = {
                'uptime_seconds': uptime,
                'uptime_hours': uptime / 3600,
                'total_messages_processed': self.total_messages_processed,
                'total_predictions_made': self.total_predictions_made,
                'total_lstm_predictions': self.total_lstm_predictions,
                'total_db_operations': self.total_db_operations,
                'error_count': self.error_count,
                'error_rate': self.error_count / max(1, self.total_messages_processed) * 100,
                'messages_per_hour': self.total_messages_processed / max(1, uptime) * 3600,
                'predictions_per_hour': self.total_predictions_made / max(1, uptime) * 3600
            }
            
            # Processing time stats
            if self.processing_times:
                stats['avg_processing_time'] = statistics.mean(self.processing_times)
                stats['max_processing_time'] = max(self.processing_times)
                stats['min_processing_time'] = min(self.processing_times)
                stats['median_processing_time'] = statistics.median(self.processing_times)
            
            # Prediction time stats
            if self.prediction_times:
                stats['avg_prediction_time'] = statistics.mean(self.prediction_times)
                stats['max_prediction_time'] = max(self.prediction_times)
                stats['min_prediction_time'] = min(self.prediction_times)
            
            # LSTM prediction time stats
            if self.lstm_prediction_times:
                stats['avg_lstm_prediction_time'] = statistics.mean(self.lstm_prediction_times)
                stats['max_lstm_prediction_time'] = max(self.lstm_prediction_times)
                stats['min_lstm_prediction_time'] = min(self.lstm_prediction_times)
            
            # Database save time stats
            if self.db_save_times:
                stats['avg_db_save_time'] = statistics.mean(self.db_save_times)
                stats['max_db_save_time'] = max(self.db_save_times)
                stats['min_db_save_time'] = min(self.db_save_times)
            
            return stats
    def check_performance_alerts(self):
        """Kiểm tra và cảnh báo các vấn đề hiệu suất"""
        alerts = []
        
        # Kiểm tra thời gian xử lý quá cao
        if self.processing_times and statistics.mean(self.processing_times) > 30:
            alerts.append("WARNING: Average processing time > 30 seconds")
        
        # Kiểm tra tỷ lệ lỗi cao
        if self.total_messages_processed > 0:
            error_rate = (self.error_count / self.total_messages_processed) * 100
            if error_rate > 5:
                alerts.append(f"CRITICAL: Error rate is {error_rate:.1f}% (> 5%)")
        
        # Kiểm tra thời gian LSTM prediction quá cao
        if self.lstm_prediction_times and statistics.mean(self.lstm_prediction_times) > 300:
            alerts.append("WARNING: LSTM prediction time > 5 minutes")
        
        # Kiểm tra tài nguyên hệ thống
        resources = self.get_system_resources()
        if 'error' not in resources:
            if resources['cpu_percent'] > 80:
                alerts.append(f"WARNING: High CPU usage {resources['cpu_percent']:.1f}%")
            if resources['memory_percent'] > 85:
                alerts.append(f"WARNING: High memory usage {resources['memory_percent']:.1f}%")
            if resources['disk_percent'] > 90:
                alerts.append(f"CRITICAL: Low disk space {resources['disk_free_gb']:.2f} GB free")
        
        return alerts

    def log_performance_alerts(self):
        """Ghi log các cảnh báo hiệu suất"""
        alerts = self.check_performance_alerts()
        if alerts:
            print("\n" + "!"*50)
            print("PERFORMANCE ALERTS")
            print("!"*50)
            for alert in alerts:
                print(f"[ALERT] {alert}")
            print("!"*50)
            
    def get_system_resources(self):
        """Lấy thông tin tài nguyên hệ thống"""
        try:
            import psutil
            import os
            
            # CPU usage
            cpu_percent = psutil.cpu_percent(interval=1)
            
            # Memory usage
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            memory_used_gb = memory.used / (1024**3)
            memory_total_gb = memory.total / (1024**3)
            
            # Process specific memory
            process = psutil.Process(os.getpid())
            process_memory = process.memory_info().rss / (1024**2)  # MB
            
            # Disk usage
            disk = psutil.disk_usage('/')
            disk_percent = disk.percent
            disk_free_gb = disk.free / (1024**3)
            
            return {
                'cpu_percent': cpu_percent,
                'memory_percent': memory_percent,
                'memory_used_gb': memory_used_gb,
                'memory_total_gb': memory_total_gb,
                'process_memory_mb': process_memory,
                'disk_percent': disk_percent,
                'disk_free_gb': disk_free_gb
            }
        except ImportError:
            return {'error': 'psutil not available'}
        except Exception as e:
            return {'error': str(e)}
    
    def save_stats_to_db(self, engine):
        """Lưu thống kê vào database"""
        try:
            stats = self.get_stats()
            from sqlalchemy.orm import sessionmaker
            SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
            session = SessionLocal()
            
            stats_entry = PerformanceStats(
                timestamp=datetime.utcnow(),
                uptime_hours=stats.get('uptime_hours', 0),
                total_messages_processed=stats.get('total_messages_processed', 0),
                total_predictions_made=stats.get('total_predictions_made', 0),
                total_lstm_predictions=stats.get('total_lstm_predictions', 0),
                total_db_operations=stats.get('total_db_operations', 0),
                error_count=stats.get('error_count', 0),
                error_rate=stats.get('error_rate', 0),
                messages_per_hour=stats.get('messages_per_hour', 0),
                predictions_per_hour=stats.get('predictions_per_hour', 0),
                avg_processing_time=stats.get('avg_processing_time', 0),
                avg_prediction_time=stats.get('avg_prediction_time', 0),
                avg_lstm_prediction_time=stats.get('avg_lstm_prediction_time', 0),
                avg_db_save_time=stats.get('avg_db_save_time', 0),
                max_processing_time=stats.get('max_processing_time', 0),
                max_prediction_time=stats.get('max_prediction_time', 0),
                max_lstm_prediction_time=stats.get('max_lstm_prediction_time', 0),
                max_db_save_time=stats.get('max_db_save_time', 0)
            )
            
            session.add(stats_entry)
            session.commit()
            session.close()
            print("[INFO] Performance stats saved to database")
            
        except Exception as e:
            print(f"[ERROR] Failed to save performance stats: {e}")
            if 'session' in locals():
                session.close()
    
    def print_stats(self):
        stats = self.get_stats()
        print("\n" + "="*50)
        print("PERFORMANCE STATISTICS")
        print("="*50)
        print(f"System Uptime: {stats['uptime_hours']:.2f} hours")
        print(f"Total Messages Processed: {stats['total_messages_processed']}")
        print(f"Total Predictions Made: {stats['total_predictions_made']}")
        print(f"Total LSTM Predictions: {stats['total_lstm_predictions']}")
        print(f"Total DB Operations: {stats['total_db_operations']}")
        print(f"Error Count: {stats['error_count']}")
        print(f"Error Rate: {stats['error_rate']:.2f}%")
        print(f"Messages per Hour: {stats['messages_per_hour']:.2f}")
        print(f"Predictions per Hour: {stats['predictions_per_hour']:.2f}")
        
        if 'avg_processing_time' in stats:
            print(f"Avg Processing Time: {stats['avg_processing_time']:.3f}s")
            print(f"Max Processing Time: {stats['max_processing_time']:.3f}s")
            print(f"Min Processing Time: {stats['min_processing_time']:.3f}s")
        
        if 'avg_prediction_time' in stats:
            print(f"Avg Prediction Time: {stats['avg_prediction_time']:.3f}s")
            print(f"Max Prediction Time: {stats['max_prediction_time']:.3f}s")
        
        if 'avg_lstm_prediction_time' in stats:
            print(f"Avg LSTM Prediction Time: {stats['avg_lstm_prediction_time']:.3f}s")
            print(f"Max LSTM Prediction Time: {stats['max_lstm_prediction_time']:.3f}s")
        
        if 'avg_db_save_time' in stats:
            print(f"Avg DB Save Time: {stats['avg_db_save_time']:.3f}s")
            print(f"Max DB Save Time: {stats['max_db_save_time']:.3f}s")
        
        resources = self.get_system_resources()
        if 'error' not in resources:
            print(f"CPU Usage: {resources['cpu_percent']:.1f}%")
            print(f"Memory Usage: {resources['memory_percent']:.1f}% ({resources['memory_used_gb']:.2f}/{resources['memory_total_gb']:.2f} GB)")
            print(f"Process Memory: {resources['process_memory_mb']:.2f} MB")
            print(f"Disk Usage: {resources['disk_percent']:.1f}% (Free: {resources['disk_free_gb']:.2f} GB)")
        
        print("="*50)

    def reset_stats(self):
        """Reset tất cả thống kê về 0"""
        with self.lock:
            self.processing_times.clear()
            self.prediction_times.clear()
            self.lstm_prediction_times.clear()
            self.db_save_times.clear()
            self.total_messages_processed = 0
            self.total_predictions_made = 0
            self.total_lstm_predictions = 0
            self.total_db_operations = 0
            self.error_count = 0
            self.start_time = time.time()
            print("[INFO] Performance statistics have been reset")	

def periodic_stats_reporter():
    """Thread function để hiển thị thống kê định kỳ"""
    while True:
        time.sleep(300)  # Hiển thị thống kê mỗi 5 phút
        performance_monitor.print_stats()
        performance_monitor.log_performance_alerts()  # Thêm dòng này

def periodic_stats_saver():
    """Thread function để lưu thống kê vào database định kỳ"""
    while True:
        time.sleep(1800)  # Lưu thống kê mỗi 30 phút
        performance_monitor.save_stats_to_db(engine)

def signal_handler(signum, frame):
    """Handler để xử lý tín hiệu thoát"""
    print(f"\n[INFO] Received signal {signum}, shutting down gracefully...")
    
    # Hiển thị thống kê cuối cùng
    performance_monitor.print_stats()
    
    # Export thống kê ra file
    performance_monitor.export_stats_to_json()
    
    # Lưu thống kê cuối cùng vào database
    performance_monitor.save_stats_to_db(engine)
    
    print("[INFO] Shutdown complete")
    sys.exit(0)

# Create global performance monitor instance
performance_monitor = PerformanceMonitor()

# Function to log memory usage
def log_memory_usage():
    """Log current memory usage of the process."""
    try:
        import psutil
        import os
        process = psutil.Process(os.getpid())
        memory_info = process.memory_info()
        print(f"[MEMORY] Current memory usage: {memory_info.rss / (1024 * 1024):.2f} MB")
    except ImportError:
        print("[MEMORY] psutil module not available. Install with 'pip install psutil' to enable memory monitoring.")

# Load model DNN
model = tf.keras.models.load_model("modelDNN.keras")
# Load model LSTM
modelLstm = tf.keras.models.load_model("modelLSTM.keras")

# Load scaler từ file đã lưu
with open("scalerDNN.pkl", "rb") as f:
    scaler = pickle.load(f)
with open("scalerLSTM.pkl", "rb") as f:
    scaler_lstm = pickle.load(f)

# Kết nối đến PostgreSQL
DB_USER = "user"
DB_PASSWORD = "pass"
DB_HOST = "192.168.1.100"
DB_PORT = "5432"
DB_NAME = "air_quality"

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Định nghĩa bảng chính - thêm trường location
class AirQualityData(Base):
    __tablename__ = "air_quality_data"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    temperature = Column(Float)
    humidity = Column(Float)
    pm25 = Column(Float)
    pm10 = Column(Float)
    no2 = Column(Float)
    so2 = Column(Float)
    co = Column(Float)
    air_quality = Column(Integer)  # Giá trị do model AI dự đoán



# Định nghĩa các bảng riêng cho từng loại dữ liệu theo dạng timestamp + value - thêm trường location
class TemperatureData(Base):
    __tablename__ = "temperature_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class HumidityData(Base):
    __tablename__ = "humidity_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class PM25Data(Base):
    __tablename__ = "pm25_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class PM10Data(Base):
    __tablename__ = "pm10_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class NO2Data(Base):
    __tablename__ = "no2_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class SO2Data(Base):
    __tablename__ = "so2_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

class COData(Base):
    __tablename__ = "co_data"
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    value = Column(Float)

# Định nghĩa bảng air_quality_predict - thêm trường location
class AirQualityPredict(Base):
    __tablename__ = "air_quality_predict"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    temperature = Column(Float)
    day_sin = Column(Float)
    day_cos = Column(Float)
    year_sin = Column(Float)
    year_cos = Column(Float)

# Định nghĩa bảng air_quality_predict_data - thêm trường location
class AirQualityPredictData(Base):
    __tablename__ = "air_quality_predict_data"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    location = Column(String(255))  # Thêm trường location
    temperature = Column(Float)
    day_sin = Column(Float)
    day_cos = Column(Float)
    year_sin = Column(Float)
    year_cos = Column(Float)

# Thêm sau class AirQualityPredictData
class PerformanceStats(Base):
    __tablename__ = "performance_stats"
    
    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(TIMESTAMP)
    uptime_hours = Column(Float)
    total_messages_processed = Column(Integer)
    total_predictions_made = Column(Integer)
    total_lstm_predictions = Column(Integer)
    total_db_operations = Column(Integer)
    error_count = Column(Integer)
    error_rate = Column(Float)
    messages_per_hour = Column(Float)
    predictions_per_hour = Column(Float)
    avg_processing_time = Column(Float)
    avg_prediction_time = Column(Float)
    avg_lstm_prediction_time = Column(Float)
    avg_db_save_time = Column(Float)
    max_processing_time = Column(Float)
    max_prediction_time = Column(Float)
    max_lstm_prediction_time = Column(Float)
    max_db_save_time = Column(Float)

# Tạo tất cả các bảng nếu chưa tồn tại
Base.metadata.create_all(bind=engine)

def calculate_sin_cos_features(timestamp):
    day = 60 * 60 * 24
    year = 365.2425 * day
    seconds = timestamp.timestamp()

    day_sin = np.sin(seconds * (2 * np.pi / day))
    day_cos = np.cos(seconds * (2 * np.pi / day))
    year_sin = np.sin(seconds * (2 * np.pi / year))
    year_cos = np.cos(seconds * (2 * np.pi / year))

    return day_sin, day_cos, year_sin, year_cos

def preprocess_for_lstm(data, scaler_lstm):
    """
    Tiền xử lý dữ liệu LSTM dựa trên danh sách 6 timestamp.

    Parameters:
        data: Danh sách các điểm dữ liệu (list of dict), mỗi điểm gồm 'temperature', 'day_sin', 'day_cos', 'year_sin', 'year_cos'.
        scaler_lstm: Bộ chuẩn hóa dữ liệu nhiệt độ.

    Returns:
        processed_data: Mảng numpy gồm các đặc trưng đã chuẩn hóa cho LSTM.
    """
    processed_data = []

    for entry in data:
        # Chuẩn hóa nhiệt độ
        temperature_reshaped = np.array([[entry['temperature']]])
        temperature_scaled = scaler_lstm.transform(temperature_reshaped)[0][0]
        
        # Tạo mảng chứa tất cả các đặc trưng (không tính lại thời gian)
        processed_data.append([
            temperature_scaled,       # Nhiệt độ đã chuẩn hóa
            entry['day_sin'],         # Giá trị đã lấy trực tiếp
            entry['day_cos'],         # Giá trị đã lấy trực tiếp
            entry['year_sin'],        # Giá trị đã lấy trực tiếp
            entry['year_cos']         # Giá trị đã lấy trực tiếp
        ])

    # Chuyển danh sách thành mảng numpy
    return np.array(processed_data)

# Hàm xử lý dữ liệu và dự đoán DNN - đã thêm performance monitoring
def predict_air_quality(data):
    start_time = time.time()
    
    try:
        # Chuyển dữ liệu thành mảng numpy
        features = np.array([[data["temperature"], data["humidity"], data["pm25"],
                              data["pm10"], data["no2"], data["so2"], data["co"]]])

        # Chuẩn hóa dữ liệu bằng scaler đã load
        features_scaled = scaler.transform(features)

        # Dự đoán chất lượng không khí
        prediction = model.predict(features_scaled, verbose=0)
        air_quality_class = np.argmax(prediction)  # Lấy nhãn có xác suất cao nhất
        
        # Giải phóng tài nguyên
        tf.keras.backend.clear_session()
        
        # Record successful prediction time
        prediction_time = time.time() - start_time
        performance_monitor.record_prediction_time(prediction_time)
        
        return int(air_quality_class)
    
    except Exception as e:
        # Record error
        performance_monitor.record_error()
        print(f"[ERROR] Error in predict_air_quality: {e}")
        raise

# Hàm dự đoán nhiệt độ sử dụng LSTM
def predict_temperature_lstm(model_lstm, data, scaler_lstm, n_days=7, time_step=15*60):
    """
    Dự đoán nhiệt độ trong n_days (mỗi 15 phút) sử dụng mô hình LSTM.

    Parameters:
        model_lstm: Mô hình LSTM đã được huấn luyện.
        data: Danh sách 6 điểm dữ liệu gần nhất (list of dict).
        scaler_lstm: Bộ chuẩn hóa dữ liệu.
        n_days: Số ngày dự đoán (mặc định: 7).
        time_step: Khoảng thời gian giữa các điểm dữ liệu (mặc định: 15 phút).

    Returns:
        predictions: Danh sách nhiệt độ dự đoán.
    """
    # Log memory usage at start
    log_memory_usage()
    # Thêm vào đầu hàm predict_temperature_lstm, sau dòng log_memory_usage()
    lstm_start_time = time.time()
    # Tiền xử lý dữ liệu (chuẩn hóa)
    print("[INFO] Bắt đầu tiền xử lý dữ liệu cho LSTM...")
    input_data = preprocess_for_lstm(data, scaler_lstm)  # (6, features)
    print(f"[INFO] Dữ liệu đã được chuẩn hóa. Kích thước: {input_data.shape}")

    # Định dạng input cho LSTM (batch_size=1, time_steps=6, features)
    current_input = np.reshape(input_data, (1, 6, -1))
    print(f"[INFO] Dữ liệu đầu vào định dạng cho LSTM: {current_input.shape}")

    predictions = []
    current_timestamp = data[-1]['timestamp']  # Bắt đầu từ thời điểm cuối cùng trong dữ liệu gốc

    print(f"[INFO] Bắt đầu dự đoán nhiệt độ cho {n_days} ngày tiếp theo...")
    
    
    # Giảm số lượng log và tối ưu vòng lặp
    total_steps = n_days * 24 * 4
    for i in range(total_steps):  # 7 ngày, mỗi 15 phút/lần => 7 * 24 * 4
        # Dự đoán nhiệt độ
        with tf.device('/CPU:0'):  # Force CPU usage to reduce GPU memory consumption
            predicted_temperature = float(model_lstm.predict(current_input, verbose=0)[0][0])
        
        # Clear TensorFlow session to free memory
        if i % 10 == 0:  # Clear session every 10 predictions
            tf.keras.backend.clear_session()
        
        # Chỉ log mỗi giờ để giảm việc ghi log
        if i % 4 == 0:
            print(f"[INFO] Dự đoán giờ thứ {i//4}: {predicted_temperature:.2f}°C")
            if i % 24 == 0 and i > 0:  # Log memory every 6 hours
                log_memory_usage()

        # Lưu kết quả
        predictions.append(predicted_temperature)

        # Tính toán timestamp tiếp theo (cộng thêm 15 phút)
        current_timestamp += timedelta(seconds=time_step)
        current_timestamp_pd = pd.to_datetime(current_timestamp)
        # Tính toán các đặc trưng thời gian mới (không cần logging mỗi lần)
        day_sin, day_cos, year_sin, year_cos = calculate_sin_cos_features(current_timestamp_pd)

        predicted_temperature_reshaped = np.array([[predicted_temperature]])
        predicted_temperature_scaler = scaler_lstm.transform(predicted_temperature_reshaped)[0][0]
        
        # Tạo dữ liệu mới cho input
        new_input = np.array([
            predicted_temperature_scaler,  # Dự đoán nhiệt độ mới
            day_sin,
            day_cos,
            year_sin,
            year_cos
        ])

        # Cập nhật dữ liệu đầu vào bằng cách loại bỏ điểm đầu và thêm điểm mới
        input_data = np.vstack([input_data[1:], new_input])  # Loại bỏ điểm đầu tiên, thêm điểm mới
        current_input = np.reshape(input_data, (1, 6, -1))  # Cập nhật input cho LSTM
        
        # Free up memory every 50 predictions
        if i % 50 == 0 and i > 0:
            gc.collect()  # Force garbage collection
            
        # Progress update
        if i % 96 == 0 and i > 0:  # Every day (96 = 4*24 predictions)
            print(f"[INFO] Progress: {i}/{total_steps} predictions completed ({i/total_steps*100:.1f}%)")

    print(f"[INFO] Hoàn thành dự đoán {len(predictions)} giá trị nhiệt độ.")
    
    # Final cleanup
    tf.keras.backend.clear_session()
    gc.collect()
    
    # Log final memory usage
    log_memory_usage()
    # Thêm vào cuối hàm predict_temperature_lstm, trước return predictions
    lstm_total_time = time.time() - lstm_start_time
    performance_monitor.record_lstm_prediction_time(lstm_total_time)
    print(f"[INFO] LSTM prediction completed in {lstm_total_time:.2f} seconds")
    return predictions

# Hàm lưu dữ liệu sensor vào các bảng riêng biệt - đã thêm performance monitoring
def save_sensor_data_to_individual_tables(session, timestamp, location, sensor_data):
    start_time = time.time()
    
    print("[INFO] Lưu dữ liệu sensor vào các bảng riêng...")
    try:
        # Lưu nhiệt độ
        temp_entry = TemperatureData(timestamp=timestamp, location=location, value=sensor_data["temperature"])
        session.add(temp_entry)
        
        # Lưu độ ẩm
        humidity_entry = HumidityData(timestamp=timestamp, location=location, value=sensor_data["humidity"])
        session.add(humidity_entry)
        
        # Lưu PM2.5
        pm25_entry = PM25Data(timestamp=timestamp, location=location, value=sensor_data["pm25"])
        session.add(pm25_entry)
        
        # Lưu PM10
        pm10_entry = PM10Data(timestamp=timestamp, location=location, value=sensor_data["pm10"])
        session.add(pm10_entry)
        
        # Lưu NO2
        no2_entry = NO2Data(timestamp=timestamp, location=location, value=sensor_data["no2"])
        session.add(no2_entry)
        
        # Lưu SO2
        so2_entry = SO2Data(timestamp=timestamp, location=location, value=sensor_data["so2"])
        session.add(so2_entry)
        
        # Lưu CO
        co_entry = COData(timestamp=timestamp, location=location, value=sensor_data["co"])
        session.add(co_entry)
        
        # Commit được thực hiện bởi hàm gọi
        
        # Record successful DB operation time
        db_save_time = time.time() - start_time
        performance_monitor.record_db_save_time(db_save_time)
        
        print("[INFO] Đã lưu tất cả các sensor vào các bảng riêng.")
        
    except Exception as e:
        performance_monitor.record_error()
        print(f"[ERROR] Lỗi khi lưu dữ liệu sensor: {e}")
        session.rollback()
        raise

# Callback khi nhận được dữ liệu từ MQTT
def on_message(client, userdata, msg, properties=None, reason_code=None):
    # Log initial memory usage
    log_memory_usage()
    message_start_time = time.time()
    session = None
    try:
        # Nhận dữ liệu từ MQTT và giải mã
        payload = json.loads(msg.payload.decode())
        print("[INFO] Received data:", payload)

        # Chuẩn hóa timestamp sử dụng Pandas
        if "timestamp" in payload:
            payload["timestamp"] = pd.to_datetime(payload["timestamp"])
            print("[INFO] Timestamp chuẩn hóa từ payload:", payload["timestamp"])
        else:
            payload["timestamp"] = pd.to_datetime(datetime.utcnow().isoformat())
            print("[INFO] Timestamp mặc định (UTC now):", payload["timestamp"])
        
        # Sử dụng location từ payload hoặc mặc định nếu không có
        if "location" not in payload:
            payload["location"] = "default"
            print("[INFO] Location không có trong payload, sử dụng giá trị mặc định:", payload["location"])
        else:
            print("[INFO] Location từ payload:", payload["location"])
        
        # Dự đoán chất lượng không khí
        print("[INFO] Bắt đầu dự đoán chất lượng không khí...")
        payload["air_quality"] = predict_air_quality(payload)
        print("[INFO] Chất lượng không khí dự đoán:", payload["air_quality"])

        # Tạo phiên làm việc với cơ sở dữ liệu
        session = SessionLocal()
        
        # Lưu vào PostgreSQL (bảng chính)
        print("[INFO] Lưu dữ liệu vào bảng air_quality_data...")
        new_entry = AirQualityData(**payload)
        session.add(new_entry)
        
        # Lưu dữ liệu vào các bảng riêng biệt
        save_sensor_data_to_individual_tables(session, payload["timestamp"], payload["location"], payload)
        
        session.commit()
        print("[INFO] Lưu thành công dữ liệu vào tất cả các bảng.")

        # Tính toán các đặc trưng thời gian
        print("[INFO] Tính toán các đặc trưng thời gian từ timestamp...")
        day_sin, day_cos, year_sin, year_cos = map(float, calculate_sin_cos_features(payload["timestamp"]))
        print(f"[INFO] Đặc trưng thời gian: day_sin={day_sin:.4f}, day_cos={day_cos:.4f}, year_sin={year_sin:.4f}, year_cos={year_cos:.4f}")

        # Lưu vào bảng air_quality_predict
        print("[INFO] Lưu dự đoán vào bảng air_quality_predict...")
        predict_entry = AirQualityPredict(
            timestamp=payload["timestamp"],
            location=payload["location"],
            temperature=payload["temperature"],
            day_sin=day_sin,
            day_cos=day_cos,
            year_sin=year_sin,
            year_cos=year_cos
        )
        session.add(predict_entry)
        session.commit()
        print("[INFO] Lưu thành công dự đoán vào air_quality_predict.")

        # Lấy 6 dữ liệu gần nhất từ air_quality_predict cho location hiện tại để dự đoán
        print(f"[INFO] Lấy 6 dữ liệu gần nhất cho location {payload['location']} từ bảng air_quality_predict...")
        last_6_entries = session.query(AirQualityPredict).filter(
            AirQualityPredict.location == payload["location"]
        ).order_by(AirQualityPredict.timestamp.desc()).limit(6).all()
        last_6_entries.reverse()  # Đảm bảo đúng thứ tự thời gian
       
       # Thêm log chi tiết cho 6 điểm dữ liệu đầu vào
        print("\n[INFO] === CHI TIẾT 6 DỮ LIỆU ĐẦU VÀO CHO LSTM ===")
        for i, entry in enumerate(last_6_entries):
             print(f"[INFO] Dữ liệu #{i+1}: timestamp={entry.timestamp}, temperature={entry.temperature:.2f}°C, "
                  f"day_sin={entry.day_sin:.4f}, day_cos={entry.day_cos:.4f}, "
                  f"year_sin={entry.year_sin:.4f}, year_cos={entry.year_cos:.4f}")
        
        if len(last_6_entries) < 6:
            print(f"[WARNING] Chỉ có {len(last_6_entries)} mẫu dữ liệu có sẵn cho location {payload['location']}, cần ít nhất 6 mẫu để dự đoán LSTM.")
            if len(last_6_entries) > 0:
                print("[INFO] Dữ liệu có sẵn:", last_6_entries)
            if session:
                session.close()
            return
            
        print("[INFO] 6 dữ liệu gần nhất đã được lấy.")

        # Chuyển dữ liệu thành định dạng cho LSTM
        print("[INFO] Chuyển dữ liệu thành định dạng cho LSTM...")
        data_for_lstm = [
            {
                'timestamp': entry.timestamp,
                'temperature': entry.temperature,
                'day_sin': entry.day_sin,
                'day_cos': entry.day_cos,
                'year_sin': entry.year_sin,
                'year_cos': entry.year_cos
            } for entry in last_6_entries
        ]
        print("[INFO] Dữ liệu LSTM đã được chuẩn bị.")

        # Log memory usage before prediction
        log_memory_usage()
        
        # Dự đoán nhiệt độ cho n ngày tiếp theo
        print("[INFO] Bắt đầu dự đoán nhiệt độ...")
        n_days = 7  # Có thể điều chỉnh số ngày dự đoán ở đây
        predictions = predict_temperature_lstm(modelLstm, data_for_lstm, scaler_lstm, n_days=n_days)
        print(f"[INFO] Dự đoán hoàn thành với {len(predictions)} giá trị nhiệt độ.")

        # Log memory after prediction
        log_memory_usage()
        
        # Xóa toàn bộ bảng air_quality_predict_data cho location hiện tại trước khi cập nhật dữ liệu mới
        print(f"[INFO] Xóa dữ liệu cho location {payload['location']} từ bảng air_quality_predict_data trước khi thêm dữ liệu mới...")
        session.query(AirQualityPredictData).filter(
            AirQualityPredictData.location == payload["location"]
        ).delete()
        session.commit()
        print(f"[INFO] Đã xóa dữ liệu cho location {payload['location']} từ bảng air_quality_predict_data.")
        
        # Lưu kết quả dự đoán vào air_quality_predict_data - xử lý theo batch
        print("[INFO] Lưu kết quả dự đoán vào bảng air_quality_predict_data...")
        current_timestamp = pd.to_datetime(payload["timestamp"])
        current_location = payload["location"]

        # Xử lý dữ liệu theo từng batch để giảm sử dụng bộ nhớ
        batch_size = 96  # Xử lý 24 giờ mỗi lần (4 mẫu/giờ * 24 giờ)
        total_predictions = len(predictions)
        total_saved = 0
        
        for batch_start in range(0, total_predictions, batch_size):
            batch_end = min(batch_start + batch_size, total_predictions)
            batch_predictions = predictions[batch_start:batch_end]
            
            # Tạo thời gian cho batch này
            batch_times = [current_timestamp + timedelta(minutes=(i+1) * 15) 
                         for i in range(batch_start, batch_end)]
            
            # Tạo DataFrame cho batch
            batch_data = pd.DataFrame({
                "timestamp": batch_times,
                "temperature": batch_predictions
            })
            
            # Tính trung bình theo giờ để giảm kích thước dữ liệu
            df_hourly = batch_data.set_index("timestamp").resample("H").mean().reset_index()
            
            # Tạo các mục nhập cơ sở dữ liệu
            predict_entries = []
            for _, row in df_hourly.iterrows():
                day_sin, day_cos, year_sin, year_cos = calculate_sin_cos_features(row["timestamp"])
                predict_entries.append(
                    AirQualityPredictData(
                        timestamp=row["timestamp"],
                        location=current_location,  # Sử dụng location từ payload
                        temperature=float(row["temperature"]),
                        day_sin=float(day_sin),
                        day_cos=float(day_cos),
                        year_sin=float(year_sin),
                        year_cos=float(year_cos)
                    )
                )
            
            # Lưu batch vào cơ sở dữ liệu
            if predict_entries:
                session.add_all(predict_entries)
                session.commit()
                total_saved += len(predict_entries)
                print(f"[INFO] Đã lưu batch {batch_start//batch_size + 1}: {len(predict_entries)} mẫu dữ liệu dự đoán.")
            
            # Giải phóng bộ nhớ sau mỗi batch
            del batch_data, df_hourly, predict_entries, batch_predictions, batch_times
            gc.collect()
        
        print(f"[INFO] Tổng cộng đã lưu {total_saved} mẫu dữ liệu dự đoán theo giờ.")
        
        # Đóng phiên làm việc với cơ sở dữ liệu
        session.close()
        session = None
        total_processing_time = time.time() - message_start_time
        performance_monitor.record_processing_time(total_processing_time)
        print(f"[INFO] Đã xử lý xong payload: {payload}.")
        print(f"[INFO] Total processing time: {total_processing_time:.2f} seconds")
        # Giải phóng bộ nhớ cuối cùng
        del predictions, data_for_lstm, last_6_entries, payload
        gc.collect()
        tf.keras.backend.clear_session()
        
        # Log memory at end
        log_memory_usage()

    except Exception as e:
        performance_monitor.record_error()
        print("[ERROR] Error processing message:", e)
        import traceback
        traceback.print_exc()
        
        # Đảm bảo đóng phiên làm việc với cơ sở dữ liệu nếu xảy ra lỗi
        if session:
            session.close()
        
        # Giải phóng tài nguyên khi có lỗi
        tf.keras.backend.clear_session()
        gc.collect()

# MQTT Setup
def setup_mqtt_client():
    MQTT_BROKER = "192.168.1.100"
    MQTT_TOPIC = "sensor/air_quality"
    MQTT_PORT = 1883
    MQTT_KEEPALIVE = 500
    
    print(f"[INFO] Kết nối đến MQTT Broker {MQTT_BROKER}:{MQTT_PORT}")
    try:
        client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
        client.on_message = on_message
        
        # Thêm các hàm callback khác nếu cần - đã sửa để phù hợp với CallbackAPIVersion.VERSION2
        client.on_connect = lambda client, userdata, flags, rc, properties=None: print(f"[INFO] Đã kết nối đến MQTT broker với mã: {rc}")
        client.on_disconnect = lambda client, userdata, rc, properties=None, reason_code=None: print(f"[INFO] Ngắt kết nối khỏi MQTT broker với mã: {rc}")
        
        client.connect(MQTT_BROKER, MQTT_PORT, MQTT_KEEPALIVE)
        client.subscribe(MQTT_TOPIC)
        print(f"[INFO] Đã đăng ký nhận thông tin từ chủ đề: {MQTT_TOPIC}")
        return client
    except Exception as e:
        print(f"[ERROR] Không thể kết nối đến MQTT broker: {e}")
        import sys
        sys.exit(1)

# Main function
def main():
    print("[INFO] Khởi động hệ thống dự đoán chất lượng không khí...")
    
    # Tạo bảng nếu chưa tồn tại
    print("[INFO] Đảm bảo rằng các bảng cơ sở dữ liệu tồn tại...")
    Base.metadata.create_all(bind=engine)
    
    # Kiểm tra kết nối đến cơ sở dữ liệu
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        print("[INFO] Kết nối thành công đến cơ sở dữ liệu PostgreSQL.")
    except Exception as e:
        print(f"[ERROR] Không thể kết nối đến cơ sở dữ liệu: {e}")
        return

    # Thiết lập và chạy MQTT client
    client = setup_mqtt_client()
    # Thêm phần này để khởi động monitoring threads
    print("[INFO] Khởi động performance monitoring threads...")
    
    # Thread để hiển thị thống kê định kỳ
    stats_thread = threading.Thread(target=periodic_stats_reporter, daemon=True)
    stats_thread.start()
    
    # Thread để lưu thống kê vào database
    db_stats_thread = threading.Thread(target=periodic_stats_saver, daemon=True)
    db_stats_thread.start()
    
    print("[INFO] Performance monitoring threads started successfully")
    # Thêm signal handlers
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    try:
        print("[INFO] Bắt đầu vòng lặp MQTT...")
        client.loop_forever()
    except KeyboardInterrupt:
        print("[INFO] Đã nhận lệnh dừng từ người dùng, đang thoát...")
        performance_monitor.print_stats()
        signal_handler(signal.SIGINT, None)
    except Exception as e:
        print(f"[ERROR] Lỗi trong vòng lặp MQTT: {e}")
    finally:
        print("[INFO] Đóng kết nối MQTT...")
        client.disconnect()
        print("[INFO] Hệ thống đã dừng.")

if __name__ == "__main__":
    main()
