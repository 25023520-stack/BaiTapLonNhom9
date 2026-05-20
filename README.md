# BaiTapLonNhom9

## Cau truc source code

Project duoc to chuc theo huong client-server va tach ro giao dien, nghiep vu, model dung chung:

```text
src/
  main/
    java/com/auction/system/
      client/
        App.java                    # bootstrap client, chon DEMO/SEPARATE mode
        Launcher.java               # entry point JavaFX
        context/
          AppContext.java           # session client, socket client, current user
        controller/
          LoginController.java      # controller man hinh dang nhap
          RegisterController.java   # controller man hinh dang ky
          AuctionController.java    # controller man hinh dau gia
        network/
          AuctionClient.java        # TCP client giao tiep voi server
      server/
        ServerMain.java             # entry point server
        controller/
          AuthController.java       # xu ly request auth
          AuctionController.java    # xu ly request dau gia
        manager/
          AuthManager.java          # nghiep vu tai khoan
          UserManager.java          # quan ly user
          ItemManager.java          # quan ly item
          AuctionManager.java       # nghiep vu dau gia
        network/
          AuctionServer.java        # TCP server
          ClientHandler.java        # xu ly tung client
        dao/
          UserDAO.java              # truy cap du lieu user
        database/
          Database.java             # ket noi/co so du lieu
      model/
        user/                       # User, Admin, Seller, Bidder
        item/                       # Item, Electronics, Art, Vehicle
        auction/                    # Auction, Bid, AuctionStatus
      common/
        payload/                    # payload client-server
        json/                       # GsonProvider dung chung
      factory/
        ItemFactory.java            # Factory Method tao item
      exception/                    # custom exception
    resources/com/auction/system/client/view/
      Login.fxml
      Register.fxml
      Auction.fxml
      Styles.css
  test/
    java/com/auction/system/...
```

## Doi chieu voi yeu cau de bai

- `client/` + `resources/.../view/`: phan Frontend JavaFX, FXML, MVC.
- `server/`: phan Backend, manager, controller, TCP socket server.
- `model/`: cac entity dung chung cho client va server.
- `common/payload/`: giao thuc truyen du lieu client-server.
- `factory/`: ap dung Factory Method.
- `exception/`: custom exception cho nghiep vu.

## Cach chay

## Cau hinh admin bootstrap

- Server khong con hardcode tai khoan admin mac dinh.
- Neu muon tu dong tao admin khi khoi dong server, dat bien moi truong `ADMIN_BOOTSTRAP_PASSWORD`.
- Neu tai khoan bootstrap admin da ton tai trong database, server se dong bo lai `username/email/full_name/password` tu cac bien moi truong tren moi lan khoi dong.
- Cac bien co the dung:
  - `ADMIN_BOOTSTRAP_USERNAME`
  - `ADMIN_BOOTSTRAP_PASSWORD`
  - `ADMIN_BOOTSTRAP_FULL_NAME`
  - `ADMIN_BOOTSTRAP_ID`
- `ADMIN_BOOTSTRAP_EMAIL` la tuy chon. Neu bo trong, server tu sinh email noi bo theo mau `<username>@bootstrap.local`.
- Da co mau trong `.env.example`.

### Chay server rieng

```powershell
set ADMIN_BOOTSTRAP_PASSWORD=doi-mat-khau-nay
mvn -DskipTests package
java -jar target/server.jar
```

### Chay client JavaFX

```powershell
mvn --% -DskipTests -Djavafx.run.jvmArgs="-Dauction.client.mode=SEPARATE -Dauction.server.host=127.0.0.1 -Dauction.server.port=5050" javafx:run
```

Neu muon mo 2 client de test realtime, chay lenh tren trong 2 terminal khac nhau sau khi server da len.

### Chay bang Docker Compose

```powershell
copy .env.example .env
```

- Sua `ADMIN_BOOTSTRAP_PASSWORD` trong `.env` truoc khi chay.

```powershell
docker compose -f docker.yml up --build
```

