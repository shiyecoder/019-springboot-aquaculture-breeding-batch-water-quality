# -*- coding: utf-8 -*-
"""生成 design/ 下的可点击 HTML 设计稿（共用 mock.css，内置假数据）。"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CSS = "mock.css"

MENU = [
    ("养殖批次看板", "dashboard", "/dashboard", 1),
    ("养殖作业", None, None, 0),
    ("塘口总览", "ponds", "/ponds", 1),
    ("养殖批次", "batches", "/batches", 1),
    ("投喂计划", "feeding", "/feeding", 1),
    ("水质监测", "water", "/water", 1),
    ("风险与病害", None, None, 0),
    ("风险预警中心", "warnings", "/warnings", 1),
    ("病害用药", "disease", "/disease", 1),
    ("产出管理", None, None, 0),
    ("分塘与起捕", "harvest", "/harvest", 1),
    ("规则中心", None, None, 0),
    ("水质风险规则", "rules", "/rules", 1),
    ("协同", None, None, 0),
    ("消息中心", "messages", "/messages", 1),
    ("个人中心", "profile", "/profile", 1),
]

def shell(active, title, crumb, body, role="场长 张海生"):
    nav = []
    for name, key, path, is_link in MENU:
        if is_link:
            cls = ' class="on"' if key == active else ""
            nav.append(f'<a href="{key}.html"{cls}><span class="dot"></span>{name}</a>')
        else:
            nav.append(f'<div class="grp">{name}</div>')
    nav.append('<div class="grp">系统管理</div>')
    for name, key in [("用户管理", "users"), ("角色管理", "roles"), ("菜单管理", "menus"), ("操作日志", "logs")]:
        cls = ' class="on"' if key == active else ""
        nav.append(f'<a href="{key}.html"{cls}><span class="dot"></span>{name}</a>')
    return f"""<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>{title} · 水产养殖批次与水质监测管理系统</title>
<link rel="stylesheet" href="{CSS}"></head><body>
<div class="app">
  <aside class="sidebar">
    <div class="brand"><i>渔</i>养殖管理平台</div>
    <nav class="menu">{''.join(nav)}</nav>
  </aside>
  <div class="main">
    <header class="topbar">
      <span class="crumb">首页 / {crumb}</span><span class="spacer"></span>
      <span class="crumb">消息 <b style="color:#AD2E3A">3</b></span>
      <span class="who"><img class="avatar" src="../uploads/seed/avatar/01.jpg">{role}</span>
    </header>
    <div class="content">{body}</div>
  </div>
</div></body></html>"""

def head(title, sub, actions='<button class="btn">导出</button><button class="btn primary">+ 新建</button>'):
    return f'<div class="page-h"><h1>{title}</h1><span class="sub">{sub}</span><span class="spacer"></span>{actions}</div>'

def filters(items):
    html = []
    for label, kind, val in items:
        if kind == "select":
            html.append(f'<label>{label}</label><select class="select"><option>{val}</option></select>')
        elif kind == "input":
            html.append(f'<label>{label}</label><input class="input" value="{val}">')
        else:
            html.append(f'<label>{label}</label><input class="input" value="{val}" style="width:130px;min-width:0">')
    html.append('<button class="btn primary">查询</button><button class="btn">重置</button>')
    return f'<div class="filters">{"".join(html)}</div>'

def table(headers, rows, widths=None):
    th = "".join(f"<th>{h}</th>" for h in headers)
    tr = ""
    for r in rows:
        tr += "<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>"
    return f'<div class="card" style="padding:0"><table><thead><tr>{th}</tr></thead><tbody>{tr}</tbody></table></div>'

def pager(total=22, cur=1):
    return (f'<div class="pager">共 <b class="num">{total}</b> 条 · 每页 '
            f'<select class="select" style="width:78px;min-width:0;height:30px"><option>10</option><option>20</option></select> '
            f'<span class="btn" style="padding:4px 9px">上一页</span><b>{cur}</b><span class="btn" style="padding:4px 9px">下一页</span></div>')

def kpi(items):
    cells = "".join(f'<div class="card"><div class="k">{k}</div><div class="v num">{v}<small>{u}</small></div><div class="tip">{t}</div></div>'
                    for k, v, u, t in items)
    return f'<div class="grid kpi" style="margin-bottom:14px">{cells}</div>'

def bars(data, unit=""):
    top = max(v for _, v in data)
    items = "".join(f'<div class="bar" style="height:{max(8, int(v/top*150))}px" title="{n} {v}{unit}"><span>{n}</span></div>' for n, v in data)
    return f'<div class="chart">{items}</div>'

def card(title, inner, extra=""):
    return f'<div class="card"><div class="section-t">{title}{extra}</div>{inner}</div>'

pages = {}

# 1 看板
pages["dashboard"] = shell("dashboard", "养殖批次看板", "工作台 / 养殖批次看板", head("养殖批次看板", "近 30 天数据 · 每 10 分钟刷新", '<button class="btn">本月</button><button class="btn">本季</button><button class="btn primary">本年</button>') + kpi([
    ("养殖批次数", "18", "个", "较上周 +2"),
    ("水质合格率", "86.4", "%", "近 30 天 120 条检测"),
    ("高风险预警", "4", "个", "未关闭"),
    ("平均处置时长", "6.8", "小时", "已关闭预警"),
    ("死亡率", "1.238", "%", "累计死亡 / 投苗量"),
    ("存塘量", "26,480", "kg", "全部在养批次"),
]) + f"""<div class="grid two" style="margin-bottom:14px">
  {card("水质趋势（合格率 / 溶解氧）", '<div class="line"><svg viewBox="0 0 640 190" preserveAspectRatio="none" style="width:100%;height:100%"><polyline fill="none" stroke="#284B63" stroke-width="2.5" points="10,120 80,96 150,132 220,74 290,88 360,60 430,104 500,70 570,48 630,66"/><polyline fill="none" stroke="#3C6E71" stroke-width="2" stroke-dasharray="5 4" points="10,150 80,140 150,120 220,128 290,100 360,96 430,118 500,92 570,84 630,78"/></svg></div><div class="legend"><span><i style="background:#284B63"></i>合格率 %</span><span><i style="background:#3C6E71"></i>溶解氧均值 mg/L</span></div>')}
  {card("预警类型分布", '<div style="display:flex;gap:18px;align-items:center"><div class="donut"></div><div class="legend" style="flex-direction:column;gap:8px"><span><i style="background:#284B63"></i>溶解氧 14</span><span><i style="background:#3C6E71"></i>氨氮 9</span><span><i style="background:#AD2E3A"></i>亚硝酸盐 7</span><span><i style="background:#C9D6D2"></i>其他 6</span></div></div>')}
