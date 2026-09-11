package com.freight.service.impl;

import com.freight.common.exception.BusinessException;
import com.freight.entity.QuoteTemplate;
import com.freight.mapper.QuoteTemplateMapper;
import com.freight.service.QuoteTemplateService;
import com.freight.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QuoteTemplateServiceImpl implements QuoteTemplateService {

    private final QuoteTemplateMapper quoteTemplateMapper;

    @Override
    public String getCurrentUserTemplate() {
        QuoteTemplate quoteTemplate = quoteTemplateMapper.selectById(requireCurrentUserId());
        return quoteTemplate == null ? null : quoteTemplate.getTemplate();
    }

    @Override
    public void saveCurrentUserTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new BusinessException("报价模版不能为空");
        }
        if (template.length() > 10000) {
            throw new BusinessException("报价模版不能超过10000个字符");
        }

        Long userId = requireCurrentUserId();
        QuoteTemplate quoteTemplate = quoteTemplateMapper.selectById(userId);
        if (quoteTemplate == null) {
            quoteTemplate = new QuoteTemplate();
            quoteTemplate.setUserId(userId);
            quoteTemplate.setTemplate(template);
            quoteTemplate.setCreateTime(LocalDateTime.now());
            quoteTemplate.setUpdateTime(LocalDateTime.now());
            quoteTemplateMapper.insert(quoteTemplate);
        } else {
            quoteTemplate.setTemplate(template);
            quoteTemplate.setUpdateTime(LocalDateTime.now());
            quoteTemplateMapper.updateById(quoteTemplate);
        }
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未获取到当前用户身份");
        }
        return userId;
    }
}
