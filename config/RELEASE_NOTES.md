# Release configuration notes (no secrets committed)

## Signing

Create a local `keystore.properties` (gitignored):

```
storeFile=/absolute/path/to/family-risk-review.jks
storePassword=***
keyAlias=frr
keyPassword=***
```

Wire into `app/build.gradle.kts` `signingConfigs` in Phase 9. Do not commit the JKS or passwords.

## Shrinking / baseline profile

Enable R8 minify + baseline profile during Phase 9 production hardening.

## Debug

Debug builds use `applicationIdSuffix = .debug` and are debuggable.
