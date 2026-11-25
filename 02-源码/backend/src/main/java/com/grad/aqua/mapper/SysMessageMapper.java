package com.grad.aqua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.aqua.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息 Mapper */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
