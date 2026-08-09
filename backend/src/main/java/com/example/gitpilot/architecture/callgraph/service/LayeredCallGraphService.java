package com.example.gitpilot.architecture.callgraph.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.util.*;

@Service
public class LayeredCallGraphService {

    private static final String BASE_PACKAGE = "com.example.gitpilot";

    private final ApplicationContext ctx;

    @Autowired
    public LayeredCallGraphService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public Map<String, Object> buildCallGraph() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, String>> edges = new ArrayList<>();
        Set<String> registered = new HashSet<>();

        String[] beanNames = ctx.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            try {
                Class<?> beanType = ctx.getType(beanName);
                if (beanType == null) continue;
                if (!beanType.getName().startsWith(BASE_PACKAGE)) continue;

                String type = classifyBean(beanType);
                if (type == null) continue;

                String id = beanType.getSimpleName();
                if (!registered.add(id)) continue;

                nodes.add(Map.of("id", id, "type", type, "layer", layerOf(type)));

                // Scan constructor dependencies
                Constructor<?>[] constructors = beanType.getDeclaredConstructors();
                if (constructors.length == 0) continue;
                Constructor<?> ctor = constructors[0];
                for (Constructor<?> c : constructors) {
                    if (c.getParameterCount() > ctor.getParameterCount()) ctor = c;
                }
                for (Class<?> param : ctor.getParameterTypes()) {
                    if (!param.getName().startsWith(BASE_PACKAGE)) continue;
                    String depType = classifyBean(param);
                    if (depType == null) continue;
                    String depId = param.getSimpleName();
                    if (registered.add(depId)) {
                        nodes.add(Map.of("id", depId, "type", depType, "layer", layerOf(depType)));
                    }
                    edges.add(Map.of("from", id, "to", depId));
                }
            } catch (Exception ignored) {}
        }

        return Map.of("nodes", nodes, "edges", edges);
    }

    private String classifyBean(Class<?> clazz) {
        if (clazz.isAnnotationPresent(RestController.class) || clazz.isAnnotationPresent(Controller.class)) {
            return "CONTROLLER";
        }
        if (clazz.isAnnotationPresent(Service.class)) {
            return "SERVICE";
        }
        if (JpaRepository.class.isAssignableFrom(clazz)) {
            return "REPOSITORY";
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (JpaRepository.class.isAssignableFrom(iface)) {
                return "REPOSITORY";
            }
        }
        return null;
    }

    private int layerOf(String type) {
        return switch (type) {
            case "CONTROLLER" -> 0;
            case "SERVICE" -> 1;
            case "REPOSITORY" -> 2;
            default -> 3;
        };
    }
}
