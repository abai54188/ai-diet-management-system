package com.diet.config;

import com.diet.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 * 1. 注册 JWT 拦截器，白名单外的接口均需携带令牌
 * 2. 静态资源兜底: 一体化部署时前端采用 Vue Router history 模式,
 *    刷新 /food/query 等深层路径在静态目录中找不到对应文件, 统一回退到 index.html
 *
 * @author diet
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // 登录、注册接口放行
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }

    /**
     * SPA 前端路由兜底: 静态资源找不到文件时返回 index.html, 交由前端路由接管,
     * 而非抛出 404/500。
     * 带真实后缀的静态资源(.js/.css/.png 等)仍由本解析器命中文件正常返回。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        return (requestedResource.exists() && requestedResource.isReadable())
                                ? requestedResource
                                : new ClassPathResource("/static/index.html");
                    }
                });
    }
}