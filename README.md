# 📚 Booknet

> Hệ thống quản lý và chia sẻ sách trực tuyến được phát triển trên nền tảng **Spring Boot** và **Thymeleaf**.

---

## 📖 Giới thiệu (Overview)

**Booknet** là dự án web application phục vụ quản trị, tìm kiếm và tương tác sách trực tuyến, được xây dựng với kiến trúc hiện đại, bảo mật cao và dễ dàng mở rộng. Dự án thuộc **Group 3**.

---

## 🛠 Công nghệ sử dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản / Chi tiết |
| :--- | :--- | :--- |
| **Language** | Java | JDK 21 |
| **Framework** | Spring Boot | 4.x (Web MVC, Data JPA, Security) |
| **Template Engine** | Thymeleaf | Tích hợp Spring Security 6 dialect |
| **Database** | MySQL / Microsoft SQL Server | `mysql-connector-j`, `mssql-jdbc` |
| **Tooling & Build** | Maven | Maven Wrapper tích hợp sẵn (`mvnw`) |
| **Utility** | Lombok & DevTools | Hỗ trợ phát triển và reload nhanh |

---

## 📁 Cấu trúc thư mục (Project Structure)

```text
Booknet/
├── .mvn/wrapper/               # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/grp3/java/booknet/
│   │   │   └── BooknetApplication.java   # Spring Boot Main Entry
│   │   └── resources/
│   │       ├── static/                  # Static assets (CSS, JS, Images)
│   │       ├── templates/               # Thymeleaf HTML templates
│   │       └── application.properties   # Cấu hình ứng dụng & Database
│   └── test/                            # Unit tests & Integration tests
├── pom.xml                              # Maven dependencies & plugins
├── mvnw / mvnw.cmd                      # Maven Wrapper scripts
└── README.md                            # Tài liệu dự án
```

---

## 🚀 Hướng dẫn cài đặt & Khởi chạy (Getting Started)

### 1. Yêu cầu hệ thống (Prerequisites)
- **Java Development Kit (JDK)**: Phiên bản 21 trở lên.
- **Cơ sở dữ liệu**: MySQL Server (phiên bản 8.0+) hoặc Microsoft SQL Server.
- **Git** (để clone mã nguồn).

### 2. Cấu hình cơ sở dữ liệu (Database Setup)
Mở file `src/main/resources/application.properties` và cấu hình kết nối database phù hợp:

```properties
spring.application.name=Booknet

# Cấu hình kết nối MySQL (Ví dụ)
# spring.datasource.url=jdbc:mysql://localhost:3306/booknet_db?useSSL=false&serverTimezone=UTC
# spring.datasource.username=root
# spring.datasource.password=your_password
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate configuration
# spring.jpa.hibernate.ddl-auto=update
# spring.jpa.show-sql=true
# spring.jpa.properties.hibernate.format_sql=true
```

### 3. Chạy ứng dụng (Run Application)

Sử dụng Maven Wrapper có sẵn trong thư mục dự án:

**Trên Windows (PowerShell/CMD):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Trên Linux / macOS:**
```bash
./mvnw spring-boot:run
```

Sau khi khởi chạy thành công, truy cập ứng dụng tại: [http://localhost:8080](http://localhost:8080)

### 4. Đóng gói ứng dụng (Build JAR)
```powershell
.\mvnw.cmd clean package
```
File `.jar` sẽ được tạo ra tại thư mục `target/Booknet-0.0.1-SNAPSHOT.jar`. Để chạy file jar:
```powershell
java -jar target/Booknet-0.0.1-SNAPSHOT.jar
```

---

## 👥 Nhóm phát triển (Contributors)
- **Team**: Group 3 (`grp3.java`)
- **Repository**: [nguyenlebinhan/Booknet](https://github.com/nguyenlebinhan/Booknet)
