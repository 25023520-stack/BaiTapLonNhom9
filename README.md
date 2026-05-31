# Hệ thống đấu giá trực tuyến (Online Auction System) — Nhóm 9

Ứng dụng đấu giá trực tuyến theo mô hình **client–server**: nhiều người dùng kết nối
đồng thời tới một máy chủ trung tâm để đăng sản phẩm, đặt giá và theo dõi phiên đấu
giá theo thời gian thực.

## 1. Bài toán và phạm vi

- **Bài toán:** xây dựng sàn đấu giá nhiều người dùng, hỗ trợ 3 vai trò **Admin / Seller / Bidder**.
- **Phạm vi:**
  - Seller đăng sản phẩm và gửi yêu cầu mở phiên đấu giá (có lịch bắt đầu/kết thúc).
  - Admin duyệt tài khoản seller, duyệt phiên đấu giá, duyệt yêu cầu nạp tiền của bidder.
  - Bidder nạp tiền, đặt giá thủ công hoặc bật **đấu giá tự động (auto-bid)**, thanh toán khi thắng.
  - Máy chủ tự động bắt đầu/kết thúc phiên theo lịch và quyết định người thắng.
  - Cập nhật **realtime** tới mọi client đang xem (giá mới, người giữ giá cao nhất, số dư).
- **Ngoài phạm vi:** thanh toán qua cổng thật (số dư là ví nội bộ trong hệ thống).

## 2. Công nghệ, môi trường và yêu cầu cài đặt

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 25 (tương thích build với JDK 21 LTS) |
| Giao diện | JavaFX 21 + FXML (mô hình MVC) |
| Giao tiếp | TCP socket, giao thức JSON dòng-lệnh (Gson) |
| Cơ sở dữ liệu | MySQL 8 |
| Connection pool | HikariCP |
| Logging | SLF4J + Logback |
| Test | JUnit 5 |
| Chất lượng mã | Checkstyle + JaCoCo (coverage) tích hợp trong Maven |
| Đóng gói | Maven + maven-shade-plugin (fat JAR) |
| Triển khai | Docker / Docker Compose (tùy chọn) |

**Yêu cầu cài đặt:**
- JDK 21 (LTS) hoặc JDK 25 — phải khớp với phiên bản dùng để build.
- Maven 3.8+ (chỉ cần trên máy build).
- **MySQL 8** cho server: tự cài (Cách A/B) **hoặc** để Docker Compose dựng sẵn (Cách C).
- (Tùy chọn) Docker Desktop / Docker Engine nếu chạy server bằng Compose.
- `client.jar` đã đóng gói sẵn JavaFX native cho **Windows, Linux, macOS** nên client chỉ cần JDK, không cần cài thêm.

## 3. Cấu trúc source code

```text
src/
  main/
    java/com/auction/system/
      client/                       # Frontend JavaFX (MVC)
        App.java                    # entry point client
        Launcher.java               # bootstrap JavaFX
        context/AppContext.java     # session, socket, user hiện tại
        network/AuctionClient.java  # TCP client tới server
        controller/                 # Login, Register, Auction, Bidder, Seller, Admin, ServerDown
      server/                       # Backend
        ServerMain.java             # entry point server
        network/                    # AuctionServer (TCP), ClientHandler
        controller/                 # Auth, Auction, Admin controller
        manager/                    # Auth, User, Item, Auction, Admin manager (nghiệp vụ)
        dao/                        # UserDAO, ItemDAO, AuctionDAO, BidDAO, AutoBidDAO, DepositRequestDAO
        observer/                   # AuctionSubject, AuctionObserver (realtime)
        scheduler/AuctionScheduler  # tự động bắt đầu/kết thúc phiên theo lịch
        database/Database.java      # khởi tạo & kết nối DB
      model/                        # entity dùng chung
        user/                       # User, Admin, Seller, Bidder, DepositRequest
        item/                       # Item, Electronics, Art, Vehicle
        auction/                    # Auction, Bid, AutoBid, AuctionStatus
      common/payload/               # giao thức Payload/ResponsePayload client-server
      common/json/GsonProvider.java
      factory/ItemFactory.java      # Factory Method tạo item theo loại
      exception/                    # custom exception nghiệp vụ
    resources/com/auction/system/client/view/   # *.fxml, Styles.css, assets
  test/java/com/auction/system/...               # JUnit test cho tầng nghiệp vụ
config/checkstyle/checkstyle.xml                  # cấu hình Checkstyle
```

**Mẫu thiết kế đã áp dụng:** Singleton (các Manager), Factory Method (`ItemFactory`),
Observer (`AuctionSubject`/`AuctionObserver` cho cập nhật realtime), DAO (tách truy cập DB), MVC (client JavaFX).

## 4. Vị trí các file .jar

Sau khi build, JAR nằm trong thư mục `target/`:

```text
target/
  server.jar   # chạy trên máy chủ   (main: com.auction.system.server.ServerMain)
  client.jar   # phân phối cho client (main: com.auction.system.client.App)
```

