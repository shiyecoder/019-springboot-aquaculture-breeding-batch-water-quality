package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.service.RuleService;
import com.grad.aqua.service.WaterService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 水质监测 + 预警闭环接口。
 * 录入水质后自动计算风险分层，超限自动生成预警，处置与复测都必须留痕。
 */
@RestController
@RequestMapping("/api/water")
public class WaterController {

    private final WaterService waterService;
    private final RuleService ruleService;

    public WaterController(WaterService waterService, RuleService ruleService) {
        this.waterService = waterService;
        this.ruleService = ruleService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) Long pondId,
                                                   @RequestParam(required = false) Long batchId,
                                                   @RequestParam(required = false) String level,
                                                   @RequestParam(required = false) String start,
                                                   @RequestParam(required = false) String end,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(waterService.page(page, size, pondId, batchId, level, start, end, keyword));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return R.ok(waterService.save(body));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(waterService.waterDetail(id));
    }

    /** 主动重新计算：修改权重后对指定记录重算，旧快照保留 */
    @PostMapping("/{id}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long id) {
        return R.ok(ruleService.evaluate(id));
    }

    // ---------- 预警中心 ----------

    @GetMapping("/warning/page")
    public R<PageResult<Map<String, Object>>> warnings(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) String warnType,
                                                       @RequestParam(required = false) Long pondId,
                                                       @RequestParam(required = false) String tab) {
        return R.ok(waterService.warnings(page, size, status, level, warnType, pondId, tab));
    }

    @GetMapping("/warning/{id}")
    public R<Map<String, Object>> warningDetail(@PathVariable Long id) {
        return R.ok(waterService.warningDetail(id));
    }

    @PostMapping("/warning/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        waterService.handle(id, (String) body.get("measure"), (String) body.get("detail"));
        return R.ok();
    }

    @PostMapping("/warning/{id}/retest")
    public R<String> retest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long waterId = body.get("waterId") == null ? null : Long.valueOf(String.valueOf(body.get("waterId")));
        return R.ok(waterService.retest(id, waterId, (String) body.get("result")));
    }

    @PostMapping("/warning/{id}/close")
    public R<Void> close(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        waterService.close(id, (String) body.get("reason"));
        return R.ok();
    }
}
