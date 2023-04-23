package io.mortezanedaei.blurhash.compose

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.ImageRequest

private const val DEFAULT_HASHED_BITMAP_WIDTH = 4
private const val DEFAULT_HASHED_BITMAP_HEIGHT = 3

@ExperimentalCoilApi
@Composable
fun AsyncBlurImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageModifier: Modifier? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    isCrossFadeRequired: Boolean = true,
    onImageLoadSuccess: () -> Unit = {},
    onImageLoadFailure: () -> Unit = {}
) {
    val context = LocalContext.current
    val resources = context.resources

    BoxWithConstraints(modifier = modifier) {
        val blurHash = blurHashProvider(imageUrl, context)
        val blurBitmap by remember(blurHash) {
            mutableStateOf(
                BlurHashDecoder.decode(
                    blurHash = blurHash,
                    width = DEFAULT_HASHED_BITMAP_WIDTH,
                    height = DEFAULT_HASHED_BITMAP_HEIGHT
                )
            )
        }

        AsyncImage(
            modifier = imageModifier ?: modifier,
            model = ImageRequest.Builder(context)
                .data(blurBitmap)
                .crossfade(isCrossFadeRequired)
                .placeholder(
                    blurBitmap?.toDrawable(resources)
                )
                .fallback(blurBitmap?.toDrawable(resources))
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            onSuccess = { onImageLoadSuccess() },
            onError = { onImageLoadFailure() }
        )
    }
}

@Composable
private fun blurHashProvider(
    imageUrl: String,
    context: Context
): String {
    var blurHash by remember { mutableStateOf("") }
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false)
        .target { result ->
            val bitmap = (result as BitmapDrawable).bitmap
            blurHash = BlurHashEncoder.encode(bitmap)
        }
        .build()
    loader.enqueue(request)
    return blurHash
}
