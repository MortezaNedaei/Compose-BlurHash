## Compose BlurHash

A library based on [BlurHash](https://github.com/woltapp/blurhash) algorithm to add blur like effects for images in android that uses size reduction to load the blurred image faster in jetpack compose.

<img width="300" src="https://user-images.githubusercontent.com/38072572/234651821-e527f4d1-efa6-4466-a25e-f55499216229.png" />

## Usage

```groovy
repositories {
    mavenCentral()
}
```

```kotlin
implementation("io.github.mortezanedaei:blurhash-compose:1.0.0")
```

### Releases
[Releases](https://mvnrepository.com/artifact/io.github.mortezanedaei/blurhash-compose)

### How it works
It encodes the given image into a generated BlurHash string using [BlurHashEncoder](https://github.com/MortezaNedaei/Compose-BlurHash/blob/master/blurhash-compose/src/main/java/io/mortezanedaei/blurhash/compose/BlurHashEncoder.kt) and then decodes it using [BlurHashDecoder](https://github.com/MortezaNedaei/Compose-BlurHash/blob/master/blurhash-compose/src/main/java/io/mortezanedaei/blurhash/compose/BlurHashDecoder.kt) to generate blurred bitmap of the image to show the image.

### Example

```kotlin
AsyncBlurImage(imageUrl = IMAGE_URL)
```


