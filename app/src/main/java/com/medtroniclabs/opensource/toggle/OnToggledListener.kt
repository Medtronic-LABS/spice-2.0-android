package com.medtroniclabs.opensource.toggle

interface OnToggledListener {
    fun onSwitched(toggleableView: ToggleableView?, isOn: Boolean)
}