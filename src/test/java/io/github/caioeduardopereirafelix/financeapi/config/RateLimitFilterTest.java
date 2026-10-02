package io.github.caioeduardopereirafelix.financeapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private RateLimitFilter filter(boolean enabled, boolean trustForwardedFor) {
        return new RateLimitFilter(enabled, trustForwardedFor, 2, 2, 2, 2, 2);
    }

    private MockHttpServletRequest post(String path, String ip, String forwarded) {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        if (forwarded != null) {
            request.addHeader("X-Forwarded-For", forwarded);
        }
        return request;
    }

    private int status(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() == null ? response.getStatus() : 200;
    }

    @Test
    void recusaDepoisDoLimiteComRetryAfterEMensagem() throws Exception {
        var filter = filter(true, false);

        assertEquals(200, status(filter, post("/v1/auth/register", "10.0.0.1", null)));
        assertEquals(200, status(filter, post("/v1/auth/register", "10.0.0.1", null)));

        var response = new MockHttpServletResponse();
        filter.doFilter(post("/v1/auth/register", "10.0.0.1", null), response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Muitas requisições"));
    }

    @Test
    void cadaEnderecoETemSeuProprioContador() throws Exception {
        var filter = filter(true, false);
        for (int i = 0; i < 2; i++) {
            status(filter, post("/v1/auth/register", "10.0.0.1", null));
        }

        assertEquals(429, status(filter, post("/v1/auth/register", "10.0.0.1", null)));
        assertEquals(200, status(filter, post("/v1/auth/register", "10.0.0.2", null)));
    }

    @Test
    void cadaRotaTemSeuProprioContador() throws Exception {
        var filter = filter(true, false);
        for (int i = 0; i < 2; i++) {
            status(filter, post("/v1/auth/register", "10.0.0.1", null));
        }

        assertEquals(429, status(filter, post("/v1/auth/register", "10.0.0.1", null)));
        assertEquals(200, status(filter, post("/v1/auth/forgot-password", "10.0.0.1", null)));
    }

    @Test
    void comConfiancaNoEncaminhamentoUsaOPrimeiroEnderecoDoCabecalho() throws Exception {
        var filter = filter(true, true);
        for (int i = 0; i < 2; i++) {
            status(filter, post("/v1/auth/register", "172.16.0.9", "203.0.113.5, 172.16.0.9"));
        }

        assertEquals(429, status(filter, post("/v1/auth/register", "172.16.0.9", "203.0.113.5, 172.16.0.9")));
        assertEquals(200, status(filter, post("/v1/auth/register", "172.16.0.9", "203.0.113.77, 172.16.0.9")));
    }

    @Test
    void semConfiancaOCabecalhoEIgnoradoEntaoNaoServeParaFugirDoLimite() throws Exception {
        var filter = filter(true, false);
        for (int i = 0; i < 2; i++) {
            status(filter, post("/v1/auth/register", "10.0.0.1", "1.1.1." + i));
        }

        assertEquals(429, status(filter, post("/v1/auth/register", "10.0.0.1", "9.9.9.9")));
    }

    @Test
    void soContaPostEmRotasListadasEPodeSerDesligado() throws Exception {
        var on = filter(true, false);
        for (int i = 0; i < 10; i++) {
            var get = new MockHttpServletRequest("GET", "/v1/auth/register");
            get.setServletPath("/v1/auth/register");
            assertEquals(200, status(on, get));
            assertEquals(200, status(on, post("/transaction", "10.0.0.1", null)));
        }

        var off = filter(false, false);
        for (int i = 0; i < 10; i++) {
            assertEquals(200, status(off, post("/v1/auth/register", "10.0.0.1", null)));
        }
    }
}
