package com.grad.aqua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.aqua.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 用户 Mapper */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
