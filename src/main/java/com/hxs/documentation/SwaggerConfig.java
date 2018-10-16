package com.hxs.documentation;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

/**
 *  Swagger2 Documentation Configuration
 *
 * @author HSteidel
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(getApiInfo())
                    .select()
                    .paths(paths())
                    .apis(RequestHandlerSelectors.any())
                    .build()
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET, getGlobalRespMessages())
                .globalResponseMessage(RequestMethod.POST, getGlobalRespMessages())
                .globalResponseMessage(RequestMethod.PUT, getGlobalRespMessages())
                .globalResponseMessage(RequestMethod.PATCH, getGlobalRespMessages())
                .globalResponseMessage(RequestMethod.DELETE, getGlobalRespMessages());
    }


    private List<ResponseMessage> getGlobalRespMessages(){
        return  Lists.newArrayList( new ResponseMessageBuilder()
                                        .code(500)
                                        .message("Unexpected error")
                                        .responseModel(new ModelRef("ApiError")).build(),
                                    new ResponseMessageBuilder()
                                        .code(401)
                                        .message("Inadequate permissions")
                                        .responseModel(new ModelRef("ApiError")).build()

                );
    }

    private Predicate<String> paths() {
        return or(
                regex("/files.*"));
    }

    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                    .title("Object Store Service")
                    .description("Object storage over REST")
                    .version("0.0.1")
                .contact(new Contact("Hermann Steidel","https://www.linkedin.com/in/hermannsteidel","hsteidel@outlook.com"))
                    .build();
    }
}