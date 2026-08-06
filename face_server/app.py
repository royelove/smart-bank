# -*- coding: utf-8 -*-
"""
本地人脸识别微服务（InsightFace / ArcFace）
- POST /api/face/feature : 传入人脸图片，返回 512 维归一化特征向量（注册用）
- POST /api/face/compare  : 传入人脸图片 + 注册特征，返回余弦相似度与是否通过（登录用）
- GET  /api/face/health   : 健康检查
"""
import base64
import logging
import os

import cv2
import numpy as np
from flask import Flask, jsonify, request

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("face_server")

app = Flask(__name__)

# 识别通过阈值（余弦相似度），请用自己数据标定
SCORE_THRESHOLD = float(os.environ.get("FACE_THRESHOLD", "0.45"))

_model = None


def get_model():
    """延迟加载模型，首次调用时初始化（自动下载 buffalo_l 模型包）"""
    global _model
    if _model is not None:
        return _model
    import insightface
    from insightface.app import FaceAnalysis

    # buffalo_l 包含检测(det_10g) + 识别(w600k_r50) + 5点关键点
    _model = FaceAnalysis(
        name="buffalo_l",
        allowed_modules=["detection", "recognition"],
    )
    _model.prepare(ctx_id=-1, det_size=(640, 640))  # ctx_id=-1 使用 CPU
    log.info("InsightFace 模型加载完成, 阈值=%s", SCORE_THRESHOLD)
    return _model


def decode_image(raw):
    """把请求体字节解码为 BGR 图片，失败返回 None"""
    if not raw:
        return None
    arr = np.frombuffer(raw, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        return None
    # 限制最大边长，加速推理
    h, w = img.shape[:2]
    scale = min(1.0, 1280.0 / max(h, w))
    if scale < 1.0:
        img = cv2.resize(img, (int(w * scale), int(h * scale)))
    return img


def embed(img):
    """检测并对最大人脸提取 512 维归一化特征"""
    faces = get_model().get(img)
    if not faces:
        return None
    faces.sort(key=lambda f: (f.bbox[2] - f.bbox[0]) * (f.bbox[3] - f.bbox[1]), reverse=True)
    return faces[0].normed_embedding


@app.get("/api/face/health")
def health():
    return jsonify({"code": 0, "status": "ok", "threshold": SCORE_THRESHOLD})


@app.post("/api/face/feature")
def face_feature():
    payload = request.get_json(silent=True) or {}
    img_b64 = payload.get("image")
    if not img_b64:
        return jsonify({"code": 400, "msg": "缺少图片(image, base64)"}), 400
    try:
        raw = base64.b64decode(img_b64)
    except Exception:  # noqa: BLE001
        return jsonify({"code": 400, "msg": "image 不是合法的 base64"}), 400
    img = decode_image(raw)
    if img is None:
        return jsonify({"code": 400, "msg": "无法解析图片"}), 400
    try:
        vec = embed(img)
    except Exception as e:  # noqa: BLE001
        log.error("特征提取失败", exc_info=True)
        return jsonify({"code": 500, "msg": f"服务异常: {e}"}), 500
    if vec is None:
        return jsonify({"code": 404, "msg": "未检测到人脸"}), 404
    return jsonify({"code": 0, "feature": vec.tolist()})


@app.post("/api/face/compare")
def face_compare():
    payload = request.get_json(silent=True) or {}
    reg_feature = payload.get("feature")
    if not reg_feature or not isinstance(reg_feature, list) or len(reg_feature) != 512:
        return jsonify({"code": 400, "msg": "缺少合法的注册特征(feature)"}), 400
    img_b64 = payload.get("image")
    if not img_b64:
        return jsonify({"code": 400, "msg": "缺少图片(image, base64)"}), 400
    try:
        raw = base64.b64decode(img_b64)
    except Exception:  # noqa: BLE001
        return jsonify({"code": 400, "msg": "image 不是合法的 base64"}), 400
    img = decode_image(raw)
    if img is None:
        return jsonify({"code": 400, "msg": "无法解析图片"}), 400
    try:
        live_vec = embed(img)
    except Exception as e:  # noqa: BLE001
        log.error("比对失败", exc_info=True)
        return jsonify({"code": 500, "msg": f"服务异常: {e}"}), 500
    if live_vec is None:
        return jsonify({"code": 404, "msg": "未检测到人脸"}), 404

    reg_vec = np.asarray(reg_feature, dtype=np.float64)
    live_vec = np.asarray(live_vec, dtype=np.float64)
    # 两个归一化向量的点积 == 余弦相似度
    score = float(np.dot(reg_vec, live_vec))
    return jsonify({"code": 0, "score": round(score, 6), "pass": score > SCORE_THRESHOLD})


if __name__ == "__main__":
    port = int(os.environ.get("FACE_PORT", "8001"))
    app.run(host="0.0.0.0", port=port)
