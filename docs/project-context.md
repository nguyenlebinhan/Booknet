# Context dự án Booknet

> Tài liệu để mention khi giao việc tiếp theo: `@docs/project-context.md`.
> Cập nhật ngày 16/11/2026; bổ sung trạng thái trang quản lý sách và kết quả kiểm thử mới nhất.
> Đây là ảnh chụp trạng thái dự án, không xác nhận nhánh đã merge vào `main`.
> Trước mỗi công việc, đọc lại source liên quan và Git diff vì dự án có thể đã thay đổi.

## Cập nhật Contact và mật khẩu Google — 16/11/2026

Phần này thay thế các mô tả cũ bên dưới về Contact chưa gửi mail và Google-only không tạo được mật khẩu.

- Contact chuyển sang `ContactController`, DTO `ContactForm` và `ContactMailService`.
  POST kiểm tra validation và CSRF, gửi SMTP đến `CONTACT_TO`, dùng `MAIL_FROM` và Reply-To của người điền form.
  Chỉ redirect với thông báo thành công sau khi SMTP chấp nhận gửi; lỗi render lại form và giữ dữ liệu.
  Chưa lưu feedback vào DB. Cần cấu hình SMTP thật và `CONTACT_TO`; không commit `.env`.
- Google-only có nút Set password trên Profile/Password settings. `POST /account/set-password`
  gửi link đến email lấy từ tài khoản đang đăng nhập; dùng chung token RESET_PASSWORD và trang `/reset-password`.
  Token SHA-256, 15 phút, một lần, cooldown 60 giây. SMTP lỗi rollback token và hiện lỗi.
- Tài khoản có password hash được đăng nhập bằng email/mật khẩu, không phụ thuộc provider gốc.
  Forgot/reset hỗ trợ tài khoản Google đang hoạt động, đã xác thực. Google identity và user ID được giữ nguyên.
  Đặt/đổi/reset mật khẩu tăng `auth_version` và thu hồi phiên cũ; không tự gộp tài khoản trùng email.
- Bộ test hiện có 125 trường hợp, bao gồm 4 test khởi tạo tài khoản seed. Test dùng H2 và mock Google/SMTP;
  kết quả đạt không chứng minh toàn bộ luồng Google/nhận thư thực tế. Chi tiết: [account-api.md](account-api.md).

## Catalog công khai — 16/11/2026

- Trang Thymeleaf `/catalog` tìm theo tên sách, tác giả, ISBN hoặc mã sách. Khách và thành viên chỉ thấy
  sách `PUBLISHED`; librarian/admin còn thấy sách nháp và ngừng phát hành để tiếp tục quản lý sách.
  Sách đã xóa không hiển thị. Có lọc thể loại, trạng thái bản sao, ngôn ngữ và năm xuất bản; sắp xếp
  theo mới thêm, tên sách hoặc năm xuất bản; phân trang 6 sách.
- Số bản sao và khả năng mượn/đặt trước lấy từ trạng thái `BookCopy`, không dùng dữ liệu mẫu.
  Sắp xếp theo độ phổ biến chưa có vì hệ thống chưa lưu hoặc tổng hợp lượt xem/mượn cho mục đích đó.
- Ô tìm kiếm và các liên kết Explore trên Home mở trang catalog. Các sách trưng bày khác trên Home
  vẫn là nội dung mẫu, không đại diện cho kết quả tìm kiếm hay tồn kho thật.
- Librarian/admin có liên kết thêm và sửa sách trên catalog; quyền được kiểm tra riêng cho `/admin/books/**`.

## Quản lý sách cho librarian — 16/11/2026

- Trang duyệt sách chung là `/catalog`; URL cũ `/admin/books` chuyển hướng đến catalog.
  `/admin/books/create` và `/admin/books/{id}/edit` vẫn hỗ trợ tạo sách nháp, sửa metadata và xuất bản.
- ID nội bộ của sách tự tăng trong database và không nhập trên form; `bookCode` là mã catalog riêng.
- Trang sửa hiển thị tồn kho bản sao, cho phép thêm/sửa bản sao qua các luồng hiện có,
  và có xác nhận xóa theo mã sách. Nếu có lịch sử tham chiếu không thể xóa, có thể ngừng phát hành sách.