## Chay client tren nhieu thiet bi (Windows / Linux / macOS)

`client.jar` duoc build 1 lan duy nhat va chay duoc tren ca 3 he dieu hanh vi no da chua san JavaFX native cho Windows, Linux va macOS.

---

### Buoc 1 — Build (chay tren may nao co Maven)

```bash
mvn -DskipTests package
```

Ket qua:
```
target/
  server.jar   # chay tren may chu
  client.jar   # phan phoi cho tat ca thiet bi client
```

---

### Buoc 2 — Chay server

Chon **mot** trong cac cach sau tuy he dieu hanh cua may chu:

**Linux (khuyen dung, dung Docker):**
```bash
docker compose -f docker.yml up --build
```

**Linux (khong dung Docker):**
```bash
java -jar target/server.jar
```

**Windows (Docker Desktop):**
```powershell
docker compose -f docker.yml up --build
```

**Windows (khong dung Docker, can MySQL da cai san):**
```powershell
set DB_HOST=localhost
set DB_USER=auction_user
set DB_PASSWORD=Auction123
java -jar target/server.jar
```

**macOS:**
```bash
# voi Docker Desktop
docker compose -f docker.yml up --build

# khong co Docker
java -jar target/server.jar
```

---

### Buoc 3 — Tim IP cua may chu

Cac thiet bi khac can biet IP nay de ket noi.

```bash
# Linux / macOS
ip a | grep "inet " | grep -v 127
# hoac
ifconfig | grep "inet "

# Windows
ipconfig
# tim dong "IPv4 Address" cua card mang dang dung
```

Vi du: `192.168.1.10`

---

### Buoc 4 — Mo port 5050 tren may chu (neu bi chặn)

**Linux — ufw:**
```bash
sudo ufw allow 5050
```

**Linux — firewalld:**
```bash
sudo firewall-cmd --add-port=5050/tcp --permanent
sudo firewall-cmd --reload
```

**Windows:**
Khi chay server lan dau, Windows Firewall se hien hop thoai — chon **"Allow access"** la xong.

Neu khong hien, mo thu cong:
```
Control Panel > Windows Defender Firewall > Advanced Settings
> Inbound Rules > New Rule > Port > TCP 5050 > Allow
```

**macOS:**
```bash
# macOS tu dong hoi khi app lang nghe ket noi den, bam "Allow" la du.
```

---

### Buoc 5 — Cai JDK tren tung thiet bi client

| He dieu hanh | Cach cai |
|---|---|
| Linux | `sudo apt install openjdk-21-jdk` hoac tai tu [Adoptium](https://adoptium.net) |
| Windows | Tai file `.msi` tu [Adoptium](https://adoptium.net), chay cai dat binh thuong |
| macOS | `brew install openjdk@21` hoac tai `.pkg` tu [Adoptium](https://adoptium.net) |

> Dung JDK 21 (LTS) hoac JDK 25 — phai khop voi phien ban dung de build.

---

### Buoc 6 — Chay client tren tung thiet bi

Copy `client.jar` sang thiet bi, sau do chay:

```bash
# Linux / macOS
java -jar client.jar

# Windows (Command Prompt / PowerShell)
java -jar client.jar
```

Trong man hinh dang nhap, o **"Dia chi may chu"**:

| Truong hop | Nhap gi |
|---|---|
| Client va server cung may | `127.0.0.1` (mac dinh) |
| Client khac may, cung mang LAN | IP may chu, vi du `192.168.1.10` |

---

### Tom tat nhanh

```
May chu (bat ky OS) ──── chay server.jar hoac Docker
     │
     │  cong 5050 (LAN)
     │
┌────┴─────┐
│          │
Windows   Linux   macOS   ──── chay client.jar, nhap IP may chu
```

---

## Luu y ve cau truc

- Khong de file IDE trong `src/`.
- Khong de file env/tooling rieng cua IDE trong `src/main/resources/.../view/`.
- Logic dau gia nam o `server/manager/`, UI chi goi request qua `client/network/`.
