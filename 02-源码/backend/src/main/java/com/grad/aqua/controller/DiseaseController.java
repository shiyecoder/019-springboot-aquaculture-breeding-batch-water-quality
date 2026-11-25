package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.DiseaseRecord;
import com.grad.aqua.service.DiseaseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 病害上报与用药审核接口 */
@RestController
@RequestMapping("/api/disease")
public class DiseaseController {

    private final DiseaseService diseaseService;

    public DiseaseController(DiseaseService diseaseService) {
        this.diseaseService = diseaseService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long batchId,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(diseaseService.page(page, size, status, batchId, keyword));
    }

    @PostMapping
    public R<Long> report(@RequestBody Map<String, Object> body) {
        return R.ok(diseaseService.report(body));
    }

    @PostMapping("/{id}/review")
    public R<DiseaseRecord> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer days = body.get("withdrawalDays") == null ? null : Integer.valueOf(String.valueOf(body.get("withdrawalDays")));
        return R.ok(diseaseService.review(id, String.valueOf(body.get("status")),
                (String) body.get("diagnosis"), (String) body.get("drugName"),
                (String) body.get("dose"), days, (String) body.get("note")));
    }

    @PostMapping("/{id}/void")
    public R<Void> voidRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        diseaseService.voidRecord(id, (String) body.get("reason"));
        return R.ok();
    }
}
