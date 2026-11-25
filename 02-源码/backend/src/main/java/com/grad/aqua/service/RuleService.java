package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.BreedingBatch;
import com.grad.aqua.entity.RiskRule;
import com.grad.aqua.entity.RiskScore;
import com.grad.aqua.entity.WaterQuality;
import com.grad.aqua.mapper.BreedingBatchMapper;
import com.grad.aqua.mapper.RiskRuleMapper;
import com.grad.aqua.mapper.RiskScoreMapper;
import com.grad.aqua.mapper.WaterQualityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 创新点——多参数水质风险分层。
 *
 * 计算思路（可解释、可复算，不依赖外部模型）：
 * 1) 五个参数各有适宜区间，按生长阶段从规则阈值表取；
 * 2) 单参数偏离度 = 超出区间的比例 × 该参数毒性放大系数，封顶 100；
 * 3) 总分 = Σ(单参数得分 × 权重) / 100，权重和为 100；
 * 4) 总分 <30 低风险、<60 中风险、≥60 高风险；
 * 5) 每次计算保存输入快照、规则版本、逐项得分，原始结果不可被人工覆盖。
 */
@Service
public class RuleService {

    private static final String DEFAULT_THRESHOLD = "{\n" +
            "  \"FRY\":      {\"temp\":[24,30],\"ph\":[6.8,8.2],\"do\":[5,12],\"ammonia\":[\"0\",\"0.15\"],\"nitrite\":[\"0\",\"0.08\"]},\n" +
            "  \"JUVENILE\": {\"temp\":[22,31],\"ph\":[6.6,8.4],\"do\":[4.5,12],\"ammonia\":[\"0\",\"0.2\"],\"nitrite\":[\"0\",\"0.1\"]},\n" +
            "  \"ADULT\":    {\"temp\":[18,32],\"ph\":[6.5,8.5],\"do\":[4,14],\"ammonia\":[\"0\",\"0.3\"],\"nitrite\":[\"0\",\"0.15\"]}\n" +
            "}";

    private final RiskRuleMapper ruleMapper;
    private final RiskScoreMapper scoreMapper;
    private final WaterQualityMapper waterMapper;
    private final BreedingBatchMapper batchMapper;
    private final ObjectMapper json = new ObjectMapper();

    public RuleService(RiskRuleMapper ruleMapper, RiskScoreMapper scoreMapper,
                       WaterQualityMapper waterMapper, BreedingBatchMapper batchMapper) {
        this.ruleMapper = ruleMapper;
        this.scoreMapper = scoreMapper;
        this.waterMapper = waterMapper;
        this.batchMapper = batchMapper;
    }

