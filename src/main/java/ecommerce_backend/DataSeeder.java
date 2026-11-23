package ecommerce_backend;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String storePassword;

    public DataSeeder(AppUserRepository userRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.store-password}") String storePassword) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.storePassword = storePassword;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", adminPassword, "ADMIN");
        seedUser("store1", storePassword, "STORE_USER");
        if (productRepository.count() == 0) {
            productRepository.save(new Product("Bluetooth Headphones", new BigDecimal("59.99"), 25));
            productRepository.save(new Product("USB-C Cable", new BigDecimal("9.99"), 100));
            productRepository.save(new Product("Energy Drink 12-pack", new BigDecimal("19.99"), 4));
            log.info("Seeded sample products");
        }
    }

    private void seedUser(String username, String password, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(password), role));
            log.info("Seeded user {} with role {}", username, role);
        }
    }
}
