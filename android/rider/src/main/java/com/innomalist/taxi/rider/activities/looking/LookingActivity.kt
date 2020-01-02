package com.innomalist.taxi.rider.activities.looking

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.networking.socket.CurrentRequestResult
import com.innomalist.taxi.common.networking.socket.GetCurrentRequestInfo
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.networking.socket.interfaces.SocketNetworkDispatcher
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.innomalist.taxi.common.utils.AlertDialogBuilder.show
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.databinding.ActivityLookingBinding
import com.innomalist.taxi.rider.networking.socket.CancelRequest
import com.innomalist.taxi.rider.ui.RiderBaseActivity

class LookingActivity : RiderBaseActivity() {
    lateinit var binding: ActivityLookingBinding
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setImmersive(true)
        binding = DataBindingUtil.setContentView(this@LookingActivity, R.layout.activity_looking)
        SocketNetworkDispatcher.instance.onDriverAccepted = {
            runOnUiThread {
                binding.loadingIndicator.pauseAnimation()
                val intent = Intent()
                travel = it
                this.setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    fun onCancelRequest(view: View) {
        CancelRequest().execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    this.setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.message)
                }
            }

        }

    }

    override fun onResume() {
        super.onResume()
        GetCurrentRequestInfo().execute<CurrentRequestResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    travel = it.body.request
                    refreshPage()
                }

                is RemoteResponse.Error -> {
                    this.setResult(Activity.RESULT_CANCELED)
                    finish()
                    AlerterHelper.showError(this, it.error.status.rawValue)
                }
            }

        }
    }

    private fun refreshPage() {
        val request = travel
        when (request!!.status) {
            Request.Status.Requested, Request.Status.Found, Request.Status.NotFound, Request.Status.Booked, Request.Status.NoCloseFound -> {

            }
            Request.Status.DriverAccepted -> {
                binding.loadingIndicator.pauseAnimation()
                val intent = Intent()
                this.setResult(Activity.RESULT_OK, intent)
                finish()
            }
            Request.Status.DriverCanceled, Request.Status.RiderCanceled -> {
                this.setResult(Activity.RESULT_CANCELED)
                finish()
            }
            Request.Status.Expired -> show(this@LookingActivity, "Sadly your request wasn't accepted in appropriate time and it is expired now.", AlertDialogBuilder.DialogButton.OK, AlertDialogEvent {
                this@LookingActivity.setResult(Activity.RESULT_CANCELED)
                finish()
            })
            else -> {
                AlerterHelper.showError(this, "An unknown state: ${request.status?.value ?: "Not Decoded"}")
            }
        }
    }
}