</div>
<div class="grid three">
  {card("塘口存塘量 Top6", bars([("一号塘", 4200), ("三号塘", 3800), ("五号塘", 3300), ("二号塘", 2900), ("六号塘", 2400), ("四号塘", 1800)], "kg"))}
  {card("风险分层结果分布", bars([("低风险", 68), ("中风险", 34), ("高风险", 18)]))}
  {card("饲料系数（批次）", bars([("YC061", 1.42), ("YC062", 1.36), ("YC063", 1.58), ("YC064", 1.29)]))}
</div>""")

# 2 塘口
rows = []
ponds = [("TK-01", "一号塘", "9.7", "1.8", "河道水", 3, "王大山", "正常", 2, "4,200"),
         ("TK-02", "二号塘", "11.4", "2.1", "地下水", 2, "刘小勇", "正常", 1, "2,900"),
         ("TK-03", "三号塘", "13.1", "2.4", "水库水", 4, "孙丽", "正常", 2, "3,800"),
         ("TK-04", "四号塘", "14.8", "1.8", "河道水", 2, "王大山", "正常", 1, "1,800"),
         ("TK-05", "五号塘", "16.5", "2.1", "地下水", 3, "刘小勇", "正常", 2, "3,300"),
         ("TK-06", "六号塘", "18.2", "2.4", "水库水", 2, "孙丽", "正常", 1, "2,400"),
         ("TK-07", "七号塘", "19.9", "1.8", "河道水", 3, "王大山", "维护中", 0, "0"),
         ("TK-08", "八号塘", "21.6", "2.1", "地下水", 2, "刘小勇", "空闲", 0, "0")]
for i, p in enumerate(ponds, 1):
    tag = "ok" if p[7] == "正常" else ("warn" if p[7] == "维护中" else "plain")
    rows.append([p[0], f'<img class="thumb" src="../uploads/seed/pond/{(i-1)%5+1:02d}.jpg">', p[1], f'<span class="num">{p[2]}</span>', f'<span class="num">{p[3]}</span>',
                 p[4], f'<span class="num">{p[5]}</span>', p[6], f'<span class="tag {tag}">{p[7]}</span>',
                 f'<span class="num">{p[8]}</span>', f'<span class="num">{p[9]}</span>',
                 '<a>查看</a> · <a>编辑</a> · <a>更多</a>'])
pond_body = head("塘口总览", "共 8 个塘口 · 6 个在养") + filters([
    ("塘口", "input", "编号 / 名称 / 负责人"), ("状态", "select", "全部状态"), ("水源", "select", "全部水源")]) + \
    '<div class="grid" style="grid-template-columns:repeat(4,1fr);margin-bottom:14px">' + \
    "".join(f'<div class="card" style="padding:0;overflow:hidden"><img src="../uploads/seed/pond/{i:02d}.jpg" style="width:100%;height:118px;object-fit:cover;display:block"><div style="padding:12px 14px"><b>{ponds[i-1][1]}</b> <span class="tag {"ok" if ponds[i-1][7]=="正常" else "warn"}">{ponds[i-1][7]}</span><div class="tip" style="color:#5B7176;font-size:12.5px;margin-top:6px">{ponds[i-1][0]} · {ponds[i-1][2]} 亩 · 存塘 {ponds[i-1][9]} kg</div></div></div>' for i in range(1, 5)) + \
    '</div>' + table(["塘口编号", "现场照片", "名称", "面积(亩)", "水深(m)", "水源", "增氧机", "负责人", "状态", "在养批次", "存塘(kg)", "操作"], rows) + pager(8)
pages["ponds"] = shell("ponds", "塘口总览", "养殖作业 / 塘口总览", pond_body)

# 3 批次
brows = []
bd = [("YC202604001", "TK-01", "草鱼", "2026-04-02", "12", "养殖中", "中期", "3,800", "1", "正常"),
      ("YC202605002", "TK-02", "南美白对虾", "2026-05-09", "8", "养殖中", "成鱼", "2,900", "2", "超限"),
      ("YC202605003", "TK-03", "加州鲈", "2026-05-21", "9", "养殖中", "中期", "3,300", "0", "正常"),
      ("YC202606004", "TK-04", "鲫鱼", "2026-06-03", "6", "起捕中", "成鱼", "1,800", "1", "超限"),
      ("YC202607005", "TK-05", "黄颡鱼", "2026-07-11", "7", "养殖中", "中期", "3,300", "0", "正常"),
      ("YC202608006", "TK-06", "罗非鱼", "2026-08-02", "10", "准备中", "苗期", "0", "0", "无数据"),
      ("YC202609007", "TK-01", "草鱼", "2026-09-15", "11", "养殖中", "苗期", "2,400", "3", "超限"),
      ("YC202603008", "TK-03", "鲫鱼", "2026-03-08", "5", "已结束", "成鱼", "0", "0", "正常")]
for b in bd:
    st = {"养殖中": "info", "起捕中": "warn", "准备中": "plain", "已结束": "plain"}[b[5]]
    lv = {"正常": "ok", "超限": "bad", "无数据": "plain"}[b[9]]
    brows.append([f'<a>{b[0]}</a>', b[1], b[2], b[3], f'<span class="num">{b[4]}</span>', f'<span class="tag {st}">{b[5]}</span>',
                  b[6], f'<span class="num">{b[7]}</span>', f'<span class="tag {lv}">{b[9]}</span>', f'<span class="num">{b[8]}</span>',
                  '<a>详情</a> · <a>流转</a>'])
pages["batches"] = shell("batches", "养殖批次", "养殖作业 / 养殖批次", head("养殖批次", "共 22 个批次 · 在养 18 个") + filters([
    ("塘口", "select", "全部塘口"), ("品种", "select", "全部品种"), ("状态", "select", "全部状态"), ("阶段", "select", "全部阶段")])
    + table(["批次编号", "塘口", "品种", "投苗日期", "投苗量(万尾)", "状态", "生长阶段", "目标产量(kg)", "水质", "存塘(kg)", "操作"], brows) + pager(22))

# 4 水质录入
wrows = []
wd = [("SZ20261031-120", "一号塘", "YC202604001", "2026-10-31 08:20", "25.4", "7.32", "5.8", "0.062", "0.021", "正常"),
      ("SZ20261030-119", "二号塘", "YC202605002", "2026-10-30 09:05", "31.2", "8.72", "3.4", "0.268", "0.132", "超限"),
      ("SZ20261029-118", "三号塘", "YC202605003", "2026-10-29 07:50", "24.8", "7.18", "6.1", "0.041", "0.018", "正常"),
      ("SZ20261028-117", "四号塘", "YC202606004", "2026-10-28 08:30", "33.9", "9.12", "2.2", "0.482", "0.228", "严重"),
      ("SZ20261027-116", "五号塘", "YC202607005", "2026-10-27 09:12", "26.1", "7.44", "5.2", "0.088", "0.032", "正常")]
for w in wd:
    tag = {"正常": "ok", "超限": "warn", "严重": "bad"}[w[9]]
    wrows.append([f'<a>{w[0]}</a>', w[1], f'<a>{w[2]}</a>', w[3], f'<span class="num">{w[4]}</span>', f'<span class="num">{w[5]}</span>',
                  f'<span class="num">{w[6]}</span>', f'<span class="num">{w[7]}</span>', f'<span class="num">{w[8]}</span>',
                  f'<span class="tag {tag}">{w[9]}</span>', '<a>风险计算</a> · <a>详情</a>'])
pages["water"] = shell("water", "水质监测", "养殖作业 / 水质监测", head("水质监测", "近 30 天 120 条检测 · 合格 86.4%") +
    f"""<div class="grid two" style="grid-template-columns:1.15fr 1fr;margin-bottom:14px">
  {card("录入水质检测", '''
  <div class="form-row"><label>养殖批次</label><select class="select"><option>YC202604001 · 一号塘 · 草鱼</option></select></div>
  <div class="form-row"><label>检测时间</label><input class="input" value="2026-10-31 08:20"></div>
  <div class="form-row"><label>水温 ℃</label><input class="input" value="25.4" style="width:120px">
    <label style="width:60px">pH</label><input class="input" value="7.32" style="width:120px"></div>
  <div class="form-row"><label>溶解氧 mg/L</label><input class="input" value="5.8" style="width:120px">
    <label style="width:80px">透明度 cm</label><input class="input" value="42" style="width:100px"></div>
  <div class="form-row"><label>氨氮 mg/L</label><input class="input" value="0.062" style="width:120px">
    <label style="width:90px">亚硝酸盐</label><input class="input" value="0.021" style="width:100px"></div>
  <div class="form-row"><label>仪表照片</label><div class="card" style="padding:6px"><img src="../uploads/seed/gauge/01.jpg" style="width:96px;height:72px;object-fit:cover;border-radius:4px"></div><span class="crumb">支持 jpg/png，≤5MB</span></div>
  <div class="form-row"><label>备注</label><input class="input" placeholder="异常情况说明"></div>
  <div style="padding-left:122px"><button class="btn primary">保存并计算风险</button> <button class="btn">存草稿</button></div>''')}
  {card("最近一次计算结果", '''
  <div class="note" style="margin-bottom:12px">规则版本 v2 · 生长阶段 苗期 · 计算时间 2026-10-31 08:21 · 结果可复算</div>
  <div class="score-item"><span class="lb">水温</span><span class="bar"><i style="width:0"></i></span><span class="sc">0</span></div>
  <div class="score-item"><span class="lb">pH</span><span class="bar"><i style="width:4%"></i></span><span class="sc">4</span></div>
  <div class="score-item"><span class="lb">溶解氧</span><span class="bar"><i style="width:14%"></i></span><span class="sc">14</span></div>
  <div class="score-item"><span class="lb">氨氮</span><span class="bar"><i class="bad" style="width:32%"></i></span><span class="sc">32</span></div>
  <div class="score-item"><span class="lb">亚硝酸盐</span><span class="bar"><i style="width:9%"></i></span><span class="sc">9</span></div>
  <div class="score-item"><span class="lb">连续异常</span><span class="bar"><i style="width:0"></i></span><span class="sc">0</span></div>
  <div style="margin-top:14px;font-size:13px">综合得分 <b class="num" style="font-size:22px;color:#2B7A5A">8.6</b> <span class="tag ok">低风险</span></div>
  <div class="note" style="margin-top:12px">建议：各项指标均在适宜区间，按每日 1 次频次继续监测。</div>''')}
