# 1.17.11 更新链路阶段验收

2026-11-01从5d54c96恢复，继承前轮预升版本，完成Android1.17.11/403、Windows1.17.11.0；Room维持34。原七项目标继续，循环绝对提醒归属、全部未公开历史处理和最终推送仍待完成。源码、当地成品和公开Release分别记录，公开Release仍为v1.15.18。

## 已实现

两端从指定GitHub项目稳定Release判断新版，按数字比较版本，排除draft/prerelease与不符资产。保留手动检查；自动检查12小时节流，不打断编辑。Android设置目录提示新版，Windows页脚提供查看更新入口。

Android Release使用系统DownloadManager，保存下载ID与目标版本，支持进程重新创建后恢复、进度、取消和失败重试。安装前核对APK大小、可用的GitHub哈希、包名、版本码、版本名和签名，支持合法证书轮换；Debug不能覆盖公开Release。未知来源许可通过系统设置，返回后再次安装，FileProvider仅开放更新目录，无额外存储权限。下载状态与安装包不进入系统迁移备份。

Windows支持签名清单、稳定安装ID灰度、手动提前参与、后台增量下载、复制未变化文件与全量目标校验。损坏增量或基线不匹配回退完整包，准备失败不切换活动版本。新旧版本目录分开，基础安装快捷方式启动独立启动器；启动nonce确认、45秒超时/进程退出/中断恢复及损坏文件回退已实现。MSI和便携构建都写入增量基线清单。用户资料、配置和备份不随程序版本切换。

发布工具生成完整zip、变化文件zip和RSA-PSS/SHA-256签名清单；私钥只在忽略的user/update-signing目录，源码只包含公钥。协议与操作见[更新协议](WINDOWS_UPDATE_PROTOCOL.md)。

## 检查与证据

过程输出在Scratch/PaykiTodo/updates-1.17.11，私有恢复副本在Backups/PaykiTodo/updates-1.17.11。

| 范围 | 结果 |
| --- | --- |
| Android完整构建 | 最终android-notice-final.log：258单测通过；Lint0错误/137警告/30信息；Debug、AndroidTest与签名Release构建通过 |
| Android运行检查 | 5项不同设备用例：更新控件3项（包括无最新发布信息仍可取消已有下载）、真实签名APK身份策略1项、系统下载ID重新创建及取消1项；重试不重复计数 |
| Windows回归 | 104项不同回归通过，2项Android联动夹具未启用；新增更新协议13项及2项正式包启动/nonce确认；完整回归、成品及安装后定向检查的重叠不重复计数 |
| Windows界面 | 1条14阶段独立UIA流程通过；源码与安装验收重跑为相同流程，不重复计数 |
| 独立启动器 | launcher/065e5b180a844d10ab68e97ef13085e8/result.json：签名维护工具生成的模拟新版本完成成功切换、失败、中断和损坏回退4种进程检查；真实WPF包另验证Loaded确认与独立数据保留 |
| 增量 | DesktopUpdaterTests核对只请求delta、基线/损坏回退full、目标文件完整性、越界/签名拒绝、灰度身份稳定、nonce及资料保留。实际维护工具生成完整/增量资产；MSI基线文件哈希与安装清单一致 |
| MSI生命周期 | installer-verified/install-66d50ff25f7d4000bfe8d91a2a8028eb/result.json：真实1.17.10升级、修复、484个payload文件、安装EXE、快捷方式、卸载及普通用户资料保留全部通过。最终成品固定后顺序检查，安装阶段仅定向重复两项成品检查 |
| 真实GitHub只读 | 稳定Release仍为v1.15.18，只有对应Release APK；没有上传测试资产或创建未来版本Release |
| 数据保留 | data-preservation-final.json：user0 646/646、user10 148/148文件哈希一致，零新增；user11测试数据、失败与最终副本保留 |
| 签名 | 最终Release签名有效，与1.17.10 Release证书相同；真实Debug/Release签名不兼容策略已验证 |

首次增量失败未进入完整包回退，回归找出InvalidDataException过滤遗漏并已修正。初期测试缺少using、设置提示缺少Compose导入，均在最终构建前修正。APK夹具通过ADB shell stdin只留下16字节，改用push与目标用户cp后身份检查通过；这是夹具传输失败，不是APK签名失败。UIA一次既有同步夹具竞争和安装回归一次HttpListener端口拒绝，针对性复验通过。微软下载入口等待较长，后续构建使用经过微软Authenticode验证的已有引导安装器，并给在线路径加60秒超时。旧快捷方式断言及构建/安装并发造成的包标识错误均保留，最终固定MSI后完成验收。

新Lint警告是IO线程持久提交、Application上下文单例及最低API冗余判断；本轮没有扩大既有运行数据清理范围。测试不使用真实手机、真实用户账号或额外生产部署。

## 成品与边界

最新Debug APK为app/build/outputs/apk/debug/PaykiTodo-1.17.11-debug.apk；签名Release为app/build/outputs/apk/release/PaykiTodo-1.17.11-release.apk；MSI为desktop/installer-output/PaykiTodo-1.17.11-windows-x64.msi。交付副本、完整Windows-portable、更新资产、说明和哈希在Deliverables/PaykiTodo/1.17.11。

最终Windows payload为desktop/obj/installer/1.17.11-edb364f892ee40dc966fcb7ff82993ca/payload，不用旧desktop/dist冒充新版。MSI SHA-256为0F257FA8C5FF69CC80A25CA602180E09FE7824AF2C7C3AE3950A664A180C098C。

没有公开未来Release可做真实线上自动升级。本次下载器/启动器使用模拟发布与系统组件验证；真实手机未知来源手动授权、系统确认安装及OEM后台行为未作为通过事实。Windows模拟更新进程与真实WPF启动确认分别验证，不将模拟程序说成新正式版本。当前没有自动删除旧更新缓存，失败副本保留。

模拟器复用emulator-5554/Pixel_8/PID20332，无窗口；安装最终Debug/AndroidTest，仅user11运行测试，最后返回user10主页，应用与测试停止，核对AVD后恢复原暂停。原用户业务数据不变。未push/tag/Release/服务器部署，原七项目标未完成。
