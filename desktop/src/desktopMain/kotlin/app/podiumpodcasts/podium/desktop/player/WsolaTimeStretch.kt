package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger

private const val TAG = "WsolaTimeStretch"

class WsolaTimeStretch(
    private val sampleRate: Int = 44100,
    private val channels: Int = 2
) {
    fun process(input: ShortArray, speed: Float): ShortArray {
        if (speed == 1.0f) return input
        if (input.size < 64) return input

        val frameSize = input.size.coerceAtLeast(64)
        val output = mutableListOf<Short>()
        var pos = 0

        while (pos + frameSize <= input.size) {
            val frame = input.copyOfRange(pos, pos + frameSize)
            val stretched = stretchFrame(frame, speed, frameSize)
            output.addAll(stretched.toList())
            pos += (frameSize * speed).toInt().coerceAtLeast(1)
        }

        while (pos < input.size) {
            output.add(input[pos])
            pos++
        }

        return output.toShortArray()
    }

    private fun stretchFrame(frame: ShortArray, speed: Float, frameSize: Int): ShortArray {
        val outputSize = (frame.size / speed).toInt().coerceAtLeast(1)
        val output = ShortArray(outputSize)

        val analysisStep = (frameSize * speed).toInt().coerceAtLeast(1)
        val synthesisStep = frameSize

        var analysisPos = 0
        var synthesisPos = 0

        while (synthesisPos + synthesisStep <= output.size && analysisPos + synthesisStep + frameSize <= frame.size) {
            val bestOffset = findBestOverlap(frame, analysisPos, synthesisStep, frameSize)

            for (i in 0 until synthesisStep) {
                val fadeIn = i.toFloat() / synthesisStep
                val fadeOut = 1.0f - fadeIn
                val srcIdx = (analysisPos + bestOffset + i).coerceIn(0, frame.size - 1)
                if (synthesisPos + i < output.size) {
                    output[synthesisPos + i] = (frame[srcIdx] * fadeOut).toInt().toShort()
                }
            }

            analysisPos += analysisStep
            synthesisPos += synthesisStep
        }

        return output
    }

    private fun findBestOverlap(frame: ShortArray, analysisPos: Int, step: Int, frameSize: Int): Int {
        var bestOffset = 0
        var bestCorrelation = Long.MIN_VALUE

        val searchRange = (step / 4).coerceAtLeast(1)
        val refEnd = (analysisPos + frameSize).coerceAtMost(frame.size)

        if (refEnd - analysisPos < frameSize) return 0

        for (offset in -searchRange..searchRange) {
            val testStart = analysisPos + offset
            val testEnd = (testStart + frameSize).coerceAtMost(frame.size)
            if (testStart < 0 || testEnd - testStart < frameSize) continue

            var correlation = 0L
            for (i in 0 until frameSize) {
                correlation += frame[analysisPos + i].toLong() * frame[testStart + i].toLong()
            }

            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestOffset = offset
            }
        }

        return bestOffset.coerceAtLeast(0)
    }
}
