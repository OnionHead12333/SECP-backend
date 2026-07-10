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
    if "department_tag" in df.columns and df["department_tag"].astype(str).str.strip().nunique() > 1:
        return df["department_tag"].astype(str).str.strip()
    if "department_id" in df.columns:
        return df["department_id"].astype(str).fillna("general")
    def label_row(t):
        txt = clean_text(t)
        if any(k in txt for k in ["胸痛", "胸闷", "心悸"]):
            return "急诊科"
        if any(k in txt for k in ["膝盖", "关节", "骨头", "腰疼"]):
            return "骨科"
        return "全科医学科"
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
    joblib.dump(clf, output_dir / "department_model.pkl")
    joblib.dump(vectorizer, output_dir / "department_vectorizer.pkl")
    print(json.dumps({"accuracy": acc, "macro_f1": f1, "model_type": "department"}, ensure_ascii=False))
    print(f"accuracy:{acc}")
    print(f"macro_f1:{f1}")


if __name__ == "__main__":
    main()
