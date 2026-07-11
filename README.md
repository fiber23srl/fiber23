# Fiber23 Cloud Android

Prima app Android nativa per Fiber23 Cloud.

## Funzioni incluse

- Login cliente con email e password del sito.
- Stato spazio cloud: totale, usato e disponibile.
- Permessi galleria Android per foto e video.
- Sincronizzazione manuale con pulsante `Sincronizza ora`.
- Sincronizzazione automatica ogni 6 ore tramite Android WorkManager.
- Upload nella cartella cloud `Backup telefono`.
- Token app valido 180 giorni, salvato sul dispositivo.

## API usata

```text
https://cloud.fiber23.it/mobile_api.php
```

Prima di provare l'app devi caricare sul sito il pacchetto server che contiene `mobile_api.php`.

## Aprire in Android Studio

1. Apri Android Studio.
2. Seleziona `Open`.
3. Scegli la cartella `fiber23-android-app`.
4. Aspetta il download delle dipendenze Gradle.
5. Collega un telefono Android oppure usa un emulatore.
6. Premi `Run`.

## Creare APK

Da Android Studio:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

Per pubblicazione Play Store servira poi creare una versione firmata:

```text
Build > Generate Signed Bundle / APK
```

## Nota tecnica

La prima versione carica i media verso `mobile_api.php`; il server poi salva sui nodi storage configurati nel piano del cliente. Le chiavi dei nodi non vengono mai inserite nell'app Android.
