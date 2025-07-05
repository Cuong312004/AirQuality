#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <SoftwareSerial.h>
#include <ArduinoJson.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <time.h>

// WiFi cấu hình
const char* ssid = "Hihi";
const char* password = "12345678";

// MQTT cấu hình
const char* mqtt_server = "192.168.162.100";
const char* mqtt_topic = "sensor/air_quality";
const char* alert_topic = "air_quality/alert";

// PMS7003
#define PMS_RX 13   // GPIO13 (NodeMCU D7)
#define PMS_TX 12   // GPIO12 (NodeMCU D6)
SoftwareSerial pmsSerial(PMS_RX, PMS_TX);

// DHT11
#define DHTPIN 0    // GPIO0 (NodeMCU D3)
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// MQ7 & MQ135
#define MQ_ANALOG_PIN A0
#define MQ7_POWER_PIN 5    // GPIO5 (NodeMCU D1)
#define MQ135_POWER_PIN 4  // GPIO4 (NodeMCU D2)

#define RL_MQ7 10.0
#define RL_MQ135 10.0
#define VCC 5.0

float R0_MQ7 = 10.0;
float R0_MQ135 = 10.0;

// LED báo mức chất lượng (D5 = GPIO14)
#define ALERT_LED_PIN 14

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 7 * 3600, 60000);

WiFiClient espClient;
PubSubClient client(espClient);

unsigned long previousMillis = 0;  
const long interval = 30000;  // 30 giây

// Hàm bật/tắt MQ7, MQ135
void powerMQ7(bool on) {
  digitalWrite(MQ7_POWER_PIN, on ? HIGH : LOW);
}

void powerMQ135(bool on) {
  digitalWrite(MQ135_POWER_PIN, on ? HIGH : LOW);
}

// Đọc Rs cảm biến
float readRs(float RL) {
  int adcValue = analogRead(MQ_ANALOG_PIN);
  float Vout = (adcValue / 1023.0) * VCC;
  if (Vout == 0) return -1;
  return RL * (VCC - Vout) / Vout;
}

// Calibrate MQ7
void calibrateMQ7() {
  powerMQ7(true);
  powerMQ135(false);
  delay(5000);
  float rsSum = 0;
  int valid = 0;
  for (int i = 0; i < 50; i++) {
    float rs = readRs(RL_MQ7);
    if (rs > 0) {
      rsSum += rs;
      valid++;
    }
    delay(100);
  }
  if (valid > 0) R0_MQ7 = rsSum / valid;
  powerMQ7(false);
}

// Calibrate MQ135
void calibrateMQ135() {
  powerMQ7(false);
  powerMQ135(true);
  delay(5000);
  float rsSum = 0;
  int valid = 0;
  for (int i = 0; i < 50; i++) {
    float rs = readRs(RL_MQ135);
    if (rs > 0) {
      rsSum += rs;
      valid++;
    }
    delay(100);
  }
  if (valid > 0) R0_MQ135 = rsSum / valid;
  powerMQ135(false);
}

// Hàm tính CO ppm
float getCOppm(float Rs, float temp, float hum) {
  if (Rs <= 0) return -1;
  float compensated_resistance = Rs / (-0.0122 * temp - 0.00609 * hum + 1.7086);
  float mq7_ratio = 100.0 * compensated_resistance / R0_MQ7;
  float ratio_ln = mq7_ratio / 100.0;
  float co_ppm = exp(-0.685 - 2.679 * ratio_ln - 0.488 * ratio_ln * ratio_ln - 0.078 * ratio_ln * ratio_ln * ratio_ln);
  return co_ppm;
}

const float MQ135_A = -0.0105;
const float MQ135_B = -0.0073;
const float MQ135_C = 1.2;

// Rs bù trừ cho MQ135
float getCompensatedRs_MQ135(float Rs, float temp, float hum) {
  if (Rs <= 0) return -1;
  float compensation_factor = MQ135_A * temp + MQ135_B * hum + MQ135_C;
  if (compensation_factor == 0) return Rs;
  return Rs / compensation_factor;
}

float getNO2ppm(float Rs_comp) {
  if (Rs_comp <= 0) return -1;
  float ratio = Rs_comp / R0_MQ135;
  float a = 3.14;
  float b = -1.15;
  return a * pow(ratio, b);
}

float getSO2ppm(float Rs_comp) {
  if (Rs_comp <= 0) return -1;
  float ratio = Rs_comp / R0_MQ135;
  float a = 3.5;
  float b = -1.2;
  return a * pow(ratio, b);
}

// Hàm đọc dữ liệu PMS7003 an toàn
bool readPMS7003(int &pm25, int &pm10) {
  pm25 = -1;
  pm10 = -1;

  if (pmsSerial.available() >= 32) {
    uint8_t buf[32];
    for (int i = 0; i < 32; i++) {
      buf[i] = pmsSerial.read();
    }

    if (buf[0] == 0x42 && buf[1] == 0x4D) {
      pm25 = (buf[12] << 8) | buf[13];  // PM2.5 ở byte 12-13
      pm10 = (buf[14] << 8) | buf[15];  // PM10 ở byte 14-15
      return true;
    } else {
      Serial.println("PMS7003 invalid header, clearing buffer");
      while (pmsSerial.available()) pmsSerial.read();  // Xóa buffer để đồng bộ lại
      return false;
    }
  }
  return false; // chưa đủ dữ liệu
}

