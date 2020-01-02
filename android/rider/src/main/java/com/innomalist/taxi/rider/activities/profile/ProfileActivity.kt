package com.innomalist.taxi.rider.activities.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.models.Media
import com.innomalist.taxi.common.models.Rider
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.common.utils.AlerterHelper.showError
import com.innomalist.taxi.common.utils.MyPreferenceManager
import com.innomalist.taxi.common.utils.MyPreferenceManager.Companion.getInstance
import com.innomalist.taxi.common.utils.Validators.validateEmailAddress
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.databinding.ActivityEditProfileBinding
import com.innomalist.taxi.rider.networking.socket.UpdateProfile
import com.innomalist.taxi.rider.networking.socket.UpdateProfileImage
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileInputStream
import java.util.*

class ProfileActivity : BaseActivity() {
    lateinit var binding: ActivityEditProfileBinding
    var rider: Rider? = null
    var SP: MyPreferenceManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SP = getInstance(applicationContext)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_profile)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, resources.getStringArray(R.array.genders))
        binding.genderAutocomplete.setAdapter(adapter)
        rider = preferences.rider
        binding.user = rider
        binding.profileImage.setOnClickListener(onProfileImageClicked)
        initializeToolbar("")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actionbar_save, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (rider!!.email != null && rider!!.email != "" && !validateEmailAddress(rider!!.email)) {
            showError(this@ProfileActivity, getString(R.string.error_invalid_email))
            return false
        }
        UpdateProfile(rider!!).execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                    saveUserInfo()
                }

                is RemoteResponse.Error -> {

                }
            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun saveUserInfo() {
        preferences.rider = rider
    }
    var onProfileImageClicked: View.OnClickListener = object : View.OnClickListener {
        var permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                ImagePicker.create(this@ProfileActivity)
                        .returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                        .folderMode(true) // folder mode (false by default)
                        .toolbarFolderTitle(getString(R.string.picker_folder)) // folder selection title
                        .toolbarImageTitle(getString(R.string.picker_tap_select)) // image selection title
                        .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                        .single() // single mode
                        .limit(10) // max images can be selected (99 by default)
                        .showCamera(true) // show camera or not (true by default)
                        .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                        .theme(R.style.ImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                        .start()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {}
        }

        override fun onClick(v: View) {
            TedPermission.with(this@ProfileActivity)
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage(getString(R.string.message_permission_denied))
                    .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .check()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) { // or get a single image only
            val image = ImagePicker.getFirstImageOrNull(data)
            val destinationUri = Uri.fromFile(File(cacheDir, "p.jpg"))
            UCrop.of(Uri.fromFile(File(image.path)), destinationUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(200, 200)
                    .start(this@ProfileActivity)
        }
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!) ?: return
            val file = File(resultUri.path!!)
            val dt = ByteArray(file.length().toInt())
            FileInputStream(file).read(dt)
            UpdateProfileImage(data = dt).execute<Media> {
                when(it) {
                    is RemoteResponse.Success -> {
                        rider!!.media = it.body
                        saveUserInfo()
                        assert(binding.user != null)
                        binding.user!!.media = it.body
                    }

                    is RemoteResponse.Error -> {
                        showError(this, it.error.status.rawValue)
                    }
                }

            }
        } else if (resultCode == UCrop.RESULT_ERROR) try {
            throw Objects.requireNonNull(UCrop.getError(data!!)!!)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }
}