- Quyền quản lý sách dành cho `ROLE_LIBRARIAN` và `ROLE_ADMIN`; các trang `/admin/**` khác vẫn theo
  quy tắc admin hiện có. Các test MVC kiểm tra quyền librarian/admin và render template nguồn.
- Ảnh bìa hiện dùng URL vì entity lưu `coverUrl`; chưa có upload ảnh. Không hiển thị checkbox đặt trước
  khi hết bản sao vì mô hình hiện tại không có thuộc tính tương ứng.

## Local seed accounts — 16/11/2026

- `DevelopmentUserDataInitializer` là `CommandLineRunner` chỉ hoạt động với profile `local`.
- Có thể cấu hình `BOOKNET_SEED_ADMIN_EMAIL` / `BOOKNET_SEED_ADMIN_PASSWORD` và
  `BOOKNET_SEED_LIBRARIAN_EMAIL` / `BOOKNET_SEED_LIBRARIAN_PASSWORD`,
  `BOOKNET_SEED_USER_EMAIL` / `BOOKNET_SEED_USER_PASSWORD` trong `.env`.
- Mỗi cặp credential đều tùy chọn. Nếu đặt thiếu email hoặc mật khẩu, hoặc mật khẩu không đạt
  chính sách tài khoản, ứng dụng báo lỗi cấu hình. Tài khoản hiện hữu được giữ nguyên; mật khẩu
  chỉ lưu dưới dạng BCrypt hash. Không có credential mặc định hoặc mật khẩu được ghi log.

## Admin dashboard — 16/11/2026

- Đăng nhập bằng `ROLE_ADMIN` mở `/admin/dashboard`; member/librarian giữ redirect hiện có.
- Dashboard hiển thị số liệu hiện tại từ database (sách đã xuất bản, thành viên, bản sao đang mượn,
  khoản mượn quá hạn, sách nháp và reservation chờ duyệt). Không có số liệu lịch sử/audit giả.
- Sidebar liên kết đến các trang đã hoạt động; tính năng chưa có backend được đánh dấu chưa khả dụng.
- Guest/member tiếp tục dùng navbar; admin và librarian dùng sidebar trên các trang chung và trang quản lý.

## 1. Những điều cần nắm trước khi làm

- Booknet là ứng dụng quản lý thư viện của Group 3, package gốc `grp3.java.booknet`.
- Kiến trúc chính là **Spring MVC + Thymeleaf**, render HTML từ server.
- Luồng người dùng dùng `@Controller`, form DTO, `BindingResult`, `Model`, flash message
  và redirect. Đã bỏ các controller JSON tài khoản; không còn endpoint `/api/auth/**`, `/api/account/**`.
- Đọc [AGENTS.md](../AGENTS.md), controller, service, security, DTO, template và test của
  tính năng trước khi sửa. Không chỉ dựa vào tài liệu hoặc tên Entity để kết luận đã có chức năng.
- Giữ thiết kế và CSS hiện tại khi nối backend; chỉ thay đổi giao diện trong phạm vi yêu cầu.
- Thông báo UI, validation và email tài khoản dùng tiếng Anh. DTO khai báo message tiếng Anh;
  `auth.js` đặt thông báo HTML validation bằng tiếng Anh và xóa lỗi khi người dùng sửa input.
- Phân biệt rõ: có Entity/Repository, có xử lý backend, có trang sử dụng được và đã kiểm thử
  môi trường thật là các mức hoàn thành khác nhau.
- Chi tiết tài khoản, request và cấu hình: [account-api.md](account-api.md).

## 2. Công nghệ thực tế trong source

| Thành phần | Hiện trạng |
| --- | --- |
| Java | JDK 21 |
| Spring Boot | 4.1.2, phiên bản khai báo tại `pom.xml` |
| Web | Spring Web MVC, Thymeleaf, Bean Validation |
| Security | Spring Security, form login, OAuth2 client và OpenID Connect cho Google |
| Dữ liệu | Spring Data JPA/Hibernate, MySQL connector |
| Email | Spring Mail, SMTP cấu hình qua biến môi trường |
| Tiện ích | Lombok, DevTools |
| Build | Maven và Maven Wrapper |
| Test | JUnit, MockMvc, Mockito, H2 với profile `test` |
| Frontend | HTML Thymeleaf, CSS, JavaScript thuần; không có SPA frontend riêng |