void setup_wifi() {
  Serial.print("Connecting to WiFi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" connected");
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.println("MQTT message received.");
  String msg;
  for (unsigned int i = 0; i < length; i++) {
    msg += (char)payload[i];
  }
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("]: ");
  Serial.println(msg);

  if (String(topic) == alert_topic) {
    StaticJsonDocument<128> doc;
    DeserializationError error = deserializeJson(doc, msg);
    if (!error) {
      int air_quality = doc["air_quality"] | 0;
      Serial.print("Received air_quality alert: ");
      Serial.println(air_quality);

      if (air_quality == 1) {
        Serial.println("Alert level: 1 (HAZARDOUS) detected - Turning ON LED.");
        digitalWrite(ALERT_LED_PIN, HIGH);
      } else if (air_quality == 3) {
        Serial.println("Alert level: 3 (POOR) detected - Turning ON LED.");
        digitalWrite(ALERT_LED_PIN, HIGH);
      } else {
        Serial.println("Alert level normal - Turning OFF LED.");
        digitalWrite(ALERT_LED_PIN, LOW);
      }
    } else {
      Serial.println("Failed to parse alert JSON");
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");
    if (client.connect("ESP8266Client")) {
      Serial.println("connected");
      client.subscribe(alert_topic);
      Serial.println("Subscribed to alert topic.");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

String getFormattedTimestamp() {
  timeClient.update();
  time_t rawTime = timeClient.getEpochTime();
  struct tm* timeinfo = localtime(&rawTime);
  char timestamp[20];
  sprintf(timestamp, "%04d-%02d-%02d %02d:%02d:%02d",
          timeinfo->tm_year + 1900,
          timeinfo->tm_mon + 1,
          timeinfo->tm_mday,
          timeinfo->tm_hour,
          timeinfo->tm_min,
          timeinfo->tm_sec);
  return String(timestamp);
}

void sendSensorData() {
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();

  // MQ7
  powerMQ7(true);
  powerMQ135(false);
  delay(5000);
  int adcMQ7 = analogRead(MQ_ANALOG_PIN);
  float Rs_MQ7 = readRs(RL_MQ7);
  float co = getCOppm(Rs_MQ7, temp, hum);
  Serial.print("MQ7 ADC: "); Serial.println(adcMQ7);
  Serial.print("MQ7 Rs: "); Serial.println(Rs_MQ7, 2);
  Serial.print("CO ppm: "); Serial.println(co, 2);
  powerMQ7(false);

  // MQ135
  powerMQ7(false);
  powerMQ135(true);
  delay(5000);
  int adcMQ135 = analogRead(MQ_ANALOG_PIN);
  float Rs_MQ135 = readRs(RL_MQ135);
  float Rs_MQ135_comp = getCompensatedRs_MQ135(Rs_MQ135, temp, hum);
  float no2 = getNO2ppm(Rs_MQ135_comp);
  float so2 = getSO2ppm(Rs_MQ135_comp);
  Serial.print("MQ135 ADC: "); Serial.println(adcMQ135);
  Serial.print("MQ135 Rs: "); Serial.println(Rs_MQ135, 2);
  Serial.print("MQ135 Rs compensated: "); Serial.println(Rs_MQ135_comp, 2);
  Serial.print("NO2 ppm: "); Serial.println(no2, 2);
  Serial.print("SO2 ppm: "); Serial.println(so2, 2);
  powerMQ135(false);

  int pm25 = -1, pm10 = -1;
  if (!readPMS7003(pm25, pm10)) {
    Serial.println("PMS7003 data not ready or invalid");
  }

  bool validData = true;
  if (isnan(temp) || isnan(hum)) {
    Serial.println("Error: DHT11 temperature or humidity is NaN");
    validData = false;
  }
  if (co <= 0) {
    Serial.println("Error: CO <= 0");
    validData = false;
  }
  if (no2 <= 0) {
    Serial.println("Error: NO2 <= 0");
    validData = false;
  }
  if (so2 <= 0) {
    Serial.println("Error: SO2 <= 0");
    validData = false;
  }
  if (pm25 < 0 || pm10 < 0) {
    Serial.println("Error: PM2.5 or PM10 < 0");
    validData = false;
  }

  if (validData) {
    StaticJsonDocument<256> doc;
    char buf[10];

    doc["timestamp"] = getFormattedTimestamp();
    doc["location"] = "thu_duc";

    dtostrf(temp, 6, 2, buf);
    doc["temperature"] = buf;
    dtostrf(hum, 6, 2, buf);
    doc["humidity"] = buf;

    doc["pm25"] = pm25;
    doc["pm10"] = pm10;

    dtostrf(no2, 6, 2, buf);
    doc["no2"] = buf;
    dtostrf(so2, 6, 2, buf);
    doc["so2"] = buf;
    dtostrf(co, 6, 2, buf);
    doc["co"] = buf;

    char payload[256];
    serializeJson(doc, payload);
    client.publish(mqtt_topic, payload);

    Serial.print("Published: ");
    Serial.println(payload);
  } else {
    Serial.println("Invalid sensor data, not publishing");
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(ALERT_LED_PIN, OUTPUT);
  digitalWrite(ALERT_LED_PIN, LOW);

  dht.begin();
  pmsSerial.begin(9600);

  pinMode(MQ7_POWER_PIN, OUTPUT);
  pinMode(MQ135_POWER_PIN, OUTPUT);
  digitalWrite(MQ7_POWER_PIN, LOW);
  digitalWrite(MQ135_POWER_PIN, LOW);

  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);

  timeClient.begin();

  calibrateMQ7();
  calibrateMQ135();
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop();

  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    sendSensorData();
  }
}