</div>""" + table(["记录编号", "塘口", "批次", "检测时间", "水温", "pH", "溶氧", "氨氮", "亚硝酸盐", "判定", "操作"], wrows) + pager(120))

# 5 预警中心
nrows = []
nd = [("YJ20261031-036", "二号塘", "YC202605002", "溶解氧", "高", "待处置", "2026-10-31 09:30", "—"),
      ("YJ20261030-035", "四号塘", "YC202606004", "多参数", "高", "处置中", "2026-10-30 08:55", "刘小勇"),
      ("YJ20261030-034", "一号塘", "YC202609007", "氨氮", "中", "待复测", "2026-10-30 08:10", "王大山"),
      ("YJ20261029-033", "三号塘", "YC202605003", "亚硝酸盐", "中", "已关闭", "2026-10-29 07:40", "李文娟"),
      ("YJ20261028-032", "五号塘", "YC202607005", "pH", "中", "已关闭", "2026-10-28 09:20", "陈志远")]
for n in nd:
    st = {"待处置": "bad", "处置中": "warn", "待复测": "warn", "已关闭": "ok"}[n[5]]
    lv = "bad" if n[4] == "高" else "warn"
    nrows.append([f'<a>{n[0]}</a>', n[1], f'<a>{n[2]}</a>', n[3], f'<span class="tag {lv}">{n[4]}</span>',
                  f'<span class="tag {st}">{n[5]}</span>', n[6], n[7], '<a>处置</a> · <a>详情</a>'])
pages["warnings"] = shell("warnings", "风险预警中心", "风险与病害 / 风险预警中心", head("风险预警中心", "未关闭 12 条 · 今日新增 3 条") +
    """<div class="filters"><span class="btn primary" style="background:#fff;color:#284B63;border-color:#284B63">全部 36</span>
