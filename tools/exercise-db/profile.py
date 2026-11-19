#!/usr/bin/env python3
"""Phase 0 profiler for the exercise-database rebuild.

Reads `tools/exercise-db/data/megaGymDataset.csv` and reports everything the
mapping decisions need:

  * shape, columns, missing values
  * value sets + counts for Type / BodyPart / Equipment / Level
  * exact and case-insensitive duplicate titles
  * slug collisions under RepIo's slugify rules
  * name junk: brand prefixes, trailing punctuation, plan-number prefixes
  * BodyPart sanity: rows whose name keywords disagree with the BodyPart
  * overlap between the dataset and the current app pool in WorkoutTemplates.kt

Run from the repository root:
    python tools/exercise-db/profile.py
"""

from __future__ import annotations

import collections
import pathlib
import re
import sys

import pandas as pd

ROOT = pathlib.Path(__file__).resolve().parents[2]
CSV = pathlib.Path(__file__).resolve().parent / "data/megaGymDataset.csv"
TEMPLATES = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt"

BRAND_PREFIXES = [
    "FYR2 ", "FYR ", "HM ", "AM ", "UNS ", "KV ", "TBS ",
    "MetaBurn ", "Holman ", "Tyler Holt ", "Dumbbell Fix ",
    "Dumbbell Fix ", "30 ",
]
VARIATION_SUFFIXES = [" - Gethin Variation", " - Variation Gethin", " - With Bands"]


def slugify(name: str) -> str:
    """Mirror of ExerciseLocalization.slugify (ASCII letters/digits kept)."""
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return re.sub(r"_+", "_", slug)


def current_pool() -> list[str]:
    """Extract the built-in pool names from WorkoutTemplates.kt."""
    src = TEMPLATES.read_text(encoding="utf-8")
    start = src.index("private val exerciseEquipment: Map<String, String>")
    end = src.index("getEquipmentTableNames", start)
    categories = {"All", "Bodyweight", "Dumbbell", "Barbell", "Gym"}
    names, seen = [], set()
    for literal in re.findall(r'"([^"]+)"', src[start:end]):
        if literal in categories or literal in seen:
            continue
        seen.add(literal)
        names.append(literal)
    return names


def value_counts(df: pd.DataFrame, column: str, top: int = 40) -> list[tuple[str, int]]:
    vc = df[column].fillna("(blank)").value_counts()
    return list(vc.head(top).items())


def norm_title(title: str) -> str:
    """Strip obvious junk to expose near-duplicates: variation suffixes,
    trailing punctuation, brand prefixes."""
    t = title.strip()
    for suf in VARIATION_SUFFIXES:
        if t.lower().endswith(suf.lower()):
            t = t[: -len(suf)]
    t = t.rstrip("- ").strip()
    changed = True
    while changed:
        changed = False
        for pref in BRAND_PREFIXES:
            if t.lower().startswith(pref.lower()):
                t = t[len(pref):].strip()
                changed = True
    return t


