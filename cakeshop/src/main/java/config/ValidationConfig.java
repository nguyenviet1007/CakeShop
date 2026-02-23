package config;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.cfg.GenericConstraintDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.validation.Validator;

@Configuration
public class ValidationConfig implements WebMvcConfigurer {

    @Autowired
    private org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory;

    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setProviderClass(HibernateValidator.class);
        // Quan trọng: Sử dụng Spring's factory để @Autowired hoạt động trong ConstraintValidator
        factory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return factory;
    }
}
