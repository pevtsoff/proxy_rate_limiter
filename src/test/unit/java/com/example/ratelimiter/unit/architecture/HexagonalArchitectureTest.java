package com.example.ratelimiter.unit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the hexagon: the domain and application layers must not know about adapters,
 * frameworks or infrastructure. Dependencies only point inwards.
 */
class HexagonalArchitectureTest {

    private static final String ROOT = "com.example.ratelimiter";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void domainDependsOnNothingButTheJdk() {
        noClasses().that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideOutsideOfPackages(ROOT + ".domain..",
                        "java..")
                .check(classes);
    }

    @Test
    void applicationDependsOnlyOnDomainAndJdk() {
        noClasses().that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideOutsideOfPackages(
                        ROOT + ".application..", ROOT + ".domain..", "java..")
                .check(classes);
    }

    @Test
    void inboundAdaptersDoNotUseOutboundAdapters() {
        noClasses().that().resideInAPackage(ROOT + ".adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.out..")
                .check(classes);
    }

    @Test
    void outboundAdaptersDoNotUseInboundAdapters() {
        noClasses().that().resideInAPackage(ROOT + ".adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.in..")
                .check(classes);
    }

    @Test
    void adaptersUseUseCasesThroughPortsOnly() {
        noClasses().that().resideInAPackage(ROOT + ".adapter..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".application.service..")
                .check(classes);
    }
}
