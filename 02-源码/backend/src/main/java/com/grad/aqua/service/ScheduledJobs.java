package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.aqua.entity.DiseaseRecord;
import com.grad.aqua.entity.WaterWarning;
import com.grad.aqua.mapper.DiseaseRecordMapper;
import com.grad.aqua.mapper.WaterWarningMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：
 * 1) 每 30 分钟扫描超限但未生成预警的水质记录，补建预警（规则调整或重启后的兜底）；
 * 2) 每天 08:00 检查已过休药期的用药记录，推送提醒给技术员，避免漏掉可起捕批次。
 */
@Slf4j
@Component
public class ScheduledJobs {

    private final WaterService waterService;
    private final WaterWarningMapper warningMapper;
    private final DiseaseRecordMapper diseaseMapper;
    private final LogService logService;

    public ScheduledJobs(WaterService waterService, WaterWarningMapper warningMapper,
                         DiseaseRecordMapper diseaseMapper, LogService logService) {
        this.waterService = waterService;
        this.warningMapper = warningMapper;
        this.diseaseMapper = diseaseMapper;
        this.logService = logService;
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void scanWater() {
        try {
            int n = waterService.scanOverLimit();
            if (n > 0) {
                log.info("水质超限扫描补建预警 {} 条", n);
            }
        } catch (Exception e) {
            log.warn("水质扫描失败：{}", e.getMessage());
        }
    }

    /** 预警超时提醒：待处置超过 24 小时仍未处置的，重新推送 */
    @Scheduled(cron = "0 0 8 * * *")
    public void remindOverdueWarning() {
        List<WaterWarning> list = warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                .eq(WaterWarning::getStatus, "PENDING")
                .lt(WaterWarning::getCreateTime, LocalDateTime.now().minusHours(24)));
        for (WaterWarning w : list) {
            logService.notifyRole("TECH", "预警 " + w.getWarningNo() + " 已超过 24 小时未处置", "WARNING", w.getId(), w.getWarningNo());
        }
    }

    /** 休药期到期提醒 */
    @Scheduled(cron = "0 10 8 * * *")
    public void remindWithdrawal() {
        List<DiseaseRecord> list = diseaseMapper.selectList(new LambdaQueryWrapper<DiseaseRecord>()
                .eq(DiseaseRecord::getReviewStatus, "APPROVED")
                .eq(DiseaseRecord::getWithdrawalEnd, LocalDate.now()));
        for (DiseaseRecord d : list) {
            logService.notifyRole("MANAGER", "用药 " + d.getDrugName() + " 休药期今日到期，批次可安排起捕", "DISEASE", d.getId(), d.getDiseaseNo());
        }
    }
}
