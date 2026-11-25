package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分塘记录（表 transfer_record）
 * 分塘数量不得超过批次可用存量，转出与转入同事务更新存量。
 */
@Data
@TableName("transfer_record")
public class TransferRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transferNo;

    private Long batchId;

    private Long fromPondId;

    private Long toPondId;

    /** 转移数量（公斤） */
    private BigDecimal qty;

    private LocalDate transferDate;

    private String operator;

    private String status;

    private String remark;

    private LocalDateTime createTime;
}
