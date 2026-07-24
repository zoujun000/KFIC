package com.freight.service.impl;

import com.freight.entity.Customer;
import com.freight.entity.FreightOrder;
import com.freight.service.AttachmentPathService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AttachmentPathServiceImpl implements AttachmentPathService {

    private static final String ATTACHMENT_ROOT = System.getProperty("user.home") + "/Desktop";

    @Override
    public Path resolveOrderDir(Customer customer, FreightOrder order) {
        String orderSo = order.getOrderSo() != null ? order.getOrderSo() : order.getOrderNo();
        return resolveOrderDir(customer, orderSo, order.getOrderNo());
    }

    @Override
    public Path resolveOrderDir(Customer customer, String orderSo, String orderNo) {
        String licenseDir = getLicenseDir(customer);
        String companyName = sanitizeFileName(customer.getCompanyName());
        String so = sanitizeFileName(orderSo != null ? orderSo : orderNo);
        return Paths.get(ATTACHMENT_ROOT, licenseDir, companyName, so);
    }

    @Override
    public String sanitizeFileName(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    public String getLicenseDir(Customer customer) {
        if ("COLOAD".equalsIgnoreCase(customer.getCustomerType())) {
            return "同行营业执照";
        }
        return "直客营业执照";
    }
}
