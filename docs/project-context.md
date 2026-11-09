# Context dự án Booknet

> Tài liệu để mention khi giao việc tiếp theo: `@docs/project-context.md`.
> Cập nhật ngày 09/11/2026, theo source tại commit `bab1cb6` trên nhánh `HaiQH`.
> Đây là ảnh chụp trạng thái dự án, không xác nhận nhánh đã merge vào `main`.
> Trước mỗi công việc, đọc lại source liên quan và Git diff vì dự án có thể đã thay đổi.

## 1. Những điều cần nắm trước khi làm

- Booknet là ứng dụng quản lý thư viện của Group 3, package gốc `grp3.java.booknet`.
- Kiến trúc chính là **Spring MVC + Thymeleaf**, render HTML từ server.
- Luồng người dùng dùng `@Controller`, form DTO, `BindingResult`, `Model`, flash message
  và redirect. JSON API chỉ bổ sung, không thay thế form Thymeleaf đang có.
- Đọc [AGENTS.md](../AGENTS.md), controller, service, security, DTO, template và test của
  tính năng trước khi sửa. Không chỉ dựa vào tài liệu hoặc tên Entity để kết luận đã có chức năng.
- Giữ thiết kế và CSS hiện tại khi nối backend; chỉ thay đổi giao diện trong phạm vi yêu cầu.
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
  config/                      # Security, Clock, quy tắc tài khoản
  controller/                  # MVC HTML và JSON API bổ sung
  dto/                         # Form/request/response; không bind trực tiếp Entity
  service/                     # Nghiệp vụ tài khoản, token, email và Google
  security/                    # Principal local/OAuth/OIDC, kiểm tra phiên
  entity/
    user/                      # User, OAuthIdentity, AccountToken
    book/                      # Book, BookCopy, Author, Category, Publisher
    book_service/              # Loan, Reservation, Fine
    notification/              # Notification
  repository/                  # JPA repositories theo các nhóm Entity
  enums/                       # Vai trò, trạng thái, provider và nghiệp vụ
  exception/                   # AccountException

src/main/resources/
  templates/                   # Trang và fragments Thymeleaf
  static/css/                  # auth, home, navbar, footer, tokens...
  static/js/auth.js            # Toggle password, strength meter, countdown...
  static/images/               # Ảnh và icon đang dùng
  application*.properties      # Cấu hình chung, local, prod

