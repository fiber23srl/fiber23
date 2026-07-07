# Fiber23 Client Area App

App Flutter per clienti Fiber23 collegata al backend:

`https://fiber23.it/mobile`

## Build APK

Da PC con Flutter installato:

```bash
flutter pub get
flutter build apk --release
```

APK generata:

`build/app/outputs/flutter-apk/app-release.apk`

## Build APK online con GitHub

1. Crea un repository GitHub.
2. Carica tutti i file di questa cartella.
3. Vai su `Actions`.
4. Apri `Build Android APK`.
5. Clicca `Run workflow`.
6. A fine build scarica `fiber23-client-area-apk` dagli artifact.

Il workflow crea automaticamente i file Android e genera:

`app-release.apk`

## Test veloce

```bash
flutter run
```

## Cambiare URL API

Modifica in `lib/main.dart`:

```dart
static const String baseUrl = 'https://fiber23.it/mobile';
```
