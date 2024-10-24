package com.bird2fish.birdtalksdk.widgets

import java.util.Arrays
import kotlin.math.max

// Class for generating audio preview from a stream of amplitudes of unknown length.

class AudioSampler constructor() {
    private val mSamples: FloatArray
    private val mScratchBuff: FloatArray

    // The index of a bucket being filled.
    private var mBucketIndex = 0

    // Number of samples per bucket in mScratchBuff.
    private var mAggregate = 1

    // Number of samples added the the current bucket.
    private var mSamplesPerBucket = 0

    init {
        mSamples = FloatArray(VISUALIZATION_BARS * 2)
        mScratchBuff = FloatArray(VISUALIZATION_BARS)
    }

    fun put(`val`: Int) {
        // Fill out the main buffer first.
        if (mAggregate == 1) {
            if (mBucketIndex < mSamples.size) {
                mSamples[mBucketIndex] = `val`.toFloat()
                mBucketIndex++
                return
            }
            compact()
        }

        // Check if the current bucket is full.
        if (mSamplesPerBucket == mAggregate) {
            // Normalize the bucket.
            mScratchBuff[mBucketIndex] =
                mScratchBuff[mBucketIndex] / mSamplesPerBucket.toFloat()
            mBucketIndex++
            mSamplesPerBucket = 0
        }
        // Check if scratch buffer is full.
        if (mBucketIndex == mScratchBuff.size) {
            compact()
        }
        mScratchBuff[mBucketIndex] += `val`.toFloat()
        mSamplesPerBucket++
    }

    // Get the count of available samples in the main buffer + scratch buffer.
    private fun length(): Int {
        if (mAggregate == 1) {
            // Only the main buffer is available.
            return mBucketIndex
        }
        // Completely filled main buffer + partially filled scratch buffer.
        return mSamples.size + mBucketIndex + 1
    }

    // Get bucket content at the given index from the main + scratch buffer.
    private fun getAt(index: Int): Float {
        // Index into the main buffer.
        var index = index
        if (index < mSamples.size) {
            return mSamples[index]
        }
        // Index into scratch buffer.
        index -= mSamples.size
        if (index < mBucketIndex) {
            return mScratchBuff[index]
        }
        // Last partially filled bucket in the scratch buffer.
        return mScratchBuff[index] / mSamplesPerBucket
    }

    fun obtain(dstCount: Int): ByteArray {
        // We can only return as many as we have.
        val dst = FloatArray(dstCount)
        val srcCount = length()
        // Resampling factor. Couple be lower or higher than 1.
        val factor = srcCount.toFloat() / dstCount
        var max = -1f
        // src = 100, dst = 200, factor = 0.5
        // src = 200, dst = 100, factor = 2.0
        for (i in 0 until dstCount) {
            val lo = (i * factor).toInt() // low bound;
            val hi = ((i + 1) * factor).toInt() // high bound;
            if (hi == lo) {
                dst[i] = getAt(lo)
            } else {
                var amp = 0f
                for (j in lo until hi) {
                    amp += getAt(j)
                }
                dst[i] = max(0.0, (amp / (hi - lo)).toDouble()).toFloat()
            }
            max = max(dst[i].toDouble(), max.toDouble()).toFloat()
        }

        val result = ByteArray(dst.size)
        if (max > 0) {
            for (i in dst.indices) {
                result[i] = (100f * dst[i] / max).toInt().toByte()
            }
        }

        return result
    }

    // Downscale the amplitudes 2x.
    private fun compact() {
        val len = VISUALIZATION_BARS / 2
        // Donwsample the main buffer: two consecutive samples make one new sample.
        for (i in 0 until len) {
            mSamples[i] = (mSamples[i * 2] + mSamples[i * 2 + 1]) * 0.5f
        }
        // Copy scratch buffer to the upper half the the main buffer.
        System.arraycopy(mScratchBuff, 0, mSamples, len, len)
        // Clear the scratch buffer.
        Arrays.fill(mScratchBuff, 0f)
        // Double the number of samples per bucket.
        mAggregate *= 2
        // Reset scratch counters.
        mBucketIndex = 0
        mSamplesPerBucket = 0
    }

    companion object {
        private const val VISUALIZATION_BARS = 128
    }
}