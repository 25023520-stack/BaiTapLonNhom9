# Hệ thống đấu giá trực tuyến (Online Auction System) - Nhóm 9

Ứng dụng đấu giá trực tuyến theo mô hình **client-server**. Nhiều người dùng có thể kết nối đồng thời tới một server trung tâm để đăng sản phẩm, gửi yêu cầu mở phiên, đặt giá, bật auto-bid và theo dõi phiên đấu giá theo thời gian thực.

## 1. Bài toán và phạm vi

**Bài toán:** xây dựng sàn đấu giá nhiều người dùng, hỗ trợ 3 vai trò chính: **Admin**, **Seller**, **Bidder**.

**Phạm vi thực hiện:**

- Seller đăng, sửa, xóa sản phẩm và gửi yêu cầu mở phiên đấu giá kèm lịch bắt đầu/kết thúc.
- Admin duyệt tài khoản seller, duyệt phiên đấu giá và duyệt yêu cầu nạp tiền của bidder.
- Bidder gửi yêu cầu nạp tiền, đặt giá thủ công, bật/tắt **auto-bid**, thanh toán khi thắng hoặc từ chối nhận hàng.
- Server tự động kiểm tra lịch để đóng phiên hết hạn, xác định người thắng và cập nhật trạng thái.
- Hệ thống cập nhật realtime tới các client đang kết nối: giá mới, người giữ giá cao nhất, số dư, trạng thái phiên.
- Có xử lý đồng thời khi nhiều bidder đặt giá cùng lúc.
- Có chức năng nâng cao: **auto-bidding** và **anti-sniping**.

**Ngoài phạm vi:** chưa tích hợp cổng thanh toán thật; số dư là ví nội bộ trong hệ thống.

## 2. Công nghệ sử dụng

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 25 |
| Giao diện | JavaFX 21 + FXML |
| Kiến trúc UI | MVC |
| Giao tiếp | TCP Socket, JSON line protocol, Gson |
| Cơ sở dữ liệu | MySQL 8 qua HikariCP |
| Đóng gói | Maven + maven-shade-plugin |
| Logging | SLF4J + Logback |
| Kiểm thử | JUnit 5, JaCoCo |
| Coding convention | Checkstyle, Qodana |
| Triển khai | Docker / Docker Compose |

**Yêu cầu cài đặt:**

- JDK 25 hoặc JDK tương thích với cấu hình Maven hiện tại.
- Maven 3.8+.
- MySQL 8 nếu chạy server trực tiếp.
- Docker Desktop / Docker Engine nếu chạy bằng Docker Compose.

## 3. Kiến trúc tổng thể

Hệ thống được tổ chức theo kiến trúc phân tầng:

1. **Client JavaFX:** gồm các màn hình FXML và controller cho Login, Register, Bidder, Seller, Admin, Auction.
2. **Tầng giao tiếp:** `AuctionClient` gửi `Payload` JSON qua TCP socket tới server và nhận `ResponsePayload`.
3. **Server TCP:** `AuctionServer` lắng nghe port `5050`, mỗi client được xử lý bởi một `ClientHandler` riêng.
4. **Server controller:** `AuthController`, `AuctionController`, `AdminController`, `ProfileController` đọc request và gọi manager tương ứng.
5. **Business layer:** `AuthManager`, `ItemManager`, `AuctionManager`, `AdminManager`, `ProfileManager` chứa logic nghiệp vụ.
6. **DAO layer:** `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidDAO`, `AutoBidDAO`, `DepositRequestDAO`, `ProfileDAO` thao tác database.
7. **Database và file storage:** MySQL lưu dữ liệu nghiệp vụ; ảnh sản phẩm lưu trong `data/uploads/items`.

Realtime update được triển khai bằng Observer Pattern: server phát sự kiện đấu giá/số dư tới các client đang kết nối thông qua `AuctionSubject` và `AuctionObserver`.

## 4. Cấu trúc source code

