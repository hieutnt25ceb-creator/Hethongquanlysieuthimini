# Hệ Thống Quản Lý MiniMart — MiniMart Management System

Ứng dụng quản lý điểm bán hàng (POS) và quản trị kho hàng MiniMart trên nền tảng Desktop (Client/Server), được thiết kế với giao diện cao cấp, độ tương phản cao (Dark Slate & Emerald theme), tối ưu hóa trải nghiệm người dùng, tải ảnh bất đồng bộ và mã hóa dữ liệu an toàn.

---

## 🛠️ Các thư viện sử dụng (Technology Stack)

Hệ thống được phát triển dưới dạng dự án Maven đa mô-đun (multi-module Maven project) với các thư viện chính:
* **Java Core & Desktop UI**: Java 17+, JavaFX 21 (Graphics, Controls, FXML) cho giao diện người dùng.
* **Database Connection Pool**: HikariCP (kết nối cơ sở dữ liệu hiệu năng cao).
* **Database Driver**: MySQL Connector Java 8.x.
* **JSON Serialization**: Google Gson (giao tiếp dữ liệu Client/Server qua TCP Socket).
* **XML Serialization**: Jakarta XML Binding (JAXB) (cho tính năng xuất/nhập danh mục sản phẩm).
* **Security & Encryption**: 
  * BCrypt (Mã hóa một chiều mật khẩu tài khoản).
  * AES-128-CBC (Mã hóa hai chiều số điện thoại khách hàng).
* **Logging Framework**: SLF4J + Logback (ghi nhật ký hệ thống hoạt động và kiểm toán bảo mật).

---

## 💾 Hướng dẫn cài đặt Cơ sở dữ liệu (Database Setup)

