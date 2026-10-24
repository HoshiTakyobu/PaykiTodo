# 1.17.1 参与者、详情与回应验收

日期：2026-10-25。Android `1.17.1 / versionCode 393`，Windows `1.17.1.0`，Room `33`。本轮完成本地实现、构建和验证，没有推送、发布或部署生产服务器。

## 功能范围

本版本在 [1.17.0 日历与账号更新](VALIDATION_1.17.0.md) 上补齐日程参与者与完整协作详情。日历主体、创建调整、多日历、循环、全天提醒、时区、登录服务器选择与紧凑账号排版继续保留；强提醒仍使用单个窗口与队列。

| 项目 | 本轮结果 | 直接证据与边界 |
| --- | --- | --- |
| 参与者选择 | 从当前日历已接受的成员具名多选，排除失效成员；嵌套完成只更新父层草稿，取消不保存，切日历重新选择 | Android 4项参与者控件用例；Windows实际编辑控件与选择回归 |
| 组织者与名单 | 详情显示日历所有者作为组织者、参与者及回应；PRIVATE只读成员仅显示服务器投影，不从日历成员表补全名单 | 两端完整/受限详情截图、共享协议与权限回归 |
| 回应权威 | 编辑保留已有回应，旧草稿不回写旧回应；新增参与者和新循环实例保持NEEDS_ACTION，复制清除原参与者与回应 | Android单测和第12项Room契约；Windows实例身份、编辑和复制回归 |
| 回应与同步冲突 | 服务器确认的本人回应先显示；不推进旧同步版本以绕过冲突，不覆盖待同步编辑；请求期间修改保留应用私有安全副本，详情刷新保留操作提示 | Android同步策略/回应单测；Windows真实本地HTTP延迟、dirty/CAS和安全副本测试 |
| 来源与权限 | 详情、编辑、复制和提醒重新检查当前账号、服务器和日历权限；删除或失权后关闭旧内容 | 两端访问守卫、撤权和跨来源回归；Android正常冷启动检查 |
| 全天通知日期 | 按日程时区和排他结束日期显示，避免跨日期变更线偏移一天 | 纽约DST与Pacific/Kiritimati单测 |

## 执行结果

- Android：Debug、AndroidTest、签名Release、单测与Lint任务通过。222项单测，0失败/错误/跳过；Lint 0 errors / 133 warnings；Release APK v2签名验证通过，包名与版本核对为 `com.paykitodo.app / 1.17.1 / 393`。
- Android设备：`emulator-5554 / Pixel_8`，12项独立Room数据契约和18项不同UI用例最终通过。首轮UI有1项因父层与弹层同名“完成”选择歧义失败；修正精确控件选择，并将回应信息保存在详情父层后，针对性复跑4项参与者用例全部通过。重复执行不增加不同用例数。
- Android初次构建因新增日期测试缺少模型必填参数失败，补齐后构建通过；之后的最终构建包含详情提示与控件选择修正。保留初次日志，不将失败写成成功。
- Windows：最终安装包验证55项回归，0失败/跳过，包含实际安装EXE启动；另1条14阶段隔离UIA流程通过。8项参与者/共享专项属于上述回归的子集，不额外相加。
- Windows安装：真实旧 `1.17.0` → `1.17.1` 升级、路径记忆、修复、全部payload哈希、启动、卸载及真实用户目录资料不变通过。隔离控件测试与合成展示资料不等于物理键鼠或生产账号全链路验收。
- 服务端：`python -m unittest discover -s server -p 'test*.py' -v`，21项通过；测试账号与数据库独立于生产。

## 安装与可视化产物

| 产物 | 路径 | SHA-256 |
| --- | --- | --- |
| Android Debug | `app/build/outputs/apk/debug/PaykiTodo-1.17.1-debug.apk` | `B0B6735A4B4B76DF6FD67AB07A4BE6310747AB3A67A2FE584750B6190D5B87B1` |
| Android签名Release | `app/build/outputs/apk/release/PaykiTodo-1.17.1-release.apk` | `A08DD3BC1934BC36C1413FCCF751AF902EA5DC7717079FB7746A4AC77F9F5BF9` |
| Windows中文MSI | `desktop/installer-output/PaykiTodo-1.17.1-windows-x64.msi` | `6DBC4E5DD47B17A172F10BEC556C5CC33CF1A5023601EBF8074C84C1750BB39C` |
| Windows便携EXE | `desktop/dist/PaykiTodo.exe` | `84AA67E3E1D39DA9739C48A9E78E115C8D9B7A0B2498C7A47A1C9E51CF7342D0` |
| Windows主程序集 | `desktop/dist/PaykiTodo.dll` | `5346953E532D42F122C1F167B69AA7BA4635902996EE8F9260BFFC43A94C8821` |

Windows EXE/DLL文件版本为 `1.17.1.0`，DLL与MSI内文件一致；便携使用必须保留整个 `dist` 目录。MSI为80,098,368字节。真实控件预览在 `G:\Workspace\Deliverables\PaykiTodo\1.17.1\界面预览.html`，精选22张Android/Windows截图使用合成展示资料，截图索引记录来源与哈希；1.17.0预览保留原位。

## 数据保护与证据

- Android验收前备份 `G:\Workspace\Backups\PaykiTodo\emulator-before-1.17.1-20261025-030232`（646文件），验收后 `emulator-after-1.17.1-20261025-034953`（644文件）。Room保持33，15张业务表原记录无修改或遗漏；账号、认证和配置文件哈希不变。原待办表为空，带任务的迁移与循环正确性由独立Room契约验证。
- 5项WebView代码缓存/指标在运行时被清理，启动状态、性能与WebView运行文件正常变化，新增隔离迁移测试数据库和锁文件。没有为恢复旧缓存哈希而覆盖应用资料；完整原始备份仍保留。
- 最终Debug已安装并正常冷启动。应用与测试包最后停止，模拟器保持运行；Wi-Fi与移动数据恢复为验收前的启用状态。
- Android构建、签名、数据/界面日志、13张精选原图与比较报告：`G:\Workspace\Scratch\PaykiTodo\calendar-1.17.1`。
- Windows最终包证据：`windows-installer-final-rsvp/install-cfa43b1c46f246578f57f4efbd9dbde0/result.json`及`installed-app`。4张参与者图仅修改合成日历展示名称后通过1个隔离控件用例重新捕获，位于`windows-fixture-recapture`；正式EXE/DLL/MSI哈希未变，没有重复计数。

## 使用边界

共享功能需要服务器提供 `/v3` 日历接口；本轮未部署生产服务器，旧服务器会提示功能尚不可用。生产账号手机到电脑协作、手机OEM锁屏/振动/长待机、Windows物理输入完整流程仍未验证。Windows到点提醒仍要求程序运行。公开下载保持 `v1.15.18`，本轮产物为本地验证版。
