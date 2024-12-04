package com.bird2fish.birdtalksdk.widgets

import java.util.Arrays
import kotlin.math.max

// 代码定义了一个 AudioSampler 类，用于从一系列未知长度的音频幅度流中生成音频预览数据。
// 该类的设计目的是将原始音频数据采样后处理成可用于音频可视化的格式。
// 具体来说，AudioSampler 将音频数据分成固定数量的桶（bucket），
// 然后将这些桶中的数据进行聚合、归一化并生成用于音频可视化的采样数据。

class AudioSampler constructor() {
    // 当采样的的缓冲区满了以后，以2:1方式平均值填充到 mScratchBuff
    private val mSamples: FloatArray
    private val mScratchBuff: FloatArray

    // 当前填充哪个桶.
    private var mBucketIndex = 0

    // mScratchBuff中每个桶的采样数，目前是1
    private var mAggregate = 1

    // 当前桶中添加了多少样本
    private var mSamplesPerBucket = 0

    init {
        mSamples = FloatArray(VISUALIZATION_BARS * 2)
        mScratchBuff = FloatArray(VISUALIZATION_BARS)
    }

    fun put(data: Int) {
        // Fill out the main buffer first.
        if (mAggregate == 1) {
            if (mBucketIndex < mSamples.size) {
                mSamples[mBucketIndex] = data.toFloat()
                mBucketIndex++
                return
            }
            // 执行到这里的时候，第一遍填充完毕了；mAggregate 就变成2了；
            compact()
        }

        // 向桶i 累加完毕后，执行正则化（计算平均值）
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

        // 普通情况下，向桶i 累加N个相邻数据
        mScratchBuff[mBucketIndex] += data.toFloat()
        mSamplesPerBucket++
    }

    // 计算所有的样本数 buffer + scratch buffer.
    private fun length(): Int {
        if (mAggregate == 1) {
            // Only the main buffer is available.
            return mBucketIndex
        }
        // Completely filled main buffer + partially filled scratch buffer.
        return mSamples.size + mBucketIndex + 1
    }

    // 获取某个桶中的数据  the main + scratch buffer.
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

    // 获取指定数量的音频采样数据，并将其归一化后转换为字节数组返回。
    // 这个方法可以对数据进行上采样或下采样（取决于 srcCount 和 dstCount 的比例），并且对样本进行最大值归一化处理。
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

    // 当临时缓冲区满了以后，该方法会对数据进行压缩处理。
    // 它将主缓冲区中的数据进行 2 倍下采样（即两个相邻样本的平均值），
    // 并将临时缓冲区中的数据复制到主缓冲区中。这样可以腾出更多空间以接收新的样本数据。
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
        // 因为这里折叠了一次，所以桶的数据等于翻倍了
        mAggregate *= 2
        // Reset scratch counters.
        mBucketIndex = 0
        mSamplesPerBucket = 0
    }

    companion object {
//        用于可视化的柱状图数量，定义为 128。
        private const val VISUALIZATION_BARS = 128
    }
}