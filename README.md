# 🛡️ AntiViaBedrock by Tiack

Plugin chống gian lận chuyên dụng dành cho máy chủ **Minecraft 1.21.11 Leaf** (Winds-Studio/Leaf), ngăn chặn triệt để người chơi sử dụng **ViaFabricPlus** hoặc module **ViaBedrock** để giả mạo client Bedrock (port qua Geyser/Floodgate) nhằm bay lượn (fly), bypass anti-cheat và raid base.

---

## 🔍 1. Phân Tích Kỹ Thuật: Cách ViaBedrock & ViaFabricPlus Giả Mạo Bedrock

Từ mã nguồn giải mã của `ViaFabricPlus` và `ViaBedrock`:

### A. Chuỗi phần cứng bị hardcode (`DeviceModel`)
* Trong file `SkinProvider.java` (dòng 98) của ViaBedrock:
  ```java
  claims.put("DeviceModel", "MS-7E51 Micro-Star International Co., Ltd. (Unknown)");
  ```
  Tác giả ViaBedrock (RaphiMC) đã hardcode cố định model bo mạch chủ của máy cá nhân (`MS-7E51`). Mọi client ViaFabricPlus/ViaBedrock chưa qua chỉnh sửa sâu đều gửi chuỗi này trong ClientData JWT.

### B. Giả lập nền tảng sai lệch (`DeviceOS = Win32`)
* Trong `SkinProvider.java` (dòng 99):
  ```java
  claims.put("DeviceOS", BuildPlatform.Win32.getValue()); // ID 8
  ```
  Game thủ Minecraft Bedrock trên Windows chính thức (Microsoft Store) **luôn luôn** chạy trên nền tảng **UWP / Windows 10 (ID 7)**. Nền tảng Win32 (ID 8) là bản thử nghiệm cổ xưa không tồn tại trong phiên bản bán lẻ chính thức. Do đó mọi kết nối Bedrock báo `Win32 (ID 8)` đều là client giả lập/ViaBedrock.

### C. Tầm nhìn phi thực tế (`MaxViewDistance = 96`)
* Trong `SkinProvider.java` (dòng 105):
  ```java
  claims.put("MaxViewDistance", 96);
  ```
  Game Bedrock thông thường chỉ hỗ trợ slider tối đa 16-72 chunk (tối đa 80 trên GPU cao cấp). Việc gửi 96 chunk là chữ ký nhận diện 100% của ViaBedrock.

### D. Tỷ lệ giao diện bất thường (`GuiScale = -1`)
* Trong `SkinProvider.java` (dòng 89):
  ```java
  claims.put("GuiScale", -1);
  ```
  Bedrock vanilla không bao giờ gửi giá trị âm cho GuiScale.

### E. Thuật toán tạo XUID offline (`FNV1-64 Hash`)
* Trong `LoginPackets.java` (dòng 140-141):
  ```java
  final long rawXuid = Fnv1.fnv1_64(javaUsername.getBytes(StandardCharsets.UTF_8));
  final String xuid = String.valueOf(Math.abs(rawXuid));
  ```
  Khi người chơi dùng ViaBedrock chế độ offline/cracked (đa số cheater), XUID gửi lên server chính là giá trị tuyệt đối của mã băm FNV1-64 từ tên Java của họ.

### F. Cách ViaFabricPlus vượt qua Anti-ViaFabricPlus thông thường trên cổng Java
* Trong `ViaFabricPlusProtocol.java` (dòng 64):
  ViaFabricPlus chủ động loại bỏ kênh `fabric:extended_game_state_particle_effect_sync` khỏi gói tin `minecraft:register` để che giấu dấu vết Fabric.
* Hackers chuyển sang cổng Bedrock để anti-cheat tưởng là Bedrock player và nới lỏng kiểm tra chuyển động (fly, speed, no-clip).

---

## ⚡ 2. Cơ Chế Xử Phạt Của AntiViaBedrock

* **Lần 1:** Kick người chơi ngay tại `AsyncPlayerPreLoginEvent` với thông báo:
  > *"Vui lòng dùng bản Bedrock chính thức để tham gia server! (Cảnh cáo: Nếu tái phạm lần 2 sẽ bị cấm vĩnh viễn)"*
* **Lần 2:** **BAN** người chơi vĩnh viễn (hoặc theo lệnh cấu hình) và lưu trữ trạng thái vào cơ sở dữ liệu `data.yml`.
* **Cảnh báo Staff:** Thông báo tức thì cho các Admin/Staff có quyền `antiviabedrock.alerts`.
* **Hỗ trợ Discord Webhook:** Gửi embed cảnh báo trực tiếp về kênh Discord quản trị.

---

## 🛠️ 3. Tự Động Tăng Phiên Bản Khi Build (`x.x.x`)

Mỗi khi chạy lệnh build Gradle:
```bash
gradle jar
```
hoặc
```bash
gradlew.bat jar
```
Build script sẽ:
1. Đọc file `version.properties`.
2. Tự động tăng `version.patch` lên 1 (ví dụ: `1.0.0` ➔ `1.0.1` ➔ `1.0.2`...).
3. Cập nhật `version` trong `plugin.yml` và tạo file jar mới tương ứng (ví dụ: `AntiViaBedrock-1.0.3.jar`).

---

## 📌 4. Lệnh & Quyền Hạn

| Lệnh | Quyền | Mô tả |
| :--- | :--- | :--- |
| `/avb reload` | `antiviabedrock.admin` | Nạp lại file `config.yml` và `data.yml` |
| `/avb check <player>` | `antiviabedrock.admin` | Kiểm tra Device OS, Model, XUID, Brand của người chơi |
| `/avb reset <player>` | `antiviabedrock.admin` | Xóa lịch sử vi phạm / ân xá cho người chơi |
| `/avb list` | `antiviabedrock.admin` | Xem danh sách các đối tượng đã bị ghi nhận vi phạm |

* `antiviabedrock.alerts`: Nhận thông báo khi phát hiện cheater cố tình kết nối fake Bedrock.
* `antiviabedrock.bypass`: Bỏ qua kiểm tra (dành cho Admin test).

---

## 📁 5. Cài Đặt

1. Copy file `AntiViaBedrock-x.x.x.jar` trong thư mục `build/libs/` vào thư mục `plugins/`
2. Khởi động lại máy chủ (Server có cài `Floodgate` và/hoặc `Geyser-Spigot`).
3. Tùy chỉnh thông báo và hành động trong `plugins/AntiViaBedrock/config.yml` nếu muốn.
