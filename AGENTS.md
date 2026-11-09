# Booknet project conventions

- Read `docs/project-context.md` for the project overview and current feature status,
  then verify the relevant source; the context is a dated snapshot, not live state.
- This is a Java 21 / Spring Boot / Spring MVC / Thymeleaf application with JPA and MySQL.
- Before changing a feature, read its existing controllers, services, security rules,
  form DTOs, templates, fragments and relevant tests. Check the current branch and diff.
- User-facing account flows use `@Controller`, form binding, `BindingResult`, `Model`,
  flash messages and redirects. Email links must open the corresponding HTML page.
  JSON APIs may be supplementary; they do not replace working Thymeleaf form flows.
- Reuse existing page designs and CSS. Change form bindings and messages as needed;
  do not redesign or add unrelated pages without a user request.
- Keep validation and token expiry consistent across services, forms and templates.
- Validate real rendered HTML, form submission, CSRF, redirects and model errors in
  addition to service/API tests. Use the isolated H2 test profile, never the real DB
  for destructive test setup. Google and SMTP mocks do not prove live connectivity.
