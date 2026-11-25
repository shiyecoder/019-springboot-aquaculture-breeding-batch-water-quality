package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创新点规则配置（表 risk_rule）
 * 六个因子权重合计必须为 100%，修改后生成新版本，旧版本保留以便追溯。
 */
@Data
@TableName("risk_rule")
public class RiskRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    /** 版本号，从 1 递增 */
    private Integer version;

    /** 水温偏差权重 */
    private BigDecimal weightTemp;

    /** pH 偏差权重 */
    private BigDecimal weightPh;

    /** 溶解氧权重 */
    private BigDecimal weightDo;

    /** 氨氮权重 */
    private BigDecimal weightAmmonia;

    /** 亚硝酸盐权重 */
    private BigDecimal weightNitrite;

    /** 连续异常次数权重 */
    private BigDecimal weightTrend;

    /** 各参数适宜区间 JSON，按生长阶段区分 */
    private String thresholdJson;

    private Integer enabled;

    private LocalDateTime effectiveTime;

    private String createBy;

    private LocalDateTime createTime;
}
