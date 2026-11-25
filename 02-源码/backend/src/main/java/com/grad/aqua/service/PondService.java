package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.BreedingBatch;
import com.grad.aqua.entity.Pond;
import com.grad.aqua.mapper.BreedingBatchMapper;
import com.grad.aqua.mapper.PondMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 塘口档案：编号唯一、面积水深校验、被批次引用时禁止删除。
 */
@Service
public class PondService {

    private final PondMapper pondMapper;
    private final BreedingBatchMapper batchMapper;

    public PondService(PondMapper pondMapper, BreedingBatchMapper batchMapper) {
        this.pondMapper = pondMapper;
        this.batchMapper = batchMapper;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String keyword, String status, String waterSource) {
        LambdaQueryWrapper<Pond> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(Pond::getPondNo, keyword).or().like(Pond::getPondName, keyword)
                    .or().like(Pond::getManagerName, keyword));
        }
        if (status != null && !status.isBlank()) {
            w.eq(Pond::getStatus, status);
        }
        if (waterSource != null && !waterSource.isBlank()) {
            w.eq(Pond::getWaterSource, waterSource);
        }
        w.orderByAsc(Pond::getPondNo);
        Page<Pond> p = pondMapper.selectPage(new Page<>(page, size), w);

        // 每个塘口的在养批次数与存塘量，列表直接展示，避免前端二次请求
        List<BreedingBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<BreedingBatch>()
                .ne(BreedingBatch::getStatus, "CLOSED"));
        Map<Long, List<BreedingBatch>> byPond = batches.stream()
                .filter(b -> b.getPondId() != null)
                .collect(Collectors.groupingBy(BreedingBatch::getPondId));

        List<Map<String, Object>> list = p.getRecords().stream().map(pd -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pd.getId());
            m.put("pondNo", pd.getPondNo());
            m.put("pondName", pd.getPondName());
            m.put("area", pd.getArea());
            m.put("depth", pd.getDepth());
            m.put("waterSource", pd.getWaterSource());
            m.put("aeratorCount", pd.getAeratorCount());
            m.put("managerId", pd.getManagerId());
            m.put("managerName", pd.getManagerName());
            m.put("status", pd.getStatus());
            m.put("image", pd.getImage());
            m.put("remark", pd.getRemark());
            m.put("createTime", pd.getCreateTime());
            List<BreedingBatch> bs = byPond.getOrDefault(pd.getId(), List.of());
            m.put("batchCount", bs.size());
            m.put("stock", bs.stream().map(b -> b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            m.put("variety", bs.isEmpty() ? "-" : bs.get(0).getVariety());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    public List<Pond> options() {
        return pondMapper.selectList(new LambdaQueryWrapper<Pond>().orderByAsc(Pond::getPondNo));
    }

    public Pond detail(Long id) {
        Pond p = pondMapper.selectById(id);
        if (p == null) {
            throw new BizException("塘口不存在");
        }
        return p;
    }

    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Pond p = id == null ? new Pond() : pondMapper.selectById(id);
        if (p == null) {
            throw new BizException("塘口不存在");
        }
        String no = str(body.get("pondNo"));
        String name = str(body.get("pondName"));
        if (no == null || no.isBlank()) {
            throw new BizException("塘口编号不能为空");
        }
        no = no.trim();
        if (no.length() > 50) {
            throw new BizException("塘口编号不能超过 50 个字符");
        }
        if (name == null || name.isBlank()) {
            throw new BizException("塘口名称不能为空");
        }
        Long dup = pondMapper.selectCount(new LambdaQueryWrapper<Pond>()
                .eq(Pond::getPondNo, no).ne(id != null, Pond::getId, id));
        if (dup != null && dup > 0) {
            throw new BizException("塘口编号已存在：" + no);
        }
        BigDecimal area = dec(body.get("area"));
        BigDecimal depth = dec(body.get("depth"));
        if (area.signum() <= 0) {
            throw new BizException("面积必须大于 0");
        }
        if (depth.signum() <= 0) {
            throw new BizException("水深必须大于 0");
        }
        p.setPondNo(no);
        p.setPondName(name.trim());
        p.setArea(area);
        p.setDepth(depth);
        p.setWaterSource(str(body.get("waterSource")));
        p.setAeratorCount(body.get("aeratorCount") == null ? 0 : Integer.valueOf(String.valueOf(body.get("aeratorCount"))));
        p.setManagerId(body.get("managerId") == null ? UserContext.userId() : Long.valueOf(String.valueOf(body.get("managerId"))));
        p.setManagerName(str(body.get("managerName")) == null ? UserContext.name() : str(body.get("managerName")));
        p.setStatus(str(body.get("status")) == null ? "NORMAL" : str(body.get("status")));
        p.setImage(str(body.get("image")));
        p.setRemark(str(body.get("remark")));
        if (id == null) {
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            pondMapper.insert(p);
        } else {
            p.setUpdateTime(LocalDateTime.now());
            pondMapper.updateById(p);
        }
        return p.getId();
    }

    @Transactional
    public void remove(Long id) {
        long used = batchMapper.selectCount(new LambdaQueryWrapper<BreedingBatch>()
                .eq(BreedingBatch::getPondId, id).ne(BreedingBatch::getStatus, "CLOSED"));
        if (used > 0) {
            throw new BizException("该塘口仍有 " + used + " 个在养批次，不能删除");
        }
        pondMapper.deleteById(id);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static BigDecimal dec(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(o));
    }
}
