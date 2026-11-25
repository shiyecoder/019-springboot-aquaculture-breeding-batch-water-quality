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
 * 病害用药记录（表 disease_record）
 * 技术员审核药名、剂量和休药期；休药期内禁止创建起捕任务。
 */
@Data
@TableName("disease_record")
public class DiseaseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String diseaseNo;

    private Long batchId;

    private Long pondId;

    private String symptom;

    private Integer deathCount;

    /** 病害图片 */
    private String image;

    private LocalDateTime reportTime;

    private String reporter;

    /** 技术员诊断结论 */
    private String diagnosis;

    private String drugName;

    /** 剂量，如 0.2g/kg 饲料 */
    private String dose;

    /** 休药期天数 */
    private Integer withdrawalDays;

    /** 休药期截止日，起捕校验直接比对 */
    private LocalDate withdrawalEnd;

    /** PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回 / VOID 已作废 */
    private String reviewStatus;

    private String reviewer;

    private LocalDateTime reviewTime;

    private String reviewNote;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
