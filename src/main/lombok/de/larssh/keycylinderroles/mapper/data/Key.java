package de.larssh.keycylinderroles.mapper.data;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class Key {
	@EqualsAndHashCode.Include
	@SuppressWarnings("PMD.ShortVariable")
	String id;

	Optional<String> name;

	Optional<String> lastName;

	Optional<String> firstName;

	Optional<String> group;

	boolean ignore;

	public String getTitle() {
		final String title = getName().orElseGet(() -> getLastName().orElse("") //
				+ getFirstName().map(firstName -> ", " + firstName).orElse(""))
				+ getGroup().map(group -> " (" + group + ')').orElse("");
		return title.isEmpty() ? getId() : title;
	}
}
