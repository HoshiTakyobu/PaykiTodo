# PaykiTodo Release Signing Template

这个文件是 **签名信息填写模板**。你把它按实际情况填好之后，我下一步就可以据此：

1. 为 PaykiTodo 生成正式 release keystore；
2. 生成本地 `keystore.properties`；
3. 把 Gradle 的 release 签名配置接起来；
4. 后续按这个签名稳定发布 release / tag / GitHub Release。

---

## 一、填写说明

- 下面带 `[待填写]` 的内容，请你后续改成真实信息。
- **密码、私钥、keystore 文件不要提交到 Git 仓库。**
- 这个模板文件可以提交；但如果你把真实密码写进来，建议改成仅本地保存。

---

## 二、签名基本信息

### 1. 是否使用已有签名

- 是否已有 keystore：`[待填写：是 / 否]`
- 如果已有 keystore，本地路径：`[待填写]`
- 如果已有 keystore，是否继续沿用：`[待填写：是 / 否]`

### 2. 新签名文件生成目标

- keystore 文件名：`[待填写，建议：PaykiTodo-release.jks]`
- keystore 本地保存目录：`[待填写，建议：release/]`
- key alias：`[待填写，建议：paykitodo]`
- 有效期（天）：`[待填写，建议：10000]`

### 3. 证书主体信息（生成 keystore 时需要）

- 姓名 / 负责人名称（CN）：`[待填写]`
- 组织 / 项目名（OU）：`[待填写，建议：PaykiTodo]`
- 公司 / 团队名（O）：`[待填写]`
- 城市（L）：`[待填写]`
- 省 / 州（ST）：`[待填写]`
- 国家代码（C）：`[待填写，两个大写字母，例如 CN]`

### 4. 密码信息

- keystore 密码：`[待填写]`
- key 密码：`[待填写]`
- 是否允许 key 密码与 keystore 密码相同：`[待填写：是 / 否]`

---

## 三、建议生成后的本地文件结构

建议最终在项目根目录本地形成：

- `keystore.properties`
- `release/PaykiTodo-release.jks`

这两个文件都不应提交到仓库。

---

## 四、keystore.properties 模板

等你把上面的信息确认完后，我会按这个结构生成本地配置文件：

```properties
storeFile=release/PaykiTodo-release.jks
storePassword=REPLACE_WITH_STORE_PASSWORD
keyAlias=paykitodo
keyPassword=REPLACE_WITH_KEY_PASSWORD
```

如果你后续决定换路径或 alias，我会同步改成你的实际值。

---

## 五、接入原则

1. 仓库中不硬编码真实签名密码。
2. `keystore.properties` 仅保留在本机。
3. release keystore 不进入版本库。
4. 如需团队协作，签名材料通过单独安全渠道保存或分发。
5. 如已有线上安装用户，后续必须持续使用同一签名，否则无法覆盖安装升级包。

---

## 六、填好之后我会继续做什么

你把本文件填好后，我会继续完成：

1. 生成 keystore；
2. 补 `.gitignore` 保护签名文件；
3. 在 Gradle 中接入 release signing；
4. 验证 `assembleRelease` / `bundleRelease`；
5. 产出正式可发布的 release 包。
