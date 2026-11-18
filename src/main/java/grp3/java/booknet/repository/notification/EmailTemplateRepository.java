package grp3.java.booknet.repository.notification;

import grp3.java.booknet.entity.notification.EmailTemplate;
import grp3.java.booknet.enums.notification.EmailTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByType(EmailTemplateType type);

    List<EmailTemplate> findAllByOrderByIdAsc();
}
