/**
 * 状态字典：中文标签 + Element tag 类型。
 * 设计规范要求状态不能只靠颜色表达，必须同时显示中文标签。
 */

/** 养殖批次状态：准备中→养殖中→起捕中→已结束 */
export const BATCH_STATUS = {
  PREP: { label: '准备中', type: 'info' },
  BREEDING: { label: '养殖中', type: 'primary' },
  HARVESTING: { label: '起捕中', type: 'warning' },
  CLOSED: { label: '已结束', type: 'success' }
}

/** 生长阶段 */
export const GROWTH_STAGE = {
  FRY: { label: '苗期', type: 'info' },
  JUVENILE: { label: '中期', type: 'primary' },
  ADULT: { label: '成鱼', type: 'success' }
}

/** 水质等级 */
export const WATER_LEVEL = {
  NORMAL: { label: '正常', type: 'success' },
  WARN: { label: '超限', type: 'warning' },
  DANGER: { label: '严重超限', type: 'danger' },
  NONE: { label: '未检测', type: 'info' }
}

/** 预警状态：待处置→处置中→待复测→已关闭 */
export const WARNING_STATUS = {
  PENDING: { label: '待处置', type: 'danger' },
  PROCESSING: { label: '处置中', type: 'warning' },
  RETEST: { label: '待复测', type: 'primary' },
  CLOSED: { label: '已关闭', type: 'success' }
}

/** 预警类型 */
export const WARN_TYPE = {
  DO: '溶解氧',
  PH: 'pH',
  NH3: '氨氮',
  NO2: '亚硝酸盐',
  TEMP: '水温',
  MULTI: '多参数'
}

/** 风险等级 */
export const RISK_LEVEL = {
  LOW: { label: '低风险', type: 'success', color: '#2B7A5A' },
  MID: { label: '中风险', type: 'warning', color: '#A9761F' },
  HIGH: { label: '高风险', type: 'danger', color: '#AD2E3A' }
}

/** 塘口状态 */
export const POND_STATUS = {
  NORMAL: { label: '使用中', type: 'success' },
  IDLE: { label: '空闲', type: 'info' },
  MAINTENANCE: { label: '维护中', type: 'warning' }
}

/** 水源类型 */
export const WATER_SOURCE = { WELL: '地下水', RESERVOIR: '水库水', RIVER: '河道水' }

/** 投喂计划状态 */
export const FEEDING_STATUS = {
  PENDING: { label: '待执行', type: 'warning' },
  DONE: { label: '已执行', type: 'success' },
  OVERDUE: { label: '已逾期', type: 'danger' }
}

/** 投喂时段 */
export const PERIOD = { AM: '早间', NOON: '午间', PM: '傍晚' }

/** 病害用药审核状态 */
export const REVIEW_STATUS = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  VOID: { label: '已作废', type: 'info' }
}

/** 起捕状态 */
export const HARVEST_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已审批', type: 'primary' },
  DONE: { label: '已完成', type: 'success' },
  VOID: { label: '已作废', type: 'info' }
}

/** 起捕等级 */
export const GRADE = {
  A: { label: 'A 级', type: 'success' },
  B: { label: 'B 级', type: 'primary' },
  C: { label: 'C 级', type: 'warning' }
}

/** 应急处置措施 */
export const MEASURE = { AERATION: '开启增氧', WATER_CHANGE: '换水', BOTTOM: '改底', DISINFECT: '消毒' }

export function statusOf(map, key) {
  return map[key] || { label: key || '-', type: 'info' }
}

/** 角色 → 视觉风格：养殖员走门户，其余走后台 */
export const PORTAL_ROLES = ['FEEDER']
