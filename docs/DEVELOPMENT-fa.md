# راهنمای توسعه

این سند ساختار پروژه، معماری لایهٔ پخش و تصمیم‌های عمدی کد را توضیح می‌دهد.

## پیش‌نیازها

| ابزار | نسخه |
| --- | --- |
| JDK | 17 |
| Gradle wrapper | 8.10.2 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Kotlin | 2.0.21 |

دستورهای متداول:

```bash
./gradlew lintDebug          # اجرای lint
./gradlew testDebugUnitTest  # تست‌های واحد JVM
./gradlew assembleDebug      # ساخت APK دیباگ
```

همین سه دستور در CI (`.github/workflows/android.yml`) هم اجرا می‌شوند.

## نقشهٔ ماژول‌ها

| پکیج | مسئولیت |
| --- | --- |
| `data` | ذخیرهٔ کتابخانه، `VideoItem`، `EpisodePattern`، پیشرفت پخش |
| `player` | منطق خالص پخش، بدون وابستگی به Compose |
| `player.subtitle` | پارس، یافتن، زمان‌بندی و ساخت گزینه‌های زیرنویس |
| `ui.player` | نگه‌دارندهٔ state پخش و composableهای مخصوص پلیر |
| `ui.screens` | composableهای سطح صفحه |
| `ui.components` | اجزای قابل استفادهٔ مجدد (شیت‌ها، کارت‌ها، overlayها) |
| `util` | فرمت‌کننده‌ها و کمکی‌های کوچک اندروید |

هرچه در `player/` است Kotlin خالص است و روی JVM تست می‌شود؛ هرچه به اندروید یا
Compose نیاز دارد در `ui/` قرار می‌گیرد.

## معماری پلیر

پلیر به سه لایه تقسیم شده است:

۱. **منطق** (`player/`, `player/subtitle/`): توابع خالص و آبجکت‌های کوچک؛
`OrientationPolicy`، `PlaybackProgress`، `SubtitleCues`،
`EmbeddedSubtitleTimeline`، `SubtitleOptions`، `EpisodeNavigator` و
`EpisodeResolver` هیچ state رابط کاربری ندارند.

۲. **state** (`ui/player/PlayerViewModel.kt`): مالک نمونهٔ ExoPlayer است، یک
`PlayerUiState` تغییرناپذیر، یک flow برای متن زیرنویس و یک کانال `messages`
برای پیام‌های یک‌بارمصرف ارائه می‌دهد. هر کنش کاربر یک متد روی این کلاس است.

۳. **رابط کاربری** (`ui/screens/PlayerScreen.kt`, `ui/player/*.kt`): فقط state را
رندر می‌کند و رویداد می‌فرستد. در `PlayerScreen` تنها state گذرای UI باقی مانده
است (نمایش شیت‌ها، seek hint، HUD حرکات).

`PlayerViewModel` با یک `ViewModelStoreOwner` اختصاصی به خود صفحه scope شده،
بنابراین با خروج صفحه از composition آزاد می‌شود، نه با پایان Activity.

## زمان‌بندی زیرنویس

زیرنویس خارجی به `SubtitleCue(startMs, endMs, text)` پارس می‌شود و با جست‌وجوی
دودویی (`SubtitleCues.textAt`) خوانده می‌شود؛ یعنی هزینهٔ هر فریم O(log n) است.

زیرنویس داخلی از `Player.Listener.onCues` می‌آید و زمان پایان ندارد.
`EmbeddedSubtitleTimeline` هر cue را با رسیدن cue بعدی می‌بندد و سقف
`MAX_CUE_DURATION_MS` (۱۰ ثانیه) را برای cue باز اعمال می‌کند. بدون این سقف،
آخرین دیالوگ در سکوت‌های طولانی روی تصویر می‌ماند.

موقعیت پخش فقط هنگام پخش خوانده می‌شود: ticker یک `collectLatest` روی flow
`isPlaying` با گام `POSITION_TICK_MS` (۱۰۰ms) است. با pause، ticker و در نتیجه
recomposition متوقف می‌شود.

## امنیت شبکه

در `res/xml/network_security_config.xml` مقدار `cleartextTrafficPermitted="true"`
روی base-config تنظیم شده است. این تصمیم عمدی است:

* کاربر لینک مستقیم دلخواه وارد می‌کند و بخش بزرگی از این سرورها فقط HTTP
  هستند، پس نمی‌توان مجوز را به فهرست دامنه محدود کرد؛
* برنامه بک‌اند، حساب کاربری، توکن و تله‌متری ندارد، پس هیچ اعتبارنامه‌ای ارسال
  نمی‌شود؛
* فقط trust anchorهای سیستمی معتبرند و CAهای نصب‌شده توسط کاربر پذیرفته
  نمی‌شوند.

اگر در آینده سرویسی با احراز هویت اضافه شود، ترافیک آن باید در یک
`domain-config` با `cleartextTrafficPermitted="false"` قرار بگیرد.

## کوچک‌سازی خروجی

بیلد release با R8 و `isMinifyEnabled` و `isShrinkResources` اجرا می‌شود.
در `proguard-rules.pro` فقط چیزی نگه داشته می‌شود که reflection لازم دارد:

* سازندهٔ factoryهای DASH، HLS، SmoothStreaming و RTSP که
  `DefaultMediaSourceFactory` با نام پیدایشان می‌کند؛
* سازندهٔ رندررهای اختیاری `androidx.media3.decoder.*Renderer` برای
  `DefaultRenderersFactory`؛
* سریالایزرهای `kotlinx.serialization`.

قاعدهٔ کلی `-keep class androidx.media3.exoplayer.** { *; }` عملاً shrinking را
برای بزرگ‌ترین وابستگی پروژه خنثی می‌کند و نباید برگردانده شود.

## تست‌ها

تست‌های واحد در `app/src/test/java/com/opplayer/app/` با همان ساختار پکیج اصلی
قرار دارند و این موارد را پوشش می‌دهند: تشخیص الگوی قسمت‌ها، پیشرفت پخش محلی،
فرمت‌کننده‌ها، پارس و جست‌وجوی زیرنویس، زمان‌بندی cue داخلی، ساخت گزینه‌های
زیرنویس، سیاست چرخش صفحه، چرخهٔ حالت نمایش و موقعیت ازسرگیری.

تست ابزاردقیق UI عمداً اضافه نشده است؛ منطقی که قبلاً داخل composable بود دقیقاً
به همین دلیل بیرون کشیده شد که بدون دستگاه قابل تست باشد.

## قواعد کدنویسی

* هیچ منطق کسب‌وکاری داخل composable نوشته نمی‌شود. اگر تصمیمی را بتوان با تابع
  خالص بیان کرد، جای آن `player/` است.
* عددهای جادویی به ثابت نام‌دار در بالای فایل تبدیل می‌شوند.
* متن‌های کاربر همیشه از `strings.xml` می‌آیند؛ ViewModel شناسهٔ رشته می‌فرستد،
  نه متن آماده.
* توابع عمومی `player/` وقتی رفتارشان از نامشان پیدا نیست KDoc می‌گیرند.
