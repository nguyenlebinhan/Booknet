# Auth pages: backend handoff

The Sign in, Member registration, Forgot password and Reset password pages were rebuilt from the Figma design. No existing Java file, configuration, dependency or database table changed. The only Java added is a placeholder route for the Forgot password page, described below. This note lists what the backend needs to add to connect the pages.

## Works today

Sign in, registration and Google sign-in run on the existing endpoints.

| Page | Template | Existing endpoint |
|---|---|---|
| Sign in | `login.html` | `POST /login` with `email`, `password`, `remember-me` |
| Member registration | `register.html` | `POST /register` with the `user` object (`UserRegisterDto`) |
| Google sign-in | `authFragments.html` | `GET /oauth2/authorization/google` |

`login.html` shows its own English messages based on URL parameters: `error`, `logout`, `registered`, `oauth2_error` and `reset`. It no longer reads `errorMessage`, `successMessage` or `logoutMessage`, so `AuthController` can stop setting them.

## Registration changes needed

| Item | Now | Needed |
|---|---|---|
| Phone number | The form sends `phone`, but nothing saves it | Add `phone` to `UserRegisterDto`, validate it and save it to `User.phone`. Then bind the field with `th:field` and show its errors in `register.html` |
| Password rule | The page asks for 8 to 32 characters with uppercase, lowercase and a number, checked only in the browser. The server accepts 6 to 32 of any character | Add the same rule to `UserRegisterDto` |
| Error language | Server validation messages are in Vietnamese on an English page | Translate them, or decide how the site handles language |

## Password reset routes needed

`GET /forgot-password` already opens the page through two placeholder files. `POST /forgot-password` sends no email: it only redirects to `/forgot-password?unavailable=true`, which shows a "not available yet" notice. `/reset-password` has no route yet.

To connect the real flow:

1. Delete `ForgotPasswordPageController` and `ForgotPasswordPageSecurityConfig`.
2. Remove the `param.unavailable` notice from `forgot-password.html`.
3. Add `/forgot-password` and `/reset-password` to `permitAll` in `SecurityConfig`.
4. Add the routes below.

| Route | Purpose | Result |
|---|---|---|
| `GET /forgot-password` | Show the request page | Render `forgot-password` |
| `POST /forgot-password` | Form field: `email` | Redirect to `GET /forgot-password` with flash attributes on success. Render `forgot-password` with `emailError` if the format is invalid |
| `GET /reset-password?token=` | Open the emailed link | Render `reset-password` |
| `POST /reset-password` | Form fields: `token`, `password`, `confirmPassword` | Redirect to `/login?reset=true` on success. Render `reset-password` with the error attributes otherwise |

### Model attributes

| Template | Attribute | Type | Effect |
|---|---|---|---|
| `forgot-password` | `requestSubmitted` | boolean | Shows "Request submitted" and the 60-second resend countdown |
| `forgot-password` | `submittedEmail` | String | Refills the email field |
| `forgot-password` | `emailError` | String | Shows the field error |
| `reset-password` | `tokenValid` | boolean | `true` shows the form. `false` or missing shows "This link has expired." |
| `reset-password` | `token` | String | Goes into the form's hidden field |
| `reset-password` | `passwordError` | String | Shows the field error |
| `reset-password` | `confirmPasswordError` | String | Shows the field error |

### What the pages tell users

The text on these pages promises the following, so the backend should do the same:

- The reset link is valid for 15 minutes.
- A link works only once.
- The user can request another email after 60 seconds.
- The response is the same whether or not the email is registered.

### Recommendations

- Store only a hash of the token.
- Build the emailed link from a configured site address, not the request's `Host` header.
- Decide what accounts that only use Google should receive, because they have no password to reset. The page says instructions are on the way if the email is registered.
- `logging.level.org.springframework.security=DEBUG` in `application.properties` can write reset links to the logs, so lower it before deploying.

## Home page

`home.html`, `Navbar.html` and `footer.html` were rebuilt from the design without changing any Java. These items need backend or product input.

| Item | Now | Needed |
|---|---|---|
| Stats | Fixed text: 12.000+ titles, 3.200+ readers, 14 days per loan | Real data, or confirm the wording |
| Genres | Fixed names and title counts | Categories and counts |
| "On the shelves" | Four fixed books with fixed availability such as 3/8 copies | Selected books, covers and live availability |
| Events | Two fixed events on 07 and 14 November | Event data |
| Opening hours and contact | Fixed text in the hero and footer, including "until 20:00 today" | Confirm the details, and base "today" on the real date |
| Search | `name="keyword"`, but the form doesn't submit | Add the search route, then set the form's `action` and remove its `onsubmit` |
| Page links | `href="#"`: Explore books, About the library, Events, My books, Reservations, notifications, genres, book cards, events, Privacy policy, Terms of use | Replace with real routes as pages are added |
| Notification count | The bell shows no number | Provide the unread count |
| Signed-in name | Shows `#authentication.name`, which is the email for email accounts | Provide the display name and initials if the design's "Minh Anh" / "MA" is wanted |
| Google link card | Same logic and endpoints, restyled; the text is still Vietnamese | Translate it, or decide how the site handles language |

## Other gaps in the design

- **Librarian tab:** the Member and Librarian tabs on Sign in only change appearance, and sign-in works the same on both.
- **Registration note:** it tells users to verify their email, but accounts are currently marked as verified automatically.
- **Fixed text:** "12.000+ titles" and the limit of 5 books come from the design, not from data.
