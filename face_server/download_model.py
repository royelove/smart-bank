# -*- coding: utf-8 -*-
"""
下载 InsightFace buffalo_l 模型到默认模型目录（国内走 ModelScope 镜像）。
只需执行一次；换机器重装时再跑。
"""
from modelscope import snapshot_download

snapshot_download('fireicewolf/buffalo_l', local_dir=r'C:\Users\26656\.insightface\models\buffalo_l')
print('模型下载完成')
