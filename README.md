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
| Cơ sở dữ liệu | MySQL 8 (chạy thật) / H2 (mặc định khi chạy nhanh, lưu ở `data/`) |
| Connection pool | HikariCP |
| Logging | SLF4J + Logback |
| Test | JUnit 5 |
| Chất lượng mã | Checkstyle + JaCoCo (coverage) tích hợp trong Maven |
| Đóng gói | Maven + maven-shade-plugin (fat JAR) |
| Triển khai | Docker / Docker Compose (tùy chọn) |

**Yêu cầu cài đặt:**
- JDK 21 (LTS) hoặc JDK 25 — phải khớp với phiên bản dùng để build.
- Maven 3.8+ (chỉ cần trên máy build).
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

## 5. Hướng dẫn chạy theo thứ tự

### Bước 1 — Build (trên máy có Maven)

```bash
# client.jar đa nền tảng (khuyến nghị khi phân phối cho nhiều máy/OS khác nhau)
mvn -Pdist -DskipTests package

# hoặc build nhanh cho riêng OS hiện tại
mvn -DskipTests package
```

### Bước 2 — Chạy **Server trước**

Chọn một trong các cách sau tùy hệ điều hành máy chủ:

```bash
# Cách A — chạy trực tiếp (H2 nhúng, không cần cài DB)
java -jar target/server.jar

# Cách B — chạy bằng Docker Compose (kèm MySQL)
cp .env.example .env        # sửa ADMIN_BOOTSTRAP_PASSWORD trong .env trước khi chạy
docker compose -f docker.yml up --build
```

> **Tài khoản admin:** server không hardcode admin. Đặt biến môi trường
> `ADMIN_BOOTSTRAP_PASSWORD` (và tùy chọn `ADMIN_BOOTSTRAP_USERNAME`,
> `ADMIN_BOOTSTRAP_FULL_NAME`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_ID`) để
> tự tạo/đồng bộ tài khoản admin khi khởi động. Mẫu có sẵn trong `.env.example`.

Server lắng nghe ở **port 5050**. Log khởi động in ra IP nội bộ của máy chủ.

### Bước 3 — Chạy **Client sau** (mở nhiều client để test nhiều người dùng)

```bash
java -jar client.jar
```

Mở **nhiều terminal** và chạy lại lệnh trên để mô phỏng nhiều người dùng đồng thời.
Trong màn hình đăng nhập, ô **"Địa chỉ máy chủ"**:

| Trường hợp | Nhập |
|---|---|
| Client cùng máy với server | `127.0.0.1` (mặc định) |
| Client khác máy, cùng mạng LAN | IP máy chủ, ví dụ `192.168.1.10` |

> Nếu firewall chặn, mở port `5050/tcp` trên máy chủ (ufw/firewalld trên Linux,
> "Allow access" trên Windows Firewall, hoặc cho phép khi macOS hỏi).

### Chạy client JavaFX từ source (cho phát triển)

Chạy được trực tiếp trên **Windows / Linux / macOS** (POM tự chọn JavaFX native theo
OS qua profile, nên không bị lỗi đồ họa kiểu "module javafx.graphics trùng" hay thiếu
native):

```bash
mvn javafx:run

# trỏ tới server khác máy:
mvn javafx:run -Dauction.server.host=192.168.1.10 -Dauction.server.port=5050
```

> **Lưu ý môi trường (Linux):** trên desktop dùng **Wayland**, nếu cửa sổ không hiện
> hoặc lỗi GTK, chạy với `GDK_BACKEND=x11 mvn javafx:run`. Trên máy ảo/không có GPU,
> ép render phần mềm bằng `mvn javafx:run -Dprism.order=sw`.

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
- Lưu trữ bền vững xuống DB (MySQL/H2); ảnh sản phẩm lưu ra file.

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
