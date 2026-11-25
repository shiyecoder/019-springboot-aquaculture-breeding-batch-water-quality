package com.grad.aqua.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.aqua.common.BizException;
import com.grad.aqua.common.PageResult;
import com.grad.aqua.common.UserContext;
import com.grad.aqua.entity.BreedingBatch;
import com.grad.aqua.entity.DiseaseRecord;
import com.grad.aqua.entity.Pond;
import com.grad.aqua.mapper.BreedingBatchMapper;
import com.grad.aqua.mapper.DiseaseRecordMapper;
import com.grad.aqua.mapper.PondMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 病害上报与用药审核。
 * 技术员审核通过后计算休药期截止日，休药期内的批次禁止创建起捕任务（见 HarvestService）。
 */
@Service
public class DiseaseService {

    private final DiseaseRecordMapper diseaseMapper;
    private final BreedingBatchMapper batchMapper;
    private final PondMapper pondMapper;
    private final LogService logService;

    public DiseaseService(DiseaseRecordMapper diseaseMapper, BreedingBatchMapper batchMapper,
                          PondMapper pondMapper, LogService logService) {
        this.diseaseMapper = diseaseMapper;
        this.batchMapper = batchMapper;
        this.pondMapper = pondMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String status, Long batchId, String keyword) {
        LambdaQueryWrapper<DiseaseRecord> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(DiseaseRecord::getReviewStatus, status);
        }
        if (batchId != null) {
            w.eq(DiseaseRecord::getBatchId, batchId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(DiseaseRecord::getDiseaseNo, keyword).or().like(DiseaseRecord::getSymptom, keyword)
                    .or().like(DiseaseRecord::getDrugName, keyword));
        }
        w.orderByDesc(DiseaseRecord::getId);
        Page<DiseaseRecord> p = diseaseMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BreedingBatch> batches = batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(BreedingBatch::getId, x -> x, (a, b) -> a));
        Map<Long, Pond> ponds = pondMapper.selectList(null).stream()
                .collect(Collectors.toMap(Pond::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = p.getRecords().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            BreedingBatch b = batches.get(d.getBatchId());
            m.put("id", d.getId());
            m.put("diseaseNo", d.getDiseaseNo());
            m.put("batchId", d.getBatchId());
            m.put("batchNo", b == null ? "-" : b.getBatchNo());
            m.put("variety", b == null ? "-" : b.getVariety());
            m.put("pondName", ponds.get(d.getPondId()) == null ? "-" : ponds.get(d.getPondId()).getPondName());
            m.put("symptom", d.getSymptom());
            m.put("deathCount", d.getDeathCount());
            m.put("image", d.getImage());
            m.put("reportTime", d.getReportTime());
            m.put("reporter", d.getReporter());
            m.put("diagnosis", d.getDiagnosis());
            m.put("drugName", d.getDrugName());
            m.put("dose", d.getDose());
            m.put("withdrawalDays", d.getWithdrawalDays());
            m.put("withdrawalEnd", d.getWithdrawalEnd());
            m.put("reviewStatus", d.getReviewStatus());
            m.put("reviewer", d.getReviewer());
            m.put("reviewTime", d.getReviewTime());
            m.put("reviewNote", d.getReviewNote());
            m.put("createTime", d.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long report(Map<String, Object> body) {
        Long batchId = body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId")));
        BreedingBatch b = batchId == null ? null : batchMapper.selectById(batchId);
        if (b == null) {
            throw new BizException("请选择有效的养殖批次");
        }
        String symptom = str(body.get("symptom"));
        if (symptom == null || symptom.trim().length() < 3) {
            throw new BizException("症状描述至少 3 个字");
        }
        DiseaseRecord d = new DiseaseRecord();
        d.setDiseaseNo("BH" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%03d", diseaseMapper.selectCount(null) % 999 + 1));
        d.setBatchId(batchId);
        d.setPondId(b.getPondId());
        d.setSymptom(symptom.trim());
        d.setDeathCount(body.get("deathCount") == null ? 0 : Integer.valueOf(String.valueOf(body.get("deathCount"))));
        d.setImage(str(body.get("image")));
        d.setReportTime(LocalDateTime.now());
        d.setReporter(UserContext.name());
        d.setReviewStatus("PENDING");
        d.setCreateTime(LocalDateTime.now());
        diseaseMapper.insert(d);
        logService.record("DISEASE", d.getId(), d.getDiseaseNo(), null, "PENDING", "病害上报", symptom);
        logService.notifyRole("TECH", "病害上报 " + d.getDiseaseNo() + " 待诊断", "DISEASE", d.getId(), d.getDiseaseNo());
        return d.getId();
    }

    /** 技术员审核：通过时必填药名、剂量、休药期；驳回必填原因 */
    @Transactional
    public DiseaseRecord review(Long id, String status, String diagnosis, String drugName, String dose,
                                Integer withdrawalDays, String note) {
        DiseaseRecord d = diseaseMapper.selectById(id);
        if (d == null) {
            throw new BizException("病害记录不存在");
        }
        if (!"PENDING".equals(d.getReviewStatus())) {
            throw new BizException("该记录已审核，请勿重复提交");
        }
        if (!List.of("APPROVED", "REJECTED").contains(status)) {
            throw new BizException("审核结论不合法");
        }
        if ("REJECTED".equals(status) && (note == null || note.trim().length() < 5)) {
            throw new BizException("驳回必须填写不少于 5 个字的原因");
        }
        if ("APPROVED".equals(status)) {
            if (drugName == null || drugName.isBlank()) {
                throw new BizException("通过审核必须填写用药品名");
            }
            if (withdrawalDays == null || withdrawalDays < 0) {
                throw new BizException("请填写有效的休药期天数");
            }
            d.setDrugName(drugName.trim());
            d.setDose(dose);
            d.setWithdrawalDays(withdrawalDays);
            d.setWithdrawalEnd(LocalDate.now().plusDays(withdrawalDays));
        }
        d.setDiagnosis(diagnosis);
        d.setReviewStatus(status);
        d.setReviewer(UserContext.name());
        d.setReviewTime(LocalDateTime.now());
        d.setReviewNote(note);
        diseaseMapper.updateById(d);
        logService.record("DISEASE", id, d.getDiseaseNo(), "PENDING", status,
                "APPROVED".equals(status) ? "用药审核通过" : "用药审核驳回", note);
        logService.notifyUser(d.getReportTime() == null ? UserContext.userId() : UserContext.userId(),
                "病害 " + d.getDiseaseNo() + ("APPROVED".equals(status) ? " 用药已通过" : " 用药被驳回"),
                "DISEASE", id, d.getDiseaseNo());
        return d;
    }

    /** 场长作废用药记录（休药期内如确需起捕，必须先作废并说明理由） */
    @Transactional
    public void voidRecord(Long id, String reason) {
        DiseaseRecord d = diseaseMapper.selectById(id);
        if (d == null) {
            throw new BizException("病害记录不存在");
        }
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "只有场长可以作废用药记录");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("作废必须填写不少于 5 个字的原因");
        }
        String from = d.getReviewStatus();
        d.setReviewStatus("VOID");
        d.setReviewNote(reason);
        d.setReviewer(UserContext.name());
        d.setReviewTime(LocalDateTime.now());
        diseaseMapper.updateById(d);
        logService.record("DISEASE", id, d.getDiseaseNo(), from, "VOID", "作废用药记录", reason);
    }

    /** 批次是否存在生效中的休药期 */
    public DiseaseRecord activeWithdrawal(Long batchId, LocalDate date) {
        List<DiseaseRecord> list = diseaseMapper.selectList(new LambdaQueryWrapper<DiseaseRecord>()
                .eq(DiseaseRecord::getBatchId, batchId)
                .eq(DiseaseRecord::getReviewStatus, "APPROVED")
                .ge(DiseaseRecord::getWithdrawalEnd, date)
                .orderByDesc(DiseaseRecord::getWithdrawalEnd).last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
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
