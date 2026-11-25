package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应急处置记录（表 emergency_record）
 * 增氧 / 换水 / 改底 / 消毒四类措施 + 复测结果，是预警闭环的证据。
 */
@Data
@TableName("emergency_record")
public class EmergencyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warningId;

    /** AERATION 增氧 / WATER_CHANGE 换水 / BOTTOM 改底 / DISINFECT 消毒 */
    private String measure;

    private String detail;

    private String operator;

    private LocalDateTime actionTime;

    /** 复测结论 */
    private String retestResult;

    private LocalDateTime retestTime;

    /** 复测引用的水质记录 */
    private Long retestWaterId;

    private LocalDateTime createTime;
}
