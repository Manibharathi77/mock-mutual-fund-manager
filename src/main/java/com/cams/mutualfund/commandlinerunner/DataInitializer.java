package com.cams.mutualfund.commandlinerunner;

import com.cams.mutualfund.data.Roles;
import com.cams.mutualfund.data.dao.CamsUser;
import com.cams.mutualfund.data.dao.Nav;
import com.cams.mutualfund.data.dao.Script;
import com.cams.mutualfund.repository.NavRepository;
import com.cams.mutualfund.repository.ScriptRepository;
import com.cams.mutualfund.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataInitializer {

    //    @Profile("local")
    @Bean
    public CommandLineRunner loadData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      ScriptRepository scriptRepository,
                                      NavRepository navRepository) {
        return args -> {
            loadMockUserData(userRepository, passwordEncoder);
            loadMockScriptAndNavData(scriptRepository, navRepository);
        };
    }

    private static void loadMockScriptAndNavData(ScriptRepository scriptRepository, NavRepository navRepository) {
        Script hdfc = new Script("HDFC_EQTY", "HDFC Equity Growth Fund", "Equity", "HDFC AMC");
        Script icici = new Script("ICICI_BOND", "ICICI Prudential Bond Fund", "Debt", "ICICI AMC");
        Script axis = new Script("AXIS_BAL", "Axis Balanced Advantage Fund", "Hybrid", "Axis Mutual");

        scriptRepository.saveAll(List.of(hdfc, icici, axis));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        navRepository.saveAll(List.of(
                new Nav(yesterday, 316.70, hdfc),
                new Nav(today, 319.20, hdfc),

                new Nav(yesterday, 100.25, icici),
                new Nav(today, 102.75, icici),

                new Nav(yesterday, 210.80, axis),
                new Nav(today, 213.40, axis)
        ));
    }

    private static void loadMockUserData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        if (userRepository.count() == 0) {
            userRepository.save(new CamsUser("admin", passwordEncoder.encode("admin123"), Roles.ADMIN));
            userRepository.save(new CamsUser("regularUser1", passwordEncoder.encode("password1"), Roles.USER));
            userRepository.save(new CamsUser("regularUser2", passwordEncoder.encode("password2"), Roles.USER));
            System.out.println("Loaded mock data for users");
        }
    }
}