[pom.xml](../pom.xml) là nguồn xác nhận dependencies. README có đoạn cũ nhắc SQL Server,
nhưng cấu hình/dependency hiện tại dùng MySQL; chưa coi SQL Server là môi trường đã hỗ trợ.

## 3. Bản đồ mã nguồn

```text
src/main/java/grp3/java/booknet/
  BooknetApplication.java       # Entry point, bật JPA auditing
  config/
    account/                   # Clock và quy tắc tài khoản
    development/               # Dữ liệu seed local
    security/                  # Security và trạng thái OAuth link
  controller/
    account/                   # Đăng nhập, hồ sơ, xác minh và liên kết tài khoản
    admin/                     # Dashboard quản trị
    book/                      # Quản lý sách và bản sao
    borrowing/                 # Yêu cầu mượn
    catalog/                   # Duyệt danh mục
    contact/                   # Liên hệ
    home/                      # Trang chủ
    member/                    # Quản lý thành viên
  dto/
    admin/                     # Dữ liệu dashboard
    book/                      # Form và dữ liệu sách/bản sao
    borrowing/                 # Form và dữ liệu yêu cầu mượn
    catalog/                   # Bộ lọc và kết quả catalog
    contact/                   # Form liên hệ
    member/                    # Dữ liệu danh sách/chi tiết thành viên
    user/                      # Request/response tài khoản
      legacy/                  # DTO cũ chưa được gỡ bỏ
  service/
    account/                   # Tài khoản, xác thực, email và Google
    admin/                     # Dashboard quản trị
    book/                      # Sách và bản sao
    borrowing/                 # Yêu cầu mượn
    catalog/                   # Duyệt danh mục
    contact/                   # Gửi liên hệ
    member/                    # Danh sách thành viên
    impl/
      account/                 # Cài đặt service tài khoản
      admin/                   # Cài đặt dashboard
      book/                    # Cài đặt sách và bản sao
      borrowing/               # Cài đặt yêu cầu mượn
      catalog/                 # Cài đặt catalog
  security/
    dto/                       # Principal details
    principal/                 # Principal local/OAuth/OIDC
    service/                   # Adapter xác thực OAuth/OIDC/local
    session/                   # Kiểm tra và thu hồi phiên
  entity/
    BaseEntity.java            # Trường và auditing dùng chung
    user/                      # User, OAuthIdentity, AccountToken
    book/                      # Book, BookCopy, Author, Category, Publisher
    book_service/              # Loan, Reservation, Fine
    notification/              # Notification
  repository/
    user/                      # Repository tài khoản
    book/                      # Repository sách và catalog
    book_service/              # Repository mượn và tiền phạt
    notification/              # Repository thông báo
  enums/
    user/                      # Vai trò, trạng thái, provider và token
    book/                      # Trạng thái phát hành và bản sao
    book_service/              # Trạng thái mượn, đặt trước và tiền phạt
    notification/              # Loại thông báo
  exception/account/           # Lỗi nghiệp vụ tài khoản

src/main/resources/
  templates/
    account/                   # Hồ sơ và cài đặt tài khoản
    admin/
      books/                   # Tạo/sửa sách
      fragments/               # Sidebar dashboard
      dashboard.html
    auth/                       # Đăng nhập, đăng ký, khôi phục mật khẩu
    borrowing/                  # Xác nhận mượn sách
    catalog/                    # Duyệt danh mục
    member/                     # Danh sách, chi tiết và fragments quản lý thành viên
    pages/                      # Home, About, Contact
    shared/                     # Navbar, footer và fragments auth dùng chung
  static/css/
    admin/                     # Dashboard and book management
    base/                      # Design tokens and shared base styles
    layout/                    # Navbar and footer
    member/                    # Member management
    pages/                     # Auth, account, home, about, contact, catalog, loans
  static/js/auth.js            # Toggle password, strength meter, countdown...
  static/images/               # Ảnh và icon đang dùng
  application*.properties      # Cấu hình chung, local, prod

src/test/java/grp3/java/booknet/ # Kiểm thử ứng dụng, tài khoản, Google và Thymeleaf
src/test/resources/            # application-test.properties, H2 riêng
docs/                          # Context dự án và tài liệu luồng tài khoản
```

