package com.bird2fish.birdtalksdk.format

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import com.bird2fish.birdtalksdk.model.Drafty
import com.bird2fish.birdtalksdk.model.PromisedReply
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper.scaleSquareBitmap
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.util.LinkedList

// Convert images to thumbnails.
class ThumbnailTransformer : Drafty.Transformer {

    protected var components: MutableList<PromisedReply<Void>> ? = null

    fun completionPromise(): PromisedReply<Void> {
        return if (components == null) {
            PromisedReply(null)
        } else {
            PromisedReply.allOf(components!!.toTypedArray())
        }
    }

    override fun <T : Drafty.Node> transform(node: T): Drafty.Node {
        if (!node.isStyle("IM")) {
            return node
        }

        var value: Any?

        node.putData("width", ImagesHelper.REPLY_THUMBNAIL_DIM)
        node.putData("height", ImagesHelper.REPLY_THUMBNAIL_DIM)

        // Trying to use in-band image first: we don't need the full image at "ref" to generate a tiny thumbnail.
        if ((node.getData("val").also { value = it }) != null) {
            // Inline image.
            try {
                var bits = Base64.decode(value as String?, Base64.DEFAULT)
                var bmp = BitmapFactory.decodeByteArray(bits, 0, bits.size)
                bmp = scaleSquareBitmap(bmp!!, ImagesHelper.REPLY_THUMBNAIL_DIM)
                bits = ImagesHelper.bitmapToBytes(bmp, "image/jpeg")
                node.putData("val", Base64.encodeToString(bits, Base64.NO_WRAP))
                node.putData("size", bits.size)
                node.putData("mime", "image/jpeg")
            } catch (ex: Exception) {
                node.clearData("val")
                node.clearData("size")
            }
        } else if (node.getData("ref").also { value = it } is String) {
            node.clearData("ref")
            val done = PromisedReply<Void>()
            if (components == null) {
                components = LinkedList()
            }
            components!!.add(done)
            Picasso.get().load(value as String?).into(object : Target {
                override fun onBitmapLoaded(bmp: Bitmap, from: LoadedFrom) {
                    var bmp: Bitmap? = bmp
                    bmp = ImagesHelper.scaleSquareBitmap(bmp!!, ImagesHelper.REPLY_THUMBNAIL_DIM)
                    val bits: ByteArray = ImagesHelper.bitmapToBytes(bmp!!, "image/jpeg")
                    node.putData("val", Base64.encodeToString(bits, Base64.NO_WRAP))
                    node.putData("size", bits.size)
                    node.putData("mime", "image/jpeg")
                    try {
                        done.resolve(null)
                    } catch (ignored: Exception) {
                    }
                }

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable) {
                    node.clearData("size")
                    node.clearData("mime")
                    try {
                        done.resolve(null)
                    } catch (ignored: Exception) {
                    }
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable) { /* do nothing */
                }
            })
        }

        return node
    }
}
