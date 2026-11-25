package com.grad.aqua.controller;

import com.grad.aqua.common.R;
import com.grad.aqua.entity.RiskRule;
import com.grad.aqua.entity.RiskScore;
import com.grad.aqua.service.RuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 创新点接口：规则配置（版本化）、计算留痕、人工修正。
 * 规则修改后只影响新计算或主动重算的数据，历史快照不被覆盖。
 */
@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/list")
    public R<List<RiskRule>> list() {
        return R.ok(ruleService.rules());
    }

    @GetMapping("/active")
    public R<RiskRule> active() {
        return R.ok(ruleService.activeRule());
    }

    @PostMapping
    public R<RiskRule> save(@RequestBody Map<String, Object> body) {
        return R.ok(ruleService.saveRule(body));
    }

    @GetMapping("/score")
    public R<List<RiskScore>> scoreHistory(@RequestParam Long waterId) {
        return R.ok(ruleService.scoreHistory(waterId));
    }

    @PostMapping("/score/{id}/manual")
    public R<RiskScore> manual(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(ruleService.manual(id, String.valueOf(body.get("level")), (String) body.get("reason")));
    }
}
