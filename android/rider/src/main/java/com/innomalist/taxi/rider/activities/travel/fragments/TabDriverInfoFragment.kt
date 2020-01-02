package com.innomalist.taxi.rider.activities.travel.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.innomalist.taxi.common.components.BaseFragment
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.utils.Adapters
import com.innomalist.taxi.common.utils.DataBinder.setMedia
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.databinding.FragmentTravelDriverBinding

class TabDriverInfoFragment : BaseFragment() {
    lateinit var binding: FragmentTravelDriverBinding
    lateinit var request: Request

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            request = Adapters.moshi.adapter<Request>(Request::class.java).fromJson(arguments!!.getString(ARG_REQUEST)!!)!!
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_travel_driver, container, false)
        binding.textPickup.isSelected = true
        binding.textDestination.isSelected = true
        binding.textPickup.text = request.addresses[0]
        binding.textDestination.text = request.addresses[1]
        if (request.driver != null) {
            if (!request.driver!!.firstName.isNullOrBlank() || !request.driver!!.lastName.isNullOrBlank()) binding.textDriverName.text = "${request.driver!!.firstName} ${request.driver!!.lastName}"
            var carName: String? = "-"
            if (request.driver!!.car != null && request.driver!!.car!!.title != null) carName = request.driver!!.car!!.title
            if (request.driver!!.carColor != null) carName += " " + request.driver!!.carColor
            if (request.driver!!.carPlate != null) carName += ", " + request.driver!!.carPlate
            binding.textCarName.text = carName
            setMedia(binding.imageDriver, request.driver!!.carMedia)
        }
        return binding.root
    }

    companion object {
        private const val ARG_REQUEST = "request"

        fun newInstance(request: Request): TabDriverInfoFragment {
            val tabDriverInfoFragment = TabDriverInfoFragment()
            val args = Bundle()
            val str = Adapters.moshi.adapter<Request>(Request::class.java).toJson(request)
            args.putString(ARG_REQUEST, str)
            tabDriverInfoFragment.arguments = args
            return tabDriverInfoFragment
        }
    }
}