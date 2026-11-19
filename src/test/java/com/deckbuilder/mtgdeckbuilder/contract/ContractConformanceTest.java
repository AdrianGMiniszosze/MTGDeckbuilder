package com.deckbuilder.mtgdeckbuilder.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the API contract against silent 501 responses.
 *
 * <p>
 * The OpenAPI generator declares every operation as a {@code default} method
 * that returns 501. A controller that forgets to override one still compiles and
 * ships a broken route, so this test fails as soon as a generated operation has
 * no explicit override in a {@code @RestController}. It also fails when a
 * generated {@code *Api} interface has no controller at all.
 */
@DisplayName("Contract Conformance Tests")
class ContractConformanceTest {

	private static final String API_PACKAGE = "com.deckbuilder.apigenerator.openapi.api";
	private static final String CONTROLLER_PACKAGE = "com.deckbuilder.mtgdeckbuilder.contract";

	private interface SampleApi {

		default String implemented() {
			return "ok";
		}

		default String forgotten() {
			return "501";
		}
	}

	@RestController
	private static class SampleController implements SampleApi {

		@Override
		public String implemented() {
			return "ok";
		}
	}

	@Test
	@DisplayName("Every generated operation should be overridden by a controller")
	void everyGeneratedOperationShouldBeOverridden() throws Exception {
		final List<Class<?>> apis = loadClasses(API_PACKAGE, "Api.class");
		final List<Class<?>> controllers = loadControllers();

		assertThat(apis).as("generated API interfaces").isNotEmpty();
		assertThat(controllers).as("REST controllers").isNotEmpty();

		assertThat(findMissingOverrides(apis, controllers))
			.as("generated operations without a controller override (they would answer 501)")
			.isEmpty();
	}

	@Test
	@DisplayName("The guard should detect a forgotten operation")
	void guardShouldDetectForgottenOperation() {
		final List<String> missing = findMissingOverrides(List.of(SampleApi.class), List.of(SampleController.class));

		assertThat(missing).containsExactly("SampleApi#forgotten");
	}

	@Test
	@DisplayName("The guard should detect an API without a controller")
	void guardShouldDetectUnimplementedApi() {
		final List<String> missing = findMissingOverrides(List.of(SampleApi.class), List.of());

		assertThat(missing).containsExactly("SampleApi (no controller implements it)");
	}

	private static List<String> findMissingOverrides(List<Class<?>> apis, List<Class<?>> controllers) {
		final List<String> missing = new ArrayList<>();
		for (final Class<?> api : apis) {
			final List<Class<?>> implementations = controllers.stream()
				.filter(controller -> api.isAssignableFrom(controller))
				.toList();
			if (implementations.isEmpty()) {
				missing.add(api.getSimpleName() + " (no controller implements it)");
				continue;
			}
			for (final Method operation : api.getDeclaredMethods()) {
				if (operation.isSynthetic() || Modifier.isStatic(operation.getModifiers())) {
					continue;
				}
				if (implementations.stream().noneMatch(controller -> declares(controller, operation))) {
					missing.add(api.getSimpleName() + "#" + operation.getName());
				}
			}
		}
		return missing;
	}

	private static boolean declares(Class<?> type, Method operation) {
		try {
			type.getDeclaredMethod(operation.getName(), operation.getParameterTypes());
			return true;
		}
		catch (NoSuchMethodException ex) {
			return false;
		}
	}

	private static List<Class<?>> loadControllers() throws Exception {
		return loadClasses(CONTROLLER_PACKAGE, ".class").stream()
			.filter(type -> type.isAnnotationPresent(RestController.class))
			.toList();
	}

	private static List<Class<?>> loadClasses(String packageName, String suffix) throws Exception {
		final String path = packageName.replace('.', '/');
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final java.util.Enumeration<URL> resources = classLoader.getResources(path);
		final java.util.Set<String> names = new java.util.LinkedHashSet<>();
		while (resources.hasMoreElements()) {
			final URL resource = resources.nextElement();
			if (!"file".equals(resource.getProtocol())) {
				continue;
			}
			final Path directory = Paths.get(resource.toURI());
			try (Stream<Path> files = Files.list(directory)) {
				files.map(file -> file.getFileName().toString())
					.filter(name -> name.endsWith(suffix))
					.forEach(name -> names.add(name.substring(0, name.length() - ".class".length())));
			}
		}
		assertThat(names).as("classes matching %s in %s", suffix, packageName).isNotEmpty();
		final List<Class<?>> classes = new ArrayList<>();
		for (final String name : names) {
			final Class<?> type = loadClass(packageName + "." + name);
			if (type != null) {
				classes.add(type);
			}
		}
		return classes;
	}

	private static Class<?> loadClass(String name) {
		try {
			return Class.forName(name);
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}
}
