package grp3.java.booknet.controller.notification;

import grp3.java.booknet.dto.notification.EmailComposerView;
import grp3.java.booknet.dto.notification.EmailSendForm;
import grp3.java.booknet.entity.notification.EmailLog;
import grp3.java.booknet.entity.notification.EmailTemplate;
import grp3.java.booknet.enums.notification.EmailLogStatus;
import grp3.java.booknet.enums.notification.EmailTemplateType;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.service.notification.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/staff/notifications/email", "/librarian/notifications/email", "/admin/notifications/email"})
@PreAuthorize("hasAuthority('SERVICE_EMAIL')")
@RequiredArgsConstructor
public class LibrarianEmailNotificationController {

    private final EmailNotificationService emailNotificationService;

    /** Autosaves wording only. Preview and saving never dispatch an email. */
    @PostMapping("/template")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> autosaveTemplate(
            @Valid @ModelAttribute EmailSendForm form, BindingResult errors) {
        if (errors.hasErrors()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "message", errors.getAllErrors().getFirst().getDefaultMessage()));
        }
        try {
            emailNotificationService.saveTemplate(form.getTemplateType(), form.getSubjectTemplate(), form.getBodyTemplate());
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "Template saved automatically."));
        } catch (AccountException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping
    public String showComposer(
            @RequestParam(required = false) EmailTemplateType templateType,
            @RequestParam(required = false) String recipientMode,
            @RequestParam(required = false) Long selectedMemberId,
            @RequestParam(required = false) String searchKeyword,
            HttpServletRequest request,
            Model model
    ) {
        EmailSendForm form;
        if (model.containsAttribute("sendForm")) {
            form = (EmailSendForm) model.getAttribute("sendForm");
        } else {
            form = EmailSendForm.builder()
                    .templateType(templateType != null ? templateType : EmailTemplateType.OVERDUE)
                    .recipientMode(recipientMode != null ? recipientMode : "SPECIFIC")
                    .selectedMemberId(selectedMemberId)
                    .searchKeyword(searchKeyword)
                    .build();
        }
        EmailComposerView view = emailNotificationService.getComposerView(form, false);
        populateComposerModel(model, form, view, resolveBasePath(request));
        return "notification/send-email";
    }

    @PostMapping(params = "action=filter")
    public String refreshComposerPreview(
            @ModelAttribute("sendForm") EmailSendForm form,
            HttpServletRequest request,
            Model model
    ) {
        form.setReviewStep(false);
        EmailComposerView view = emailNotificationService.getComposerView(form, true);
        populateComposerModel(model, form, view, resolveBasePath(request));
        return "notification/send-email";
    }

    @PostMapping(params = "action=switch-template")
    public String switchTemplate(
            @ModelAttribute("sendForm") EmailSendForm form,
            HttpServletRequest request,
            Model model
    ) {
        form.setReviewStep(false);
        form.setSubjectTemplate(null);
        form.setBodyTemplate(null);
        form.setSelectedMemberId(null);
        EmailComposerView view = emailNotificationService.getComposerView(form, false);
        populateComposerModel(model, form, view, resolveBasePath(request));
        return "notification/send-email";
    }

    @PostMapping(params = "action=review")
    public String reviewBeforeSending(
            @Valid @ModelAttribute("sendForm") EmailSendForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        EmailComposerView view = emailNotificationService.getComposerView(form, true);
        if (!bindingResult.hasErrors() && view.preview().valid()) {
            form.setReviewStep(true);
        } else {
            form.setReviewStep(false);
        }
        populateComposerModel(model, form, view, resolveBasePath(request));
        return "notification/send-email";
    }

    @PostMapping(params = "action=edit")
    public String backToEditing(
            @ModelAttribute("sendForm") EmailSendForm form,
            HttpServletRequest request,
            Model model
    ) {
        form.setReviewStep(false);
        EmailComposerView view = emailNotificationService.getComposerView(form, true);
        populateComposerModel(model, form, view, resolveBasePath(request));
        return "notification/send-email";
    }

    @PostMapping(params = "action=save-template")
    public String saveTemplate(
            @ModelAttribute("sendForm") EmailSendForm form,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        try {
            emailNotificationService.saveTemplate(
                    form.getTemplateType(),
                    form.getSubjectTemplate(),
                    form.getBodyTemplate()
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Template \"" + form.getTemplateType().name() + "\" saved for future dispatches.");
        } catch (AccountException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + basePath + "?templateType=" + form.getTemplateType();
    }

    @PostMapping(params = "action=reset-template")
    public String resetTemplate(
            @ModelAttribute("sendForm") EmailSendForm form,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        EmailTemplate reset = emailNotificationService.resetTemplate(form.getTemplateType());
        redirectAttributes.addFlashAttribute("successMessage",
                "Template \"" + reset.getName() + "\" restored to the library default.");
        return "redirect:" + basePath + "?templateType=" + reset.getType();
    }

    @PostMapping
    public String sendEmails(
            @Valid @ModelAttribute("sendForm") EmailSendForm form,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        if (bindingResult.hasErrors()) {
            form.setReviewStep(false);
            EmailComposerView view = emailNotificationService.getComposerView(form, true);
            populateComposerModel(model, form, view, basePath);
            return "notification/send-email";
        }

        try {
            EmailNotificationService.SendBatchResult result =
                    emailNotificationService.sendEmails(form, authentication.getName());

            if (result.sentCount() > 0 && result.failedCount() == 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Sent " + result.sentCount() + " email notification(s) individually."
                                + (result.skippedDuplicateCount() > 0
                                ? " Skipped " + result.skippedDuplicateCount() + " duplicate(s)."
                                : ""));
            } else if (result.sentCount() > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Sent " + result.sentCount() + " email(s), but " + result.failedCount()
                                + " failed. Use Retry in the log table below.");
            } else if (result.failedCount() > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Delivery failed for " + result.failedCount()
                                + " recipient(s). Check the Recent sending log below and click Retry.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Duplicate submission skipped; this notification was already delivered.");
            }
            return "redirect:" + basePath + "?templateType=" + form.getTemplateType();
        } catch (AccountException ex) {
            form.setReviewStep(false);
            EmailComposerView view = emailNotificationService.getComposerView(form, true);
            model.addAttribute("errorMessage", ex.getMessage());
            populateComposerModel(model, form, view, basePath);
            return "notification/send-email";
        }
    }

    @PostMapping("/logs/{id}/retry")
    public String retryFailedEmail(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        try {
            EmailLog log = emailNotificationService.retryFailedLog(id, authentication.getName());
            if (log.getStatus() == EmailLogStatus.SENT) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Successfully retried and sent notification to " + log.getRecipientEmail() + ".");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Retry failed for " + log.getRecipientEmail() + ": " + log.getErrorMessage());
            }
        } catch (AccountException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + basePath;
    }

    private void populateComposerModel(
            Model model,
            EmailSendForm form,
            EmailComposerView view,
            String basePath
    ) {
        model.addAttribute("sendForm", form);
        model.addAttribute("view", view);
        model.addAttribute("basePath", basePath);
        model.addAttribute("activePage", "email-notifications");
        model.addAttribute("section", "Send email notifications");
    }

    private String resolveBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/admin/notifications/email")) {
            return "/admin/notifications/email";
        }
        if (uri != null && uri.startsWith("/librarian/notifications/email")) {
            return "/librarian/notifications/email";
        }
        return "/staff/notifications/email";
    }
}
