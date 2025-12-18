<div align="center">
<img src="images/logo.png" style="width: 20%" />
<h4><a href="https://github.com/gh0stkey/MaR">精准匹配，智能替换！</a></h4>
<h5>第一作者： <a href="https://github.com/gh0stkey">EvilChen</a><br>第二作者： <a href="https://github.com/0chencc">0chencc</a>（米斯特安全团队）</h5>
</div>

README 版本: \[[English](README.md) | [简体中文](README_CN.md)\]

## 项目介绍

**MaR**（Matcher and Replacement）是一款网络安全（漏洞挖掘）领域下的辅助型项目，主要用于对HTTP协议报文进行精准匹配和智能替换。它可以根据用户定义的规则，在满足特定条件时自动修改HTTP请求或响应内容，帮助安全研究人员在渗透测试过程中实现自动化的数据篡改。

**MaR**的设计思想来源于BurpSuite原生的Match and Replace功能，但提供了更加灵活和强大的规则配置能力，支持条件匹配、正则表达式、多作用域等高级特性。

**注意事项**:

1. MaR采用`Montoya API`进行开发，需要满足BurpSuite版本（>=2023.12.1）才能使用。

## 使用方法

**插件装载**: `Extender - Extensions - Add - Select File - Next`

初次装载`MaR`会自动创建配置文件`Config.yml`和规则文件`Rules.yml`：

1. Linux/Mac用户的配置文件目录：`~/.config/MaR/`
2. Windows用户的配置文件目录：`%USERPROFILE%/.config/MaR/`

除此之外，您也可以选择将配置文件存放在`MaR Jar包`的同级目录下的`/.config/MaR/`中，**以便于离线携带**。

### 实用技巧

你可以在HTTP请求/响应编辑器中选中文本，右键选择"Create MaR Rule"快速创建规则，选中的文本会自动填充到条件和匹配字段中。

<img src="images/create-rule.png" style="width: 80%" />

### 功能说明

**规则配置项**：

| 配置项 | 说明 |
| ------ | ---- |
| Name | 规则名称，用于标识规则 |
| C-Scope | 条件作用域，指定在哪个部分检查条件 |
| Relationship | 匹配关系，支持"Matches"（匹配）和"Does not match"（不匹配） |
| Condition | 条件内容，用于判断是否执行替换 |
| C-Regex | 条件是否使用正则表达式 |
| M-Scope | 替换作用域，指定在哪个部分执行替换 |
| Match | 匹配内容，要被替换的内容 |
| Replace | 替换内容，替换后的新内容 |
| M-Regex | 替换是否使用正则表达式 |

**支持的作用域**：

- `request` - 完整请求
- `request method` - 请求方法
- `request uri` - 请求URI
- `request header` - 请求头
- `request body` - 请求体
- `response` - 完整响应
- `response status` - 响应状态码
- `response header` - 响应头
- `response body` - 响应体

**配置管理**：

1. **Exclude suffix** - 排除指定后缀的请求，避免对静态资源进行处理
2. **Block host** - 排除指定域名的请求
3. **Scope** - 选择MaR生效的BurpSuite模块（Proxy、Repeater、Intruder等）

### 界面信息

| 界面名称 | 界面展示 |
| -------- | -------- |
| Rules（规则管理） | <img src="images/rules.png" style="width: 80%" /> |
| Config（配置管理） | <img src="images/config.png" style="width: 80%" /> |

## 使用场景

1. **参数篡改** - 根据条件自动修改请求参数值
2. **响应修改** - 修改响应内容以绕过前端校验
3. **请求注入** - 自动添加或修改请求/响应头

## 赞赏榜单

感谢各位对项目的赞赏，以下名单基于赞赏时间进行排序，不分先后，如有遗留可联系项目作者进行补充。

| ID       | 金额     |
| -------- | -------- |
| 柯林斯  | 888.00 元 |
| JaveleyQAQ | 50.00 元 |
| Kite | 20.00 元 |
| ArG3 | 66.00 元 |
| 祝祝 | 288.00 元 |
| 洺熙 | 88.88 元 |
| 秋之 | 99.00 元 |
| Redbag | 66.00 元 |
| 毁三观大人 | 200.00 元 |

## 最后

如果你觉得MaR好用，可以打赏一下作者，给作者持续更新下去的动力！

<div align=center>
<img src="images/reward.jpeg" style="width: 30%" />
</div>
