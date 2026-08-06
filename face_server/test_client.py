# -*- coding: utf-8 -*-
"""
人脸识别微服务测试脚本
用法:
    1. 确保服务已启动: .venv\\Scripts\\python.exe app.py
    2. 功能测试(用自带测试图): .venv\\Scripts\\python.exe test_client.py
    3. 真实效果测试(需准备照片):
       - imgs/me/     放本人照片 3~5 张(不同角度/光线)
       - imgs/others/ 放他人照片 3~5 张
       然后: .venv\\Scripts\\python.exe test_client.py --realdir
"""
import argparse
import base64
import os
import sys

import requests

BASE = "http://localhost:8001/api/face"


def b64(path):
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode()


def post_feature(img_b64):
    r = requests.post(f"{BASE}/feature", json={"image": img_b64}, timeout=30)
    return r.status_code, r.json()


def post_compare(img_b64, feature):
    r = requests.post(f"{BASE}/compare", json={"image": img_b64, "feature": feature}, timeout=30)
    return r.status_code, r.json()


def test_functional(test_img):
    """接口功能用例"""
    print("=" * 60)
    print("[1] 接口功能测试")
    print("=" * 60)
    results = []
    img = b64(test_img)

    def case(name, ok, detail=""):
        results.append((name, ok))
        print(f"  {'PASS' if ok else 'FAIL'}  {name}  {detail}")

    # 正常提取特征
    st, body = post_feature(img)
    ok = st == 200 and body.get("code") == 0 and len(body.get("feature", [])) == 512
    case("正常提取特征(512维)", ok, f"status={st}")

    feat = body.get("feature", [])
    # 同图比对
    st, body = post_compare(img, feat)
    case("同图比对 pass=True", st == 200 and body.get("pass") is True,
         f"score={body.get('score')}")

    # 缺 feature
    r = requests.post(f"{BASE}/compare", json={"image": img}, timeout=30)
    case("缺 feature 返回400", r.status_code == 400, f"msg={r.json().get('msg')}")

    # 缺 image
    r = requests.post(f"{BASE}/feature", json={}, timeout=30)
    case("缺 image 返回400", r.status_code == 400, f"msg={r.json().get('msg')}")

    # 非法图片(非base64)
    r = requests.post(f"{BASE}/feature", json={"image": "not-a-real-image!!"}, timeout=30)
    case("非法base64返回400", r.status_code == 400)

    # 空字节图
    r = requests.post(f"{BASE}/feature", json={"image": base64.b64encode(b"xx").decode()}, timeout=30)
    case("损坏图片返回400", r.status_code == 400)

    failed = [n for n, ok in results if not ok]
    print(f"  => 功能用例: {len(results)-len(failed)}/{len(results)} 通过")
    return results


def test_real(feature_cb=post_feature, compare_cb=post_compare):
    """真实照片: 正负样本对 + 阈值推荐"""
    me_dir = os.path.join(os.path.dirname(__file__), "imgs", "me")
    other_dir = os.path.join(os.path.dirname(__file__), "imgs", "others")
    if not (os.path.isdir(me_dir) and os.path.isdir(other_dir)):
        print("没有 imgs/me 与 imgs/others 目录, 跳过真实效果测试")
        return None

    me_files = sorted(os.listdir(me_dir))
    other_files = sorted(os.listdir(other_dir))
    if len(me_files) < 2:
        print(f"本人照片至少2张(当前{len(me_files)}张), 跳过真实效果测试")
        return None

    print("=" * 60)
    print("[2] 真实效果测试 (正负样本对相似度分布)")
    print("=" * 60)

    me_feats = {}
    for fn in me_files:
        p = os.path.join(me_dir, fn)
        if not fn.lower().endswith((".jpg", ".jpeg", ".png")):
            continue
        st, body = post_feature(b64(p))
        if st == 200 and body.get("code") == 0:
            me_feats[fn] = body["feature"]
            print(f"  注册特征 OK: {fn}")

    if len(me_feats) < 2:
        print("  可用的本人照片不足, 跳过")
        return None

    # 正样本对: 本人 x 本人
    pos = []
    me_names = list(me_feats)
    for i, a in enumerate(me_names):
        for b_name in me_names[i + 1:]:
            st, body = post_compare(b64(os.path.join(me_dir, b_name)), me_feats[a])
            if body.get("code") == 0:
                pos.append(body["score"])
    # 负样本对: 本人 x 他人
    neg = []
    for a in me_names:
        for fn in other_files:
            p = os.path.join(other_dir, fn)
            if not fn.lower().endswith((".jpg", ".jpeg", ".png")):
                continue
            st, body = post_compare(b64(p), me_feats[a])
            if body.get("code") == 0:
                neg.append(body["score"])

    print(f"\n  正样本对(本人x本人) {len(pos)} 对: min={min(pos):.3f} avg={sum(pos)/len(pos):.3f} max={max(pos):.3f}")
    if neg:
        print(f"  负样本对(本人x他人) {len(neg)} 对: min={min(neg):.3f} avg={sum(neg)/len(neg):.3f} max={max(neg):.3f}")

    # 推荐阈值: 让正样本全过 + 负样本全拒
    if pos and neg:
        rec = (min(pos) + max(neg)) / 2
        far_at_rec = sum(1 for s in neg if s > rec) / len(neg)
        frr_at_rec = sum(1 for s in pos if s <= rec) / len(pos)
        print(f"\n  [推荐阈值] {rec:.3f}  (FAR={far_at_rec:.1%}, FRR={frr_at_rec:.1%})")
        print("  建议: 银行场景更看重安全, 可适当上调阈值")
    return pos, neg


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--realdir", action="store_true", help="同时运行真实照片测试")
    parser.add_argument("--img", default=os.path.join(os.path.dirname(__file__), "astronaut_test.jpg"))
    args = parser.parse_args()

    try:
        requests.get("http://localhost:8001/api/face/health", timeout=5)
    except requests.exceptions.RequestException:
        print("错误: 服务未启动。请先运行: start_face_server.bat")
        sys.exit(1)

    test_functional(args.img)
    if args.realdir:
        test_real()
