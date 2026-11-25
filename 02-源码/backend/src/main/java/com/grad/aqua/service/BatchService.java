package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.*;
import com.grad.aqua.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 养殖批次：核心主数据。
 *
 * 状态机（后端强制校验，不依赖前端按钮）：
 * PREP 准备中 → BREEDING 养殖中 → HARVESTING 起捕中 → CLOSED 已结束。
 * 任何状态流转都写 flow_log，并推送站内消息给场长。
 */
@Service
public class BatchService {

    private static final Map<String, List<String>> NEXT = Map.of(
            "PREP", List.of("BREEDING"),
            "BREEDING", List.of("HARVESTING"),
            "HARVESTING", List.of("CLOSED"),
            "CLOSED", List.of());

    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final WaterQualityMapper waterMapper;
    private final WaterWarningMapper warningMapper;
    private final FeedingPlanMapper planMapper;
    private final HarvestRecordMapper harvestMapper;
    private final RiskScoreMapper scoreMapper;
    private final LogService logService;

    public BatchService(BreedingBatchMapper batchMapper, PondMapper pondMapper, WaterQualityMapper waterMapper,
                        WaterWarningMapper warningMapper, FeedingPlanMapper planMapper,
                        HarvestRecordMapper harvestMapper, RiskScoreMapper scoreMapper, LogService logService) {
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.waterMapper = waterMapper;
        this.warningMapper = warningMapper;
        this.planMapper = planMapper;
        this.harvestMapper = harvestMapper;
        this.scoreMapper = scoreMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String keyword, Long pondId,
                                                String variety, String status, String growthStage) {
        LambdaQueryWrapper<BreedingBatch> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(BreedingBatch::getBatchNo, keyword).or().like(BreedingBatch::getVariety, keyword)
                    .or().like(BreedingBatch::getManagerName, keyword));
        }
        if (pondId != null) {
            w.eq(BreedingBatch::getPondId, pondId);
        }
        if (variety != null && !variety.isBlank()) {
            w.eq(BreedingBatch::getVariety, variety);
        }
        if (status != null && !status.isBlank()) {
            w.eq(BreedingBatch::getStatus, status);
        }
        if (growthStage != null && !growthStage.isBlank()) {
            w.eq(BreedingBatch::getGrowthStage, growthStage);
        }
        w.orderByDesc(BreedingBatch::getId);
        Page<BreedingBatch> p = batchMapper.selectPage(new Page<>(page, size), w);

        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<WaterWarning> openWarnings = warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                .notIn(WaterWarning::getStatus, "CLOSED"));

        List<Map<String, Object>> list = p.getRecords().stream().map(b -> {
            Map<String, Object> m = baseVo(b, ponds.get(b.getPondId()));
            long open = openWarnings.stream().filter(x -> Objects.equals(x.getBatchId(), b.getId())).count();
            m.put("openWarning", open);
            // 最近一次水质记录用于列表展示水质等级
            List<WaterQuality> ws = waterMapper.selectList(new LambdaQueryWrapper<WaterQuality>()
                    .eq(WaterQuality::getBatchId, b.getId()).orderByDesc(WaterQuality::getCheckTime).last("limit 1"));
            WaterQuality last = ws.isEmpty() ? null : ws.get(0);
            m.put("lastCheckTime", last == null ? null : last.getCheckTime());
            m.put("waterLevel", last == null ? "NONE" : last.getLevel());
            m.put("dissolvedOxygen", last == null ? null : last.getDissolvedOxygen());
            m.put("ph", last == null ? null : last.getPh());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    public List<BreedingBatch> options() {
        return batchMapper.selectList(new LambdaQueryWrapper<BreedingBatch>()
                .ne(BreedingBatch::getStatus, "CLOSED").orderByDesc(BreedingBatch::getId));
    }

    /** 详情：基础信息 + 塘口 + 最近水质 + 风险留痕 + 预警 + 投喂 + 时间轴，不重复列表页 */
    public Map<String, Object> detail(Long id) {
        BreedingBatch b = batchMapper.selectById(id);
        if (b == null) {
            throw new BizException("养殖批次不存在");
        }
        Pond pond = b.getPondId() == null ? null : pondMapper.selectById(b.getPondId());
        Map<String, Object> out = baseVo(b, pond);
        List<WaterQuality> waters = waterMapper.selectList(new LambdaQueryWrapper<WaterQuality>()
                .eq(WaterQuality::getBatchId, id).orderByDesc(WaterQuality::getCheckTime).last("limit 6"));
        out.put("waters", waters);
        out.put("latestWater", waters.isEmpty() ? null : waters.get(0));
        out.put("warnings", warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                .eq(WaterWarning::getBatchId, id).orderByDesc(WaterWarning::getId).last("limit 8")));
        out.put("plans", planMapper.selectList(new LambdaQueryWrapper<FeedingPlan>()
                .eq(FeedingPlan::getBatchId, id).orderByDesc(FeedingPlan::getPlanDate).last("limit 8")));
        out.put("harvests", harvestMapper.selectList(new LambdaQueryWrapper<HarvestRecord>()
                .eq(HarvestRecord::getBatchId, id).orderByDesc(HarvestRecord::getId)));
        out.put("timeline", logService.timeline("BATCH", id));
        if (!waters.isEmpty()) {
            List<RiskScore> scores = scoreMapper.selectList(new LambdaQueryWrapper<RiskScore>()
                    .eq(RiskScore::getBizId, waters.get(0).getId()).orderByDesc(RiskScore::getId).last("limit 1"));
            out.put("latestScore", scores.isEmpty() ? null : scores.get(0));
        }
        out.put("nextStatus", NEXT.getOrDefault(b.getStatus(), List.of()));
        return out;
    }

    private Map<String, Object> baseVo(BreedingBatch b, Pond pond) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("batchNo", b.getBatchNo());
        m.put("pondId", b.getPondId());
        m.put("pondNo", pond == null ? "-" : pond.getPondNo());
        m.put("pondName", pond == null ? "-" : pond.getPondName());
        m.put("variety", b.getVariety());
        m.put("seedSource", b.getSeedSource());
        m.put("stockDate", b.getStockDate());
        m.put("stockQty", b.getStockQty());
        m.put("avgWeight", b.getAvgWeight());
        m.put("expectHarvestDate", b.getExpectHarvestDate());
        m.put("targetOutput", b.getTargetOutput());
        m.put("growthStage", b.getGrowthStage());
        m.put("status", b.getStatus());
        m.put("currentStock", b.getCurrentStock());
        m.put("feedTotal", b.getFeedTotal());
        m.put("managerId", b.getManagerId());
        m.put("managerName", b.getManagerName());
        m.put("remark", b.getRemark());
        m.put("createTime", b.getCreateTime());
        m.put("updateTime", b.getUpdateTime());
        return m;
    }

    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        BreedingBatch b = id == null ? new BreedingBatch() : batchMapper.selectById(id);
        if (b == null) {
            throw new BizException("养殖批次不存在");
        }
        Long pondId = body.get("pondId") == null ? null : Long.valueOf(String.valueOf(body.get("pondId")));
        if (pondId == null) {
            throw new BizException("养殖池不能为空");
        }
        if (pondMapper.selectById(pondId) == null) {
            throw new BizException("选择的养殖池不存在");
        }
        String variety = str(body.get("variety"));
        if (variety == null || variety.isBlank()) {
            throw new BizException("养殖品种不能为空");
        }
        LocalDate stockDate = body.get("stockDate") == null || String.valueOf(body.get("stockDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("stockDate")).substring(0, 10));
        LocalDate expect = body.get("expectHarvestDate") == null || String.valueOf(body.get("expectHarvestDate")).isBlank()
                ? null : LocalDate.parse(String.valueOf(body.get("expectHarvestDate")).substring(0, 10));
        if (expect != null && expect.isBefore(stockDate)) {
            throw new BizException("预计起捕日不能早于投苗日期");
        }
        Integer qty = body.get("stockQty") == null ? null : Integer.valueOf(String.valueOf(body.get("stockQty")));
        if (qty != null && qty <= 0) {
            throw new BizException("投苗数量必须大于 0");
        }
        b.setPondId(pondId);
        b.setVariety(variety.trim());
        b.setSeedSource(str(body.get("seedSource")));
        b.setStockDate(stockDate);
        b.setStockQty(qty);
        b.setAvgWeight(dec(body.get("avgWeight")));
        b.setExpectHarvestDate(expect);
        b.setTargetOutput(dec(body.get("targetOutput")));
        b.setGrowthStage(str(body.get("growthStage")) == null ? "FRY" : str(body.get("growthStage")));
        b.setManagerId(body.get("managerId") == null ? UserContext.userId() : Long.valueOf(String.valueOf(body.get("managerId"))));
        b.setManagerName(str(body.get("managerName")) == null ? UserContext.name() : str(body.get("managerName")));
        b.setRemark(str(body.get("remark")));
        if (id == null) {
            b.setBatchNo(nextBatchNo());
            b.setStatus("PREP");
            b.setCurrentStock(dec(body.get("currentStock")));
            b.setFeedTotal(BigDecimal.ZERO);
            b.setCreateTime(LocalDateTime.now());
            b.setUpdateTime(LocalDateTime.now());
            batchMapper.insert(b);
            logService.record("BATCH", b.getId(), b.getBatchNo(), null, "PREP", "批次建档", null);
        } else {
            b.setUpdateTime(LocalDateTime.now());
            batchMapper.updateById(b);
            logService.record("BATCH", b.getId(), b.getBatchNo(), b.getStatus(), b.getStatus(), "编辑批次", null);
        }
        return b.getId();
    }

    /** 批次编号由后端生成：YC + 年月 + 三位流水 */
    private String nextBatchNo() {
        String prefix = "YC" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = batchMapper.selectCount(new LambdaQueryWrapper<BreedingBatch>().likeRight(BreedingBatch::getBatchNo, prefix));
        return prefix + String.format("%03d", count + 1);
    }

    /** 状态流转：校验目标状态是否合法，写入前后状态与原因 */
    @Transactional
    public BreedingBatch transition(Long id, String target, String reason) {
        BreedingBatch b = batchMapper.selectById(id);
        if (b == null) {
            throw new BizException("养殖批次不存在");
        }
        List<String> allowed = NEXT.getOrDefault(b.getStatus(), List.of());
        if (!allowed.contains(target)) {
            throw new BizException("当前状态「" + statusText(b.getStatus()) + "」不允许流转到「" + statusText(target) + "」");
        }
        if ("CLOSED".equals(target) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("归档结束批次必须填写不少于 5 个字的原因");
        }
        String from = b.getStatus();
        b.setStatus(target);
        b.setUpdateTime(LocalDateTime.now());
        batchMapper.updateById(b);
        logService.record("BATCH", id, b.getBatchNo(), from, target, "状态流转", reason);
        logService.oper("养殖批次", "状态流转", b.getBatchNo(), from + " → " + target);
        logService.notifyRole("MANAGER", "批次 " + b.getBatchNo() + " 已流转为" + statusText(target), "BATCH", id, b.getBatchNo());
        return b;
    }

    public static String statusText(String s) {
        return switch (s == null ? "" : s) {
            case "PREP" -> "准备中";
            case "BREEDING" -> "养殖中";
            case "HARVESTING" -> "起捕中";
            case "CLOSED" -> "已结束";
            default -> s;
        };
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
