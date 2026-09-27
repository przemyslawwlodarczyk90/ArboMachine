package com.example.arbomachine.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pilnuje reguly warstw: kontroler -> domain/<modul>/facade/XxxFacade -> domain/<modul>/service/xxxfacade/*Handler.
 * Bez dodatkowych bibliotek (skan klas Springa + refleksja).
 */
class FacadeHandlerArchitectureTest {

    private static final String BASE_PACKAGE = "com.example.arbomachine";

    private static List<Class<?>> scan(String basePackage, ClassPathScanningCandidateComponentProvider scanner) {
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            try {
                classes.add(Class.forName(candidate.getBeanClassName()));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException(exception);
            }
        }
        return classes;
    }

    private static List<Class<?>> allDomainClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return true; // takze interfejsy i klasy package-private
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        return scan(BASE_PACKAGE + ".domain", scanner);
    }

    @Test
    void controllersInjectOnlyFacades() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> controllers = scan(BASE_PACKAGE + ".infrastructure.api", scanner);

        assertThat(controllers).isNotEmpty();
        for (Class<?> controller : controllers) {
            for (Constructor<?> constructor : controller.getConstructors()) {
                for (Class<?> dependency : constructor.getParameterTypes()) {
                    assertThat(dependency.getPackageName())
                            .as("%s wstrzykuje %s - kontroler moze wolac tylko fasady", controller.getSimpleName(), dependency.getName())
                            .matches("com\\.example\\.arbomachine\\.domain\\.[a-z]+\\.facade")
                            .satisfies(packageName -> assertThat(dependency.getSimpleName()).endsWith("Facade"));
                }
            }
        }
    }

    @Test
    void domainServiceFoldersContainOnlyFacadeNamedSubfolders() {
        Set<String> facadeFolderNames = allDomainClasses().stream()
                .filter(domainClass -> domainClass.getPackageName().endsWith(".facade"))
                .map(facadeClass -> facadeClass.getSimpleName().toLowerCase())
                .collect(Collectors.toSet());

        for (Class<?> domainClass : allDomainClasses()) {
            String packageName = domainClass.getPackageName();
            if (!packageName.contains(".service")) continue;
            assertThat(packageName)
                    .as("%s lezy bezposrednio w service/ - musi byc w podfolderze fasady", domainClass.getName())
                    .matches("com\\.example\\.arbomachine\\.domain\\.[a-z]+\\.service\\.[a-z]+");
            String serviceSubfolder = packageName.substring(packageName.lastIndexOf('.') + 1);
            assertThat(facadeFolderNames)
                    .as("folder service/%s nie ma fasady o tej samej nazwie", serviceSubfolder)
                    .contains(serviceSubfolder);
        }
    }

    @Test
    void handlersExposeExactlyOnePublicHandleMethod() {
        List<Class<?>> handlers = allDomainClasses().stream()
                .filter(domainClass -> domainClass.getSimpleName().endsWith("Handler"))
                .toList();
        assertThat(handlers).as("skan nie znalazl handlerow - test sprawdzalby pusty zbior").hasSizeGreaterThan(10);

        for (Class<?> domainClass : handlers) {
            List<String> publicMethodNames = Arrays.stream(domainClass.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                    .map(Method::getName)
                    .toList();
            assertThat(publicMethodNames)
                    .as("%s - handler ma jedna operacje: publiczna metode handle(...)", domainClass.getSimpleName())
                    .containsExactly("handle");
        }
    }
}
