package com.innomalist.taxi.common.networking.http.interfaces

import android.annotation.SuppressLint
import com.innomalist.taxi.common.Config
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*


class HTTPNetworkDispatcher: HTTPNetworkDispatcherBase {
    companion object {
        var instance = HTTPNetworkDispatcher()
    }

    @SuppressLint("CheckResult")
    override fun dispatch(path: String, params: Map<String, Any>?, completionHandler: (RemoteResponse<Any, HTTPStatusCode>) -> Unit) {
        try {
            Observable.create<Any> {
                val url = URL("${Config.Backend}$path")
                val client = url.openConnection() as HttpURLConnection
                client.requestMethod = "POST"
                client.doOutput = true
                client.doInput = true
                client.setRequestProperty("Accept", "application/json")
                client.setRequestProperty("Content-Type", "application/json")
                //val wr = OutputStream(client.outputStream)
                val postDataParams = HashMap<String, Any>()
                for (param in params!!.iterator()) {
                    postDataParams[param.key] = param.value
                }

                /*val result = StringBuilder()
                var first = true
                for ((key, value) in postDataParams) {
                    if (first) first = false else result.append("&")
                    result.append(URLEncoder.encode(key, "UTF-8"))
                    result.append("=")
                    result.append(URLEncoder.encode(value.toString(), "UTF-8"))
                }*/
                //client.outputStream.write(JSONObject(params).toString().toByteArray())
                val bts = JSONObject(params).toString().toByteArray(Charset.forName("UTF-8"))
                client.outputStream.write(bts)
                client.outputStream.close()
                if(client.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(client.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) sb.append(line)
                    it.onNext(sb)
                } else {
                    it.onError(Error(client.responseCode.toString()))
                }
            }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        completionHandler(RemoteResponse.createSuccess(it))
                    }, {
                        completionHandler(RemoteResponse.createError(HTTPStatusCode.invoke(it.message!!.toInt())!!))
                    })

        } catch (c: Exception) {
            c.printStackTrace()
        }
    }
}