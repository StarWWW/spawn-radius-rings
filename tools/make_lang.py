"""Writes every lang file and checks them against English (same keys, same %s placeholders, no lone %).

Minecraft picks the file matching the game's language automatically and falls back to en_us.
Run: python3 tools/make_lang.py
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from lang_en_tr import EN, TR  # noqa: E402
from lang_eu import DE, FR, ES, IT, PT_BR, PT_PT, NL, PL  # noqa: E402
from lang_other import RU, UK, ZH_CN, ZH_TW, JA, KO, AZ  # noqa: E402

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/spawnradius/lang"

LANGS = {
    "en_us": EN, "tr_tr": TR, "de_de": DE, "fr_fr": FR, "es_es": ES, "es_mx": ES, "es_ar": ES,
    "it_it": IT, "pt_br": PT_BR, "pt_pt": PT_PT, "nl_nl": NL, "pl_pl": PL, "ru_ru": RU, "uk_ua": UK,
    "zh_cn": ZH_CN, "zh_tw": ZH_TW, "ja_jp": JA, "ko_kr": KO, "az_az": AZ,
}


def full_key(key):
    return key if key.startswith("key.") else "spawnradius." + key


# the same pattern the game uses; anything else with a % in it is shown as raw text
FORMAT = re.compile(r"%(?:(\d+)\$)?([A-Za-z%]|$)")


def placeholders(text):
    """Number of %s arguments, or -1 if the game cannot format the text."""
    count, current = 0, 0
    for m in FORMAT.finditer(text):
        if "%" in text[current:m.start()]:
            return -1
        if m.group(0) == "%%":
            pass
        elif m.group(2) == "s":
            count += 1
        else:
            return -1
        current = m.end()
    return -1 if "%" in text[current:] else count


def main():
    errors = []
    for code, table in LANGS.items():
        missing = set(EN) - set(table)
        extra = set(table) - set(EN)
        if missing:
            errors.append(f"{code}: missing {sorted(missing)}")
        if extra:
            errors.append(f"{code}: unknown {sorted(extra)}")
        for key, text in table.items():
            if placeholders(text) < 0:
                errors.append(f"{code}: {key} has a % that is not %s or %%")
            elif key in EN and placeholders(text) != placeholders(EN[key]):
                errors.append(f"{code}: {key} has {placeholders(text)} %s, expected {placeholders(EN[key])}")
    if errors:
        print("\n".join(errors))
        sys.exit(1)

    for old in OUT.glob("*.json"):
        old.unlink()
    OUT.mkdir(parents=True, exist_ok=True)
    for code, table in LANGS.items():
        data = {full_key(k): table[k] for k in EN}
        # newline="\n": the same files on every OS
        (OUT / f"{code}.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8",
                                         newline="\n")
    print(f"wrote {len(LANGS)} languages, {len(EN)} keys each")


if __name__ == "__main__":
    main()
