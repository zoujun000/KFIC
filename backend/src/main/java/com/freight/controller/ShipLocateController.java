package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.service.AisStreamService;
import com.freight.vo.ShipLocateSearchVO;
import com.freight.vo.ShipLocateStatusVO;
import com.freight.vo.ShipLocateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "船舶定位")
@RestController
@RequestMapping("/api/ship-locate")
@RequiredArgsConstructor
public class ShipLocateController {

    private final AisStreamService aisStreamService;

    @Operation(summary = "按船名/船号/MMSI 搜索船舶")
    @GetMapping("/search")
    public Result<List<ShipLocateSearchVO>> search(@RequestParam String keyword) {
        return Result.success(aisStreamService.search(keyword));
    }

    @Operation(summary = "获取船舶实时位置与轨迹")
    @GetMapping("/{mmsi}")
    public Result<ShipLocateVO> locate(@PathVariable String mmsi) {
        ShipLocateVO vo = aisStreamService.locate(mmsi);
        if (vo == null) {
            return Result.error("未找到该船：索引随 AIS 实时数据持续积累，可稍后重试或确认船名/MMSI 正确");
        }
        return Result.success(vo);
    }

    @Operation(summary = "获取 AIS 数据流连接状态")
    @GetMapping("/status")
    public Result<ShipLocateStatusVO> status() {
        return Result.success(aisStreamService.status());
    }
}
