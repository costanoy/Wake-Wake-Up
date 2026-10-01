package com.wakewakeup.ui.sound

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.wakewakeup.R

/** An alarm sound shipped inside the app (res/raw). */
data class BundledSound(@RawRes val rawRes: Int, @StringRes val nameRes: Int, @StringRes val descRes: Int)

object BundledSounds {
    val all = listOf(
        BundledSound(R.raw.campainha_mecanica, R.string.sound_bell_name, R.string.sound_bell_desc),
        BundledSound(R.raw.telefone_de_disco, R.string.sound_phone_name, R.string.sound_phone_desc),
        BundledSound(R.raw.bipe_digital_1, R.string.sound_beep1_name, R.string.sound_beep1_desc),
        BundledSound(R.raw.bipe_digital_2, R.string.sound_beep2_name, R.string.sound_beep2_desc),
        BundledSound(R.raw.sirene_1, R.string.sound_siren1_name, R.string.sound_siren1_desc),
        BundledSound(R.raw.sirene_2, R.string.sound_siren2_name, R.string.sound_siren2_desc),
        BundledSound(R.raw.passaros, R.string.sound_birds_name, R.string.sound_birds_desc),
    )

    /** Addressed by resource name, so a saved alarm keeps its sound across app updates. */
    fun uriOf(context: Context, sound: BundledSound): String =
        "android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(sound.rawRes)}"
}
