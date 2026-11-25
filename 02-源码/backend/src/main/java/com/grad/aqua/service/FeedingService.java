package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.BreedingBatch;
import com.grad.aqua.entity.FeedingPlan;
import com.grad.aqua.entity.FeedingRecord;
import com.grad.aqua.entity.Pond;
import com.grad.aqua.mapper.BreedingBatchMapper;
import com.grad.aqua.mapper.FeedingPlanMapper;
import com.grad.aqua.mapper.FeedingRecordMapper;
import com.grad.aqua.mapper.PondMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 投喂计划与执行。
 * 规则：实际用量不得超过领料量；同一批次当日实际投喂量超过计划总量 20% 时标记需技术员复核。
 */
@Service
public class FeedingService {

    private final FeedingPlanMapper planMapper;
    private final FeedingRecordMapper recordMapper;
    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final LogService logService;

    public FeedingService(FeedingPlanMapper planMapper, FeedingRecordMapper recordMapper,
                          BreedingBatchMapper batchMapper, PondMapper pondMapper, LogService logService) {
        this.planMapper = planMapper;
        this.recordMapper = recordMapper;
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> plans(int page, int size, String status, Long batchId, String planDate) {
        LambdaQueryWrapper<FeedingPlan> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(FeedingPlan::getStatus, status);
        }
        if (batchId != null) {
            w.eq(FeedingPlan::getBatchId, batchId);
        }
        if (planDate != null && !planDate.isBlank()) {
            w.eq(FeedingPlan::getPlanDate, LocalDate.parse(planDate.substring(0, 10)));
        }
        w.orderByDesc(FeedingPlan::getPlanDate).orderByAsc(FeedingPlan::getPeriod);
        Page<FeedingPlan> p = planMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(pl -> {
            Map<String, Object> m = new LinkedHashMap<>();
            BreedingBatch b = batches.get(pl.getBatchId());
            m.put("id", pl.getId());
            m.put("planNo", pl.getPlanNo());
            m.put("batchId", pl.getBatchId());
            m.put("batchNo", b == null ? "-" : b.getBatchNo());
            m.put("variety", b == null ? "-" : b.getVariety());
            m.put("pondName", b == null || ponds.get(b.getPondId()) == null ? "-" : ponds.get(b.getPondId()).getPondName());
            m.put("feedName", pl.getFeedName());
            m.put("feedQty", pl.getFeedQty());
            m.put("planDate", pl.getPlanDate());
            m.put("period", pl.getPeriod());
            m.put("executorName", pl.getExecutorName());
            m.put("status", pl.getStatus());
            m.put("remark", pl.getRemark());
            m.put("createTime", pl.getCreateTime());
            List<FeedingRecord> rs = recordMapper.selectList(new LambdaQueryWrapper<FeedingRecord>()
                    .eq(FeedingRecord::getPlanId, pl.getId()));
            m.put("actualQty", rs.isEmpty() ? null : rs.get(0).getActualQty());
            m.put("checkFlag", rs.isEmpty() ? 0 : rs.get(0).getCheckFlag());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long savePlan(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        FeedingPlan plan = id == null ? new FeedingPlan() : planMapper.selectById(id);
        if (plan == null) {
            throw new BizException("投喂计划不存在");
        }
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        if (batchId == null || batchMapper.selectById(batchId) == null) {
            throw new BizException("请选择有效的养殖批次");
        }
        BigDecimal qty = dec(body.get("feedQty"));
        if (qty.signum() <= 0) {
            throw new BizException("标准投喂量必须大于 0");
        }
        plan.setBatchId(batchId);
        plan.setFeedName(str(body.get("feedName")) == null ? "颗粒配合饲料" : str(body.get("feedName")));
        plan.setFeedQty(qty);
        plan.setPlanDate(body.get("planDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(body.get("planDate")).substring(0, 10)));
        plan.setPeriod(str(body.get("period")) == null ? "AM" : str(body.get("period")));
        plan.setExecutorId(body.get("executorId") == null ? UserContext.userId() : Long.valueOf(String.valueOf(body.get("executorId"))));
        plan.setExecutorName(str(body.get("executorName")) == null ? UserContext.name() : str(body.get("executorName")));
        plan.setRemark(str(body.get("remark")));
        if (id == null) {
            plan.setPlanNo("TF" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                    + String.format("%03d", planMapper.selectCount(null) % 999 + 1));
            plan.setStatus("PENDING");
            plan.setCreateTime(LocalDateTime.now());
            plan.setUpdateTime(LocalDateTime.now());
            planMapper.insert(plan);
            logService.record("FEEDING", plan.getId(), plan.getPlanNo(), null, "PENDING", "生成投喂任务", null);
        } else {
            plan.setUpdateTime(LocalDateTime.now());
            planMapper.updateById(plan);
        }
        return plan.getId();
    }

    /** 执行投喂：回填实际用量，超标自动标记复核并通知技术员 */
    @Transactional
    public Map<String, Object> execute(Long planId, BigDecimal actualQty, String remark) {
        FeedingPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("投喂计划不存在");
        }
        if ("DONE".equals(plan.getStatus())) {
            throw new BizException("该任务已执行，请勿重复提交");
        }
        if (actualQty == null || actualQty.signum() < 0) {
            throw new BizException("实际用量不能为负数");
        }
        // 硬性规则：实际用量不得超过领料量
        if (actualQty.compareTo(plan.getFeedQty()) > 0) {
            throw new BizException("实际用量 " + actualQty + " kg 超过领料量 " + plan.getFeedQty() + " kg，请先补领料单");
        }
        FeedingRecord rec = new FeedingRecord();
        rec.setPlanId(plan.getId());
        rec.setBatchId(plan.getBatchId());
        rec.setFeedQty(plan.getFeedQty());
        rec.setActualQty(actualQty);
        rec.setExecuteTime(LocalDateTime.now());
        rec.setOperator(UserContext.name());
        rec.setRemark(remark);
        rec.setCheckFlag(0);
        rec.setCreateTime(LocalDateTime.now());

        // 当日计划总量与实际总量比对，超 20% 需技术员复核
        List<FeedingPlan> todayPlans = planMapper.selectList(new LambdaQueryWrapper<FeedingPlan>()
                .eq(FeedingPlan::getBatchId, plan.getBatchId()).eq(FeedingPlan::getPlanDate, plan.getPlanDate()));
        BigDecimal planTotal = todayPlans.stream().map(FeedingPlan::getFeedQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal doneTotal = recordMapper.selectList(new LambdaQueryWrapper<FeedingRecord>()
                        .eq(FeedingRecord::getBatchId, plan.getBatchId())
                        .ge(FeedingRecord::getExecuteTime, plan.getPlanDate().atStartOfDay()))
                .stream().map(FeedingRecord::getActualQty).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(actualQty);
        boolean over = planTotal.signum() > 0
                && doneTotal.compareTo(planTotal.multiply(new BigDecimal("1.2"))) > 0;
        if (over) {
            rec.setCheckFlag(1);
            rec.setCheckNote("当日实际投喂 " + doneTotal.setScale(1, RoundingMode.HALF_UP) + " kg，超过计划 "
                    + planTotal.setScale(1, RoundingMode.HALF_UP) + " kg 的 20%，待技术员复核");
        }
        recordMapper.insert(rec);

        plan.setStatus("DONE");
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);

        BreedingBatch batch = batchMapper.selectById(plan.getBatchId());
        if (batch != null) {
            batch.setFeedTotal((batch.getFeedTotal() == null ? BigDecimal.ZERO : batch.getFeedTotal()).add(actualQty));
            batch.setUpdateTime(LocalDateTime.now());
            batchMapper.updateById(batch);
        }
        logService.record("FEEDING", plan.getId(), plan.getPlanNo(), "PENDING", "DONE", "执行投喂", remark);
        if (over) {
            logService.notifyRole("TECH", "投喂超标待复核：" + plan.getPlanNo(), "FEEDING", plan.getId(), plan.getPlanNo());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", rec.getId());
        out.put("checkFlag", rec.getCheckFlag());
        out.put("checkNote", rec.getCheckNote());
        out.put("dayTotal", doneTotal.setScale(1, RoundingMode.HALF_UP));
        return out;
    }

    public PageResult<FeedingRecord> records(int page, int size, Long batchId) {
        LambdaQueryWrapper<FeedingRecord> w = new LambdaQueryWrapper<FeedingRecord>()
                .eq(batchId != null, FeedingRecord::getBatchId, batchId)
                .orderByDesc(FeedingRecord::getExecuteTime);
        Page<FeedingRecord> p = recordMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
