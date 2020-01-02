package com.innomalist.taxi.common.activities.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.innomalist.taxi.common.R
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.models.ChatMessage
import com.innomalist.taxi.common.networking.socket.GetMessages
import com.innomalist.taxi.common.networking.socket.SendMessage
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.networking.socket.interfaces.SocketNetworkDispatcher
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.common.utils.TravelRepository
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChatActivity : BaseActivity() {
    private lateinit var messagesList: MessagesList
    private lateinit var messageInput: MessageInput
    var app: String? = null
    private lateinit var adapter: MessagesListAdapter<ChatMessage>
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        initializeToolbar(getString(R.string.chat_activity_title))
        val imageLoader = ImageLoader { imageView: ImageView?, url: String?, payload: Any? -> Glide.with(this@ChatActivity).load(url).into(imageView!!) }
        app = intent.extras!!.getString("app")
        if (app == null) app = "rider"
        adapter = MessagesListAdapter(if (app == "driver") preferences.driver!!.id.toString() else preferences.rider!!.getId(), imageLoader)
        messagesList = findViewById(R.id.messages_list)
        messageInput = findViewById(R.id.message_input)
        messagesList.setAdapter(adapter)
        messageInput.setInputListener { input: CharSequence ->
            SendMessage(input.toString()).execute<ChatMessage> {
                when(it) {
                    is RemoteResponse.Success -> {
                        it.body.request = TravelRepository.get(this@ChatActivity, if (app == "driver") TravelRepository.AppType.DRIVER else TravelRepository.AppType.RIDER)
                        adapter.addToStart(it.body, true)
                    }

                    is RemoteResponse.Error -> {
                        AlerterHelper.showError(this, it.error.status.name)
                    }
                }

            }
            true
        }
        SocketNetworkDispatcher.instance.newMessage.subscribe {
            GlobalScope.launch(Dispatchers.Main) {
                it.request = TravelRepository.get(this@ChatActivity, if (app == "driver") TravelRepository.AppType.DRIVER else TravelRepository.AppType.RIDER)
                adapter.addToStart(it, true)
            }
        }
        GetMessages().executeArray<ChatMessage> {
            when(it) {
                is RemoteResponse.Success -> {
                    adapter.clear()
                    val request = TravelRepository.get(this@ChatActivity, if (app == "driver") TravelRepository.AppType.DRIVER else TravelRepository.AppType.RIDER)
                    for (message in it.body.iterator()) {
                        message.request = request
                    }
                    adapter.addToEnd(it.body, true)
                }

                is RemoteResponse.Error -> {

                }
            }

        }
    }
}