# hueman

Human-readable names for any color — **hue + human**. A small Kotlin Multiplatform library that maps a
hex/`Color` to the closest human color name (perceptual CIELAB distance, K-D tree lookups), with the
~30,000-name [meodai/color-names](https://github.com/meodai/color-names) palette bundled.

```kotlin
val hueman = Hueman.default()

hueman.name("#facfea")    // "Classic Rose"
hueman.name("ff0000")     // "Red"
hueman.nearest("#3a7d44") // NamedColor(name = "Jungle", hex = "#347c48")
```

Built it once and reuse it — it's immutable and thread-safe.

Ergonomic forms:

```kotlin
hueman["#facfea"]                 // operator get → "Classic Rose"
colorName("#facfea")              // one-shot, lazy default instance
RGB("#facfea").closestName()      // colormath Color extension

val brand = Hueman.of("Brand Red" to "#e23", "Brand Ink" to "#123")
```

## Install

```kotlin
// settings.gradle.kts: the kmpile binaries Maven repo
dependencies {
    implementation("com.kmpile:hueman:0.1.0")
}
```

Its only dependency is [colormath](https://github.com/ajalt/colormath) (also Kotlin Multiplatform).

## Targets

JVM · Android · iOS (x64/arm64/simulator) · macOS (arm64) · JS · Wasm/JS.

## Custom palette

```kotlin
val brand = Hueman.of(mapOf("Brand Red" to "#e23", "Brand Ink" to "#123"))
brand.name("#ee3333")     // "Brand Red"
```

## How it works

Each palette color is converted to CIELAB and indexed in a 3-D K-D tree; a query converts to CIELAB and
does a branch-and-prune nearest-neighbour search. The dataset is embedded as generated Kotlin (no
runtime resource loading), so it works identically on every target.

## License

MIT (see `LICENSE`). Bundles the meodai/color-names dataset under its MIT license (see `NOTICE`).
