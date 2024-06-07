package uz.umarov.handgesture.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PermissionHelper {

    fun isCameraPermissionGranted(context: Context) : Boolean{
        return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}