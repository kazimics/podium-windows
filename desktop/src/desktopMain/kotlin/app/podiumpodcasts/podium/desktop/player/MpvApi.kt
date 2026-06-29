package app.podiumpodcasts.podium.desktop.player

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface MpvApi : Library {

    fun mpv_create(): Long
    fun mpv_initialize(handle: Long): Int
    fun mpv_destroy(handle: Long)
    fun mpv_terminate_destroy(handle: Long)

    fun mpv_command(handle: Long, args: Array<String?>): Int
    fun mpv_command_string(handle: Long, args: String): Int

    fun mpv_set_property_string(handle: Long, name: String, data: String): Int
    fun mpv_get_property_string(handle: Long, name: String): Pointer
    fun mpv_set_option_string(handle: Long, name: String, data: String): Int

    fun mpv_observe_property(handle: Long, reply_userdata: Long, name: String, format: Int): Int
    fun mpv_unobserve_property(handle: Long, reply_userdata: Long): Int

    fun mpv_wait_event(handle: Long, timeout: Double): Pointer
    fun mpv_request_event(handle: Long, event: Int, enable: Int): Int

    fun mpv_request_log_messages(handle: Long, min_level: String): Int
    fun mpv_free(data: Pointer)

    companion object {
        val INSTANCE: MpvApi = Native.load("mpv-1", MpvApi::class.java)
            ?: throw UnsatisfiedLinkError("Failed to load mpv-1.dll")

        const val MPV_FORMAT_NONE = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_FLAG = 2
        const val MPV_FORMAT_INT64 = 3
        const val MPV_FORMAT_DOUBLE = 4

        fun command(handle: Long, vararg args: String) {
            val arr = arrayOfNulls<String>(args.size + 1)
            args.copyInto(arr)
            arr[args.size] = null
            INSTANCE.mpv_command(handle, arr)
        }
    }
}
