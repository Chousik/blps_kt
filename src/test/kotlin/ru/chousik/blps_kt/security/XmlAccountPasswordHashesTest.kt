package ru.chousik.blps_kt.security

import kotlin.test.Test
import kotlin.test.assertTrue
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class XmlAccountPasswordHashesTest {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Test
    fun `current xml accounts are stored as valid bcrypt hashes`() {
        assertTrue(
            passwordEncoder.matches(
                "guestpass",
                "\$2y\$10\$8xbHDMYmisik70DQ/X4Gy.gKZESN3Z/OKpey1rSkmPSC0y82edW9e"
            )
        )
        assertTrue(
            passwordEncoder.matches(
                "guestvika",
                "\$2y\$10\$jN.wdTU.roZM9h.BYBCzsuivVAg8efnKKElsae4UjP.9H3jBHc9W2"
            )
        )
        assertTrue(
            passwordEncoder.matches(
                "hostpass",
                "\$2y\$10\$XXmxzcukcMDH9Ym3Hr.4HeafeUxrRsE9cWHtU98r.Xp0Uk1tdO262"
            )
        )
        assertTrue(
            passwordEncoder.matches(
                "hostelena",
                "\$2y\$10\$AOVbKghwZhIEpXgFSP6fIeLu5NYl4FY.HFfLNJg1tW/VAedrxfnj2"
            )
        )
        assertTrue(
            passwordEncoder.matches(
                "opspass",
                "\$2y\$10\$R6dIdw6FVdVfONTlRdlED.xO/1bHMLJz0pxN7IhfdEpZ8WAtHbsUe"
            )
        )
    }
}
