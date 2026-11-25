package com.grad.aqua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.aqua.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 Mapper */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
