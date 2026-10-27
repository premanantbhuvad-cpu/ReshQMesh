#include <WiFi.h>
#include <esp_now.h>
#include <BluetoothSerial.h>
#include <string.h>

#define ESPNOW_CHANNEL 6
#define NODE_NAME "NODE_2"
#define MAX_DATA_LEN 240
#define BLUETOOTH_MESSAGE_TIMEOUT 50

// --------------------------------------------------
// BLUETOOTH
// --------------------------------------------------

BluetoothSerial SerialBT;

// --------------------------------------------------
// NODE 1 MAC ADDRESS
// This is the destination when Node 2 sends to Node 1.
// --------------------------------------------------

uint8_t peerAddress[] = {
  0x00, 0x70, 0x07, 0xE3, 0x11, 0xE0
};

// --------------------------------------------------
// ESP-NOW RECEIVE BUFFER
// --------------------------------------------------

uint8_t espNowBuffer[MAX_DATA_LEN];
uint8_t espNowSourceMac[6];

volatile int espNowLength = 0;
volatile bool espNowMessageReady = false;

portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;

// --------------------------------------------------
// BLUETOOTH RECEIVE BUFFER
// --------------------------------------------------

uint8_t bluetoothBuffer[MAX_DATA_LEN];

int bluetoothLength = 0;
unsigned long lastBluetoothByteTime = 0;

// --------------------------------------------------
// PRINT MAC ADDRESS
// --------------------------------------------------

void printMac(const uint8_t* mac) {

  for (int i = 0; i < 6; i++) {
    Serial.printf("%02X", mac[i]);

    if (i < 5) {
      Serial.print(":");
    }
  }
}

// --------------------------------------------------
// ESP-NOW SEND CALLBACK
// --------------------------------------------------

void onDataSent(
    const wifi_tx_info_t* txInfo,
    esp_now_send_status_t status
) {

  (void)txInfo;

  Serial.print("Node 2 -> Node 1 delivery status: ");

  if (status == ESP_NOW_SEND_SUCCESS) {
    Serial.println("SUCCESS");
  } else {
    Serial.println("FAILED");
  }
}

// --------------------------------------------------
// ESP-NOW RECEIVE CALLBACK
// --------------------------------------------------

void onDataReceived(
    const esp_now_recv_info_t* info,
    const uint8_t* data,
    int len
) {

  if (info == nullptr ||
      info->src_addr == nullptr ||
      data == nullptr ||
      len <= 0) {
    return;
  }

  if (len > MAX_DATA_LEN) {
    len = MAX_DATA_LEN;
  }

  portENTER_CRITICAL(&mux);

  memcpy(
      espNowBuffer,
      data,
      len
  );

  memcpy(
      espNowSourceMac,
      info->src_addr,
      6
  );

  espNowLength = len;
  espNowMessageReady = true;

  portEXIT_CRITICAL(&mux);
}

// --------------------------------------------------
// SEND BLUETOOTH DATA THROUGH ESP-NOW
// Phone -> Node 2 -> Node 1
// --------------------------------------------------

void sendBluetoothData() {

  if (bluetoothLength <= 0) {
    return;
  }

  int messageLength = bluetoothLength;

  Serial.println();
  Serial.println("========== NODE 2: BLUETOOTH -> ESP-NOW ==========");

  Serial.print("Message: ");

  for (int i = 0; i < messageLength; i++) {
    Serial.print((char)bluetoothBuffer[i]);
  }

  Serial.println();

  Serial.print("Destination Node 1 MAC: ");
  printMac(peerAddress);
  Serial.println();

  esp_err_t result = esp_now_send(
      peerAddress,
      bluetoothBuffer,
      messageLength
  );

  if (result == ESP_OK) {
    Serial.println("ESP-NOW packet queued successfully.");
  } else {
    Serial.print("ESP-NOW ERROR: ");
    Serial.println(result);
  }

  Serial.println("===================================================");

  bluetoothLength = 0;
}

// --------------------------------------------------
// FORWARD ESP-NOW DATA TO BLUETOOTH
// Node 1 -> Node 2 -> Phone
// --------------------------------------------------

void forwardEspNowToBluetooth() {

  uint8_t localBuffer[MAX_DATA_LEN];
  uint8_t localSourceMac[6];

  int localLength = 0;

  portENTER_CRITICAL(&mux);

  if (!espNowMessageReady) {
    portEXIT_CRITICAL(&mux);
    return;
  }

  localLength = espNowLength;

  memcpy(
      localBuffer,
      espNowBuffer,
      localLength
  );

  memcpy(
      localSourceMac,
      espNowSourceMac,
      6
  );

  espNowMessageReady = false;

  portEXIT_CRITICAL(&mux);

  Serial.println();
  Serial.println("========== NODE 2: ESP-NOW PACKET RECEIVED ==========");

  Serial.print("Sender MAC: ");
  printMac(localSourceMac);
  Serial.println();

  Serial.print("Message length: ");
  Serial.println(localLength);

  Serial.print("Message: ");

  for (int i = 0; i < localLength; i++) {
    Serial.print((char)localBuffer[i]);
  }

  Serial.println();

  if (SerialBT.hasClient()) {

    size_t sent = SerialBT.write(
        localBuffer,
        localLength
    );

    if (sent == (size_t)localLength) {
      Serial.println("Bluetooth forwarding SUCCESS.");
    } else {
      Serial.println("Bluetooth forwarding FAILED.");
    }

  } else {

    Serial.println("No Bluetooth phone connected to Node 2.");

  }

  Serial.println("======================================================");
}