Templates được nhóm theo chức năng trong các thư mục `account/`, `admin/`, `auth/`, `borrowing/`,
`catalog/`, `member/`, `pages/` và `shared/`; tên view và fragment Thymeleaf dùng đường dẫn tương ứng.
About/Contact giữ giao diện từ main, đã dịch nội dung, nhãn form, FAQ và mô tả ảnh sang tiếng Anh.
`pages/about.html` và `pages/contact.html` dùng `lang="en"` và fragment từ `shared/`. Contact dùng `auth.js`
cho HTML validation tiếng Anh và bản đồ nhúng đặt ngôn ngữ English. Contact gửi email đồng bộ qua SMTP,
dùng Reply-To của người gửi; chưa lưu feedback vào DB.
About/Contact phải nạp `base/tokens.css` trước `layout/navbar.css`/`layout/footer.css` để các biến màu của fragment được định nghĩa.
Giữ đúng tên và chữ hoa/thường khi tham chiếu fragment để tránh lỗi khi đóng gói/chạy Linux.

## 4. Chức năng hiện có và phần còn thiếu

| Nhóm | Đã có | Chưa hoàn chỉnh/chưa triển khai |
| --- | --- | --- |
| Đăng ký local | Form Thymeleaf, validation, lưu điện thoại, BCrypt, gửi xác thực | Gửi email thật phụ thuộc SMTP |
| Đăng nhập local | Email/mật khẩu, phiên, remember-me, logout, kiểm tra trạng thái | Chưa xác nhận triển khai thực tế |
| Google | Tạo tài khoản, đăng nhập, liên kết local, xử lý OAuth2/OIDC | Chưa có hủy liên kết; cần thử Google thật |
| Email và mật khẩu | Xác thực/gửi lại email, quên/reset và đổi mật khẩu qua form Thymeleaf | SMTP thật cần kiểm tra môi trường |
| Hồ sơ cá nhân | Service, DTO, form Thymeleaf xem/cập nhật hồ sơ và link menu | Chưa xác nhận triển khai thực tế |
| Home | Có giao diện, dữ liệu trưng bày mẫu và trạng thái/liên kết Google; ô tìm kiếm mở `/catalog` | Kệ sách/genre trưng bày trên Home vẫn là dữ liệu mẫu |
| Catalog | Trang công khai `/catalog` tìm kiếm sách đã xuất bản, lọc genre/trạng thái/ngôn ngữ/năm, sắp xếp và phân trang; số lượng bản sao lấy từ DB | Chưa có dữ liệu lượt xem/mượn tổng hợp để xếp hạng phổ biến hoặc trang chi tiết sách |
| Quản lý sách | CRUD quản trị sách/bản sao, service, repository và kiểm thử | — |
| Yêu cầu mượn | Form `/loans/confirm`; POST tạo `Reservation` trạng thái PENDING sau khi kiểm tra quota, fine và CSRF | Chưa có quy trình thủ thư duyệt, phát hành bản sao hoặc trả/gia hạn |
| Quản lý thành viên | Danh sách/tìm kiếm/chi tiết tại `/staff/members` và `/admin/users`; role/permission kiểm thử | Bộ lọc circulation chỉ chạy ở preview; điều khiển cập nhật role/trạng thái trên UI đang disabled, chưa có backend mutation |
| Đặt trước | Repository và tạo reservation chờ duyệt từ yêu cầu mượn | Chưa có hủy/hàng chờ và xử lý reservation của thủ thư |
| Tiền phạt | Entity và Repository `Fine` | Chưa có tính/phê duyệt/thanh toán phạt |
| Thông báo | Entity và Repository `Notification` | Chưa có luồng thông báo nghiệp vụ/gửi nhắc |
| Phân quyền | `ROLE_USER` được ánh xạ thành authority `ROLE_MEMBER`; có `ROLE_LIBRARIAN`, `ROLE_ADMIN` và quyền thành viên | Chưa có đầy đủ luồng kích hoạt thẻ, cập nhật vai trò/trạng thái và báo cáo |

