package com.freight.service;

import com.freight.entity.FreightVesselSchedule;
import com.freight.entity.VesselScheduleUploadLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface VesselScheduleService {

    VesselScheduleUploadLog uploadAndParse(MultipartFile file);

    List<FreightVesselSchedule> listUpcomingByPortCodes(Collection<String> portCodes, int perPortLimit);

    List<VesselScheduleUploadLog> uploadLogs();
}
