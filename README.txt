LAIS Kellner v2 – Android-App (Hülle um die Kellner-App der Kasse)
===================================================================
NEU in v2: findet die Kasse automatisch im WLAN (kein IP-Tippen), schöner Start-Bildschirm (Splash),
adaptives Icon, Offline-Seite mit automatischem Wiederverbinden, Display bleibt an,
Zurück-Taste = Menü (Neu laden / Kasse suchen / Beenden).

Voraussetzung: Handy und Kasse im selben WLAN, Kasse läuft, Kellner-App in der Kasse aktiv (Einstellungen -> Mobile).

--- SCHNELLSTE LÖSUNG OHNE APK (sofort) ---
Chrome am Handy öffnen -> http://IP-DER-KASSE:3000 -> Menü (⋮) -> "Zum Startbildschirm hinzufügen".
(Die IP steht in der Kasse unter Einstellungen -> Mobile.) Das Symbol verhält sich wie eine App.

--- APK BAUEN (3 Wege) ---
A) GitHub (ohne Installation, ca. 5 Minuten, kostenlos):
   1. github.com -> kostenlos registrieren -> "New repository" (privat) anlegen.
   2. Alle Dateien dieses Ordners hochladen ("Add file -> Upload files"). WICHTIG: der versteckte Ordner
      ".github" muss mit hoch. Falls nicht: "Add file -> Create new file", Name:  .github/workflows/build.yml
      und den Inhalt aus der Datei build.yml.txt einfügen.
   3. Reiter "Actions" -> "Build APK" -> "Run workflow". Nach 3-5 Minuten: Lauf anklicken -> unten "Artifacts"
      -> "LAIS-Kellner-apk" herunterladen (ZIP) -> entpacken -> app-debug.apk.
B) Android Studio (kostenlos): File -> Open -> diesen Ordner -> Sync -> Build -> Build APK(s).
   Datei: app/build/outputs/apk/debug/app-debug.apk
C) Kommandozeile (Android SDK + Gradle 8.7): gradle assembleDebug

--- APK AUFS HANDY ---
1. app-debug.apk aufs Handy schicken: WhatsApp (an dich selbst), E-Mail, Google Drive oder USB-Kabel.
2. Datei am Handy antippen. Android fragt: "Aus dieser Quelle installieren erlauben" -> Einstellungen -> erlauben.
3. "Installieren" -> "Öffnen". Beim ersten Start sucht die App die Kasse automatisch.
   Falls nichts gefunden wird: IP der Kasse manuell eingeben.

--- طريقة التنزيل على الموبايل (عربي) ---
الطريقة الأسرع بدون تطبيق: افتح Chrome ← اكتب http://IP-الكاشير:3000 ← القائمة (⋮) ← "إضافة إلى الشاشة الرئيسية".
لعمل ملف APK: ارفع المجلد على GitHub (مجاني) ← Actions ← Build APK ← Run workflow ← حمّل app-debug.apk من Artifacts.
أو افتح المجلد بـ Android Studio ← Build APK. ثم أرسل الملف للموبايل (واتساب/كابل) وثبّته (اسمح بالمصادر غير المعروفة).
التطبيق بيلاقي الكاشير لحاله على الواي فاي.
