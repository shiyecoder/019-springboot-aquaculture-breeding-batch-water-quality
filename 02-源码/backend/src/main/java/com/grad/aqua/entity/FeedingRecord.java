package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投喂执行记录（表 feeding_record）
 * 实际用量不得超过领料量；日投喂超标需技术员复核（checkFlag=1）。
 */
@Data
@TableName("feeding_record")
public class FeedingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long batchId;

    /** 领料量（公斤） */
    private BigDecimal feedQty;

    /** 实际用量（公斤） */
    private BigDecimal actualQty;

    private LocalDateTime executeTime;

    private String operator;

    /** 1 表示超标待技术员复核 */
    private Integer checkFlag;

    private String checkNote;

    private String remark;

    private LocalDateTime createTime;
}