Không coi nút, link menu hoặc dữ liệu mẫu trên Home là chứng cứ backend đã triển khai.
About/Contact và `/catalog` có MVC route công khai.

## 5. Luồng tài khoản và nơi cần đọc

| Phần | File chính |
| --- | --- |
| Đăng ký, hiển thị Login | [AuthController.java](../src/main/java/grp3/java/booknet/controller/account/AuthController.java), `UserServiceImpl`, `UserRegisterDto` |
| Quên/reset mật khẩu qua HTML | [PasswordRecoveryController.java](../src/main/java/grp3/java/booknet/controller/account/PasswordRecoveryController.java), `PasswordResetForm` |
| Xác thực/gửi lại email | [EmailVerificationController.java](../src/main/java/grp3/java/booknet/controller/account/EmailVerificationController.java) |
| Token, mật khẩu, hồ sơ | [AccountService.java](../src/main/java/grp3/java/booknet/service/account/AccountService.java) |
| Gửi email và tạo link | `AccountMailService` trong `service/account/` |
| Google và liên kết | [OAuthAccountService.java](../src/main/java/grp3/java/booknet/service/account/OAuthAccountService.java), `AccountLinkController` |
| Google adapters | `CustomOAuth2UserService`, `CustomOidcUserService` trong `security/service/` và principals trong `security/principal/` |
| Phân quyền, phiên | [SecurityConfig.java](../src/main/java/grp3/java/booknet/config/security/SecurityConfig.java), `AccountSessionFilter`, `CustomUserDetailsService` |
| Chính sách mật khẩu/thời hạn | [AccountPolicy.java](../src/main/java/grp3/java/booknet/config/account/AccountPolicy.java) |
| Hồ sơ/đổi mật khẩu qua HTML | [AccountController.java](../src/main/java/grp3/java/booknet/controller/account/AccountController.java), `ProfileUpdateRequest`, `ChangePasswordRequest`, `ProfileResponse`, `profile.html`, `change-password.html` |

### Đăng ký và đăng nhập local

1. `GET /register` render form; `POST /register` bind họ tên, email, điện thoại,
   mật khẩu, xác nhận và đồng ý điều khoản.
2. Tạo tài khoản LOCAL, role USER, đang hoạt động nhưng **chưa xác thực email**.
3. Email dẫn đến `/verify-email?token=...`; xác thực xong redirect Login với flash message.
4. `POST /login` do Spring Security xử lý với `email`, `password` và tùy chọn `remember-me`.
5. Email đăng nhập được trim/chuyển chữ thường. Tài khoản phải enabled, đã xác thực,
   trạng thái ACTIVE. Thành công về Home. Remember-me tối đa 7 ngày; logout dùng POST và CSRF.

### Quên, reset và đổi mật khẩu

- `GET/POST /forgot-password` render/nhận form email; phản hồi chung cho tài khoản đủ
  hoặc không đủ điều kiện. Link email mở `/reset-password?token=...` và render HTML.
- `POST /reset-password` nhận **`token`, `password`, `confirmPassword`**. Sai dữ liệu
  render lại với lỗi; thành công xóa phiên hiện tại và redirect `/login?reset=true`.
- `GET/POST /account/change-password` dùng `currentPassword`, `newPassword`, `confirmPassword`;
  yêu cầu đăng nhập, lỗi render lại, thành công xóa phiên/cookie và redirect Login với flash message.
- Token reset có hiệu lực **15 phút**, token xác thực email **24 giờ**, dùng một lần.
  Mỗi loại email có cooldown 60 giây/tài khoản; gửi lại hợp lệ hủy token cũ.
- Token ngẫu nhiên 256 bit; database chỉ lưu SHA-256. Dùng transaction và khóa khi
  cấp/tiêu thụ token, gồm locking read phù hợp MySQL REPEATABLE READ.
