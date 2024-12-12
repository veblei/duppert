package com.example.badeapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.badeapp.viewmodel.KartActivityViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class KartActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun sjekkFilterInputTest() {
        var result = false
        //To false input returnerer false.
        result = KartActivity().sjekkFilterInput(false, false, "10.0", "10.0", "12.0", "12.0")
        assertThat(result).isFalse()
        //To true input returnerer true.
        result = KartActivity().sjekkFilterInput(true, true, "10.0", "10.0", "12.0", "12.0")
        assertThat(result).isTrue()
        //True luftinput og false vanninput returnerer true.
        result = KartActivity().sjekkFilterInput(true, false, "10.0", "10.0", "12.0", "12.0")
        assertThat(result).isTrue()
        //False luftinput og true vanninput returnerer true.
        result = KartActivity().sjekkFilterInput(false, true, "10.0", "10.0", "12.0", "12.0")
        assertThat(result).isTrue()
        //Luftinput > lufttemp returnerer false.
        result = KartActivity().sjekkFilterInput(true, true, "10.0", "10.0", "8.0", "12.0")
        assertThat(result).isFalse()
        //Vanninput > vanntemp returnerer false.
        result = KartActivity().sjekkFilterInput(true, true, "10.0", "10.0", "12.0", "8.0")
        assertThat(result).isFalse()
    }

    @Test
    fun sjekkLinkTest() {
        val viewModel = KartActivityViewModel()
        val urlUtenStedsnavn = "https://www.oslo.kommune.no/natur-kultur-og-fritid/tur-og-friluftsliv/badeplasser-og-temperaturer/"
        //Sjekker etter link for Sognsvann skal returnere true.
        viewModel.sjekkLink("${urlUtenStedsnavn}sognsvann")
        viewModel.linkModel.observe(KartActivity()) { gyldig ->
            assertThat(gyldig).isTrue()
        }
        //Sjekker etter link for Helgelandsmoen skal returnere false.
        viewModel.sjekkLink("${urlUtenStedsnavn}helgelandsmoen")
        viewModel.linkModel.observe(KartActivity()) { gyldig ->
            assertThat(gyldig).isFalse()
        }
    }

}