<span class="btn">待处置 6</span><span class="btn">处置中 4</span><span class="btn">待复测 2</span><span class="btn">已关闭 24</span>
<span class="spacer"></span><select class="select"><option>全部等级</option></select><select class="select"><option>全部类型</option></select></div>"""
    + table(["预警编号", "塘口", "批次", "类型", "等级", "状态", "生成时间", "处置人", "操作"], nrows) + pager(36))

# 6 风险详情（创新点）
pages["warning-detail"] = shell("warnings", "预警详情与风险分层", "风险与病害 / 风险预警中心 / 详情", f"""
{head("YJ20261031-036", "二号塘 · YC202605002 · 南美白对虾", '<button class="btn">导出处置单</button><button class="btn primary">登记处置措施</button>')}
<div class="grid two" style="grid-template-columns:1.25fr 1fr;margin-bottom:14px">
  {card("风险分层计算明细", '''
  <div class="note" style="margin-bottom:14px">输入快照：水温 31.2℃ · pH 8.72 · 溶氧 3.4 mg/L · 氨氮 0.268 mg/L · 亚硝酸盐 0.132 mg/L · 连续异常 2 次<br>规则版本 v2（权重 20/15/25/20/15/5）· 生长阶段 成鱼 · 计算时间 2026-10-31 09:31</div>
  <div class="score-item"><span class="lb">水温</span><span class="bar"><i style="width:0"></i></span><span class="sc">0</span></div>
  <div class="score-item"><span class="lb">pH</span><span class="bar"><i style="width:26%"></i></span><span class="sc">26</span></div>
  <div class="score-item"><span class="lb">溶解氧</span><span class="bar"><i class="bad" style="width:80%"></i></span><span class="sc">80</span></div>
  <div class="score-item"><span class="lb">氨氮</span><span class="bar"><i class="bad" style="width:56%"></i></span><span class="sc">56</span></div>
  <div class="score-item"><span class="lb">亚硝酸盐</span><span class="bar"><i style="width:38%"></i></span><span class="sc">38</span></div>
  <div class="score-item"><span class="lb">连续异常</span><span class="bar"><i style="width:50%"></i></span><span class="sc">50</span></div>
  <div style="display:flex;align-items:center;gap:14px;margin-top:16px"><div>综合得分 <b class="num" style="font-size:26px;color:#AD2E3A">62.6</b></div><span class="tag bad">高风险</span><span class="tag plain">人工修正：未调整</span></div>
  <div class="note" style="margin-top:14px">未满足条件：溶解氧 3.4 mg/L 低于下限 4.0；氨氮 0.268 mg/L 高于上限 0.3 的 89%<br>建议：立即开启全部增氧机，2 小时内复测；氨氮偏高先换水 1/3 并投放改底剂，次日复检。</div>
  <div style="margin-top:14px"><button class="btn danger">人工修正等级</button> <span class="crumb">修正需填写不少于 5 个字的原因，原始结果不被覆盖</span></div>''')}
  {card("处置与复测时间轴", '''
  <ul class="timeline">
    <li><div class="t">生成预警（待处置）</div><div class="m">2026-10-31 09:31 · 系统自动 · 得分 62.6 高风险</div></li>
    <li><div class="t">登记处置：开启增氧</div><div class="m">2026-10-31 10:05 · 刘小勇 · 全部增氧机开启，水位加深 10cm</div></li>
    <li><div class="t">状态流转：处置中 → 待复测</div><div class="m">2026-10-31 10:06 · 刘小勇</div></li>
    <li><div class="t">复测录入</div><div class="m">2026-10-31 12:10 · 王大山 · 溶氧 4.9 mg/L、氨氮 0.212 mg/L</div></li>
  </ul>
  <div class="note">复测结论：溶氧回升但仍低于 5.0，建议继续增氧并延长复测间隔至 6 小时。</div>''')}
