package com.innomalist.taxi.driver.activities.statistics

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.utils.AlerterHelper.showInfo
import com.innomalist.taxi.driver.R
import com.innomalist.taxi.driver.databinding.ActivityStatisticsBinding
import com.innomalist.taxi.driver.networking.socket.RequestPayment

class StatisticsActivity : BaseActivity() {
    lateinit var binding: ActivityStatisticsBinding
    //var incomeLineChart: IncomeLineChart? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_statistics)
        binding.setDriver(preferences.driver)
        initializeToolbar(getString(R.string.title_activity_statistics))
        binding.tabDate.addOnTabSelectedListener(tabSelectedListener)
        binding.tabDate.isEnabled = false
        //TODO: Payment bound
        //binding.textPaymentBound.text = getString(R.string.payment_bound, getString(R.string.unit_money, CommonUtils.driver.getBalance()), getString(R.string.unit_money, resources.getInteger(R.integer.minimum_payment_request).toDouble()))
        if (!resources.getBoolean(R.bool.request_payment_enabled)) {
            binding.paymentRequestCard.visibility = View.GONE
        }
    }

    var tabSelectedListener: OnTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            //eventBus.post(GetStatisticsEvent(get(tab.position + 1)!!))
            binding.tabDate.isEnabled = false
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}
        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    //@Subscribe(threadMode = ThreadMode.MAIN)
    fun onChartUpdated() {
        binding.chart.dismissAllTooltips()
        binding.chart.reset()
        /*if (event.reports != null && event.reports!!.size != 0) {
            val labels = ArrayList<String?>()
            for (report in event.reports!!) {
                val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                var date: Date?
                try {
                    date = formatter.parse(report.date!!.replace("T", " ").replace("Z", ""))
                    if (resources.getBoolean(com.innomalist.taxi.common.R.bool.use_date_converter)) {
                        labels.add(getDate(date))
                    } else {
                        labels.add(SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date))
                    }
                } catch (e: ParseException) {
                    labels.add(report.date)
                }
            }
            val mLabels = labels.toTypedArray()
            val data = ArrayList<Float?>()
            for (report in event.reports!!) {
                data.add(report.amount)
            }
            val mValues = FloatArray(data.size)
            var i = 0
            for (f in data) {
                mValues[i++] = f ?: Float.NaN // Or whatever default you want.
            }
            incomeLineChart = IncomeLineChart(binding.chart, this@StatisticsActivity)
            incomeLineChart!!.init(mLabels, mValues)
        }
        if (event.stats != null) {
            //TODO: Show balance
            //binding.incomeText.text = if (event.stats!!.amount != null) getString(R.string.unit_money, event.stats.getAmount()) else "-"
            binding.serviceText.text = if (event.stats!!.services != 0) String.format(Locale.getDefault(), "%d", event.stats!!.services) else "-"
            binding.ratingText.text = if (event.stats!!.rating != null) String.format(Locale.getDefault(), "%.0f %%", event.stats!!.rating) else "-"
        } else {
            binding.incomeText.text = "-"
            binding.serviceText.text = "-"
            binding.ratingText.text = "-"
        }
        binding.tabDate.isEnabled = true*/
    }

    fun onPaymentRequestClicked(view: View?) {
        binding.buttonPaymentRequest.isEnabled = false
        RequestPayment().execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    showInfo(this@StatisticsActivity, getString(R.string.message_payment_request_sent))

                }

                is RemoteResponse.Error -> {
                }
            }

        }
    }
}