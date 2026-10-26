package grp3.java.booknet.service;

import grp3.java.booknet.dto.UserLoginDto;
import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.dto.UserSessionDto;

public interface AuthService {
    UserSessionDto register(UserRegisterDto registerDto);
    UserSessionDto login(UserLoginDto loginDto);
}
