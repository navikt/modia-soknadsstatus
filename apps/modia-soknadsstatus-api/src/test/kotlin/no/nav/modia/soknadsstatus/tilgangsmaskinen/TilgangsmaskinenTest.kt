import io.mockk.*
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.apis.TilgangControllerApi
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.infrastructure.ClientException
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.infrastructure.ServerException
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsmaskinenImpl
import no.nav.modia.soknadsstatus.utils.CacheUtils
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TilgangsmaskinenImplTest {
    private lateinit var tilgangsmaskinen: TilgangsmaskinenImpl
    private val mockClient: OkHttpClient = mockk()
    private val tilgangsMaskinenApi: TilgangControllerApi = mockk()

    private val mockUrl = "http://test-url.no"
    private val fnr = Fnr("10108000398")
    private val veilederIdent = NavIdent("Z99999")

    @BeforeEach
    fun setUp() {
        tilgangsmaskinen = TilgangsmaskinenImpl(mockUrl, mockClient, CacheUtils.createCache(), tilgangsMaskinenApi)
    }

    @Test
    fun `skal returnere respons med harTilgang true når API-kall lykkes`() {
        every { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) } answers { Unit }
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(true, result?.harTilgang)
        assertEquals(null, result?.error)
        verify { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) }
    }

    @Test
    fun `skal returnere respons med harTilgang false når API kaster ClientException med 403`() {
        every { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) } throws
            ClientException(message = "", response = mockk(), statusCode = 403)
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(false, result?.harTilgang)
        assertEquals(null, result?.error)
        verify { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) }
    }

    @Test
    fun `skal returnere null når API kaster en uventet feil`() {
        every { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) } throws RuntimeException("Unexpected error")
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(null, result)
        verify { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) }
    }

    @Test
    fun `skal returnere respons med harTilgang false når API kaster ServerException`() {
        every { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) } throws ServerException(message = "Server error")
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(false, result?.harTilgang)
        assertEquals(null, result?.error)
        verify { tilgangsMaskinenApi.kompletteReglerCCF(veilederIdent.get(), fnr.get()) }
    }
}
