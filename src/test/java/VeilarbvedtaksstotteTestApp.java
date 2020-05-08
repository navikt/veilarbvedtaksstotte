
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VeilarbvedtaksstotteTestApp {

    private static final String TEST_PORT = "8812";
    private static final String[] ARGUMENTS = {TEST_PORT};

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VeilarbvedtaksstotteTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }

}
