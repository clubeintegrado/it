package com.innomalist.taxi.common.networking.socket.interfaces

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.innomalist.taxi.common.BuildConfig
import com.innomalist.taxi.common.Config
import com.innomalist.taxi.common.models.ChatMessage
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.utils.Adapters
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.squareup.moshi.Json
import io.reactivex.Observable
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class SocketNetworkDispatcher : NetworkDispatcher {
    companion object {
        var instance = SocketNetworkDispatcher()
    }
    lateinit var socket: Socket
    lateinit var newMessage: Observable<ChatMessage>
    lateinit var arrived: Observable<Int>
    lateinit var started: Observable<Request>
    lateinit var travelInfo: Observable<LatLng>
    var onNewRequest: ((Request) -> Unit)? = null
    var onFinished: ((FinishResult) -> Unit)? = null
    var onCancel: ((Int) -> Unit)? = null
    var onCancelRequest: ((Long) -> Unit)? = null
    var onDriverAccepted: ((Request) -> Unit)? = null
    var onPaid: ((Int) -> Unit)? = null

    override fun dispatch(event: String, params: Array<Any>?, completionHandler: (RemoteResponse<Any, SocketClientError>) -> Unit) {
        socket.emit(event, params) {
            if ((it.size > 1)) {
                completionHandler(RemoteResponse.createError(SocketClientError.InvalidAckParamCount))
                return@emit
            }
            if ((it.isEmpty())) {
                GlobalScope.launch(Dispatchers.Main) {
                    completionHandler(RemoteResponse.createSuccess(EmptyClass()))
                }
                return@emit
            }
            completionHandler(RemoteResponse.createSuccess(it[0]))
        }
    }

    fun connect(namespace: Namespace, token: String, notificationId: String, completionHandler: (RemoteResponse<Boolean, ConnectionError>) -> Unit) {
        val options = IO.Options()
        options.query = "token=$token&os=android&ver=${BuildConfig.VERSION_CODE}&not=$notificationId"
        socket = IO.socket("${Config.Backend}${namespace.rawValue}", options)
        socket.on(Socket.EVENT_CONNECT) {
            completionHandler(RemoteResponse.createSuccess(true))
        }
        socket.on(Socket.EVENT_ERROR) {
            if (it.isEmpty()) {
                completionHandler(RemoteResponse.createError(ConnectionError.ErrorWithoutData))
                completionHandler(RemoteResponse.createError(ConnectionError.ErrorWithoutData))
            } else if (it[0] is JSONObject) {
                Log.e("Error message", (it[0] as JSONObject)["message"] as String)
                completionHandler(RemoteResponse.createError(ConnectionError.TokenVerificationError))
            } else if (it[0] is String) {
                val knownError = ConnectionError(rawValue = it[0] as String)
                if (knownError != null) {
                    completionHandler(RemoteResponse.createError(knownError))
                } else {
                    completionHandler(RemoteResponse.createError(ConnectionError.Unknown))
                }
            } else {
                completionHandler(RemoteResponse.createError(ConnectionError.NotDecodableError))
            }
        }
            socket.on("cancelRequest") { item ->
                GlobalScope.launch(Dispatchers.Main) {
                    onCancelRequest?.invoke(item[0] as Long)
                }
            }
        // Driver Events
        socket.on("requestReceived") { item ->
            val travel = Adapters.moshi.adapter<Request>(Request::class.java).fromJson(item[0].toString())
            GlobalScope.launch(Dispatchers.Main) {
                onNewRequest?.invoke(travel!!)
            }
        }
        newMessage = Observable.create {
            socket.on("messageReceived") { item ->
                val message = Adapters.moshi.adapter<ChatMessage>(ChatMessage::class.java).fromJson(item[0].toString())
                it.onNext(message!!)
            }
        }
        socket.on("cancelTravel") { _ ->
            GlobalScope.launch(Dispatchers.Main) {
                onCancel?.invoke(0)
            }
        }
            socket.on("paid") { _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    onPaid?.invoke(0)
                }
            }
        arrived = Observable.create {
            socket.on("arrived") { _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    it.onNext(0)
                }
            }
        }
        started = Observable.create {
            socket.on("started") { item ->
                val travel = Adapters.moshi.adapter<Request>(Request::class.java).fromJson(item[0].toString())
                GlobalScope.launch(Dispatchers.Main) {
                    it.onNext(travel!!)
                }
            }
        }
        travelInfo = Observable.create {
            socket.on("travelInfoReceived") { item ->
                val json = item[0] as JSONObject
                val lng = json.getDouble("x")
                val lat = json.getDouble("y")
                val loc = LatLng(lat, lng)
                GlobalScope.launch(Dispatchers.Main) {
                    it.onNext(loc)
                }
            }
        }
        socket.on("Finished") { item ->
            GlobalScope.launch(Dispatchers.Main) {
                if(item[1] is Int) {
                    onFinished?.invoke(FinishResult(item[0] as Boolean, (item[1] as Int).toDouble()))
                } else {
                    onFinished?.invoke(FinishResult(item[0] as Boolean, item[1] as Double))

                }
            }
        }
        socket.on("driverAccepted") { item ->
            val travel = Adapters.moshi.adapter<Request>(Request::class.java).fromJson(item[0].toString())
            GlobalScope.launch(Dispatchers.Main) {
                onDriverAccepted?.invoke(travel!!)
            }
        }
        /*

        socket.on("driverInfoChanged") {
            UserDefaultsConfig.user = (Driver(from = data[0] as Any).asDictionary())
        }


        //Rider Events
        socket.on("riderInfoChanged") {
            UserDefaultsConfig.user = (Rider(from = data[0] as Any).asDictionary())
        }


        */
        socket.connect()
    }

    fun disconnect() {
        socket.disconnect()
    }
}

enum class Namespace(val rawValue: String) {
    Driver("drivers"), Rider("riders")
}

enum class ConnectionError(val rawValue: String) {
    @Json(name = "VersionOutdated")
    VersionOutdated("VersionOutdated"),
    @Json(name="NotFound")
    NotFound("NotFound"),
    @Json(name="NotFound")
    Blocked("Blocked"),
    @Json(name="RegistrationIncomplete")
    RegistrationIncomplete("RegistrationIncomplete"),
    @Json(name="TokenVerificationError")
    TokenVerificationError("TokenVerificationError"),
    @Json(name="NotDecodableError")
    NotDecodableError("NotDecodableError"),
    @Json(name="Unknown")
    Unknown("Unknown"),
    @Json(name="ErrorWithoutData")
    ErrorWithoutData("ErrorWithoutData");

    fun showAlert(context: Context) {
        AlertDialogBuilder.show(context, this.toString(), AlertDialogBuilder.DialogButton.OK, null)
    }

    companion object {
        operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }

    }
}

data class FinishResult(
        val paid: Boolean,
        val remainingAmount: Double
)

enum class SocketClientError(val rawValue: String) {
    InvalidAckParamCount("InvalidAckParamCount"), RequestTimeout("RequestTimeout");

    companion object {
        operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
    }

    val localizedDescription: String
        get() {
            return when (this) {
                InvalidAckParamCount -> "Result parameter count is more than one. It's unexpected."
                RequestTimeout -> "Request Timeout"
            }
        }
}
