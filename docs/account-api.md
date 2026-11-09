# Account backend

Các thay đổi này không thêm hoặc sửa giao diện HTML/CSS. API trả JSON và dùng chung phiên
Spring Security với form đăng nhập hiện tại.

## Cấu hình và chạy

- Dùng JDK 21. Nếu Maven đang chọn JDK 17, đặt `JAVA_HOME` về JDK 21 trước khi chạy.
- Cấu hình database như trước. Với môi trường local, `DB_DDL_AUTO=update` bổ sung bảng
  `account_tokens`, cột `users.auth_version` (mặc định 0), và ràng buộc duy nhất
  `(user_id, provider)` trên `oauth_identities`. Không dùng `create` trên database có dữ liệu.
- Nếu triển khai với `ddl-auto=validate`/`none`, cần áp dụng các thay đổi schema này qua
  quy trình migration của môi trường trước khi khởi động. Kiểm tra và xử lý bản ghi
  OAuth trùng `(user_id, provider)` trước khi tạo ràng buộc.
- Cấu hình SMTP trong `.env` theo `.env.example`. Mặc định trỏ tới SMTP local
  `localhost:1025` (ví dụ Mailpit); ứng dụng không tự khởi động SMTP server.
  Đặt `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`,
  `MAIL_AUTH`, `MAIL_STARTTLS` theo nhà cung cấp. Không commit mật khẩu SMTP.
- Đặt `APP_BASE_URL` về URL tin cậy của ứng dụng, dùng HTTPS khi triển khai. Link trong email
  được tạo từ cấu hình này, không dùng Host header của request.
- `BOOKNET_REMEMBER_ME_KEY` là chuỗi ngẫu nhiên dài, dùng ổn định giữa các instance/restart.
  Nếu để trống, ứng dụng tạo khóa ngẫu nhiên cho mỗi lần chạy và cookie remember-me cũ hết hiệu lực.
- Google vẫn dùng `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; cấu hình redirect URI trên Google
  là `{APP_BASE_URL}/login/oauth2/code/google`. Scope `openid,profile,email` đi qua
  `CustomOidcUserService` và cùng logic tài khoản với OAuth2 thường.

Chạy `mvn test` bằng JDK 21 để kiểm thử trên H2 với SMTP giả lập. Các test không kết nối
database thư viện thực và không gửi email ra ngoài. Maven Wrapper có sẵn có thể được dùng
nếu môi trường hỗ trợ script của wrapper.

## Phiên đăng nhập và CSRF

1. Gọi `GET /api/auth/csrf`, giữ cookie `JSESSIONID` trả về.
2. Đọc `headerName` và `token` từ JSON, gửi header đó cùng cookie trên mọi POST/PUT.
   Mặc định header là `X-CSRF-TOKEN`. CSRF vẫn được bật cho cả API.
3. Đăng nhập qua `POST /login` với `application/x-www-form-urlencoded`: `email`, `password`,
   và tùy chọn `remember-me=on`. Giữ cookie phiên sau redirect. Email đăng nhập được trim
   và chuyển chữ thường. Gọi lại `/api/auth/csrf` sau đăng nhập để lấy token của phiên mới.
4. Đăng xuất qua `POST /logout` kèm CSRF.

Các API bảo vệ trả 401 nếu chưa đăng nhập/phiên bị thu hồi; thiếu CSRF trả 403.
Các response dùng header không cache mặc định của Spring Security. Response chứa token qua
query string thêm `Referrer-Policy: no-referrer`.

## API công khai

| Method | Endpoint | Dữ liệu | Kết quả |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | Không | 200: thông tin CSRF |
| POST | `/api/auth/register` | JSON đăng ký bên dưới | 201: đã gửi email xác thực |
| POST | `/api/auth/verification/resend` | `{"email":"reader@example.com"}` | 202: phản hồi chung |
| GET | `/api/auth/verify-email?token=...` | Token trong email | 200: đã xác thực |
| POST | `/api/auth/forgot-password` | `{"email":"reader@example.com"}` | 202: phản hồi chung |
| GET | `/api/auth/reset-password?token=...` | Token trong email | 200: kiểm tra link, chưa đổi mật khẩu |
| POST | `/api/auth/reset-password` | JSON mật khẩu bên dưới | 200: đã đặt lại mật khẩu |

Ví dụ đăng ký:

```json
{
  "fullName": "Nguyen Van A",
  "email": "reader@example.com",
  "password": "Secret123",
  "confirmPassword": "Secret123",
  "termsAccepted": true
}
```

Form `POST /register` hiện có cũng gửi email xác thực theo cùng quy trình. Tài khoản local mới
chưa được đăng nhập cho đến khi xác thực email. Các tài khoản cũ đã lưu `email_verified=true`
được giữ nguyên; thay đổi này không chứng minh lại email lịch sử.

Ví dụ đặt lại mật khẩu:

```json
{
  "token": "token-lay-tu-email",
  "newPassword": "NewSecret123",
  "confirmPassword": "NewSecret123"
}
```

Link đặt lại mật khẩu mở endpoint kiểm tra JSON vì chưa tích hợp API với UI. Để hoàn tất, gọi POST
với token nhận qua email và mật khẩu mới. Link xác thực email hoàn tất trực tiếp qua GET.

Các trang forgot/reset password mới từ PR #18 trên `main` vẫn là phần giao diện riêng.
`POST /forgot-password` hiện trả redirect `?unavailable=true`, chưa gọi API gửi email;
template reset-password chưa được nối vào luồng token. Commit backend này không sửa các trang đó.

- Token ngẫu nhiên 256 bit, chỉ lưu SHA-256 trong database, dùng một lần.
- Xác thực email hết hạn sau 24 giờ, đặt lại mật khẩu sau 30 phút.
- Mỗi loại email giới hạn một lần/60 giây/tài khoản; gửi lại sau thời gian này hủy link cũ.
- Forgot/resend luôn trả cùng nội dung cho tài khoản không tồn tại, không đủ điều kiện hoặc
  SMTP lỗi. Khi SMTP lỗi, token vừa tạo được vô hiệu hóa và ghi cảnh báo không chứa token/email.
- Đăng ký gặp SMTP lỗi trả 503 và rollback tài khoản/token, có thể đăng ký lại sau khi sửa SMTP.
- Reset chỉ áp dụng cho tài khoản LOCAL đang hoạt động, đã xác thực email. Tài khoản Google-only
  tiếp tục đăng nhập Google. Local chưa xác thực dùng chức năng gửi lại email xác thực trước.
- Mật khẩu từ 6–32 ký tự, tối đa 72 byte UTF-8 theo giới hạn BCrypt; xác nhận phải trùng khớp.
- Dữ liệu sai/token hết hạn/đã dùng trả 400; đăng ký trùng email trả 409. API không trả token
  bí mật hoặc chi tiết lỗi database/SMTP cho client.

## API cần đăng nhập (local hoặc Google)

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/account/profile` | Hồ sơ của chính người đăng nhập |
| PUT | `/api/account/profile` | Thay thế các trường hồ sơ được phép |
| POST | `/api/account/change-password` | Đổi mật khẩu local, yêu cầu mật khẩu hiện tại |

