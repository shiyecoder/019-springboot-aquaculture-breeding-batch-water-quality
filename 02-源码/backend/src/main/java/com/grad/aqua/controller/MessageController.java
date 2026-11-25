package com.grad.aqua.controller;

import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.R;
import com.grad.aqua.entity.SysMessage;
import com.grad.aqua.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 站内消息中心接口 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/page")
    public R<PageResult<SysMessage>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Boolean unreadOnly) {
        return R.ok(messageService.page(page, size, unreadOnly));
    }

    @GetMapping("/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("count", messageService.unreadCount()));
    }

    @GetMapping("/latest")
    public R<List<SysMessage>> latest(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(messageService.latest(limit));
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Void> readAll() {
        messageService.readAll();
        return R.ok();
    }
}
