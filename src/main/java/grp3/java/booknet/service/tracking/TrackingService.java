package grp3.java.booknet.service.tracking;

import grp3.java.booknet.entity.tracking.PageVisitLog;
import grp3.java.booknet.entity.tracking.UserLoginLog;
import grp3.java.booknet.repository.tracking.PageVisitLogRepository;
import grp3.java.booknet.repository.tracking.UserLoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for all Admin Tracking Dashboard data operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserLoginLogRepository loginLogRepo;
    private final PageVisitLogRepository pageVisitRepo;

    // -- Write operations (independent transaction so they never break caller) --

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLoginLog(UserLoginLog logEntry) {
        try {
            loginLogRepo.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save login log for {}: {}", logEntry.getUserEmail(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePageVisit(PageVisitLog visit) {
        try {
            pageVisitRepo.save(visit);
        } catch (Exception e) {
            log.warn("Failed to save page visit for {}: {}", visit.getUserEmail(), e.getMessage());
        }
    }

    // -- KPI Statistics --

    @Transactional(readOnly = true)
    public long totalLogins() {
        return loginLogRepo.count();
    }

    @Transactional(readOnly = true)
    public long loginsToday() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        return loginLogRepo.countByLoginAtAfter(startOfDay);
    }

    @Transactional(readOnly = true)
    public long uniqueActiveUsers() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return loginLogRepo.countDistinctEmailSince(since);
    }

    @Transactional(readOnly = true)
    public long totalPageVisits() {
        return pageVisitRepo.count();
    }

    @Transactional(readOnly = true)
    public List<Object[]> topVisitedPages(int limit) {
        return pageVisitRepo.topVisitedPages(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> loginLocationDistribution(int limit) {
        return loginLogRepo.groupByLocation(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> visitLocationDistribution(int limit) {
        return pageVisitRepo.groupByLocation(PageRequest.of(0, limit));
    }

    // -- Paginated queries --

    @Transactional(readOnly = true)
    public Page<UserLoginLog> searchLogins(String keyword, String role,
                                            LocalDateTime from, LocalDateTime to,
                                            Pageable pageable) {
        return loginLogRepo.search(
                blankToNull(keyword), blankToNull(role), from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PageVisitLog> searchVisits(String keyword, String role,
                                            LocalDateTime from, LocalDateTime to,
                                            Pageable pageable) {
        return pageVisitRepo.search(
                blankToNull(keyword), blankToNull(role), from, to, pageable);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
