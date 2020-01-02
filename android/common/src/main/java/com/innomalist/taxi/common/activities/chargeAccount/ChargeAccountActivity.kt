package com.innomalist.taxi.common.activities.chargeAccount

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import com.braintreepayments.api.dropin.DropInActivity
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.cooltechworks.creditcarddesign.CardEditActivity
import com.cooltechworks.creditcarddesign.CreditCardUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.innomalist.taxi.common.R
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.databinding.ActivityChargeAccountBinding
import com.innomalist.taxi.common.models.PaymentGateway
import com.innomalist.taxi.common.models.PaymentGatewayType
import com.innomalist.taxi.common.models.WalletItem
import com.innomalist.taxi.common.networking.socket.WalletInfo
import com.innomalist.taxi.common.networking.socket.WalletInfoResult
import com.innomalist.taxi.common.networking.socket.WalletTopUp
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.utils.AlerterHelper
import com.stripe.android.Stripe
import com.stripe.android.TokenCallback
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import kotlinx.android.synthetic.main.activity_charge_account.*
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class ChargeAccountActivity : BaseActivity() {
    lateinit var binding: ActivityChargeAccountBinding
    val GET_NEW_CARD = 2
    var selectedPayment: PaymentGateway? = null
    var paymentGateways: List<PaymentGateway> = ArrayList()
    var currency: String? = null
    var walletItems: List<WalletItem> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_charge_account)
        initializeToolbar(getString(R.string.title_wallet))
        binding.loadingMode = true
        WalletInfo().execute<WalletInfoResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    this.paymentGateways = it.body.gateways
                    this.walletItems = it.body.wallet.sortedByDescending { x -> x.amount }
                    val items = this.walletItems.map { walletItem ->
                        val nf = NumberFormat.getCurrencyInstance()
                        nf.currency = Currency.getInstance(walletItem.currency!!)
                        nf.format(walletItem.amount)
                    }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
                    binding.balanceAutocomplete.setAdapter(adapter)
                    binding.balanceAutocomplete.setOnItemClickListener { parent, view, position, id ->
                        currency = walletItems[position].currency
                    }
                    if(it.body.wallet.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        binding.balanceAutocomplete.setText(items[0], false)
                    }
                    if(it.body.gateways.size > 1) {
                        for (gw in it.body.gateways) {
                            val btn = MaterialButton(ContextThemeWrapper(this, R.style.Widget_MaterialComponents_Button_TextButton))
                            btn.id = gw.id
                            btn.text = gw.title
                            val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 0, 8, 0)
                            params.weight = 1f
                            btn.layoutParams = params
                            btn.setOnClickListener {
                                selectedPayment = gw
                                binding.checkoutButton.isEnabled = true
                                binding.checkoutButton.text = getString(R.string.checkout_filled, gw.title)
                            }
                            binding.layoutMethods.addView(btn)
                            binding.loadingMode = false

                        }
                    } else if(it.body.gateways.size == 1) {
                        binding.paymentToggleLayout.visibility = View.GONE
                    } else {
                        AlerterHelper.showError(this, "No Payment Gateway found.")
                        finish()
                    }
                    if(intent.getStringExtra("currency") != null) {
                        val str = intent.getDoubleExtra("defaultAmount", 0.0).roundToInt().toString()
                        text_amount.setText(str)
                        //binding.editText.setText()
                        this.currency = intent.getStringExtra("currency")
                        //binding.editText.isEnabled = false
                        binding.chargeAddFirst.visibility = View.GONE
                        binding.chargeAddSecond.visibility = View.GONE
                        binding.chargeAddThird.visibility = View.GONE
                        if(it.body.gateways.size == 1 && it.body.gateways.first().type == PaymentGatewayType.Braintree) {
                            this.selectedPayment = it.body.gateways.first()
                            this.startBraintree()
                        }
                    }
                }
                
                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                    finish()
                }
            }
        }
        binding.paymentToggleLayout.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (!isChecked) {
                selectedPayment = null
                binding.checkoutButton.isEnabled = false
                binding.checkoutButton.text = getString(R.string.checkout_empty)
                return@addOnButtonCheckedListener
            } else {
                selectedPayment = paymentGateways.first { it.id == checkedId }
            }
        }
        //binding.editText.addTextChangedListener(NumberThousandWatcher(binding.editText))
        binding.chargeAddFirst.text = resources.getInteger(R.integer.charge_first).toString()
        binding.chargeAddSecond.text = resources.getInteger(R.integer.charge_second).toString()
        binding.chargeAddThird.text = resources.getInteger(R.integer.charge_third).toString()
        binding.chargeAddFirst.setOnClickListener { addCharge(R.integer.charge_first) }
        binding.chargeAddSecond.setOnClickListener { addCharge(R.integer.charge_second) }
        binding.chargeAddThird.setOnClickListener { addCharge(R.integer.charge_third) }
    }

    fun onCheckoutClicked(view: View) {
        if(currency == null && walletItems.isNotEmpty()) {
            AlerterHelper.showError(this@ChargeAccountActivity, "Select a currency from your wallet items.")
            return
        } else if(walletItems.isEmpty() && currency == null) {
            AlerterHelper.showError(this@ChargeAccountActivity, "You can't top up your account credit right now until you do at least one travel.")
            return
        }
        if (binding.textAmount.text.toString().isEmpty()) {
            AlerterHelper.showError(this@ChargeAccountActivity, getString(R.string.error_charge_field_empty))
            return
        }
        when (selectedPayment!!.type) {
            PaymentGatewayType.Stripe -> {
                val intent = Intent(this@ChargeAccountActivity, CardEditActivity::class.java)
                startActivityForResult(intent, GET_NEW_CARD)
            }
            PaymentGatewayType.Braintree -> {
                startBraintree()
            }
            PaymentGatewayType.Flutterwave -> {
                val intent = Intent(this@ChargeAccountActivity, CardEditActivity::class.java)
                startActivityForResult(intent, GET_NEW_CARD)
            }
        }
    }

    private fun startBraintree() {
        val dropInRequest = DropInRequest().clientToken(selectedPayment!!.publicKey)
        startActivityForResult(dropInRequest.getIntent(this), REQUEST_CODE)
    }

    fun addCharge(resId: Int) {
        try {
            binding.textAmount.setText(getString(resId))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun chargeAccount(amount: Double, paymentToken: String) {
        WalletTopUp(selectedPayment!!.id, currency!!, paymentToken, amount).execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                is RemoteResponse.Error -> {

                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val amount = binding.textAmount.text.toString().replace(",", "").toDouble()
        if (requestCode == GET_NEW_CARD && resultCode == Activity.RESULT_OK) {
            val cardNumber = data!!.getStringExtra(CreditCardUtils.EXTRA_CARD_NUMBER)
            val expiryYear = data.getStringExtra(CreditCardUtils.EXTRA_CARD_EXPIRY)!!.split("/").toTypedArray()[0]
            val expiryMonth = data.getStringExtra(CreditCardUtils.EXTRA_CARD_EXPIRY)!!.split("/").toTypedArray()[1]
            val cvc = data.getStringExtra(CreditCardUtils.EXTRA_CARD_CVV)
            when(selectedPayment!!.type) {
                PaymentGatewayType.Stripe -> {
                    val card = Card(cardNumber,
                            Integer.valueOf(expiryYear),
                            Integer.valueOf(expiryMonth),
                            cvc)
                    val stripe = Stripe()
                    stripe.createToken(card, selectedPayment!!.publicKey, object : TokenCallback() {
                        override fun onSuccess(token: Token) {
                            chargeAccount(amount, token.id)
                        }

                        override fun onError(error: Exception) {
                            Log.e("Stripe", error.localizedMessage!!)
                        }
                    })
                }

                PaymentGatewayType.Flutterwave -> {
                    val token = "{\"cardNumber\":${cardNumber},\"cvv\":${cvc},\"expiryMonth\":${expiryMonth},\"expiryYear\":${expiryYear}}"
                    chargeAccount(amount, token)
                }
                else -> AlerterHelper.showError(this, "This gateway shouldn't have used in-app card input.")
            }

        }
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result: DropInResult = data!!.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT)!!
                chargeAccount(amount, result.paymentMethodNonce!!.nonce)
            } else if (resultCode == Activity.RESULT_CANCELED) { // the user canceled
            } else { // handle errors here, an exception may be available in
                val error = data!!.getSerializableExtra(DropInActivity.EXTRA_ERROR) as Exception
                AlerterHelper.showError(this@ChargeAccountActivity, error.message)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 243
    }
}