</div>
{card("关联水质记录", '<table><thead><tr><th>记录编号</th><th>检测时间</th><th>水温</th><th>pH</th><th>溶氧</th><th>氨氮</th><th>亚硝酸盐</th><th>判定</th></tr></thead><tbody><tr><td>SZ20261031-120</td><td>2026-10-31 09:20</td><td class="num">31.2</td><td class="num">8.72</td><td class="num">3.4</td><td class="num">0.268</td><td class="num">0.132</td><td><span class="tag bad">严重超限</span></td></tr><tr><td>SZ20261031-121</td><td>2026-10-31 12:10</td><td class="num">30.4</td><td class="num">8.41</td><td class="num">4.9</td><td class="num">0.212</td><td class="num">0.108</td><td><span class="tag warn">超限</span></td></tr></tbody></table>')}""")

# 7 投喂
frows = []
fd = [("TF20261031-001", "YC202604001", "草鱼膨化配合饲料", "28.0", "早间", "王大山", "待执行", "—"),
      ("TF20261031-002", "YC202605002", "淡水鱼配合饲料", "34.0", "早间", "刘小勇", "已执行", "31.6"),
      ("TF20261030-087", "YC202605003", "加州鲈膨化料", "42.0", "傍晚", "孙丽", "已执行", "44.8"),
      ("TF20261030-086", "YC202606004", "淡水鱼配合饲料", "26.0", "傍晚", "王大山", "逾期", "—")]
for f in fd:
    st = {"待执行": "info", "已执行": "ok", "逾期": "bad"}[f[6]]
    chk = '<span class="tag warn">待复核</span>' if f[7] != "—" and float(f[7]) > float(f[3]) else "—"
    frows.append([f'<a>{f[0]}</a>', f'<a>{f[1]}</a>', f[2], f'<span class="num">{f[3]}</span>', f[4], f[5],
                  f'<span class="tag {st}">{f[6]}</span>', f'<span class="num">{f[7]}</span>', chk, '<a>执行</a> · <a>详情</a>'])
pages["feeding"] = shell("feeding", "投喂计划", "养殖作业 / 投喂计划", head("投喂计划", "今日 26 条任务 · 待执行 6 条") + filters([
    ("日期", "date", "2026-10-31"), ("状态", "select", "全部状态"), ("批次", "select", "全部批次")])
    + table(["计划编号", "批次", "饲料", "标准量(kg)", "时段", "执行人", "状态", "实际量(kg)", "复核", "操作"], frows) + pager(624))

# 8 病害
drows = []
dd = [("BH20261030-015", "YC202605002", "虾体附肢发红、肠道空肠", "86", "氟苯尼考", "14 天", "已通过", "01"),
      ("BH20261028-014", "YC202606004", "鱼体表充血、鳍条溃烂", "42", "聚维酮碘", "7 天", "已通过", "02"),
      ("BH20261026-013", "YC202609007", "鱼群浮头、鳃丝发白", "120", "—", "—", "待审核", "03"),
      ("BH20261021-012", "YC202605003", "体表白色棉絮状菌丝", "18", "—", "—", "已驳回", "01"),
      ("BH20261012-011", "YC202607005", "肝脏发白、游动缓慢", "56", "恩诺沙星", "21 天", "已作废", "02")]
for d in dd:
    st = {"已通过": "ok", "待审核": "warn", "已驳回": "bad", "已作废": "plain"}[d[6]]
    drows.append([f'<a>{d[0]}</a>', f'<a>{d[1]}</a>', d[2], f'<span class="num">{d[3]}</span>',
                  f'<img class="thumb" src="../uploads/seed/disease/{d[7]}.jpg">', d[4], d[5],
                  f'<span class="tag {st}">{d[6]}</span>', '<a>审核</a> · <a>详情</a>'])
pages["disease"] = shell("disease", "病害用药", "风险与病害 / 病害用药", head("病害用药", "待审核 3 条 · 休药期中 2 个批次") + filters([
    ("审核状态", "select", "全部状态"), ("批次", "select", "全部批次")]) +
    table(["病害编号", "批次", "症状", "死亡数", "现场图片", "药品", "休药期", "状态", "操作"], drows) + pager(15) +
    f'<div class="note" style="margin-top:14px">休药期规则：用药审核通过后按休药天数计算截止日，截止日之前该批次禁止创建起捕任务；确需起捕须由场长作废用药记录并填写原因。</div>')

# 9 起捕
hrows = []
hd = [("QB20261026-006", "YC202606004", "鲫鱼", "2026-10-26", "1,020.5", "A", "批发市场", "已完成", "01"),
      ("QB20261022-005", "YC202606004", "鲫鱼", "2026-10-22", "860.0", "B", "加工厂", "已完成", "02"),
      ("QB20261018-004", "YC202603008", "草鱼", "2026-10-18", "1,460.0", "A", "商超直供", "已完成", "01"),
      ("QB20261031-007", "YC202605002", "南美白对虾", "2026-11-02", "900.0", "A", "批发市场", "已审批", "02")]
for h in hd:
    st = "ok" if h[7] == "已完成" else "info"
    hrows.append([f'<a>{h[0]}</a>', f'<a>{h[1]}</a>', h[2], h[3], f'<span class="num">{h[4]}</span>', h[5], h[6],
                  f'<span class="tag {st}">{h[7]}</span>', f'<img class="thumb" src="../uploads/seed/harvest/{h[8]}.jpg">',
                  '<a>完成</a> · <a>作废</a>'])
pages["harvest"] = shell("harvest", "分塘与起捕", "产出管理 / 分塘与起捕", head("分塘与起捕", "本季度起捕 3,340.5 kg") +
    f'<div class="mini"><div class="card"><div class="k">起捕总量</div><div class="v num" style="font-size:20px">3,340.5<small>kg</small></div></div><div class="card"><div class="k">平均损耗率</div><div class="v num" style="font-size:20px">2.1<small>%</small></div></div><div class="card"><div class="k">A 级占比</div><div class="v num" style="font-size:20px">66.7<small>%</small></div></div><div class="card"><div class="k">分塘记录</div><div class="v num" style="font-size:20px">10<small>条</small></div></div></div>'
    + table(["起捕编号", "批次", "品种", "计划日期", "起捕重量(kg)", "等级", "去向", "状态", "照片", "操作"], hrows) + pager(6))

# 10 规则配置
pages["rules"] = shell("rules", "水质风险规则", "规则中心 / 水质风险规则", head("多参数水质风险分层规则", "当前生效 v2 · 权重合计 100%", '<button class="btn">查看历史版本</button><button class="btn primary">保存为新版本</button>') +
    f"""<div class="grid two" style="grid-template-columns:1fr 1fr">
  {card("因子权重配置", '''
  <div class="form-row"><label>水温偏差</label><input class="input" value="20" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>pH 偏差</label><input class="input" value="15" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>溶解氧</label><input class="input" value="25" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>氨氮</label><input class="input" value="20" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>亚硝酸盐</label><input class="input" value="15" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>连续异常次数</label><input class="input" value="5" style="width:110px"><span class="crumb">%</span></div>
  <div class="form-row"><label>合计</label><b class="num" style="color:#2B7A5A">100%</b></div>
  <div class="note">修改保存后生成新版本，仅影响新计算或主动重新计算的数据；历史快照与规则版本永久保留。</div>''')}
  {card("适宜区间（按生长阶段）", '''<table><thead><tr><th>参数</th><th>苗期</th><th>中期</th><th>成鱼</th></tr></thead><tbody>
  <tr><td>水温 ℃</td><td>24~30</td><td>22~31</td><td>18~32</td></tr>
  <tr><td>pH</td><td>6.8~8.2</td><td>6.6~8.4</td><td>6.5~8.5</td></tr>
  <tr><td>溶解氧 mg/L</td><td>5~12</td><td>4.5~12</td><td>4~14</td></tr>
  <tr><td>氨氮 mg/L</td><td>0~0.15</td><td>0~0.2</td><td>0~0.3</td></tr>
  <tr><td>亚硝酸盐 mg/L</td><td>0~0.08</td><td>0~0.1</td><td>0~0.15</td></tr>
  </tbody></table><div class="note" style="margin-top:12px">等级阈值：总分 &lt;30 低风险，30~60 中风险，≥60 高风险。</div>''')}
