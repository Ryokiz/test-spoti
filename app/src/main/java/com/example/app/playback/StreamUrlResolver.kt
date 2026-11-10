package com.example.app.playback

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.example.app.data.supabase.ApiException
import com.example.app.data.supabase.SupabaseHttp
import org.json.JSONObject
import java.io.IOException

/**
 * Turns `track://<id>[?q=low|normal|high]` into a short-lived signed URL by asking the stream-url function.
 * Runs on Media3's loading thread, so the blocking call is fine. Other schemes pass through untouched.
 */
class StreamUrlResolver(private val http: SupabaseHttp) : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != "track") return dataSpec
        val id = uri.host ?: throw IOException("bad track uri")
        val quality = uri.getQueryParameter("q") ?: "normal"
        return try {
            val res = JSONObject(
                http.call("POST", "/functions/v1/stream-url", JSONObject().put("track_id", id).put("quality", quality)),
            )
            dataSpec.withUri(Uri.parse(res.getString("url")))
        } catch (e: ApiException) {
            throw IOException("stream url refused (${e.code ?: e.status})", e)
        }
    }
}
