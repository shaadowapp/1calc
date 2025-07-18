<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" class="logo" width="120"/>

### Sharing a PDF via Bottom Sheet with Installed Apps (Kotlin/Android)

If you need to let your users share a PDF via a bottom sheet listing all installed, compatible apps (**like WhatsApp, Gmail, Drive, etc.**) in your Kotlin Android app, here’s the best way to achieve this:

#### 1. Use the System Sharesheet (Recommended)

Android already provides a **system sharesheet** UI which appears as a bottom sheet and lists all installed apps that can handle your content—including filtering only apps that can actually share PDFs. You do **not** need a third-party package for the core functionality; the sharesheet is the standard, preferred solution—recommended by Google for consistency and reliability[^1].

**Kotlin Example:**

```kotlin
// Create a File pointing to your PDF
val file = File(this.getExternalFilesDir(null), "your-file.pdf")

// Create URI for file using FileProvider (see below)
val uri = FileProvider.getUriForFile(
    this,
    "${applicationContext.packageName}.provider",
    file
)

// Build the share intent
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

// Show system sharesheet as bottom sheet
startActivity(Intent.createChooser(shareIntent, "Share PDF using"))
```

- Make sure to use **FileProvider** for URIs for API 24+ (Android 7+) or you'll get exceptions[^2].


#### 2. Why Not a Separate 3rd-party Library?

- **No major 3rd party library** is widely recommended/needed specifically for showing the share-dialog as a bottom sheet. The built-in intent chooser on modern Android always appears as a bottom sheet if supported by the device/OS version[^3].
- If you still want full custom control, you can use libraries like:
    - `Flipboard/bottomsheet`: lets you build custom bottom sheets and even create intent choosers, but it’s unnecessary for typical sharing use cases[^4].
    - You can build your own bottom sheet, query apps with `PackageManager.queryIntentActivities()`, and show as you wish—but this is not recommended due to UX, security, and maintenance concerns[^3].


#### 3. How Android Finds Shareable Apps

- The `ACTION_SEND` intent with the PDF `Uri` and `type = "application/pdf"` ensures that only apps which can receive and handle PDF files are suggested to the user.
- The system dialog appears as a **bottom sheet** on compatible Android versions (Android 10+ and many custom OEM skins even before that), showing all installed apps that can share the PDF[^1][^3][^5].


#### 4. FileProvider Setup (Important!)

In your `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths"/>
</provider>
```

In `res/xml/file_paths.xml`:

```xml
<paths>
    <external-files-path name="external_files" path="."/>
</paths>
```

- This ensures other apps can access your PDFs in a secure way[^2].


### Summary Table

| Requirement | 3rd Party Library Needed? | How to Implement |
| :-- | :-- | :-- |
| Show bottom sheet of shareable apps | No | Use Android sharesheet with intent chooser |
| Share PDF to any compatible app | No | Use `Intent.ACTION_SEND` and FileProvider |
| Custom look for bottom sheet (optional) | Yes, but not needed | Use custom libraries like Flipboard/bottomsheet[^4] |

### Key Points

- **Use the built-in sharesheet** for consistent UX, up-to-date app list, and full OS support[^1][^3].
- Do **not** build or replicate your own share-app bottom sheet unless you have an advanced need.
- If wanting a true custom UI beyond Android’s built-in, open source options exist but should be approached with care[^4][^6].

<div style="text-align: center">⁂</div>

[^1]: https://developer.android.com/training/sharing/send

[^2]: https://stackoverflow.com/questions/64820009/how-can-i-share-pdf-file-in-android

[^3]: https://stackoverflow.com/questions/31330223/bottom-share-sheet-in-android

[^4]: https://github.com/Flipboard/bottomsheet

[^5]: https://www.codexpedia.com/android/android-share-intent-with-html-text/

[^6]: https://github.com/tutti-ch/android-bottomsheet

[^7]: https://stackoverflow.com/questions/76919374/how-can-i-do-bottom-sheet-share-component-in-kotlin-compose

[^8]: https://www.youtube.com/watch?v=vxF3Ayw3Xuc

[^9]: https://notesjam.com/android-bottom-sheet/

[^10]: https://www.geeksforgeeks.org/kotlin/how-to-build-pdf-downloader-app-in-android-with-kotlin/

[^11]: https://pub.dev/packages/share_pdf/versions/1.0.1

[^12]: https://pub.dev/packages/share_pdf/versions/0.0.8

[^13]: https://stackoverflow.com/questions/32223990/how-to-use-bottomsheets-to-show-share-dialog

[^14]: https://pub.dev/packages/share_pdf

[^15]: https://blog.mindorks.com/android-bottomsheet-in-kotlin/

[^16]: https://www.youtube.com/watch?v=6GBnYLcXKXY

[^17]: https://www.youtube.com/watch?v=EtnFbx6mhZw

[^18]: https://stackoverflow.com/q/40281537

[^19]: https://stackoverflow.com/questions/64820009/how-can-i-share-pdf-file-in-android/64820529

[^20]: https://developer.android.com/develop/ui/compose/components/bottom-sheets

