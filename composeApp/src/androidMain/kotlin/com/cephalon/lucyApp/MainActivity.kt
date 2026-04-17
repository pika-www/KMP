package com.cephalon.lucyApp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cephalon.lucyApp.App

class MainActivity : ComponentActivity() {

    // 进入 app 需要申请的定位权限（FINE + COARSE，二者配套）
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // 结果不在启动期强制处理：
        // - 授权成功：后续 BLE / Wi‑Fi 扫描等场景可直接使用；
        // - 授权拒绝：相关功能在使用时会再次弹出引导。
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestLocationPermission()
        setContent {
            App()
        }
    }

    private fun maybeRequestLocationPermission() {
        val allGranted = locationPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }
}
