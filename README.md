# 🛡️ AntiViaBedrock by Tiack

A specialized anti-cheat plugin for **Minecraft 1.21.11 Leaf** servers (Winds-Studio/Leaf). It aims to prevent players from using **ViaFabricPlus** or the **ViaBedrock** module to spoof a Bedrock client (connecting through Geyser/Floodgate) to fly, bypass anti-cheat checks, or raid bases.

---

## 🔍 1. Technical Analysis: How ViaBedrock and ViaFabricPlus Spoof Bedrock

Based on the decompiled source code of `ViaFabricPlus` and `ViaBedrock`:

### A. Hardcoded hardware string (`DeviceModel`)
* In `ViaBedrock`’s `SkinProvider.java` (line 98):
  ```java
  claims.put("DeviceModel", "MS-7E51 Micro-Star International Co., Ltd. (Unknown)");
  ```
  The ViaBedrock author (RaphiMC) hardcoded a motherboard model from their personal computer (`MS-7E51`). All unmodified ViaFabricPlus/ViaBedrock clients send this string in the ClientData JWT.

### B. Incorrectly simulated platform (`DeviceOS = Win32`)
* In `SkinProvider.java` (line 99):
  ```java
  claims.put("DeviceOS", BuildPlatform.Win32.getValue()); // ID 8
  ```
  The official Minecraft Bedrock game on Windows (Microsoft Store) **always** runs on the **UWP / Windows 10 platform (ID 7)**. Win32 (ID 8) is an old, experimental platform that does not exist in the official retail release. Therefore, any Bedrock connection reporting `Win32 (ID 8)` is an emulated client/ViaBedrock.

### C. Unrealistic view distance (`MaxViewDistance = 96`)
* In `SkinProvider.java` (line 105):
  ```java
  claims.put("MaxViewDistance", 96);
  ```
  Typical Bedrock clients support a view-distance slider of only 16–72 chunks (up to 80 on high-end GPUs). Sending 96 chunks is a 100% identifying signature of ViaBedrock.

### D. Unusual interface scale (`GuiScale = -1`)
* In `SkinProvider.java` (line 89):
  ```java
  claims.put("GuiScale", -1);
  ```
  Vanilla Bedrock never sends a negative `GuiScale` value.

### E. Offline XUID generation algorithm (`FNV1-64 Hash`)
* In `LoginPackets.java` (lines 140–141):
  ```java
  final long rawXuid = Fnv1.fnv1_64(javaUsername.getBytes(StandardCharsets.UTF_8));
  final String xuid = String.valueOf(Math.abs(rawXuid));
  ```
  When a player uses ViaBedrock in offline/cracked mode (as many cheaters do), the XUID sent to the server is the absolute value of the FNV1-64 hash of their Java username.

### F. How ViaFabricPlus bypasses ordinary anti-ViaFabricPlus checks on the Java port
* In `ViaFabricPlusProtocol.java` (line 64), ViaFabricPlus deliberately removes the `fabric:extended_game_state_particle_effect_sync` channel from the `minecraft:register` packet to hide traces of Fabric.
* Hackers switch to the Bedrock port so the anti-cheat treats them as Bedrock players and relaxes movement checks (fly, speed, no-clip).

---

## ⚡ 2. AntiViaBedrock Enforcement Mechanism

* **First offense:** Kicks the player during `AsyncPlayerPreLoginEvent` with this message:
  > *“Please use the official Bedrock version to join the server! (Warning: A second offense will result in a permanent ban.)”*
* **Second offense:** **Bans** the player permanently (or according to the configured command) and stores the status in the `data.yml` database.
* **Staff alerts:** Immediately notifies admins/staff with the `antiviabedrock.alerts` permission.
* **Discord webhook support:** Sends an alert embed directly to the staff Discord channel.

---

## 🛠️ 3. Automatically Incrementing the Version When Building (`x.x.x`)

Each time you run a Gradle build command:
```bash
gradle jar
```
or
```bash
gradlew.bat jar
```
the build script will:
1. Read `version.properties`.
2. Automatically increment `version.patch` by 1 (for example, `1.0.0` ➔ `1.0.1` ➔ `1.0.2`...).
3. Update the `version` in `plugin.yml` and create the corresponding JAR file (for example, `AntiViaBedrock-1.0.3.jar`).

---

## 📌 4. Commands and Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/avb reload` | `antiviabedrock.admin` | Reloads `config.yml` and `data.yml` |
| `/avb check <player>` | `antiviabedrock.admin` | Checks the player’s DeviceOS, DeviceModel, XUID, and brand |
| `/avb reset <player>` | `antiviabedrock.admin` | Clears the player’s violation history / pardons the player |
| `/avb list` | `antiviabedrock.admin` | Shows the list of players recorded as having violated the rules |

* `antiviabedrock.alerts`: Receives alerts when a suspected cheater attempts to connect using a spoofed Bedrock client.
* `antiviabedrock.bypass`: Bypasses checks (for admin testing).

---

## 📁 5. Installation

1. Copy `AntiViaBedrock-x.x.x.jar` from the `build/libs/` folder into the `plugins/` folder.
2. Restart the server (which should have `Floodgate` and/or `Geyser-Spigot` installed).
3. If desired, customize messages and actions in `plugins/AntiViaBedrock/config.yml`.