// --------------------------------------------------
// SETUP
// --------------------------------------------------

void setup() {

  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("==========================================");
  Serial.println("         ResQMesh NODE 2 BRIDGE");
  Serial.println("==========================================");

  Serial.print("Node name: ");
  Serial.println(NODE_NAME);

  // ------------------------------------------------
  // START WI-FI FIRST
  // ------------------------------------------------

  if (!WiFi.mode(WIFI_STA)) {
    Serial.println("ERROR: Failed to start Wi-Fi station mode.");
    return;
  }

  while (!WiFi.STA.started()) {
    delay(100);
  }

  // Set channel after the station has started.
  int channelResult = WiFi.setChannel(ESPNOW_CHANNEL);

  if (channelResult != 0) {
    Serial.print("ERROR: Failed to set Wi-Fi channel. Code: ");
    Serial.println(channelResult);
    return;
  }

  Serial.print("Own MAC (Node 2): ");
  Serial.println(WiFi.macAddress());

  Serial.print("Actual Wi-Fi channel: ");
  Serial.println(WiFi.channel());

  // ------------------------------------------------
  // START BLUETOOTH CLASSIC
  // ------------------------------------------------

  if (!SerialBT.begin("ResQMesh_NODE_2")) {
    Serial.println("ERROR: Bluetooth initialization failed.");
    return;
  }

  Serial.println("Bluetooth started successfully.");
  Serial.println("Bluetooth device name: ResQMesh_NODE_2");

  // ------------------------------------------------
  // INITIALIZE ESP-NOW
  // ------------------------------------------------

  if (esp_now_init() != ESP_OK) {
    Serial.println("ERROR: ESP-NOW initialization failed.");
    return;
  }

  Serial.println("ESP-NOW initialized successfully.");

  // ------------------------------------------------
  // REGISTER CALLBACKS
  // ------------------------------------------------

  esp_now_register_send_cb(onDataSent);
  esp_now_register_recv_cb(onDataReceived);

  // ------------------------------------------------
  // ADD NODE 1 AS PEER
  // ------------------------------------------------

  esp_now_peer_info_t peerInfo = {};

  memcpy(
      peerInfo.peer_addr,
      peerAddress,
      6
  );

  peerInfo.channel = ESPNOW_CHANNEL;
  peerInfo.encrypt = false;

  if (esp_now_add_peer(&peerInfo) != ESP_OK) {
    Serial.println("ERROR: Failed to add Node 1 peer.");
    return;
  }

  Serial.println("Node 1 peer added successfully.");

  Serial.print("Destination peer MAC (Node 1): ");
  printMac(peerAddress);
  Serial.println();

  Serial.println();
  Serial.println("NODE 2 BRIDGE READY.");
  Serial.println("Phone -> Bluetooth -> Node 2 -> ESP-NOW -> Node 1");
  Serial.println("Node 2 is ready to receive ESP-NOW messages.");
  Serial.println();
}

// --------------------------------------------------
// MAIN LOOP
// --------------------------------------------------

void loop() {

  // -----------------------------------------------
  // RECEIVE DATA FROM PHONE OVER BLUETOOTH
  // -----------------------------------------------

  while (SerialBT.available()) {

    int incomingByte = SerialBT.read();

    if (incomingByte < 0) {
      continue;
    }

    if (bluetoothLength < MAX_DATA_LEN) {

      bluetoothBuffer[bluetoothLength++] =
          (uint8_t)incomingByte;

      lastBluetoothByteTime = millis();

    } else {

      Serial.println("Bluetooth message too long.");
      bluetoothLength = 0;
    }
  }

  // -----------------------------------------------
  // WAIT FOR THE END OF THE BLUETOOTH MESSAGE
  // -----------------------------------------------

  if (
      bluetoothLength > 0 &&
      millis() - lastBluetoothByteTime >=
          BLUETOOTH_MESSAGE_TIMEOUT
  ) {
    sendBluetoothData();
  }

  // -----------------------------------------------
  // FORWARD ESP-NOW MESSAGES TO THE PHONE
  // -----------------------------------------------

  forwardEspNowToBluetooth();

  delay(5);
}