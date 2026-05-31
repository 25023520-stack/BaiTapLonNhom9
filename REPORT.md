# Báo cáo: Hệ thống đấu giá trực tuyến — Nhóm 9

> Nội dung dùng để xuất file PDF (tối đa 5 trang). Có thể chuyển sang PDF bằng
> `pandoc REPORT.md -o REPORT.pdf` hoặc in từ trình xem Markdown.

## 1. Giới thiệu — mục tiêu và phạm vi

Đề tài xây dựng một **sàn đấu giá trực tuyến nhiều người dùng** theo mô hình
client–server. Mục tiêu là cho phép nhiều người dùng kết nối đồng thời tới một máy
chủ trung tâm để đăng sản phẩm, đặt giá và theo dõi diễn biến phiên đấu giá theo
thời gian thực, với ba vai trò: **Admin**, **Seller**, **Bidder**.

Phạm vi thực hiện:

- Quản lý tài khoản và phân quyền theo vai trò.
- Seller đăng sản phẩm, gửi yêu cầu mở phiên đấu giá kèm lịch.
- Admin duyệt seller, duyệt phiên đấu giá, duyệt yêu cầu nạp tiền.
- Bidder nạp tiền, đặt giá thủ công, đấu giá tự động (auto-bid), thanh toán khi thắng.
- Tự động kết thúc phiên theo lịch và quyết định người thắng.
- Cập nhật realtime tới mọi client; xử lý an toàn khi nhiều người cùng đặt giá.

Ngoài phạm vi: cổng thanh toán thật (dùng ví số dư nội bộ).

## 2. Kiến trúc tổng thể

Hệ thống gồm **nhiều client JavaFX** kết nối tới **một server TCP** dùng chung một
**cơ sở dữ liệu** MySQL.

```text
 ┌───────────────┐        ┌───────────────┐        ┌───────────────┐
 │  Client #1    │        │  Client #2    │  ...   │  Client #N    │
 │ JavaFX (MVC)  │        │ JavaFX (MVC)  │        │ JavaFX (MVC)  │
 │ AuctionClient │        │ AuctionClient │        │ AuctionClient │
 └───────┬───────┘        └───────┬───────┘        └───────┬───────┘
         │  TCP socket — JSON (Gson), mỗi request 1 dòng    │
         └───────────────────────┬─────────────────────────┘
                                  │  port 5050
                        ┌─────────▼──────────┐
                        │   AuctionServer    │  (TCP, 1 thread / client)
                        │   ClientHandler*N  │──┐  (Observer: nhận update)
                        └─────────┬──────────┘  │
              Controller (Auth / Auction / Admin) phân tích request
                                  │              │ notifyObservers()
                        ┌─────────▼──────────┐   │
                        │   Manager (BLL)    │───┘  AuctionSubject
                        │ Auth/User/Item/    │
                        │ Auction/Admin      │  + ReentrantLock theo item
                        │ AuctionScheduler   │  (tự kết thúc phiên hết giờ)
                        └─────────┬──────────┘
                        ┌─────────▼──────────┐
                        │   DAO + HikariCP   │
                        └─────────┬──────────┘
                        ┌─────────▼──────────┐
                        │     MySQL DB       │  + file ảnh sản phẩm
                        └────────────────────┘
```

**Mô tả theo kiến trúc phân tầng:**

1. **Tầng giao diện (client):** JavaFX + FXML theo MVC. Mỗi controller chỉ dựng UI
   và gọi `AuctionClient` (TCP client) — không chứa nghiệp vụ. `AppContext` giữ
   session và socket.
2. **Tầng giao tiếp:** TCP socket, giao thức JSON theo dòng (`Payload`/`ResponsePayload`,
   serialize bằng Gson). `AuctionServer` cấp một `ClientHandler` cho mỗi client.
3. **Tầng điều phối (controller server):** `AuthController`, `AuctionController`,
   `AdminController` đọc payload, gọi manager tương ứng và trả response.
4. **Tầng nghiệp vụ (manager):** chứa toàn bộ logic đấu giá. Dùng **Singleton**;
   khóa **`ReentrantLock` theo từng item** + **transaction DB** để đảm bảo an toàn
   khi nhiều bidder cùng đặt giá. `AuctionScheduler` chạy nền, tự kết thúc phiên hết giờ.
