package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.FlowLog;
import com.grad.aqua.entity.SysLog;
import com.grad.aqua.entity.SysMessage;
import com.grad.aqua.mapper.FlowLogMapper;
import com.grad.aqua.mapper.SysLogMapper;
import com.grad.aqua.mapper.SysMessageMapper;
import com.grad.aqua.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 留痕服务：业务时间轴 + 站内消息 + 操作日志。
 * 所有改变状态的操作都必须调用 record()，保证时间轴完整、不可由用户修改。
 */
@Service
public class LogService {

    private final FlowLogMapper flowLogMapper;
    private final SysMessageMapper messageMapper;
    private final SysLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final com.grad.aqua.mapper.SysRoleMapper roleMapper;

    public LogService(FlowLogMapper flowLogMapper, SysMessageMapper messageMapper,
                      SysLogMapper logMapper, SysUserMapper userMapper,
                      com.grad.aqua.mapper.SysRoleMapper roleMapper) {
        this.flowLogMapper = flowLogMapper;
        this.messageMapper = messageMapper;
        this.logMapper = logMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /** 业务时间轴：记录操作人、时间、前后状态与原因 */
    public void record(String bizType, Long bizId, String bizNo, String from, String to, String action, String reason) {
        FlowLog log = new FlowLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setBizNo(bizNo);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setAction(action);
        log.setReason(reason);
        log.setOperatorId(UserContext.userId());
        log.setOperatorName(UserContext.name());
        log.setCreateTime(LocalDateTime.now());
        flowLogMapper.insert(log);
    }

    public List<FlowLog> timeline(String bizType, Long bizId) {
        return flowLogMapper.selectList(new LambdaQueryWrapper<FlowLog>()
                .eq(FlowLog::getBizType, bizType).eq(FlowLog::getBizId, bizId)
                .orderByAsc(FlowLog::getId));
    }

    public void oper(String module, String action, String bizNo, String detail) {
        SysLog log = new SysLog();
        log.setUserId(UserContext.userId());
        log.setUserName(UserContext.name());
        log.setModule(module);
        log.setAction(action);
        log.setBizNo(bizNo);
        log.setDetail(detail);
        log.setIp("127.0.0.1");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 推送给指定角色（技术员/场长），消息带业务编号与业务主键，前端可直接跳详情 */
    public void notifyRole(String roleCode, String title, String bizType, Long bizId, String bizNo) {
        com.grad.aqua.entity.SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<com.grad.aqua.entity.SysRole>()
                        .eq(com.grad.aqua.entity.SysRole::getRoleCode, roleCode).last("limit 1"));
        if (role == null) {
            return;
        }
        userMapper.selectList(new LambdaQueryWrapper<com.grad.aqua.entity.SysUser>()
                        .eq(com.grad.aqua.entity.SysUser::getStatus, 1)
                        .eq(com.grad.aqua.entity.SysUser::getRoleId, role.getId()))
                .forEach(u -> notifyUser(u.getId(), title, bizType, bizId, bizNo));
    }

    public void notifyUser(Long userId, String title, String bizType, Long bizId, String bizNo) {
        SysMessage m = new SysMessage();
        m.setTitle(title);
        m.setContent(title);
        m.setBizType(bizType);
        m.setBizNo(bizNo);
        m.setBizId(bizId);
        m.setReceiverId(userId);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }
}
