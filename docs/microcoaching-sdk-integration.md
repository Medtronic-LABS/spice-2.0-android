# MicroCoaching SDK — Integration Guide (SPICE SL)

A brief overview of how the **MicroCoaching Android SDK** is integrated into the SPICE app — the
dependencies, host changes, available hooks and UI components, and the rationale behind them.

> **Design principle:** the integration is *additive and fail-safe*. Every SDK entry point is guarded
> by `MicroCoachingSDK.isInitialized()`, and the SDK wraps event recording in `runCatching`. Removing
> the SDK dependency (and the call sites) returns the app to stock behaviour.

---

## 1. Dependencies & repository

`app/build.gradle.kts`:

```kotlin
// resolved from mavenLocal() — declared in settings.gradle.kts
implementation("com.medtroniclabs.microcoaching:sdk-android:0.5.3-SNAPSHOT")
implementation("com.medtroniclabs.microcoaching:sdk-android-sherpa:0.4.0-SNAPSHOT") // optional, offline Bengali STT
```

- **Why `mavenLocal()`** — the SDK is built from the sibling repo (`../micro-coaching-android-sdk`)
  and published locally with `./gradlew :sdk-android:publishToMavenLocal`. `mavenLocal()` is already
  present in `settings.gradle.kts` (`dependencyResolutionManagement`). Until the SDK is published to a
  shared Maven repo, every dev must publish it locally first.
- **`sdk-android-sherpa` is optional** — it bundles `sherpa-onnx` (~30 MB native) for offline Bengali
  speech-to-text. Drop it (and `.offlineSttEngineFactory(...)` / `.enableVoice(true)`) if voice isn't needed.

---

## 2. Toolchain compatibility

The SDK is published from an **AGP 9 / Kotlin 2.2** build; SPICE is on **AGP 8.13 / Kotlin 2.0.21**.
Two host-visible consequences:

| Symptom | Fix (already applied) | Where |
|---|---|---|
| `Module was compiled with an incompatible version of Kotlin … metadata is 2.2.0, expected 2.0.0` | `freeCompilerArgs += "-Xskip-metadata-version-check"` | `app/build.gradle.kts` → `kotlinOptions` |
| Manifest merge: two `androidx.core.content.FileProvider` providers collide (host `${appId}.provider` vs SDK `${appId}.microcoaching.provider`) | SDK ships a dedicated `CoachingFileProvider` subclass so the two coexist | **SDK side** (its manifest) — no host change needed |

If SPICE later adopts the SDK's Kotlin/AGP line, the `-Xskip-metadata-version-check` flag can be
removed. Do **not** remove it while the SDK is on a newer Kotlin than the host.

---

## 3. Build-config fields

Surfaced in `app/build.gradle.kts` from the root `environment.properties`. **All three are optional** —
the build supplies working fallbacks and the SDK tolerates blanks (its `backendUrl` and
`huggingFaceToken` both default to any value the sdk was built with), so the app builds and runs without them. Set them only to
customize: point at a different backend environment, supply a token, or turn telemetry on.

| Field | Default if unset | Set it to… |
|---|---|---|
| `COACHING_BACKEND_URL` | `http://10.0.2.2:8000/` (emulator loopback; per flavor) | Point coaching at a real / different backend environment |
| `HF_TOKEN` | `""` (SDK default model handling / non-gated mirror) | A Hugging Face token, when the on-device model to download is gated |
| `ENABLE_COACHING_TELEMETRY` | `false` | `true` to enable OpenTelemetry export |

---

## 4. Initialization & lifecycle

Two-phase init — the SDK needs a JWT that only exists after login:

1. **`SpiceBaseApplication`** builds the SDK at startup with an **empty auth token** (so home surfaces
   can render immediately).
2. **`LandingActivity.reinitCoachingSdkWithToken()`** (called in `onCreate` after auth) rebuilds the
   SDK with the real JWT, language, persona, and model path. `Builder.build()` shuts the old instance
   down before creating the new one.
3. **`LandingActivity.onResume()`** calls `MicroCoachingSDK.getInstance().onConnectivityRestored()` to
   flush pending telemetry / trigger sync when back online.

Persona is resolved host-side by `common/CoachingPersonaResolver.kt` (SPICE role → `CoachingPersona`;
uses `RoleConstant.PO`).

### Builder options (the ones SPICE uses)

