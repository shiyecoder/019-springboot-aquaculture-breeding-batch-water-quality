package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典（表 sys_dict）
 *
 * 说明：物理表字段为 dict_type / dict_code / dict_label / sort，
 * 实体保持 dictValue 语义（字典值），通过 @TableField 映射到 dict_code。
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型 */
    private String dictType;

    /** 标签 */
    private String dictLabel;

    /** 值（对应列 dict_code） */
    @TableField("dict_code")
    private String dictValue;

    /** 排序 */
    private Integer sort;
}
