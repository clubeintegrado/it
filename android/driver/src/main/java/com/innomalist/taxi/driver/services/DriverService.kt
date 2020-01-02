package com.innomalist.taxi.driver.services


/*class DriverService : Service() {
    var locationCallback: LocationCallback? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    lateinit var socket: Socket
    var vibe: Vibrator? = null
    var eventBus = EventBus.getDefault()
    @Subscribe
    fun connectSocket(event: ConnectEvent) {
        try {
            val options = IO.Options()
            options.query = "token=" + event.token + "&os=android&version=" + BuildConfig.VERSION_CODE
            socket = IO.socket(getString(R.string.server_address) + "client", options)
            socket.on(Socket.EVENT_CONNECT, Emitter.Listener { args: Array<Any?>? ->
                eventBus.post(ConnectResultEvent(ServerResponse.OK.value))
                eventBus.post(SocketConnectionEvent(Socket.EVENT_CONNECT))
            })
                    .on(Socket.EVENT_DISCONNECT) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_DISCONNECT)) }
                    .on(Socket.EVENT_CONNECTING) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_CONNECTING)) }
                    .on(Socket.EVENT_CONNECT_ERROR) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_CONNECT_ERROR)) }
                    .on(Socket.EVENT_CONNECT_TIMEOUT) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_CONNECT_TIMEOUT)) }
                    .on(Socket.EVENT_RECONNECT) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_RECONNECT)) }
                    .on(Socket.EVENT_RECONNECTING) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_RECONNECTING)) }
                    .on(Socket.EVENT_RECONNECT_ATTEMPT) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_RECONNECT_ATTEMPT)) }
                    .on(Socket.EVENT_RECONNECT_ERROR) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_RECONNECT_ERROR)) }
                    .on(Socket.EVENT_RECONNECT_FAILED) { args: Array<Any?>? -> eventBus.post(SocketConnectionEvent(Socket.EVENT_RECONNECT_FAILED)) }
                    .on("error") { args: Array<Any> ->
                        try {
                            val obj = JSONObject(args[0].toString())
                            eventBus.post(ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.value, obj.getString("message")))
                        } catch (c: JSONException) {
                            eventBus.post(ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.value, args[0].toString()))
                        }
                    }
                    .on("requestReceived") { args: Array<Any?> -> eventBus.post(RequestReceivedEvent(args)) }
                    .on("cancelRequest") { args: Array<Any?> -> eventBus.post(CancelRequestEvent(args)) }
                    .on("driverInfoChanged") { args: Array<Any> ->
                        val SP = MyPreferenceManager(applicationContext)
                        SP.putString("driver_user", args[0].toString())
                        CommonUtils.driver = Gson().fromJson(args[0].toString(), Driver::class.java)
                        eventBus.postSticky(ProfileInfoChangedEvent())
                    }
                    .on("messageReceived") { args: Array<Any?> -> eventBus.post(MessageReceivedEvent(args)) }
                    .on("cancelTravel") { args: Array<Any?>? -> eventBus.post(ServiceCancelResultEvent(200)) }
            socket.connect()
        } catch (c: Exception) {
            Log.e("Connect Socket", c.message)
        }
    }

    @Subscribe
    fun getStatus(event: GetStatusEvent?) {
        socket.emit("getStatus", Ack { args: Array<Any?> -> eventBus.post(GetStatusResultEvent(args)) })
    }

    @Subscribe
    fun login(event: LoginEvent) {
        LoginRequest().execute(event.userName.toString(), event.versionNumber.toString())
    }

    @Subscribe
    fun FinishTravel(event: ServiceFinishEvent) {
        socket.emit("finishedTaxi", event.finishService.toJson(), Ack { args: Array<Any?> -> eventBus.post(ServiceFinishResultEvent(args)) })
    }

    @Subscribe
    fun PaymentRequested(event: PaymentRequestEvent?) {
        socket.emit("requestPayment", Ack { args: Array<Any> -> eventBus.post(PaymentRequestResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun editProfile(event: EditProfileInfoEvent) {
        socket.emit("editProfile", event.userInfo, Ack { args: Array<Any?> -> eventBus.post(EditProfileInfoResultEvent((args[0] as Int?)!!)) })
    }

    @Subscribe
    fun notificationPlayerId(event: NotificationPlayerId) {
        socket.emit("notificationPlayerId", event.playerId)
    }

    @Subscribe
    fun acceptOrder(event: AcceptOrderEvent) {
        socket.emit("driverAccepted", event.travelId, Ack { args: Array<Any?> -> eventBus.post(AcceptOrderResultEvent(args)) })
    }

    @Subscribe
    fun changeStatus(event: ChangeStatusEvent) {
        socket.emit("changeStatus", event.status.value, Ack { args: Array<Any> -> eventBus.post(ChangeStatusResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun serviceInLocation(event: ServiceInLocationEvent?) {
        socket.emit("buzz")
    }

    @Subscribe
    fun startTaxi(event: ServiceStartEvent?) {
        socket.emit("startTravel", Ack { args: Array<Any?> -> eventBus.post(ServiceStartResultEvent(args)) })
    }

    @Subscribe
    fun callRequest(event: ServiceCallRequestEvent?) {
        socket.emit("callRequest", Ack { args: Array<Any> -> eventBus.post(ServiceCallRequestResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun cancelTaxi(event: ServiceCancelEvent?) {
        socket.emit("cancelTravel", Ack { args: Array<Any> -> eventBus.post(ServiceCancelResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun getTravels(event: GetTravelsEvent?) {
        socket.emit("getTravels", Ack { args: Array<Any> -> eventBus.post(GetTravelsResultEvent(args[0] as Int, args[1].toString())) })
    }

    @Subscribe
    fun locationChanged(event: LocationChangedEvent) {
        socket.emit("locationChanged", event.location.latitude, event.location.longitude)
    }

    @Subscribe
    fun chargeAccount(event: ChargeAccountEvent) {
        socket.emit("chargeAccount", event.type, event.stripeToken, event.amount, Ack { args: Array<Any?> -> eventBus.post(ChargeAccountResultEvent(args)) })
    }

    @Subscribe
    fun ChangeProfileImage(event: ChangeProfileImageEvent) {
        val file = File(event.path)
        val data = ByteArray(file.length().toInt())
        try {
            val len = FileInputStream(file).read(data)
            if (len > 0) socket.emit("changeProfileImage", data, Ack { args: Array<Any> -> eventBus.post(ChangeProfileImageResultEvent(args[0] as Int, args[1].toString())) })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe
    fun HideTravel(event: HideTravelEvent) {
        socket.emit("hideTravel", event.travelId, Ack { args: Array<Any?>? -> eventBus.post(HideTravelResultEvent(ServerResponse.OK.value)) })
    }

    @Subscribe
    fun changeHeaderImage(event: ChangeHeaderImageEvent) {
        val file = File(event.path)
        val data = ByteArray(file.length().toInt())
        try {
            val len = FileInputStream(file).read(data)
            if (len > 0) socket.emit("changeHeaderImage", data, Ack { args: Array<Any> -> eventBus.post(ChangeHeaderImageResultEvent(args[0] as Int, args[1].toString())) })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe
    fun getDriverStatistics(event: GetStatisticsEvent) {
        socket.emit("getStats", event.queryTime.value, Ack { args: Array<Any?> -> eventBus.post(GetStatisticsResultEvent((args[0] as Int?)!!, if (args[1] != null) args[1].toString() else null, if (args[2] != null) args[2].toString() else null)) })
    }

    @Subscribe
    fun WriteComplaint(event: WriteComplaintEvent) {
        socket.emit("writeComplaint", event.travelId, event.subject, event.content, Ack { args: Array<Any> -> eventBus.post(WriteComplaintResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun sendTravelInfo(event: SendTravelInfoEvent) {
        socket.emit("travelInfo", event.location.latitude, event.location.longitude)
    }

    @Subscribe
    fun getTransactions(event: GetTransactionsRequestEvent?) {
        socket.emit("getTransactions", Ack { args: Array<Any?> -> eventBus.post(GetTransactionsResultEvent(args)) })
    }

    @Subscribe
    fun getRequests(event: GetRequestsRequestEvent?) {
        socket.emit("getRequests", Ack { args: Array<Any?> -> eventBus.post(GetRequestsResultEvent(args)) })
    }

    @Subscribe
    fun sendMessage(event: SendMessageEvent) {
        socket.emit("sendMessage", event.content, Ack { args: Array<Any?> -> eventBus.post(SendMessageResultEvent(args)) })
    }

    @Subscribe
    fun getMessages(event: GetMessagesEvent?) {
        socket.emit("getMessages", Ack { args: Array<Any?> -> eventBus.post(GetMessagesResultEvent(args)) })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        vibe = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        eventBus.post(BackgroundServiceStartedEvent())
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        stopTracking(null)
    }

    override fun onBind(intent: Intent): IBinder? { //return new LocationServiceBinder();
        return null
    }

    private fun initializeLocationManager() {
        if (mFusedLocationClient == null) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }
    }

    @Subscribe
    @SuppressLint("MissingPermission")
    fun startTracking(event: StartTrackingRequestEvent?) {
        initializeLocationManager()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                socket.emit("locationChanged", locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
            }
        }
        mFusedLocationClient!!.requestLocationUpdates(LocationRequest.create(), locationCallback, null)
    }

    @Subscribe
    fun stopTracking(event: StopTrackingRequestEvent?) {
        if (mFusedLocationClient != null) {
            try {
                mFusedLocationClient!!.removeLocationUpdates(locationCallback)
            } catch (ignored: Exception) {
            }
        }
    }

    private val notification: Notification
        private get() {
            if (Build.VERSION.SDK_INT > 26) {
                val channel = NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
            val builder = NotificationCompat.Builder(applicationContext, "channel_01").setAutoCancel(true)
            return builder.build()
        }

    inner class LocationServiceBinder : Binder() {
        val service: DriverService
            get() = this@DriverService
    }

    private inner class LoginRequest : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg uri: String): String? {
            return try {
                val url = URL(getString(R.string.server_address) + "driver_login")
                val client = url.openConnection() as HttpURLConnection
                client.requestMethod = "POST"
                client.doOutput = true
                client.doInput = true
                val wr = DataOutputStream(client.outputStream)
                val postDataParams = HashMap<String, String>()
                postDataParams["user_name"] = uri[0]
                postDataParams["version"] = uri[1]
                val result = StringBuilder()
                var first = true
                for ((key, value) in postDataParams) {
                    if (first) first = false else result.append("&")
                    result.append(URLEncoder.encode(key, "UTF-8"))
                    result.append("=")
                    result.append(URLEncoder.encode(value, "UTF-8"))
                }
                wr.write(result.toString().toByteArray())
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                sb.toString()
            } catch (c: Exception) {
                c.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val obj = JSONObject(result)
                val status = obj.getInt("status")
                if (status == 200 || status == 411) eventBus.post(LoginResultEvent(obj.getInt("status"), obj.getString("user"), obj.getString("token"))) else eventBus.post(LoginResultEvent(status,obj.getString("token"), obj.getString("user")))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}*/