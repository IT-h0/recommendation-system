package com.recommendation.server.service;

import com.recommendation.pojo.dto.UserLoginDTO;
import com.recommendation.pojo.dto.UserRegisterDTO;
import com.recommendation.pojo.vo.LoginVO;
import com.recommendation.pojo.vo.UserVO;

public interface UserService {

    LoginVO login(UserLoginDTO loginDTO);

    void register(UserRegisterDTO registerDTO);

    UserVO getUserById(Long userId);
}
