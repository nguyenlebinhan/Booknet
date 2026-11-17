package grp3.java.booknet.service.impl.admin;

import grp3.java.booknet.entity.AuditLog;
import grp3.java.booknet.repository.AuditLogRepository;
import grp3.java.booknet.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Ghi log trong transaction riêng (REQUIRES_NEW) để đảm bảo audit luôn được lưu
     * kể cả khi transaction gọi nó bị rollback.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String actorEmail, String actorRole,
                        String action,
                        String targetType, Long targetId,
                        String description) {
        AuditLog entry = AuditLog.builder()
                .actorEmail(actorEmail != null ? actorEmail : "system")
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .build();
        AuditLog saved = auditLogRepository.save(entry);
        log.debug("AuditLog [{}] by {} on {}/{}", action, actorEmail, targetType, targetId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecent10() {
        return auditLogRepository.findTop10ByOrderByOccurredAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByOccurredAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> searchWithKeyword(String keyword, String action, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String act = (action == null || action.isBlank()) ? null : action.trim();
        return auditLogRepository.searchWithKeyword(kw, act, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(String actorEmail, String action,
                                 LocalDateTime from, LocalDateTime to,
                                 Pageable pageable) {
        // normalize blanks thành null để query JPQL đúng
        String emailParam = (actorEmail == null || actorEmail.isBlank()) ? null : actorEmail.trim();
        String actionParam = (action == null || action.isBlank()) ? null : action.trim();
        return auditLogRepository.search(emailParam, actionParam, from, to, pageable);
    }
}
