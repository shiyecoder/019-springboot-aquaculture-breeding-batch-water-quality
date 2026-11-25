package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.*;
import com.grad.aqua.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分塘与起捕。
 *
 * 校验规则（后端强制）：
 * 1) 起捕/分塘数量不得超过批次可用存量；
 * 2) 休药期内禁止创建起捕任务（除非用药记录已被场长作废）；
 * 3) 起捕完成扣减存量，存量归零时批次自动进入已结束。
 */
@Service
public class HarvestService {

    private final HarvestRecordMapper harvestMapper;
    private final TransferRecordMapper transferMapper;
    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final DiseaseService diseaseService;
    private final LogService logService;

    public HarvestService(HarvestRecordMapper harvestMapper, TransferRecordMapper transferMapper,
                          BreedingBatchMapper batchMapper, PondMapper pondMapper,
                          DiseaseService diseaseService, LogService logService) {
        this.harvestMapper = harvestMapper;
        this.transferMapper = transferMapper;
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.diseaseService = diseaseService;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> harvestPage(int page, int size, String status, Long batchId, String grade) {
        LambdaQueryWrapper<HarvestRecord> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(HarvestRecord::getStatus, status);
        }
        if (batchId != null) {
            w.eq(HarvestRecord::getBatchId, batchId);
        }
        if (grade != null && !grade.isBlank()) {
            w.eq(HarvestRecord::getGrade, grade);
        }
        w.orderByDesc(HarvestRecord::getId);
        Page<HarvestRecord> p = harvestMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            BreedingBatch b = batches.get(h.getBatchId());
            m.put("id", h.getId());
            m.put("harvestNo", h.getHarvestNo());
            m.put("batchId", h.getBatchId());
            m.put("batchNo", b == null ? "-" : b.getBatchNo());
            m.put("variety", b == null ? "-" : b.getVariety());
            m.put("pondName", ponds.get(h.getPondId()) == null ? "-" : ponds.get(h.getPondId()).getPondName());
            m.put("planDate", h.getPlanDate());
            m.put("actualDate", h.getActualDate());
            m.put("weight", h.getWeight());
            m.put("grade", h.getGrade());
            m.put("destination", h.getDestination());
            m.put("lossRate", h.getLossRate());
            m.put("estStock", h.getEstStock());
            m.put("status", h.getStatus());
            m.put("image", h.getImage());
            m.put("approver", h.getApprover());
            m.put("remark", h.getRemark());
            m.put("createTime", h.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    /** 创建起捕任务：存量与休药期双重校验 */
    @Transactional
    public Long createHarvest(Map<String, Object> body) {
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        BreedingBatch b = batchId == null ? null : batchMapper.selectById(batchId);
        if (b == null) {
            throw new BizException("请选择有效的养殖批次");
        }
        if (!List.of("BREEDING", "HARVESTING").contains(b.getStatus())) {
            throw new BizException("批次当前为「" + BatchService.statusText(b.getStatus()) + "」，不能创建起捕任务");
        }
        LocalDate planDate = body.get("planDate") == null || String.valueOf(body.get("planDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("planDate")).substring(0, 10));
        // 休药期校验：存在已通过审核且未到期的用药记录时禁止起捕
        DiseaseRecord wd = diseaseService.activeWithdrawal(batchId, planDate);
        if (wd != null) {
            throw new BizException("批次处于休药期（药品 " + wd.getDrugName() + "，截止 "
                    + wd.getWithdrawalEnd() + "），禁止创建起捕任务；如确需起捕，请场长先作废该用药记录并说明原因");
        }
        BigDecimal weight = dec(body.get("weight"));
        BigDecimal estStock = dec(body.get("estStock"));
        BigDecimal stock = b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock();
        if (weight.signum() <= 0) {
            throw new BizException("起捕重量必须大于 0");
        }
        if (estStock.signum() <= 0) {
            estStock = weight;
        }
        if (estStock.compareTo(stock) > 0) {
            throw new BizException("起捕估算存量 " + estStock + " kg 超过批次可用存量 " + stock + " kg");
        }
        HarvestRecord h = new HarvestRecord();
        h.setHarvestNo("QB" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%03d", harvestMapper.selectCount(null) % 999 + 1));
        h.setBatchId(batchId);
        h.setPondId(b.getPondId());
        h.setPlanDate(planDate);
        h.setWeight(weight);
        h.setEstStock(estStock);
        h.setGrade(str(body.get("grade")) == null ? "A" : str(body.get("grade")));
        h.setDestination(str(body.get("destination")));
        h.setLossRate(dec(body.get("lossRate")));
        h.setImage(str(body.get("image")));
        h.setRemark(str(body.get("remark")));
        h.setStatus("APPROVED");
        h.setApprover(UserContext.name());
        h.setCreateTime(LocalDateTime.now());
        harvestMapper.insert(h);

        if ("BREEDING".equals(b.getStatus())) {
            b.setStatus("HARVESTING");
            b.setUpdateTime(LocalDateTime.now());
            batchMapper.updateById(b);
            logService.record("BATCH", b.getId(), b.getBatchNo(), "BREEDING", "HARVESTING", "创建起捕任务", null);
        }
        logService.record("HARVEST", h.getId(), h.getHarvestNo(), null, "APPROVED", "创建起捕任务", h.getRemark());
        logService.notifyRole("MANAGER", "起捕任务 " + h.getHarvestNo() + " 已创建，待执行", "HARVEST", h.getId(), h.getHarvestNo());
        return h.getId();
    }

    /** 起捕完成：扣减存量，存量归零则批次结束 */
    @Transactional
    public HarvestRecord finish(Long id, Map<String, Object> body) {
        HarvestRecord h = harvestMapper.selectById(id);
        if (h == null) {
            throw new BizException("起捕记录不存在");
        }
        if ("DONE".equals(h.getStatus())) {
            throw new BizException("该起捕任务已完成，请勿重复提交");
        }
        BreedingBatch b = batchMapper.selectById(h.getBatchId());
        BigDecimal stock = b == null || b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock();
        BigDecimal weight = body.get("weight") == null ? h.getWeight() : dec(body.get("weight"));
        if (weight.compareTo(stock) > 0) {
            throw new BizException("实际起捕重量 " + weight + " kg 超过可用存量 " + stock + " kg");
        }
        h.setWeight(weight);
        h.setActualDate(body.get("actualDate") == null || String.valueOf(body.get("actualDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("actualDate")).substring(0, 10)));
        h.setGrade(str(body.get("grade")) == null ? h.getGrade() : str(body.get("grade")));
        h.setDestination(str(body.get("destination")) == null ? h.getDestination() : str(body.get("destination")));
        h.setLossRate(body.get("lossRate") == null ? h.getLossRate() : dec(body.get("lossRate")));
        h.setStatus("DONE");
        harvestMapper.updateById(h);

        if (b != null) {
            BigDecimal left = stock.subtract(weight);
            b.setCurrentStock(left.signum() < 0 ? BigDecimal.ZERO : left);
            b.setUpdateTime(LocalDateTime.now());
            if (b.getCurrentStock().signum() == 0) {
                b.setStatus("CLOSED");
            }
            batchMapper.updateById(b);
            logService.record("BATCH", b.getId(), b.getBatchNo(), "HARVESTING", b.getStatus(),
                    "起捕完成，存量扣减 " + weight + " kg", null);
        }
        logService.record("HARVEST", id, h.getHarvestNo(), "APPROVED", "DONE", "起捕完成", null);
        return h;
    }

    @Transactional
    public void voidHarvest(Long id, String reason) {
        HarvestRecord h = harvestMapper.selectById(id);
        if (h == null) {
            throw new BizException("起捕记录不存在");
        }
        if ("DONE".equals(h.getStatus())) {
            throw new BizException("已完成的起捕不能作废");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("作废必须填写不少于 5 个字的原因");
        }
        h.setStatus("VOID");
        h.setRemark(reason);
        harvestMapper.updateById(h);
        logService.record("HARVEST", id, h.getHarvestNo(), "APPROVED", "VOID", "作废起捕任务", reason);
    }

    // ---------------- 分塘 ----------------

    public PageResult<Map<String, Object>> transferPage(int page, int size, Long batchId) {
        LambdaQueryWrapper<TransferRecord> w = new LambdaQueryWrapper<TransferRecord>()
                .eq(batchId != null, TransferRecord::getBatchId, batchId)
                .orderByDesc(TransferRecord::getId);
        Page<TransferRecord> p = transferMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("transferNo", t.getTransferNo());
            m.put("batchId", t.getBatchId());
            m.put("batchNo", batches.get(t.getBatchId()) == null ? "-" : batches.get(t.getBatchId()).getBatchNo());
            m.put("fromPondName", ponds.get(t.getFromPondId()) == null ? "-" : ponds.get(t.getFromPondId()).getPondName());
            m.put("toPondName", ponds.get(t.getToPondId()) == null ? "-" : ponds.get(t.getToPondId()).getPondName());
            m.put("qty", t.getQty());
            m.put("transferDate", t.getTransferDate());
            m.put("operator", t.getOperator());
            m.put("status", t.getStatus());
            m.put("remark", t.getRemark());
            m.put("createTime", t.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long createTransfer(Map<String, Object> body) {
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        BreedingBatch b = batchId == null ? null : batchMapper.selectById(batchId);
        if (b == null) {
            throw new BizException("请选择有效的养殖批次");
        }
        if ("CLOSED".equals(b.getStatus())) {
            throw new BizException("批次已结束，不能分塘");
        }
        Long toPondId = body.get("toPondId") == null ? null : Long.valueOf(String.valueOf(body.get("toPondId")));
        if (toPondId == null || pondMapper.selectById(toPondId) == null) {
            throw new BizException("请选择目标塘口");
        }
        if (Objects.equals(toPondId, b.getPondId())) {
            throw new BizException("目标塘口不能与原塘口相同");
        }
        BigDecimal qty = dec(body.get("qty"));
        BigDecimal stock = b.getCurrentStock() == null ? BigDecimal.ZERO : b.getCurrentStock();
        if (qty.signum() <= 0) {
            throw new BizException("分塘数量必须大于 0");
        }
        if (qty.compareTo(stock) > 0) {
            throw new BizException("分塘数量 " + qty + " kg 超过批次可用存量 " + stock + " kg");
        }
        TransferRecord t = new TransferRecord();
        t.setTransferNo("FT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%03d", transferMapper.selectCount(null) % 999 + 1));
        t.setBatchId(batchId);
        t.setFromPondId(b.getPondId());
        t.setToPondId(toPondId);
        t.setQty(qty);
        t.setTransferDate(body.get("transferDate") == null || String.valueOf(body.get("transferDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("transferDate")).substring(0, 10)));
        t.setOperator(UserContext.name());
        t.setStatus("DONE");
        t.setRemark(str(body.get("remark")));
        t.setCreateTime(LocalDateTime.now());
        transferMapper.insert(t);
        logService.record("TRANSFER", t.getId(), t.getTransferNo(), null, "DONE", "分塘登记", t.getRemark());
        return t.getId();
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
