# Account flows: Spring MVC and Thymeleaf

Luồng tài khoản dùng `@Controller`, form HTML Thymeleaf, `BindingResult`, `Model`,
flash message và redirect. Đã bỏ `AccountAuthApiController`, `AccountApiController`
và `AccountApiExceptionHandler`; các endpoint `/api/auth/**`, `/api/account/**` không còn được hỗ trợ.
Giữ tên tài liệu này để các liên kết hiện có tiếp tục hoạt động.

## Routes và form

| Route | Xử lý |
| --- | --- |
| `GET/POST /register` | Form `user`: `fullName`, `email`, `phone`, `password`, `confirmPassword`, `termsAccepted`; thành công redirect `/login?registered=true` |
| `GET/POST /login` | Render Login / Spring Security xác thực `email`, `password`, tùy chọn `remember-me` |
| `GET/POST /forgot-password` | Form email; redirect lại với thông báo chung |
| `GET /reset-password?token=...` | Kiểm tra token, render form hoặc thông báo link không hợp lệ |
| `POST /reset-password` | `token`, `password`, `confirmPassword`; thành công xóa phiên và redirect `/login?reset=true` |
| `GET /verify-email?token=...` | Xác thực rồi redirect Login với flash message thành công/lỗi |
| `GET/POST /verification/resend` | Dùng lại form email, gửi lại xác thực, thông báo chung |
| `GET/POST /account/profile` | Form `profileForm`: `fullName`, `phone`, `address`, `avatarUrl`; thành công redirect lại với flash message |
| `GET/POST /account/change-password` | Form `passwordForm`: `currentPassword`, `newPassword`, `confirmPassword`; thành công đăng xuất và redirect Login với flash message |
| `POST /account/link/google` | Bắt đầu liên kết Google qua form có CSRF |
| `POST /account/set-password` | Người đang đăng nhập yêu cầu email tạo mật khẩu; lấy user từ principal, không nhận email/ID từ form |
| `POST /logout` | Xóa phiên và redirect `/login?logout=true` |

Email chỉ gửi link HTML `/verify-email` hoặc `/reset-password`. Không cần JavaScript fetch
hay JSON client để hoàn tất các luồng. POST thành công redirect để tránh gửi lại form;
lỗi validation render lại view cùng dữ liệu và thông báo. Không điền lại mật khẩu vào HTML.
Hồ sơ và đổi mật khẩu truy cập từ menu tài khoản Home, dùng lại `auth.css` và `authFragments`.
Toàn bộ thông báo tài khoản, validation DTO và email xác thực/reset dùng tiếng Anh.
`auth.js` đặt thông báo HTML validation bằng tiếng Anh cho form đăng nhập, đăng ký, email,
reset, hồ sơ và đổi mật khẩu; lỗi custom validity được xóa khi input/change.

## Đăng ký và mật khẩu

- Local mới có role USER, enabled, status ACTIVE, **chưa xác thực email**. Phải xác thực trước khi đăng nhập.
- Email đăng nhập được trim/chuyển chữ thường; tài khoản phải enabled, đã xác thực và ACTIVE.
- Đăng ký kiểm tra họ tên 2–120 ký tự, email tối đa 120, điện thoại hợp lệ, xác nhận và điều khoản.
- Mật khẩu mới 8–32 ký tự, có chữ hoa, chữ thường, số; tối đa 72 byte UTF-8. Mật khẩu xác nhận phải trùng.
  Quy tắc tạo mới không chặn mật khẩu cũ khi đăng nhập.
- Token ngẫu nhiên 256 bit, chỉ lưu SHA-256, dùng một lần. Xác thực email 24 giờ, reset 15 phút.
- Mỗi loại email có cooldown 60 giây/tài khoản; cấp lại sau cooldown vô hiệu hóa link cũ.
- Forgot/resend trả kết quả chung cho tài khoản không tồn tại, không đủ điều kiện hoặc SMTP lỗi.
  SMTP lỗi làm token vừa tạo bị vô hiệu hóa; log không chứa token/email.
- Đăng ký lỗi SMTP rollback cả user/token và render lại lỗi trên form; có thể đăng ký lại.
- Reset dành cho tài khoản đang hoạt động, đã xác thực, gồm cả GOOGLE chưa có mật khẩu.
- Profile và Password settings hiển thị Set password khi `hasPassword = false`.
  POST `/account/set-password` gửi link `/reset-password` đến email của tài khoản đang đăng nhập,
  dùng chung token RESET_PASSWORD 15 phút, một lần và cooldown 60 giây với Forgot password.
  SMTP lỗi rollback token và hiện lỗi; không báo đã gửi thành công. Tài khoản đã có mật khẩu bị từ chối tạo lại qua route này.
- Khả năng đăng nhập bằng mật khẩu dựa trên `User.hasPassword()`, không dựa trên provider gốc.
  Tạo mật khẩu không đổi provider hay identity Google; cùng user dùng được cả hai cách đăng nhập.
- Đổi mật khẩu kiểm tra mật khẩu hiện tại và không cho dùng lại mật khẩu cũ.
  Đổi/reset tăng `users.auth_version`, hủy token reset, thu hồi phiên local/Google cũ ở request tiếp theo.
  Phiên thực hiện được xóa ngay, cookie `JSESSIONID`/`remember-me` bị xóa. Remember-me cũ hết hiệu lực theo hash mật khẩu.