Ví dụ cập nhật hồ sơ:

```json
{
  "fullName": "Nguyen Van A",
  "phone": "+84 912345678",
  "address": "Ho Chi Minh City",
  "avatarUrl": "https://example.com/avatar.png"
}
```

`fullName` bắt buộc, 2–120 ký tự sau trim; điện thoại tối đa 20 ký tự, địa chỉ 255 ký tự,
URL avatar HTTP(S) tối đa 500 ký tự. Trường tùy chọn null/rỗng sẽ xóa giá trị cũ (PUT).
Không hỗ trợ thay đổi email, role, provider, status, memberCode hay mật khẩu qua API hồ sơ.
Response chỉ chứa các trường công khai của hồ sơ và `googleLinked`, không trả entity/password/hash token.

Ví dụ đổi mật khẩu:

```json
{
  "currentPassword": "Secret123",
  "newPassword": "NewSecret123",
  "confirmPassword": "NewSecret123"
}
```

Đổi mật khẩu không cho dùng lại mật khẩu hiện tại. Đổi hoặc reset mật khẩu hủy các token reset
còn lại, tăng `auth_version`; các phiên local/Google cũ bị từ chối ở request kế tiếp. Phiên
thực hiện đổi mật khẩu được đăng xuất ngay. Cookie remember-me cũ không còn hợp lệ vì hash mật khẩu đã đổi.
Tài khoản Google-only không được đặt/đổi mật khẩu local qua các endpoint này.

## Liên kết Google

1. Đăng nhập bằng email/mật khẩu vào tài khoản local đã xác thực và đang hoạt động.
2. `POST /account/link/google` kèm CSRF. Phiên chỉ khôi phục qua remember-me không được phép
   bắt đầu liên kết; người dùng cần đăng xuất rồi đăng nhập lại bằng mật khẩu.
3. Theo redirect sang Google. Ý định liên kết lưu trong cùng phiên, ràng buộc user ID và có
   hiệu lực 10 phút. Callback kiểm tra lại người đăng nhập, trạng thái và phiên bản xác thực.
4. Google phải cung cấp email đã xác thực và `sub`. `sub` xác định tài khoản Google, không tự
   gộp tài khoản chỉ vì trùng email. Liên kết thủ công có thể dùng email Google khác email local.
5. Mỗi Google identity chỉ thuộc một user; mỗi user chỉ có một identity Google. Có khóa
   database và unique constraint chống ghi trùng khi request đồng thời. Thử lại cùng identity
   trên cùng user là thao tác idempotent.
6. Sau liên kết, Google đăng nhập vào đúng user local, giữ nguyên mật khẩu, role và dữ liệu.
   Tài khoản bị khóa, hết hạn hoặc ngừng hoạt động không thể đăng nhập qua cả hai cách.

Hủy/lỗi callback xóa trạng thái liên kết và chuyển về Home với `google_link_error=true`.
Spring Security có thể xóa phiên xác thực khi OAuth thất bại; người dùng đăng nhập lại nếu cần.
Chưa có chức năng hủy liên kết. Kiểm thử tự động mô phỏng Google/OIDC; cần thử vòng OAuth thực
trên môi trường có client ID/secret và redirect URI hợp lệ trước khi triển khai.