</div>
<div class="grid three" style="margin-top:14px">
  {card("版本记录", '<table><thead><tr><th>版本</th><th>生效时间</th><th>创建人</th><th>状态</th></tr></thead><tbody><tr><td>v2</td><td>2026-08-01 08:30</td><td>李文娟</td><td><span class="tag ok">生效中</span></td></tr><tr><td>v1</td><td>2026-05-06 09:00</td><td>李文娟</td><td><span class="tag plain">已停用</span></td></tr></tbody></table>')}
  {card("推荐采纳率", '<div style="font-size:30px;color:#284B63;font-weight:600" class="num">83.3<small style="font-size:13px">%</small></div><div class="crumb">120 次计算，20 次人工修正</div><div class="note" style="margin-top:12px">人工修正率 16.7%，主要用于现场已浮头但规则判为中风险的场景。</div>')}
  {card("近 30 天结果分布", bars([("低", 68), ("中", 34), ("高", 18)]))}
</div>""")

# 11 用户 / 角色 / 菜单 / 日志
urows = []
ud = [("admin", "张海生", "系统管理员", "东区养殖场", "启用"), ("manager01", "周建国", "场长", "西区养殖场", "启用"),
      ("tech01", "李文娟", "技术员", "技术质检部", "启用"), ("tech02", "陈志远", "技术员", "技术质检部", "启用"),
      ("feeder01", "王大山", "养殖员", "东区养殖场", "启用"), ("feeder02", "刘小勇", "养殖员", "西区养殖场", "启用"),
      ("store01", "赵明", "仓管员", "东区养殖场", "启用"), ("feeder03", "孙丽", "养殖员", "西区养殖场", "启用")]
for i, u in enumerate(ud, 1):
    urows.append([f'<img class="avatar" src="../uploads/seed/avatar/{(i-1)%6+1:02d}.jpg">', u[0], u[1], u[2], u[3],
                  '<span class="tag ok">启用</span>', '<a>编辑</a> · <a>重置密码</a>'])
pages["users"] = shell("users", "用户管理", "系统管理 / 用户管理", head("用户管理", "共 8 个账号") + filters([("关键字", "input", "账号 / 姓名"), ("角色", "select", "全部角色")]) +
    table(["头像", "账号", "姓名", "角色", "组织", "状态", "操作"], urows) + pager(8))

pages["roles"] = shell("roles", "角色管理", "系统管理 / 角色管理", head("角色管理", "5 个角色 · 数据范围独立控制") +
    table(["角色名称", "编码", "数据范围", "说明", "操作"], [
        ["系统管理员", "ADMIN", "全部", "全部数据与系统配置", '<a>分配菜单</a> · <a>编辑</a>'],
        ["场长", "MANAGER", "全部", "塘口、批次、审批与统计", '<a>分配菜单</a> · <a>编辑</a>'],
        ["技术员", "TECH", "本部门", "阈值配置、病害诊断、用药审核", '<a>分配菜单</a> · <a>编辑</a>'],
        ["养殖员", "FEEDER", "本人", "投喂、换水、消毒、水质检测", '<a>分配菜单</a> · <a>编辑</a>'],
        ["仓管员", "STORE", "本人", "饲料与药品出入库", '<a>分配菜单</a> · <a>编辑</a>']]) +
    '<div class="note" style="margin-top:14px">菜单权限、按钮权限与数据范围分别控制；「菜单管理」仅对系统管理员可见，且不参与分配。</div>')

pages["menus"] = shell("menus", "菜单管理", "系统管理 / 菜单管理", head("菜单管理", "目录 / 菜单 / 按钮三级") +
    table(["名称", "类型", "路由", "权限标识", "排序", "可见", "操作"], [
        ["工作台", "目录", "/dashboard", "—", "1", "是", '<a>编辑</a>'],
        ["养殖批次看板", "菜单", "/dashboard/index", "dashboard:view", "2", "是", '<a>编辑</a>'],
        ["养殖作业", "目录", "/farm", "—", "10", "是", '<a>编辑</a>'],
        ["塘口总览", "菜单", "/ponds", "pond:list", "11", "是", '<a>编辑</a>'],
        ["养殖批次", "菜单", "/batches", "batch:list", "12", "是", '<a>编辑</a>'],
        ["执行投喂", "按钮", "—", "feeding:execute", "103", "—", '<a>编辑</a>']]))

pages["logs"] = shell("logs", "操作日志", "系统管理 / 操作日志", head("操作日志", "近 30 天 40 条") + filters([("关键字", "input", "操作人 / 模块 / 业务编号")]) +
    table(["时间", "操作人", "模块", "动作", "业务编号", "结果", "IP"], [
        ["2026-10-31 09:31", "李文娟", "预警处置", "处置", "YJ20261031-036", "成功", "127.0.0.1"],
        ["2026-10-31 09:20", "王大山", "水质监测", "新增", "SZ20261031-120", "成功", "127.0.0.1"],
        ["2026-10-30 17:42", "刘小勇", "投喂管理", "执行", "TF20261031-002", "成功", "127.0.0.1"],
        ["2026-10-30 08:55", "陈志远", "病害用药", "审核", "BH20261030-015", "成功", "127.0.0.1"]]) + pager(40))

pages["messages"] = shell("messages", "消息中心", "协同 / 消息中心", head("消息中心", "未读 3 条") +
    table(["标题", "业务类型", "业务编号", "接收人", "时间", "状态"], [
        ["水质预警 YJ20261031-036 待处置", "预警", "YJ20261031-036", "李文娟", "2026-10-31 09:31", '<span class="tag bad">未读</span>'],
        ["病害上报 BH20261026-013 待诊断", "病害", "BH20261026-013", "陈志远", "2026-10-26 10:12", '<span class="tag bad">未读</span>'],
        ["批次 YC202606004 已流转为起捕中", "批次", "YC202606004", "周建国", "2026-10-22 08:40", '<span class="tag plain">已读</span>']]) + pager(3))

pages["profile"] = shell("profile", "个人中心", "协同 / 个人中心", head("个人中心", "账号信息与安全设置", "") + f"""<div class="grid two" style="grid-template-columns:1fr 1fr">
  {card("基本资料", '''<div style="display:flex;gap:18px;align-items:center;margin-bottom:16px"><img class="avatar" style="width:64px;height:64px" src="../uploads/seed/avatar/01.jpg"><button class="btn">更换头像</button></div>
  <div class="form-row"><label>姓名</label><input class="input" value="张海生"></div>
  <div class="form-row"><label>手机号</label><input class="input" value="130****0001"></div>
  <div class="form-row"><label>邮箱</label><input class="input" value="admin@aqua.example.com"></div>
  <div style="padding-left:122px"><button class="btn primary">保存资料</button></div>''')}
  {card("修改密码", '''<div class="form-row"><label>原密码</label><input class="input" type="password" value="······"></div>
  <div class="form-row"><label>新密码</label><input class="input" type="password" placeholder="不少于 6 位"></div>
  <div class="form-row"><label>确认新密码</label><input class="input" type="password"></div>
  <div style="padding-left:122px"><button class="btn primary">修改密码</button></div>
  <div class="note" style="margin-top:12px">密码使用 BCrypt 保存；连续 5 次登录失败将锁定 10 分钟。</div>''')}
