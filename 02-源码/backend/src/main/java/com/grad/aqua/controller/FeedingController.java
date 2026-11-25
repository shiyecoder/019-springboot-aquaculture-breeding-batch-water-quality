package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.FeedingRecord;
import com.grad.aqua.service.FeedingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 投喂计划与执行接口 */
@RestController
@RequestMapping("/api/feeding")
public class FeedingController {

    private final FeedingService feedingService;

    public FeedingController(FeedingService feedingService) {
        this.feedingService = feedingService;
    }

    @GetMapping("/plan/page")
    public R<PageResult<Map<String, Object>>> plans(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long batchId,
                                                    @RequestParam(required = false) String planDate) {
        return R.ok(feedingService.plans(page, size, status, batchId, planDate));
    }

    @PostMapping("/plan")
    public R<Long> savePlan(@RequestBody Map<String, Object> body) {
        return R.ok(feedingService.savePlan(body));
    }

    @PostMapping("/plan/{id}/execute")
    public R<Map<String, Object>> execute(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal actual = body.get("actualQty") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("actualQty")));
        return R.ok(feedingService.execute(id, actual, (String) body.get("remark")));
    }

    @GetMapping("/record/page")
    public R<PageResult<FeedingRecord>> records(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Long batchId) {
        return R.ok(feedingService.records(page, size, batchId));
    }
}