```kotlin
MicroCoachingSDK.Builder(applicationContext)
    .language(SpiceBaseApplication.spiceLanguageToSdkLanguage(culture)) // Language.ENGLISH | BANGLA
    .backendUrl(BuildConfig.COACHING_BACKEND_URL)
    .authToken(jwt)
    .persona(resolveCoachingPersona())                 // CoachingPersona.PO | SK | UNKNOWN
    .enableTelemetry(BuildConfig.ENABLE_COACHING_TELEMETRY)
    .enableChat(true).enableLearnModule(true).enableApplyModule(true)
    .enableVoice(true).offlineSttEngineFactory(SherpaOnnxStt.factory) // optional (sherpa module)
    .modelDownloadStrategy(strategy)                   // ON_FIRST_USE (no local model) | PROVIDED
    .modelProviders(listOf(ModelProvider.HuggingFace)) // or ModelProvider.Backend
    .modelPath(existingModel?.absolutePath ?: "")
    .huggingFaceToken(BuildConfig.HF_TOKEN)
    .wifiOnlyModelDownload(false)
    .forceMode(CoachingMode.EDGE)                      // ONLINE | EDGE | CACHED
    .build()
```

Advanced/optional Builder knobs exist but SPICE leaves them at defaults: `tenantId`, `uiTheme`,
`selectedModel`, `chatScopeStrictness`, `chatTuning`, `refresherTuning`, `inferenceTemperature`,
`maxInferenceTokens`, `minFreeStorageBytes`, connection/read timeouts, and the `otel*` telemetry
export settings. Enable modules you don't use with care — each adds a home surface.

---

## 5. Hooks — feeding SPICE data to the SDK

All are no-ops unless the SDK is initialised. **Contract: pass no PII** — only clinical-type signals,
derived flags, and non-identifying projections cross the boundary.

| Hook | Called from | What SPICE sends |
|---|---|---|
| `onHomeScreenShown(chwId)` | `HomeScreenFragment.setupCoachingSurfaces()` | CHW id (from `SecuredPreference.getUserId()`) |
| `onTodaysVisitsUpdated(visits)` | `HomeScreenFragment.pushTodaysVisits()` | today's due visits — type/encounter/dueDate/villageId + derived `isPregnant` (see `FollowUpDao.getVisitsDueOn` + `microcoaching/TodaysVisitRow`) |
| `onAssessmentSubmitted(encounterId, patientId, assessmentData)` | `AssessmentActivity.notifyMicroCoachingSDK()` | PII-free assessment map (`microcoaching/AssessmentEntityExt.toSdkAssessmentMap`) |
| `onReferralSubmitted(encounterId, patientId, referralData)` | same (on referral commit) | `{recommended, actual}` compliance state (`toComplianceState`) incl. picked facility tier |
| `refreshRefreshers()` | pull-to-refresh on home | (none) — re-runs the gap evaluation |
| `markRefresherSkipped(familyId)` | MorningCard "Skip" | module family id |
| `onConnectivityRestored()` | `LandingActivity.onResume()` | (none) |

Other hooks the SDK exposes but SPICE doesn't use yet: `onVisitCompleted`, `onFormSubmitted`,
`onRuleFired`, `onRiskFlagObserved`, `onModuleQuizCompleted`, `onMorningOpen`, `updateAuthToken`
(token-only refresh — lighter than a full rebuild), `shutdown`.

---

## 6. UI components (all Composables / SDK-owned screens)

| Component | Host usage |
|---|---|
| `ui.components.MorningCard` | Home banner (`fragment_home_screen.xml` → `coachingCardBanner` ComposeView) |
| `ui.components.ChatFab` | Home chat FAB (`chatFab` ComposeView) → opens `CoachingChatBottomSheet` |
| `ui.components.CoachingGridTile` | Home menu-grid coaching tile (`DashboardMenuItemsAdapter`, own view type + `row_coaching_tile.xml`) |
| `ui.chat.CoachingChatBottomSheet.show(fm)` | Chat assistant (bottom sheet) |
| `ui.learn.modules.bottomsheet.RefresherBottomSheet.show(...)` | MorningCard "Start" → cards-first refresher flow |
| `ui.flow.CoachingFlowActivity.launchLearn(ctx, chwId)` | Coaching tile tap → Learn flow |
| `ui.theme.MicroCoachingTheme` | Wraps SDK Composables hosted in `ComposeView`s |

