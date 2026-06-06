package com.nova.assistant.commands

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class FlashlightExecutor(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun setFlashlight(on: Boolean): String {
        return try {
            val cameraId = torchCameraId() ?: return "This device does not have a flashlight"
            cameraManager.setTorchMode(cameraId, on)
            if (on) "Flashlight on" else "Flashlight off"
        } catch (e: Exception) {
            "Flashlight error: ${e.message}"
        }
    }

    private fun torchCameraId(): String? =
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
}
