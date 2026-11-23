package ecommerce_backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Product sample() {
        Product product = new Product("Phone", new BigDecimal("499.99"), 10);
        product.setId(1L);
        return product;
    }

    @Test
    void getProducts_withToken_returnsList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phone"))
                .andExpect(jsonPath("$[0].price").value(499.99));
    }

    @Test
    void getProducts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_asStoreUser_returns403() throws Exception {
        mockMvc.perform(post("/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STORE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"price\":499.99,\"stock\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_asAdmin_returns201() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"price\":499.99,\"stock\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProduct_withInvalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"stock\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stock").exists());
    }

    @Test
    void getProduct_unknownId_returns404() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ApiExceptions.NotFoundException("Product 99 not found"));

        mockMvc.perform(get("/products/99").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product 99 not found"));
    }
}
