package com.freight.service;

import com.freight.dto.LoginDTO;
import com.freight.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginDTO dto);
    void register(RegisterDTO dto);
}