    /** 当前生效规则，缺失时自动落一条 v1 默认规则，保证系统开箱可用 */
    public RiskRule activeRule() {
        RiskRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEnabled, 1).orderByDesc(RiskRule::getVersion).last("limit 1"));
        if (rule == null) {
            rule = new RiskRule();
            rule.setRuleName("多参数水质风险分层默认规则");
            rule.setVersion(1);
            rule.setWeightTemp(new BigDecimal("20"));
            rule.setWeightPh(new BigDecimal("15"));
            rule.setWeightDo(new BigDecimal("25"));
            rule.setWeightAmmonia(new BigDecimal("20"));
            rule.setWeightNitrite(new BigDecimal("15"));
            rule.setWeightTrend(new BigDecimal("5"));
            rule.setThresholdJson(DEFAULT_THRESHOLD);
            rule.setEnabled(1);
            rule.setEffectiveTime(LocalDateTime.now());
            rule.setCreateBy("system");
            rule.setCreateTime(LocalDateTime.now());
            ruleMapper.insert(rule);
        }
        return rule;
    }

    public List<RiskRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<RiskRule>().orderByDesc(RiskRule::getVersion));
    }

    /** 规则配置：权重和必须 100，修改即生成新版本，旧版本停用但保留 */
    @Transactional
    public RiskRule saveRule(Map<String, Object> body) {
        BigDecimal sum = BigDecimal.ZERO;
        String[] keys = {"weightTemp", "weightPh", "weightDo", "weightAmmonia", "weightNitrite", "weightTrend"};
        BigDecimal[] vals = new BigDecimal[keys.length];
        for (int i = 0; i < keys.length; i++) {
            vals[i] = dec(body.get(keys[i]));
            if (vals[i].signum() < 0) {
                throw new BizException("权重不能为负数");
            }
            sum = sum.add(vals[i]);
        }
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException("六项权重合计必须等于 100%，当前为 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        RiskRule old = activeRule();
        ruleMapper.update(null, new LambdaUpdateWrapper<RiskRule>()
                .eq(RiskRule::getEnabled, 1).set(RiskRule::getEnabled, 0));

        RiskRule rule = new RiskRule();
        rule.setRuleName(body.get("ruleName") == null ? "多参数水质风险分层规则" : String.valueOf(body.get("ruleName")));
        rule.setVersion(old.getVersion() + 1);
        rule.setWeightTemp(vals[0]);
        rule.setWeightPh(vals[1]);
        rule.setWeightDo(vals[2]);
        rule.setWeightAmmonia(vals[3]);
        rule.setWeightNitrite(vals[4]);
        rule.setWeightTrend(vals[5]);
        rule.setThresholdJson(body.get("thresholdJson") == null ? old.getThresholdJson() : String.valueOf(body.get("thresholdJson")));
        rule.setEnabled(1);
        rule.setEffectiveTime(LocalDateTime.now());
        rule.setCreateBy(UserContext.name());
        rule.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        return rule;
    }

    /**
     * 对一条水质记录执行风险分层计算，并保存留痕。
     * @param waterId 水质记录主键
     */
    @Transactional
    public Map<String, Object> evaluate(Long waterId) {
        WaterQuality w = waterMapper.selectById(waterId);
        if (w == null) {
            throw new BizException("水质记录不存在，无法计算");
        }
        BreedingBatch batch = w.getBatchId() == null ? null : batchMapper.selectById(w.getBatchId());
        RiskRule rule = activeRule();
        String stage = batch == null || batch.getGrowthStage() == null ? "ADULT" : batch.getGrowthStage();
        JsonNode th = readThreshold(rule.getThresholdJson(), stage);

        List<Map<String, Object>> items = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double weighted = 0;

        weighted += item(items, missing, "temp", "水温", w.getWaterTemp(), th.get("temp"), 200,
                rule.getWeightTemp(), "℃");
        weighted += item(items, missing, "ph", "pH", w.getPh(), th.get("ph"), 250,
                rule.getWeightPh(), "");
        weighted += item(items, missing, "do", "溶解氧", w.getDissolvedOxygen(), th.get("do"), 300,
                rule.getWeightDo(), "mg/L");
        weighted += item(items, missing, "ammonia", "氨氮", w.getAmmonia(), th.get("ammonia"), 400,
                rule.getWeightAmmonia(), "mg/L");
        weighted += item(items, missing, "nitrite", "亚硝酸盐", w.getNitrite(), th.get("nitrite"), 400,
                rule.getWeightNitrite(), "mg/L");

        // 连续异常次数：最近 5 次同批次记录中等级非 NORMAL 的条数
        int abnormal = 0;
        if (w.getBatchId() != null) {
            List<WaterQuality> recent = waterMapper.selectList(new LambdaQueryWrapper<WaterQuality>()
                    .eq(WaterQuality::getBatchId, w.getBatchId())
                    .le(WaterQuality::getCheckTime, w.getCheckTime())
                    .orderByDesc(WaterQuality::getCheckTime).last("limit 5"));
            for (WaterQuality r : recent) {
                if (r.getLevel() != null && !"NORMAL".equals(r.getLevel())) {
                    abnormal++;
                }
            }
        }
        double trendScore = Math.min(100, abnormal * 25.0);
        Map<String, Object> trendItem = new LinkedHashMap<>();
        trendItem.put("key", "trend");
        trendItem.put("label", "连续异常次数");
        trendItem.put("value", abnormal + " 次");
        trendItem.put("range", "0-1 次");
        trendItem.put("score", round(trendScore));
        trendItem.put("weight", rule.getWeightTrend());
        trendItem.put("contribution", round(trendScore * rule.getWeightTrend().doubleValue() / 100));
        trendItem.put("status", trendScore == 0 ? "NORMAL" : trendScore < 50 ? "WARN" : "DANGER");
        items.add(trendItem);
        weighted += trendScore * rule.getWeightTrend().doubleValue() / 100;

        BigDecimal total = BigDecimal.valueOf(round(weighted));
        String level = total.doubleValue() < 30 ? "LOW" : total.doubleValue() < 60 ? "MID" : "HIGH";

        List<Map<String, Object>> unmet = new ArrayList<>();
        List<String> advice = new ArrayList<>();
        items.stream()
                .filter(i -> ((Number) i.get("score")).doubleValue() > 0)
                .sorted((a, b) -> Double.compare(((Number) b.get("score")).doubleValue(), ((Number) a.get("score")).doubleValue()))
                .forEach(i -> {
                    unmet.add(Map.of("label", i.get("label"), "value", i.get("value"), "range", i.get("range")));
                    advice.add(adviceOf(String.valueOf(i.get("key"))));
                });
        if (advice.isEmpty()) {
            advice.add("各项指标均在适宜区间内，按常规频次（每日 1 次）继续监测即可。");
        }
        if (!missing.isEmpty()) {
            advice.add("以下指标缺失未参与计算，请尽快补录：" + String.join("、", missing) + "。");
        }

        RiskScore score = new RiskScore();
        score.setBizType("WATER");
        score.setBizId(w.getId());
        score.setBizNo(w.getRecordNo());
        score.setRuleVersion(rule.getVersion());
        score.setInputJson(writeJson(Map.of(
                "waterTemp", nz(w.getWaterTemp()), "ph", nz(w.getPh()), "do", nz(w.getDissolvedOxygen()),
                "ammonia", nz(w.getAmmonia()), "nitrite", nz(w.getNitrite()),
                "abnormalCount", abnormal, "growthStage", stage)));
        score.setScoreJson(writeJson(items));
        score.setTotalScore(total);
        score.setRiskLevel(level);
        score.setAdvice(String.join(" ", advice));
        score.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(score);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scoreId", score.getId());
        out.put("recordNo", w.getRecordNo());
        out.put("ruleVersion", rule.getVersion());
        out.put("growthStage", stage);
        out.put("totalScore", total);
        out.put("riskLevel", level);
        out.put("levelText", levelText(level));
        out.put("items", items);
        out.put("unmet", unmet);
        out.put("missing", missing);
        out.put("advice", String.join(" ", advice));
        out.put("manualLevel", score.getManualLevel());
        out.put("finalLevel", score.getManualLevel() == null ? level : score.getManualLevel());
        out.put("createTime", score.getCreateTime());
        out.put("reproducible", true);
        return out;
    }

    public List<RiskScore> scoreHistory(Long waterId) {
        return scoreMapper.selectList(new LambdaQueryWrapper<RiskScore>()
                .eq(RiskScore::getBizId, waterId).orderByDesc(RiskScore::getId));
    }

    /** 人工修正：只写 manualLevel/manualReason，原始 totalScore 与 riskLevel 不动 */
    @Transactional
    public RiskScore manual(Long id, String level, String reason) {
        RiskScore s = scoreMapper.selectById(id);
        if (s == null) {
            throw new BizException("计算记录不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工修正必须填写不少于 5 个字的原因");
        }
        if (!List.of("LOW", "MID", "HIGH").contains(level)) {
            throw new BizException("修正等级不合法");
        }
        s.setManualLevel(level);
        s.setManualReason(reason.trim());
        s.setManualBy(UserContext.name());
        s.setManualTime(LocalDateTime.now());
        scoreMapper.updateById(s);
        return s;
    }

    // ---------- 内部工具 ----------

    private double item(List<Map<String, Object>> items, List<String> missing, String key, String label,
                        BigDecimal value, JsonNode range, double mult, BigDecimal weight, String unit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("weight", weight);
        if (value == null || range == null || !range.isArray()) {
            m.put("value", "缺失");
            m.put("range", range == null ? "-" : range.get(0).asText() + "~" + range.get(1).asText());
            m.put("score", 0);
            m.put("contribution", 0);
            m.put("status", "MISSING");
            items.add(m);
            missing.add(label);
            return 0;
        }
        double v = value.doubleValue();
        double lo = range.get(0).asDouble();
        double hi = range.get(1).asDouble();
        double deviation = 0;
        if (v > hi) {
            deviation = (v - hi) / Math.max(hi, 0.0001);
        } else if (v < lo) {
            deviation = (lo - v) / Math.max(lo, 0.0001);
        }
        double score = Math.min(100, deviation * mult);
        m.put("value", v + unit);
        m.put("range", trimNum(range.get(0).asText()) + "~" + trimNum(range.get(1).asText()) + unit);
        m.put("score", round(score));
        m.put("contribution", round(score * weight.doubleValue() / 100));
        m.put("status", score == 0 ? "NORMAL" : score < 50 ? "WARN" : "DANGER");
        m.put("deviation", round(deviation * 100));
        items.add(m);
        return score * weight.doubleValue() / 100;
    }

    private String adviceOf(String key) {
        return switch (key) {
            case "temp" -> "水温偏离适宜区间，建议加深水位或调整遮阳，并记录昼夜温差。";
            case "ph" -> "pH 异常：偏高可换水 1/3 并停用生石灰，偏低可泼洒生石灰水后复测。";
            case "do" -> "溶解氧不足，立即开启全部增氧机，2 小时内复测；持续低于 3 mg/L 应减料并换水。";
            case "ammonia" -> "氨氮偏高，先换水 1/3、投放改底剂，次日检测氨氮与亚硝酸盐。";
            case "nitrite" -> "亚硝酸盐偏高，加开增氧、控制投喂量，并补充碳源促进转化。";
            case "trend" -> "近期连续异常，建议提高检测频次至每日 2 次并复核增氧设备运行情况。";
            default -> "按处置建议执行并复测。";
        };
    }

    private JsonNode readThreshold(String json, String stage) {
        try {
            JsonNode root = this.json.readTree(json);
            JsonNode node = root.get(stage);
            return node == null ? root.get("ADULT") : node;
        } catch (Exception e) {
            throw new BizException("规则阈值配置格式错误，请检查规则配置页");
        }
    }

    public static String levelText(String level) {
        return switch (level == null ? "" : level) {
            case "LOW" -> "低风险";
            case "MID" -> "中风险";
            case "HIGH" -> "高风险";
            default -> level;
        };
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String trimNum(String s) {
        try {
            return new BigDecimal(s).stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return s;
        }
    }

    private BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            throw new BizException("权重必须为数字");
        }
    }

    private static Object nz(Object o) {
        return o == null ? "" : o;
    }

    private String writeJson(Object o) {
        try {
            return this.json.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
