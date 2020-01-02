package com.innomalist.taxi.common.networking.socket.interfaces

import android.content.Context
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteError(
        var status: ErrorStatus,
        var message: String? = null)
enum class ErrorStatus (val rawValue: String) {
    DistanceCalculationFailed("DistanceCalculationFailed"), DriversUnavailable("DriversUnavailable"), ConfirmationCodeRequired("ConfirmationCodeRequired"), ConfirmationCodeInvalid("ConfirmationCodeInvalid"), OrderAlreadyTaken("OrderAlreadyTaken"), Unknown("Unknown"), Networking("Networking"), FailedEncoding("FailedEncoding"), FailedToVerify("FailedToVerify"), RegionUnsupported("RegionUnsupported"), NoServiceInRegion("NoServiceInRegion");

    companion object {
        operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        fun showAlert(context: Context) {
            AlertDialogBuilder.show(context, this.toString(), AlertDialogBuilder.DialogButton.OK, null)
        }
    }
}
