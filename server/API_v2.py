from fastapi import FastAPI, Query
import psycopg2
from pydantic import BaseModel
from typing import List, Dict, Any, Optional

# Define the Location model to match your mobile app's needs
class Location(BaseModel):
    id: str
    name: str

app = FastAPI()

# Cấu hình kết nối đến PostgreSQL
DB_USER = "user"
DB_PASSWORD = "pass"
DB_HOST = "192.168.1.100"
DB_PORT = "5432"
DB_NAME = "air_quality"

def get_db_connection():
    return psycopg2.connect(
        dbname=DB_NAME, user=DB_USER, password=DB_PASSWORD, host=DB_HOST, port=DB_PORT
    )

@app.get("/latest_air_quality_data")
def get_latest_air_quality_data(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy dòng dữ liệu gần nhất từ air_quality_data với temperature làm tròn 2 chữ số thập phân"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = """
        SELECT timestamp, location, ROUND(temperature::numeric, 2) AS temperature, 
               humidity, pm25, pm10, no2, so2, co, air_quality  
        FROM air_quality_data 
    """
    
    # Thêm điều kiện location nếu được cung cấp
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 1"
    
    cursor.execute(query, params)
    row = cursor.fetchone()
    cursor.close()
    conn.close()
    
    # Trả về mảng giá trị
    return [list(row)] if row else []

@app.get("/all_air_quality_predict_data")
def get_all_air_quality_predict_data(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy tất cả dữ liệu timestamp, location, temperature từ air_quality_predict_data với temperature làm tròn 2 chữ số thập phân"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(temperature::numeric, 2) FROM air_quality_predict_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Trả về mảng các giá trị
    return [list(row) for row in rows]

@app.get("/latest_12_air_quality_predict")
def get_latest_12_air_quality_predict(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 dòng gần nhất từ air_quality_predict với temperature làm tròn 2 chữ số thập phân"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(temperature::numeric, 2) FROM air_quality_predict"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Trả về mảng các giá trị
    return [list(row) for row in rows]

# Cập nhật tất cả các API để thêm location
@app.get("/latest_12_humidity")
def get_latest_12_humidity(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của humidity"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(humidity::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

@app.get("/latest_12_pm25")
def get_latest_12_pm25(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của PM2.5"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(pm25::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

@app.get("/latest_12_pm10")
def get_latest_12_pm10(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của PM10"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(pm10::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

@app.get("/latest_12_no2")
def get_latest_12_no2(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của NO2"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(no2::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

@app.get("/latest_12_so2")
def get_latest_12_so2(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của SO2"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(so2::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

@app.get("/latest_12_co")
def get_latest_12_co(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của CO"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT timestamp, location, ROUND(co::numeric, 2) FROM air_quality_data"
    
    params = []
    if location:
        query += " WHERE location = %s"
        params.append(location)
    
    query += " ORDER BY timestamp DESC LIMIT 12"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return [list(row) for row in rows]

# API để lấy tất cả dữ liệu thông số trong một lần gọi
@app.get("/latest_12_all_parameters")
def get_latest_12_all_parameters(location: Optional[str] = Query(None, description="Filter by location")):
    """Lấy 12 điểm dữ liệu cuối cùng của tất cả các thông số"""
    # Truyền location parameter vào các hàm gọi 
    result = {
        "humidity": get_latest_12_humidity(location),
        "pm25": get_latest_12_pm25(location),
        "pm10": get_latest_12_pm10(location), 
        "no2": get_latest_12_no2(location),
        "so2": get_latest_12_so2(location),
        "co": get_latest_12_co(location)
    }
    
    return result

# Thêm API mới để lấy danh sách các location có trong hệ thống
@app.get("/locations")
def get_locations():
    """Lấy danh sách tất cả các location có trong hệ thống"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT location FROM air_quality_data ORDER BY location;")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Trả về danh sách các location
    return [row[0] for row in rows]
    
    
# Add the new endpoint to match your mobile app's API request
@app.get("/available_locations", response_model=List[Location])
def get_available_locations():
    """Lấy danh sách tất cả các location có trong hệ thống dưới dạng objects"""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT location FROM air_quality_data ORDER BY location;")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Convert to Location objects with id and name
    locations = []
    for row in rows:
        location_name = row[0]
        # Using the location name as both id and name, but you can modify this if needed
        locations.append(Location(id=location_name, name=location_name))
    
    return locations