- `mvn -DskipTests package` → `client.jar` chứa JavaFX native của **đúng OS đang build**.
- `mvn -Pdist -DskipTests package` → `client.jar` **đa nền tảng** (gói sẵn native cho
  Windows + Linux + macOS Intel/ARM), build 1 lần chạy được trên cả 3 hệ điều hành.

## 5. Hướng dẫn chạy

Luôn theo thứ tự: **Server chạy trước → Client chạy sau**. Có **3 cách** chạy server,
chọn 1 cách phù hợp:

| Cách | Phù hợp với | Cần cài | MySQL |
|---|---|---|---|
| **A. Maven (từ source)** | Lập trình, phát triển, chấm bài nhanh | JDK + Maven | tự cung cấp |
| **B. JAR (đóng gói)** | Phân phối, demo trên máy không có Maven | Chỉ JDK | tự cung cấp |
| **C. Docker Compose** | Chạy giống production, gồm cả MySQL | Chỉ Docker | dựng sẵn trong container |

> **MySQL (áp dụng cho Cách A và B):** server kết nối MySQL qua các biến môi trường, mặc định:
> `DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=auction_db`, `DB_USER=auction_user`,
> `DB_PASSWORD=Auction123`. Trước khi chạy server, cần có sẵn MySQL với database/user tương
> ứng (bảng được tạo tự động lúc khởi động). Cách nhanh nhất là **chỉ dựng MySQL bằng Docker**:
>
> ```bash
> docker compose -f docker.yml up -d mysql     # MySQL nghe ở localhost:3306, đã tạo sẵn DB/user
> ```
>
> Nếu MySQL chạy nơi khác, đặt lại các biến `DB_*` (ví dụ `DB_HOST=192.168.1.10`). **Cách C
> không cần bước này** — Compose tự dựng MySQL.

