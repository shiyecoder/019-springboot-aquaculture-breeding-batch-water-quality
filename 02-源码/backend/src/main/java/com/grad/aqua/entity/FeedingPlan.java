package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投喂计划（表 feeding_plan）
 * 按日期 + 时段生成任务，执行人回填实际用量后进入 feeding_record。
 */
@Data
@TableName("feeding_plan")
public class FeedingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;

    private Long batchId;

    /** 饲料名称 */
    private String feedName;

    /** 标准投喂量（公斤） */
    private BigDecimal feedQty;

    private LocalDate planDate;

    /** 时段：AM 早 / NOON 午 / PM 晚 */
    private String period;

    private Long executorId;

    private String executorName;

    /** 状态：PENDING 待执行 / DONE 已执行 / OVERDUE 已逾期 */
    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