def main() -> int:
    df = pd.read_csv(CSV, index_col=0)
    print(f"rows: {len(df)}   columns: {list(df.columns)}\n")

    print("== missing values ==")
    missing = df.isna().sum()
    for col, n in missing.items():
        print(f"  {col:<12} {n}")
    no_desc_pct = 100.0 * missing.get("Desc", 0) / len(df)
    print(f"  -> {no_desc_pct:.1f}% of rows have no description\n")

    for col in ("Type", "BodyPart", "Equipment", "Level"):
        print(f"== {col} values ==")
        for value, count in value_counts(df, col):
            print(f"  {value:<22} {count}")
        print()

    print("== duplicates ==")
    exact = df["Title"].duplicated().sum()
    ci = df["Title"].str.lower().str.strip().duplicated().sum()
    normed = df["Title"].map(norm_title).str.lower()
    norm_dupes = normed.duplicated().sum()
    print(f"  exact duplicate titles          : {exact}")
    print(f"  case-insensitive duplicates     : {ci}")
    print(f"  after junk-strip normalization  : {norm_dupes}")
    print(f"  unique titles                   : {df['Title'].str.lower().str.strip().nunique()}")
    print(f"  unique after normalization      : {normed.nunique()}")

    collisions = normed[normed.duplicated(keep=False)].sort_values()
    if not collisions.empty:
        print("\n  top collision groups (normalized name -> raw titles):")
        groups = collections.defaultdict(set)
        for raw, norm in zip(df["Title"], normed):
            if norm in set(collisions):
                groups[norm].add(raw)
        for norm, raws in list(sorted(groups.items(), key=lambda kv: -len(kv[1]))[:12]):
            print(f"    {norm}  ({len(raws)}): {sorted(raws)[:4]}")

    print("\n== slug collisions (RepIo slugify, full titles) ==")
    slug_map = collections.defaultdict(list)
    for title in df["Title"]:
        slug_map[slugify(title)].append(title)
    coll = {s: t for s, t in slug_map.items() if len(t) > 1}
    print(f"  colliding slugs: {len(coll)}")
    for slug, titles in list(sorted(coll.items()))[:10]:
        print(f"    {slug}: {titles}")

    print("\n== name junk ==")
    brand = df["Title"][df["Title"].map(lambda t: any(t.lower().startswith(p.lower()) for p in BRAND_PREFIXES))]
    trail = df["Title"][df["Title"].str.endswith(("-", " ", "_"))]
    print(f"  brand/plan-prefixed titles      : {len(brand)}")
    print(f"  trailing punctuation/space      : {len(trail)}  e.g. {list(trail[:5])}")
    rated = df[(df["Rating"].fillna(0) == 0) & (df["Desc"].isna())]
    print(f"  rows with no rating AND no desc : {len(rated)}  (likely brand-junk candidates)")

    print("\n== BodyPart sanity (name keywords vs BodyPart) ==")
    KEYWORDS = {
        "Abdominals": ["crunch", "plank", "sit-up", "sit up", "ab ", " abs", "core", "hollow", "leg raise", "v-up"],
        "Quadriceps": ["squat", "lunge", "leg press", "leg extension", "step-up"],
        "Hamstrings": ["leg curl", "deadlift", "romanian", "stiff"],
        "Glutes": ["hip thrust", "glute bridge"],
        "Chest": ["bench press", "fly", "push-up", "push up", "press"],
        "Biceps": ["curl"],
        "Triceps": ["skullcrusher", "pushdown", "tricep", "dip"],
        "Shoulders": ["shoulder press", "lateral raise", "delt", "overhead press"],
        "Lats": ["pull-up", "pull up", "pulldown", "row"],
        "Calves": ["calf", "calf raise"],
    }
    mismatch = 0
    examples = []
    for _, row in df.iterrows():
        part = row["BodyPart"]
        title = str(row["Title"]).lower()
        for expected, words in KEYWORDS.items():
            if part != expected and any(w in title for w in words):
                mismatch += 1
                if len(examples) < 10:
                    examples.append(f"    [{part}] {row['Title']}")
                break
    print(f"  rows where a name keyword implies a different BodyPart: {mismatch}")
    for line in examples:
        print(line)

    print("\n== BodyPart x Equipment (row counts) ==")
    ct = pd.crosstab(df["BodyPart"], df["Equipment"])
    print(ct.to_string())

    print("\n== overlap with current app pool ==")
    pool = current_pool()
    pool_slugs = {slugify(n) for n in pool}
    data_slugs = {slugify(t) for t in normed.unique()}
    direct = pool_slugs & data_slugs
    print(f"  current pool size                 : {len(pool)}")
    print(f"  pool names found in dataset (slug): {len(direct)} ({100*len(direct)/len(pool_slugs):.0f}%)")
    missing_from_data = sorted(pool_slugs - data_slugs)
    print(f"  pool names NOT in dataset         : {len(missing_from_data)}")
    for slug in missing_from_data[:15]:
        print(f"    {slug}")
    if len(missing_from_data) > 15:
        print(f"    ... and {len(missing_from_data) - 15} more")

    return 0


if __name__ == "__main__":
    sys.exit(main())
