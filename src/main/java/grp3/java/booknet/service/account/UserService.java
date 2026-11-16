package grp3.java.booknet.service.account;

import grp3.java.booknet.dto.user.response.UserRegisterDto;
import grp3.java.booknet.entity.user.User;

public interface UserService {

    User registerNewUser(UserRegisterDto registerDto);

    boolean existsByEmail(String email);

    User findByEmail(String email);
}
