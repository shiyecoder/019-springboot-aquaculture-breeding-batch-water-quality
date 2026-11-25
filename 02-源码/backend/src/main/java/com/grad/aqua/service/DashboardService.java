package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.aqua.entity.*;
import com.grad.aqua.mapper.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计看板：6 个核心指标 + 6 个图表，全部在后端聚合，前端只渲染。
 * 统计口径与明细列表一致，图表点击可带筛选条件回到列表。
 */
@Service
public class DashboardService {

    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final WaterQualityMapper waterMapper;
    private final WaterWarningMapper warningMapper;
    private final FeedingRecordMapper feedingMapper;
    private final DiseaseRecordMapper diseaseMapper;
    private final HarvestRecordMapper harvestMapper;
    private final RiskScoreMapper scoreMapper;
    private final EmergencyRecordMapper emergencyMapper;

    public DashboardService(BreedingBatchMapper batchMapper, PondMapper pondMapper, WaterQualityMapper waterMapper,
                            WaterWarningMapper warningMapper, FeedingRecordMapper feedingMapper,
                            DiseaseRecordMapper diseaseMapper, HarvestRecordMapper harvestMapper,
                            RiskScoreMapper scoreMapper, EmergencyRecordMapper emergencyMapper) {
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.waterMapper = waterMapper;
        this.warningMapper = warningMapper;
        this.feedingMapper = feedingMapper;
        this.diseaseMapper = diseaseMapper;
        this.harvestMapper = harvestMapper;
        this.scoreMapper = scoreMapper;
        this.emergencyMapper = emergencyMapper;
    }

