package com.innomalist.taxi.rider.activities.travel.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.innomalist.taxi.common.models.Review
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.databinding.FragmentReviewBinding
import java.util.*

class ReviewDialog : DialogFragment() {
    lateinit var binding: FragmentReviewBinding
    private var mListener: onReviewFragmentInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
        }
    }

    fun onCreateDialogView(inflater: LayoutInflater?, container: ViewGroup?): View {
        binding = DataBindingUtil.inflate(inflater!!, R.layout.fragment_review, container, false)
        return binding.getRoot()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(context!!)
        alertDialogBuilder.setTitle(R.string.review_dialog_title)
        val view = onCreateDialogView(activity!!.layoutInflater, null)
        onViewCreated(view, null)
        alertDialogBuilder.setView(view)
        binding.ratingBar.onRatingBarChangeListener = OnRatingBarChangeListener { ratingBar: RatingBar?, v: Float, b: Boolean ->
            val dialog = dialog as AlertDialog?
            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
        alertDialogBuilder.setPositiveButton(getString(R.string.alert_ok)) { _: DialogInterface?, which: Int -> mListener!!.onReviewTravelClicked(Review(binding.ratingBar.rating.toInt() * 20, binding.reviewText.text.toString(), 0)) }
        return alertDialogBuilder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is onReviewFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement onEditAddressInteractionListener")
        }
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog as AlertDialog?
        dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface onReviewFragmentInteractionListener {
        fun onReviewTravelClicked(review: Review)
    }

    companion object {
        fun newInstance(): ReviewDialog { /*Bundle args = new Bundle();
        args.putSerializable(ARG_ADDRESS, param1);
        fragment.setArguments(args);*/
            return ReviewDialog()
        }
    }
}