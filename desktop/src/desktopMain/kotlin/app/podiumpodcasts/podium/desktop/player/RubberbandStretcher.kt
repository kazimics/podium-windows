package app.podiumpodcasts.podium.desktop.player

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer

class RubberbandStretcher(
    sampleRate: Int,
    channels: Int,
    options: Int = RubberbandApi.RUBBERBAND_OPTION_PROCESS_REALTIME or
                  RubberbandApi.RUBBERBAND_OPTION_STRETCH_ELASTIC or
                  RubberbandApi.RUBBERBAND_OPTION_FORMANT_PRESERVED
) {
    private val api = RubberbandApi.INSTANCE
    private val state: Pointer = api.rubberband_new(sampleRate, channels, options, 0.0, 0.0)
    private val channels = channels

    fun setSpeed(speed: Float) {
        api.rubberband_set_time_ratio(state, 1.0 / speed)
    }

    fun process(input: Array<FloatArray>, sampleCount: Int, isFinal: Boolean) {
        val nativeInput = RubberbandApi.toNativePointerArray(input)
        api.rubberband_process(state, nativeInput, sampleCount, if (isFinal) 1 else 0)
    }

    fun retrieve(): Array<FloatArray> {
        val available = api.rubberband_available(state)
        if (available <= 0) return Array(channels) { floatArrayOf() }
        val output = Array(channels) { FloatArray(available) }

        val ptrArray = Memory(channels.toLong() * Native.POINTER_SIZE)
        val channelBuffers = Array(channels) { Memory(available.toLong() * 4) }
        for (i in 0 until channels) {
            ptrArray.setPointer(i.toLong() * Native.POINTER_SIZE, channelBuffers[i])
        }

        api.rubberband_retrieve(state, ptrArray, available)

        for (i in 0 until channels) {
            output[i] = channelBuffers[i].getFloatArray(0, available)
        }
        return output
    }

    fun getPreferredStartPad(): Int = api.rubberband_get_preferred_start_pad(state)
    fun getStartDelay(): Int = api.rubberband_get_start_delay(state)
    fun getLatency(): Int = api.rubberband_get_latency(state)
    fun reset() = api.rubberband_reset(state)
    fun release() = api.rubberband_delete(state)
}