src/test/java/grp3/java/booknet/ # Kiểm thử ứng dụng, tài khoản, Google và Thymeleaf
src/test/resources/            # application-test.properties, H2 riêng
docs/                          # Context dự án và tài liệu luồng tài khoản
```

Template hiện có: `home.html`, `login.html`, `register.html`, `forgot-password.html`,
`reset-password.html`, `authFragments.html`, `Navbar.html`, `footer.html`.
Giữ đúng tên và chữ hoa/thường khi tham chiếu fragment để tránh lỗi khi đóng gói/chạy Linux.

## 4. Chức năng hiện có và phần còn thiếu

| Nhóm | Đã có | Chưa hoàn chỉnh/chưa triển khai |
| --- | --- | --- |
| Đăng ký local | Form Thymeleaf, validation, lưu điện thoại, BCrypt, gửi xác thực | Gửi email thật phụ thuộc SMTP |
| Đăng nhập local | Email/mật khẩu, phiên, remember-me, logout, kiểm tra trạng thái | Chưa xác nhận triển khai thực tế |
| Google | Tạo tài khoản, đăng nhập, liên kết local, xử lý OAuth2/OIDC | Chưa có hủy liên kết; cần thử Google thật |
| Email và mật khẩu | Xác thực/gửi lại email, quên/reset qua form, đổi mật khẩu qua API | Đổi mật khẩu chưa có template riêng |
| Hồ sơ cá nhân | Service, DTO và API xem/cập nhật hồ sơ | Chưa có template/form hồ sơ |
| Home | Có giao diện, dữ liệu sách mẫu, trạng thái/liên kết Google | Chưa nối catalog/tìm kiếm sách thực |
| Quản lý sách | Entity và Repository cho sách, bản sao, tác giả, thể loại, NXB | Chưa có đầy đủ service/controller/trang CRUD |
| Mượn/trả/gia hạn | Entity và Repository `Loan` | Chưa có luồng nghiệp vụ hoàn chỉnh |
| Đặt trước | Entity và Repository `Reservation` | Chưa có luồng đặt/hủy/hàng chờ |
| Tiền phạt | Entity và Repository `Fine` | Chưa có tính/phê duyệt/thanh toán phạt |
| Thông báo | Entity và Repository `Notification` | Chưa có luồng thông báo nghiệp vụ/gửi nhắc |
| Quản trị | Rule `/admin/**` yêu cầu ADMIN | Chưa có dashboard/quản lý người dùng/báo cáo |

Không coi nút, link menu hoặc dữ liệu mẫu trên Home là chứng cứ backend đã triển khai.
Các đường dẫn catalog/search/services/about/contact cần được đối chiếu controller khi làm tiếp.

## 5. Luồng tài khoản và nơi cần đọc

| Phần | File chính |
| --- | --- |
| Đăng ký, hiển thị Login | [AuthController.java](../src/main/java/grp3/java/booknet/controller/AuthController.java), `UserServiceImpl`, `UserRegisterDto` |
| Quên/reset mật khẩu qua HTML | [PasswordRecoveryController.java](../src/main/java/grp3/java/booknet/controller/PasswordRecoveryController.java), `PasswordResetForm` |
| Xác thực/gửi lại email | [EmailVerificationController.java](../src/main/java/grp3/java/booknet/controller/EmailVerificationController.java) |
| Token, mật khẩu, hồ sơ | [AccountService.java](../src/main/java/grp3/java/booknet/service/AccountService.java) |
| Gửi email và tạo link | `AccountMailService` |
| Google và liên kết | [OAuthAccountService.java](../src/main/java/grp3/java/booknet/service/OAuthAccountService.java), `AccountLinkController` |
| Google adapters | `CustomOAuth2UserService`, `CustomOidcUserService` và các principal tương ứng |
| Phân quyền, phiên | [SecurityConfig.java](../src/main/java/grp3/java/booknet/config/SecurityConfig.java), `AccountSessionFilter`, `CustomUserDetailsService` |
| Chính sách mật khẩu/thời hạn | [AccountPolicy.java](../src/main/java/grp3/java/booknet/config/AccountPolicy.java) |
| API JSON bổ sung | `AccountAuthApiController`, `AccountApiController`, `AccountApiExceptionHandler` |

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
- JSON API reset dùng **`newPassword`**, khác tên trường `password` của form HTML.
- Token reset có hiệu lực **15 phút**, token xác thực email **24 giờ**, dùng một lần.
  Mỗi loại email có cooldown 60 giây/tài khoản; gửi lại hợp lệ hủy token cũ.
- Token ngẫu nhiên 256 bit; database chỉ lưu SHA-256. Dùng transaction và khóa khi
  cấp/tiêu thụ token, gồm locking read phù hợp MySQL REPEATABLE READ.
- Đổi/reset mật khẩu tăng `users.auth_version`, hủy token reset còn lại; phiên local
  và Google cũ bị từ chối ở request tiếp theo. Remember-me cũ hết hiệu lực theo password hash.
- Mật khẩu mới: **8–32 ký tự, có chữ hoa, chữ thường, số, tối đa 72 byte UTF-8**.
  Quy tắc này không ép người dùng đổi mật khẩu cũ chỉ để đăng nhập.
- Google-only chưa có mật khẩu local; không tự chuyển provider hoặc tạo mật khẩu qua reset.

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

- Role hiện có: `ROLE_GUEST`, `ROLE_USER`, `ROLE_ADMIN`. Chưa có role LIBRARIAN riêng;
  tab Member/Librarian trên Login không phải cơ chế gán quyền.
- Status user: `ACTIVE`, `SUSPENDED`, `EXPIRED`; provider: `LOCAL`, `GOOGLE`.
- API hồ sơ chỉ cho sửa họ tên, điện thoại, địa chỉ, avatar HTTP(S). Email, mật khẩu,
  role, provider, status và memberCode không được sửa qua request hồ sơ.
- Khi làm template hồ sơ/đổi mật khẩu, dùng lại service/DTO và bổ sung MVC form handler;
  không trả trực tiếp Entity chứa dữ liệu nhạy cảm vào JSON.

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

Lần xác nhận gần nhất tại mốc source trên: **37 test đạt, không lỗi/skip**.

| Bộ test | Số test tại mốc này | Phạm vi |
| --- | --- | --- |
| `BooknetApplicationTests` | 1 | Khởi tạo application context |
| `AccountFlowTests` | 20 | Tài khoản, API, token, race reset, phiên, quyền hồ sơ, CSRF |
| `OAuthCallbackTests` | 6 | Filter chain thật, redirect/callback/state của Google |
| `ThymeleafAccountFlowTests` | 10 | HTML render, tên field, CSRF từ form thật, lỗi/flash/redirect, email link |

Test dùng profile `test`, H2 riêng và mock SMTP/Google. Kết quả này **không xác nhận**
Google client, gửi email thật, migration hoặc concurrency trên MySQL triển khai thực.
Thay đổi tài liệu đơn thuần không cần chạy lại toàn bộ test; sửa luồng tài khoản phải
kiểm thử cả controller/template, không chỉ gọi service hoặc JSON API.

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