</div>""")

# 12 登录
pages["login"] = f"""<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>登录 · 水产养殖批次与水质监测管理系统</title>
<link rel="stylesheet" href="{CSS}"></head><body>
<div class="login-wrap">
  <div class="login-art">
    <div style="font-weight:600;letter-spacing:1px;margin-bottom:36px">渔 · 养殖管理平台</div>
    <h2>把塘口、批次与水质<br>管在同一张图上</h2>
    <p>面向中小型养殖场：塘口档案、养殖批次、投喂计划、水质手工检测、阈值预警、病害用药与起捕统计，一条链路走完。</p>
    <div class="badge">
      <div><b class="num">8</b><span>塘口接入</span></div>
      <div><b class="num">120</b><span>水质记录</span></div>
      <div><b class="num">86.4%</b><span>合格率</span></div>
    </div>
    <div class="rings"></div>
  </div>
  <div class="login-form"><div class="box">
    <h3>账号登录</h3>
    <div class="hint">使用场里分配的账号登录，账号异常请联系场长</div>
    <div class="fld"><label>账号</label><input value="admin" placeholder="请输入账号"></div>
    <div class="fld"><label>密码</label><input type="password" value="admin123" placeholder="请输入密码"></div>
    <button class="submit" onclick="location.href='dashboard.html'">登 录</button>
    <div class="tip">默认管理员：admin / admin123　·　普通账号：feeder01、tech01、store01，密码 123456<br>登录失败 5 次将锁定 10 分钟</div>
  </div></div>
