# 唯一候选双向对账

> 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。

## 已实现

以参考号、精确金额和日期容差构建双向候选，仅在两侧都唯一时匹配，重复候选保留异常，不贪心随意核销。

每侧最多300行，调用者须先保证同一币种、口径及方向。参考号首尾去空白后大小写敏感，金额精确相等且>0，日期容差0–31自然日。只接受左右度数都为1的候选，不递归消解歧义，不冲销、不记账、不更改余额。

## 入口与身份

从原工作台点击“专项业务工具”，或访问同源`/practical.html`。原页面与架构保留，不替换为新框架。

沿用现有工作台数据库会话，后端通过Auth.current验证账号启用状态及会话有效期；不接受请求自报角色，不绕过停用/注销。请求时从原sessionStorage读取当前会话，不保存额外账号密码。



接口`POST /api/practical/unique-reference-reconciliation`：JSON请求，`Authorization: Bearer <配置令牌或当前会话>`。200返回`success:true,data`；凭据缺失/无效401；参数错误400。不读取其他租户记录、不读取文件、不执行命令、不上传第三方模型、不实际审批/发布/发信/记账。

## 合成样例

```json
{
  "dateToleranceDays": 2,
  "left": [
    {
      "id": "L1",
      "reference": "A",
      "amount": 100,
      "date": "2026-10-23"
    },
    {
      "id": "L2",
      "reference": "B",
      "amount": 50,
      "date": "2026-10-23"
    },
    {
      "id": "L3",
      "reference": "B",
      "amount": 50,
      "date": "2026-10-23"
    }
  ],
  "right": [
    {
      "id": "R1",
      "reference": "A",
      "amount": 100,
      "date": "2026-10-24"
    },
    {
      "id": "R2",
      "reference": "B",
      "amount": 50,
      "date": "2026-10-23"
    }
  ]
}
```

## 参数与显示限制

- 页面按UTF-8字节限制128 KiB，超时15秒，无离线成功兜底，后端异常会直接提示。
- 后端校验最大12层、10000节点、总文本128000字符、单文本64000字符；通用数值≤30位有效数字、尺度绝对值≤12。各业务数组和文本有更小上限，详见实现。
- 日期限定1900–2200；金额只接受非负且最多2位小数；不暗中换算币种或忽略非法记录。
- 字段未知、标识重复和错误引用按各业务规则明确拒绝；所有结果来自本次输入，不改变输入对象。
- 结果表预览最多100行；JSON导出包含完整数据。
- 返回字符串使用DOM.textContent显示，来源文字不注入HTML。真实令牌不写浏览器持久存储、不进入导出文件。
- 服务端校验在JSON解析之后；生产网关仍需HTTPS、请求体上限、速率限制、日志脱敏和明确的访问控制，不能用这些校验替代网关防护。

## 测试与部署

Java21：`cd backend && mvn test`，包含原有用例、新算法单元和真实随机端口HTTP测试。新增接口测试覆盖匿名/错误凭据、有效调用、参数缺失、未知字段、非法JSON、数组根请求及已注销会话拒绝。

前端：`node --test frontend/test/practical-format.test.mjs`；保持原Vue工具链并运行`cd frontend && npm ci && npm test && npm run build`。

本次未增加数据库迁移。5个业务项目沿用原MySQL部署及初始化方式；轻量H5保持原有演示边界，不因此被描述成完整企业生产系统。完整执行结果以本批交付报告为准，不以本说明代替测试证据。

## Scope

Unique-candidate bilateral reconciliation is implemented as a deterministic computation over caller-provided, authorized data. This endpoint does not call external inference, fetch URLs, inspect private files or persist workflow decisions. Algorithm-specific limits above remain part of its contract. Existing source-available/non-commercial licensing, website and contacts are preserved.

### 新接口JSON精度防护

新增接口单独使用BigDecimal解析小数，拒绝重复JSON字段和尾随JSON对象；不修改旧接口的全局解析配置。数字在业务计算前统一校验精度/尺度，极端指数不能先下溢成0再进入金额或数量计算。前端只检查JSON语法和根对象，发送用户输入原文，不对数值重编码；后端响应内的BigDecimal统一以十进制字符串表示，避免浏览器解析和导出时丢失金额尾数。整数计数和布尔字段仍保持原类型。
