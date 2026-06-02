# Map needle drawable flow (for debugging person_active issue)

**Filter logcat:** `adb logcat MapNeedle:D *:S` or in Android Studio filter by tag `MapNeedle`.

---

## Where the drawable is SET (code locations)

### 1. TeamStatusViewModel

| Code location | What sets the drawable | Debug log (tag MapNeedle) |
|---------------|------------------------|----------------------------|
| `getDefaultTeamMemberIcon(timestamp)` | `person_active` (recent) or `person_away` (old); decodeResource or drawableToBitmap | `[ViewModel.getDefaultTeamMemberIcon] drawable=person_active|person_away source=decodeResource|drawableToBitmap anHourOld=...` |
| "Me" from API (isMe path) | `myIcon` = custom needle or getDefaultTeamMemberIcon | `[ViewModel me-from-API] name=... iconSource=custom_needle index=N \| getDefaultTeamMemberIcon listSize=...` |
| Other members: cache hit | `finalIconBitmap` from cache | `[ViewModel other-member] name=... iconSource=cache effectiveNeedleId=...` |
| Other members: custom needle | `finalIconBitmap` = allAvailableCustomNeedles.get(needleIndex) | `[ViewModel other-member] name=... iconSource=custom_needle index=N` |
| Other members: fallback | `finalIconBitmap` = getDefaultTeamMemberIcon | `[ViewModel other-member] name=... iconSource=getDefaultTeamMemberIcon (out of bounds \| NumberFormatException \| no server/persisted id \| sanity null fallback)` |
| "Me" added locally | `myIcon` = custom needle or getDefaultTeamMemberIcon | `[ViewModel me-added-locally] name=... iconSource=custom_needle index=N \| getDefaultTeamMemberIcon listSize=...` |

### 2. MapTemplate.applyTeamMembersToMap

| Code location | What sets the drawable | Debug log |
|---------------|------------------------|-----------|
| `iconBitmap = gop.getIcon()` | From ViewModel (GisPointObject) | `[MapTemplate.applyTeamMembersToMap] index=... name=... iconBitmap=non-null|null iconSource=gop.getIcon() \| fallback ic_needle_symbol` |
| If null: fallback | `ContextCompat.getDrawable(ctx, R.drawable.ic_needle_symbol)` → drawableToBitmap | same line, iconSource=fallback ic_needle_symbol |

### 3. MapboxMapHolder.updateTeamLayer

| Code location | What sets the drawable | Debug log |
|---------------|------------------------|-----------|
| `bitmap = member.iconBitmap` (non-null) | Bitmap from TeamMemberMapPoint (set in MapTemplate) | `[MapboxMapHolder.updateTeamLayer] index=... name=... iconBitmap=non-null drawableSource=member.iconBitmap (from MapTemplate/ViewModel)` |
| bitmap == null: fallback | ic_needle_symbol then person_away | `[MapboxMapHolder.updateTeamLayer] index=... name=... iconBitmap=null drawableSource=fallback ic_needle_symbol|person_away` |

---

## Interpretation

- If you see **person_active** on the map, the bitmap was created from `person_active` in **TeamStatusViewModel.getDefaultTeamMemberIcon** (recent timestamp → person_active). So look for `[ViewModel.getDefaultTeamMemberIcon] drawable=person_active` and for members using `iconSource=getDefaultTeamMemberIcon` instead of `custom_needle`.
- MapboxMapHolder **never** uses person_active; its fallback is only ic_needle_symbol or person_away. So when the map shows person_active, `member.iconBitmap` was already non-null and that bitmap came from the ViewModel (or MapTemplate’s gop.getIcon()).
