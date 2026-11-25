package com.grad.aqua.controller;

import com.grad.aqua.common.R;
import com.grad.aqua.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 养殖批次看板接口 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/api/dashboard", "/api/aquaculture-water/dashboard"})
    public R<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days,
                                           @RequestParam(required = false) Long pondId) {
        return R.ok(dashboardService.overview(days, pondId));
    }
}
