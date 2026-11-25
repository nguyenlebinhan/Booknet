package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.CirculationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CirculationPolicyRepository extends JpaRepository<CirculationPolicy, Long> {
}
