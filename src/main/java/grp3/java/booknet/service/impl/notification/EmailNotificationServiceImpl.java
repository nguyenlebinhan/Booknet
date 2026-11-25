package grp3.java.booknet.service.impl.notification;

import grp3.java.booknet.dto.notification.EmailComposerView;
import grp3.java.booknet.dto.notification.EmailSendForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.notification.EmailLog;
import grp3.java.booknet.entity.notification.EmailTemplate;
import grp3.java.booknet.entity.notification.NotificationPreference;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.notification.EmailLogStatus;
import grp3.java.booknet.enums.notification.EmailTemplateType;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.notification.EmailLogRepository;
import grp3.java.booknet.repository.notification.EmailTemplateRepository;
import grp3.java.booknet.repository.notification.NotificationPreferenceRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import grp3.java.booknet.service.mail.BooknetMailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final int DUE_SOON_DAYS = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private static final Set<String> ALLOWED_VARIABLES = Set.of(
            "memberName", "ten_thanh_vien",
            "memberCode", "ma_the",
            "bookTitle", "ten_sach",
            "dueDate", "han_tra",
            "daysOverdue", "so_ngay_qua_han",
            "pickupUntil", "han_nhan_sach",
            "reservationCode", "ma_dat_truoc",
            "copyBarcode", "ma_ban_sao",
            "libraryName"
    );

    private static final Set<LoanStatus> ACTIVE_LOAN_STATUSES = EnumSet.of(
            LoanStatus.BORROWING,
            LoanStatus.RENEW_PENDING,
            LoanStatus.RETURN_PENDING,
            LoanStatus.OVERDUE
    );

    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository logRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final BooknetMailService mailSender;

    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String mailFrom;

    @Override
    @Transactional
    public void ensureDefaultTemplates() {
        for (EmailTemplateType type : EmailTemplateType.values()) {
            if (templateRepository.findByType(type).isEmpty()) {
                templateRepository.save(buildDefaultTemplate(type));
            }
        }
    }

    @Override
    @Transactional
    public EmailComposerView getComposerView(EmailSendForm form, boolean searchOnly) {
        ensureDefaultTemplates();

        EmailTemplateType activeType = form.getTemplateType() != null
                ? form.getTemplateType()
                : EmailTemplateType.OVERDUE;
        form.setTemplateType(activeType);

        String mode = normalizeMode(form.getRecipientMode());
        form.setRecipientMode(mode);

        if (form.getIdempotencyToken() == null || form.getIdempotencyToken().isBlank()) {
            form.setIdempotencyToken(UUID.randomUUID().toString());
        }

        List<EmailTemplate> allTemplates = templateRepository.findAllByOrderByIdAsc();
        EmailTemplate currentTemplate = allTemplates.stream()
                .filter(t -> t.getType() == activeType)
                .findFirst()
                .orElseGet(() -> buildDefaultTemplate(activeType));

        if (!searchOnly) {
            if (form.getSubjectTemplate() == null || form.getSubjectTemplate().isBlank()) {
                form.setSubjectTemplate(currentTemplate.getSubjectTemplate());
            }
            if (form.getBodyTemplate() == null || form.getBodyTemplate().isBlank()) {
                form.setBodyTemplate(currentTemplate.getBodyTemplate());
            }
        } else {
            if (form.getSubjectTemplate() == null) {
                form.setSubjectTemplate(currentTemplate.getSubjectTemplate());
            }
            if (form.getBodyTemplate() == null) {
                form.setBodyTemplate(currentTemplate.getBodyTemplate());
            }
        }

        List<EmailComposerView.RecipientOptionDto> eligibleGroup = resolveEligibleGroupRecipients(activeType);
        List<EmailComposerView.RecipientOptionDto> memberChoices = resolveMemberChoices(
                activeType, eligibleGroup, form.getSearchKeyword());

        EmailComposerView.RecipientOptionDto selectedRecipient = selectCurrentRecipient(form, memberChoices, eligibleGroup);
        if ("SPECIFIC".equals(mode) && selectedRecipient != null) {
            form.setSelectedMemberId(selectedRecipient.userId());
            if (selectedRecipient.referenceId() != null) {
                form.setSelectedReferenceId(selectedRecipient.referenceId());
            }
        }

        EmailComposerView.EmailPreviewResult preview = buildPreview(
                form.getSubjectTemplate(),
                form.getBodyTemplate(),
                mode,
                selectedRecipient,
                eligibleGroup
        );

        List<EmailComposerView.EmailTemplateDto> templateDtos = allTemplates.stream()
                .map(t -> new EmailComposerView.EmailTemplateDto(
                        t.getType(),
                        t.getName(),
                        t.getDescription(),
                        t.getSubjectTemplate(),
                        t.getBodyTemplate()
                ))
                .toList();

        List<EmailComposerView.EmailLogRowDto> recentLogs = logRepository.findTop50ByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(this::toLogDto)
                .toList();

        return new EmailComposerView(
                templateDtos,
                activeType,
                mode,
                memberChoices,
                eligibleGroup,
                selectedRecipient,
                preview,
                recentLogs,
                LocalDate.now().format(DATE_FMT)
        );
    }

    @Override
    @Transactional
    public EmailTemplate getTemplate(EmailTemplateType type) {
        ensureDefaultTemplates();
        return templateRepository.findByType(type)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Email template not found."));
    }

    @Override
    @Transactional
    public void saveTemplate(EmailTemplateType type, String subjectTemplate, String bodyTemplate) {
        ensureDefaultTemplates();
        if (subjectTemplate == null || subjectTemplate.isBlank() || bodyTemplate == null || bodyTemplate.isBlank()) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Template subject and message body cannot be blank.");
        }
        List<String> unknown = findUnknownVariables(subjectTemplate, bodyTemplate);
        if (!unknown.isEmpty()) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "Unknown template variables: " + String.join(", ", unknown));
        }

        EmailTemplate template = templateRepository.findByType(type)
                .orElseGet(() -> buildDefaultTemplate(type));
        template.setSubjectTemplate(subjectTemplate.trim());
        template.setBodyTemplate(bodyTemplate.trim());
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public EmailTemplate resetTemplate(EmailTemplateType type) {
        ensureDefaultTemplates();
        EmailTemplate defaults = buildDefaultTemplate(type);
        EmailTemplate existing = templateRepository.findByType(type)
                .orElseGet(() -> buildDefaultTemplate(type));
        existing.setSubjectTemplate(defaults.getSubjectTemplate());
        existing.setBodyTemplate(defaults.getBodyTemplate());
        existing.setName(defaults.getName());
        existing.setDescription(defaults.getDescription());
        return templateRepository.save(existing);
    }

    @Override
    @Transactional
    public SendBatchResult sendEmails(EmailSendForm form, String staffEmail) {
        ensureDefaultTemplates();

        EmailTemplateType type = form.getTemplateType() != null ? form.getTemplateType() : EmailTemplateType.OVERDUE;
        String mode = normalizeMode(form.getRecipientMode());
        EmailTemplate template = templateRepository.findByType(type)
                .orElseGet(() -> buildDefaultTemplate(type));

        String subjectTpl = (form.getSubjectTemplate() != null && !form.getSubjectTemplate().isBlank())
                ? form.getSubjectTemplate().trim()
                : template.getSubjectTemplate();
        String bodyTpl = (form.getBodyTemplate() != null && !form.getBodyTemplate().isBlank())
                ? form.getBodyTemplate().trim()
                : template.getBodyTemplate();

        List<EmailComposerView.RecipientOptionDto> eligibleGroup = resolveEligibleGroupRecipients(type);
        List<EmailComposerView.RecipientOptionDto> memberChoices = resolveMemberChoices(type, eligibleGroup, null);
        EmailComposerView.RecipientOptionDto selectedRecipient = selectCurrentRecipient(form, memberChoices, eligibleGroup);

        EmailComposerView.EmailPreviewResult preview = buildPreview(
                subjectTpl, bodyTpl, mode, selectedRecipient, eligibleGroup);
        if (!preview.valid()) {
            throw new AccountException(HttpStatus.BAD_REQUEST, String.join(" ", preview.validationErrors()));
        }

        User staff = (staffEmail != null && !staffEmail.isBlank())
                ? userRepository.findByEmail(staffEmail).orElse(null)
                : null;
        String staffName = staff != null ? staff.getFullName() : (staffEmail != null ? staffEmail : "Library Staff");
        Long staffId = staff != null ? staff.getId() : null;

        String batchToken = (form.getIdempotencyToken() != null && !form.getIdempotencyToken().isBlank())
                ? form.getIdempotencyToken().trim()
                : UUID.randomUUID().toString();

        int sentCount = 0;
        int failedCount = 0;
        int skippedDuplicate = preview.skippedDuplicateCount();
        List<String> errors = new ArrayList<>();

        for (EmailComposerView.RecipientOptionDto recipient : preview.resolvedRecipients()) {
            String perRecipientKey = batchToken + ":" + recipient.userId() + ":"
                    + (recipient.referenceId() != null ? recipient.referenceId() : "0");
            if (logRepository.existsByIdempotencyKeyAndStatus(perRecipientKey, EmailLogStatus.SENT)) {
                skippedDuplicate++;
                continue;
            }

            String renderedSubject = substituteVariables(subjectTpl, recipient.variableValues());
            String renderedBody = substituteVariables(bodyTpl, recipient.variableValues());
            LocalDateTime attemptTime = LocalDateTime.now();

            EmailLog logEntry = EmailLog.builder()
                    .templateType(type)
                    .templateName(template.getName())
                    .recipientUserId(recipient.userId())
                    .recipientName(recipient.fullName())
                    .recipientEmail(recipient.email())
                    .sentByUserId(staffId)
                    .sentByName(staffName)
                    .subject(renderedSubject)
                    .body(renderedBody)
                    .status(EmailLogStatus.FAILED)
                    .referenceType(recipient.referenceType())
                    .referenceId(recipient.referenceId())
                    .idempotencyKey(perRecipientKey)
                    .retryCount(0)
                    .lastAttemptAt(attemptTime)
                    .build();

            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom);
                msg.setTo(recipient.email());
                msg.setSubject(renderedSubject);
                msg.setText(renderedBody);
                mailSender.send(msg, "LIBRARY NOTIFICATION");

                logEntry.setStatus(EmailLogStatus.SENT);
                logEntry.setSentAt(attemptTime);
                logEntry.setErrorMessage(null);
                sentCount++;
            } catch (Exception ex) {
                String msg = sanitizeErrorMessage(ex);
                logEntry.setStatus(EmailLogStatus.FAILED);
                logEntry.setErrorMessage(msg);
                failedCount++;
                errors.add(recipient.email() + ": " + msg);
                log.warn("Failed sending {} email to {}: {}", type, recipient.email(), msg);
            }
            logRepository.save(logEntry);
        }

        return new SendBatchResult(sentCount, failedCount, skippedDuplicate, errors);
    }

    @Override
    @Transactional
    public EmailLog retryFailedLog(Long logId, String staffEmail) {
        EmailLog logEntry = logRepository.findLockedById(logId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Email log record not found."));

        if (logEntry.getStatus() == EmailLogStatus.SENT) {
            throw new AccountException(HttpStatus.CONFLICT, "This email has already been sent.");
        }
        if (logEntry.getIdempotencyKey() != null
                && logRepository.existsByIdempotencyKeyAndStatus(logEntry.getIdempotencyKey(), EmailLogStatus.SENT)) {
            throw new AccountException(HttpStatus.CONFLICT,
                    "A successful delivery for this notification is already recorded.");
        }

        User staff = (staffEmail != null && !staffEmail.isBlank())
                ? userRepository.findByEmail(staffEmail).orElse(null)
                : null;
        if (staff != null) {
            logEntry.setSentByUserId(staff.getId());
            logEntry.setSentByName(staff.getFullName());
        }

        LocalDateTime now = LocalDateTime.now();
        logEntry.setRetryCount(logEntry.getRetryCount() + 1);
        logEntry.setLastAttemptAt(now);

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(logEntry.getRecipientEmail());
            msg.setSubject(logEntry.getSubject());
            msg.setText(logEntry.getBody());
            mailSender.send(msg, "LIBRARY NOTIFICATION");

            logEntry.setStatus(EmailLogStatus.SENT);
            logEntry.setSentAt(now);
            logEntry.setErrorMessage(null);
        } catch (Exception ex) {
            String msg = sanitizeErrorMessage(ex);
            logEntry.setStatus(EmailLogStatus.FAILED);
            logEntry.setErrorMessage(msg);
            log.warn("Retry #{} failed for email log {}: {}", logEntry.getRetryCount(), logId, msg);
        }

        return logRepository.saveAndFlush(logEntry);
    }

    private List<EmailComposerView.RecipientOptionDto> resolveEligibleGroupRecipients(EmailTemplateType type) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<EmailComposerView.RecipientOptionDto> results = new ArrayList<>();

        switch (type) {
            case DUE_SOON -> {
                List<Loan> loans = loanRepository.findByStatusInAndDueDateBetweenOrderByDueDateAsc(
                        ACTIVE_LOAN_STATUSES, today, today.plusDays(DUE_SOON_DAYS));
                for (Loan loan : loans) {
                    results.add(fromLoan(loan, false, today));
                }
            }
            case OVERDUE -> {
                List<Loan> loans = loanRepository.findByStatusInAndDueDateBeforeOrderByDueDateAsc(
                        ACTIVE_LOAN_STATUSES, today);
                for (Loan loan : loans) {
                    results.add(fromLoan(loan, true, today));
                }
            }
            case PICKUP_READY -> {
                List<Reservation> reservations = reservationRepository.findByStatusOrderByReadyAtAscIdAsc(ReservationStatus.READY);
                for (Reservation r : reservations) {
                    if (r.getPickupUntil() == null || r.getPickupUntil().isAfter(now)) {
                        results.add(fromReservation(r));
                    }
                }
            }
        }
        return results;
    }

    private List<EmailComposerView.RecipientOptionDto> resolveMemberChoices(
            EmailTemplateType type,
            List<EmailComposerView.RecipientOptionDto> eligibleGroup,
            String searchKeyword
    ) {
        Map<Long, EmailComposerView.RecipientOptionDto> byUser = new LinkedHashMap<>();
        for (EmailComposerView.RecipientOptionDto dto : eligibleGroup) {
            byUser.putIfAbsent(dto.userId(), dto);
        }

        if (type != EmailTemplateType.PICKUP_READY) {
            LocalDate today = LocalDate.now();
            List<Loan> allActiveLoans = loanRepository.findByStatusInOrderByDueDateAsc(ACTIVE_LOAN_STATUSES);
            for (Loan loan : allActiveLoans) {
                byUser.putIfAbsent(loan.getMember().getId(),
                        fromLoan(loan, loan.getDueDate().isBefore(today), today));
            }
        }

        String kw = searchKeyword == null ? "" : searchKeyword.trim();
        List<User> directoryUsers = userRepository
                .searchDirectory(false, kw, PageRequest.of(0, 25))
                .getContent();

        for (User u : directoryUsers) {
            if (!u.isEnabled() || u.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            if (!byUser.containsKey(u.getId())) {
                byUser.put(u.getId(), fromMemberFallback(u, type));
            }
        }

        if (kw.isBlank()) {
            return new ArrayList<>(byUser.values());
        }

        String lowerKw = kw.toLowerCase(Locale.ROOT);
        return byUser.values().stream()
                .filter(opt -> containsIgnoreCase(opt.fullName(), lowerKw)
                        || containsIgnoreCase(opt.email(), lowerKw)
                        || containsIgnoreCase(opt.memberCode(), lowerKw)
                        || containsIgnoreCase(opt.bookTitle(), lowerKw))
                .toList();
    }

    private EmailComposerView.RecipientOptionDto selectCurrentRecipient(
            EmailSendForm form,
            List<EmailComposerView.RecipientOptionDto> memberChoices,
            List<EmailComposerView.RecipientOptionDto> eligibleGroup
    ) {
        if (form.getSelectedMemberId() != null) {
            for (EmailComposerView.RecipientOptionDto opt : memberChoices) {
                if (opt.userId().equals(form.getSelectedMemberId())) {
                    return opt;
                }
            }
            Optional<User> memberOpt = userRepository.findById(form.getSelectedMemberId());
            if (memberOpt.isPresent() && memberOpt.get().getRole() == UserRole.ROLE_USER) {
                return findBestOptionForMember(memberOpt.get(), form.getTemplateType(), eligibleGroup);
            }
        }
        if (!eligibleGroup.isEmpty()) {
            return eligibleGroup.get(0);
        }
        return memberChoices.isEmpty() ? null : memberChoices.get(0);
    }

    private EmailComposerView.RecipientOptionDto findBestOptionForMember(
            User member,
            EmailTemplateType type,
            List<EmailComposerView.RecipientOptionDto> eligibleGroup
    ) {
        for (EmailComposerView.RecipientOptionDto opt : eligibleGroup) {
            if (opt.userId().equals(member.getId())) {
                return opt;
            }
        }
        return fromMemberFallback(member, type);
    }

    private EmailComposerView.EmailPreviewResult buildPreview(
            String subjectTemplate,
            String bodyTemplate,
            String mode,
            EmailComposerView.RecipientOptionDto selectedRecipient,
            List<EmailComposerView.RecipientOptionDto> eligibleGroup
    ) {
        List<String> errors = new ArrayList<>();
        String subject = subjectTemplate != null ? subjectTemplate.trim() : "";
        String body = bodyTemplate != null ? bodyTemplate.trim() : "";

        if (subject.isBlank()) {
            errors.add("Email subject is required.");
        }
        if (body.isBlank()) {
            errors.add("Email message body is required.");
        }

        List<String> unknownVars = findUnknownVariables(subject, body);
        if (!unknownVars.isEmpty()) {
            errors.add("Unknown variable(s): " + String.join(", ", unknownVars) + ".");
        }

        List<EmailComposerView.RecipientOptionDto> candidates = "GROUP".equals(mode)
                ? eligibleGroup
                : (selectedRecipient != null ? List.of(selectedRecipient) : List.of());

        List<EmailComposerView.RecipientOptionDto> validRecipients = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        int skippedDuplicate = 0;
        int missingEmail = 0;
        int optedOut = 0;

        for (EmailComposerView.RecipientOptionDto opt : candidates) {
            if (opt.email() == null || opt.email().isBlank()) {
                missingEmail++;
                continue;
            }
            if (!opt.emailOptedIn()) {
                optedOut++;
                continue;
            }
            String normalizedEmail = opt.email().trim().toLowerCase(Locale.ROOT);
            if (!seenEmails.add(normalizedEmail)) {
                skippedDuplicate++;
                continue;
            }
            validRecipients.add(opt);
        }

        if (validRecipients.isEmpty()) {
            if ("SPECIFIC".equals(mode)) {
                if (selectedRecipient == null) {
                    errors.add("Please select a valid member recipient.");
                } else if (!selectedRecipient.emailOptedIn()) {
                    errors.add(selectedRecipient.fullName() + " has disabled email notifications in their preferences.");
                } else {
                    errors.add("Selected member does not have a valid email address.");
                }
            } else {
                errors.add("No eligible recipients with enabled email notifications were found for this template.");
            }
        }

        EmailComposerView.RecipientOptionDto sample = !validRecipients.isEmpty()
                ? validRecipients.get(0)
                : selectedRecipient;

        Map<String, String> sampleVars = sample != null ? sample.variableValues() : defaultSampleVars();
        String renderedSubject = substituteVariables(subject, sampleVars);
        String renderedBody = substituteVariables(body, sampleVars);

        return new EmailComposerView.EmailPreviewResult(
                errors.isEmpty(),
                unknownVars,
                errors,
                validRecipients.size(),
                skippedDuplicate,
                missingEmail,
                optedOut,
                mailFrom,
                sample != null && sample.email() != null ? sample.email() : "reader@booknet.local",
                sample != null ? sample.fullName() : "Reader",
                renderedSubject,
                renderedBody,
                validRecipients
        );
    }

    private EmailComposerView.RecipientOptionDto fromLoan(Loan loan, boolean overdue, LocalDate today) {
        User member = loan.getMember();
        Book book = loan.getCopy().getBook();
        long daysLate = overdue ? Math.max(1, ChronoUnit.DAYS.between(loan.getDueDate(), today)) : 0;
        long daysLeft = !overdue ? Math.max(0, ChronoUnit.DAYS.between(today, loan.getDueDate())) : 0;

        String dueFormatted = loan.getDueDate() != null ? loan.getDueDate().format(DATE_FMT) : today.format(DATE_FMT);
        String daysText = overdue
                ? daysLate + (daysLate == 1 ? " day overdue" : " days overdue")
                : (daysLeft == 0 ? "Due today" : "Due in " + daysLeft + (daysLeft == 1 ? " day" : " days"));

        Map<String, String> vars = buildVariableMap(
                member,
                book.getTitle(),
                dueFormatted,
                String.valueOf(daysLate),
                "",
                "",
                loan.getCopy().getBarcode()
        );

        return new EmailComposerView.RecipientOptionDto(
                member.getId(),
                member.getFullName(),
                initialsOf(member.getFullName()),
                member.getEmail(),
                displayMemberCode(member),
                isEmailOptedIn(member.getId()),
                loan.getId(),
                "LOAN",
                "LN-" + loan.getId(),
                book.getTitle(),
                authorsOf(book),
                coverOf(book),
                dueFormatted,
                daysText,
                "",
                overdue ? "OVERDUE" : "DUE SOON",
                overdue ? "danger" : "warning",
                overdue ? "Active overdue loan" : "Upcoming loan due date",
                vars
        );
    }

    private EmailComposerView.RecipientOptionDto fromReservation(Reservation r) {
        User member = r.getMember();
        Book book = r.getBook();
        String pickupFmt = r.getPickupUntil() != null
                ? r.getPickupUntil().format(DATE_TIME_FMT)
                : LocalDateTime.now().plusDays(2).format(DATE_TIME_FMT);
        String dueOnlyFmt = r.getPickupUntil() != null
                ? r.getPickupUntil().format(DATE_FMT)
                : LocalDate.now().plusDays(2).format(DATE_FMT);
        String barcode = r.getAssignedCopy() != null ? r.getAssignedCopy().getBarcode() : "";

        Map<String, String> vars = buildVariableMap(
                member,
                book.getTitle(),
                dueOnlyFmt,
                "0",
                pickupFmt,
                r.displayCode(),
                barcode
        );

        return new EmailComposerView.RecipientOptionDto(
                member.getId(),
                member.getFullName(),
                initialsOf(member.getFullName()),
                member.getEmail(),
                displayMemberCode(member),
                isEmailOptedIn(member.getId()),
                r.getId(),
                "RESERVATION",
                r.displayCode(),
                book.getTitle(),
                authorsOf(book),
                coverOf(book),
                dueOnlyFmt,
                "Hold expires " + pickupFmt,
                pickupFmt,
                "READY FOR PICKUP",
                "positive",
                "Held copy waiting at desk",
                vars
        );
    }

    private EmailComposerView.RecipientOptionDto fromMemberFallback(User member, EmailTemplateType type) {
        String todayFmt = LocalDate.now().format(DATE_FMT);
        String pickupFmt = LocalDateTime.now().plusDays(2).format(DATE_TIME_FMT);
        String sampleTitle = "Library Circulation Item";

        Map<String, String> vars = buildVariableMap(
                member,
                sampleTitle,
                todayFmt,
                "0",
                pickupFmt,
                "RS-2026-0000",
                "-"
        );

        String note = switch (type) {
            case PICKUP_READY -> "No active held reservation · general template context";
            case DUE_SOON, OVERDUE -> "No active loan · general template context";
        };

        return new EmailComposerView.RecipientOptionDto(
                member.getId(),
                member.getFullName(),
                initialsOf(member.getFullName()),
                member.getEmail(),
                displayMemberCode(member),
                isEmailOptedIn(member.getId()),
                null,
                "MEMBER",
                displayMemberCode(member),
                sampleTitle,
                "booknet Library",
                "/images/home/cover-hoa-vang.jpg",
                todayFmt,
                "General reminder",
                pickupFmt,
                "MEMBER ACCOUNT",
                "neutral",
                note,
                vars
        );
    }

    private boolean isEmailOptedIn(Long userId) {
        return preferenceRepository.findByUser_Id(userId)
                .map(NotificationPreference::isEmailEnabled)
                .orElse(true);
    }

    private Map<String, String> buildVariableMap(
            User member,
            String bookTitle,
            String dueDate,
            String daysOverdue,
            String pickupUntil,
            String reservationCode,
            String copyBarcode
    ) {
        String name = member.getFullName() != null ? member.getFullName() : "Reader";
        String code = displayMemberCode(member);
        String title = bookTitle != null ? bookTitle : "Library book";
        String due = dueDate != null ? dueDate : "";
        String days = daysOverdue != null ? daysOverdue : "0";
        String pickup = pickupUntil != null ? pickupUntil : due;
        String resCode = reservationCode != null ? reservationCode : "";
        String barcode = copyBarcode != null ? copyBarcode : "";

        Map<String, String> map = new LinkedHashMap<>();
        map.put("memberName", name);
        map.put("ten_thanh_vien", name);
        map.put("memberCode", code);
        map.put("ma_the", code);
        map.put("bookTitle", title);
        map.put("ten_sach", title);
        map.put("dueDate", due);
        map.put("han_tra", due);
        map.put("daysOverdue", days);
        map.put("so_ngay_qua_han", days);
        map.put("pickupUntil", pickup);
        map.put("han_nhan_sach", pickup);
        map.put("reservationCode", resCode);
        map.put("ma_dat_truoc", resCode);
        map.put("copyBarcode", barcode);
        map.put("ma_ban_sao", barcode);
        map.put("libraryName", "booknet Library");
        return map;
    }

    private Map<String, String> defaultSampleVars() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("memberName", "Reader");
        map.put("ten_thanh_vien", "Reader");
        map.put("memberCode", "BN-2026-0001");
        map.put("ma_the", "BN-2026-0001");
        map.put("bookTitle", "Clean Code");
        map.put("ten_sach", "Clean Code");
        map.put("dueDate", LocalDate.now().format(DATE_FMT));
        map.put("han_tra", LocalDate.now().format(DATE_FMT));
        map.put("daysOverdue", "3");
        map.put("so_ngay_qua_han", "3");
        map.put("pickupUntil", LocalDateTime.now().plusDays(2).format(DATE_TIME_FMT));
        map.put("han_nhan_sach", LocalDateTime.now().plusDays(2).format(DATE_TIME_FMT));
        map.put("reservationCode", "RS-2026-0001");
        map.put("ma_dat_truoc", "RS-2026-0001");
        map.put("copyBarcode", "BC-0001");
        map.put("ma_ban_sao", "BC-0001");
        map.put("libraryName", "booknet Library");
        return map;
    }

    private List<String> findUnknownVariables(String subject, String body) {
        Set<String> unknown = new LinkedHashSet<>();
        collectUnknown(subject, unknown);
        collectUnknown(body, unknown);
        return new ArrayList<>(unknown);
    }

    private void collectUnknown(String input, Set<String> unknown) {
        if (input == null || input.isBlank()) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!ALLOWED_VARIABLES.contains(key)) {
                unknown.add("{{" + key + "}}");
            }
        }
    }

    private String substituteVariables(String template, Map<String, String> vars) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = vars.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private EmailTemplate buildDefaultTemplate(EmailTemplateType type) {
        return switch (type) {
            case DUE_SOON -> EmailTemplate.builder()
                    .type(EmailTemplateType.DUE_SOON)
                    .name("Due soon")
                    .description("Reminder sent 3 days before a borrowed book's due date")
                    .subjectTemplate("[booknet] Reminder: {{bookTitle}} is due on {{dueDate}}")
                    .bodyTemplate("""
                            Dear {{memberName}},

                            Your borrowed copy of "{{bookTitle}}" is due back on {{dueDate}}.
                            Please return the book to the Circulation Desk on time or request a renewal from My books if no other reader is waiting.

                            Member card: {{memberCode}}
                            Warm regards,
                            booknet Library""")
                    .active(true)
                    .build();
            case OVERDUE -> EmailTemplate.builder()
                    .type(EmailTemplateType.OVERDUE)
                    .name("Overdue books")
                    .description("Notice for loans past their due date with daily late fee guidance")
                    .subjectTemplate("[booknet] Overdue notice: {{bookTitle}} is {{daysOverdue}} days overdue")
                    .bodyTemplate("""
                            Dear {{memberName}},

                            Your borrowed book "{{bookTitle}}" was due on {{dueDate}} and is currently {{daysOverdue}} days overdue.
                            Please return it to the Circulation Desk at your earliest convenience so another waiting reader can borrow it and to prevent further late fees.

                            Member card: {{memberCode}}
                            Warm regards,
                            booknet Library""")
                    .active(true)
                    .build();
            case PICKUP_READY -> EmailTemplate.builder()
                    .type(EmailTemplateType.PICKUP_READY)
                    .name("Reservation ready")
                    .description("Hold alert confirming shelf pickup window before expiry")
                    .subjectTemplate("[booknet] Your reserved book \"{{bookTitle}}\" is ready for pickup")
                    .bodyTemplate("""
                            Dear {{memberName}},

                            Good news! A copy of "{{bookTitle}}" ({{copyBarcode}}) is now held for reservation {{reservationCode}}.
                            Please bring your library card ({{memberCode}}) to the Circulation Desk before {{pickupUntil}}. Holds expire automatically at the end of the pickup window.

                            Warm regards,
                            booknet Library""")
                    .active(true)
                    .build();
        };
    }

    private EmailComposerView.EmailLogRowDto toLogDto(EmailLog log) {
        return new EmailComposerView.EmailLogRowDto(
                log.getId(),
                log.getTemplateType(),
                log.getTemplateName(),
                log.getRecipientName(),
                log.getRecipientEmail(),
                log.getSubject(),
                log.getBody(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getRetryCount(),
                log.getLastAttemptAt() != null ? log.getLastAttemptAt() : log.getCreatedAt(),
                log.getSentByName()
        );
    }

    private String normalizeMode(String recipientMode) {
        if (recipientMode != null && "GROUP".equalsIgnoreCase(recipientMode.trim())) {
            return "GROUP";
        }
        return "SPECIFIC";
    }

    private String displayMemberCode(User user) {
        if (user.getMemberCard() != null && user.getMemberCard().getCardNumber() != null) {
            return user.getMemberCard().getCardNumber();
        }
        return user.getMemberCode() != null ? user.getMemberCode() : "MEMBER-" + user.getId();
    }

    private String authorsOf(Book book) {
        String authors = book.getAuthors().stream()
                .map(Author::getName)
                .sorted()
                .collect(Collectors.joining(", "));
        return authors.isBlank() ? "Unknown author" : authors;
    }

    private String coverOf(Book book) {
        return (book.getCoverUrl() != null && !book.getCoverUrl().isBlank())
                ? book.getCoverUrl()
                : "/images/home/cover-hoa-vang.jpg";
    }

    private String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "U";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String lowerKw) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKw);
    }

    private String sanitizeErrorMessage(Exception ex) {
        String raw = ex.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = ex.getClass().getSimpleName();
        }
        return raw.length() > 480 ? raw.substring(0, 480) : raw;
    }
}
