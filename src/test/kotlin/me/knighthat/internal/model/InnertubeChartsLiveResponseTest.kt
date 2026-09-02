package me.knighthat.internal.model

import me.knighthat.innertube.decode
import me.knighthat.innertube.response.Tabs
import me.knighthat.internal.response.BrowseResponseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

/**
 * Full `FEmusic_charts` response captured on 2026-09-02 (gl=DE, hl=de).
 *
 * YouTube replaced `selectedAccessibility`/`deselectedAccessibility` of the country menu
 * items with a single `accessibility`; the strict model used to fail deserialization,
 * which surfaced as "Failed to get charts" on the home screen.
 */
class InnertubeChartsLiveResponseTest {

    @Test
    fun testChartsResponseSeptember2026() {
        val fileName = "ytm/browse/charts_endpoint_response_de_2026-09.json"
        ClassLoader.getSystemResourceAsStream( fileName )
            .also( ::assertNotNull )
            ?.use { inStream ->
                val response = inStream.decode<BrowseResponseImpl>()
                assertNotNull( response )

                val renderer = response.contents
                                       ?.singleColumnBrowseResultsRenderer
                                       ?.tabs
                                       ?.firstNotNullOfOrNull( Tabs.Tab::tabRenderer )
                                       ?.content
                                       ?.sectionListRenderer
                assertNotNull( renderer )

                val charts = InnertubeChartsImpl.from( renderer )
                assertEquals( "Deutschland", charts.selectedCountryName )
                assertEquals( "Land auswählen", charts.menu.title )
                // 71 options minus the divider
                assertEquals( 70, charts.menu.items.size )
                // Video charts + top artists; the menu shelf is not a section
                assertEquals( 2, charts.sections.size )
            }
    }
}
