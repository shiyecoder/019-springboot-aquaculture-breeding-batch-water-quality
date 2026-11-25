package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.HarvestRecord;
import com.grad.aqua.service.HarvestService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 分塘与起捕接口 */
@RestController
@RequestMapping("/api/harvest")
public class HarvestController {

    private final HarvestService harvestService;

    public HarvestController(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long batchId,
                                                   @RequestParam(required = false) String grade) {
        return R.ok(harvestService.harvestPage(page, size, status, batchId, grade));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(harvestService.createHarvest(body));
    }

    @PostMapping("/{id}/finish")
    public R<HarvestRecord> finish(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(harvestService.finish(id, body));
    }

    @PostMapping("/{id}/void")
    public R<Void> voidRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        harvestService.voidHarvest(id, (String) body.get("reason"));
        return R.ok();
    }

    @GetMapping("/transfer/page")
    public R<PageResult<Map<String, Object>>> transfers(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Long batchId) {
        return R.ok(harvestService.transferPage(page, size, batchId));
    }

    @PostMapping("/transfer")
    public R<Long> createTransfer(@RequestBody Map<String, Object> body) {
        return R.ok(harvestService.createTransfer(body));
    }
}
