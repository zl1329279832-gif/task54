# 跨站点隔离与 PreviewToken 安全模型

> 依据 `PublishTaskCrossSiteTest` 测试用例及源码校验逻辑整理。二开时如需扩展跨站能力，须遵守本文约束。

---

## 1. 隔离维度总览

预约发布涉及 **三个 ID 维度** 做隔离校验，各维度在不同层生效：

| 维度 | 字段 | 隔离层 | 校验方式 |
|---|---|---|---|
| 站点归属 | `PublishTask.siteId` | Controller 层 | `ValidUtils.dataInSite(bean.siteId, currentSiteId)` |
| 目标站点 | `PublishTask.targetSiteId` | Service 层 | `siteService.select(targetSiteId)` 校验存在性 |
| 角色限制 | `PreviewTokenRole.roleId` | 应用层（预览访问时） | 关联表 JOIN 校验当前用户角色 |

---

## 2. siteId 隔离——Controller 层

### 2.1 写入隔离

所有写操作（`create` / `update` / `delete` / `cancel` / `generate-token`）：

```
Controller 从 Contexts.getCurrentSiteId() 取得当前站点
      │
      ├── create：siteId ← currentSiteId（覆盖前端传入）
      ├── update：先 select(id) → 校验 bean.siteId == currentSiteId
      ├── delete：逐条 select(id) → 校验 siteId == currentSiteId
      ├── cancel：同上
      └── generate-token：同上
```

> **关键点**：`siteId` 由服务端强制注入，前端即使传了其他站点 ID 也会被覆盖。这意味着 **A 站点的用户无法为 B 站点创建预约发布任务**。

### 2.2 查询隔离

`GET /`（列表）：`selectPage()` 内部通过 `PublishTaskArgs` 自动附加 `siteId` 过滤条件。

`GET /{id}`（详情）：加载后校验 `bean.siteId == currentSiteId`。

### 2.3 测试覆盖（PublishTaskCrossSiteTest）

| 测试方法 | 场景 | 断言 |
|---|---|---|
| `selectBySiteA_doesNotReturnSiteBTasks` | 查 A 站任务列表 | 不返回 B 站任务 |
| `previewToken_siteIsolation_siteMismatchRejectsToken` | 用 B 站上下文验证 A 站令牌 | `result.isEmpty()` |
| `previewToken_siteIsolation_sameSiteAcceptsToken` | 用 A 站上下文验证 A 站令牌 | 返回 token 且 usageCount++ |
| `deleteCascade_removesTokensForTask` | 删除任务 | `previewTokenMapper.deleteByPublishTaskId` 被调用 |
| `pendingTasks_siteIsolation_taskExecutesWithOwnSiteContext` | 定时执行 A 站任务 | 任务在自身站点上下文中执行（siteId=A） |

---

## 3. PreviewToken 安全模型

### 3.1 令牌生成

```
PreviewTokenService.createToken()
      │
      ├── SecureRandom(32 bytes) → 64 字符 hex（plainToken）
      ├── SHA-256(plainToken) → 64 字符 hex（tokenHash）
      ├── tokenHash 入库，plainToken 标记 @JsonIgnore
      └── plainToken 仅在创建响应中返回一次
```

| 安全属性 | 实现 |
|---|---|
| 不可逆 | 库中仅存 SHA-256 哈希，无法从数据库反推明文 |
| 不可序列化 | `@JsonIgnore` 确保 plainToken 不会意外暴露 |
| 一次性返回 | 仅在 `createToken()` / `generate-token` 时返回明文 |

### 3.2 令牌验证——五重校验

```
validateToken(plainToken, siteId, contentType, contentId)
      │
      ├── ① 哈希查找：SHA-256(plainToken) → 按 tokenHash 查库
      ├── ② 过期检查：expiresAt > now
      ├── ③ 次数检查：usageCount < maxUsage
      ├── ④ 站点隔离：token.siteId == requestSiteId     ← 核心
      └── ⑤ 内容绑定：token.contentType == contentType
                        && token.contentId == contentId
      全部通过 → usageCount++ → 返回 token
      任一失败 → Optional.empty()
```

### 3.3 角色限制（roleIds）

