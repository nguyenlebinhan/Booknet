package grp3.java.booknet.config.security;
@org.springframework.context.annotation.Configuration
public class RoleDefinitionInitializer {
 @org.springframework.context.annotation.Bean
 org.springframework.boot.CommandLineRunner initializeRoles(grp3.java.booknet.service.member.RoleManagementService service){
 return args->service.initialize();
 }
}