```text
src/
  main/
    java/com/auction/system/
      client/                         # JavaFX client
        App.java
        Launcher.java
        context/AppContext.java
        network/AuctionClient.java
        controller/                   # Login, Register, Auction, Bidder, Seller, Admin, ServerDown
      server/                         # TCP server + backend
        ServerMain.java
        network/                      # AuctionServer, ClientHandler
        controller/                   # Auth, Auction, Admin, Profile controller
        manager/                      # Auth, User, Item, Auction, Admin, Profile manager
        dao/                          # User, Item, Auction, Bid, AutoBid, DepositRequest, Profile DAO
        database/Database.java        # HikariCP + schema initialization
        observer/                     # AuctionSubject, AuctionObserver
        scheduler/AuctionScheduler.java
        util/                         # AutoBidEnricher, ItemImageService
      model/
        user/                         # User, Admin, Seller, Bidder
        item/                         # Item, Electronics, Art, Vehicle, Book, Fashion, Home, Collectible
        auction/                      # Auction, Bid, AutoBid, AuctionStatus
        payment/DepositRequest.java
      factory/                        # ItemFactory + ItemCreator theo từng category
      common/
        payload/                      # PayloadType, Payload, ResponsePayload, BidPayload, AutoBidPayload, UserProfilePayload
        json/GsonProvider.java
        money/Money.java
      exception/                      # Custom exceptions
    resources/com/auction/system/client/view/
      *.fxml, Styles.css, assets/
  test/java/com/auction/system/...    # JUnit tests
config/checkstyle/checkstyle.xml
.github/workflows/                   # CI + Qodana
```

## 5. Design patterns và kỹ thuật chính

- **Singleton:** các manager và `Database` có điểm truy cập thống nhất.
- **Factory Method:** `ItemFactory` phối hợp các `ItemCreator` để tạo item theo category như `Electronics`, `Art`, `Vehicle`, `Book`, `Fashion`, `Home`, `Collectible`, `Other`.
- **Observer:** `AuctionSubject`/`AuctionObserver` dùng cho realtime update từ server tới client.
- **DAO:** tách truy cập database khỏi logic nghiệp vụ.
- **MVC:** JavaFX client tách FXML view, controller và model/payload.
- **Concurrency control:** `AuctionManager` dùng `ReentrantLock` theo từng item và transaction DB để tránh race condition khi nhiều bidder đặt giá đồng thời.

## 6. Chức năng đã hoàn thành

### Tài khoản và phân quyền

- Đăng ký, đăng nhập.
- 3 vai trò: Admin, Seller, Bidder.
- Admin duyệt tài khoản seller trước khi seller được hoạt động.
- Bidder/Seller có màn hình hồ sơ, thống kê và lịch sử liên quan.

### Seller

- Đăng, sửa, xóa sản phẩm.
- Chọn loại sản phẩm qua Factory Method.
- Upload ảnh sản phẩm.
- Gửi yêu cầu mở phiên đấu giá kèm thời gian bắt đầu/kết thúc.
- Xem tình trạng phiên, người giữ giá cao nhất và kết quả bán hàng.
- Relist/gửi duyệt lại phiên phù hợp với trạng thái item.

### Bidder

- Gửi yêu cầu nạp tiền.
- Đặt giá thủ công với kiểm tra hợp lệ.
- Bật/tắt auto-bid theo giá tối đa và bước giá.
- Xem lịch sử bid, danh sách item đã thắng và thông tin hồ sơ.
- Thanh toán khi thắng hoặc từ chối nhận hàng.

### Admin

- Dashboard tổng quan.
- Duyệt seller.
- Duyệt hoặc từ chối yêu cầu mở phiên đấu giá.
- Duyệt yêu cầu nạp tiền cho bidder.

### Hệ thống đấu giá

- Tự động đóng phiên hết hạn bằng `AuctionScheduler`.
- Xác định người thắng dựa trên highest bidder.
- Nếu phiên hết hạn không có bid, hệ thống reset để seller có thể gửi duyệt lại.
- Auto-bid tự nâng giá theo max bid, increment và thời điểm tạo.
- Anti-sniping: nếu bid xuất hiện trong 60 giây cuối, hệ thống gia hạn phiên thêm 60 giây.
- Realtime update qua Observer/Socket.
- Lưu dữ liệu bền vững xuống MySQL; ảnh sản phẩm lưu trong `data/uploads/items`.

## 7. Vị trí các file `.jar`

Project dùng `maven-shade-plugin` để đóng gói executable fat JAR. Sau khi build thành công, các file chạy nằm trong thư mục `target/`:

```text
target/
  server.jar   # chạy server, main class: com.auction.system.server.ServerMain
  client.jar   # chạy client JavaFX, main class: com.auction.system.client.App
```

Lệnh build:

```bash
mvn -DskipTests package
```

Nếu muốn build client JAR đa nền tảng có JavaFX native cho nhiều hệ điều hành:

```bash
mvn -Pdist -DskipTests package
```

## 8. Hướng dẫn chạy Server/Client theo thứ tự

### Cách A: chạy bằng Docker Compose

```bash
docker compose -f docker.yml up --build
```

Docker Compose sẽ chạy:

- MySQL 8.4 tại port `3306`.
- Server tại port `5050`.

