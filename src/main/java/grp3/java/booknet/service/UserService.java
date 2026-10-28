package grp3.java.booknet.service;

import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.entity.user.User;

public interface UserService {

    User registerNewUser(UserRegisterDto registerDto);

    boolean existsByEmail(String email);

    User findByEmail(String email);
}
