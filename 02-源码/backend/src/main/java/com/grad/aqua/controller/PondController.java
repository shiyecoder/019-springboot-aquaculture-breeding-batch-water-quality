package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.Pond;
import com.grad.aqua.service.PondService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 塘口档案接口 */
@RestController
@RequestMapping("/api/pond")
public class PondController {

    private final PondService pondService;

    public PondController(PondService pondService) {
        this.pondService = pondService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String waterSource) {
        return R.ok(pondService.page(page, size, keyword, status, waterSource));
    }

    @GetMapping("/options")
    public R<List<Pond>> options() {
        return R.ok(pondService.options());
    }

    @GetMapping("/{id}")
    public R<Pond> detail(@PathVariable Long id) {
        return R.ok(pondService.detail(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(pondService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(pondService.save(body));
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        pondService.remove(id);
        return R.ok();
    }
}
