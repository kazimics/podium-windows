package app.podiumpodcasts.podium.desktop.player

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer

interface RubberbandApi : Library {
    companion object {
        val INSTANCE: RubberbandApi = Native.load("rubberband", RubberbandApi::class.java)
            ?: throw UnsatisfiedLinkError("Failed to load rubberband.dll")

        const val RUBBERBAND_OPTION_PROCESS_OFFLINE = 0x00000000
        const val RUBBERBAND_OPTION_PROCESS_REALTIME = 0x00000001
        const val RUBBERBAND_OPTION_STRETCH_RIGID = 0x00000000
        const val RUBBERBAND_OPTION_STRETCH_MID = 0x00000010
        const val RUBBERBAND_OPTION_STRETCH_ELASTIC = 0x00000020
        const val RUBBERBAND_OPTION_TRANSIENTS_MIX = 0x00000000
        const val RUBBERBAND_OPTION_TRANSIENTS_UNMIXED = 0x00000100
        const val RUBBERBAND_OPTION_TRANSIENTS_DETECT = 0x00000000
        const val RUBBERBAND_OPTION_TRANSIENTS_IGNORE = 0x00000200
        const val RUBBERBAND_OPTION_PHASES_LAMINAR = 0x00000000
        const val RUBBERBAND_OPTION_PHASES_INDEPENDENT = 0x00001000
        const val RUBBERBAND_OPTION_THIRD_HARMONIC = 0x00010000
        const val RUBBERBAND_OPTION_FORMANT_PRESERVED = 0x01000000
        const val RUBBERBAND_OPTION_CHANNEL_NAMES = 0x04000000
        const val RUBBERBAND_OPTION_ENGINE_FFT = 0x00000000
        const val RUBBERBAND_OPTION_ENGINE_SPEEX = 0x02000000

        fun toNativePointerArray(channels: Array<FloatArray>): Pointer {
            val ptrArray = Memory(channels.size.toLong() * Native.POINTER_SIZE)
            for (i in channels.indices) {
                val floatMem = Memory(channels[i].size.toLong() * 4)
                floatMem.write(0, channels[i], 0, channels[i].size)
                ptrArray.setPointer(i.toLong() * Native.POINTER_SIZE, floatMem)
            }
            return ptrArray
        }

        fun toNativePointerArrayEmpty(channels: Int, size: Int): Pointer {
            val ptrArray = Memory(channels.toLong() * Native.POINTER_SIZE)
            for (i in 0 until channels) {
                val floatMem = Memory(size.toLong() * 4)
                ptrArray.setPointer(i.toLong() * Native.POINTER_SIZE, floatMem)
            }
            return ptrArray
        }
    }

    fun rubberband_new(sampleRate: Int, channels: Int, options: Int, startTcRate: Double, startTcSecs: Double): Pointer
    fun rubberband_delete(state: Pointer)
    fun rubberband_reset(state: Pointer)
    fun rubberband_set_time_ratio(state: Pointer, ratio: Double)
    fun rubberband_set_pitch_scale(state: Pointer, scale: Double)
    fun rubberband_get_preferred_start_pad(state: Pointer): Int
    fun rubberband_get_start_delay(state: Pointer): Int
    fun rubberband_get_latency(state: Pointer): Int
    fun rubberband_set_expected_input_duration(state: Pointer, samples: Int)
    fun rubberband_get_sample_ratio(state: Pointer): Double
    fun rubberband_process(state: Pointer, input: Pointer, samples: Int, isFinal: Int)
    fun rubberband_available(state: Pointer): Int
    fun rubberband_retrieve(state: Pointer, output: Pointer, samples: Int): Int
    fun rubberband_get_vsamplerate(state: Pointer): Double
}