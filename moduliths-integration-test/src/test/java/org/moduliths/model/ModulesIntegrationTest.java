/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moduliths.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;
import org.moduliths.model.Module;
import org.moduliths.model.Modules;
import org.moduliths.model.NamedInterface;
import org.moduliths.model.NamedInterfaces;
import org.moduliths.model.Module.DependencyType;

import com.acme.myproject.Application;
import com.acme.myproject.complex.internal.FirstTypeBasedPort;
import com.acme.myproject.complex.internal.SecondTypeBasePort;
import com.acme.myproject.moduleA.SomeConfigurationA.SomeAtBeanComponentA;

/**
 * @author Oliver Gierke
 * @author Peter Gafert
 */
public class ModulesIntegrationTest {

	Modules modules = Modules.of(Application.class);

	@Test
	public void exposesModulesForPrimaryPackages() {

		Optional<Module> module = modules.getModuleByName("moduleB");

		assertThat(module).hasValueSatisfying(it -> {
			assertThat(it.getBootstrapDependencies(modules)).anySatisfy(dep -> {
				assertThat(dep.getName()).isEqualTo("moduleA");
			});
		});
	}

	@Test
	public void usesExplicitlyAnnotatedDisplayName() {

		Optional<Module> module = modules.getModuleByName("moduleC");

		assertThat(module).hasValueSatisfying(it -> {
			assertThat(it.getDisplayName()).isEqualTo("MyModule C");
		});
	}

	@Test
	public void rejectsDependencyIntoInternalPackage() {

		Optional<Module> module = modules.getModuleByName("invalid");

		assertThat(module).hasValueSatisfying(it -> {
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
				it.verifyDependencies(modules);
			});
		});
	}

	@Test
	public void complexModuleExposesNamedInterfaces() {

		Optional<Module> module = modules.getModuleByName("complex");

		assertThat(module).hasValueSatisfying(it -> {

			NamedInterfaces interfaces = it.getNamedInterfaces();

			assertThat(interfaces.stream().map(NamedInterface::getName)) //
					.containsExactlyInAnyOrder("API", "SPI", "Port 1", "Port 2", "Port 3");

			verifyNamedInterfaces(interfaces, "Port 1", FirstTypeBasedPort.class, SecondTypeBasePort.class);
			verifyNamedInterfaces(interfaces, "Port 2", FirstTypeBasedPort.class, SecondTypeBasePort.class);
			verifyNamedInterfaces(interfaces, "Port 3", FirstTypeBasedPort.class, SecondTypeBasePort.class);
		});
	}

	private static void verifyNamedInterfaces(NamedInterfaces interfaces, String name, Class<?>... types) {

		Optional<NamedInterface> byName = interfaces.getByName(name);

		Stream.of(types).forEach(type -> {
			assertThat(byName).hasValueSatisfying(named -> named.contains(type));
		});
	}

	@Test
	public void discoversAtBeanComponent() {

		Optional<Module> module = modules.getModuleByName("moduleA");

		assertThat(module).hasValueSatisfying(it -> {
			assertThat(it.getSpringBeans().contains(SomeAtBeanComponentA.class.getName())).isTrue();
		});
	}

	@Test
	public void moduleBListensToModuleA() {

		Optional<Module> module = modules.getModuleByName("moduleB");
		Module moduleA = modules.getModuleByName("moduleA").orElseThrow(IllegalStateException::new);

		assertThat(module).hasValueSatisfying(it -> {
			assertThat(it.getDependencies(modules, DependencyType.EVENT_LISTENER)) //
					.contains(moduleA);
		});
	}

	@Test
	public void rejectsNotExplicitlyListedDependency() {

		Optional<Module> moduleByName = modules.getModuleByName("invalid2");

		assertThat(moduleByName).hasValueSatisfying(it -> {
			assertThatExceptionOfType(IllegalStateException.class) //
					.isThrownBy(() -> it.verifyDependencies(modules)) //
					.withMessageContaining(it.getName()); //
		});
	}
}