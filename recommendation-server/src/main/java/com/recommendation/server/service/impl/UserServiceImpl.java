package com.recommendation.server.service.impl;

import com.recommendation.common.exception.BusinessException;
import com.recommendation.common.util.JwtUtil;
import com.recommendation.common.util.RedisUtil;
import com.recommendation.pojo.dto.UserLoginDTO;
import com.recommendation.pojo.dto.UserRegisterDTO;
import com.recommendation.pojo.entity.User;
import com.recommendation.pojo.vo.LoginVO;
import com.recommendation.pojo.vo.UserVO;
import com.recommendation.server.mapper.UserMapper;
import com.recommendation.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(UserLoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("Username or password is incorrect");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("Username or password is incorrect");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("Account has been disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        redisUtil.set("rec:user:" + user.getId(), user, 7, TimeUnit.DAYS);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(toUserVO(user));

        return loginVO;
    }

    @Override
    public void register(UserRegisterDTO registerDTO) {
        User existUser = userMapper.selectByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException("Username already exists");
        }

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setNickname(registerDTO.getNickname());
        user.setStatus(1);

        userMapper.insert(user);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