1. Cài đặt hệ quản trị cơ sở dữ liệu **MySQL 8.0+** trên máy tính của bạn.
2. Mở MySQL Command Line Client hoặc ứng dụng quản lý như **MySQL Workbench**, sau đó chạy lệnh để import cấu trúc bảng và 48 sản phẩm Việt Nam mẫu có sẵn:
   ```sql
   source d:/MiniMart/db/minimart_schema.sql
   ```
   *(Hoặc copy toàn bộ nội dung file [minimart_schema.sql](file:///d:/MiniMart/db/minimart_schema.sql) chạy trực tiếp trong Query tab).*

---

## 🚀 Hướng dẫn khởi chạy hệ thống (Run Guide)

Hệ thống đã được trang bị sẵn các tệp chạy tự động nhanh `.bat` ở thư mục gốc của dự án. Bạn có thể khởi chạy theo hai cách:

### Cách 1: Sử dụng File chạy tự động nhanh (Khuyên dùng)
* **Chạy toàn bộ hệ thống (Server + Client)**: Nhấp đúp vào **[run-all.bat](file:///d:/MiniMart/run-all.bat)**. File này sẽ tự động khởi chạy Server trong một cửa sổ riêng, đợi 3 giây để Server kết nối DB thành công, sau đó khởi chạy Client UI.
* **Chạy riêng lẻ Server**: Nhấp đúp vào **[run-server.bat](file:///d:/MiniMart/run-server.bat)**.
* **Chạy riêng lẻ Client**: Nhấp đúp vào **[run-client.bat](file:///d:/MiniMart/run-client.bat)**.

### Cách 2: Khởi chạy thủ công qua dòng lệnh (Command Line)
1. **Biên dịch & Đóng gói dự án**:
   ```bash
   cd d:\MiniMart
   mvn clean install -DskipTests
   ```
2. **Khởi chạy Server**:
   ```bash
   java "-Ddb.password=123456" -jar minimart-server/target/minimart-server.jar
   ```
   *(Thay đổi `123456` bằng mật khẩu MySQL thực tế trên máy bạn nếu có thay đổi).*
3. **Khởi chạy Client**:
   ```bash
   java -jar minimart-client/target/minimart-client.jar
   ```

---

## 🔑 Tài khoản đăng nhập mẫu (Sample Accounts)

Bạn có thể sử dụng các tài khoản mặc định dưới đây sau khi khởi tạo cơ sở dữ liệu để đăng nhập vào hệ thống:

| Vai trò | Tên đăng nhập | Mật khẩu | Chức năng chính được phân quyền |
|---|---|---|---|
| **Quản trị viên (Admin)** | `admin` | `Admin@123` | Bảng điều khiển doanh thu, Quản lý sản phẩm, Khách hàng, Đơn hàng, Nhân viên, Nhật ký hệ thống |
| **Nhân viên (Employee)** | `nhanvien1` | `Emp@123` | Điểm bán hàng (POS), Tìm kiếm sản phẩm, Giỏ hàng, Tạo đơn và Thanh toán |

*Ngoài ra, nhân viên mới cũng có thể tự đăng ký tài khoản trực tiếp từ màn hình đăng nhập (chức năng Đăng ký tài khoản).*

---

## 🌟 Các chức năng đã hoàn thiện (Feature Roadmap)

### 1. Điểm bán hàng (POS) & Thanh toán:
* Tìm kiếm sản phẩm thông minh theo tên hoặc mã vạch thời gian thực.
* Quản lý giỏ hàng trực quan: thêm/bớt số lượng sản phẩm, tự động tính tổng tiền Việt Nam Đồng (VND).
* Chọn tài khoản khách hàng thân thiết từ danh sách (hoặc mặc định Khách vãng lai).
* **Giao dịch an toàn (DB Transactions)**: Quá trình tạo đơn hàng được đóng gói trong một Transaction duy nhất ở Database (kiểm tra tồn kho -> trừ kho -> tạo đơn -> tạo chi tiết đơn). Bất kỳ bước nào gặp lỗi sẽ tự động Rollback để đảm bảo tính nhất quán dữ liệu.
* Xuất hóa đơn bán hàng trực quan dạng Modal Pop-up sau khi thanh toán thành công.

### 2. Quản lý hình ảnh sản phẩm & Đồ họa thông minh:
* Tải ảnh sản phẩm **Bất đồng bộ (Asynchronous Background Load)** bằng JavaFX giúp giao diện siêu mượt, không bao giờ bị đứng hình khi tải ảnh từ đường dẫn cục bộ hoặc Internet.
* Tích hợp công cụ duyệt ảnh **FileChooser** giúp Admin dễ dàng chọn tệp ảnh cục bộ lưu trực tiếp vào ứng dụng.
* **Bộ nạp ảnh thông minh (Fallback system)**: Nếu sản phẩm không có ảnh hoặc file ảnh bị hỏng/xóa, giao diện sẽ tự động dựng hình đại diện thay thế dạng hình hộp màu sắc pastel tương ứng với phân loại, kèm 2 chữ cái viết tắt của sản phẩm.

### 3. Bảng điều khiển & Thống kê quản trị (Admin Dashboard):
* Biểu đồ cột trực quan hiển thị doanh thu hàng ngày trong 30 ngày qua bằng JavaFX `BarChart`.
* Nhóm 4 thẻ KPI thống kê tự động: Tổng doanh thu, Tổng đơn hàng, Tổng sản phẩm hoạt động, Tổng số khách hàng.
* Bảng cảnh báo sản phẩm có lượng hàng tồn kho sắp hết (tồn kho dưới mức tối thiểu).

### 4. Quản lý danh mục & Hệ thống CRUD:
* Quản lý Sản phẩm: Thêm, sửa, khóa trạng thái, tìm kiếm.
* Quản lý Khách hàng: Thêm, sửa, xóa, tìm kiếm, lưu trữ bảo mật số điện thoại (AES).
* Xem danh sách Đơn hàng và chi tiết các mặt hàng của từng hóa đơn.
* Xem danh sách Tài khoản Nhân viên, khóa/mở khóa trạng thái hoạt động.
* Xem Nhật ký hoạt động (System Log) thời gian thực của hệ thống.
* **Nhập/Xuất XML (JAXB)**: Cho phép xuất toàn bộ danh mục sản phẩm ra file XML hoặc nhập ngược lại từ file XML ngoài để cập nhật hàng loạt danh sách sản phẩm nhanh chóng.