- Đổi/reset mật khẩu tăng `users.auth_version`, hủy token reset còn lại; phiên local
  và Google cũ bị từ chối ở request tiếp theo. Remember-me cũ hết hiệu lực theo password hash.
- Mật khẩu mới: **8–32 ký tự, có chữ hoa, chữ thường, số, tối đa 72 byte UTF-8**.
  Quy tắc này không ép người dùng đổi mật khẩu cũ chỉ để đăng nhập.
- Google-only không có password hash ban đầu; có thể yêu cầu link Set password từ Profile.
  Reset/set password giữ nguyên provider GOOGLE và identity; sau đó email/mật khẩu và Google đều đăng nhập cùng user.

### Đăng nhập và liên kết Google

- Scope Google là `openid,profile,email`: phải duy trì cả xử lý OIDC, không chỉ OAuth2 thường.
- Google đã có identity: đăng nhập đúng user qua `sub`, không dựa vào trùng email.
- Google mới, email chưa có trong hệ thống: tạo user GOOGLE đã xác thực email.
- Email trùng user LOCAL chưa liên kết: **không tự gộp**, yêu cầu đăng nhập bằng mật khẩu
  rồi liên kết chủ động từ Home qua `POST /account/link/google`.
- Liên kết yêu cầu phiên local đăng nhập bằng mật khẩu, không chỉ phiên remember-me;
  ý định liên kết gắn user ID, phiên và thời hạn **10 phút**.
- Có thể liên kết Google khác email local. Giữ nguyên user, mật khẩu, role và dữ liệu local.
- Một identity Google chỉ thuộc một user; mỗi user chỉ có một identity Google.
  Ràng buộc DB kiểm soát `(provider, provider_user_id)` và `(user_id, provider)`.
- Callback sai state, hết hạn, identity thuộc người khác hoặc tài khoản ngừng hoạt động
  phải bị từ chối. Hủy/lỗi OAuth có thể làm người dùng cần đăng nhập lại.

### Vai trò và hồ sơ

- Role hiện có: `ROLE_GUEST`, `ROLE_USER`, `ROLE_LIBRARIAN`, `ROLE_ADMIN`.
  `ROLE_USER` được giữ nguyên trong DB nhưng có authority `ROLE_MEMBER` để tương thích tài khoản cũ.
  Quyền member/staff/admin được kiểm soát ở filter chain và method security.
- Status user: `ACTIVE`, `SUSPENDED`, `EXPIRED`; provider: `LOCAL`, `GOOGLE`.
- Form `GET/POST /account/profile` chỉ cho sửa họ tên, điện thoại, địa chỉ, avatar HTTP(S). Email, mật khẩu,
  role, provider, status và memberCode không được sửa qua request hồ sơ.
- Hồ sơ dùng DTO an toàn để render; form dùng constructor binding của record và validation.
  Đổi mật khẩu không điền lại giá trị mật khẩu trong HTML lỗi. Google-only thấy Set password trong Profile;
  trang Change password hướng dẫn đăng nhập Google nếu chưa có password hash.

## 6. Dữ liệu và cấu hình cần lưu ý

- `BaseEntity` quản lý `createdAt`/`updatedAt`; entry point bật JPA auditing.
- `User` lưu cờ xác thực, enabled, status và authVersion; không khôi phục logic tự ép
  `emailVerified=true` trong `@PrePersist`.
- Các tài khoản lịch sử đã có `email_verified=true` được giữ nguyên; đó không phải
  bằng chứng chúng đã được kiểm tra email lại theo luồng mới.
- `OAuthIdentity.providerSubject` được giữ đồng bộ với `providerUserId` vì schema cũ có
  cột bắt buộc này. Đọc schema thực tế trước khi xóa/đổi trường tương thích.
- Khi bắt đầu CRUD tác giả/thể loại/NXB, kiểm tra kiểu ID: hiện Entity dùng `Integer`
  còn Repository của ba nhóm này khai báo `Long`; đây là chỗ cần rà soát trước khi mở rộng.
- Cấu hình đọc `.env` tại thư mục dự án hoặc thư mục cha; có profile `local`, `prod`.
  `.env.example` chứa tên biến để cấu hình, không dùng tài liệu context để lưu credentials.
