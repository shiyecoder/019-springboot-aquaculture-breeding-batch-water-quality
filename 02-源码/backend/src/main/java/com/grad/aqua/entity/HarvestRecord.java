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
 * 起捕记录（表 harvest_record）
 * 起捕重量不得超过批次可用存量，休药期内禁止创建。
 */
@Data
@TableName("harvest_record")
public class HarvestRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String harvestNo;

    private Long batchId;

    private Long pondId;

    private LocalDate planDate;

    private LocalDate actualDate;

    /** 起捕重量（公斤） */
    private BigDecimal weight;

    /** 等级：A / B / C */
    private String grade;

    /** 去向：批发市场 / 加工厂 / 商超直供 */
    private String destination;

    /** 损耗率 % */
    private BigDecimal lossRate;

    /** 起捕前估算存量（公斤） */
    private BigDecimal estStock;

    /** APPROVED 已审批 / DONE 已完成 / VOID 已作废 */
    private String status;

    /** 起捕照片 */
    private String image;

    private String approver;

    private String remark;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