创建任务时可选传入 `roleIds` 参数：

```
POST /publish-task?roleIds=1,2,3
      │
      └── 创建 PreviewToken 后：
            for each roleId in roleIds:
                previewTokenRoleMapper.insert(token.id, roleId)
```

关联表 `preview_token_role`：

| 字段 | 说明 |
|---|---|
| `tokenId` | 预览令牌 ID |
| `roleId` | 允许的角色 ID |

> **语义**：若关联表中有记录，则只有拥有指定角色的用户才能使用该预览令牌。若无记录（`roleIds` 为空或未传），则不做角色限制——任何持有有效令牌的用户均可预览。

### 3.4 安全边界总结

```
┌────────────────────────────────────────────────────────┐
│                   PreviewToken 安全边界                  │
├───────────────┬────────────────────────────────────────┤
│ 密码学        │ 32 字节 SecureRandom + SHA-256 哈希存储  │
│ 传输          │ 明文仅返回一次，@JsonIgnore 防止二次泄露   │
│ 时效          │ expiresAt = publishDate + N hours       │
│ 次数          │ maxUsage = 100（硬编码）                  │
│ 站点隔离      │ token.siteId 必须 == 请求上下文 siteId    │
│ 内容绑定      │ contentType + contentId 精确匹配         │
│ 角色限制      │ 可选，通过 preview_token_role 关联        │
│ 自动清理      │ 每 5 分钟删除过期令牌                     │
│ 主动清理      │ 任务成功/取消/删除时级联删除               │
└───────────────┴────────────────────────────────────────┘
```

---

## 4. targetSiteId 的作用与边界

`targetSiteId` 标识内容**发布到的目标站点**，与 `siteId`（任务归属站点）可以不同。

| 场景 | siteId | targetSiteId | 说明 |
|---|---|---|---|
| 同站发布 | A | A | 最常见场景 |
| 跨站发布 | A | B | A 站管理员为 B 站安排发布 |

**校验规则**：
- `siteId`：Controller 层强制从上下文注入，做归属隔离
- `targetSiteId`：Service 层仅校验**站点存在性**（`siteService.select(targetSiteId) != null`），**不做权限校验**

> **二开风险点**：当前实现中，A 站用户只要知道 B 站的 `siteId`，即可为 B 站创建预约发布任务（`targetSiteId=B`）。如需限制跨站发布权限，需在 `PublishTaskService.insert()` 中增加 `targetSiteId` 的权限校验。`PublishTaskCrossSiteTest` **未覆盖** `targetSiteId` 的权限隔离场景。

---

## 5. 跨站场景矩阵

| 操作 | siteId 校验 | targetSiteId 校验 | 测试覆盖 |
|---|---|---|---|
| 创建任务 | 服务端注入，不可伪造 | 仅校验存在性 | 无跨站创建测试 |
| 查看/列表 | `dataInSite` 校验 | — | 有（`selectBySiteA_doesNotReturnSiteBTasks`） |
| 预览验证 | `token.siteId == requestSiteId` | — | 有（`siteMismatchRejectsToken`） |
| 执行发布 | 任务在自身 siteId 上下文中执行 | 发布到 targetSiteId | 有（`taskExecutesWithOwnSiteContext`） |
| 取消 | `dataInSite` 校验 | — | 无专项测试 |
| 删除 | 逐条 `dataInSite` 校验 | — | 有级联删除测试 |

---

## 6. 二开建议

1. **跨站发布权限**：若业务要求限制"只有特定角色才能跨站发布"，建议在 `insert()` 中增加 `targetSiteId != siteId` 时的额外权限检查。
2. **roleIds 扩展**：当前 `roleIds` 仅在创建时设置，不支持后续修改。如需动态调整可预览角色，需新增 API 端点（如 `PUT /{id}/token-roles`）。
3. **PreviewToken 审计**：当前不记录令牌的使用日志（仅递增 `usageCount`）。若需审计谁在何时使用了预览链接，需扩展 `validateToken()` 增加日志或审计表。
4. **maxUsage 可配置化**：当前硬编码为 `100`。若不同场景需要不同的使用次数限制，建议在 `PublishTask` 实体上增加 `previewMaxUsage` 字段。