> **Tablet note:** the grid uses `FlexboxLayoutManager`, which measures item views before attaching
> them. `DashboardMenuItemsAdapter` supplies an explicit lifecycle-scoped `Recomposer` via
> `setParentCompositionContext` so the coaching tile's `ComposeView` composes safely while detached.

### On-device model download (host-triggered)
The SDK does **not** auto-download. The host gates on `modelManager.isModelPresent()` (and
`isLowEndDevice` — <3 GB RAM runs retrieval-only, no model), prompts the user, then calls
`modelManager.triggerDownload()`. Size is read live via `selectedModelVariant().sizeInBytes`. See
`HomeScreenFragment.showCoachingModelDownloadPrompt()`.

---

## 7. Database

- `HealthFacilityEntity.type` (nullable facility tier) + `AutoMigration(7 → 8)` in `SpiceDataBase`
  (schema `8.json`). Feeds the referral-**location** compliance gaps.
- **Optional / removable:** referral-based gaps aren't consumed by the refresher surface yet. If the
  schema shouldn't change, drop the column + migration with no functional loss.

---

## 8. Enums quick reference

- `Language`: `ENGLISH`, `BANGLA`
- `CoachingPersona`: `PO`, `SK`, `UNKNOWN`
- `CoachingMode`: `ONLINE`, `EDGE`, `CACHED`
- `ModelProvider`: `HuggingFace`, `Backend`
- `ModelDownloadStrategy`: `ON_FIRST_USE`, `PROVIDED`

---

## 9. Integration checklist / what changed in the host

- `settings.gradle.kts` — `mavenLocal()` (repo for the SDK)
- `app/build.gradle.kts` — SDK deps, `COACHING_BACKEND_URL`/`HF_TOKEN`/`ENABLE_COACHING_TELEMETRY`, `-Xskip-metadata-version-check`
- `SpiceBaseApplication.kt` — SDK build/init + `spiceLanguageToSdkLanguage`
- `ui/landing/LandingActivity.kt` — `reinitCoachingSdkWithToken()`, connectivity forwarding
- `ui/home/HomeScreenFragment.kt` — MorningCard, chat FAB, grid tile, today's-visit push, model-download prompt
- `ui/home/adapter/DashboardMenuItemsAdapter.kt` — coaching grid tile view type
- `ui/assessment/AssessmentActivity.kt` — `notifyMicroCoachingSDK()` on assessment/referral submit
- `common/CoachingPersonaResolver.kt`, `microcoaching/AssessmentEntityExt.kt`, `microcoaching/TodaysVisitRow.kt` — mapping helpers (new)
- `db/dao/FollowUpDao.kt` — `getVisitsDueOn()`; `db/SpiceDataBase.kt` + `db/entity/HealthFacilityEntity.kt` — tier column + migration
- `res/` — `row_coaching_tile.xml`, `ic_coaching.xml`, coaching strings (EN + `values-bn-rBD`)

---

## 10. Environment setup

The coaching-specific keys are added to the root `environment.properties`. A template with every
required key (URLs, salts, DB keys, signing, and the coaching keys) is provided in
`environment.properties.example` — copy it to `environment.properties` and fill in real values.
Never commit real secrets.

Coaching-specific keys (all **optional** — the build/SDK have working defaults, see §3):
`UHIS_<FLAVOR>_COACHING_BACKEND_URL` (per flavor), `HF_TOKEN`, `ENABLE_COACHING_TELEMETRY`. Signing
keys used by the debug flavors: `NON_PROD_JKS_ALIAS`, `NON_PROD_JKS_PASSWORD`,
`NON_PROD_JKS_STORE_PASSWORD`.

---

## 11. Follow-ups / integration debt

- **Publish the SDK to a shared Maven repo.** Today the SDK resolves from `mavenLocal()`, so every
  dev (and CI) must run `./gradlew :sdk-android:publishToMavenLocal` from the SDK repo first. Hosting
  the artifact on a shared Maven repository (and referencing it there) removes that manual step and
  makes SDK versions reproducible across machines.
- **Align toolchains to drop `-Xskip-metadata-version-check`.** The flag exists only because the SDK
  is published on a newer Kotlin/AGP than the host (see §2). Once both sides share a Kotlin line, the
  flag can be removed.
- **Decide on the DB `type` column + `AutoMigration(7 → 8)`** (see §7) — keep or drop depending on
  whether referral-location compliance gaps are adopted.