- Các nhóm biến: `BOOKNET_DB_*`, `DB_*`, `GOOGLE_CLIENT_ID/SECRET`, `MAIL_*`,
  `APP_BASE_URL`, `BOOKNET_REMEMBER_ME_KEY`, `SPRING_PROFILES_ACTIVE`, `PORT`.
- Port mặc định 8080. SMTP local mặc định `localhost:1025`; ứng dụng không tự chạy SMTP server.
- `APP_BASE_URL` quyết định link email. Google redirect URI là
  `{APP_BASE_URL}/login/oauth2/code/google`.
- Remember-me key để trống sẽ dùng khóa ngẫu nhiên mỗi lần chạy, khiến cookie cũ mất hiệu lực
  sau restart. Triển khai nhiều instance/restart cần cấu hình cùng khóa ổn định.
- Local mặc định `ddl-auto=update`. Nếu prod dùng `validate`/`none`, cần migration cho
  `account_tokens`, `users.auth_version` và unique constraints OAuth trước khi chạy.
  H2 test dùng `create-drop` trên database riêng; không áp dụng vào database thực.

## 7. Chạy và kiểm thử

Chọn JDK 21 cho cả IDE và Maven; kiểm tra `java -version` và `mvn -v` nếu đã cài Maven.
JDK mà lệnh `java` sử dụng có thể khác JDK Maven lấy qua `JAVA_HOME`.

```powershell
# Tại thư mục gốc dự án, dùng Maven Wrapper
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Nếu Wrapper lỗi do môi trường/script hoặc tải distribution, dùng Maven đã cài với JDK 21:

```powershell
mvn test
mvn spring-boot:run
```

Xác nhận ngày 16/11/2026 bằng JDK 21 và Maven Wrapper, profile `test`: **125 test đạt, không lỗi/skip**.
Ba lớp `AdminBookControllerFeature1/2/3Tests` đạt 13/13, kiểm tra chuyển hướng URL cũ,
render form, quyền librarian/admin và xuất bản. Toàn bộ test cũng bao gồm luồng tài khoản, OAuth,
Contact, catalog, bản sao/vòng đời sách, mượn sách, thành viên và seed runner.
Catalog có test H2/MockMvc cho tìm kiếm, lọc, phân trang, giữ bộ lọc qua trang, HTML, trạng thái rỗng,
và phân biệt sách hiển thị công khai với sách nội bộ dành cho librarian/admin.
Local seed runner có 4 test đơn vị cho tạo tài khoản, idempotency, disabled-by-default
và kiểm tra credential.

Test dùng profile `test`, H2 riêng và mock SMTP/Google. Kết quả này **không xác nhận**
Google client, gửi email thật, migration hoặc concurrency trên MySQL triển khai thực.
Thay đổi tài liệu đơn thuần không cần chạy lại toàn bộ test; sửa luồng tài khoản phải
kiểm thử cả controller/template, không chỉ gọi service.

## 8. Cách dùng context cho công việc tiếp theo

Ví dụ mention:

```text
@docs/project-context.md
Hãy đọc context, AGENTS.md và source liên quan trước khi làm.
Yêu cầu: [mô tả chức năng].
Phạm vi UI: [giữ thiết kế / bổ sung form / tạo trang mới theo yêu cầu].
```

Trước khi triển khai: kiểm tra branch, working tree và những phần đã tồn tại; xác định
DTO, service, controller, template, rule security và schema bị ảnh hưởng. Không ghi đè
thay đổi đang có của người khác. Khi giao việc push, xác nhận đúng nhánh đích từ yêu cầu,
không mặc định `HaiQH` cho mọi công việc và không tự merge vào `main`.

Sau thay đổi có ảnh hưởng kiến trúc/chức năng: cập nhật context này cùng
[account-api.md](account-api.md) nếu liên quan tài khoản, ghi rõ phần đã làm, phần còn thiếu
và bằng chứng kiểm thử. Source hiện tại và yêu cầu mới của người dùng quyết định cách làm;
không coi ảnh chụp context cũ là chỉ dẫn để giữ lại một lỗi đã được sửa.
