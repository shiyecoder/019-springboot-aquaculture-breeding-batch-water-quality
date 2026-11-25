package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风险分层计算留痕（表 risk_score）
 * 每次计算保存输入快照、规则版本、逐项得分与最终等级，保证结果可追溯、可复算。
 */
@Data
@TableName("risk_score")
public class RiskScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型，固定 WATER */
    private String bizType;

    private Long bizId;

    private String bizNo;

    private Integer ruleVersion;

    /** 输入快照 JSON */
    private String inputJson;

    /** 逐项得分 JSON */
    private String scoreJson;

    private BigDecimal totalScore;

    /** LOW / MID / HIGH */
    private String riskLevel;

    /** 规则建议 */
    private String advice;

    /** 人工修正后的等级 */
    private String manualLevel;

    /** 人工修正原因，不少于 5 个字 */
    private String manualReason;

    private String manualBy;

    private LocalDateTime manualTime;

    private LocalDateTime createTime;
}
