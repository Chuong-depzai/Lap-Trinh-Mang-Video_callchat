# 📞 Ứng Dụng Call Video & Chat (Java Socket)

Đây là dự án lập trình mạng xây dựng ứng dụng chat và gọi video trực tuyến sử dụng **Java Socket** và thư viện **OpenCV** để xử lý hình ảnh camera. Dự án được chia thành hai module hoạt động độc lập: `Server` (Máy chủ xử lý) và `Client` (Giao diện người dùng).

## 🛠️ Yêu cầu hệ thống & Môi trường
Để chạy được dự án, máy tính của bạn cần cài đặt:
- **Ngôn ngữ:** Java (JDK 8 trở lên)
- **IDE:** Khuyên dùng IntelliJ IDEA (hoặc Eclipse)
- **Cơ sở dữ liệu:** MySQL Server (hoặc XAMPP)
- **Thư viện bên ngoài cần thiết (External Libraries):**
  - `opencv-4120.jar` (Thư viện Java cho OpenCV)
  - `mysql-connector-j-8.4.0.jar` (Driver kết nối MySQL)
  - File lõi `opencv_java4140.dll` (Thư viện động của OpenCV cho Windows)

---

## 📂 Cấu trúc dự án
- `/Server`: Chứa mã nguồn phía máy chủ, chịu trách nhiệm quản lý kết nối đa luồng (Multi-threading), định tuyến tin nhắn, luồng video và thao tác với Database.
- `/Client`: Chứa mã nguồn phía máy trạm, bao gồm giao diện người dùng (Login, Chat, Video Call) và logic thu thập hình ảnh từ Camera.

---

## ⚙️ Hướng dẫn Cài đặt & Cấu hình

### 1. Cài đặt Cơ sở dữ liệu (Database)
1. Mở MySQL (thông qua MySQL Workbench hoặc phpMyAdmin).
2. Tạo một cơ sở dữ liệu mới (ví dụ: `videocall_db`) và tạo các bảng cần thiết (User, Friend, Message...).
3. Mở file `ConnectDatabase.java` trong thư mục `Server/src/config/`.
4. Cập nhật lại thông tin cấu hình cho đúng với máy của bạn: `URL`, `Username` (thường là root), và `Password`.

### 2. Thêm thư viện `.jar` vào IDE (IntelliJ IDEA)
Để code không bị báo lỗi đỏ chữ, bạn cần thêm 2 file jar (`opencv` và `mysql`):
1. Mở dự án bằng IntelliJ.
2. Nhấn tổ hợp phím `Ctrl + Alt + Shift + S` (hoặc vào **File > Project Structure**).
3. Ở menu bên trái, chọn **Modules**.
4. Chuyển sang tab **Dependencies** ở cửa sổ bên phải.
5. Nhấn vào dấu `+` (dưới cùng hoặc bên phải) > Chọn **JARs or directories...**.
6. Tìm và quét chọn 2 file `opencv-4120.jar` và `mysql-connector-j-8.4.0.jar` > Bấm **OK** > **Apply**.

### 3. Cấu hình thư viện lõi OpenCV (`.dll`)
Để dòng code `System.loadLibrary("opencv_java4140");` chạy được và gọi được Camera, hệ thống cần đọc được file `.dll`. Có 2 cách để nạp file này:
- **Cách 1 (Nhanh & Dễ nhất):** Copy file `opencv_java4140.dll` và dán trực tiếp vào **thư mục gốc** của dự án (ngang hàng với thư mục `Client` và `Server`).
- **Cách 2 (Add VM Options):** 
  1. Chọn file chạy Client (`Client.java`), nhấn vào nút **Edit Configurations** (cạnh nút Run màu xanh trên thanh công cụ).
  2. Mở rộng phần **Modify options** > Chọn **Add VM options**.
  3. Thêm dòng lệnh sau (nhớ đổi đường dẫn trỏ tới đúng thư mục chứa file dll trên máy bạn):
     `-Djava.library.path="C:\Đường\Dẫn\Tới\Thư\Mục\Chứa\File\DLL"`

---

## 🚀 Hướng dẫn khởi chạy ứng dụng (Run)

### Bước 1: Chạy Server trước
1. Mở thư mục `Server`, tìm file Main khởi chạy (thường là `Server.java` hoặc `ServerView.java`).
2. Click chuột phải -> Chọn **Run 'Server.main()'**.
3. Quan sát Terminal/Console, nếu in ra dòng chữ thông báo dạng *"Server is running on port..."* hoặc *"Database connected"* là thành công.

### Bước 2: Chạy Client sau
1. Mở thư mục `Client`, tìm file giao diện khởi động (thường là `LoginView.java` hoặc `Client.java`).
2. Click chuột phải -> Chọn **Run 'Client.main()'**.
3. Cửa sổ ứng dụng sẽ hiện lên. Để test tính năng chat và call video, bạn hãy **Run thêm một lần nữa** để mở Client thứ 2.
4. Đăng nhập bằng 2 tài khoản khác nhau ở 2 cửa sổ Client để thực hiện thao tác nhắn tin và gọi video chéo cho nhau.

---

## ⚠️ Khắc phục sự cố thường gặp (Troubleshooting)
- **Lỗi `java.lang.UnsatisfiedLinkError`:** File `.dll` của OpenCV chưa được cấu hình đúng chỗ. Hãy kiểm tra lại phần *Cấu hình thư viện lõi OpenCV* ở trên.
- **Lỗi `java.net.BindException: Address already in use`:** Port của Server đang bị chiếm dụng. Hãy kiểm tra Task Manager và tắt các tiến trình Java cũ đang chạy ngầm.
- **Không bật được Camera:** Đảm bảo rằng không có ứng dụng nào khác (Zoom, Meet, OBS) đang chiếm quyền sử dụng Camera của thiết bị.
