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
 * 水质监测与预警闭环。
 *
 * 预警状态机：PENDING 待处置 → PROCESSING 处置中 → RETEST 待复测 → CLOSED 已关闭。
 * 每次录入水质后自动调用创新模块计算风险等级，中/高风险生成预警并推送技术员。
 */
@Service
public class WaterService {

    private final WaterQualityMapper waterMapper;
    private final WaterWarningMapper warningMapper;
    private final EmergencyRecordMapper emergencyMapper;
    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final RiskScoreMapper scoreMapper;
    private final RuleService ruleService;
    private final LogService logService;

    public WaterService(WaterQualityMapper waterMapper, WaterWarningMapper warningMapper,
                        EmergencyRecordMapper emergencyMapper, BreedingBatchMapper batchMapper,
                        PondMapper pondMapper, RiskScoreMapper scoreMapper,
                        RuleService ruleService, LogService logService) {
        this.waterMapper = waterMapper;
        this.warningMapper = warningMapper;
        this.emergencyMapper = emergencyMapper;
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.scoreMapper = scoreMapper;
        this.ruleService = ruleService;
        this.logService = logService;
    }

    // ---------------- 水质记录 ----------------

    public PageResult<Map<String, Object>> page(int page, int size, Long pondId, Long batchId, String level,
                                                String start, String end, String keyword) {
        LambdaQueryWrapper<WaterQuality> w = new LambdaQueryWrapper<>();
        if (pondId != null) {
            w.eq(WaterQuality::getPondId, pondId);
        }
        if (batchId != null) {
            w.eq(WaterQuality::getBatchId, batchId);
        }
        if (level != null && !level.isBlank()) {
            w.eq(WaterQuality::getLevel, level);
        }
        if (start != null && !start.isBlank()) {
            w.ge(WaterQuality::getCheckTime, LocalDate.parse(start.substring(0, 10)).atStartOfDay());
        }
        if (end != null && !end.isBlank()) {
            w.le(WaterQuality::getCheckTime, LocalDate.parse(end.substring(0, 10)).atTime(23, 59, 59));
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(WaterQuality::getRecordNo, keyword).or().like(WaterQuality::getTesterName, keyword));
        }
        // 待本人处理优先、更新时间倒序
        w.orderByDesc(WaterQuality::getCheckTime);
        Page<WaterQuality> p = waterMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            BreedingBatch b = batches.get(r.getBatchId());
            Pond pd = ponds.get(r.getPondId());
            m.put("id", r.getId());
            m.put("recordNo", r.getRecordNo());
            m.put("pondId", r.getPondId());
            m.put("pondName", pd == null ? "-" : pd.getPondName());
            m.put("batchId", r.getBatchId());
            m.put("batchNo", b == null ? "-" : b.getBatchNo());
            m.put("variety", b == null ? "-" : b.getVariety());
            m.put("checkTime", r.getCheckTime());
            m.put("waterTemp", r.getWaterTemp());
            m.put("ph", r.getPh());
            m.put("dissolvedOxygen", r.getDissolvedOxygen());
            m.put("ammonia", r.getAmmonia());
            m.put("nitrite", r.getNitrite());
            m.put("transparency", r.getTransparency());
            m.put("testerName", r.getTesterName());
            m.put("level", r.getLevel());
            m.put("image", r.getImage());
            m.put("remark", r.getRemark());
            List<WaterWarning> ws = warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                    .eq(WaterWarning::getWaterId, r.getId()));
            m.put("warningId", ws.isEmpty() ? null : ws.get(0).getId());
            m.put("warningStatus", ws.isEmpty() ? null : ws.get(0).getStatus());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    /** 录入水质：保存 → 计算风险 → 回写等级 → 需要时生成预警 */
    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        BreedingBatch batch = batchId == null ? null : batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("请选择有效的养殖批次");
        }
        if ("CLOSED".equals(batch.getStatus())) {
            throw new BizException("批次已结束，不能继续录入水质");
        }
        WaterQuality w = new WaterQuality();
        w.setRecordNo("SZ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%03d", waterMapper.selectCount(null) % 999 + 1));
        w.setPondId(batch.getPondId());
        w.setBatchId(batchId);
        w.setCheckTime(body.get("checkTime") == null || String.valueOf(body.get("checkTime")).isBlank()
                ? LocalDateTime.now()
                : LocalDateTime.parse(String.valueOf(body.get("checkTime")).replace(' ', 'T')));
        w.setWaterTemp(dec(body.get("waterTemp")));
        w.setPh(dec(body.get("ph")));
        w.setDissolvedOxygen(dec(body.get("dissolvedOxygen")));
        w.setAmmonia(dec(body.get("ammonia")));
        w.setNitrite(dec(body.get("nitrite")));
        w.setTransparency(body.get("transparency") == null ? null : Integer.valueOf(String.valueOf(body.get("transparency"))));
        w.setTesterId(UserContext.userId());
        w.setTesterName(UserContext.name());
        w.setImage(str(body.get("image")));
        w.setRemark(str(body.get("remark")));
        w.setLevel("NORMAL");
        w.setCreateTime(LocalDateTime.now());
        waterMapper.insert(w);

        // 创新模块计算（同一输入 + 同一规则版本必然得到同一结果）
        Map<String, Object> risk = ruleService.evaluate(w.getId());
        String level = String.valueOf(risk.get("riskLevel"));
        w.setLevel("LOW".equals(level) ? "NORMAL" : "MID".equals(level) ? "WARN" : "DANGER");
        waterMapper.updateById(w);
        logService.record("WATER", w.getId(), w.getRecordNo(), null, w.getLevel(), "水质检测录入", null);

        Long warningId = null;
        if (!"NORMAL".equals(w.getLevel())) {
            warningId = createWarning(w, risk, batch);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", w.getId());
        out.put("recordNo", w.getRecordNo());
        out.put("level", w.getLevel());
        out.put("risk", risk);
        out.put("warningId", warningId);
        return out;
    }

    @Transactional
    public Long createWarning(WaterQuality w, Map<String, Object> risk, BreedingBatch batch) {
        // 同一批次同一时间点只保留一个未关闭预警，避免重复告警
        Long open = warningMapper.selectCount(new LambdaQueryWrapper<WaterWarning>()
                .eq(WaterWarning::getWaterId, w.getId()).ne(WaterWarning::getStatus, "CLOSED"));
        if (open != null && open > 0) {
            WaterWarning exist = warningMapper.selectOne(new LambdaQueryWrapper<WaterWarning>()
                    .eq(WaterWarning::getWaterId, w.getId()).last("limit 1"));
            return exist.getId();
        }
        String level = String.valueOf(risk.get("riskLevel"));
        WaterWarning wn = new WaterWarning();
        wn.setWarningNo("YJ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%03d", warningMapper.selectCount(null) % 999 + 1));
        wn.setPondId(w.getPondId());
        wn.setBatchId(w.getBatchId());
        wn.setWaterId(w.getId());
        wn.setWarnType(warnTypeOf(risk));
        wn.setLevel("HIGH".equals(level) ? "HIGH" : "MID");
        wn.setStatus("PENDING");
        wn.setContent("批次 " + (batch == null ? "-" : batch.getBatchNo()) + " 水质风险分层得分 "
                + risk.get("totalScore") + "（" + risk.get("levelText") + "）。" + risk.get("advice"));
        wn.setHandlerId(null);
        wn.setHandlerName(null);
        wn.setCreateTime(LocalDateTime.now());
        warningMapper.insert(wn);
        logService.record("WARNING", wn.getId(), wn.getWarningNo(), null, "PENDING", "超限生成预警", null);
        logService.notifyRole("TECH", "水质预警 " + wn.getWarningNo() + " 待处置", "WARNING", wn.getId(), wn.getWarningNo());
        return wn.getId();
    }

    private String warnTypeOf(Map<String, Object> risk) {
        Object items = risk.get("items");
        if (!(items instanceof List<?> list)) {
            return "MULTI";
        }
        Map<String, Integer> typeMap = Map.of("temp", 1, "ph", 2, "do", 3, "ammonia", 4, "nitrite", 5);
        String best = null;
        double bestScore = 0;
        for (Object o : list) {
            Map<?, ?> m = (Map<?, ?>) o;
            double s = ((Number) m.get("score")).doubleValue();
            if (s > bestScore) {
                bestScore = s;
                best = String.valueOf(m.get("key"));
            }
        }
        if (best == null) {
            return "MULTI";
        }
        return switch (best) {
            case "temp" -> "TEMP";
            case "ph" -> "PH";
            case "do" -> "DO";
            case "ammonia" -> "NH3";
            case "nitrite" -> "NO2";
            default -> "MULTI";
        };
    }

    public Map<String, Object> waterDetail(Long id) {
        WaterQuality w = waterMapper.selectById(id);
        if (w == null) {
            throw new BizException("水质记录不存在");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("record", w);
        out.put("pond", w.getPondId() == null ? null : pondMapper.selectById(w.getPondId()));
        out.put("batch", w.getBatchId() == null ? null : batchMapper.selectById(w.getBatchId()));
        out.put("scores", scoreMapper.selectList(new LambdaQueryWrapper<RiskScore>()
                .eq(RiskScore::getBizId, id).orderByDesc(RiskScore::getId)));
        out.put("warnings", warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                .eq(WaterWarning::getWaterId, id)));
        return out;
    }

    // ---------------- 预警闭环 ----------------

    public PageResult<Map<String, Object>> warnings(int page, int size, String status, String level,
                                                    String warnType, Long pondId, String tab) {
        LambdaQueryWrapper<WaterWarning> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(WaterWarning::getStatus, status);
        }
        if (level != null && !level.isBlank()) {
            w.eq(WaterWarning::getLevel, level);
        }
        if (warnType != null && !warnType.isBlank()) {
            w.eq(WaterWarning::getWarnType, warnType);
        }
        if (pondId != null) {
            w.eq(WaterWarning::getPondId, pondId);
        }
        if (tab != null && !tab.isBlank()) {
            switch (tab) {
                case "todo" -> w.eq(WaterWarning::getStatus, "PENDING");
                case "doing" -> w.in(WaterWarning::getStatus, "PROCESSING", "RETEST");
                case "done" -> w.eq(WaterWarning::getStatus, "CLOSED");
                default -> { }
            }
        }
        w.orderByAsc(WaterWarning::getStatus).orderByDesc(WaterWarning::getId);
        Page<WaterWarning> p = warningMapper.selectPage(new Page<>(page, size), w);
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, WaterQuality> waters = waterMapper.selectList(null).stream()
                .collect(Collectors.toMap(WaterQuality::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(wn -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wn.getId());
            m.put("warningNo", wn.getWarningNo());
            m.put("pondName", ponds.get(wn.getPondId()) == null ? "-" : ponds.get(wn.getPondId()).getPondName());
            m.put("batchNo", batches.get(wn.getBatchId()) == null ? "-" : batches.get(wn.getBatchId()).getBatchNo());
            m.put("waterId", wn.getWaterId());
            m.put("warnType", wn.getWarnType());
            m.put("level", wn.getLevel());
            m.put("status", wn.getStatus());
            m.put("content", wn.getContent());
            m.put("handlerName", wn.getHandlerName());
            m.put("createTime", wn.getCreateTime());
            m.put("closeTime", wn.getCloseTime());
            WaterQuality wq = waters.get(wn.getWaterId());
            m.put("checkTime", wq == null ? null : wq.getCheckTime());
            m.put("dissolvedOxygen", wq == null ? null : wq.getDissolvedOxygen());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    public Map<String, Object> warningDetail(Long id) {
        WaterWarning wn = warningMapper.selectById(id);
        if (wn == null) {
            throw new BizException("预警不存在");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("warning", wn);
        WaterQuality wq = wn.getWaterId() == null ? null : waterMapper.selectById(wn.getWaterId());
        out.put("water", wq);
        out.put("pond", wn.getPondId() == null ? null : pondMapper.selectById(wn.getPondId()));
        out.put("batch", wn.getBatchId() == null ? null : batchMapper.selectById(wn.getBatchId()));
        out.put("emergencies", emergencyMapper.selectList(new LambdaQueryWrapper<EmergencyRecord>()
                .eq(EmergencyRecord::getWarningId, id).orderByDesc(EmergencyRecord::getId)));
        out.put("timeline", logService.timeline("WARNING", id));
        if (wq != null) {
            out.put("scores", scoreMapper.selectList(new LambdaQueryWrapper<RiskScore>()
                    .eq(RiskScore::getBizId, wq.getId()).orderByDesc(RiskScore::getId)));
        }
        return out;
    }

    /** 处置：登记措施，状态 待处置 → 处置中 */
    @Transactional
    public void handle(Long id, String measure, String detail) {
        WaterWarning wn = warningMapper.selectById(id);
        if (wn == null) {
            throw new BizException("预警不存在");
        }
        if (!List.of("PENDING", "RETEST").contains(wn.getStatus())) {
            throw new BizException("当前状态不允许登记处置措施");
        }
        if (measure == null || measure.isBlank()) {
            throw new BizException("请选择处置措施");
        }
        EmergencyRecord e = new EmergencyRecord();
        e.setWarningId(id);
        e.setMeasure(measure);
        e.setDetail(detail);
        e.setOperator(UserContext.name());
        e.setActionTime(LocalDateTime.now());
        e.setCreateTime(LocalDateTime.now());
        emergencyMapper.insert(e);

        String from = wn.getStatus();
        wn.setStatus("PROCESSING");
        wn.setHandlerId(UserContext.userId());
        wn.setHandlerName(UserContext.name());
        warningMapper.updateById(wn);
        logService.record("WARNING", id, wn.getWarningNo(), from, "PROCESSING", "登记处置措施：" + measureText(measure), detail);
        logService.notifyRole("TECH", "预警 " + wn.getWarningNo() + " 已处置，等待复测", "WARNING", id, wn.getWarningNo());
    }

    /** 复测：登记复测结论，正常则关闭，异常则回到处置中 */
    @Transactional
    public String retest(Long id, Long retestWaterId, String result) {
        WaterWarning wn = warningMapper.selectById(id);
        if (wn == null) {
            throw new BizException("预警不存在");
        }
        if (!List.of("PROCESSING", "RETEST").contains(wn.getStatus())) {
            throw new BizException("当前状态不允许复测");
        }
        if (retestWaterId == null) {
            throw new BizException("请先录入复测水质记录");
        }
        WaterQuality rw = waterMapper.selectById(retestWaterId);
        if (rw == null) {
            throw new BizException("复测水质记录不存在");
        }
        if (!Objects.equals(rw.getBatchId(), wn.getBatchId())) {
            throw new BizException("复测记录与本预警不属于同一批次");
        }
        String from = wn.getStatus();
        wn.setStatus("CLOSED");
        wn.setCloseTime(LocalDateTime.now());
        warningMapper.updateById(wn);

        EmergencyRecord e = new EmergencyRecord();
        e.setWarningId(id);
        e.setMeasure("RETEST");
        e.setDetail(result);
        e.setOperator(UserContext.name());
        e.setActionTime(LocalDateTime.now());
        e.setRetestResult(result);
        e.setRetestTime(LocalDateTime.now());
        e.setRetestWaterId(retestWaterId);
        e.setCreateTime(LocalDateTime.now());
        emergencyMapper.insert(e);

        logService.record("WARNING", id, wn.getWarningNo(), from, "CLOSED", "复测确认关闭", result);
        logService.oper("水质预警", "复测关闭", wn.getWarningNo(), result);
        return "CLOSED";
    }

    @Transactional
    public void close(Long id, String reason) {
        WaterWarning wn = warningMapper.selectById(id);
        if (wn == null) {
            throw new BizException("预警不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("关闭预警必须填写不少于 5 个字的原因");
        }
        String from = wn.getStatus();
        wn.setStatus("CLOSED");
        wn.setCloseTime(LocalDateTime.now());
        warningMapper.updateById(wn);
        logService.record("WARNING", id, wn.getWarningNo(), from, "CLOSED", "人工关闭", reason);
    }

    /** 定时扫描：历史超限但未生成预警的记录补建预警（系统重启或规则调整后兜底） */
    @Transactional
    public int scanOverLimit() {
        List<WaterQuality> list = waterMapper.selectList(new LambdaQueryWrapper<WaterQuality>()
                .ne(WaterQuality::getLevel, "NORMAL").orderByDesc(WaterQuality::getId).last("limit 50"));
        int count = 0;
        for (WaterQuality w : list) {
            Long open = warningMapper.selectCount(new LambdaQueryWrapper<WaterWarning>()
                    .eq(WaterWarning::getWaterId, w.getId()).ne(WaterWarning::getStatus, "CLOSED"));
            if (open == null || open == 0) {
                Map<String, Object> risk = ruleService.evaluate(w.getId());
                createWarning(w, risk, w.getBatchId() == null ? null : batchMapper.selectById(w.getBatchId()));
                count++;
            }
        }
        return count;
    }

    public static String measureText(String m) {
        return switch (m == null ? "" : m) {
            case "AERATION" -> "开启增氧";
            case "WATER_CHANGE" -> "换水";
            case "BOTTOM" -> "改底";
            case "DISINFECT" -> "消毒";
            case "RETEST" -> "复测";
            default -> m;
        };
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return null;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