> **Tài khoản admin (áp dụng cho cả 3 cách):** server **không hardcode** admin. Đặt biến
> môi trường `ADMIN_BOOTSTRAP_PASSWORD` (và tùy chọn `ADMIN_BOOTSTRAP_USERNAME`,
> `ADMIN_BOOTSTRAP_FULL_NAME`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_ID`) để tự
> tạo/đồng bộ admin khi khởi động. Mẫu có sẵn trong `.env.example`.
>
> Server luôn lắng nghe ở **port 5050**; log khởi động in ra IP nội bộ của máy chủ.

---

### Cách A — Chạy bằng Maven (từ source)

Không cần build JAR trước, Maven tự biên dịch và chạy. Hợp với phát triển.
Đảm bảo MySQL đã sẵn sàng (xem ghi chú MySQL ở trên).

**A.1 — Server (chạy trước):**

```bash
# cần MySQL ở localhost:3306 (DB auction_db / user auction_user / pass Auction123)
ADMIN_BOOTSTRAP_PASSWORD=admin123 \
  mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.auction.system.server.ServerMain
```

**A.2 — Client (chạy sau, mở nhiều cửa sổ để test nhiều người dùng):**

JavaFX chạy trực tiếp trên **Windows / Linux / macOS** (POM tự chọn native theo OS qua
profile nên không lỗi "module javafx.graphics trùng" hay thiếu native):

```bash
mvn javafx:run

# trỏ tới server ở máy khác:
mvn javafx:run -Dauction.server.host=192.168.1.10 -Dauction.server.port=5050
```

> **Lưu ý môi trường (Linux):** desktop dùng **Wayland** nếu cửa sổ không hiện / lỗi GTK
> thì chạy `GDK_BACKEND=x11 mvn javafx:run`. Máy ảo/không có GPU thì ép render phần mềm:
> `mvn javafx:run -Dprism.order=sw`.

---

### Cách B — Chạy bằng JAR (đóng gói)

**B.1 — Build JAR (trên máy có Maven):**

```bash
# client.jar đa nền tảng — gói sẵn native Windows + Linux + macOS (khuyến nghị khi phân phối)
mvn -Pdist -DskipTests package

# hoặc build nhanh chỉ cho OS hiện tại
mvn -DskipTests package
```

Kết quả: `target/server.jar` và `target/client.jar` (xem mục 4).

**B.2 — Server (chạy trước, chỉ cần JDK + MySQL, không cần Maven):**

```bash
java -jar target/server.jar

# đặt mật khẩu admin lần đầu (Linux/macOS):
ADMIN_BOOTSTRAP_PASSWORD=admin123 java -jar target/server.jar
```

```powershell
# Windows PowerShell:
$env:ADMIN_BOOTSTRAP_PASSWORD="admin123"; java -jar target/server.jar
```

**B.3 — Client (chạy sau, mở nhiều terminal để mô phỏng nhiều người dùng):**

```bash
java -jar target/client.jar
```

---

### Cách C — Chạy bằng Docker (Docker Compose + MySQL)

Cách này dựng **MySQL 8 thật** trong container và build server từ `Dockerfile`
(multi-stage: `maven:3.9-eclipse-temurin-25` build → `eclipse-temurin:25-jre` chạy).
Không cần cài JDK/Maven/MySQL trên máy — chỉ cần Docker.

**C.1 — Cấu hình admin:**

```bash
cp .env.example .env        # sửa ADMIN_BOOTSTRAP_PASSWORD trong .env trước khi chạy
```

**C.2 — Khởi động (server + DB):**

```bash
docker compose -f docker.yml up --build      # thêm -d để chạy nền
```

Hoặc dùng **Makefile** cho gọn:

```bash
make up         # build + khởi động (docker compose -f docker.yml up --build)
make logs       # xem log realtime
make ps         # liệt kê container
make down       # dừng và xóa container
make restart    # down rồi up lại
make clean      # down kèm xóa volume DB (mất sạch dữ liệu MySQL)
```

Compose mở các port: **`5050`** (server) và **`3306`** (MySQL). Thông tin DB mặc định
nằm trong `docker.yml` (`auction_db` / `auction_user` / `Auction123`). Dữ liệu MySQL
được giữ trong volume `db_data` (chỉ mất khi `make clean` / `docker compose down -v`).

**C.3 — Client:** Docker chỉ chạy **server** (giao diện JavaFX không chạy trong
container). Trên máy thật, chạy client trỏ tới server trong Docker bằng Cách A hoặc B
(host `127.0.0.1` vì port `5050` đã map ra ngoài).

---

### Kết nối Client tới Server

Trong màn hình đăng nhập, ô **"Địa chỉ máy chủ"**:

| Trường hợp | Nhập |
|---|---|
| Client cùng máy với server (kể cả server chạy bằng Docker) | `127.0.0.1` (mặc định) |
| Client khác máy, cùng mạng LAN | IP máy chủ, ví dụ `192.168.1.10` |

> Nếu firewall chặn, mở port `5050/tcp` trên máy chủ (ufw/firewalld trên Linux,
> "Allow access" trên Windows Firewall, hoặc cho phép khi macOS hỏi).

## 6. Chức năng đã hoàn thành

**Tài khoản & phân quyền**
- Đăng ký / đăng nhập; 3 vai trò Admin, Seller, Bidder.
- Admin duyệt tài khoản Seller trước khi seller được phép hoạt động.

**Seller**
- Đăng / sửa / xóa sản phẩm (Electronics, Art, Vehicle qua Factory Method).
- Gửi yêu cầu mở phiên đấu giá kèm lịch bắt đầu/kết thúc.
- Xem người đang giữ giá cao nhất; nhận tiền vào số dư khi sản phẩm bán được.
- Sản phẩm không có ai đặt giá khi hết giờ sẽ tự reset để gửi duyệt lại.

**Bidder**
- Gửi yêu cầu nạp tiền; Admin duyệt để cộng số dư.
- Đặt giá thủ công (kiểm tra giá hợp lệ, không tự đấu sản phẩm của mình).
- **Đấu giá tự động (auto-bid):** đặt giá tối đa + bước giá, hệ thống tự nâng giá theo độ ưu tiên.
- Thanh toán khi thắng; hoặc từ chối nhận hàng (hủy phiên).

**Admin**
- Dashboard tổng quan; duyệt seller, duyệt phiên đấu giá, duyệt yêu cầu nạp tiền.

**Hệ thống**
- Server tự động bắt đầu/kết thúc phiên theo lịch (`AuctionScheduler`), tự quyết định người thắng.
- Cập nhật **realtime** qua Observer: giá mới, người giữ giá cao nhất, số dư, trạng thái phiên đẩy tới mọi client.
- **Xử lý đồng thời (concurrency):** khóa theo từng item (`ReentrantLock`) + giao dịch DB để chống race khi nhiều bidder cùng đặt giá.
- Cô lập lỗi từng request: một request lỗi không làm rớt kết nối của client.
- Lưu trữ bền vững xuống DB (MySQL); ảnh sản phẩm lưu ra file.

**Chất lượng mã**
- Checkstyle tích hợp Maven (`config/checkstyle/checkstyle.xml`).
- Báo cáo độ phủ test bằng JaCoCo (xem mục dưới).

## 7. Kiểm thử & độ phủ (coverage)

```bash
mvn test                      # chạy JUnit + sinh báo cáo JaCoCo
# Báo cáo HTML: target/site/jacoco/index.html

mvn verify                    # chạy test + kiểm tra ngưỡng coverage + Checkstyle
mvn checkstyle:check          # chỉ chạy Checkstyle
```

Coverage được đo trên **tầng nghiệp vụ** (`manager`, `dao`, `model`, `factory`, `scheduler`);
tầng giao diện JavaFX và entry point được kiểm thử thủ công nên loại khỏi phép đo.
Bộ test tập trung vào `AuctionManager` (đặt giá, auto-bid, đồng thời).

## 8. Báo cáo & video demo

- **Báo cáo PDF:** _(cập nhật link)_ — nội dung trong `REPORT.md`.
- **Video demo (≤ 3 phút):** _(cập nhật link)_

## 9. Lưu ý về cấu trúc

- Không để file IDE trong `src/`.
- Logic đấu giá nằm ở `server/manager/`; UI chỉ gọi request qua `client/network/`.
- Không để file env/cấu hình riêng của IDE trong `src/main/resources/.../view/`.