    public Map<String, Object> overview(int days, Long pondId) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<BreedingBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<BreedingBatch>()
                .eq(pondId != null, BreedingBatch::getPondId, pondId));
        List<WaterQuality> waters = waterMapper.selectList(new LambdaQueryWrapper<WaterQuality>()
                .ge(WaterQuality::getCheckTime, from)
                .eq(pondId != null, WaterQuality::getPondId, pondId)
                .orderByAsc(WaterQuality::getCheckTime));
        List<WaterWarning> warnings = warningMapper.selectList(new LambdaQueryWrapper<WaterWarning>()
                .eq(pondId != null, WaterWarning::getPondId, pondId));
        List<Pond> ponds = pondMapper.selectList(null);
        List<HarvestRecord> harvests = harvestMapper.selectList(new LambdaQueryWrapper<HarvestRecord>()
                .eq(HarvestRecord::getStatus, "DONE"));
        List<DiseaseRecord> diseases = diseaseMapper.selectList(null);
        List<FeedingRecord> feeds = feedingMapper.selectList(null);
        List<RiskScore> scores = scoreMapper.selectList(new LambdaQueryWrapper<RiskScore>()
                .ge(RiskScore::getCreateTime, from));

        Map<String, Object> out = new LinkedHashMap<>();

        // ---------- 指标卡 ----------
        long batchCount = batches.stream().filter(b -> !"CLOSED".equals(b.getStatus())).count();
        long qualified = waters.stream().filter(w -> "NORMAL".equals(w.getLevel())).count();
        double qualifiedRate = waters.isEmpty() ? 100 : round(qualified * 100.0 / waters.size(), 1);
        long highOpen = warnings.stream().filter(w -> "HIGH".equals(w.getLevel()) && !"CLOSED".equals(w.getStatus())).count();
        double avgHours = warnings.stream().filter(w -> w.getCloseTime() != null)
                .mapToDouble(w -> java.time.Duration.between(w.getCreateTime(), w.getCloseTime()).toMinutes() / 60.0)
                .average().orElse(0);
        int deaths = diseases.stream().mapToInt(d -> d.getDeathCount() == null ? 0 : d.getDeathCount()).sum();
        int stockQty = batches.stream().mapToInt(b -> b.getStockQty() == null ? 0 : b.getStockQty()).sum();
        double deathRate = stockQty == 0 ? 0 : round(deaths * 100.0 / (stockQty * 10000) * 100, 3);

        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(card("养殖批次数", batchCount, "个", "在养批次"));
        cards.add(card("水质合格率", qualifiedRate, "%", "近 " + days + " 天 " + waters.size() + " 条检测"));
        cards.add(card("高风险预警", highOpen, "个", "未关闭的高风险"));
        cards.add(card("平均处置时长", round(avgHours, 1), "小时", "已关闭预警"));
        cards.add(card("死亡率", deathRate, "%", "累计死亡数 / 投苗量"));
        cards.add(card("存塘量", batches.stream().map(b -> b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP), "kg", "全部在养批次"));
        out.put("cards", cards);

        // ---------- 图 1：水质趋势（合格率 + 溶解氧均值） ----------
        Map<LocalDate, List<WaterQuality>> byDay = waters.stream()
                .collect(Collectors.groupingBy(w -> w.getCheckTime().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<String> trendLabels = new ArrayList<>();
        List<Double> trendRate = new ArrayList<>();
        List<Double> trendDo = new ArrayList<>();
        for (Map.Entry<LocalDate, List<WaterQuality>> e : byDay.entrySet()) {
            trendLabels.add(e.getKey().format(DateTimeFormatter.ofPattern("MM-dd")));
            long ok = e.getValue().stream().filter(w -> "NORMAL".equals(w.getLevel())).count();
            trendRate.add(round(ok * 100.0 / e.getValue().size(), 1));
            trendDo.add(round(e.getValue().stream()
                    .filter(w -> w.getDissolvedOxygen() != null)
                    .mapToDouble(w -> w.getDissolvedOxygen().doubleValue()).average().orElse(0), 2));
        }
        out.put("trend", Map.of("labels", trendLabels, "qualified", trendRate, "dissolvedOxygen", trendDo));

        // ---------- 图 2：预警类型分布 ----------
        Map<String, Long> typeCount = warnings.stream()
                .collect(Collectors.groupingBy(w -> w.getWarnType() == null ? "MULTI" : w.getWarnType(), Collectors.counting()));
        out.put("warnType", typeCount.entrySet().stream()
                .map(e -> Map.of("name", typeName(e.getKey()), "value", e.getValue())).collect(Collectors.toList()));

        // ---------- 图 3：塘口存塘量 ----------
        Map<Long, BigDecimal> stockByPond = batches.stream().filter(b -> b.getPondId() != null)
                .collect(Collectors.groupingBy(BreedingBatch::getPondId,
                        Collectors.reducing(BigDecimal.ZERO, b -> b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock(), BigDecimal::add)));
        out.put("stock", ponds.stream()
                .sorted((a, b) -> stockByPond.getOrDefault(b.getId(), BigDecimal.ZERO)
                        .compareTo(stockByPond.getOrDefault(a.getId(), BigDecimal.ZERO)))
                .limit(8)
                .map(p -> Map.of("name", p.getPondName(), "value", stockByPond.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList()));

        // ---------- 图 4：预警处置人排行 ----------
        Map<String, Long> handler = warnings.stream().filter(w -> w.getHandlerName() != null)
                .collect(Collectors.groupingBy(WaterWarning::getHandlerName, Collectors.counting()));
        out.put("handler", handler.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6).map(e -> Map.of("name", e.getKey(), "value", e.getValue())).collect(Collectors.toList()));

        // ---------- 图 5：处置时效对比（按预警类型平均小时） ----------
        Map<String, List<WaterWarning>> closedByType = warnings.stream().filter(w -> w.getCloseTime() != null)
                .collect(Collectors.groupingBy(w -> typeName(w.getWarnType() == null ? "MULTI" : w.getWarnType())));
        List<Map<String, Object>> efficiency = new ArrayList<>();
        closedByType.forEach((k, v) -> efficiency.add(Map.of("name", k,
                "value", round(v.stream().mapToDouble(w -> java.time.Duration.between(w.getCreateTime(), w.getCloseTime()).toMinutes() / 60.0).average().orElse(0), 1))));
        out.put("efficiency", efficiency);

        // ---------- 图 6：风险分层结果分布 ----------
        Map<String, Long> riskDist = scores.stream().collect(Collectors.groupingBy(RiskScore::getRiskLevel, Collectors.counting()));
        out.put("riskDist", List.of(
                Map.of("name", "低风险", "value", riskDist.getOrDefault("LOW", 0L)),
                Map.of("name", "中风险", "value", riskDist.getOrDefault("MID", 0L)),
                Map.of("name", "高风险", "value", riskDist.getOrDefault("HIGH", 0L))));

        // ---------- 图 7：饲料系数（累计投喂 / 存塘） ----------
        out.put("feedCoefficient", batches.stream().filter(b -> b.getCurrentStock() != null && b.getCurrentStock().signum() > 0)
                .limit(7)
                .map(b -> Map.of("name", b.getBatchNo(),
                        "value", b.getFeedTotal() == null ? BigDecimal.ZERO
                                : b.getFeedTotal().divide(b.getCurrentStock(), 2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList()));

        // 侧栏：进行中的预警、今日待执行投喂任务
        out.put("openWarnings", warnings.stream().filter(w -> !"CLOSED".equals(w.getStatus()))
                .sorted(Comparator.comparing(WaterWarning::getId).reversed()).limit(5)
                .map(w -> Map.of("id", w.getId(), "warningNo", w.getWarningNo(), "level", w.getLevel(),
                        "content", w.getContent() == null ? "" : w.getContent(), "status", w.getStatus()))
                .collect(Collectors.toList()));
        out.put("acceptance", Map.of(
                "manualRate", scores.isEmpty() ? 0 : round(scores.stream().filter(s -> s.getManualLevel() != null).count() * 100.0 / scores.size(), 1),
                "adoptRate", scores.isEmpty() ? 0 : round(scores.stream().filter(s -> s.getManualLevel() == null).count() * 100.0 / scores.size(), 1),
                "scoreCount", scores.size(),
                "harvestWeight", harvests.stream().map(h -> h.getWeight() == null ? BigDecimal.ZERO : h.getWeight())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        out.put("lastUpdate", LocalDateTime.now());
        return out;
    }

    private Map<String, Object> card(String name, Object value, String unit, String tip) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("unit", unit);
        m.put("tip", tip);
        return m;
    }

    public static String typeName(String t) {
        return switch (t == null ? "" : t) {
            case "DO" -> "溶解氧";
            case "PH" -> "pH";
            case "NH3" -> "氨氮";
            case "NO2" -> "亚硝酸盐";
            case "TEMP" -> "水温";
            case "MULTI" -> "多参数";
            default -> t;
        };
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
