package grp3.java.booknet.entity.user;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import grp3.java.booknet.enums.user.*;
@Entity @Table(name="role_definitions") @Getter @Setter @NoArgsConstructor
public class RoleDefinition {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String name;
 @Column(length=500) private String description;
 @Enumerated(EnumType.STRING) @Column(unique=true) private UserRole systemRole;
 @Version private long revision;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="role_capabilities",joinColumns=@JoinColumn(name="role_id"))
 @Enumerated(EnumType.STRING) @Column(name="capability",length=40) private Set<RoleCapability> capabilities=new HashSet<>();
 public String label(){return name;}
}
