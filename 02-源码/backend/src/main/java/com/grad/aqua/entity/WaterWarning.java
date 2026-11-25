package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阈值预警（表 water_warning）
 * 状态机：PENDING 待处置 → PROCESSING 处置中 → RETEST 待复测 → CLOSED 已关闭。
 */
@Data
@TableName("water_warning")
public class WaterWarning {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String warningNo;

    private Long pondId;

    private Long batchId;

    /** 触发该预警的水质记录 */
    private Long waterId;

    /** 预警类型：DO 溶氧 / PH / NH3 氨氮 / NO2 亚硝酸盐 / TEMP / MULTI 多参数 */
    private String warnType;

    /** 等级：LOW / MID / HIGH */
    private String level;

    private String status;

    private String content;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime createTime;

    private LocalDateTime closeTime;

    @TableLogic
    private Integer deleted;
}
