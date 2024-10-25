package dev.arias.huapaya.gateway.configuration;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
public class GatewayConfig {

        @Profile(value = "eureka-off")
        @Bean
        public RouteLocator RouteLocatorEurekaOff(RouteLocatorBuilder builder) {
                return builder
                                .routes()
                                .route(route -> route.path("/api/maintenance/**")
                                                .uri("http://localhost:8080"))
                                .build();
        }

        @Profile(value = "eureka-on")
        @Bean
        public RouteLocator RouteLocatorEurekaOn(RouteLocatorBuilder builder) {
                return builder
                                .routes()
                                .route(route -> route.path("/proxy-maintenance/**")
                                                .uri("lb://ms-maintenance-proxy"))
                                .route(route -> route.path("/proxy-report-maintenance/**")
                                                .uri("lb://ms-maintenance-report-proxy"))

                                .route(route -> route.path("/proxy-sale/**")
                                                .uri("lb://ms-sale-proxy"))
                                .route(route -> route.path("/proxy-report-sale/**")
                                                .uri("lb://ms-sale-report-proxy"))

                                .route(route -> route.path("/api/maintenance/**")
                                                .uri("lb://ms-maintenance"))
                                .route(route -> route.path("/api/report/maintenance/**")
                                                .uri("lb://ms-maintenance-report"))

                                .route(route -> route.path("/api/sale/**")
                                                .uri("lb://ms-sale"))
                                .build();
        }

}
