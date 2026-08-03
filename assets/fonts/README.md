# Fonts

Bundle these families for production:

| Family | Role | Suggested files |
|--------|------|-----------------|
| Manrope | English headings | `manrope_*.ttf` |
| Inter | English body / UI | `inter_*.ttf` |
| Noto Sans Tamil | Tamil | `noto_sans_tamil_*.ttf` |
| Noto Sans Devanagari | Hindi | `noto_sans_devanagari_*.ttf` |

Copy licensed files into `core/designsystem/src/main/res/font/` and update `Type.kt`.

Phase 0 uses `FontFamily.SansSerif` placeholders so the project compiles without binary font commits.
