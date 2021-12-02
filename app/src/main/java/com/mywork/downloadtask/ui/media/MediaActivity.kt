package com.mywork.downloadtask.ui.media

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
import com.mywork.downloadtask.R
import com.mywork.downloadtask.data.remote.model.Media
import com.mywork.downloadtask.databinding.ActivityMediaBinding
import com.mywork.downloadtask.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaActivity : AppCompatActivity(), MediaAdapter.ClickListener {
    private var _binding: ActivityMediaBinding? = null
    val binding get() = _binding!!
    private lateinit var viewModel: MediaViewModel
    private lateinit var navController: NavController

    @Inject
    lateinit var adapter: MediaAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_media)

        binding.lifecycleOwner = this
        //databinding
        binding.adapter = adapter
        adapter.setClickListener(this)

        viewModel = ViewModelProvider(this)[MediaViewModel::class.java]

        viewModel.getMedias()

        //check if the response didn't come after 5 seconds
        GlobalScope.launch { checkAvilabilityForResponse() }

        refreshLayout()
        getMediasObserver()
        registerReceiver()
    }

    //retrieve the data from database until fetch api response and save the new data in database again
    private fun getMediasObserver() {
        viewModel.mediasLiveData.observe(this) { response ->
            when (response) {
                is Resource.Success -> {
                    adapter.submitList(response.data)
                    checkRefreshVisibility()
                }
                is Resource.Error -> {
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
                    checkRefreshVisibility()
                }
                is Resource.Loading -> {
                    adapter.submitList(response.data)
                    if (binding.refreshMedias.isRefreshing)
                        binding.progressBar.visibility = View.GONE
                    else
                        binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun refreshLayout() {
        binding.refreshMedias.setOnRefreshListener {
            if (binding.progressBar.visibility == View.VISIBLE)
                binding.progressBar.visibility = View.GONE
            viewModel.getMedias()
        }
    }

    private fun checkRefreshVisibility() {
        if (binding.refreshMedias.isRefreshing)
            binding.refreshMedias.isRefreshing = false
        else
            binding.progressBar.visibility = View.GONE
    }


    // item download button click
    private var position: Int? = null
    private var oldPosition: Int? = null
    override fun clickDownload(position: Int) {
        this.oldPosition= this.position
        this.position = position
        //need this condition when cancelled download
        if (downloading) {

            showAlertIfFileDownloadingNow()
        } else
            requestPermissionWrite()
    }

    //when media downloading and want another media
    private fun showAlertIfFileDownloadingNow() {
        Snackbar.make(
            binding.constraint,
            "cancel another download and download this",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("Cancel") {
                //to stop while loop and updating ui
                GlobalScope.launch {updateUiWhenDownload(false, isDownloading = false, oldPosition!!)}
                downloading = false
                manager.remove(mediaDownloadId)
                dismiss()
                //already have a permission
                startDownloadAfterCheckPermission()
            }
                .show()
        }
    }

    private var mediaDownloadId: Long = 0
    private var downloading: Boolean = false
    private lateinit var manager: DownloadManager

    //this function is responsible to prepare request to download manager and calculate the progress percentage
    @SuppressLint("Range")
    private fun startDownloadAfterCheckPermission() {
        GlobalScope.launch {
            val media = adapter.currentList[position!!]
            updateUiWhenDownload(isDownloaded = false, isDownloading = true , position!!)

            manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(Uri.parse(media.url.trim('(', ')')))
                .setTitle(media.name + if (media.type == "VIDEO") ".mp4" else ".pdf")
                .setMimeType(if (media.type == "VIDEO") "video/mp4" else "application/pdf")
                .setDescription("${media.name} is Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                .setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                media.name + if (media.type == "VIDEO") ".mp4" else ".pdf"
            )
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            mediaDownloadId = manager.enqueue(request)
            downloading = true
            while (downloading) {
                val query = DownloadManager.Query()

                query.setFilterById(mediaDownloadId)
                val cursor: Cursor = manager.query(query)
                cursor.moveToFirst()
                val status: Int =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded: Long =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal: Long =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                var progress = ((bytesDownloaded * 100L) / bytesTotal)

                withContext(Dispatchers.Main) {

                    if (binding.tvPercentage.visibility == View.GONE) {
                        binding.circularProgressBar.visibility = View.VISIBLE
                        binding.tvPercentage.visibility = View.VISIBLE
                    }
                    binding.circularProgressBar.progress = progress.toFloat()
                    binding.tvPercentage.text = "$progress%"
                }

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    GlobalScope.launch {
                        updateUiWhenDownload(isDownloaded = true, isDownloading = false , position!!)
                    }
                    downloading = false
                    withContext(Dispatchers.Main) {
                        binding.circularProgressBar.visibility = View.GONE
                        binding.tvPercentage.visibility = View.GONE
                    }
                }
                cursor.close()
            }
        }

    }

    private suspend fun checkAvilabilityForResponse() {
        delay(5000)
        if (adapter.currentList == null || adapter.currentList.size == 0) {
            Snackbar.make(
                binding.constraint,
                "please wait we  will save all the data to prevent this problem from recurring again",
                Snackbar.LENGTH_INDEFINITE
            ).apply {
                setAction("OK") {
                    dismiss()
                }
                    .show()
            }
        }
    }

    // change state of download button through different operations
    private suspend fun updateUiWhenDownload(isDownloaded: Boolean, isDownloading: Boolean ,position:Int) {
        val media = adapter.currentList[position!!]
        val medias: MutableList<Media> = ArrayList(adapter.currentList)
        val updatedMedia = Media(
            media.id,
            media.type,
            media.url,
            media.name,
            isDownloaded,
            isDownloading,
            downloadedId = media.downloadedId
        )
        medias.add(position!!, updatedMedia)
        medias.remove(media)
        adapter.submitList(medias)

    }


    private fun registerReceiver() {
        registerReceiver(
            brMediaDownload,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }


    //receiver to listen to download manager when download complete
    private var brMediaDownload = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == mediaDownloadId) {
                if (binding.tvPercentage.visibility == View.VISIBLE) {
                    binding.circularProgressBar.visibility = View.GONE
                    binding.tvPercentage.visibility = View.GONE
                }
            } else {
                Toast.makeText(context, "download cancelled", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(brMediaDownload)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //permission to write on storage
    private fun checkWriteStoragePermission() = ActivityCompat.checkSelfPermission(
        this,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    //permission to read from storage
    private fun checkReadStoragePermission() = ActivityCompat.checkSelfPermission(
        this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissionWrite() {
        var permissionsToRequest = mutableListOf<String>()
        if (!checkWriteStoragePermission()) permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (!checkReadStoragePermission()) permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionsToRequest.isNotEmpty())
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                0
            )
        else
            startDownloadAfterCheckPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {// && permissions[i] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    Log.d("Permission", "${permissions[i]} is Granted")
                    startDownloadAfterCheckPermission()
                } else {
                    Log.d("Permission", "${permissions[i]} is NOT Granted")
                }
            }
        }
    }
}