package com.innomalist.taxi.rider.services


/*class RiderService : Service() {
    lateinit var socket: Socket
    var vibe: Vibrator? = null
    var eventBus = EventBus.getDefault()
    @Subscribe
    fun connectSocket(connectEvent: ConnectEvent) {
        try {
            val options = IO.Options()
            if (socket.connected()) {
                eventBus.post(ConnectResultEvent(ServerResponse.OK.value))
                return
            }
            options.query = "token=" + connectEvent.token + "&os=android&version=" + BuildConfig.VERSION_CODE
            socket = IO.socket(getString(R.string.server_address) + "client", options)
            socket.on(Socket.EVENT_CONNECT) { args: Array<Any?>? ->
                eventBus.post(ConnectResultEvent(ServerResponse.OK.value))
                eventBus.post(SocketConnectionEvent(Socket.EVENT_CONNECT))
            }
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
                            socket.disconnect()
                            val obj = JSONObject(args[0].toString())
                            eventBus.post(ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.value, obj.getString("message")))
                        } catch (c: JSONException) {
                            eventBus.post(ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.value, args[0].toString()))
                        }
                    }.on("driverInLocation") { args: Array<Any?>? ->
                        try {
                            val mBuilder = NotificationCompat.Builder(this@RiderService)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                                    .setContentText(getString(R.string.notification_driver_in_location))
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager?.notify(0, mBuilder.build())
                        } catch (c: Exception) {
                            c.printStackTrace()
                        }
                    }.on("startTravel") { args: Array<Any?> -> eventBus.post(ServiceStartedEvent(*args)) }.on("cancelTravel") { args: Array<Any?>? -> eventBus.post(ServiceCancelResultEvent(200)) }
                    .on("driverAccepted") { args: Array<Any?> -> eventBus.post(DriverAcceptedEvent(*args)) }
                    .on("finishedTaxi") { args: Array<Any> -> eventBus.post(ServiceFinishedEvent(args[0] as Int, args[1] as Boolean, args[2].toString().toDouble())) }
                    .on("riderInfoChanged") { args: Array<Any> ->
                        val SP = MyPreferenceManager(applicationContext)
                        SP.putString("rider_user", args[0].toString())
                        CommonUtils.rider = Rider.fromJson(args[0].toString())
                        eventBus.postSticky(ProfileInfoChangedEvent())
                    }
                    .on("messageReceived") { args: Array<Any?> -> eventBus.post(MessageReceivedEvent(*args)) }
                    .on("travelInfoReceived") { args: Array<Any?> -> eventBus.post(GetTravelInfoResultEvent(*args)) }
            socket.connect()
        } catch (exc: Exception) {
            Log.e(exc.toString(), "Socket connection error")
        }
    }

    @Subscribe
    fun getStatus(event: GetStatusEvent?) {
        socket.emit("getStatus", Ack { args: Array<Any?> -> eventBus.post(GetStatusResultEvent(*args)) })
    }

    @Subscribe
    fun EditProfile(editProfileInfoEvent: EditProfileInfoEvent) {
        socket.emit("editProfile", editProfileInfoEvent.userInfo, Ack { args: Array<Any> -> eventBus.post(EditProfileInfoResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun requestTaxi(event: ServiceRequestEvent) {
        socket.emit("requestTaxi", event.jsonObject, Ack { args: Array<Any> ->
            if (args[0] as Int == 200) eventBus.post(ServiceRequestResultEvent(args[1] as Int)) else {
                if (args[0] as Int == 666) eventBus.post(ServiceRequestErrorEvent(args[0] as Int, args[1].toString())) else eventBus.post(ServiceRequestErrorEvent(args[0] as Int))
            }
        })
    }

    @Subscribe
    fun cancelTravel(event: ServiceCancelEvent?) {
        socket.emit("cancelTravel", Ack { args: Array<Any?>? -> eventBus.post(ServiceCancelResultEvent(ServerResponse.OK.value)) })
    }

    @Subscribe
    fun cancelRequest(event: CancelRequestRequestEvent?) {
        socket.emit("cancelRequest")
    }

    @Subscribe
    fun acceptDriver(acceptDriverEvent: AcceptDriverEvent) {
        socket.emit("riderAccepted", acceptDriverEvent.driverId)
    }

    @Subscribe
    fun getTravels(getTravelsEvent: GetTravelsEvent?) {
        socket.emit("getTravels", Ack { args: Array<Any> -> eventBus.postSticky(GetTravelsResultEvent(args[0] as Int, args[1].toString())) })
    }

    @Subscribe
    fun getTravelStatus(event: GetTravelStatus) {
        socket.emit("getTravelStatus", event.travelId, Ack { args: Array<Any?> -> eventBus.post(GetTravelStatusResultEvent(*args)) })
    }

    @Subscribe
    fun getDriverLocation(event: GetTravelInfoEvent?) {
        socket.emit("getTravelInfo")
    }

    @Subscribe
    fun reviewDriver(event: ReviewDriverEvent) {
        socket.emit("reviewDriver", event.review.score, event.review.review, Ack { args: Array<Any> -> eventBus.post(ReviewDriverResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun ChangeProfileImage(changeProfileImageEvent: ChangeProfileImageEvent) {
        val file = File(changeProfileImageEvent.path)
        val data = ByteArray(file.length().toInt())
        try {
            val len = FileInputStream(file).read(data)
            if (len > 0) socket.emit("changeProfileImage", data, Ack { args: Array<Any> -> eventBus.post(ChangeProfileImageResultEvent(args[0] as Int, args[1].toString())) })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe
    fun getDriversLocation(event: GetDriversLocationEvent) {
        socket.emit("getDriversLocation", event.point, Ack { args: Array<Any> -> eventBus.post(GetDriversLocationResultEvent(args[0] as Int, args[1] as JSONArray)) })
    }

    @Subscribe
    fun WriteComplaint(event: WriteComplaintEvent) {
        socket.emit("writeComplaint", event.travelId, event.subject, event.content, Ack { args: Array<Any> -> eventBus.post(WriteComplaintResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun chargeAccount(event: ChargeAccountEvent) {
        socket.emit("chargeAccount", event.type, event.stripeToken, event.amount, Ack { args: Array<Any?> -> eventBus.post(ChargeAccountResultEvent(*args)) })
    }

    @Subscribe
    fun HideTravel(event: HideTravelEvent) {
        socket.emit("hideTravel", event.travelId, Ack { args: Array<Any?>? -> eventBus.post(HideTravelResultEvent(ServerResponse.OK.value)) })
    }

    @Subscribe
    fun callRequest(event: ServiceCallRequestEvent?) {
        socket.emit("callRequest", Ack { args: Array<Any> -> eventBus.post(ServiceCallRequestResultEvent(args[0] as Int)) })
    }

    @Subscribe
    fun onCalculateFareRequested(event: CalculateFareRequestEvent) {
        socket.emit("calculateFare", event.points, Ack { args: Array<Any?> -> eventBus.post(CalculateFareResultEvent(*args)) })
    }

    @Subscribe
    fun crudAddress(event: CRUDAddressRequestEvent) {
        socket.emit("crudAddress", event.crud.getValue(), event.address, Ack { args: Array<Any> -> if (event.crud === CRUD.READ) eventBus.post(CRUDAddressResultEvent(args[0] as Int, event.crud, args[1] as JSONArray)) else eventBus.post(CRUDAddressResultEvent(args[0] as Int, event.crud)) })
    }

    @Subscribe
    fun getCoupons(event: GetCouponsRequestEvent?) {
        socket.emit("getCoupons", Ack { args: Array<Any?> -> eventBus.post(GetCouponsResultEvent(*args)) })
    }

    @Subscribe
    fun getPromotions(event: GetPromotionsRequestEvent?) {
        socket.emit("getPromotions", Ack { args: Array<Any?> -> eventBus.post(GetPromotionsResultEvent(*args)) })
    }

    @Subscribe
    fun applyCoupon(event: ApplyCouponRequestEvent) {
        socket.emit("applyCoupon", event.code, Ack { args: Array<Any?> -> eventBus.post(ApplyCouponResultEvent(*args)) })
    }

    @Subscribe
    fun addCoupon(event: AddCouponRequestEvent) {
        socket.emit("addCoupon", event.code, Ack { args: Array<Any?> -> eventBus.post(AddCouponResultEvent(*args)) })
    }

    @Subscribe
    fun getTransactions(event: GetTransactionsRequestEvent?) {
        socket.emit("getTransactions", Ack { args: Array<Any?> -> eventBus.post(GetTransactionsResultEvent(*args)) })
    }

    @Subscribe
    fun notificationPlayerId(event: NotificationPlayerId) {
        socket.emit("notificationPlayerId", event.playerId)
    }

    @Subscribe
    fun sendMessage(event: SendMessageEvent) {
        socket.emit("sendMessage", event.content, Ack { args: Array<Any?> -> eventBus.post(SendMessageResultEvent(*args)) })
    }

    @Subscribe
    fun enableVerification(event: ConfirmationCodeEvent?) {
        socket.emit("enableConfirmation", Ack { args: Array<Any?> -> eventBus.post(ConfirmationCodeResultEvent(*args)) })
    }

    @Subscribe
    fun getMessages(event: GetMessagesEvent?) {
        socket.emit("getMessages", Ack { args: Array<Any?> -> eventBus.post(GetMessagesResultEvent(*args)) })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        vibe = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        EventBus.getDefault().register(this)
        eventBus.post(BackgroundServiceStartedEvent())
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}*/