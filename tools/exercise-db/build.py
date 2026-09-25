#!/usr/bin/env python3
"""CSV -> cleaned exercise catalog for RepIo.

Applies the mapping decisions in mapping.py, dedupes, auto-fixes wrong body
parts, and writes:

  * tools/exercise-db/out/catalog.json      — the cleaned catalog
  * tools/exercise-db/out/report.txt        — build statistics + invariants
  * tools/exercise-db/out/review.csv        — every row that needs human eyes
                                              (dropped, renamed, reassigned)

Run from the repository root:
    python tools/exercise-db/build.py
"""

from __future__ import annotations

import csv
import json
import pathlib
import re
import sys

import pandas as pd

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import mapping as M

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path(__file__).resolve().parent / "out"

LOWERCASE_TOKENS = {"of", "and", "to", "with", "in", "on", "the", "for", "a", "at", "or"}


def smart_case(title: str) -> str:
    """Normalize casing to Title Case without touching digits or existing
    inner capitals. Cosmetic; every change is logged for review."""
    tokens = title.split(" ")
    out = []
    for i, tok in enumerate(tokens):
        if not tok or not tok[0].isalpha():
            out.append(tok)
            continue
        if tok.lower() in LOWERCASE_TOKENS and i > 0:
            out.append(tok.lower())
            continue
        # Sub-tokenize on hyphens; capitalize each alphabetic sub-part.
        subs = tok.split("-")
        fixed = []
        for j, sub in enumerate(subs):
            if not sub or not sub[0].isalpha():
                fixed.append(sub)
            else:
                fixed.append(sub[0].upper() + sub[1:].lower())
        out.append("-".join(fixed))
    return " ".join(out)


