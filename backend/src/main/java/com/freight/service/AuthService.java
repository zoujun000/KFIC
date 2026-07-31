package com.freight.service;

import com.freight.dto.LoginDTO;
import com.freight.dto.RefreshTokenDTO;
import com.freight.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginDTO dto);
    Map<String, Object> refresh(RefreshTokenDTO dto);
    void register(RegisterDTO dto);
}
