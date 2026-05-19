package com.freight.service;

import com.freight.dto.PortChargeSummaryDTO;
import com.freight.entity.DestPortCharge;
import com.freight.entity.PortChargeUploadLog;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface DestPortChargeService {
    PortChargeUploadLog uploadAndParse(MultipartFile file);
    PortChargeSummaryDTO calcCharges(String destination, BigDecimal volume, String clientType);
    List<String> listDestinations();
    List<PortChargeUploadLog> uploadLogs();
    List<DestPortCharge> listByDestination(String destination);
    void updateCharge(DestPortCharge charge);
    void deleteCharge(Long id);
    List<DestPortCharge> listAll();
}
