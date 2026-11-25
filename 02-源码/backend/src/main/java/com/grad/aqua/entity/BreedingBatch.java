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
 * 养殖批次（表 breeding_batch）——系统核心主数据。
 * 状态机：PREP 准备中 → BREEDING 养殖中 → HARVESTING 起捕中 → CLOSED 已结束。
 */
@Data
@TableName("breeding_batch")
public class BreedingBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次编号，后端生成，前端只读 */
    private String batchNo;

    private Long pondId;

    /** 养殖品种：草鱼 / 南美白对虾 / 鲫鱼 … */
    private String variety;

    /** 苗种来源 */
    private String seedSource;

    /** 投苗日期 */
    private LocalDate stockDate;

    /** 投苗数量（万尾） */
    private Integer stockQty;

    /** 平均规格（克/尾） */
    private BigDecimal avgWeight;

    /** 预计起捕日 */
    private LocalDate expectHarvestDate;

    /** 目标产量（公斤） */
    private BigDecimal targetOutput;

    /** 生长阶段：FRY 苗期 / JUVENILE 中期 / ADULT 成鱼 */
    private String growthStage;

    /** 批次状态 */
    private String status;

    /** 当前存塘量（公斤），由投苗、分塘、起捕共同推算 */
    private BigDecimal currentStock;

    /** 累计投喂量（公斤） */
    private BigDecimal feedTotal;

    private Long managerId;

    private String managerName;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