5. **Tầng truy cập dữ liệu (DAO):** tách riêng từng bảng, dùng `HikariCP` pool.
6. **Tầng dữ liệu:** MySQL; ảnh sản phẩm lưu file.

**Cập nhật realtime** theo mẫu **Observer**: mỗi `ClientHandler` là một
`AuctionObserver` đăng ký với `AuctionSubject` (server). Khi giá/trạng thái/số dư
đổi, manager gọi `notifyObservers()` và server đẩy bản cập nhật tới mọi client liên quan.

**Mẫu thiết kế áp dụng:** Singleton (Manager), Factory Method (`ItemFactory` tạo
Electronics/Art/Vehicle), Observer (realtime), DAO, MVC (client).

## 3. Các chức năng đạt được (theo barem)

| Chức năng | Hướng giải quyết | Lý do chọn |
|---|---|---|
| **Client–server nhiều người dùng** | TCP socket, mỗi client một `ClientHandler` chạy trên thread riêng | Socket thuần đáp ứng yêu cầu nhiều client đồng thời, dễ kiểm soát; tách thread để không block lẫn nhau |
| **Phân quyền Admin/Seller/Bidder** | Kế thừa từ `User`; kiểm tra vai trò ở controller/manager | Mô hình OOP rõ ràng, dễ mở rộng vai trò |
| **Đăng & quản lý sản phẩm** | `ItemFactory` (Factory Method) tạo item theo loại | Tách logic khởi tạo, thêm loại mới không sửa chỗ gọi |
| **Mở/duyệt phiên đấu giá** | Seller gửi yêu cầu → Admin duyệt → chuyển RUNNING; có lịch start/end | Tách quyền: seller đề xuất, admin kiểm soát |
| **Đặt giá thủ công** | `placeBid` kiểm tra trạng thái, giá hợp lệ, không tự đấu | Đảm bảo luật đấu giá cơ bản |
| **Đấu giá tự động (auto-bid)** | Lưu max-bid + bước giá; xếp ưu tiên theo max-bid rồi thời gian, server tự nâng giá | Trải nghiệm như sàn thật; xử lý tập trung ở server để nhất quán |
| **Xử lý đồng thời** | `ReentrantLock` theo item + transaction DB (commit/rollback) | Chống race khi nhiều bidder cùng đặt; khóa theo item để vẫn song song giữa các phiên |
| **Tự kết thúc phiên & chọn người thắng** | `AuctionScheduler` quét định kỳ 10s, đóng phiên hết giờ | Không phụ thuộc thao tác tay; tự phục hồi phiên hết giờ khi server vừa bật |
| **Cập nhật realtime** | Observer: server đẩy update tới mọi client | Người dùng thấy giá/số dư đổi ngay, không cần refresh |
| **Ví & thanh toán** | Bidder nạp tiền (Admin duyệt); trừ tiền bidder, cộng tiền seller khi thanh toán | Mô phỏng dòng tiền khép kín mà không cần cổng thật |
| **Lưu trữ bền vững** | DAO + HikariCP trên MySQL; ảnh lưu file | Dữ liệu không mất khi tắt server; pool để chịu tải đồng thời |
| **Cô lập lỗi** | Mỗi request bọc try/catch, trả lỗi thay vì rớt kết nối | Một request hỏng không đẩy client sang màn "mất kết nối" |
| **Chất lượng mã** | Checkstyle + JaCoCo tích hợp Maven | Đảm bảo coding convention và đo độ phủ test |

## 4. Kiểm thử

- JUnit 5 cho tầng nghiệp vụ, tập trung `AuctionManager`: đặt giá, auto-bid, và
  **kịch bản đồng thời** (nhiều luồng cùng đặt giá để kiểm chứng khóa/transaction).
- JaCoCo đo độ phủ trên tầng nghiệp vụ (`manager`, `dao`, `model`, `factory`,
  `scheduler`); tầng UI JavaFX kiểm thử thủ công nên loại khỏi phép đo.
- Lệnh: `mvn verify` (chạy test + ngưỡng coverage + Checkstyle).
  Báo cáo HTML: `target/site/jacoco/index.html`.
