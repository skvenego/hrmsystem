import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for 'password123'
        String rawPassword = "password123";
        String hashedPassword = encoder.encode(rawPassword);
        
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println("\nVerify match: " + encoder.matches(rawPassword, hashedPassword));
        
        // Generate 3 more hashes (they will all be different due to random salt)
        System.out.println("\n--- Additional hashes (all valid for 'password123') ---");
        for (int i = 0; i < 3; i++) {
            System.out.println(encoder.encode(rawPassword));
        }
    }
}
