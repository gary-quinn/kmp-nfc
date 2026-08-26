package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand

/** Active HCE session wired into [HceServiceRegistry]. */
internal interface HceSession {
    fun dispatch(command: ApduCommand)

    fun onDeactivated(reason: DeactivationReason)
}
