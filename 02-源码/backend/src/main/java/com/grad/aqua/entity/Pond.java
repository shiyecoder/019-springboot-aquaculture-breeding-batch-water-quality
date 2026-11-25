package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 塘口档案（表 pond）
 * 批次、水质、预警都挂在塘口下，实现按塘口隔离数据。
 */
@Data
@TableName("pond")
public class Pond {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 塘口编号，如 TK-01 */
    private String pondNo;

    /** 塘口名称 */
    private String pondName;

    /** 面积（亩） */
    private BigDecimal area;

    /** 平均水深（米） */
    private BigDecimal depth;

    /** 水源：河道 / 地下水 / 水库 */
    private String waterSource;

    /** 增氧设备台数 */
    private Integer aeratorCount;

    /** 负责人 */
    private Long managerId;

    private String managerName;

    /** 状态：NORMAL 正常 / MAINTENANCE 维护 / IDLE 空闲 */
    private String status;

    /** 现场照片 */
    private String image;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
