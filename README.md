# 中華電信 IoT 服務平台 Open API
 - ***karafuto/*** 為 Open API 主程式，包含 **RESTful**, **MQTT**, **WebSocket** 的原始程式碼，另外可以從 *[karafuto/src/test/java/com/cht/iot/service/api](https://github.com/YunYenWang/IoT/tree/master/karafuto/src/test/java/com/cht/iot/service/api)* 中獲取上述 3 種 Open API 的使用範例原始程式碼。
 - ***karafuto-demo-pi/*** 為基於 **Raspberry Pi 3** 平台的範例程式，可控制 GPIO 燈泡與移動感知器，可偵測人員移動即時進行攝影 (Pi Camera) 並上傳至 IoT 服務平台儲存，亦提供透過 BLE 控制小米手環振動提醒的功能。  
### 開發步驟
 1. 請向中華電信申請 IoT 服務平台開發者帳號。(e-mail: rickwang@cht.com.tw) 
 2. 登入中華電信 IoT 服務平台後，請建立專案，並從專案詳細內容中的『權限資料』獲取 Open API 的 API KEY。
 3. 請預先安裝 git, JDK 7與 gradle 編譯工具。
 4. 透過 git clone https://github.com/YunYenWang/IoT.git 獲取 Open API 主程式。
 5. cd karafuto 後下達 gradle eclipse 可產生 Eclipse 專案，開發者可透過 Eclipse 匯入專案進行研發驗證測試。
 6. 若要編譯 Raspberry Pi 3 的範例程式，請 cd karafuto-demo-pi 後下達 gradle distTar 即可在 build/distributions 獲取可執行的包裝環境，上傳至 Pi 平台解壓縮後，直接執行 bin/karafuto-demo-pi 即可啟動應用。
### Open API RESTful 通訊協定
  http://iot.cht.com.tw/iot/developer/device
  