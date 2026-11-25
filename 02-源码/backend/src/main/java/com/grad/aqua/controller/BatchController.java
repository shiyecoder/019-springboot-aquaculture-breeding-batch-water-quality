package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.BreedingBatch;
import com.grad.aqua.service.BatchService;
import com.grad.aqua.service.RuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 养殖批次接口（对应需求文档 /api/aquaculture-water 主体接口）。
 * 状态流转、评分计算均在服务端校验，前端只传目标状态与原因。
 */
@RestController
@RequestMapping("/api/aquaculture-water")
public class BatchController {

    private final BatchService batchService;
    private final RuleService ruleService;

    public BatchController(BatchService batchService, RuleService ruleService) {
        this.batchService = batchService;
        this.ruleService = ruleService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long pondId,
                                                   @RequestParam(required = false) String variety,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String growthStage) {
        return R.ok(batchService.page(page, size, keyword, pondId, variety, status, growthStage));
    }

    @GetMapping("/options")
    public R<List<BreedingBatch>> options() {
        return R.ok(batchService.options());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(batchService.detail(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(batchService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(batchService.save(body));
    }

    @PostMapping("/{id}/transition")
    public R<BreedingBatch> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(batchService.transition(id, String.valueOf(body.get("target")), (String) body.get("reason")));
    }

    /** 风险分层计算：waterId 指向一条水质记录 */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@RequestParam Long waterId) {
        return R.ok(ruleService.evaluate(waterId));
    }
}
