import io.mockk.*
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsmaskinenImpl
import no.nav.modia.soknadsstatus.utils.CacheUtils
import okhttp3.*
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TilgangsmaskinenImplTest {
    private lateinit var tilgangsmaskinen: TilgangsmaskinenImpl
    private val mockClient: OkHttpClient = mockk()
    private val mockCall = mockk<Call>()

    private val mockUrl = "http://test-url.no"
    private val fnr = Fnr("10108000398")
    private val veilederIdent = NavIdent("Z99999")

    @BeforeEach
    fun setUp() {
        tilgangsmaskinen = TilgangsmaskinenImpl(mockUrl, mockClient, CacheUtils.createCache())
        every { mockClient.newCall(any()) } returns mockCall
    }

    @Test
    fun `skal returnere respons med harTilgang true når API-kall lykkes`() {
        every { mockCall.execute() } returns mockResponse(204)
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(true, result?.harTilgang)
        verify { mockClient.newCall(any()) }
    }

    @Test
    fun `skal returnere respons med harTilgang false når API kaster ClientException med 403`() {
        every { mockCall.execute() } returns mockResponse(403)
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(false, result?.harTilgang)
        verify { mockClient.newCall(any()) }
    }

    @Test
    fun `skal returnere null når API kaster en uventet feil`() {
        every { mockCall.execute() } returns mockResponse(302)
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(null, result)
        verify { mockClient.newCall(any()) }
    }

    @Test
    fun `skal returnere respons med harTilgang false når API kaster ServerException`() {
        every { mockCall.execute() } returns mockResponse(500)
        val result = tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr)
        assertEquals(false, result?.harTilgang)
        verify { mockClient.newCall(any()) }
    }

    private fun mockResponse(
        code: Int,
        message: String = "Mocked Response",
        bodyContent: String = "{}",
    ): Response {
        val responseBody =
            mockk<ResponseBody> {
                every { contentType() } returns "application/json".toMediaTypeOrNull()
                every { string() } returns bodyContent
            }

        return Response
            .Builder()
            .code(code)
            .message(message)
            .protocol(Protocol.HTTP_1_1) // Specify HTTP version
            .request(mockk(relaxed = true)) // Mock an OkHttp request for the Response
            .body(responseBody) // Attach mocked ResponseBody
            .build()
    }
}
