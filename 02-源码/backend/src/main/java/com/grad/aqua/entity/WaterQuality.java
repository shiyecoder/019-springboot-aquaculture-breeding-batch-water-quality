package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 水质检测记录（表 water_quality）——核心过程单据。
 * 五个参数即创新模块的输入来源，阈值判定在 RuleService 中完成。
 */
@Data
@TableName("water_quality")
public class WaterQuality {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    private Long pondId;

    private Long batchId;

    /** 检测时间 */
    private LocalDateTime checkTime;

    /** 水温 ℃ */
    private BigDecimal waterTemp;

    private BigDecimal ph;

    /** 溶解氧 mg/L */
    private BigDecimal dissolvedOxygen;

    /** 氨氮 mg/L */
    private BigDecimal ammonia;

    /** 亚硝酸盐 mg/L */
    private BigDecimal nitrite;

    /** 透明度 cm */
    private Integer transparency;

    private Long testerId;

    private String testerName;

    /** 单次判定：NORMAL 正常 / WARN 超限 / DANGER 严重超限 */
    private String level;

    /** 仪表照片 */
    private String image;

    private String remark;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
