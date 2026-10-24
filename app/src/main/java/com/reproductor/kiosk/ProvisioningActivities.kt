package com.reproductor.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

/**
 * Android 10+ asks the app, during QR provisioning, which mode it wants: this app is always a
 * fully managed device (kiosk). Older Android versions never open it.
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(
            RESULT_OK,
            Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE),
        )
        finish()
    }
}

/** Android 10+ lets the app set its policies before the setup wizard ends; the home screen does it anyway. */
class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Kiosk.isOwner(this)) Kiosk.applyPolicies(this)
        setResult(RESULT_OK)
        finish()
    }
}
