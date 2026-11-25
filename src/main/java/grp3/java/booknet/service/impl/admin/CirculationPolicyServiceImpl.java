package grp3.java.booknet.service.impl.admin;

import grp3.java.booknet.entity.book_service.CirculationPolicy;
import grp3.java.booknet.repository.book_service.CirculationPolicyRepository;
import grp3.java.booknet.service.admin.CirculationPolicyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CirculationPolicyServiceImpl implements CirculationPolicyService {

    private static final long SINGLETON_ID = 1L;

    private final CirculationPolicyRepository policyRepository;

    /**
     * Seed default policy khi app khởi động lần đầu.
     * Chạy trong transaction riêng để không block startup.
     */
    @PostConstruct
    @Transactional
    public void seedDefaultPolicy() {
        if (!policyRepository.existsById(SINGLETON_ID)) {
            CirculationPolicy defaults = CirculationPolicy.builder()
                    .maxLoansPerMember(5)
                    .loanPeriodDays(14)
                    .renewPeriodDays(7)
                    .dailyFineRate(new java.math.BigDecimal("5000.00"))
                    .reservationHoldDays(2)
                    .updatedBy("system")
                    .build();
            policyRepository.save(defaults);
            log.info("CirculationPolicy: seeded default policy (id=1).");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CirculationPolicy getPolicy() {
        return policyRepository.findById(SINGLETON_ID)
                .orElseGet(() -> {
                    // Fallback phòng trường hợp seed chưa chạy (test context)
                    CirculationPolicy fallback = new CirculationPolicy();
                    return fallback;
                });
    }

    @Override
    @Transactional
    public CirculationPolicy updatePolicy(CirculationPolicy updated, String adminEmail) {
        CirculationPolicy current = policyRepository.findById(SINGLETON_ID)
                .orElse(CirculationPolicy.builder().build());

        current.setMaxLoansPerMember(updated.getMaxLoansPerMember());
        current.setLoanPeriodDays(updated.getLoanPeriodDays());
        current.setRenewPeriodDays(updated.getRenewPeriodDays());
        current.setDailyFineRate(updated.getDailyFineRate());
        current.setReservationHoldDays(updated.getReservationHoldDays());
        current.setUpdatedBy(adminEmail);

        CirculationPolicy saved = policyRepository.save(current);
        log.info("CirculationPolicy updated by {} — rate={}/day, maxLoans={}, loanDays={}",
                adminEmail, saved.getDailyFineRate(), saved.getMaxLoansPerMember(), saved.getLoanPeriodDays());
        return saved;
    }
}