</div></body></html>"""

# 13 养殖员门户
pages["portal"] = f"""<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>我的养殖任务 · 水产养殖批次与水质监测管理系统</title>
<link rel="stylesheet" href="{CSS}"></head><body>
<header class="portal-top">
  <div class="logo"><i>渔</i>养殖作业台</div>
  <nav><a class="on">我的任务</a><a>水质录入</a><a>预警处置</a><a>消息</a></nav>
  <span class="spacer"></span>
  <span class="who"><img class="avatar" src="../uploads/seed/avatar/04.jpg">王大山 · 养殖员</span>
</header>
<div class="portal-body">
  <div class="hero"><div><h2>今天有 6 条投喂任务、2 条预警待处置</h2><p>一号塘、三号塘上午已完成检测；二号塘溶解氧偏低，请优先处置。</p></div>
  <button class="btn" style="background:#fff;color:#284B63;border:0">去处理</button></div>
  <div class="mini">
    <div class="card"><div class="k" style="color:#5B7176;font-size:12.5px">待执行投喂</div><div class="v num" style="font-size:22px;color:#284B63">6<small>条</small></div></div>
    <div class="card"><div class="k" style="color:#5B7176;font-size:12.5px">待处置预警</div><div class="v num" style="font-size:22px;color:#AD2E3A">2<small>条</small></div></div>
    <div class="card"><div class="k" style="color:#5B7176;font-size:12.5px">今日已检测</div><div class="v num" style="font-size:22px;color:#284B63">3<small>塘</small></div></div>
    <div class="card"><div class="k" style="color:#5B7176;font-size:12.5px">负责塘口</div><div class="v num" style="font-size:22px;color:#284B63">3<small>个</small></div></div>
  </div>
  <div class="grid two" style="grid-template-columns:1.3fr 1fr">
    <div class="card"><div class="section-t">今日待办</div>
      <div class="task"><div class="t"><b>一号塘 · YC202604001 · 早间投喂</b><div class="m">标准量 28.0 kg · 草鱼膨化配合饲料</div></div><span class="tag warn">待执行</span><button class="btn primary" style="padding:5px 12px">回填用量</button></div>
      <div class="task"><div class="t"><b>三号塘 · YC202605003 · 水质检测</b><div class="m">上次检测 2026-10-30 07:50 · 溶解氧 6.1</div></div><span class="tag info">今日待检</span><button class="btn" style="padding:5px 12px">录入</button></div>
      <div class="task"><div class="t"><b>二号塘 · YJ20261031-036 · 溶解氧预警</b><div class="m">高风险 · 建议立即开启增氧机并复测</div></div><span class="tag bad">待处置</span><button class="btn danger" style="padding:5px 12px">处置</button></div>
      <div class="task"><div class="t"><b>一号塘 · YC202609007 · 傍晚投喂</b><div class="m">标准量 22.0 kg · 今日已投 18.4 kg</div></div><span class="tag warn">待执行</span><button class="btn" style="padding:5px 12px">回填用量</button></div>
    </div>
    <div class="card"><div class="section-t">我负责的塘口</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
        <div><img src="../uploads/seed/pond/01.jpg" style="width:100%;height:90px;object-fit:cover;border-radius:5px"><div style="font-size:13px;margin-top:6px">一号塘 · 草鱼</div><div class="crumb">存塘 3,800 kg</div></div>
        <div><img src="../uploads/seed/pond/04.jpg" style="width:100%;height:90px;object-fit:cover;border-radius:5px"><div style="font-size:13px;margin-top:6px">四号塘 · 鲫鱼</div><div class="crumb">存塘 1,800 kg</div></div>
      </div>
      <div class="note" style="margin-top:12px">现场录入支持拍照上传仪表照片；断网时先存本地草稿，恢复后自动提交。</div>
    </div>
  </div>
</div></body></html>"""

for name, html in pages.items():
    (ROOT / f"{name}.html").write_text(html, encoding="utf-8")
print("生成页面：", len(pages), sorted(pages))
