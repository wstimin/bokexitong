# 正式安装包

安装包不再直接提交到 Git 仓库，避免仓库中的大文件与正式版本不同步。

请从 [GitHub Releases](https://github.com/wstimin/bokexitong/releases/latest) 下载：

- `bokexitong-linux.tar.gz`：Linux / Docker Compose 一键部署包。
- `bokexitong-web.tar.gz`：1Panel / 宝塔 Web 包。
- `update-manifest.json`：后台一键更新清单。

仓库中的 `update-manifest.json` 是后台检查更新的稳定入口，其中的 SHA-256 必须与对应 Release 附件一致。