Có thể đặt tài khoản admin bootstrap bằng biến môi trường trong file `.env`:

```env
ADMIN_BOOTSTRAP_USERNAME=admin
ADMIN_BOOTSTRAP_PASSWORD=your_password
ADMIN_BOOTSTRAP_FULL_NAME=System Admin
ADMIN_BOOTSTRAP_EMAIL=admin@example.com
```

Sau đó mở client từ JAR đã build:

```bash
java -jar target/client.jar
```

### Cách B: chạy trực tiếp bằng JAR (mỗi người tự cài MySQL trên máy mình)

#### B.1. Khởi tạo MySQL bằng MySQL Workbench

Yêu cầu: đã cài **MySQL Server** và **MySQL Workbench** trên máy đang chạy server.

1. Mở **MySQL Workbench** → kết nối vào MySQL local (mặc định `localhost:3306`, user `root`).
2. Mở tab **Query** và chạy lần lượt các lệnh sau:

```sql
CREATE DATABASE IF NOT EXISTS auction_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auction_user'@'%' IDENTIFIED BY 'Auction123';

GRANT ALL PRIVILEGES ON auction_db.* TO 'auction_user'@'%';

FLUSH PRIVILEGES;
```

> Không cần tạo bảng thủ công — server tự tạo toàn bộ schema khi khởi động lần đầu.

#### B.2. Chạy server và client

```bash
mvn -DskipTests package
```

Lần đầu chạy, cần tạo tài khoản admin bằng cách set biến môi trường trước khi khởi động server:

**Windows (PowerShell):**
```powershell
$env:ADMIN_BOOTSTRAP_USERNAME="admin"
$env:ADMIN_BOOTSTRAP_PASSWORD="matkhaucuaban"
$env:ADMIN_BOOTSTRAP_FULL_NAME="System Admin"
java -jar target/server.jar
```

Server tự tạo tài khoản admin vào database khi khởi động. Các lần chạy sau không cần set lại.

Nếu đã có tài khoản admin trong database:

```bash
java -jar target/server.jar
```

Sau khi server chạy, mở client:

```bash
java -jar target/client.jar
```

Nếu client kết nối tới server khác máy:

```bash
java "-Dauction.server.host=<IP>" -jar target/client.jar
```

> **Cách kiểm tra IP của máy chạy server:**
>
> - **Windows:** mở Command Prompt → gõ `ipconfig` → lấy giá trị **IPv4 Address** (ví dụ `192.168.1.x`)
> - **Linux/macOS:** mở Terminal → gõ `ip addr` hoặc `ifconfig` → lấy giá trị `inet` của card mạng đang dùng
Chạy client trực tiếp từ source:

```bash
mvn javafx:run
```




## 9. Kiểm thử và chất lượng mã

```bash
mvn test
mvn verify
mvn checkstyle:check
```

- `mvn test`: chạy JUnit và sinh báo cáo JaCoCo.
- `mvn verify`: chạy test, coverage check và Checkstyle.
- Báo cáo coverage HTML: `target/site/jacoco/index.html`.
- CI cấu hình trong `.github/workflows/ci.yml`.
- Qodana cấu hình trong `.github/workflows/qodana_code_quality.yml`.

Các test chính nằm ở:

- `AuctionManagerTest`
- `AuctionManagerAutoBidTest`
- `AuctionManagerConcurrencyTest`
- `AuctionManagerExtendedTest`
- `AuthManagerTest`
- `AdminManagerTest`
- `ModelTest`
- `TestDatabase`, `TestItemDAO`

## 10. Báo cáo và demo

- Báo cáo PDF: [Bao_cao_BTL_He_thong_dau_gia_truc_tuyen_Nhom9.pdf](./Bao_cao_BTL_He_thong_dau_gia_truc_tuyen_Nhom9.pdf)
- Bản nội dung báo cáo: `REPORT.md`
- Video demo: cập nhật https://drive.google.com/file/d/1Ch7bDouZ-cmvBTbT2NKqYSHM4-F9IuFc/view.

## 11. Lưu ý phát triển

- Logic nghiệp vụ đấu giá nằm ở `server/manager`, đặc biệt là `AuctionManager`.
- UI JavaFX chỉ gửi request qua `AuctionClient`, không tự xử lý luật đấu giá.
- Khi sửa chức năng đặt giá, auto-bid, anti-sniping hoặc thanh toán, cần chạy lại các test trong nhóm `AuctionManager*Test`.
- Không commit file database local như `data/*.db`, log hoặc file IDE nếu không cần thiết.
