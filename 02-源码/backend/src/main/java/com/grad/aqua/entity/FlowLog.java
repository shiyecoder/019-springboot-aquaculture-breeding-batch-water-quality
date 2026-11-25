package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务时间轴（表 flow_log）：创建、提交、审核、处置、复测、关闭等事件不可修改。
 */
@Data
@TableName("flow_log")
public class FlowLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String fromStatus;

    private String toStatus;

    private String action;

    private String reason;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime createTime;
}