def slugify(name: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return re.sub(r"_+", "_", slug)


def load_and_clean() -> tuple[pd.DataFrame, list[dict]]:
    df = pd.read_csv(ROOT / "tools/exercise-db/data/megaGymDataset.csv", index_col=0)
    df["Rating"] = pd.to_numeric(df["Rating"], errors="coerce")
    report: list[dict] = []

    kept, dropped = [], []
    for idx, row in df.iterrows():
        title_raw = str(row["Title"])
        clean, _stripped = M.strip_title_junk(title_raw)
        reasons = []
        if not clean:
            reasons.append("empty name after junk strip")
        if clean.lower() in M.DROP_TITLES_EXACT:
            reasons.append("exact drop list")
        if not M.KEEP_IF(row["Desc"], row["Rating"]):
            reasons.append("no description and no rating")
        equipment = M.EQUIPMENT_MAP.get(str(row["Equipment"]).strip() if pd.notna(row["Equipment"]) else "", None)
        if equipment is None:
            reasons.append(f"unknown equipment value: {row['Equipment']!r}")
        if equipment == "Unknown":
            inferred = M.equipment_from_name(clean)
            if inferred == "Unknown":
                reasons.append("blank equipment, not inferable from name")
            else:
                equipment = inferred
                report.append({"row": idx, "title": title_raw, "action": "equipment-inferred", "detail": inferred})
        part = str(row["BodyPart"])
        if part == "Shoulders":
            precise = M.shoulder_precision(clean)
            report.append({"row": idx, "title": title_raw, "action": "shoulder-split",
                           "detail": f"Shoulders -> {precise}"})
        elif part not in M.BODYPART_MAP:
            reasons.append(f"unmapped body part: {part}")
        else:
            mapped = M.BODYPART_MAP[part]
            if mapped is None:
                reasons.append(f"body part dropped by policy: {part}")
            else:
                precise = mapped

        if not reasons:
            # The dataset has no obliques bucket; carve true oblique work out of
            # the Abdominals rows first (twists, side bends, woodchoppers...).
            if part == "Abdominals" and M.oblique_refinement(clean):
                report.append({"row": idx, "title": title_raw, "action": "oblique-refined",
                               "detail": f"{precise} -> Obliques"})
                precise = "Obliques"
            overridden = M.muscle_override(clean)
            if overridden is not None and overridden != precise:
                report.append({"row": idx, "title": title_raw, "action": "muscle-overridden",
                               "detail": f"{precise} -> {overridden}"})
                precise = overridden
            # The dataset's 'Abdominals' bucket is a dumping ground: movements
            # like kettlebell swings, hip thrusts or curls sit there while no
            # core keyword appears in the name. Reassign those — and ONLY those
            # — to the muscle the movement pattern implies.
            if part == "Abdominals" and not M.core_keyword_hit(clean):
                implied = M.movement_muscle_for_dump(clean)
                if implied is not None:
                    report.append({"row": idx, "title": title_raw, "action": "muscle-reassigned",
                                   "detail": f"{precise} -> {implied}"})
                    precise = implied

            cased = smart_case(clean)
            if cased != clean:
                report.append({"row": idx, "title": title_raw, "action": "case-normalized",
                               "detail": f"{clean!r} -> {cased!r}"})
            desc = "" if pd.isna(row["Desc"]) else str(row["Desc"]).strip()
            kept.append({
                "row": int(idx), "title_raw": title_raw, "name": cased,
                "muscle": precise, "broad": M.BROAD_FROM_PRECISE[precise],
                "equipment": equipment, "type": str(row["Type"]),
                "level": str(row["Level"]), "rating": None if pd.isna(row["Rating"]) else float(row["Rating"]),
                "description": desc,
            })
        else:
            dropped.append({"row": int(idx), "title": title_raw, "reason": "; ".join(reasons)})

    # Dedupe on the cleaned name (case-insensitive): prefer rows with a
    # description, then the higher rating, then the first occurrence.
    kept.sort(key=lambda r: (0 if r["description"] else 1, -(r["rating"] or 0.0), r["row"]))
    seen: dict[str, dict] = {}
    for r in kept:
        key = r["name"].lower()
        if key in seen:
            report.append({"row": r["row"], "title": r["title_raw"], "action": "deduped",
                           "detail": f"kept row {seen[key]['row']}"})
        else:
            seen[key] = r
    # Slug-level dedupe collapses punctuation variants of one movement
    # ("Bent Over Barbell Row" vs "Bent-Over Barbell Row"). `kept` is sorted
    # by (has-description, rating), so the first occurrence per slug wins.
    final = sorted(seen.values(), key=lambda r: r["name"].lower())
    for r in final:
        r["slug"] = slugify(r["name"])
    by_slug: dict[str, dict] = {}
    slug_deduped: list[dict] = []
    for r in final:
        if r["slug"] in by_slug:
            keep = by_slug[r["slug"]]
            report.append({"row": r["row"], "title": r["title_raw"], "action": "slug-deduped",
                           "detail": f"punctuation variant of row {keep['row']} ({keep['name']})"})
            slug_deduped.append(r)
        else:
            by_slug[r["slug"]] = r
    final = sorted(by_slug.values(), key=lambda r: r["slug"])

    for d in dropped:
        report.append({"row": d["row"], "title": d["title"], "action": "dropped", "detail": d["reason"]})
    return pd.DataFrame(final), report


def invariants(final: pd.DataFrame) -> list[str]:
    problems = []
    slugs = final["slug"].value_counts()
    dup_slugs = slugs[slugs > 1]
    if not dup_slugs.empty:
        problems.append(f"slug collisions after cleaning: {dict(dup_slugs)}")
    if final["name"].str.lower().duplicated().any():
        problems.append("duplicate names after cleaning")
    if (final["muscle"] == "").any() or final["muscle"].isna().any():
        problems.append("blank muscle assignments")
    if (final["equipment"] == "").any() or final["equipment"].isna().any():
        problems.append("blank equipment assignments")
    unknown_equip = set(final["equipment"]) - set(M.NEW_EQUIPMENT_CATEGORIES)
    if unknown_equip:
        problems.append(f"equipment outside taxonomy: {unknown_equip}")
    unknown_muscle = set(final["muscle"]) - set(M.BROAD_FROM_PRECISE)
    if unknown_muscle:
        problems.append(f"muscles outside the precise set: {unknown_muscle}")
    return problems


def main() -> int:
    OUT.mkdir(exist_ok=True)
    final, report = load_and_clean()
    problems = invariants(final)

    lines = []
    lines.append("EXERCISE CATALOG BUILD")
    lines.append(f"final exercises          : {len(final)}")
    lines.append(f"review actions           : {sum(1 for r in report if r['action'] != 'dropped')}")
    lines.append(f"dropped rows             : {sum(1 for r in report if r['action'] == 'dropped')}")
    lines.append("")
    lines.append("per broad body part (must partition the pool):")
    broad_counts = final.groupby("broad")["name"].count()
    for part in ["Arms", "Back", "Chest", "Core", "Legs", "Shoulders"]:
        lines.append(f"  {part:<10} {int(broad_counts.get(part, 0))}")
    lines.append("per equipment category:")
    for cat in M.NEW_EQUIPMENT_CATEGORIES:
        lines.append(f"  {cat:<10} {int((final['equipment'] == cat).sum())}")
    lines.append("per precise muscle:")
    for muscle, n in final["muscle"].value_counts().sort_index().items():
        lines.append(f"  {muscle:<16} {int(n)}")
    if problems:
        lines.append("")
        lines.append("INVARIANT FAILURES:")
        lines.extend(f"  - {p}" for p in problems)
    report_txt = "\n".join(lines)

    (OUT / "report.txt").write_text(report_txt, encoding="utf-8")
    (OUT / "catalog.json").write_text(
        json.dumps(final.to_dict(orient="records"), ensure_ascii=False, indent=1), encoding="utf-8")
    with (OUT / "review.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["row", "title", "action", "detail"])
        writer.writeheader()
        writer.writerows(report)

    print(report_txt)
    print(f"\nwrote {OUT / 'catalog.json'}")
    print(f"wrote {OUT / 'report.txt'}")
    print(f"wrote {OUT / 'review.csv'}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
