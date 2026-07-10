import json
import sys
from pathlib import Path

import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.model_selection import train_test_split


def clean_text(text):
    if not isinstance(text, str):
        return ""
    return " ".join(text.strip().split())


def build_label(df):
    if "risk_level" in df.columns and df["risk_level"].astype(str).str.lower().nunique() > 1:
        return df["risk_level"].astype(str).str.lower()
    high_keywords = ["胸痛", "胸闷", "呼吸困难", "喘不上气", "意识不清", "昏迷", "突然说不清话", "口角歪斜", "一侧肢体无力", "偏瘫", "剧烈头痛", "大出血", "便血", "黑便", "摔倒后不能动", "持续高热", "抽搐", "心悸严重", "血压特别高"]
    def label_row(t):
        txt = clean_text(t)
        return "high" if any(k in txt for k in high_keywords) else "low"
    return df["question"].astype(str).map(label_row)


def main():
    dataset = Path(sys.argv[1] if len(sys.argv) > 1 else "backend/data/medical_qa_dataset.csv")
    output_dir = Path(sys.argv[2] if len(sys.argv) > 2 else "backend/data/ai-models")
    output_dir.mkdir(parents=True, exist_ok=True)
    df = pd.read_csv(dataset)
    q = df["question"].astype(str).fillna("") if "question" in df.columns else pd.Series([""] * len(df))
    qs = df["question_seg"].astype(str).fillna("") if "question_seg" in df.columns else pd.Series([""] * len(df))
    ans = df["answer_seg"].astype(str).fillna("") if "answer_seg" in df.columns else pd.Series([""] * len(df))
    text = q + " " + qs + " " + ans
    y = build_label(df)
    X_train, X_test, y_train, y_test = train_test_split(text.map(clean_text), y, test_size=0.2, random_state=42, stratify=y if y.nunique() > 1 else None)
    vectorizer = TfidfVectorizer(max_features=5000, ngram_range=(1, 2))
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)
    clf = LogisticRegression(max_iter=1000)
    clf.fit(X_train_vec, y_train)
    pred = clf.predict(X_test_vec)
    acc = accuracy_score(y_test, pred)
    f1 = f1_score(y_test, pred, average="macro")
    joblib.dump(clf, output_dir / "risk_triage_model.pkl")
    joblib.dump(vectorizer, output_dir / "risk_triage_vectorizer.pkl")
    print(json.dumps({"accuracy": acc, "macro_f1": f1, "model_type": "risk"}, ensure_ascii=False))
    print(f"accuracy:{acc}")
    print(f"macro_f1:{f1}")


if __name__ == "__main__":
    main()
