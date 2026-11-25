package com.grad.aqua.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件元数据（表 attachment）：只存相对路径，不存外部 URL。
 */
@Data
@TableName("attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private Long bizId;

    private String filePath;

    private String fileName;

    private Long fileSize;

    private Long uploaderId;

    private LocalDateTime createTime;
}