## Hồ sơ, phiên và CSRF

- `AccountController` dùng `AccountServiceImpl`, các form `ProfileUpdateRequest`/`ChangePasswordRequest`
  và DTO `ProfileResponse` an toàn. `PasswordRecoveryController` dùng `PasswordResetForm`
  và `ResetPasswordRequest`; không bind trực tiếp entity.
- Hồ sơ chỉ cập nhật họ tên, điện thoại, địa chỉ, avatar HTTP(S). Không bind Entity.
  Form record dùng constructor binding; email, role, provider, status, ID, memberCode, password không được sửa.
- Họ tên bắt buộc 2–120 ký tự, điện thoại tối đa 20, địa chỉ tối đa 255, URL avatar HTTP(S) tối đa 500.
  Trường tùy chọn để trống sẽ xóa giá trị cũ. Họ tên và trường tùy chọn được trim khi lưu.
- Người chưa đăng nhập vào trang bảo vệ được redirect Login. Phiên bị thu hồi redirect `/login?error=true`.
- CSRF bật cho mọi POST. Lấy `_csrf` từ form HTML render và gửi cùng cookie phiên;
  `th:action` tự thêm trường ẩn. Thiếu/sai CSRF trả 403. Không còn endpoint JSON lấy CSRF.
- Remember-me tối đa 7 ngày. Response dùng header không cache của Spring Security;
  route token thêm `Referrer-Policy: no-referrer`.

## Google và liên kết tài khoản

- Scope `openid,profile,email`, hỗ trợ cả OAuth2/OIDC. `sub` xác định identity, không tự gộp theo email.
- Google mới tạo user GOOGLE đã xác thực; Google trùng email LOCAL chưa liên kết phải đăng nhập local rồi liên kết chủ động.
- `POST /account/link/google` cần phiên local đăng nhập bằng mật khẩu và CSRF, không chấp nhận phiên chỉ remember-me.
  Ý định liên kết gắn user ID, phiên và thời hạn 10 phút.
- Có thể liên kết Google khác email local; giữ nguyên mật khẩu, role và dữ liệu local.
  Một identity chỉ thuộc một user; mỗi user tối đa một identity Google.
- Sai state, hết hạn, sai chủ sở hữu, phiên cũ hoặc tài khoản ngừng hoạt động đều bị từ chối.
  Hủy/lỗi OAuth có thể yêu cầu đăng nhập lại. Chưa có chức năng hủy liên kết.

## Cấu hình và kiểm thử

- Contact dùng `ContactController`, DTO record `ContactForm` và `ContactMailService`.
  GET/POST `/contact` công khai nhưng POST bắt buộc CSRF; không bind Entity.
  Validation server: tên 2–120, email tối đa 120, topic thuộc danh sách form, nội dung bắt buộc/tối đa 5000 ký tự.
  Lỗi validation/SMTP render lại form cùng dữ liệu đã escape, không đặt flash thành công.
  Gửi đồng bộ qua `JavaMailSender`, thành công redirect `/contact` với flash message.
- `CONTACT_TO` là hộp thư thủ thư bắt buộc để gửi Contact; bỏ trống sẽ hiện lỗi gửi.
  From lấy từ `MAIL_FROM`, Reply-To là email người liên hệ. Contact không gửi thư xác nhận cho người điền form và chưa lưu DB.
  Thông báo thành công nghĩa là SMTP chấp nhận yêu cầu, không đảm bảo thư đã vào Inbox.

- Ngày 16/11/2026: `mvnw.cmd test` đạt **122/122 test**, không lỗi/skip;
  gồm 23 AccountFlow, 6 OAuthCallback, 24 ThymeleafAccountFlow, 1 ContactMailService,
  1 application context, 16 Authorization, 4 BorrowFlow, 6 MemberManagement UI và 41 test catalog.
- Dùng JDK 21 cho Maven. Chạy `mvn test` hoặc `./mvnw.cmd test`.
- Test dùng profile `test`, H2 riêng với `create-drop`, mock SMTP/Google; không xóa dữ liệu DB thật.
- Local dùng MySQL, mặc định `ddl-auto=update`. Môi trường `validate`/`none` cần migration
  `account_tokens`, `users.auth_version`, unique `(user_id, provider)` và `(provider, provider_user_id)` của OAuth.
- SMTP đọc `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_AUTH`, `MAIL_STARTTLS`.
  Mặc định `localhost:1025`; ứng dụng không tự chạy SMTP server. Không commit credentials.
- `APP_BASE_URL` quyết định link email, không dùng Host header; dùng HTTPS khi triển khai.
  Google dùng `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, redirect `{APP_BASE_URL}/login/oauth2/code/google`.
- `BOOKNET_REMEMBER_ME_KEY` cần ổn định giữa restart/instances; bỏ trống sẽ tạo khóa ngẫu nhiên mỗi lần chạy.
- Test bao gồm HTML render, tên field, CSRF lấy từ form thật, lỗi model/flash, redirect,
  đăng ký/xác thực, SMTP rollback, token hết hạn/dùng lại/concurrency, phiên, hồ sơ và callback Google.
  Test giả lập **không chứng minh** SMTP/Google thật hay concurrency/migration trên MySQL thực tế.
