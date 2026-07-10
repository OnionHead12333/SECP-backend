import json
import sys
from pathlib import Path


def main():
    text = sys.argv[1] if len(sys.argv) > 1 else ""
    text = text.strip()
    high = any(k in text for k in ["胸痛", "胸闷", "呼吸困难", "喘不上气", "意识不清", "昏迷", "突然说不清话", "口角歪斜", "一侧肢体无力", "偏瘫", "剧烈头痛", "大出血", "便血", "黑便", "摔倒后不能动", "持续高热", "抽搐", "心悸严重", "血压特别高"])
    result = {
        "riskLevel": "high" if high else "low",
        "departmentTag": "急诊科" if high else "全科医学科",
        "confidence": 0.92 if high else 0.64,
        "message": "预测成功"
    }
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
